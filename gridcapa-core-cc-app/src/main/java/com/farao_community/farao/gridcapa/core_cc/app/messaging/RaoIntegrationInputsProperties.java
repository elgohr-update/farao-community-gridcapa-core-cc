/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "rao-integration")
public class RaoIntegrationInputsProperties {

    private final FilenamesProperties filenames;

    public RaoIntegrationInputsProperties(FilenamesProperties filenames) {
        this.filenames = filenames;
    }

    public FilenamesProperties getFilenames() {
        return filenames;
    }

    public static class FilenamesProperties {
        private final String request;
        private final String cgms;
        private final String crac;
        private final String glsk;
        private final String refprog;
        private final String virtualhubs;

        public FilenamesProperties(String request, String cgms, String glsk, String refprog, String crac, String virtualhubs) {
            this.request = request;
            this.cgms = cgms;
            this.glsk = glsk;
            this.refprog = refprog;
            this.crac = crac;
            this.virtualhubs = virtualhubs;
        }

        public String getRequest() {
            return request;
        }

        public String getCgms() {
            return cgms;
        }

        public String getGlsk() {
            return glsk;
        }

        public String getRefprog() {
            return refprog;
        }

        public String getCrac() {
            return crac;
        }

        public String getVirtualhubs() {
            return virtualhubs;
        }
    }
}
