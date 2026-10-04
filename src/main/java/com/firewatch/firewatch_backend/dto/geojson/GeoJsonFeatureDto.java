package com.firewatch.firewatch_backend.dto.geojson;

import java.util.HashMap;
import java.util.Map;

public class GeoJsonFeatureDto {

    private String type = "Feature";
    private GeoJsonGeometryDto geometry;
    private Map<String, Object> properties = new HashMap<>();

    public GeoJsonFeatureDto() {
    }

    public GeoJsonFeatureDto(GeoJsonGeometryDto geometry, Map<String, Object> properties) {
        this.type = "Feature";
        this.geometry = geometry;
        this.properties = properties != null ? properties : new HashMap<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public GeoJsonGeometryDto getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonGeometryDto geometry) {
        this.geometry = geometry;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
