package com.firewatch.firewatch_backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LandCoverFeaturesDto {

    private Double bare;

    @JsonProperty("flooded_vegetation")
    private Double floodedVegetation;

    private Double grass;

    @JsonProperty("shrub_and_scrub")
    private Double shrubAndScrub;

    @JsonProperty("snow_and_ice")
    private Double snowAndIce;

    private Double water;

    @JsonProperty("provenance")
    private String provenance;

    @JsonProperty("quality")
    private String quality;

    public LandCoverFeaturesDto() {
    }

    public LandCoverFeaturesDto(Double bare, Double floodedVegetation, Double grass,
                                Double shrubAndScrub, Double snowAndIce, Double water) {
        this(bare, floodedVegetation, grass, shrubAndScrub, snowAndIce, water, "FALLBACK_ESTIMATED", "ESTIMATED");
    }

    public LandCoverFeaturesDto(Double bare, Double floodedVegetation, Double grass,
                                Double shrubAndScrub, Double snowAndIce, Double water,
                                String provenance, String quality) {
        this.bare = bare;
        this.floodedVegetation = floodedVegetation;
        this.grass = grass;
        this.shrubAndScrub = shrubAndScrub;
        this.snowAndIce = snowAndIce;
        this.water = water;
        this.provenance = provenance;
        this.quality = quality;
    }

    public Double getBare() {
        return bare;
    }

    public void setBare(Double bare) {
        this.bare = bare;
    }

    public Double getFloodedVegetation() {
        return floodedVegetation;
    }

    public void setFloodedVegetation(Double floodedVegetation) {
        this.floodedVegetation = floodedVegetation;
    }

    public Double getGrass() {
        return grass;
    }

    public void setGrass(Double grass) {
        this.grass = grass;
    }

    public Double getShrubAndScrub() {
        return shrubAndScrub;
    }

    public void setShrubAndScrub(Double shrubAndScrub) {
        this.shrubAndScrub = shrubAndScrub;
    }

    public Double getSnowAndIce() {
        return snowAndIce;
    }

    public void setSnowAndIce(Double snowAndIce) {
        this.snowAndIce = snowAndIce;
    }

    public Double getWater() {
        return water;
    }

    public void setWater(Double water) {
        this.water = water;
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
