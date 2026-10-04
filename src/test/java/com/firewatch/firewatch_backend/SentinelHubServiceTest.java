package com.firewatch.firewatch_backend;

import com.firewatch.firewatch_backend.Service.SentinelHubService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SentinelHubServiceTest {

    @Test
    void testGetSpectralFeaturesWithoutCredentialsReturnsEmpty() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        SentinelHubService service = new SentinelHubService(restTemplate);

        Optional<Map<String, Double>> result = service.getSpectralFeatures(28.6139, 77.2090);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLandCoverFeaturesWithoutCredentialsReturnsEmpty() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        SentinelHubService service = new SentinelHubService(restTemplate);

        Optional<Map<String, Double>> result = service.getLandCoverFeatures(28.6139, 77.2090);

        assertTrue(result.isEmpty());
    }

    @Test
    void testDominantClassSelectedFromHistogram_30_30_30_80_80() {
        // User example: pixel counts are 3 for class 30 (Grassland) and 2 for class 80 (Water)
        // Mean would be (30*3 + 80*2)/5 = 50 (Built-up) or 52 (invalid class).
        // Correct categorical histogram must select class 30 (dominant).
        String histogramJson = """
            {
              "data": [
                {
                  "outputs": {
                    "default": {
                      "bands": {
                        "B0": {
                          "histogram": {
                            "bins": [
                              { "lowEdge": 30.0, "highEdge": 31.0, "count": 3 },
                              { "lowEdge": 80.0, "highEdge": 81.0, "count": 2 }
                            ]
                          }
                        }
                      }
                    }
                  }
                }
              ]
            }
            """;

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        SentinelHubService service = new SentinelHubService(restTemplate);

        Optional<Map<String, Double>> result = service.parseLandCoverResponse(histogramJson);

        assertTrue(result.isPresent(), "Dominant class must be parsed successfully");
        Map<String, Double> features = result.get();
        assertEquals(1.0, features.get("grass"), "Class 30 (Grassland) must be selected as dominant");
        assertEquals(0.0, features.get("water"));
        assertEquals(0.0, features.get("bare"));
    }

    @Test
    void testNoDataPixelsAreIgnoredInHistogram() {
        // Class 0 (no data) has 500 pixels, but must be ignored.
        // Class 60 (Bare) has 25 pixels, Class 80 (Water) has 10 pixels -> Dominant valid class = 60
        String histogramJson = """
            {
              "data": [
                {
                  "outputs": {
                    "default": {
                      "bands": {
                        "B0": {
                          "histogram": {
                            "bins": [
                              { "lowEdge": 0.0, "highEdge": 1.0, "count": 500 },
                              { "lowEdge": 60.0, "highEdge": 61.0, "count": 25 },
                              { "lowEdge": 80.0, "highEdge": 81.0, "count": 10 }
                            ]
                          }
                        }
                      }
                    }
                  }
                }
              ]
            }
            """;

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        SentinelHubService service = new SentinelHubService(restTemplate);

        Optional<Map<String, Double>> result = service.parseLandCoverResponse(histogramJson);

        assertTrue(result.isPresent());
        Map<String, Double> features = result.get();
        assertEquals(1.0, features.get("bare"), "Class 60 (Bare) must be selected, ignoring class 0");
        assertEquals(0.0, features.get("water"));
    }

    @Test
    void testNoValidPixelsReturnsEmptyTriggeringFallback() {
        // Only no-data pixels (code 0) exist in histogram
        String histogramJson = """
            {
              "data": [
                {
                  "outputs": {
                    "default": {
                      "bands": {
                        "B0": {
                          "histogram": {
                            "bins": [
                              { "lowEdge": 0.0, "highEdge": 1.0, "count": 150 }
                            ]
                          }
                        }
                      }
                    }
                  }
                }
              ]
            }
            """;

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        SentinelHubService service = new SentinelHubService(restTemplate);

        Optional<Map<String, Double>> result = service.parseLandCoverResponse(histogramJson);

        assertTrue(result.isEmpty(), "When no valid pixels exist, Optional.empty() must be returned to trigger fallback");
    }

    @Test
    void testEmptyOrMalformedHistogramReturnsEmpty() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        SentinelHubService service = new SentinelHubService(restTemplate);

        assertTrue(service.parseLandCoverResponse("{}").isEmpty());
        assertTrue(service.parseLandCoverResponse("{\"data\":[]}").isEmpty());
    }

    @Test
    void testMapCategoricalLandCoverCLMS10m() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        SentinelHubService service = new SentinelHubService(restTemplate);

        // Water: Code 80 (Permanent water bodies)
        Map<String, Double> waterMap = service.mapCategoricalLandCover(80);
        assertEquals(1.0, waterMap.get("water"));
        assertEquals(0.0, waterMap.get("bare"));
        assertEquals(0.0, waterMap.get("grass"));
        assertEquals(0.0, waterMap.get("shrubAndScrub"));
        assertEquals(0.0, waterMap.get("floodedVegetation"));
        assertEquals(0.0, waterMap.get("snowAndIce"));

        // Bare: Code 60 (Bare / sparse vegetation)
        Map<String, Double> bareMap = service.mapCategoricalLandCover(60);
        assertEquals(1.0, bareMap.get("bare"));
        assertEquals(0.0, bareMap.get("water"));

        // Grass: Code 30 (Grassland)
        Map<String, Double> grassMap = service.mapCategoricalLandCover(30);
        assertEquals(1.0, grassMap.get("grass"));
        assertEquals(0.0, grassMap.get("bare"));

        // Shrubland: Code 20 (Shrubland) and Code 100 (Moss and lichen)
        Map<String, Double> shrubMap20 = service.mapCategoricalLandCover(20);
        assertEquals(1.0, shrubMap20.get("shrubAndScrub"));
        assertEquals(0.0, shrubMap20.get("grass"));

        Map<String, Double> shrubMap100 = service.mapCategoricalLandCover(100);
        assertEquals(1.0, shrubMap100.get("shrubAndScrub"));

        // Wetland / Flooded vegetation: Code 90 (Herbaceous wetland) and Code 95 (Mangroves)
        Map<String, Double> wetMap90 = service.mapCategoricalLandCover(90);
        assertEquals(1.0, wetMap90.get("floodedVegetation"));
        assertEquals(0.0, wetMap90.get("water"));

        Map<String, Double> wetMap95 = service.mapCategoricalLandCover(95);
        assertEquals(1.0, wetMap95.get("floodedVegetation"));

        // Snow and ice: Code 70
        Map<String, Double> snowMap = service.mapCategoricalLandCover(70);
        assertEquals(1.0, snowMap.get("snowAndIce"));

        // Tree cover (Code 10), Cropland (Code 40), Built-up (Code 50), No-data (Code 0): all 0.0
        for (int nonMatchingCode : new int[]{10, 40, 50, 0}) {
            Map<String, Double> otherMap = service.mapCategoricalLandCover(nonMatchingCode);
            assertEquals(0.0, otherMap.get("bare"));
            assertEquals(0.0, otherMap.get("floodedVegetation"));
            assertEquals(0.0, otherMap.get("grass"));
            assertEquals(0.0, otherMap.get("shrubAndScrub"));
            assertEquals(0.0, otherMap.get("snowAndIce"));
            assertEquals(0.0, otherMap.get("water"));
        }
    }
}