package com.firewatch.firewatch_backend;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Entity.MlFeature;
import com.firewatch.firewatch_backend.Respositry.MlFeatureRepository;
import com.firewatch.firewatch_backend.Service.FeatureEnrichmentService;
import com.firewatch.firewatch_backend.Service.SentinelHubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

class FeatureEnrichmentServiceTest {

    private MlFeatureRepository mlFeatureRepository;
    private SentinelHubService sentinelHubService;
    private FeatureEnrichmentService featureEnrichmentService;

    @BeforeEach
    void setUp() {
        mlFeatureRepository = Mockito.mock(MlFeatureRepository.class);
        sentinelHubService = Mockito.mock(SentinelHubService.class);
        featureEnrichmentService = new FeatureEnrichmentService(mlFeatureRepository);
        ReflectionTestUtils.setField(featureEnrichmentService, "sentinelHubService", sentinelHubService);
        ReflectionTestUtils.setField(featureEnrichmentService, "satelliteApiEnabled", true);

        when(mlFeatureRepository.findByHotspot(any())).thenReturn(Optional.empty());
        when(mlFeatureRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Hotspot createSampleHotspot() {
        Hotspot hotspot = new Hotspot();
        hotspot.setId(1L);
        hotspot.setLatitude(22.30);
        hotspot.setLongitude(70.80);
        hotspot.setAcqDate(LocalDate.of(2026, 8, 27));
        hotspot.setAcqTime(LocalTime.of(12, 0));
        hotspot.setBrightTi4(350.0);
        hotspot.setBrightTi5(295.0);
        hotspot.setFrp(50.0);
        hotspot.setScan(0.39);
        hotspot.setTrack(0.36);
        return hotspot;
    }

    @Test
    void testEnrichHotspotFeaturesFullFallbackWhenSentinelFails() {
        when(sentinelHubService.getSpectralFeatures(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(sentinelHubService.getLandCoverFeatures(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        Hotspot hotspot = createSampleHotspot();
        MlFeature enriched = featureEnrichmentService.enrichHotspotFeatures(hotspot);

        assertNotNull(enriched);
        assertEquals(22.30, enriched.getLatitude());
        assertEquals(70.80, enriched.getLongitude());
        assertNotNull(enriched.getShapeLength());
        assertNotNull(enriched.getShapeArea());
        assertNotNull(enriched.getNdvi());
        assertNotNull(enriched.getNdbi());
        assertNotNull(enriched.getNdwi());
        assertNotNull(enriched.getBare());
        assertNotNull(enriched.getGrass());
    }

    @Test
    void testEnrichHotspotFeaturesRealSpectralAndRealLandCoverSuccess() {
        Map<String, Double> realSpectral = new HashMap<>();
        realSpectral.put("ndvi", 0.6521);
        realSpectral.put("ndwi", -0.1234);
        realSpectral.put("ndbi", -0.3456);

        Map<String, Double> realLandCover = new HashMap<>();
        realLandCover.put("bare", 0.0);
        realLandCover.put("floodedVegetation", 0.0);
        realLandCover.put("grass", 1.0);
        realLandCover.put("shrubAndScrub", 0.0);
        realLandCover.put("snowAndIce", 0.0);
        realLandCover.put("water", 0.0);

        when(sentinelHubService.getSpectralFeatures(anyDouble(), anyDouble())).thenReturn(Optional.of(realSpectral));
        when(sentinelHubService.getLandCoverFeatures(anyDouble(), anyDouble())).thenReturn(Optional.of(realLandCover));

        Hotspot hotspot = createSampleHotspot();
        MlFeature enriched = featureEnrichmentService.enrichHotspotFeatures(hotspot);

        assertNotNull(enriched);
        assertEquals(0.6521, enriched.getNdvi());
        assertEquals(-0.1234, enriched.getNdwi());
        assertEquals(-0.3456, enriched.getNdbi());
        assertEquals(1.0, enriched.getGrass());
        assertEquals(0.0, enriched.getBare());
        assertEquals(0.0, enriched.getWater());
    }

    @Test
    void testPartialSuccessSpectralSucceedsLandCoverFails() {
        Map<String, Double> realSpectral = new HashMap<>();
        realSpectral.put("ndvi", 0.7000);
        realSpectral.put("ndwi", 0.1000);
        realSpectral.put("ndbi", -0.2000);

        when(sentinelHubService.getSpectralFeatures(anyDouble(), anyDouble())).thenReturn(Optional.of(realSpectral));
        when(sentinelHubService.getLandCoverFeatures(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        Hotspot hotspot = createSampleHotspot();
        MlFeature enriched = featureEnrichmentService.enrichHotspotFeatures(hotspot);

        assertNotNull(enriched);
        // Real spectral indices must be preserved
        assertEquals(0.7000, enriched.getNdvi());
        assertEquals(0.1000, enriched.getNdwi());
        assertEquals(-0.2000, enriched.getNdbi());
        // Land cover falls back to estimation method derived from spectral features
        assertNotNull(enriched.getBare());
        assertNotNull(enriched.getGrass());
    }

    @Test
    void testPartialSuccessLandCoverSucceedsSpectralFails() {
        Map<String, Double> realLandCover = new HashMap<>();
        realLandCover.put("bare", 0.0);
        realLandCover.put("floodedVegetation", 0.0);
        realLandCover.put("grass", 0.0);
        realLandCover.put("shrubAndScrub", 0.0);
        realLandCover.put("snowAndIce", 0.0);
        realLandCover.put("water", 1.0);

        when(sentinelHubService.getSpectralFeatures(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(sentinelHubService.getLandCoverFeatures(anyDouble(), anyDouble())).thenReturn(Optional.of(realLandCover));

        Hotspot hotspot = createSampleHotspot();
        MlFeature enriched = featureEnrichmentService.enrichHotspotFeatures(hotspot);

        assertNotNull(enriched);
        // Real land-cover values must be preserved
        assertEquals(1.0, enriched.getWater());
        assertEquals(0.0, enriched.getBare());
        // Spectral falls back to thermal index estimation
        assertNotNull(enriched.getNdvi());
        assertNotNull(enriched.getNdbi());
        assertNotNull(enriched.getNdwi());
    }
}