package com.firewatch.firewatch_backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpectralFeaturesDto {

    @JsonProperty("ndvi")
    private Double ndvi;

    @JsonProperty("ndwi")
    private Double ndwi;

    @JsonProperty("ndbi")
    private Double ndbi;

    @JsonProperty("provenance")
    private String provenance;

    @JsonProperty("quality")
    private String quality;

    public SpectralFeaturesDto() {
    }

    public SpectralFeaturesDto(Double ndvi, Double ndwi, Double ndbi) {
        this(ndvi, ndwi, ndbi, "FALLBACK_ESTIMATED", "ESTIMATED");
    }

    public SpectralFeaturesDto(Double ndvi, Double ndwi, Double ndbi, String provenance, String quality) {
        this.ndvi = ndvi;
        this.ndwi = ndwi;
        this.ndbi = ndbi;
        this.provenance = provenance;
        this.quality = quality;
    }

    public Double getNdvi() {
        return ndvi;
    }

    public void setNdvi(Double ndvi) {
        this.ndvi = ndvi;
    }

    public Double getNdwi() {
        return ndwi;
    }

    public void setNdwi(Double ndwi) {
        this.ndwi = ndwi;
    }

    public Double getNdbi() {
        return ndbi;
    }

    public void setNdbi(Double ndbi) {
        this.ndbi = ndbi;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }
}
