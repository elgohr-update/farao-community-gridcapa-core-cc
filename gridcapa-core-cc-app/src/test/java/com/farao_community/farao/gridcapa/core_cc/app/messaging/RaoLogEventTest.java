/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.messaging;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoLogEventTest {

    @Test
    public void testToString() {
        RaoLogEvent event = new RaoLogEvent("taskId", "2022-01-13T10:20:59.466+01:00", "INFO", "some message");
        assertEquals("2022-01-13T09:20:59.4660Z INFO - some message", event.toString());
    }
}
