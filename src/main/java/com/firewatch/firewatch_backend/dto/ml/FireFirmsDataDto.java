package com.firewatch.firewatch_backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;

public class FireFirmsDataDto {

    private Double latitude;
    private Double longitude;

    @JsonProperty("acq_date")
    private LocalDate acqDate;

    @JsonProperty("acq_time")
    private LocalTime acqTime;

    @JsonProperty("bright_ti4")
    private Double brightTi4;

    @JsonProperty("bright_ti5")
    private Double brightTi5;

    private Double frp;
    private Double scan;
    private Double track;

    public FireFirmsDataDto() {
    }

    public FireFirmsDataDto(Double latitude, Double longitude, LocalDate acqDate, LocalTime acqTime,
                             Double brightTi4, Double brightTi5, Double frp, Double scan, Double track) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.acqDate = acqDate;
        this.acqTime = acqTime;
        this.brightTi4 = brightTi4;
        this.brightTi5 = brightTi5;
        this.frp = frp;
        this.scan = scan;
        this.track = track;
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

    public LocalTime getAcqTime() {
        return acqTime;
    }

    public void setAcqTime(LocalTime acqTime) {
        this.acqTime = acqTime;
    }

    public Double getBrightTi4() {
        return brightTi4;
    }

    public void setBrightTi4(Double brightTi4) {
        this.brightTi4 = brightTi4;
    }

    public Double getBrightTi5() {
        return brightTi5;
    }

    public void setBrightTi5(Double brightTi5) {
        this.brightTi5 = brightTi5;
    }

    public Double getFrp() {
        return frp;
    }

    public void setFrp(Double frp) {
        this.frp = frp;
    }

    public Double getScan() {
        return scan;
    }

    public void setScan(Double scan) {
        this.scan = scan;
    }

    public Double getTrack() {
        return track;
    }

    public void setTrack(Double track) {
        this.track = track;
    }
}
