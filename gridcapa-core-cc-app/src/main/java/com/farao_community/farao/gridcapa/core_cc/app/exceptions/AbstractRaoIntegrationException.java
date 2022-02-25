/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.exceptions;

import org.springframework.core.NestedExceptionUtils;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public abstract class AbstractRaoIntegrationException extends RuntimeException {

    public AbstractRaoIntegrationException(String message) {
        super(message);
    }

    public AbstractRaoIntegrationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public abstract int getStatus();

    public abstract String getCode();

    public final String getTitle() {
        return getMessage();
    }

    public final String getDetails() {
        return NestedExceptionUtils.buildMessage(getMessage(), getCause());
    }
}

