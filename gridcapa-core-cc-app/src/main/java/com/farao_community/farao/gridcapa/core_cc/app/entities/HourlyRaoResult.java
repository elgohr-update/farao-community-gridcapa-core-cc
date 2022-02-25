/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.entities;

import com.farao_community.farao.rao_runner.api.resource.RaoResponse;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Embeddable
public class HourlyRaoResult {

    private String instant;

    @Column(columnDefinition = "TEXT")
    private String networkWithPraUrl;

    @Column(columnDefinition = "TEXT")
    private String raoResultFileUrl;

    private TaskStatus status;

    private String errorCode;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private String cneResultDocumentId;

    private Instant computationStartInstant =  Instant.ofEpochSecond(0);

    private Instant computationEndInstant =  Instant.ofEpochSecond(0);

    public HourlyRaoResult() {
    }

    public String getInstant() {
        return instant;
    }

    public void setInstant(String instant) {
        this.instant = instant;
    }

    public String getNetworkWithPraUrl() {
        return networkWithPraUrl;
    }

    public void setNetworkWithPraUrl(String networkWithPraUrl) {
        this.networkWithPraUrl = networkWithPraUrl;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) {
            return true;
        }
        if (!(o instanceof HourlyRaoResult)) {
            return false;
        }
        HourlyRaoResult hourlyRaoResult = (HourlyRaoResult) o;
        if (hourlyRaoResult.instant != null) {
            return hourlyRaoResult.instant.equals(instant);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(instant);
    }

    public String getRaoResultFileUrl() {
        return raoResultFileUrl;
    }

    public void setRaoResultFileUrl(String raoResultFileUrl) {
        this.raoResultFileUrl = raoResultFileUrl;
    }

    public String getCneResultDocumentId() {
        return cneResultDocumentId;
    }

    public void setCneResultDocumentId(String cneResultDocumentId) {
        this.cneResultDocumentId = cneResultDocumentId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Instant getComputationStartInstant() {
        return computationStartInstant;
    }

    public void setComputationStartInstant(Instant computationStartInstant) {
        this.computationStartInstant = computationStartInstant;
    }

    public Instant getComputationEndInstant() {
        return computationEndInstant;
    }

    public void setComputationEndInstant(Instant computationEndInstant) {
        this.computationEndInstant = computationEndInstant;
    }

    public void setRaoResponseData(RaoResponse raoResponse) {
        this.networkWithPraUrl = raoResponse.getNetworkWithPraFileUrl();
        this.raoResultFileUrl = raoResponse.getRaoResultFileUrl();
        this.computationStartInstant = raoResponse.getComputationStartInstant();
        this.computationEndInstant = raoResponse.getComputationEndInstant();
    }

}
