package com.firewatch.firewatch_backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class HistoricalDetectionDto {

    private Double latitude;
    private Double longitude;

    @JsonProperty("acq_date")
    private LocalDate acqDate;

    private Double frp;

    public HistoricalDetectionDto() {
    }

    public HistoricalDetectionDto(Double latitude, Double longitude, LocalDate acqDate, Double frp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.acqDate = acqDate;
        this.frp = frp;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDate getAcqDate() {
        return acqDate;
    }

    public void setAcqDate(LocalDate acqDate) {
        this.acqDate = acqDate;
    }

    public Double getFrp() {
        return frp;
    }

    public void setFrp(Double frp) {
        this.frp = frp;
    }
}
