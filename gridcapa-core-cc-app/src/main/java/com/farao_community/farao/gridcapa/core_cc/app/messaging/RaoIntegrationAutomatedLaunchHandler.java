/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.messaging;

import com.farao_community.farao.gridcapa.core_cc.app.RaoIntegrationService;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationRepository;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.exceptions.RaoIntegrationException;
import com.farao_community.farao.gridcapa.core_cc.app.preprocessing.RaoIPreProcessService;
import com.farao_community.farao.gridcapa.core_cc.app.util.ZipUtil;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class RaoIntegrationAutomatedLaunchHandler {

    private final MinioAdapter minioAdapter;
    private final RaoIntegrationRepository repository;
    private final RaoIPreProcessService preprocessService;
    private final RaoIntegrationService raoIntegrationService;

    public RaoIntegrationAutomatedLaunchHandler(MinioAdapter minioAdapter, RaoIntegrationRepository repository, RaoIPreProcessService preprocessService, RaoIntegrationService raoIntegrationService) {
        this.minioAdapter = minioAdapter;
        this.repository = repository;
        this.preprocessService = preprocessService;
        this.raoIntegrationService = raoIntegrationService;
    }

    public void handleProcessRequest(String targetMinioFolder, String raoRequestMinioObjectName, String cracMinioObjectName, String cgmsZipMinioObjectName, String virtualHubsMinioObjectName, String refProgPreSignedUrl, String glskPreSignedUrl) {
        try {
            minioAdapter.createBucketIfDoesNotExist();
            RaoIntegrationTask raoIntegrationTask = new RaoIntegrationTask();
            Path targetTempPath = Paths.get(raoIntegrationTask.getTmpInputsPath()); //NOSONAR
            Path raoRequestP = minioAdapter.copyFileInTargetSystemPath(raoRequestMinioObjectName, targetTempPath);
            Path cracP = minioAdapter.copyFileInTargetSystemPath(cracMinioObjectName, targetTempPath);
            Path cgmsZipP = minioAdapter.copyFileInTargetSystemPath(cgmsZipMinioObjectName, targetTempPath);
            Path cgmsUnZippedTempFolderPath = Paths.get(raoIntegrationTask.getTmpCgmInputsPath()); //NOSONAR
            ZipUtil.unzipFile(cgmsZipP, cgmsUnZippedTempFolderPath);
            ZipUtil.deletePath(cgmsZipP);
            Path virtualHubsP = minioAdapter.copyFileInTargetSystemPath(virtualHubsMinioObjectName, targetTempPath);

            String preSignedCracXmlUrl = minioAdapter.generatePreSignedUrl(cracMinioObjectName);
            String raoRequestFileName = minioAdapter.getFileNameFromUrl(preSignedCracXmlUrl);
            raoIntegrationTask.setRaoRequestFileName(raoRequestFileName);
            raoIntegrationTask.setInputCracXmlFileUrl(preSignedCracXmlUrl);
            repository.save(raoIntegrationTask);
            preprocessService.initializeTaskFromAutomatedLaunch(raoIntegrationTask, targetMinioFolder, raoRequestP, cracP, cgmsUnZippedTempFolderPath, virtualHubsP, refProgPreSignedUrl, glskPreSignedUrl);

            repository.save(raoIntegrationTask);
            raoIntegrationService.runTaskAsynchronouslyAutomatically(raoIntegrationTask, targetMinioFolder);
        } catch (Exception e) {
            throw new RaoIntegrationException("Exception occurred:", e);
        }
    }
}
