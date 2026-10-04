package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.dto.geojson.GeoJsonFeatureCollectionDto;
import com.firewatch.firewatch_backend.dto.geojson.GeoJsonFeatureDto;
import com.firewatch.firewatch_backend.dto.geojson.GeoJsonGeometryDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeoJsonService {

    // Convert hotspots to GeoJSON FeatureCollection
    public GeoJsonFeatureCollectionDto toGeoJson(List<Hotspot> hotspots) {
        if (hotspots == null || hotspots.isEmpty()) {
            return new GeoJsonFeatureCollectionDto(new ArrayList<>());
        }

        List<GeoJsonFeatureDto> features = hotspots.stream()
                .filter(h -> h.getLatitude() != null && h.getLongitude() != null)
                .map(this::toFeature)
                .toList();

        return new GeoJsonFeatureCollectionDto(features);
    }

    public GeoJsonFeatureDto toFeature(Hotspot hotspot) {
        GeoJsonGeometryDto geometry = new GeoJsonGeometryDto(
                hotspot.getLongitude(),
                hotspot.getLatitude()
        );

        Map<String, Object> props = new HashMap<>();
        props.put("id", hotspot.getId());
        props.put("latitude", hotspot.getLatitude());
        props.put("longitude", hotspot.getLongitude());
        props.put("brightness", hotspot.getBrightness());
        props.put("bright_ti4", hotspot.getBrightTi4());
        props.put("bright_ti5", hotspot.getBrightTi5());
        props.put("frp", hotspot.getFrp());
        props.put("confidence", hotspot.getConfidence());
        props.put("acq_date", hotspot.getAcqDate() != null ? hotspot.getAcqDate().toString() : null);
        props.put("acq_time", hotspot.getAcqTime() != null ? hotspot.getAcqTime().toString() : null);
        props.put("satellite", hotspot.getSatellite());
        props.put("instrument", hotspot.getInstrument());
        props.put("daynight", hotspot.getDaynight());
        props.put("source", hotspot.getSource());
        props.put("classification", hotspot.getClassification());
        props.put("ml_confidence", hotspot.getMlConfidence());
        props.put("inference_method", hotspot.getInferenceMethod());

        // Decision-support attributes
        props.put("risk_score", hotspot.getRiskScore());
        props.put("risk_level", hotspot.getRiskLevel());
        props.put("evidence", hotspot.getEvidence());
        props.put("recommended_action", hotspot.getRecommendedAction());
        props.put("persistence_count", hotspot.getPersistenceCount());
        props.put("active_days", hotspot.getActiveDays());

        if (hotspot.getMlFeature() != null) {
            props.put("spectral_provenance", hotspot.getMlFeature().getSpectralProvenance());
            props.put("land_cover_provenance", hotspot.getMlFeature().getLandCoverProvenance());
        }

        boolean isIndustrial = hotspot.getClassification() != null &&
                (hotspot.getClassification().toUpperCase().contains("INDUSTRIAL") ||
                 hotspot.getClassification().toUpperCase().contains("PERSISTENT"));
        props.put("is_industrial", isIndustrial);

        return new GeoJsonFeatureDto(geometry, props);
    }
}
