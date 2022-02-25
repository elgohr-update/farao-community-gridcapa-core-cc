/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.messaging;

import com.farao_community.farao.gridcapa.core_cc.app.messaging.configuration.MinioConfiguration;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class MinioConfigurationTest {

    @Autowired
    private MinioConfiguration minioConfiguration;

    @Test
    void checkMinioConfiguration() {
        assertNotNull(minioConfiguration);
        MinioClient minioClient = minioConfiguration.generateMinioClient();
        assertNotNull(minioClient);
    }
}
