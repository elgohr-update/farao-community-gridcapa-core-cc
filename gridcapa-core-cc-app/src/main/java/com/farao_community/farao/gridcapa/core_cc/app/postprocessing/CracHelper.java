/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.postprocessing;

import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.FbConstraint;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCracCreator;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCreationContext;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.importer.FbConstraintImporter;
import com.farao_community.farao.gridcapa.core_cc.app.exceptions.RaoIntegrationException;
import com.powsybl.iidm.network.Network;

import java.io.InputStream;
import java.time.OffsetDateTime;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public final class CracHelper {

    private CracHelper() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static FbConstraintCreationContext importCracXmlGetFbInfoWithNetwork(String timestamp, Network network, InputStream cracXmlFileInputStream) {
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp);
            FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(cracXmlFileInputStream);
            FbConstraintCreationContext cracCreationContext = new FbConstraintCracCreator().createCrac(fbConstraint, network, offsetDateTime, CracCreationParameters.load());
            if (cracCreationContext.isCreationSuccessful()) {
                return cracCreationContext;
            } else {
                throw new RaoIntegrationException("Crac creation context failed for timestamp: " + timestamp);
            }
        } catch (Exception e) {
            throw new RaoIntegrationException("Crac creation context failed, timestamp: " + timestamp, e);
        }
    }
}
