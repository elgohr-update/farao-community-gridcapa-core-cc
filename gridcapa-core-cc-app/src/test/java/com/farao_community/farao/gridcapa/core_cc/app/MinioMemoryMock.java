/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app;

import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.configuration.MinioConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A MinioAdapter mock that saves uploaded info in memory for testing
 */
public class MinioMemoryMock extends MinioAdapter {
    private Map<String, String> filePathsAndContents;

    public MinioMemoryMock() {
        super(Mockito.mock(MinioConfiguration.class), null, null);
        this.filePathsAndContents = new HashMap<>();
    }

    @Override
    public InputStream getInputStreamFromUrl(String url) {
        try {
            return FileUtils.openInputStream(new File(url));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getFileNameFromUrl(String stringUrl) {
        return stringUrl;
    }

    @Override
    public void uploadFile(String pathDestination, InputStream sourceInputStream) {
        try {
            filePathsAndContents.put(pathDestination, new String(sourceInputStream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            Assert.fail("Failed to read input stream");
        }
    }

    @Override
    public void uploadFile(String bucketName, String pathDestination, InputStream sourceInputStream) {
        uploadFile(pathDestination, sourceInputStream);
    }

    @Override
    public String getOutputsBucket() {
        return "";
    }

    @Override
    public String generatePreSignedUrl(String minioPath) {
        return "presigned:" + minioPath;
    }

    public String getFileContents(String filePath) {
        return filePathsAndContents.get(filePath);
    }
}
