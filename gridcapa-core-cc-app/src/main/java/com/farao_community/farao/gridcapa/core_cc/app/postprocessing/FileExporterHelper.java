/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.postprocessing;

import com.farao_community.farao.data.core_cne_exporter.CoreCneExporter;
import com.farao_community.farao.data.core_cne_exporter.CoreCneExporterParameters;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCreationContext;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.gridcapa.core_cc.app.RaoClient;
import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoRequest;
import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoResult;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.exceptions.RaoIntegrationException;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import com.farao_community.farao.gridcapa.core_cc.app.util.IntervalUtil;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileExporterHelper {

    private final MinioAdapter minioAdapter;

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoClient.class);

    private static final String ALEGRO_GEN_BE = "XLI_OB1B_generator";
    private static final String ALEGRO_GEN_DE = "XLI_OB1A_generator";
    private static final String DOMAIN_ID = "10Y1001C--00059P";

    public FileExporterHelper(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public void exportNetworkInTmpOutput(RaoIntegrationTask raoIntegrationTask, HourlyRaoResult hourlyRaoResult) throws IOException {
        LOGGER.info("RAO integration task: '{}', exporting uct network with pra for timestamp: '{}'", raoIntegrationTask.getTaskId(), hourlyRaoResult.getInstant());

        Network network;
        try (InputStream cgmInputStream = minioAdapter.getInputStreamFromUrl(hourlyRaoResult.getNetworkWithPraUrl())) {
            network = Importers.loadNetwork(minioAdapter.getFileNameFromUrl(hourlyRaoResult.getNetworkWithPraUrl()), cgmInputStream);
        }
        MemDataSource memDataSource = new MemDataSource();

        // work around until the problem of "Too many loads connected to this bus" is corrected
        removeVirtualLoadsFromNetwork(network);
        // work around until the problem of "Too many generators connected to this bus" is corrected
        removeAlegroVirtualGeneratorsFromNetwork(network);
        // work around until fictitious loads and generators are not created in groovy script anymore
        removeFictitiousGeneratorsFromNetwork(network);
        removeFictitiousLoadsFromNetwork(network);
        Exporters.export("UCTE", network, new Properties(), memDataSource);
        String networkNewFileName = OutputFileNameUtil.generateUctFileName(hourlyRaoResult.getInstant(), raoIntegrationTask.getVersion());
        File targetFile = new File(raoIntegrationTask.getDailyOutputs().getNetworkTmpOutputsPath(), networkNewFileName); //NOSONAR

        try (InputStream is = memDataSource.newInputStream("", "uct")) {
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void removeVirtualLoadsFromNetwork(Network network) {
        List<String> virtualLoadsList = new ArrayList<>();
        network.getSubstationStream().forEach(substation -> substation.getVoltageLevels()
                .forEach(voltageLevel -> voltageLevel.getBusBreakerView().getBuses()
                        .forEach(bus -> bus.getLoadStream().filter(busLoad -> busLoad.getNameOrId().contains("_virtualLoad")).forEach(virtualLoad -> virtualLoadsList.add(virtualLoad.getNameOrId()))
                        )));
        virtualLoadsList.forEach(virtualLoad -> network.getLoad(virtualLoad).remove());
    }

    private void removeAlegroVirtualGeneratorsFromNetwork(Network network) {
        Optional.ofNullable(network.getGenerator(ALEGRO_GEN_BE)).ifPresent(Generator::remove);
        Optional.ofNullable(network.getGenerator(ALEGRO_GEN_DE)).ifPresent(Generator::remove);
    }

    private void removeFictitiousGeneratorsFromNetwork(Network network) {
        Set<String> generatorsToRemove = network.getGeneratorStream().filter(Generator::isFictitious).map(Generator::getId).collect(Collectors.toSet());
        generatorsToRemove.forEach(id -> network.getGenerator(id).remove());
    }

    private void removeFictitiousLoadsFromNetwork(Network network) {
        Set<String> loadsToRemove = network.getLoadStream().filter(Load::isFictitious).map(Load::getId).collect(Collectors.toSet());
        loadsToRemove.forEach(id -> network.getLoad(id).remove());
    }

    Crac importCracFromHourlyRaoRequest(RaoIntegrationTask raoIntegrationTask, HourlyRaoResult raoResult) {
        HourlyRaoRequest hourlyRaoRequest = raoIntegrationTask.getHourlyRequestFromResponse(raoResult);
        String cracFileUrl = hourlyRaoRequest.getCracFileUrl();
        try (InputStream cracFileInputStream = minioAdapter.getInputStreamFromUrl(cracFileUrl)) {
            return CracImporters.importCrac(minioAdapter.getFileNameFromUrl(cracFileUrl), cracFileInputStream);
        } catch (Exception e) {
            throw new RaoIntegrationException(String.format("Exception occurred while importing CRAC file: %s. Cause: %s", minioAdapter.getFileNameFromUrl(cracFileUrl), e.getMessage()));
        }
    }

    public void exportCneInTmpOutput(RaoIntegrationTask raoIntegrationTask, HourlyRaoResult hourlyRaoResult) throws IOException {
        LOGGER.info("RAO integration task: '{}', creating CNE Result for timestamp: '{}'", raoIntegrationTask.getTaskId(), hourlyRaoResult.getInstant());
        //create CNE with input from inputNetwork, outputCracJson and inputCraxXml
        HourlyRaoRequest hourlyRaoRequest = raoIntegrationTask.getHourlyRaoRequests().stream().filter(request -> request.getInstant().equals(hourlyRaoResult.getInstant()))
                .findFirst().orElseThrow(() -> new RaoIntegrationException(String.format("Exception occurred while creating CNE file for timestamp %s. Cause: no rao result.", hourlyRaoResult.getInstant())));

        //get input network
        String networkFileUrl = hourlyRaoRequest.getNetworkFileUrl();
        Network network;
        try (InputStream networkInputStream = minioAdapter.getInputStreamFromUrl(networkFileUrl)) {
            network = Importers.loadNetwork(minioAdapter.getFileNameFromUrl(networkFileUrl), networkInputStream);
        }

        //import input crac xml file and get FbConstraintCreationContext
        String cracXmlFileUrl = raoIntegrationTask.getInputCracXmlFileUrl();
        FbConstraintCreationContext fbConstraintCreationContext;
        try (InputStream cracInputStream = minioAdapter.getInputStreamFromUrl(cracXmlFileUrl)) {
            fbConstraintCreationContext = CracHelper.importCracXmlGetFbInfoWithNetwork(hourlyRaoResult.getInstant(), network, cracInputStream);
        }

        //get crac from hourly inputs
        Crac cracJson = importCracFromHourlyRaoRequest(raoIntegrationTask, hourlyRaoResult);

        //get raoResult from result
        RaoResult raoResult;
        try (InputStream raoResultInputStream = minioAdapter.getInputStreamFromUrl(hourlyRaoResult.getRaoResultFileUrl())) {
            RaoResultImporter raoResultImporter = new RaoResultImporter();
            raoResult = raoResultImporter.importRaoResult(raoResultInputStream, cracJson);
        }
        //get raoParams from input
        RaoParameters raoParameters;
        try (InputStream raoParametersInputStream = minioAdapter.getInputStreamFromUrl(hourlyRaoRequest.getRaoParametersFileUrl())) {
            raoParameters = JsonRaoParameters.read(raoParametersInputStream);
        }

        //export CNE
        String cneNewFileName = OutputFileNameUtil.generateCneFileName(hourlyRaoResult.getInstant(), raoIntegrationTask);
        File targetFile = new File(raoIntegrationTask.getDailyOutputs().getCneTmpOutputsPath(), cneNewFileName); //NOSONAR

        try (FileOutputStream outputStreamCne = new FileOutputStream(targetFile)) {
            CoreCneExporter standardCneExporter = new CoreCneExporter();
            CoreCneExporterParameters cneExporterParameters = getCneExporterParameters(raoIntegrationTask);
            standardCneExporter.exportCne(cracJson, network, fbConstraintCreationContext, raoResult, raoParameters, cneExporterParameters, outputStreamCne);

            //remember mrid f299 for f305 rao response payload
            hourlyRaoResult.setCneResultDocumentId(cneExporterParameters.getDocumentId());
        }
    }

    private CoreCneExporterParameters getCneExporterParameters(RaoIntegrationTask raoIntegrationTask) {
        return new CoreCneExporterParameters(
                generateCneMRID(raoIntegrationTask),
                raoIntegrationTask.getVersion(),
                DOMAIN_ID,
                CoreCneExporterParameters.ProcessType.DAY_AHEAD_CC,
                RaoIXmlResponseGenerator.SENDER_ID,
                CoreCneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR,
                RaoIXmlResponseGenerator.RECEIVER_ID,
                CoreCneExporterParameters.RoleType.CAPACITY_COORDINATOR,
                raoIntegrationTask.getTimeInterval()
        );
    }

    private String generateCneMRID(RaoIntegrationTask raoIntegrationTask) {
        return String.format("%s-%s-F299v%s", RaoIXmlResponseGenerator.SENDER_ID, IntervalUtil.getFormattedBusinessDay(raoIntegrationTask.getTimeInterval()), raoIntegrationTask.getVersion());
    }

    public void exportMetadataFile(RaoIntegrationTask raoIntegrationTask, String targetMinioFolder, boolean isManualRun) {
        try {
            new RaoIMetadataGenerator(minioAdapter).exportMetadataFile(raoIntegrationTask, targetMinioFolder, isManualRun);
        } catch (Exception e) {
            LOGGER.error("Could not generate metadata file for rao task {}: {}", raoIntegrationTask.getTaskId(), e.getMessage());
        }
    }
}
