/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ApplicationStartupConfig implements ApplicationListener<ApplicationReadyEvent> {

    private final Environment springEnvironment;

    private static String taskTempOutputsDir;

    public ApplicationStartupConfig(Environment springEnvironment) {
        this.springEnvironment = springEnvironment;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        taskTempOutputsDir = springEnvironment.getProperty("rao-integration.filesystem.tmp-output-directory");
    }

    public static String getTaskTempOutputsDir() {
        return taskTempOutputsDir != null ? taskTempOutputsDir : "/tmp/tmp-outputs"; // workaround for unit tests
    }
}
