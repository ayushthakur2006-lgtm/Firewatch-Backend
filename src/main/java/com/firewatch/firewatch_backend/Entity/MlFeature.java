package com.firewatch.firewatch_backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "ml_features",
        indexes = {
                @Index(name = "idx_ml_feature_lat_lon", columnList = "latitude, longitude"),
                @Index(name = "idx_ml_feature_acq_date", columnList = "acq_date")
        }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MlFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double latitude;
    private Double longitude;

    @Column(name = "acq_date")
    private LocalDate acqDate;

    @Column(name = "acq_time")
    private LocalTime acqTime;

    private Double brightTi4;
    private Double brightTi5;

    private Double frp;

    private Double scan;
    private Double track;

    private Double shapeLength;
    private Double shapeArea;

    private Double frpDw;

    private Integer imageCount;

    private Double bare;
    private Double floodedVegetation;
    private Double grass;
    private Double shrubAndScrub;
    private Double snowAndIce;
    private Double water;

    private Double ndvi;
    private Double ndwi;
    private Double ndbi;

    // Feature Provenance: SENTINEL_DERIVED, COPERNICUS_DERIVED, FALLBACK_ESTIMATED, UNAVAILABLE
    @Column(name = "spectral_provenance")
    private String spectralProvenance;

    @Column(name = "land_cover_provenance")
    private String landCoverProvenance;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotspot_id", nullable = false, unique = true)
    @JsonIgnore
    private Hotspot hotspot;

    public MlFeature() {
    }

    public MlFeature(Hotspot hotspot) {
        this.hotspot = hotspot;
        if (hotspot != null) {
            this.latitude = hotspot.getLatitude();
            this.longitude = hotspot.getLongitude();
            this.acqDate = hotspot.getAcqDate();
            this.acqTime = hotspot.getAcqTime();
            this.brightTi4 = hotspot.getBrightTi4();
            this.brightTi5 = hotspot.getBrightTi5();
            this.frp = hotspot.getFrp();
            this.scan = hotspot.getScan();
            this.track = hotspot.getTrack();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSpectralProvenance() {
        return spectralProvenance;
    }

    public void setSpectralProvenance(String spectralProvenance) {
        this.spectralProvenance = spectralProvenance;
    }

    public String getLandCoverProvenance() {
        return landCoverProvenance;
    }

    public void setLandCoverProvenance(String landCoverProvenance) {
        this.landCoverProvenance = landCoverProvenance;
    }

    public Hotspot getHotspot() {
        return hotspot;
    }

    public void setHotspot(Hotspot hotspot) {
        this.hotspot = hotspot;
    }
}