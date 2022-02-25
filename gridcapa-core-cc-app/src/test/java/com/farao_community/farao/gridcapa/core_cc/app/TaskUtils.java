/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app;

import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;

import java.lang.reflect.Field;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class TaskUtils {

    public static void setTaskId(RaoIntegrationTask task, long id) {
        try {
            Field idField = task.getClass().getDeclaredField("taskId");
            idField.setAccessible(true);
            idField.set(task, id);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            // Should not happen
        }
    }
}
