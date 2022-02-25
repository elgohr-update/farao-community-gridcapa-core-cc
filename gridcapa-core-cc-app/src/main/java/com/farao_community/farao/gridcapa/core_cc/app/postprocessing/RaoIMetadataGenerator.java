/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.postprocessing;

import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoResult;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.entities.TaskStatus;
import com.farao_community.farao.gridcapa.core_cc.app.exceptions.RaoIntegrationException;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import com.farao_community.farao.gridcapa.core_cc.app.util.RaoMetadata;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.gridcapa.core_cc.app.util.RaoMetadata.Indicator.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoIMetadataGenerator {

    private final MinioAdapter minioAdapter;

    public RaoIMetadataGenerator(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public void exportMetadataFile(RaoIntegrationTask raoIntegrationTask, String targetMinioFolder, boolean isManualRun) {
        byte[] csv = generateMetadataCsv(raoIntegrationTask).getBytes();
        String metadataFileName = OutputFileNameUtil.generateMetadataFileName(raoIntegrationTask);
        String metadataDestinationPath = OutputFileNameUtil.generateOutputsDestinationPath(targetMinioFolder, metadataFileName);

        try (InputStream csvIs = new ByteArrayInputStream(csv)) {
            minioAdapter.uploadFile(metadataDestinationPath, csvIs);
            if (!isManualRun) {
                minioAdapter.copyObject(metadataDestinationPath, metadataFileName, minioAdapter.getDefaultBucket(), minioAdapter.getOutputsBucket());
            }
        } catch (IOException e) {
            throw new RaoIntegrationException(String.format("Exception occurred while uploading metadata file of task %s", raoIntegrationTask.getTaskId()));
        }
        raoIntegrationTask.getDailyOutputs().setMetadataOutputsPath(metadataDestinationPath);
    }

    private static String generateMetadataCsv(RaoIntegrationTask raoIntegrationTask) {
        MultiKeyMap data = structureDataFromTask(raoIntegrationTask);
        return writeCsvFromMap(raoIntegrationTask, data);
    }

    private static MultiKeyMap structureDataFromTask(RaoIntegrationTask raoIntegrationTask) {
        // Store data in a MultiKeyMap
        // First key is column (indicator)
        // Second key is timestamp (or whole business day)
        // Value is the value of the indicator for the given timestamp
        MultiKeyMap data = new MultiKeyMap<>();

        data.put(RAO_REQUESTS_RECEIVED, raoIntegrationTask.getTimeInterval(), raoIntegrationTask.getRaoRequestFileName());
        data.put(RAO_REQUEST_RECEPTION_TIME, raoIntegrationTask.getTimeInterval(), raoIntegrationTask.getInputsReceptionInstant().toString());
        data.put(RAO_OUTPUTS_SENT, raoIntegrationTask.getTimeInterval(), raoIntegrationTask.getTaskStatus().equals(TaskStatus.SUCCESS) ? "YES" : "NO");
        data.put(RAO_OUTPUTS_SENDING_TIME, raoIntegrationTask.getTimeInterval(), raoIntegrationTask.getOutputsSendingInstant().toString());
        data.put(RAO_COMPUTATION_STATUS, raoIntegrationTask.getTimeInterval(), raoIntegrationTask.getTaskStatus().toString());
        data.put(RAO_START_TIME, raoIntegrationTask.getTimeInterval(), raoIntegrationTask.getComputationStartInstant().toString());
        data.put(RAO_END_TIME, raoIntegrationTask.getTimeInterval(), raoIntegrationTask.getComputationEndInstant().toString());
        data.put(RAO_COMPUTATION_TIME, raoIntegrationTask.getTimeInterval(), String.valueOf(ChronoUnit.MINUTES.between(raoIntegrationTask.getComputationStartInstant(), raoIntegrationTask.getComputationEndInstant())));
        raoIntegrationTask.getHourlyRaoResults().forEach(hourlyRaoResult -> {
            data.put(RAO_START_TIME, hourlyRaoResult.getInstant(), hourlyRaoResult.getComputationStartInstant().toString());
            data.put(RAO_END_TIME, hourlyRaoResult.getInstant(), hourlyRaoResult.getComputationEndInstant().toString());
            data.put(RAO_COMPUTATION_TIME, hourlyRaoResult.getInstant(), String.valueOf(ChronoUnit.MINUTES.between(hourlyRaoResult.getComputationStartInstant(), hourlyRaoResult.getComputationEndInstant())));
            data.put(RAO_RESULTS_PROVIDED, hourlyRaoResult.getInstant(), hourlyRaoResult.getStatus().equals(TaskStatus.SUCCESS) ? "YES" : "NO");
            data.put(RAO_COMPUTATION_STATUS, hourlyRaoResult.getInstant(), hourlyRaoResult.getStatus().toString());
        });
        return data;
    }

    private static String writeCsvFromMap(RaoIntegrationTask raoIntegrationTask, MultiKeyMap data) {
        // Get headers for columns & lines
        List<RaoMetadata.Indicator> indicators = Arrays.stream(values())
                .sorted(Comparator.comparing(RaoMetadata.Indicator::getOrder))
                .collect(Collectors.toList());
        List<String> timestamps = raoIntegrationTask.getHourlyRaoResults().stream().map(HourlyRaoResult::getInstant).sorted(String::compareTo).collect(Collectors.toList());
        timestamps.add(0, raoIntegrationTask.getTimeInterval());

        // Generate CSV string
        char delimiter = ';';
        char cr = '\n';
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(delimiter);
        csvBuilder.append(indicators.stream().map(RaoMetadata.Indicator::getCsvLabel).collect(Collectors.joining(";")));
        csvBuilder.append(cr);
        for (String timestamp : timestamps) {
            csvBuilder.append(timestamp);
            for (RaoMetadata.Indicator indicator : indicators) {
                String value = data.containsKey(indicator, timestamp) ? data.get(indicator, timestamp).toString() : "";
                csvBuilder.append(delimiter);
                csvBuilder.append(value);
            }
            csvBuilder.append(cr);
        }
        return csvBuilder.toString();
    }
}
