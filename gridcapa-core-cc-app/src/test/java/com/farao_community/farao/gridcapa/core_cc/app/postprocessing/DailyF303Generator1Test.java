/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.postprocessing;

import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.ActionType;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.ActionsSetType;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.CriticalBranchType;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.xsd.FlowBasedConstraintDocument;
import com.farao_community.farao.gridcapa.core_cc.app.TaskUtils;
import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoRequest;
import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoResult;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.entities.TaskStatus;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */

@SpringBootTest
class DailyF303Generator1Test {

    /*
     * test-case initially designed to test the F303 export, with two hours of data, one contingency
     * and some elements which are not CNEC and not MNEC
     */

    @Autowired
    private DailyF303Generator dailyF303Generator;

    @MockBean
    private MinioAdapter minioAdapter;
    private RaoIntegrationTask task;

    @BeforeEach
    public void setUp() {
        InputStream inputCracXmlInputStream = getClass().getResourceAsStream("/postprocessing/f303-1/inputs/F301.xml");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/inputCracXml.xml")).thenReturn(inputCracXmlInputStream);

        InputStream network1InputStream = getClass().getResourceAsStream("/postprocessing/f303-1/inputs/networks/20190108_1230.xiidm");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/network1.xiidm")).thenReturn(network1InputStream);
        InputStream raoResult1InputStream = getClass().getResourceAsStream("/postprocessing/f303-1/hourly_rao_results/20190108_1230/raoResult.json");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/raoResult1.json")).thenReturn(raoResult1InputStream);

        InputStream network2InputStream = getClass().getResourceAsStream("/postprocessing/f303-1/inputs/networks/20190108_1330.xiidm");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/network2.xiidm")).thenReturn(network2InputStream);
        InputStream raoResult2InputStream = getClass().getResourceAsStream("/postprocessing/f303-1/hourly_rao_results/20190108_1330/raoResult.json");
        Mockito.when(minioAdapter.getInputStreamFromUrl("http://host:9000/raoResult2.json")).thenReturn(raoResult2InputStream);

        Mockito.when(minioAdapter.getFileNameFromUrl(Mockito.any())).thenCallRealMethod();

        task = new RaoIntegrationTask();
        TaskUtils.setTaskId(task, 1L);

        task.setTimeInterval("2019-01-07T23:00Z/2019-01-08T23:00Z");
        task.setInputCracXmlFileUrl("http://host:9000/inputCracXml.xml");

        Set<HourlyRaoRequest> hourlyRaoRequests = new HashSet<>();
        HourlyRaoRequest hourlyRaoRequest1 = new HourlyRaoRequest();
        hourlyRaoRequest1.setInstant("2019-01-08T12:30:00Z");
        hourlyRaoRequest1.setNetworkFileUrl("http://host:9000/network1.xiidm");
        hourlyRaoRequests.add(hourlyRaoRequest1);
        HourlyRaoRequest hourlyRaoRequest2 = new HourlyRaoRequest();
        hourlyRaoRequest2.setInstant("2019-01-08T13:30:00Z");
        hourlyRaoRequest2.setNetworkFileUrl("http://host:9000/network2.xiidm");
        hourlyRaoRequests.add(hourlyRaoRequest2);
        task.setHourlyRaoRequests(hourlyRaoRequests);
        Set<HourlyRaoResult> hourlyRaoResults = new HashSet<>();
        HourlyRaoResult hourlyRaoResult1 = new HourlyRaoResult();
        hourlyRaoResult1.setInstant("2019-01-08T12:30:00Z");
        hourlyRaoResult1.setStatus(TaskStatus.SUCCESS);
        hourlyRaoResult1.setRaoResultFileUrl("http://host:9000/raoResult1.json");
        hourlyRaoResults.add(hourlyRaoResult1);
        HourlyRaoResult hourlyRaoResult2 = new HourlyRaoResult();
        hourlyRaoResult2.setInstant("2019-01-08T13:30:00Z");
        hourlyRaoResult2.setStatus(TaskStatus.SUCCESS);
        hourlyRaoResult2.setRaoResultFileUrl("http://host:9000/raoResult2.json");
        hourlyRaoResults.add(hourlyRaoResult2);
        task.setHourlyRaoResults(hourlyRaoResults);
    }

