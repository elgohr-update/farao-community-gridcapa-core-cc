/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.postprocessing;

import com.farao_community.farao.data.crac_creation.creator.fb_constraint.FbConstraint;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.FlowBasedConstraintDocument;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.importer.FbConstraintImporter;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.exceptions.RaoIntegrationException;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import com.farao_community.farao.gridcapa.core_cc.app.util.IntervalUtil;
import org.springframework.stereotype.Service;
import org.threeten.extra.Interval;

import java.io.*;
import java.util.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com}
 */
@Service
public class DailyF303Generator {

    private final MinioAdapter minioAdapter;

    public DailyF303Generator(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public FlowBasedConstraintDocument generate(RaoIntegrationTask raoIntegrationTask) {

        try (InputStream cracXmlInputStream = minioAdapter.getInputStreamFromUrl(raoIntegrationTask.getInputCracXmlFileUrl())) {

            // get native CRAC
            FbConstraint nativeCrac = new FbConstraintImporter().importNativeCrac(cracXmlInputStream);

            // generate F303Info for each 24 hours of the initial CRAC
            Map<Integer, Interval> positionMap = IntervalUtil.getPositionsMap(nativeCrac.getDocument().getConstraintTimeInterval().getV());
            List<HourlyF303Info> hourlyF303Infos = new ArrayList<>();
            positionMap.values().forEach(interval -> hourlyF303Infos.add(new HourlyF303InfoGenerator(nativeCrac, interval, raoIntegrationTask, minioAdapter).generate()));

            // gather hourly info in one common document, cluster the elements that can be clusterized
            FlowBasedConstraintDocument flowBasedConstraintDocument = new DailyF303Clusterizer(hourlyF303Infos, nativeCrac).generateClusterizedDocument();

            // save this to fill in rao response
            raoIntegrationTask.getDailyOutputs().setOutputFlowBasedConstraintDocumentMessageId(flowBasedConstraintDocument.getDocumentIdentification().getV());
            return flowBasedConstraintDocument;
        } catch (Exception e) {
            throw new RaoIntegrationException("Exception occurred during F303 file creation", e);
        }
    }
}
