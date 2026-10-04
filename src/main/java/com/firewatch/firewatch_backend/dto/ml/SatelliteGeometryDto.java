package com.firewatch.firewatch_backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SatelliteGeometryDto {

    @JsonProperty("shape_length")
    private Double shapeLength;

    @JsonProperty("shape_area")
    private Double shapeArea;

    @JsonProperty("frp_dw")
    private Double frpDw;

    @JsonProperty("image_count")
    private Integer imageCount;

    public SatelliteGeometryDto() {
    }

    public SatelliteGeometryDto(Double shapeLength, Double shapeArea, Double frpDw, Integer imageCount) {
        this.shapeLength = shapeLength;
        this.shapeArea = shapeArea;
        this.frpDw = frpDw;
        this.imageCount = imageCount;
    }

    public Double getShapeLength() {
        return shapeLength;
    }

    public void setShapeLength(Double shapeLength) {
        this.shapeLength = shapeLength;
    }

    public Double getShapeArea() {
        return shapeArea;
    }

    public void setShapeArea(Double shapeArea) {
        this.shapeArea = shapeArea;
    }

    public Double getFrpDw() {
        return frpDw;
    }

    public void setFrpDw(Double frpDw) {
        this.frpDw = frpDw;
    }

    public Integer getImageCount() {
        return imageCount;
    }

    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }
}
