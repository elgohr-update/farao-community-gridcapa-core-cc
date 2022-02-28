/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.messaging;

import io.minio.MinioClient;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class MinioAdapterTest {
    @Autowired
    private MinioAdapter minioAdapter;

    @MockBean
    private MinioClient minioClient;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(minioClient);
    }

    @Test
    void checkUploadFile() throws Exception {
        minioAdapter.uploadFile("file/path", new ByteArrayInputStream("File content".getBytes()));
        Mockito.verify(minioClient, Mockito.times(1)).putObject(Mockito.any());
    }

    @Test
    @Ignore
    void checkGetPresignedObjectUrl() throws Exception {
        Mockito.when(minioClient.getPresignedObjectUrl(Mockito.any())).thenReturn("http://url");
        String url = minioAdapter.generatePreSignedUrl("file/path");
        Mockito.verify(minioClient, Mockito.times(1)).getPresignedObjectUrl(Mockito.any());
        assertEquals("http://url", url);
    }

    @Test
    void checkFileNameReturnedCorrectlyFromUrl() {
        String stringUrl = "http://localhost:9000/rao-integration-data/181/outputs/20200330-F305-v0-22XCORESO------S-to-17XTSO-CS------W.xml?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20210218%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20210218T160811Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=61e252359e5cb6ff99a06e9c04b6b4191554b9d427ac6d7a6ad65423a67c7434";
        assertEquals("20200330-F305-v0-22XCORESO------S-to-17XTSO-CS------W.xml", minioAdapter.getFileNameFromUrl(stringUrl));
    }

}

