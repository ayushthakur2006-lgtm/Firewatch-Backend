package com.firewatch.firewatch_backend.dto.geojson;

import java.util.List;

public class GeoJsonGeometryDto {

    private String type = "Point";
    private List<Double> coordinates; // [longitude, latitude]

    public GeoJsonGeometryDto() {
    }

    public GeoJsonGeometryDto(Double longitude, Double latitude) {
        this.type = "Point";
        this.coordinates = List.of(longitude, latitude);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Double> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Double> coordinates) {
        this.coordinates = coordinates;
    }
}
