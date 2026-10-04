package com.firewatch.firewatch_backend.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SentinelHubService {

    private static final Logger log = LoggerFactory.getLogger(SentinelHubService.class);

    // ESA WorldCover 10m (2020/2021) — available on services.sentinel-hub.com
    // Identical 11-class nomenclature to CLMS 10m: 10=Tree cover, 20=Shrubland, 30=Grassland,
    // 40=Cropland, 50=Built-up, 60=Bare, 70=Snow/Ice, 80=Water, 90=Wetland, 95=Mangroves, 100=Moss
    public static final String CLMS_10M_COLLECTION_ID = "0b940c63-45dd-4e6b-8019-c3660b81b884";
    public static final String CLMS_10M_BYOC_DATA_TYPE = "byoc-0b940c63-45dd-4e6b-8019-c3660b81b884";
    public static final String CLMS_10M_BAND = "Map";

    @Value("${sentinel.hub.token-url:https://services.sentinel-hub.com/auth/realms/main/protocol/openid-connect/token}")
    private String tokenUrl;

    @Value("${sentinel.hub.process-url:https://services.sentinel-hub.com/api/v1/statistics}")
    private String processUrl;

    @Value("${sentinel.hub.client-id:}")
    private String clientId;

    @Value("${sentinel.hub.client-secret:}")
    private String clientSecret;

    @Value("${sentinel.hub.land-cover-dataset:byoc-0b940c63-45dd-4e6b-8019-c3660b81b884}")
    private String landCoverDataset;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String cachedToken = null;
    private volatile long tokenExpiryMs = 0L;

    public SentinelHubService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Returns NDVI, NDWI, NDBI from real Sentinel-2 data for the given coordinate.
     * Returns empty if credentials are missing, API is unreachable, or no data is available.
     */
    public Optional<Map<String, Double>> getSpectralFeatures(double lat, double lon) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.debug("Sentinel Hub credentials not configured, skipping real satellite fetch.");
            return Optional.empty();
        }

        try {
            String token = getAccessToken();
            if (token == null) return Optional.empty();

            double delta = 0.005;
            double minLon = lon - delta;
            double minLat = lat - delta;
            double maxLon = lon + delta;
            double maxLat = lat + delta;

            String dateTo = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String dateFrom = LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE);

            String requestBody = buildStatsRequest(minLon, minLat, maxLon, maxLat, dateFrom, dateTo);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    processUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("Sentinel Hub stats API returned {}", response.getStatusCode());
                return Optional.empty();
            }

            return parseSpectralIndices(response.getBody());

        } catch (Exception e) {
            log.warn("Sentinel Hub spectral request failed for ({}, {}): {}", lat, lon, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns real Copernicus/CLMS global 10m land-cover classification for the given coordinate.
     * Queries official CLMS Land Cover Map at 10m (Annual V1, BYOC collection 828f6b20-8ffd-48f8-a1da-fefd271456db).
     * Determines the dominant land-cover class from the categorical histogram distribution of valid pixels.
     * Maps deterministic categorical land-cover classes to existing ML feature fields:
     * bare, floodedVegetation, grass, shrubAndScrub, snowAndIce, water (1.0 for matching class, 0.0 otherwise).
     * Returns empty if credentials are missing, API is unreachable, or no valid pixels are available.
     */
    public Optional<Map<String, Double>> getLandCoverFeatures(double lat, double lon) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.debug("Sentinel Hub credentials not configured, skipping real land-cover fetch.");
            return Optional.empty();
        }

        try {
            String token = getAccessToken();
            if (token == null) return Optional.empty();

            double delta = 0.005; // ~0.5 km bounding box around hotspot
            double minLon = lon - delta;
            double minLat = lat - delta;
            double maxLon = lon + delta;
            double maxLat = lat + delta;

            String requestBody = buildLandCoverStatsRequest(minLon, minLat, maxLon, maxLat);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("Requesting Sentinel Hub Copernicus CLMS 10m land-cover data for ({}, {})", lat, lon);
            ResponseEntity<String> response = restTemplate.exchange(
                    processUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("Sentinel Hub land-cover stats API returned {}", response.getStatusCode());
                return Optional.empty();
            }

            return parseLandCoverResponse(response.getBody());

        } catch (Exception e) {
            log.warn("Sentinel Hub land-cover request failed for ({}, {}): {}", lat, lon, e.getMessage());
            return Optional.empty();
        }
    }

    private String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiryMs) {
            return cachedToken;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("Sentinel Hub token request failed: {}", response.getStatusCode());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String accessToken = root.path("access_token").asText(null);
            int expiresIn = root.path("expires_in").asInt(3600);

            cachedToken = accessToken;
            tokenExpiryMs = now + ((long) expiresIn - 60) * 1000L;

            log.debug("Sentinel Hub token obtained, expires in {} s", expiresIn);
            return accessToken;

        } catch (Exception e) {
            log.warn("Sentinel Hub token fetch failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildStatsRequest(double minLon, double minLat,
                                     double maxLon, double maxLat,
                                     String dateFrom, String dateTo) {
        String evalscript =
            "//VERSION=3\n" +
            "function setup() {\n" +
            "  return {\n" +
            "    input: [{ bands: [\"B03\", \"B04\", \"B08\", \"B11\", \"dataMask\"], units: \"REFLECTANCE\" }],\n" +
            "    output: [\n" +
            "      { id: \"bands\", bands: 4 },\n" +
            "      { id: \"dataMask\", bands: 1 }\n" +
            "    ]\n" +
            "  };\n" +
            "}\n" +
            "function evaluatePixel(s) {\n" +
            "  return {\n" +
            "    bands: [s.B03, s.B04, s.B08, s.B11],\n" +
            "    dataMask: [s.dataMask]\n" +
            "  };\n" +
            "}";

        try {
            String escapedEvalscript = objectMapper.writeValueAsString(evalscript);
            return String.format(
                "{" +
                "  \"input\": {" +
                "    \"bounds\": {" +
                "      \"bbox\": [%f, %f, %f, %f]," +
                "      \"properties\": { \"crs\": \"http://www.opengis.net/def/crs/EPSG/0/4326\" }" +
                "    }," +
                "    \"data\": [{" +
                "      \"type\": \"sentinel-2-l2a\"," +
                "      \"dataFilter\": {" +
                "        \"timeRange\": { \"from\": \"%sT00:00:00Z\", \"to\": \"%sT23:59:59Z\" }," +
                "        \"maxCloudCoverage\": 60" +
                "      }" +
                "    }]" +
                "  }," +
                "  \"aggregation\": {" +
                "    \"timeRange\": { \"from\": \"%sT00:00:00Z\", \"to\": \"%sT23:59:59Z\" }," +
                "    \"aggregationInterval\": { \"of\": \"P30D\" }," +
                "    \"evalscript\": %s," +
                "    \"resx\": 10," +
                "    \"resy\": 10" +
                "  }" +
                "}",
                minLon, minLat, maxLon, maxLat,
                dateFrom, dateTo,
                dateFrom, dateTo,
                escapedEvalscript
            );
        } catch (Exception e) {
            log.warn("Failed to build Sentinel Hub spectral request: {}", e.getMessage());
            return "{}";
        }
    }

    private String buildLandCoverStatsRequest(double minLon, double minLat,
                                              double maxLon, double maxLat) {
        String datasetType = (landCoverDataset != null && !landCoverDataset.isBlank())
                ? landCoverDataset : CLMS_10M_BYOC_DATA_TYPE;

        String bandName = CLMS_10M_BAND;
        String timeFrom = "2021-01-01";
        String timeTo = "2021-12-31";
        int res = 10;

        // Evalscript masks out invalid pixels (dataMask === 0) and no-data class pixels (LCM10 === 0)
        String evalscript = String.format(
            "//VERSION=3\n" +
            "function setup() {\n" +
            "  return {\n" +
            "    input: [\"%s\", \"dataMask\"],\n" +
            "    output: [\n" +
            "      { id: \"default\", bands: 1, sampleType: \"UINT8\" },\n" +
            "      { id: \"dataMask\", bands: 1, sampleType: \"UINT8\" }\n" +
            "    ]\n" +
            "  };\n" +
            "}\n" +
            "function evaluatePixel(sample) {\n" +
            "  var isValid = (sample.dataMask === 1 && sample.%s > 0) ? 1 : 0;\n" +
            "  return {\n" +
            "    default: [sample.%s],\n" +
            "    dataMask: [isValid]\n" +
            "  };\n" +
            "}",
            bandName, bandName, bandName
        );

        try {
            String escapedEvalscript = objectMapper.writeValueAsString(evalscript);
            return String.format(
                "{" +
                "  \"input\": {" +
                "    \"bounds\": {" +
                "      \"bbox\": [%f, %f, %f, %f]," +
                "      \"properties\": { \"crs\": \"http://www.opengis.net/def/crs/EPSG/0/4326\" }" +
                "    }," +
                "    \"data\": [{" +
                "      \"type\": \"%s\"," +
                "      \"dataFilter\": {" +
                "        \"timeRange\": { \"from\": \"%sT00:00:00Z\", \"to\": \"%sT23:59:59Z\" }" +
                "      }" +
                "    }]" +
                "  }," +
                "  \"aggregation\": {" +
                "    \"timeRange\": { \"from\": \"%sT00:00:00Z\", \"to\": \"%sT23:59:59Z\" }," +
                "    \"aggregationInterval\": { \"of\": \"P365D\" }," +
                "    \"evalscript\": %s," +
                "    \"resx\": %d," +
                "    \"resy\": %d" +
                "  }," +
                "  \"calculations\": {" +
                "    \"default\": {" +
                "      \"histograms\": {" +
                "        \"default\": {" +
                "          \"binWidth\": 1," +
                "          \"lowEdge\": 0," +
                "          \"highEdge\": 105" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}",
                minLon, minLat, maxLon, maxLat,
                datasetType,
                timeFrom, timeTo,
                timeFrom, timeTo,
                escapedEvalscript,
                res, res
            );
        } catch (Exception e) {
            log.warn("Failed to build Sentinel Hub land-cover request: {}", e.getMessage());
            return "{}";
        }
    }

    private Optional<Map<String, Double>> parseSpectralIndices(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                log.debug("Sentinel Hub: no data in spectral statistics response");
                return Optional.empty();
            }

            JsonNode bandsNode = data.get(0).path("outputs").path("bands").path("bands");

            double b03 = bandsNode.path("B0").path("stats").path("mean").asDouble(0.0);
            double b04 = bandsNode.path("B1").path("stats").path("mean").asDouble(0.0);
            double b08 = bandsNode.path("B2").path("stats").path("mean").asDouble(0.0);
            double b11 = bandsNode.path("B3").path("stats").path("mean").asDouble(0.0);

            if (b03 == 0 && b04 == 0 && b08 == 0 && b11 == 0) {
                log.debug("Sentinel Hub: all band means zero, no valid spectral pixels found");
                return Optional.empty();
            }

            double ndvi = safeIndex(b08, b04);
            double ndwi = safeIndex(b03, b08);
            double ndbi = safeIndex(b11, b08);

            Map<String, Double> result = new HashMap<>();
            result.put("ndvi", round(ndvi, 4));
            result.put("ndwi", round(ndwi, 4));
            result.put("ndbi", round(ndbi, 4));

            log.info("Sentinel Hub spectral indices: NDVI={}, NDWI={}, NDBI={}", ndvi, ndwi, ndbi);
            return Optional.of(result);

        } catch (Exception e) {
            log.warn("Sentinel Hub spectral response parse error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Determines the dominant land-cover class from the categorical histogram distribution of valid pixels.
     * Invalid/no-data pixels (code 0) and masked pixels are excluded.
     * If no valid pixels exist, returns Optional.empty() so the existing fallback is invoked.
     */
    public Optional<Map<String, Double>> parseLandCoverResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                log.debug("Sentinel Hub: no data in land-cover statistics response");
                return Optional.empty();
            }

            JsonNode b0Node = data.get(0).path("outputs").path("default").path("bands").path("B0");
            JsonNode binsNode = b0Node.path("histogram").path("bins");
            if (!binsNode.isArray() || binsNode.isEmpty()) {
                binsNode = b0Node.path("histograms").path("default").path("bins");
            }
            if (!binsNode.isArray() || binsNode.isEmpty()) {
                binsNode = b0Node.path("histograms").path("bins");
            }

            if (!binsNode.isArray() || binsNode.isEmpty()) {
                log.debug("Sentinel Hub: no histogram bins found in land-cover response");
                return Optional.empty();
            }

            int dominantClass = -1;
            long maxCount = 0;
            long totalValidPixels = 0;

            for (JsonNode bin : binsNode) {
                long count = bin.path("count").asLong(0);
                if (count <= 0) continue;

                double lowEdge = bin.path("lowEdge").asDouble(-1.0);
                int classCode = (int) Math.round(lowEdge);

                // Exclude invalid/no-data pixels (code 0 or negative)
                if (classCode <= 0) {
                    continue;
                }

                totalValidPixels += count;
                if (count > maxCount) {
                    maxCount = count;
                    dominantClass = classCode;
                }
            }

            if (totalValidPixels == 0 || dominantClass <= 0) {
                log.info("Sentinel Hub: No valid land-cover pixels in AOI - falling back to estimation");
                return Optional.empty();
            }

            log.info("Sentinel Hub Copernicus CLMS 10m land-cover dominant class: {} ({} of {} valid pixels)",
                    dominantClass, maxCount, totalValidPixels);

            Map<String, Double> result = mapCategoricalLandCover(dominantClass);
            return Optional.of(result);

        } catch (Exception e) {
            log.warn("Sentinel Hub land-cover response parse error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Maps Copernicus Land Monitoring Service (CLMS) 10m Land Cover Map (Annual V1, collection 828f6b20-8ffd-48f8-a1da-fefd271456db)
     * discrete classification classes to existing FireWatch ML feature fields.
     *
     * Official Copernicus CLMS 10m Land Cover classification nomenclature (UN-FAO LCCS):
     *  10 -> Tree cover
     *  20 -> Shrubland
     *  30 -> Grassland
     *  40 -> Cropland
     *  50 -> Built-up
     *  60 -> Bare / sparse vegetation
     *  70 -> Snow and ice
     *  80 -> Permanent water bodies
     *  90 -> Herbaceous wetland
     *  95 -> Mangroves
     * 100 -> Moss and lichen
     *   0 -> No data
     *
     * Deterministic mapping to existing FireWatch ML fields (1.0 for matching class, 0.0 otherwise):
     * - bare:              Code 60 (Bare / sparse vegetation)
     * - floodedVegetation: Code 90 (Herbaceous wetland), Code 95 (Mangroves)
     * - grass:             Code 30 (Grassland)
     * - shrubAndScrub:     Code 20 (Shrubland), Code 100 (Moss and lichen)
     * - snowAndIce:        Code 70 (Snow and ice)
     * - water:             Code 80 (Permanent water bodies)
     * Non-applicable classes (Code 10 Tree cover, Code 40 Cropland, Code 50 Built-up): deterministically 0.0.
     */
    public Map<String, Double> mapCategoricalLandCover(int code) {
        Map<String, Double> features = new HashMap<>();
        features.put("bare", 0.0);
        features.put("floodedVegetation", 0.0);
        features.put("grass", 0.0);
        features.put("shrubAndScrub", 0.0);
        features.put("snowAndIce", 0.0);
        features.put("water", 0.0);

        switch (code) {
            case 60 -> features.put("bare", 1.0);
            case 90, 95 -> features.put("floodedVegetation", 1.0);
            case 30 -> features.put("grass", 1.0);
            case 20, 100 -> features.put("shrubAndScrub", 1.0);
            case 70 -> features.put("snowAndIce", 1.0);
            case 80 -> features.put("water", 1.0);
            default -> {
                // Code 10 (Tree cover), 40 (Cropland), 50 (Built-up), or 0 (No data)
                // are not in the 6 FireWatch specific land classes and remain 0.0.
            }
        }

        return features;
    }

    private double safeIndex(double a, double b) {
        double denom = a + b;
        return denom == 0.0 ? 0.0 : (a - b) / denom;
    }

    private double round(double val, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(val * scale) / scale;
    }
}