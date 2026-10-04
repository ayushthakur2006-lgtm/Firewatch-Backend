package com.firewatch.firewatch_backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class MlPredictionRequestDto {

    @JsonProperty("fire_firms_data")
    private FireFirmsDataDto fireFirmsData;

    @JsonProperty("satellite_geometry_features")
    private SatelliteGeometryDto satelliteGeometryFeatures;

    @JsonProperty("land_cover_features")
    private LandCoverFeaturesDto landCoverFeatures;

    @JsonProperty("spectral_features")
    private SpectralFeaturesDto spectralFeatures;

    @JsonProperty("historical_context_2km")
    private List<HistoricalDetectionDto> historicalContext2km = new ArrayList<>();

    public MlPredictionRequestDto() {
    }

    public MlPredictionRequestDto(FireFirmsDataDto fireFirmsData,
                                  SatelliteGeometryDto satelliteGeometryFeatures,
                                  LandCoverFeaturesDto landCoverFeatures,
                                  SpectralFeaturesDto spectralFeatures,
                                  List<HistoricalDetectionDto> historicalContext2km) {
        this.fireFirmsData = fireFirmsData;
        this.satelliteGeometryFeatures = satelliteGeometryFeatures;
        this.landCoverFeatures = landCoverFeatures;
        this.spectralFeatures = spectralFeatures;
        this.historicalContext2km = historicalContext2km != null ? historicalContext2km : new ArrayList<>();
    }

    public FireFirmsDataDto getFireFirmsData() {
        return fireFirmsData;
    }

    public void setFireFirmsData(FireFirmsDataDto fireFirmsData) {
        this.fireFirmsData = fireFirmsData;
    }

    public SatelliteGeometryDto getSatelliteGeometryFeatures() {
        return satelliteGeometryFeatures;
    }

    public void setSatelliteGeometryFeatures(SatelliteGeometryDto satelliteGeometryFeatures) {
        this.satelliteGeometryFeatures = satelliteGeometryFeatures;
    }

    public LandCoverFeaturesDto getLandCoverFeatures() {
        return landCoverFeatures;
    }

    public void setLandCoverFeatures(LandCoverFeaturesDto landCoverFeatures) {
        this.landCoverFeatures = landCoverFeatures;
    }

    public SpectralFeaturesDto getSpectralFeatures() {
        return spectralFeatures;
    }

    public void setSpectralFeatures(SpectralFeaturesDto spectralFeatures) {
        this.spectralFeatures = spectralFeatures;
    }

    public List<HistoricalDetectionDto> getHistoricalContext2km() {
        return historicalContext2km;
    }

    public void setHistoricalContext2km(List<HistoricalDetectionDto> historicalContext2km) {
        this.historicalContext2km = historicalContext2km;
    }
}
