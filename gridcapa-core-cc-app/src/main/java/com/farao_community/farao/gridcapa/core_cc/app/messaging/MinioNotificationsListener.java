/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.messaging;

import com.farao_community.farao.gridcapa.core_cc.app.exceptions.RaoIntegrationException;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.configuration.MinioConfiguration;
import com.google.common.collect.Maps;
import io.minio.*;
import io.minio.messages.Event;
import io.minio.messages.NotificationRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
public class MinioNotificationsListener {

    private static final String RAO_REQUEST = "CORESO-F302";
    private static final String CRAC = "CORESO-F301";
    private static final String CGMS_ZIP = "CORESO-F119";
    private static final String REF_PROG = "CORESO-F120";
    private static final String GLSK = "CORESO-F319";
    private static final String VIRTUAL_HUBS = "CORESO-F327";
    private static final String BUSINESS_TYPE_PREFIX = "CORESO-F";

    private final RaoIntegrationAutomatedLaunchHandler raoIntegrationAutomatedLaunchHandler;
    private static final Logger LOGGER = LoggerFactory.getLogger(MinioNotificationsListener.class);
    private final MinioClient minioClient;
    private final MinioAdapter minioAdapter;
    private final String defaultBucket;
    private final Map<String, Map<String, String>> availableFilesByDay = new ConcurrentHashMap<>();  // K -> businessDay, (k,v) --> businessType, minioObjectName

    public MinioNotificationsListener(RaoIntegrationAutomatedLaunchHandler raoIntegrationAutomatedLaunchHandler, MinioClient minioClient, MinioAdapter minioAdapter, MinioConfiguration minioConfiguration) {
        this.raoIntegrationAutomatedLaunchHandler = raoIntegrationAutomatedLaunchHandler;
        this.minioClient = minioClient;
        this.minioAdapter = minioAdapter;
        this.defaultBucket = minioConfiguration.getDefaultBucket();

    }

    //a single connection is open between raoi and minio (a single listening thread), the retry after 10s is useful for recovery in case of exception...
    @Scheduled(fixedDelay = 10000)
    public void scheduledMinioNotificationsListening() {
        String listeningPath = "ORIGINAL_INPUTS/*/*/*/*"; // * is minio wildcard // here expecting ORIGINAL_INPUTS/year/month/day/any-file
        LOGGER.info("Listening To RaoRequests under '{}/{}'", this.defaultBucket, listeningPath);
        String[] events = {"s3:ObjectCreated:*"};  // listen only to new file creation events
        try (CloseableIterator<Result<NotificationRecords>> ci =
                     minioClient.listenBucketNotification(
                             ListenBucketNotificationArgs.builder()
                                     .bucket(this.defaultBucket)  // inputs come to default bucket  // outputs generated under outputs bucket
                                     .prefix(listeningPath)
                                     .suffix("")
                                     .events(events)
                                     .build())) {
            while (ci.hasNext()) {
                NotificationRecords records = ci.next().get();
                for (Event event : records.events()) {
                    LOGGER.info("Event '{}' occurred at '{}' for '{}/{}'", event.eventType(), event.eventTime(), Optional.ofNullable(event.bucketName()).orElse(defaultBucket), Optional.ofNullable(event.objectName()).orElse("unknown event"));
                    String minioObjectName = event.objectName();
                    String fileName = minioObjectName.substring(minioObjectName.lastIndexOf('/') + 1);
                    String businessType = fileName.substring(fileName.indexOf(BUSINESS_TYPE_PREFIX), fileName.indexOf(BUSINESS_TYPE_PREFIX) + BUSINESS_TYPE_PREFIX.length() + 3);

                    String businessDay = minioObjectName.substring(minioObjectName.indexOf("ORIGINAL_INPUTS/") + "ORIGINAL_INPUTS/".length(), minioObjectName.indexOf("ORIGINAL_INPUTS/") + "ORIGINAL_INPUTS/".length() + 10);
                    fillReceivedFilesOfBusinessDay(businessDay, businessType, minioObjectName);

                    Map<String, String> receivedFilesOfBusinessDay = availableFilesByDay.get(businessDay);
                    if (receivedFilesOfBusinessDay.containsKey(RAO_REQUEST)
                            && receivedFilesOfBusinessDay.containsKey(CRAC)
                            && receivedFilesOfBusinessDay.containsKey(CGMS_ZIP)
                            && receivedFilesOfBusinessDay.containsKey(REF_PROG)
                            && receivedFilesOfBusinessDay.containsKey(VIRTUAL_HUBS)
                            && receivedFilesOfBusinessDay.containsKey(GLSK)) {
                        LOGGER.info("Received all required inputs: Run RaoRequest for day: '{}'", businessDay);

                        String refProgPreSignedUrl = minioAdapter.generatePreSignedUrl(receivedFilesOfBusinessDay.get(REF_PROG));
                        String glskPreSignedUrl = minioAdapter.generatePreSignedUrl(receivedFilesOfBusinessDay.get(GLSK));

                        raoIntegrationAutomatedLaunchHandler.handleProcessRequest("RAO_WORKING_DIR" + "/" + businessDay,
                                receivedFilesOfBusinessDay.get(RAO_REQUEST),
                                receivedFilesOfBusinessDay.get(CRAC),
                                receivedFilesOfBusinessDay.get(CGMS_ZIP),
                                receivedFilesOfBusinessDay.get(VIRTUAL_HUBS),
                                refProgPreSignedUrl,
                                glskPreSignedUrl);
                    }
                }
            }

        } catch (Exception e) {
            throw new RaoIntegrationException("Exception preventing listening to minio", e);
        }
    }

    private void fillReceivedFilesOfBusinessDay(String businessDay, String businessType, String minioObjectName) {
        availableFilesByDay.computeIfPresent(businessDay, (k, v) -> {
            v.put(businessType, minioObjectName);
            return v;
        });
        availableFilesByDay.putIfAbsent(businessDay, Maps.newHashMap(Map.of(businessType, minioObjectName)));
    }
}

