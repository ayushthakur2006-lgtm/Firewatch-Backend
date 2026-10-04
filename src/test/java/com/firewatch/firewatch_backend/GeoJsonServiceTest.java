package com.firewatch.firewatch_backend;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Service.GeoJsonService;
import com.firewatch.firewatch_backend.dto.geojson.GeoJsonFeatureCollectionDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonServiceTest {

    private final GeoJsonService geoJsonService = new GeoJsonService();

    @Test
    void testToGeoJson() {
        Hotspot hotspot = new Hotspot();
        hotspot.setId(101L);
        hotspot.setLatitude(21.1702);
        hotspot.setLongitude(72.8311);
        hotspot.setBrightness(345.6);
        hotspot.setFrp(42.5);
        hotspot.setAcqDate(LocalDate.of(2026, 8, 27));
        hotspot.setAcqTime(LocalTime.of(14, 30));
        hotspot.setClassification("INDUSTRIAL_FIRE");
        hotspot.setMlConfidence(0.94);

        GeoJsonFeatureCollectionDto geoJson = geoJsonService.toGeoJson(List.of(hotspot));

        assertNotNull(geoJson);
        assertEquals("FeatureCollection", geoJson.getType());
        assertEquals(1, geoJson.getFeatures().size());

        var feature = geoJson.getFeatures().get(0);
        assertEquals("Feature", feature.getType());
        assertEquals("Point", feature.getGeometry().getType());
        assertEquals(72.8311, feature.getGeometry().getCoordinates().get(0)); // Longitude first
        assertEquals(21.1702, feature.getGeometry().getCoordinates().get(1)); // Latitude second
        assertEquals("INDUSTRIAL_FIRE", feature.getProperties().get("classification"));
        assertEquals(true, feature.getProperties().get("is_industrial"));
    }
}
