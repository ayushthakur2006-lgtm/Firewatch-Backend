package com.firewatch.firewatch_backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "hotspots",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "latitude",
                                "longitude",
                                "acq_date",
                                "acq_time",
                                "satellite"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_hotspot_lat_lon", columnList = "latitude, longitude"),
                @Index(name = "idx_hotspot_acq_date", columnList = "acq_date"),
                @Index(name = "idx_hotspot_classification", columnList = "classification")
        }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Hotspot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Location
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // Existing FIRMS-related fields
    private Double brightness;
    private String confidence;
    
    @Column(name = "acq_date", nullable = false)
    private LocalDate acqDate;

    @Column(name = "acq_time")
    private LocalTime acqTime;

    private String source;

    // Additional NASA FIRMS fields
    private Double brightTi4;
    private Double brightTi5;
    private Double scan;
    private Double track;
    private String satellite;
    private String instrument;
    private String version;
    private Double frp;
    private String daynight;

    // ML output & Decision-Support
    private String classification;
    private Double mlConfidence;

    @Column(name = "inference_method")
    private String inferenceMethod; // ML_MODEL or RULE_BASED_FALLBACK

    // Decision-support & Risk Assessment (0-100)
    private Integer riskScore;
    private String riskLevel; // LOW, MODERATE, HIGH, CRITICAL

    @Column(length = 2000)
    private String evidence;

    @Column(length = 1000)
    private String recommendedAction;

    private Integer persistenceCount;
    private Integer activeDays;

    @OneToOne(mappedBy = "hotspot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MlFeature mlFeature;

    public Hotspot() {
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

    public Double getBrightness() {
        return brightness;
    }

    public void setBrightness(Double brightness) {
        this.brightness = brightness;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    public String getSatellite() {
        return satellite;
    }

    public void setSatellite(String satellite) {
        this.satellite = satellite;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Double getFrp() {
        return frp;
    }

    public void setFrp(Double frp) {
        this.frp = frp;
    }

    public String getDaynight() {
        return daynight;
    }

    public void setDaynight(String daynight) {
        this.daynight = daynight;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Double getMlConfidence() {
        return mlConfidence;
    }

    public void setMlConfidence(Double mlConfidence) {
        this.mlConfidence = mlConfidence;
    }

    public String getInferenceMethod() {
        return inferenceMethod;
    }

    public void setInferenceMethod(String inferenceMethod) {
        this.inferenceMethod = inferenceMethod;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public Integer getPersistenceCount() {
        return persistenceCount;
    }

    public void setPersistenceCount(Integer persistenceCount) {
        this.persistenceCount = persistenceCount;
    }

    public Integer getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(Integer activeDays) {
        this.activeDays = activeDays;
    }

    public MlFeature getMlFeature() {
        return mlFeature;
    }

    public void setMlFeature(MlFeature mlFeature) {
        this.mlFeature = mlFeature;
        if (mlFeature != null && mlFeature.getHotspot() != this) {
            mlFeature.setHotspot(this);
        }
    }
}