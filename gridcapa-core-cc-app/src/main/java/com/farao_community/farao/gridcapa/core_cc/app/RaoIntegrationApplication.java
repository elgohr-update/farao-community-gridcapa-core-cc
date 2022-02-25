/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app;

import com.farao_community.farao.gridcapa.core_cc.app.messaging.RaoIntegrationInputsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.farao_community.*"})
@EntityScan(basePackages = {"com.farao_community.*"})
@EnableConfigurationProperties(RaoIntegrationInputsProperties.class)
public class RaoIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaoIntegrationApplication.class, args);
    }

}
