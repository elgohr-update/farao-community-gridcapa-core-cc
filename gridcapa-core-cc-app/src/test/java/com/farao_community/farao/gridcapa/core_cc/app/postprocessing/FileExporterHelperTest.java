/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.postprocessing;

import com.farao_community.farao.data.core_cne_exporter.xsd.CriticalNetworkElementMarketDocument;
import com.farao_community.farao.gridcapa.core_cc.app.TaskUtils;
import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoRequest;
import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoResult;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.entities.TaskStatus;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import com.farao_community.farao.gridcapa.core_cc.app.util.CoreNetworkImporterWrapper;
import com.farao_community.farao.gridcapa.core_cc.app.util.JaxbUtil;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FileExporterHelperTest {

    @Autowired
    FileExporterHelper fileExporterHelper;

    @MockBean
    MinioAdapter minioAdapter;

    @BeforeEach
    public void setUp() {
        Mockito.when(minioAdapter.getFileNameFromUrl(Mockito.any())).thenCallRealMethod();
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void exportCneTest() throws IOException {
        InputStream inputCracXmlInputStream = getClass().getResourceAsStream("/postprocessing/post_processing_cne/F301.xml");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/inputCracXml.xml")).thenReturn(inputCracXmlInputStream);
        InputStream crac1InputStream = getClass().getResourceAsStream("/postprocessing/post_processing_cne/crac.json");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/crac1.json")).thenReturn(crac1InputStream);
        InputStream networkInputStream = getClass().getResourceAsStream("/postprocessing/post_processing_cne/network.xiidm");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/network1.xiidm")).thenReturn(networkInputStream);
        InputStream raoParamsInputStream = getClass().getResourceAsStream("/postprocessing/post_processing_cne/rao_params.json");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/rao_params.json")).thenReturn(raoParamsInputStream);
        InputStream raoResult1InputStream = getClass().getResourceAsStream("/postprocessing/post_processing_cne/raoResult.json");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/raoResult1.json")).thenReturn(raoResult1InputStream);

        RaoIntegrationTask task = new RaoIntegrationTask();
        TaskUtils.setTaskId(task, 2L);
        task.setVersion(6);
        task.setTimeInterval("2019-01-07T23:00Z/2019-01-08T23:00Z");
        task.setInputCracXmlFileUrl("http://host:9000/inputCracXml.xml");
        Set<HourlyRaoRequest> hourlyInputs = new HashSet<>();

        HourlyRaoRequest hourlyInput1 = new HourlyRaoRequest();
        hourlyInput1.setInstant("2019-01-08T12:30:00Z");
        hourlyInput1.setCracFileUrl("http://host:9000/crac1.json");
        hourlyInput1.setNetworkFileUrl("http://host:9000/network1.xiidm");
        hourlyInput1.setRaoParametersFileUrl("http://host:9000/rao_params.json");
        hourlyInputs.add(hourlyInput1);

        task.setHourlyRaoRequests(hourlyInputs);

        Set<HourlyRaoResult> hourlyResults = new HashSet<>();
        HourlyRaoResult hourlyRaoResult1 = new HourlyRaoResult();
        hourlyRaoResult1.setInstant("2019-01-08T12:30:00Z");
        hourlyRaoResult1.setStatus(TaskStatus.SUCCESS);
        hourlyRaoResult1.setRaoResultFileUrl("http://host:9000/raoResult1.json");
        hourlyResults.add(hourlyRaoResult1);
        task.setHourlyRaoResults(hourlyResults);
        fileExporterHelper.exportCneInTmpOutput(task, hourlyRaoResult1);
        Path cnePath = Files.find(Paths.get(task.getDailyOutputs().getCneTmpOutputsPath()), 1, (p, a) -> p.getFileName().toString().contains(".xml")).findFirst().get();
        assertEquals("20190108_1330_20190108-F299-v6-22XCORESO------S_to_17XTSO-CS------W.xml", cnePath.getFileName().toString());

        try (FileInputStream inputStreamCne = new FileInputStream(cnePath.toString())) {
            CriticalNetworkElementMarketDocument cneFile = JaxbUtil.unmarshalContent(CriticalNetworkElementMarketDocument.class, inputStreamCne);
            assertEquals("22XCORESO------S-20190108-F299v6", cneFile.getMRID());
            assertEquals("6", cneFile.getRevisionNumber());
            assertEquals("10Y1001C--00059P", cneFile.getDomainMRID().getValue());
            assertEquals("A48", cneFile.getProcessProcessType());
            assertEquals("A44", cneFile.getSenderMarketParticipantMarketRoleType());
            assertEquals("22XCORESO------S", cneFile.getSenderMarketParticipantMRID().getValue());
            assertEquals("A36", cneFile.getReceiverMarketParticipantMarketRoleType());
            assertEquals("17XTSO-CS------W", cneFile.getReceiverMarketParticipantMRID().getValue());
            assertEquals("2019-01-07T23:00Z", cneFile.getTimePeriodTimeInterval().getStart());
            assertEquals("2019-01-08T23:00Z", cneFile.getTimePeriodTimeInterval().getEnd());
        } catch (Exception e) {
            fail("Failed to read generated CNE file");
        }
    }

    @Test
    void exportNetworkTest() throws IOException {
        InputStream network1InputStream = getClass().getResourceAsStream("/postprocessing/post_processing_networks/network_with_virtual_hubs.xiidm");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/network1.xiidm")).thenReturn(network1InputStream);

        RaoIntegrationTask task = new RaoIntegrationTask();
        TaskUtils.setTaskId(task, 1L);
        task.setVersion(1);
        Set<HourlyRaoRequest> hourlyInputs = new HashSet<>();

        HourlyRaoRequest hourlyInput1 = new HourlyRaoRequest();
        hourlyInput1.setInstant("2020-03-30T02:00:00Z");
        hourlyInputs.add(hourlyInput1);

        task.setHourlyRaoRequests(hourlyInputs);

        Set<HourlyRaoResult> hourlyResults = new HashSet<>();
        HourlyRaoResult hourlyRaoResult1 = new HourlyRaoResult();
        hourlyRaoResult1.setInstant("2020-03-30T02:00:00Z");
        hourlyRaoResult1.setStatus(TaskStatus.SUCCESS);
        hourlyRaoResult1.setNetworkWithPraUrl("http://host:9000/network1.xiidm");

        hourlyResults.add(hourlyRaoResult1);
        task.setHourlyRaoResults(hourlyResults);

        fileExporterHelper.exportNetworkInTmpOutput(task, hourlyRaoResult1);
        Path uctCgmPath = Files.find(Paths.get(task.getDailyOutputs().getNetworkTmpOutputsPath()), 1, (p, a) -> p.getFileName().toString().contains(".uct")).findFirst().get();
        assertEquals("20200330_0430_2D1_UX1.uct", uctCgmPath.getFileName().toString());
        Network networkAfterPostTreatment = CoreNetworkImporterWrapper.loadNetwork(uctCgmPath);

        assertTrue(networkAfterPostTreatment.getLoadStream().noneMatch(load -> load.getId().contains("_virtualLoad")));
        assertTrue(networkAfterPostTreatment.getLoadStream().noneMatch(Identifiable::isFictitious));
        assertNull(networkAfterPostTreatment.getGenerator("XLI_OB1A"));
        assertNull(networkAfterPostTreatment.getGenerator("XLI_OB1B"));
    }
}
