/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.postprocessing;

import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.FlowBasedConstraintDocument;

import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.entities.TaskStatus;
import com.farao_community.farao.gridcapa.core_cc.app.exceptions.RaoIntegrationException;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import com.farao_community.farao.gridcapa.core_cc.app.util.JaxbUtil;
import com.farao_community.farao.gridcapa.core_cc.app.util.ZipUtil;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Service
public class RaoIPostProcessService {

    private final MinioAdapter minioAdapter;
    private final RaoIXmlResponseGenerator raoIXmlResponseGenerator;
    private final DailyF303Generator dailyF303Generator;
    private final FileExporterHelper fileExporterHelper;

    public RaoIPostProcessService(MinioAdapter minioAdapter, RaoIXmlResponseGenerator raoIXmlResponseGenerator, DailyF303Generator dailyF303Generator, FileExporterHelper fileExporterHelper) {
        this.minioAdapter = minioAdapter;
        this.raoIXmlResponseGenerator = raoIXmlResponseGenerator;
        this.dailyF303Generator = dailyF303Generator;
        this.fileExporterHelper = fileExporterHelper;
    }

    public void postProcessHourlyResults(RaoIntegrationTask raoIntegrationTask, String targetMinioFolder, boolean isManualRun) {
        String outputsTargetMinioFolder = targetMinioFolder.replace("RAO_WORKING_DIR/", "RAO_OUTPUTS_DIR/");
        renameRaoHourlyResultsAndSendToDailyOutputs(raoIntegrationTask, outputsTargetMinioFolder, isManualRun);
        FlowBasedConstraintDocument dailyFlowBasedConstraintDocument = dailyF303Generator.generate(raoIntegrationTask);
        uploadDailyOutputFlowBasedConstraintDocument(raoIntegrationTask, dailyFlowBasedConstraintDocument, outputsTargetMinioFolder, isManualRun);
        raoIXmlResponseGenerator.generateRaoResponse(raoIntegrationTask, outputsTargetMinioFolder, isManualRun); //f305 rao response
        raoIntegrationTask.setOutputsSendingInstant(Instant.now());
        raoIntegrationTask.setTaskStatus(TaskStatus.SUCCESS); // status success should be set before exportMetadataFile because it's displayed within it
        fileExporterHelper.exportMetadataFile(raoIntegrationTask, outputsTargetMinioFolder, isManualRun);
    }

