package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Service.SentinelHubService;
import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Entity.MlFeature;
import com.firewatch.firewatch_backend.Respositry.MlFeatureRepository;
import com.firewatch.firewatch_backend.dto.ml.LandCoverFeaturesDto;
import com.firewatch.firewatch_backend.dto.ml.SatelliteGeometryDto;
import com.firewatch.firewatch_backend.dto.ml.SpectralFeaturesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class FeatureEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(FeatureEnrichmentService.class);

    @Value("${satellite.api.enabled:false}")
    private boolean satelliteApiEnabled;

    private final MlFeatureRepository mlFeatureRepository;

    @Autowired(required = false)
    private SentinelHubService sentinelHubService;

    public FeatureEnrichmentService(MlFeatureRepository mlFeatureRepository) {
        this.mlFeatureRepository = mlFeatureRepository;
    }

    @Transactional
    public MlFeature enrichHotspotFeatures(Hotspot hotspot) {
        if (hotspot == null) {
            return null;
        }

        Optional<MlFeature> existingOpt = mlFeatureRepository.findByHotspot(hotspot);
        MlFeature mlFeature = existingOpt.orElseGet(() -> new MlFeature(hotspot));

        mlFeature.setLatitude(hotspot.getLatitude());
        mlFeature.setLongitude(hotspot.getLongitude());
        mlFeature.setAcqDate(hotspot.getAcqDate());
        mlFeature.setAcqTime(hotspot.getAcqTime());
        mlFeature.setBrightTi4(hotspot.getBrightTi4());
        mlFeature.setBrightTi5(hotspot.getBrightTi5());
        mlFeature.setFrp(hotspot.getFrp());
        mlFeature.setScan(hotspot.getScan());
        mlFeature.setTrack(hotspot.getTrack());

        computeSatelliteGeometry(hotspot, mlFeature);

        boolean spectralSuccess = false;
        boolean landCoverSuccess = false;

        if (satelliteApiEnabled && sentinelHubService != null) {
            // 1. Retrieve real Sentinel-2 spectral indices (NDVI, NDWI, NDBI)
            Optional<Map<String, Double>> spectralOpt =
                    sentinelHubService.getSpectralFeatures(hotspot.getLatitude(), hotspot.getLongitude());
            if (spectralOpt.isPresent()) {
                applySpectralFromSentinel(mlFeature, spectralOpt.get());
                spectralSuccess = true;
            }

            // 2. Retrieve real Copernicus/CLMS land-cover classification
            Optional<Map<String, Double>> landCoverOpt =
                    sentinelHubService.getLandCoverFeatures(hotspot.getLatitude(), hotspot.getLongitude());
            if (landCoverOpt.isPresent()) {
                applyLandCoverFromSentinel(mlFeature, landCoverOpt.get());
                landCoverSuccess = true;
            }
        }

        // Handle fallbacks and partial success with explicit provenance recording
        if (spectralSuccess) {
            mlFeature.setSpectralProvenance("SENTINEL_DERIVED");
        } else {
            mlFeature.setSpectralProvenance("FALLBACK_ESTIMATED");
        }

        if (landCoverSuccess) {
            mlFeature.setLandCoverProvenance("COPERNICUS_DERIVED");
        } else {
            mlFeature.setLandCoverProvenance("FALLBACK_ESTIMATED");
        }

        if (!spectralSuccess && !landCoverSuccess) {
            // Full fallback to existing estimation method
            estimateLandCoverAndSpectral(hotspot, mlFeature);
        } else {
            if (!spectralSuccess) {
                // Land cover succeeded, but spectral retrieval failed -> fallback spectral estimation
                estimateSpectralOnly(hotspot, mlFeature);
            }
            if (!landCoverSuccess) {
                // Spectral succeeded, but land cover retrieval failed -> fallback land-cover estimation
                estimateLandCoverFromSpectral(mlFeature);
            }
        }

        mlFeature.setHotspot(hotspot);
        hotspot.setMlFeature(mlFeature);

        return mlFeatureRepository.save(mlFeature);
    }

    private void computeSatelliteGeometry(Hotspot hotspot, MlFeature mlFeature) {
        double scan = hotspot.getScan() != null ? hotspot.getScan() : 0.39;
        double track = hotspot.getTrack() != null ? hotspot.getTrack() : 0.36;

        double shapeLengthMeters = 2.0 * (scan + track) * 1000.0;
        double shapeAreaSqMeters = scan * track * 1_000_000.0;

        double frp = hotspot.getFrp() != null ? hotspot.getFrp() : 0.0;
        double frpDw = shapeAreaSqMeters > 0 ? (frp / (shapeAreaSqMeters / 10_000.0)) : frp;

        mlFeature.setShapeLength(round(shapeLengthMeters, 2));
        mlFeature.setShapeArea(round(shapeAreaSqMeters, 2));
        mlFeature.setFrpDw(round(frpDw, 4));
        mlFeature.setImageCount(mlFeature.getImageCount() != null ? mlFeature.getImageCount() : 1);
    }

    /** Apply NDVI/NDWI/NDBI values retrieved from Sentinel Hub. */
    private void applySpectralFromSentinel(MlFeature mlFeature, Map<String, Double> spectral) {
        Double ndvi = spectral.get("ndvi");
        Double ndwi = spectral.get("ndwi");
        Double ndbi = spectral.get("ndbi");
        if (ndvi != null) mlFeature.setNdvi(ndvi);
        if (ndwi != null) mlFeature.setNdwi(ndwi);
        if (ndbi != null) mlFeature.setNdbi(ndbi);
        log.info("Applied Sentinel Hub spectral values: NDVI={}, NDWI={}, NDBI={}", ndvi, ndwi, ndbi);
    }

    /** Apply categorical land-cover values retrieved from Copernicus/CLMS dataset. */
    private void applyLandCoverFromSentinel(MlFeature mlFeature, Map<String, Double> landCover) {
        if (landCover.containsKey("bare")) mlFeature.setBare(landCover.get("bare"));
        if (landCover.containsKey("floodedVegetation")) mlFeature.setFloodedVegetation(landCover.get("floodedVegetation"));
        if (landCover.containsKey("grass")) mlFeature.setGrass(landCover.get("grass"));
        if (landCover.containsKey("shrubAndScrub")) mlFeature.setShrubAndScrub(landCover.get("shrubAndScrub"));
        if (landCover.containsKey("snowAndIce")) mlFeature.setSnowAndIce(landCover.get("snowAndIce"));
        if (landCover.containsKey("water")) mlFeature.setWater(landCover.get("water"));
        log.info("Applied real Copernicus land-cover to hotspot");
    }

    /** Fallback spectral estimation based on VIIRS thermal index */
    private void estimateSpectralOnly(Hotspot hotspot, MlFeature mlFeature) {
        double ti4 = hotspot.getBrightTi4() != null ? hotspot.getBrightTi4() : 300.0;
        double ti5 = hotspot.getBrightTi5() != null ? hotspot.getBrightTi5() : 290.0;

        double thermalIndex = (ti4 - ti5) / Math.max(ti5, 1.0);

        double ndvi = Math.max(-0.2, Math.min(0.85, 0.45 - (thermalIndex * 1.5)));
        double ndbi = Math.max(-0.5, Math.min(0.9, 0.10 + (thermalIndex * 2.0)));
        double ndwi = Math.max(-0.5, Math.min(0.5, -0.15 - (thermalIndex * 0.5)));

        mlFeature.setNdvi(round(ndvi, 4));
        mlFeature.setNdbi(round(ndbi, 4));
        mlFeature.setNdwi(round(ndwi, 4));
    }

    /**
     * Fallback land-cover estimation derived from NDVI/NDBI.
     * Preserved as fallback when real land-cover data is unavailable.
     */
    private void estimateLandCoverFromSpectral(MlFeature mlFeature) {
        double ndvi = mlFeature.getNdvi() != null ? mlFeature.getNdvi() : 0.0;
        double ndbi = mlFeature.getNdbi() != null ? mlFeature.getNdbi() : 0.0;

        double bareFraction = Math.max(0.0, Math.min(1.0, 0.30 + (ndbi * 0.4)));
        double grassFraction = Math.max(0.0, Math.min(1.0 - bareFraction, 0.35 + (ndvi * 0.3)));
        double shrubFraction = Math.max(0.0, Math.min(1.0 - bareFraction - grassFraction, 0.20));
        double waterFraction = Math.max(0.0, Math.min(1.0 - bareFraction - grassFraction - shrubFraction, 0.05));

        mlFeature.setBare(round(bareFraction, 4));
        mlFeature.setGrass(round(grassFraction, 4));
        mlFeature.setShrubAndScrub(round(shrubFraction, 4));
        mlFeature.setWater(round(waterFraction, 4));
        mlFeature.setFloodedVegetation(0.0);
        mlFeature.setSnowAndIce(0.0);
    }

    /**
     * Complete fallback estimation method for both spectral and land-cover features.
     * Preserved exactly as existing fallback when both satellite requests fail or API is disabled.
     */
    private void estimateLandCoverAndSpectral(Hotspot hotspot, MlFeature mlFeature) {
        double ti4 = hotspot.getBrightTi4() != null ? hotspot.getBrightTi4() : 300.0;
        double ti5 = hotspot.getBrightTi5() != null ? hotspot.getBrightTi5() : 290.0;

        double thermalIndex = (ti4 - ti5) / Math.max(ti5, 1.0);

        double ndvi = Math.max(-0.2, Math.min(0.85, 0.45 - (thermalIndex * 1.5)));
        double ndbi = Math.max(-0.5, Math.min(0.9, 0.10 + (thermalIndex * 2.0)));
        double ndwi = Math.max(-0.5, Math.min(0.5, -0.15 - (thermalIndex * 0.5)));

        mlFeature.setNdvi(round(ndvi, 4));
        mlFeature.setNdbi(round(ndbi, 4));
        mlFeature.setNdwi(round(ndwi, 4));

        double bareFraction = Math.max(0.0, Math.min(1.0, 0.30 + (ndbi * 0.4)));
        double grassFraction = Math.max(0.0, Math.min(1.0 - bareFraction, 0.35 + (ndvi * 0.3)));
        double shrubFraction = Math.max(0.0, Math.min(1.0 - bareFraction - grassFraction, 0.20));
        double waterFraction = Math.max(0.0, Math.min(1.0 - bareFraction - grassFraction - shrubFraction, 0.05));

        mlFeature.setBare(round(bareFraction, 4));
        mlFeature.setGrass(round(grassFraction, 4));
        mlFeature.setShrubAndScrub(round(shrubFraction, 4));
        mlFeature.setWater(round(waterFraction, 4));
        mlFeature.setFloodedVegetation(0.0);
        mlFeature.setSnowAndIce(0.0);
    }

    public SatelliteGeometryDto toSatelliteGeometryDto(MlFeature feature) {
        if (feature == null) return new SatelliteGeometryDto(0.0, 0.0, 0.0, 1);
        return new SatelliteGeometryDto(
                feature.getShapeLength(),
                feature.getShapeArea(),
                feature.getFrpDw(),
                feature.getImageCount()
        );
    }

    public LandCoverFeaturesDto toLandCoverFeaturesDto(MlFeature feature) {
        if (feature == null) return new LandCoverFeaturesDto(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "UNAVAILABLE", "NONE");
        String prov = feature.getLandCoverProvenance() != null ? feature.getLandCoverProvenance() : "FALLBACK_ESTIMATED";
        String quality = "COPERNICUS_DERIVED".equals(prov) ? "REAL" : "ESTIMATED";
        return new LandCoverFeaturesDto(
                feature.getBare(),
                feature.getFloodedVegetation(),
                feature.getGrass(),
                feature.getShrubAndScrub(),
                feature.getSnowAndIce(),
                feature.getWater(),
                prov,
                quality
        );
    }

    public SpectralFeaturesDto toSpectralFeaturesDto(MlFeature feature) {
        if (feature == null) return new SpectralFeaturesDto(0.0, 0.0, 0.0, "UNAVAILABLE", "NONE");
        String prov = feature.getSpectralProvenance() != null ? feature.getSpectralProvenance() : "FALLBACK_ESTIMATED";
        String quality = "SENTINEL_DERIVED".equals(prov) ? "REAL" : "ESTIMATED";
        return new SpectralFeaturesDto(
                feature.getNdvi(),
                feature.getNdwi(),
                feature.getNdbi(),
                prov,
                quality
        );
    }

    private double round(double val, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(val * scale) / scale;
    }
}