    @Test
    void validateMergedFlowBasedCreation() {
        FlowBasedConstraintDocument dailyFbConstDocument = dailyF303Generator.generate(task);
        assertEquals("22XCORESO------S-20190108-F303v1", dailyFbConstDocument.getDocumentIdentification().getV());
        assertEquals(1, dailyFbConstDocument.getDocumentVersion().getV());
        assertEquals("B07", dailyFbConstDocument.getDocumentType().getV().value());
        assertEquals("22XCORESO------S", dailyFbConstDocument.getSenderIdentification().getV());
        assertEquals("A44", dailyFbConstDocument.getSenderRole().getV().value());
        assertEquals("17XTSO-CS------W", dailyFbConstDocument.getReceiverIdentification().getV());
        assertEquals("A36", dailyFbConstDocument.getReceiverRole().getV().value());
        assertEquals("2019-01-07T23:00Z/2019-01-08T23:00Z", dailyFbConstDocument.getConstraintTimeInterval().getV());
        assertEquals("10YDOM-REGION-1V", dailyFbConstDocument.getDomain().getV());
        assertEquals(23, dailyFbConstDocument.getCriticalBranches().getCriticalBranch().size());
        List<CriticalBranchType> criticalBranchTypes = dailyFbConstDocument.getCriticalBranches().getCriticalBranch();
        List<CriticalBranchType> fr1Fr4CO1 = criticalBranchTypes.stream().filter(cb -> cb.getId().equals("fr1_fr4_CO1")).collect(Collectors.toList());
        assertEquals("2019-01-07T23:00Z/2019-01-08T12:00Z", fr1Fr4CO1.get(0).getTimeInterval().getV());
        assertEquals("2019-01-08T14:00Z/2019-01-08T23:00Z", fr1Fr4CO1.get(1).getTimeInterval().getV());
        List<CriticalBranchType> fr1Fr4Co1Patl = criticalBranchTypes.stream().filter(cb -> cb.getId().equals("fr1_fr4_CO1_PATL")).collect(Collectors.toList());
        assertEquals("2019-01-08T12:00Z/2019-01-08T13:00Z", fr1Fr4Co1Patl.get(0).getTimeInterval().getV());
        assertEquals("fr1_fr4_CO1", fr1Fr4Co1Patl.get(0).getOriginalId());
        assertEquals("2019-01-08T13:00Z/2019-01-08T14:00Z", fr1Fr4Co1Patl.get(1).getTimeInterval().getV());
        assertEquals("fr1_fr4_CO1", fr1Fr4Co1Patl.get(1).getOriginalId());
        assertNotEquals(fr1Fr4Co1Patl.get(0).getComplexVariantId(), fr1Fr4Co1Patl.get(1).getComplexVariantId());
        assertEquals("1", fr1Fr4Co1Patl.get(0).getImaxFactor().toString());
        assertNull(fr1Fr4Co1Patl.get(0).getImaxA());

        List<CriticalBranchType> fr1Fr4Co1Tatl = criticalBranchTypes.stream().filter(cb -> cb.getId().equals("fr1_fr4_CO1_TATL")).collect(Collectors.toList());
        assertEquals("2019-01-08T12:00Z/2019-01-08T14:00Z", fr1Fr4Co1Tatl.get(0).getTimeInterval().getV());
        assertEquals("fr1_fr4_CO1", fr1Fr4Co1Tatl.get(0).getOriginalId());
        assertEquals("1000", fr1Fr4Co1Tatl.get(0).getImaxFactor().toString());
        assertNull(fr1Fr4Co1Tatl.get(0).getImaxA());
        assertNull(fr1Fr4Co1Tatl.get(0).getComplexVariantId());

        assertEquals(2, dailyFbConstDocument.getComplexVariants().getComplexVariant().size());
        assertEquals("open_fr1_fr3;pst_be", dailyFbConstDocument.getComplexVariants().getComplexVariant().get(0).getName());
        assertEquals("2019-01-08T12:00Z/2019-01-08T13:00Z", dailyFbConstDocument.getComplexVariants().getComplexVariant().get(0).getTimeInterval().getV());
        assertEquals("open_fr1_fr3;pst_be", dailyFbConstDocument.getComplexVariants().getComplexVariant().get(1).getName());
        assertEquals("2019-01-08T13:00Z/2019-01-08T14:00Z", dailyFbConstDocument.getComplexVariants().getComplexVariant().get(1).getTimeInterval().getV());

        assertEquals("XX", dailyFbConstDocument.getComplexVariants().getComplexVariant().get(1).getTsoOrigin());
        List<ActionsSetType> actionsSetTypeList = dailyFbConstDocument.getComplexVariants().getComplexVariant().get(1).getActionsSet();
        assertEquals(2, actionsSetTypeList.size());
        assertEquals("open_fr1_fr3", actionsSetTypeList.get(0).getName());
        assertTrue(actionsSetTypeList.get(0).isCurative());
        assertFalse(actionsSetTypeList.get(0).isPreventive());
        List<String> afterCOListList = actionsSetTypeList.get(0).getAfterCOList().getAfterCOId();
        assertEquals(1, afterCOListList.size());
        assertEquals("CO1_fr2_fr3_1", afterCOListList.get(0));
        List<ActionType> actionTypeList = actionsSetTypeList.get(0).getAction();
        assertEquals("STATUS", actionTypeList.get(0).getType());

        assertEquals("pst_be", actionsSetTypeList.get(1).getName());
        assertTrue(actionsSetTypeList.get(1).isCurative());
        assertFalse(actionsSetTypeList.get(0).isPreventive());
        List<String> afterCOListList1 = actionsSetTypeList.get(1).getAfterCOList().getAfterCOId();
        assertEquals(1, afterCOListList1.size());
        assertEquals("CO1_fr2_fr3_1", afterCOListList1.get(0));
        List<ActionType> actionTypeList1 = actionsSetTypeList.get(1).getAction();
        assertEquals("PSTTAP", actionTypeList1.get(0).getType());
    }

}

