/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.rest;

import com.farao_community.farao.gridcapa.core_cc.app.RaoIntegrationService;
import com.farao_community.farao.gridcapa.core_cc.app.entities.RaoIntegrationTask;
import com.farao_community.farao.gridcapa.core_cc.app.util.FileUtil;
import io.swagger.annotations.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@RestController
@RequestMapping("/rao-integration")
public class RaoIntegrationController {

    private static final String JSON_API_MIME_TYPE = "application/vnd.api+json";

    private final RaoIntegrationService raoIntegrationService;

    public RaoIntegrationController(RaoIntegrationService raoIntegrationService) {
        this.raoIntegrationService = raoIntegrationService;
    }

    @PostMapping(value = "/asyncTasks", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = JSON_API_MIME_TYPE)
    @ApiOperation(value = "Create and Start a daily RAO computation task and returns it's ID.", tags = "RAO computation")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "The RAO task has been created successfully."),
            @ApiResponse(code = 400, message = "Invalid inputs.")})
    public ResponseEntity<RaoIntegrationTask> runDailyRao(@ApiParam(value = "Input files ZIP archive") @RequestPart MultipartFile inputFilesArchive) {
        return ResponseEntity.ok().body(raoIntegrationService.runRaoAsynchronously(inputFilesArchive));
    }

    @GetMapping(value = "/tasks/{taskId}/results-archive", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @ApiOperation(value = "Return a zip archive containing all results of the rao integration task with ID {taskId} .", tags = "RAO computation")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The result has been returned successfully."),
            @ApiResponse(code = 400, message = "Rao integration task with given ID has not finished yet"),
            @ApiResponse(code = 404, message = "Rao integration task with given ID not found in the server")})
    public ResponseEntity getDailyResultsArchive(@ApiParam(value = "Rao integration task ID") @PathVariable String taskId,
                                                 @ApiParam(value = "Output zip name (Optional)") @RequestParam(required = false) String outputZipName) {
        try {
            String zipName = Optional.ofNullable(outputZipName).orElse("rao-integration-results.zip");
            return FileUtil.toFileAttachmentResponse(raoIntegrationService.getDailyResultsZip(Long.parseLong(taskId)), zipName);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e);
        }
    }

    @GetMapping(value = "/tasks/{taskId}", produces = JSON_API_MIME_TYPE)
    @ApiOperation(value = "Return the status of the rao integration task with ID {taskId} .", tags = "RAO computation")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The task has been returned successfully."),
            @ApiResponse(code = 404, message = "Rao integration task with given ID not found in the server")})
    public ResponseEntity<RaoIntegrationTask> getTask(@ApiParam(value = "Rao integration task ID") @PathVariable String taskId) {
        return ResponseEntity.ok().body(raoIntegrationService.getEntityByTaskId(Long.parseLong(taskId)));
    }

}
