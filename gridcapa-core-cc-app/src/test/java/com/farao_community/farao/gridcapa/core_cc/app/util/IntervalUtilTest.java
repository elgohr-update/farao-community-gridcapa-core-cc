/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.util;

import static org.junit.jupiter.api.Assertions.*;

import com.farao_community.farao.gridcapa.core_cc.app.exceptions.RaoIntegrationException;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class IntervalUtilTest {

    @Test
    public void getPositionMApShouldReturn24Position() {
        Map<Integer, Interval> map = IntervalUtil.getPositionsMap("2020-08-10T22:00Z/2020-08-11T22:00Z");
        assertEquals(24, map.size());
    }

    @Test
    public void getInstantPositionShouldReturn1() {
        String instant = "2020-08-10T22:59:00Z";
        String interval = "2020-08-10T22:00Z/2020-08-11T22:00Z";
        assertEquals(1, IntervalUtil.getInstantPosition(instant, interval));
    }

    @Test
    public void getInstantPositionShouldReturn24() {
        String instant = "2020-08-11T21:59:00Z";
        String interval = "2020-08-10T22:00Z/2020-08-11T22:00Z";
        assertEquals(24, IntervalUtil.getInstantPosition(instant, interval));
    }

    @Test
    public void getInstantPositionShouldThrowException() {
        String instant = "2020-08-11T22:00:00Z";
        String interval = "2020-08-10T22:00Z/2020-08-11T22:00Z";
        assertThrows(RaoIntegrationException.class, () -> IntervalUtil.getInstantPosition(instant, interval));
    }

    @Test
    public void isInTimeIntervalTest() {
        String instant = "2020-08-10T22:00:00Z";
        String interval = "2020-08-10T22:00Z/2020-08-11T22:00Z";
        assertTrue(IntervalUtil.isInTimeInterval(OffsetDateTime.parse(instant), interval));
    }
}
