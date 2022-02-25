/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app;

import com.farao_community.farao.gridcapa.core_cc.app.entities.HourlyRaoResult;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationRepository;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.entities.TaskStatus;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.configuration.AmqpConfiguration;
import com.farao_community.farao.gridcapa.core_cc.app.postprocessing.FileExporterHelper;
import com.farao_community.farao.gridcapa.core_cc.app.postprocessing.LogsExporter;
import com.farao_community.farao.gridcapa.core_cc.app.postprocessing.RaoIPostProcessService;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.AsynchronousRaoRunnerClient;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpReplyTimeoutException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Service
public class RaoClient {

    private final AsynchronousRaoRunnerClient asynchronousRaoRunnerClient;
    private final FileExporterHelper fileExporterHelper;
    private final RaoIPostProcessService raoIPostProcessService;
    private final RaoIntegrationRepository raoIntegrationRepository;
    private final AmqpConfiguration amqpConfiguration;
    private final LogsExporter logsExporter;
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoClient.class);
    private static final String RAO_FAILED_LOG_PATTERN = "Exception occurred in RAO computation for TimeStamp: '{}'. Origin cause: '{}'";

    public RaoClient(AsynchronousRaoRunnerClient asynchronousRaoRunnerClient, FileExporterHelper fileExporterHelper, RaoIPostProcessService raoIPostProcessService, RaoIntegrationRepository raoIntegrationRepository, AmqpConfiguration amqpConfiguration, LogsExporter logsExporter) {
        this.asynchronousRaoRunnerClient = asynchronousRaoRunnerClient;
        this.fileExporterHelper = fileExporterHelper;
        this.raoIPostProcessService = raoIPostProcessService;
        this.raoIntegrationRepository = raoIntegrationRepository;
        this.amqpConfiguration = amqpConfiguration;
        this.logsExporter = logsExporter;
    }

    public void startRunningAllRaoHourlyInputsAsync(RaoIntegrationTask raoIntegrationTask, String targetMinioFolder, boolean isManualRun) {
        raoIntegrationTask.setComputationStartInstant(Instant.now());
        raoIntegrationTask.getHourlyRaoRequests().forEach(hourlyRaoRequest -> {
            LOGGER.info("Running RAO computation for TimeStamp '{}'", hourlyRaoRequest.getInstant());
            CompletableFuture<RaoResponse> raoResponseFuture = asynchronousRaoRunnerClient.runRaoAsynchronously(hourlyRaoRequest.toRaoRequest(String.valueOf(raoIntegrationTask.getTaskId())));
            raoResponseFuture.thenApply(raoResponse -> {

                synchronized (this) {
                    LOGGER.info("RAO computation answer received  for TimeStamp: '{}'", hourlyRaoRequest.getInstant());
                    HourlyRaoResult hourlyRaoResult = new HourlyRaoResult();
                    hourlyRaoResult.setInstant(hourlyRaoRequest.getInstant());
                    convertAndSaveAsynchronouslyReceivedRaoResult(raoIntegrationTask, hourlyRaoResult, raoResponse);
                    raoIntegrationTask.getHourlyRaoResults().add(hourlyRaoResult);
                    raoIntegrationRepository.save(raoIntegrationTask);
                    runFinalPostProcessIfAllTimestampsAreFinished(raoIntegrationTask, targetMinioFolder, isManualRun);
                }
                return null;
            }).exceptionally(exception -> {

                synchronized (this) {
                    HourlyRaoResult hourlyRaoResult = new HourlyRaoResult();
                    hourlyRaoResult.setInstant(hourlyRaoRequest.getInstant());
                    handleRaoRunnerException(hourlyRaoResult, exception);
                    raoIntegrationTask.getHourlyRaoResults().add(hourlyRaoResult);
                    raoIntegrationRepository.save(raoIntegrationTask);
                    runFinalPostProcessIfAllTimestampsAreFinished(raoIntegrationTask, targetMinioFolder, isManualRun);
                }
                return null;
            });
        });
        System.gc();
    }

    private void runFinalPostProcessIfAllTimestampsAreFinished(RaoIntegrationTask raoIntegrationTask, String
        targetMinioFolder, boolean isManualRun) {
        try {
            long requestedRaos = raoIntegrationTask.getHourlyRaoRequests().size();
            long receivedRaos = raoIntegrationTask.getHourlyRaoResults().stream().filter(hourlyRaoResult -> hourlyRaoResult.getStatus().equals(TaskStatus.SUCCESS)).count() +
                raoIntegrationTask.getHourlyRaoResults().stream().filter(hourlyRaoResult -> hourlyRaoResult.getStatus().equals(TaskStatus.ERROR)).count();
            if (requestedRaos == receivedRaos) {
                LOGGER.info("Rao integration server received answers for all requested '{}' timestamps, --> proceeding to generating outputs", requestedRaos);
                raoIntegrationTask.setComputationEndInstant(Instant.now());
                logsExporter.exportLogs(raoIntegrationTask);
                raoIPostProcessService.postProcessHourlyResults(raoIntegrationTask, targetMinioFolder, isManualRun);
                raoIntegrationRepository.save(raoIntegrationTask);
            }
        } catch (Exception e) {
            raoIntegrationTask.setTaskStatus(TaskStatus.ERROR);
            raoIntegrationRepository.save(raoIntegrationTask);
        }
    }

    private void convertAndSaveAsynchronouslyReceivedRaoResult(RaoIntegrationTask
                                                                   raoIntegrationTask, HourlyRaoResult hourlyRaoResult, RaoResponse raoResponse) {
        try {
            hourlyRaoResult.setRaoResponseData(raoResponse);
            fileExporterHelper.exportCneInTmpOutput(raoIntegrationTask, hourlyRaoResult);
            fileExporterHelper.exportNetworkInTmpOutput(raoIntegrationTask, hourlyRaoResult);
            hourlyRaoResult.setStatus(TaskStatus.SUCCESS);
        } catch (Exception e) {
            //no throwing exception, just save cause and pass to next timestamp
            String errorMessage = String.format("error occurred while post processing rao outputs for timestamp: %s, Cause: %s", hourlyRaoResult.getInstant(), e.getMessage());
            LOGGER.error(errorMessage);
            hourlyRaoResult.setStatus(TaskStatus.ERROR);
            hourlyRaoResult.setErrorMessage(errorMessage);
        }
    }

    private void handleRaoRunnerException(HourlyRaoResult hourlyRaoResult, Throwable exception) {
        hourlyRaoResult.setStatus(TaskStatus.ERROR);
        if (exception instanceof ResourceParseException) {
            // Sync scenario : exception details from rao-runner comes wrapped into ResourceParseException on json Api Error format.
            ResourceParseException resourceParseException = (ResourceParseException) exception;
            String originCause = resourceParseException.getErrors().getErrors().get(0).getDetail();
            hourlyRaoResult.setErrorMessage(originCause);
            LOGGER.warn(RAO_FAILED_LOG_PATTERN, hourlyRaoResult.getInstant(), originCause);
        } else if (exception.getCause() instanceof ResourceParseException) {
            // Async scenario : exception details from rao-runner comes wrapped into ResourceParseException on json Api Error format, which is wrapped itself into a ConcurrencyException.
            ResourceParseException resourceParseException = (ResourceParseException) exception.getCause();
            String originCause = resourceParseException.getErrors().getErrors().get(0).getDetail();
            hourlyRaoResult.setErrorMessage(originCause);
            LOGGER.warn(RAO_FAILED_LOG_PATTERN, hourlyRaoResult.getInstant(), originCause);
        } else if (exception.getCause() instanceof AmqpReplyTimeoutException) {
            String originCause = "Timeout reached, Rao has not finished within allocated time of : " + amqpConfiguration.getAsyncTimeOutInMinutes() + " minutes";
            hourlyRaoResult.setErrorMessage(originCause);
            LOGGER.warn(RAO_FAILED_LOG_PATTERN, hourlyRaoResult.getInstant(), originCause);
        } else {
            // if exception is not a json api Error neither an AmqpReplyTimeoutException
            String originCause = exception.getMessage();
            hourlyRaoResult.setErrorMessage(originCause);
            LOGGER.warn(RAO_FAILED_LOG_PATTERN, hourlyRaoResult.getInstant(), originCause);
        }
    }

}
