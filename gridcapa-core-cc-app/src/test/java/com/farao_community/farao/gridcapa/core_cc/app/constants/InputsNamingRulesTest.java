/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.constants;

import org.junit.Test;

import static com.farao_community.farao.gridcapa.core_cc.app.constants.InputsNamingRules.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InputsNamingRulesTest {

    @Test
    public void testRaoRequestName() {
        assertTrue("20210612-F302-v2-17XTSO-CS------W-to-22XCORESO------S.xml".matches(RAO_REQUEST_FILE_NAME));
        assertTrue("20220213-F302-v4-17XTSO-CS------W-to-22XCORESO------S.xml".matches(RAO_REQUEST_FILE_NAME));
        assertFalse("20210612-F302-v2-17XTSO-CS------W-to-22XCORESO------S".matches(RAO_REQUEST_FILE_NAME));
        assertFalse("20210612-F302-17XTSO-CS------W-to-22XCORESO------S.xml".matches(RAO_REQUEST_FILE_NAME));
        assertFalse("2021061-F302-v2-17XTSO-CS------W-to-22XCORESO------S.xml".matches(RAO_REQUEST_FILE_NAME));
    }

    @Test
    public void testCgmZipName() {
        assertTrue("20210612-F119-v1-17XTSO-CS------W-to-22XCORESO------S.zip".matches(CGM_ZIP_FILE_NAME));
        assertTrue("20210814-F119-v2-17XTSO-CS------W-to-22XCORESO------S.zip".matches(CGM_ZIP_FILE_NAME));
        assertFalse("20210612-F119-v1-17XTSO-CS------W-to-22XCORESO------S".matches(CGM_ZIP_FILE_NAME));
        assertFalse("20210612-F119-17XTSO-CS------W-to-22XCORESO------S.zip".matches(CGM_ZIP_FILE_NAME));
        assertFalse("2021061-F1-19-v1-17XTSO-CS------W-to-22XCORESO------S.zip".matches(CGM_ZIP_FILE_NAME));
    }

    @Test
    public void testCgmXmlHeaderName() {
        assertTrue("CGM_XML_HEADER.xml".matches(CGM_XML_HEADER_NAME));
        assertTrue("CGM_XML_Header.xml".matches(CGM_XML_HEADER_NAME));
        assertFalse("CGM_XML_HEADER".matches(CGM_XML_HEADER_NAME));
        assertTrue("CGM_XML_HEAER.xml".matches(CGM_XML_HEADER_NAME));
        assertTrue("anything.xml".matches(CGM_XML_HEADER_NAME));
    }

    @Test
    public void testRealGlskName() {
        assertTrue("20210612-F319-v1-17XTSO-CS------W-to-22XCORESO------S.xml".matches(REAL_GLSK_FILE_NAME));
        assertTrue("20231210-F319-v5-17XTSO-CS------W-to-22XCORESO------S.xml".matches(REAL_GLSK_FILE_NAME));
        assertFalse("20210612-F319-v1-17XTSO-CS------W-to-22XCORESO------S".matches(REAL_GLSK_FILE_NAME));
        assertFalse("20210612-F319-17XTSO-CS------W-to-22XCORESO------S.xml".matches(REAL_GLSK_FILE_NAME));
        assertFalse("2021061-F319-v1-17XTSO-CS------W-to-22XCORESO------S.xml".matches(REAL_GLSK_FILE_NAME));
    }

    @Test
    public void testCracName() {
        assertTrue("20210612-F301-v2-17XTSO-CS------W-to-22XCORESO------S.xml".matches(CRAC_FILE_NAME));
        assertTrue("20210101-F301-v1-17XTSO-CS------W-to-22XCORESO------S.xml".matches(CRAC_FILE_NAME));
        assertFalse("20210612-F301-v2-17XTSO-CS------W-to-22XCORESO------S".matches(CRAC_FILE_NAME));
        assertFalse("20210612-F301-17XTSO-CS------W-to-22XCORESO------S.xml".matches(CRAC_FILE_NAME));
        assertFalse("2210612-F301-v2-17XTSO-CS------W-to-22XCORESO------S.xml".matches(CRAC_FILE_NAME));
    }

    @Test
    public void testRefProgName() {
        assertTrue("20210612-F120-v1-17XTSO-CS------W-to-22XCORESO------S.xml".matches(REF_PROG_FILE_NAME));
        assertTrue("20220417-F120-v2-17XTSO-CS------W-to-22XCORESO------S.xml".matches(REF_PROG_FILE_NAME));
        assertFalse("20210612-F120-v1-17XTSO-CS------W-to-22XCORESO------S".matches(REF_PROG_FILE_NAME));
        assertFalse("20210612-F120-17XTSO-CS------W-to-22XCORESO------S.xml".matches(REF_PROG_FILE_NAME));
        assertFalse("2021012-F120-v1-17XTSO-CS------W-to-22XCORESO------S.xml".matches(REF_PROG_FILE_NAME));
    }

    @Test
    public void testVirtualHubNames() {
        assertTrue("20210612-F327-v1-17XTSO-CS------W-to-22XCORESO------S.xml".matches(VIRTUAL_HUBS_FILE_NAME));
        assertTrue("20210928-F327-v2-17XTSO-CS------W-to-22XCORESO------S.xml".matches(VIRTUAL_HUBS_FILE_NAME));
        assertFalse("20210612-F327-v1-17XTSO-CS------W-to-22XCORESO------S".matches(VIRTUAL_HUBS_FILE_NAME));
        assertFalse("20210612-F327-17XTSO-CS------W-to-22XCORESO------S.xml".matches(VIRTUAL_HUBS_FILE_NAME));
        assertFalse("2020612-F327-v1-17XTSO-CS------W-to-22XCORESO------S.xml".matches(VIRTUAL_HUBS_FILE_NAME));
    }
}
