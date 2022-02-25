/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.preprocessing;

import com.farao_community.farao.gridcapa.core_cc.app.MinioMemoryMock;
import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoRequest;
import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoResult;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.entities.TaskStatus;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
public class RaoIPreProcessServiceTest {

    @Autowired
    RaoIPreProcessService raoIPreprocessService;

    @MockBean
    MinioAdapter minioAdapter;

    @Test
    void initTaskSuccessfullyTest() throws IOException {
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("MockUrl");
        RaoIntegrationTask task = new RaoIntegrationTask();
        InputStream inputsArchiveInputStream = getClass().getResourceAsStream("/preprocessing/test3TS.zip");
        MultipartFile multipartFileArchive = new MockMultipartFile("test3TS",
                "test3TS.zip", "application/octet-stream", IOUtils.toByteArray(inputsArchiveInputStream));
        raoIPreprocessService.initializeTaskFromZipArchive(task, multipartFileArchive);
        Assertions.assertEquals("e880ff7e-8d81-4f89-86b0-e3276e9d9476", task.getCorrelationId());
        Assertions.assertEquals(3, task.getHourlyRaoRequests().size());
        Assertions.assertEquals(0, task.getHourlyRaoResults().size());
        HourlyRaoRequest hourlyRaoRequest = task.getHourlyRaoRequests().iterator().next();
        Assertions.assertEquals("2020-03-29T22:00:00Z", hourlyRaoRequest.getInstant());
        Assertions.assertEquals("MockUrl", hourlyRaoRequest.getCracFileUrl());
        Assertions.assertEquals("MockUrl", hourlyRaoRequest.getNetworkFileUrl());
        Assertions.assertEquals("MockUrl", hourlyRaoRequest.getRaoParametersFileUrl());
        Assertions.assertEquals("MockUrl", hourlyRaoRequest.getRealGlskFileUrl());
        Assertions.assertEquals("MockUrl", hourlyRaoRequest.getRefprogFileUrl());
        Assertions.assertEquals("0/hourly_rao_results/20200329_2200", hourlyRaoRequest.getResultsDestination());
    }

    @Test
    void initTaskWithFailureTest() throws IOException {
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("MockUrl");
        RaoIntegrationTask task = new RaoIntegrationTask();
        InputStream inputsArchiveInputStream = getClass().getResourceAsStream("/preprocessing/test3TS_Error.zip");
        MultipartFile multipartFileArchive = new MockMultipartFile("test3TS_Error",
                "test3TS_Error.zip", "application/octet-stream", IOUtils.toByteArray(inputsArchiveInputStream));
        raoIPreprocessService.initializeTaskFromZipArchive(task, multipartFileArchive);
        Assertions.assertEquals(3, task.getHourlyRaoRequests().size());
        HourlyRaoRequest hourlyRaoRequest = task.getHourlyRaoRequests().iterator().next();
        Assertions.assertEquals("2020-03-29T22:00:00Z", hourlyRaoRequest.getInstant());

        Assertions.assertEquals(3, task.getHourlyRaoResults().size());
        HourlyRaoResult hourlyRaoResult = task.getHourlyRaoResults().iterator().next();
        Assertions.assertEquals("2020-03-29T22:00:00Z", hourlyRaoResult.getInstant());
        Assertions.assertEquals(TaskStatus.ERROR, hourlyRaoResult.getStatus());
        Assertions.assertEquals("Please check the naming format of Refprog. No match with: ^[0-9]{8}-F120-v[0-9]-17XTSO-CS------W-to-22XCORESO------S.xml", hourlyRaoResult.getErrorMessage());

    }

    @Test
    void initTaskWithPartialErrorTest() throws IOException {
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("MockUrl");
        RaoIntegrationTask task = new RaoIntegrationTask();
        InputStream inputsArchiveInputStream = getClass().getResourceAsStream("/preprocessing/test3TS_Partial_Error.zip");
        MultipartFile multipartFileArchive = new MockMultipartFile("test3TS_Partial_Error",
                "test3TS_Partial_Error.zip", "application/octet-stream", IOUtils.toByteArray(inputsArchiveInputStream));
        raoIPreprocessService.initializeTaskFromZipArchive(task, multipartFileArchive);
        Assertions.assertEquals(3, task.getHourlyRaoRequests().size());
        Assertions.assertEquals(1, task.getHourlyRaoResults().size());
        HourlyRaoResult hourlyRaoResult = task.getHourlyRaoResults().iterator().next();
        Assertions.assertEquals("2020-03-29T23:00:00Z", hourlyRaoResult.getInstant());
        Assertions.assertEquals(TaskStatus.ERROR, hourlyRaoResult.getStatus());
        Assertions.assertEquals("Error occurred while trying to import inputs at timestamp: 2020-03-29T23:00:00Z. Origin cause : Please check the naming format of the CGMs. No match with: 20200330_0030_2D1_UC5_F100_CORESO.uct", hourlyRaoResult.getErrorMessage());
    }

    @Test
    void testRaoRequestAcknowledgment() throws IOException {
        MinioMemoryMock minioAdapterMock = new MinioMemoryMock();
        RaoIntegrationTask task = new RaoIntegrationTask();
        InputStream inputsArchiveInputStream = getClass().getResourceAsStream("/preprocessing/test3TS.zip");
        MultipartFile multipartFileArchive = new MockMultipartFile("test3TS",
                "test3TS.zip", "application/octet-stream", IOUtils.toByteArray(inputsArchiveInputStream));
        new RaoIPreProcessService(minioAdapterMock, Mockito.mock(RaoParametersService.class)).initializeTaskFromZipArchive(task, multipartFileArchive);

        Assertions.assertEquals("0/outputs/CASTOR-RAO_22VCOR0CORE0TST4_RTE-F302-ACK_20200330-F302-01.xml", task.getDailyOutputs().getRaoRequestAckPath());
        String expectedFileContents = new String(getClass().getResourceAsStream("/preprocessing/RaoRequestACK.xml").readAllBytes()).replace("\r", "");
        Assertions.assertEquals(expectedFileContents, minioAdapterMock.getFileContents(task.getDailyOutputs().getRaoRequestAckPath()));
    }
}