    public void renameRaoHourlyResultsAndSendToDailyOutputs(RaoIntegrationTask raoIntegrationTask, String targetMinioFolder, boolean isManualRun) {
        String networksArchiveTempPath = raoIntegrationTask.getDailyOutputs().getNetworkTmpOutputsPath();
        String cneArchiveTempPath = raoIntegrationTask.getDailyOutputs().getCneTmpOutputsPath();
        String logsArchiveTempPath = raoIntegrationTask.getDailyOutputs().getLogsTmpOutputPath();

        raoIXmlResponseGenerator.generateCgmXmlHeaderFile(raoIntegrationTask, networksArchiveTempPath); // add cgm xml header to networks folder

        // Zip CGMS
        byte[] cgmsZipResult = ZipUtil.zipDirectory(networksArchiveTempPath);
        String targetCgmsFolderName = OutputFileNameUtil.generateCgmZipName(raoIntegrationTask);
        String targetCgmsFolderPath = OutputFileNameUtil.generateOutputsDestinationPath(targetMinioFolder, targetCgmsFolderName);
        try (InputStream cgmZipIs = new ByteArrayInputStream(cgmsZipResult)) {
            minioAdapter.uploadFile(targetCgmsFolderPath, cgmZipIs);
            if (!isManualRun) {
                minioAdapter.copyObject(targetCgmsFolderPath, targetCgmsFolderName, minioAdapter.getDefaultBucket(), minioAdapter.getOutputsBucket());
            }
            raoIntegrationTask.getDailyOutputs().setCgmsZipPath(targetCgmsFolderPath);
        } catch (IOException e) {
            throw new RaoIntegrationException(String.format("Exception occurred while zipping CGMs of task %s", raoIntegrationTask.getTaskId()));
        } finally {
            ZipUtil.deletePath(Paths.get(networksArchiveTempPath)); //NOSONAR
        }

        // Zip CNE
        byte[] cneZipResult = ZipUtil.zipDirectory(cneArchiveTempPath);
        String targetCneFolderName = OutputFileNameUtil.generateCneZipName(raoIntegrationTask);
        String targetCneFolderPath = OutputFileNameUtil.generateOutputsDestinationPath(targetMinioFolder, targetCneFolderName);

        try (InputStream cneZipIs = new ByteArrayInputStream(cneZipResult)) {
            minioAdapter.uploadFile(targetCneFolderPath, cneZipIs);
            if (!isManualRun) {
                minioAdapter.copyObject(targetCneFolderPath, targetCneFolderName, minioAdapter.getDefaultBucket(), minioAdapter.getOutputsBucket());
            }
            raoIntegrationTask.getDailyOutputs().setCnesZipPath(targetCneFolderPath);
        } catch (IOException e) {
            throw new RaoIntegrationException(String.format("Exception occurred while zipping CNEs of task %s", raoIntegrationTask.getTaskId()));
        } finally {
            ZipUtil.deletePath(Paths.get(cneArchiveTempPath)); //NOSONAR
        }

        // Zip logs
        byte[] logsZipResult = ZipUtil.zipDirectory(logsArchiveTempPath);
        String targetLogsFolderName = OutputFileNameUtil.generateLogsZipName(raoIntegrationTask);
        String targetLogsFolderPath = OutputFileNameUtil.generateOutputsDestinationPath(targetMinioFolder, targetLogsFolderName);
        try (InputStream logsZipIs = new ByteArrayInputStream(logsZipResult)) {
            minioAdapter.uploadFile(targetLogsFolderPath, logsZipIs);
            if (!isManualRun) {
                minioAdapter.copyObject(targetLogsFolderPath, targetLogsFolderName, minioAdapter.getDefaultBucket(), minioAdapter.getOutputsBucket());
            }
            raoIntegrationTask.getDailyOutputs().setLogsZipPath(targetLogsFolderPath);
        } catch (IOException e) {
            throw new RaoIntegrationException(String.format("Exception occurred while zipping logs of task %s", raoIntegrationTask.getTaskId()));
        } finally {
            ZipUtil.deletePath(Paths.get(logsArchiveTempPath));
        }
    }

    void uploadDailyOutputFlowBasedConstraintDocument(RaoIntegrationTask raoIntegrationTask, FlowBasedConstraintDocument dailyFbDocument, String targetMinioFolder, boolean isManualRun) {
        byte[] dailyFbConstraint = JaxbUtil.writeInBytes(FlowBasedConstraintDocument.class, dailyFbDocument);
        String fbConstraintFileName = OutputFileNameUtil.generateOptimizedCbFileName(raoIntegrationTask);
        String fbConstraintDestinationPath = OutputFileNameUtil.generateOutputsDestinationPath(targetMinioFolder, fbConstraintFileName);

        try (InputStream dailyFbIs = new ByteArrayInputStream(dailyFbConstraint)) {
            minioAdapter.uploadFile(fbConstraintDestinationPath, dailyFbIs);
            if (!isManualRun) {
                minioAdapter.copyObject(fbConstraintDestinationPath, fbConstraintFileName, minioAdapter.getDefaultBucket(), minioAdapter.getOutputsBucket());
            }
        } catch (IOException e) {
            throw new RaoIntegrationException(String.format("Exception occurred while uploading F303 file of task %s", raoIntegrationTask.getTaskId()));
        }

        raoIntegrationTask.getDailyOutputs().setFlowBasedConstraintDocumentPath(fbConstraintDestinationPath);
    }

}
