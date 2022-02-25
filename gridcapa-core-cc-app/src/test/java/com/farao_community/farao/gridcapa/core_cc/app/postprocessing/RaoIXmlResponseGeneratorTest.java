/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.postprocessing;

import com.farao_community.farao.gridcapa.core_cc.app.TaskUtils;
import com.farao_community.farao.gridcapa.core_cc.app.entities.DailyOutputs;
import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoResult;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.entities.TaskStatus;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import com.farao_community.farao.gridcapa.core_cc.app.outputs.rao_response.HeaderType;
import com.farao_community.farao.gridcapa.core_cc.app.outputs.rao_response.PayloadType;
import com.farao_community.farao.gridcapa.core_cc.app.outputs.rao_response.ResponseMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class RaoIXmlResponseGeneratorTest {

    @Autowired
    RaoIXmlResponseGenerator raoIXmlResponseGenerator;

    @Autowired
    MinioAdapter minioAdapter;

    @BeforeEach
    public void setUp() {
        minioAdapter = Mockito.mock(MinioAdapter.class);
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(minioAdapter).uploadFile(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void generateRaoResponseHeaderTest() {
        ResponseMessageType responseMessage = new ResponseMessageType();
        RaoIntegrationTask task = new RaoIntegrationTask();
        task.setTimeInterval("2019-01-07T23:00Z/2019-01-08T23:00Z");
        TaskUtils.setTaskId(task, 1L);
        task.setCorrelationId("correlationId");
        try {
            raoIXmlResponseGenerator.generateRaoResponseHeader(task, responseMessage);
            HeaderType header = responseMessage.getHeader();
            assertEquals("correlationId", header.getCorrelationID());
            assertEquals("created", header.getVerb());
            assertEquals("OptimizedRemedialActions", header.getNoun());
            assertEquals("PRODUCTION", header.getContext());
            assertEquals("0", header.getRevision());
            assertEquals("22XCORESO------S", header.getSource());
            assertEquals("22XCORESO------S-20190108-F305", header.getMessageID());
        } catch (DatatypeConfigurationException e) {
            // should not happen
        }
    }

    @Test
    void generateRaoResponsePayloadTest() {
        ResponseMessageType responseMessage = new ResponseMessageType();
        RaoIntegrationTask task = new RaoIntegrationTask();
        TaskUtils.setTaskId(task, 1L);
        task.setTimeInterval("2020-03-29T22:00Z/2020-03-30T22:00Z");
        Set<HourlyRaoResult> hourlyArtifacts = new HashSet<>();

        HourlyRaoResult hourlyArtifact1 = new HourlyRaoResult();
        hourlyArtifact1.setInstant("2020-03-29T23:00:00Z");
        hourlyArtifact1.setStatus(TaskStatus.RUNNING);
        hourlyArtifacts.add(hourlyArtifact1);

        HourlyRaoResult hourlyArtifact2 = new HourlyRaoResult();
        hourlyArtifact2.setInstant("2020-03-30T00:00:00Z");
        hourlyArtifact2.setStatus(TaskStatus.ERROR);
        hourlyArtifacts.add(hourlyArtifact2);

        HourlyRaoResult hourlyArtifact3 = new HourlyRaoResult();
        hourlyArtifact3.setInstant("2020-03-30T01:00:00Z");
        hourlyArtifact3.setStatus(TaskStatus.SUCCESS);
        hourlyArtifacts.add(hourlyArtifact3);

        task.setHourlyRaoResults(hourlyArtifacts);

        raoIXmlResponseGenerator.generateRaoResponsePayLoad(task, responseMessage);
        PayloadType payload = responseMessage.getPayload();
        assertEquals(3, payload.getResponseItems().getResponseItem().size());
        assertEquals("2020-03-29T23:00Z/2020-03-30T00:00Z", payload.getResponseItems().getResponseItem().get(0).getTimeInterval());
        assertEquals("2020-03-30T00:00Z/2020-03-30T01:00Z", payload.getResponseItems().getResponseItem().get(1).getTimeInterval());
        assertEquals("2020-03-30T01:00Z/2020-03-30T02:00Z", payload.getResponseItems().getResponseItem().get(2).getTimeInterval());

        assertEquals("INFORM", payload.getResponseItems().getResponseItem().get(0).getError().getLevel());
        assertNull(payload.getResponseItems().getResponseItem().get(0).getFiles());

        assertEquals("FATAL", payload.getResponseItems().getResponseItem().get(1).getError().getLevel());
        assertNull(payload.getResponseItems().getResponseItem().get(1).getFiles());

        assertNull(payload.getResponseItems().getResponseItem().get(2).getError());
        assertEquals(3, payload.getResponseItems().getResponseItem().get(2).getFiles().getFile().size());

        assertEquals("OPTIMIZED_CGM", payload.getResponseItems().getResponseItem().get(2).getFiles().getFile().get(0).getCode());
        assertEquals("OPTIMIZED_CB", payload.getResponseItems().getResponseItem().get(2).getFiles().getFile().get(1).getCode());
        assertEquals("RAO_REPORT", payload.getResponseItems().getResponseItem().get(2).getFiles().getFile().get(2).getCode());
    }

    @Test
    void generateCgmXmlHeaderFileTest() throws IOException {
        RaoIntegrationTask task = new RaoIntegrationTask();
        task.setTimeInterval("2020-03-29T22:00Z/2020-03-30T22:00Z");
        task.setVersion(1);
        task.setCorrelationId("corr-id");

        HourlyRaoResult hourlyRaoResult1 = new HourlyRaoResult();
        hourlyRaoResult1.setInstant("2020-03-30T01:00:00Z");
        hourlyRaoResult1.setStatus(TaskStatus.SUCCESS);
        HourlyRaoResult hourlyRaoResult2 = new HourlyRaoResult();
        hourlyRaoResult2.setInstant("2020-03-30T03:00:00Z");
        hourlyRaoResult2.setStatus(TaskStatus.SUCCESS);
        Set<HourlyRaoResult> hourlyRaoResults = new HashSet<>();
        hourlyRaoResults.add(hourlyRaoResult1);
        hourlyRaoResults.add(hourlyRaoResult2);
        task.setHourlyRaoResults(hourlyRaoResults);
        DailyOutputs dailyOutputs = new DailyOutputs();
        task.setDailyOutputs(dailyOutputs);
        raoIXmlResponseGenerator.generateCgmXmlHeaderFile(task, task.getDailyOutputs().getNetworkTmpOutputsPath());
        Path cgmsPath = Files.find(Paths.get(task.getDailyOutputs().getNetworkTmpOutputsPath()), 1, (p, a) -> p.getFileName().toString().contains(".xml")).findFirst().get();
        assertEquals("CGM_XML_Header.xml", cgmsPath.getFileName().toString());
    }

}
