package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Respositry.HotspotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class FirmsService {

    private static final Logger log = LoggerFactory.getLogger(FirmsService.class);

    @Value("${nasa.firms.map-key:}")
    private String mapKey;

    @Value("${nasa.firms.base-url:https://firms.modaps.eosdis.nasa.gov/api/area/csv}")
    private String baseUrl;

    @Value("${nasa.firms.default-source:VIIRS_SNPP_NRT}")
    private String defaultSource;

    @Value("${nasa.firms.default-days:5}")
    private int defaultDays;

    private final RestTemplate restTemplate;
    private final FirmsCsvParser firmsCsvParser;
    private final HotspotRepository hotspotRepository;
    private final FeatureEnrichmentService featureEnrichmentService;
    private final MlServiceClient mlServiceClient;

    public FirmsService(
            RestTemplate restTemplate,
            FirmsCsvParser firmsCsvParser,
            HotspotRepository hotspotRepository,
            FeatureEnrichmentService featureEnrichmentService,
            MlServiceClient mlServiceClient) {
        this.restTemplate = restTemplate;
        this.firmsCsvParser = firmsCsvParser;
        this.hotspotRepository = hotspotRepository;
        this.featureEnrichmentService = featureEnrichmentService;
        this.mlServiceClient = mlServiceClient;
    }

    public List<Hotspot> fetchAndSaveHotspots(double minLon, double minLat, double maxLon, double maxLat) {
        return fetchAndSaveHotspots(minLon, minLat, maxLon, maxLat, defaultSource, defaultDays, true);
    }

    @Transactional
    public List<Hotspot> fetchAndSaveHotspots(
            double minLon,
            double minLat,
            double maxLon,
            double maxLat,
            String source,
            Integer days,
            boolean autoClassify) {

        if (minLon >= maxLon || minLat >= maxLat) {
            throw new IllegalArgumentException("Invalid bounding box: minLon must be < maxLon and minLat must be < maxLat");
        }

        if (mapKey == null || mapKey.isBlank() || mapKey.startsWith("YOUR_")) {
            log.warn("NASA FIRMS Map Key is not configured. Please set NASA_FIRMS_MAP_KEY in application.properties or environment.");
            throw new IllegalStateException("NASA FIRMS Map Key is not configured. Please provide a valid NASA FIRMS map key.");
        }

        String dataset = (source != null && !source.isBlank()) ? source : defaultSource;
        int dayCount = (days != null && days >= 1 && days <= 10) ? days : defaultDays;

        String bbox = minLon + "," + minLat + "," + maxLon + "," + maxLat;
        String url = baseUrl + "/" + mapKey + "/" + dataset + "/" + bbox + "/" + dayCount;

        log.info("Fetching FIRMS data from URL: {}", url.replace(mapKey, "REDACTED_KEY"));

        String csv;
        try {
            csv = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("Error communicating with NASA FIRMS API: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch data from NASA FIRMS API: " + e.getMessage(), e);
        }

        if (csv == null || csv.isBlank()) {
            log.info("No fire hotspots returned by NASA FIRMS for bbox: {}", bbox);
            return new ArrayList<>();
        }

        List<Hotspot> parsedHotspots = firmsCsvParser.parse(csv);
        log.info("Parsed {} hotspots from NASA FIRMS CSV", parsedHotspots.size());

        // Deduplicate against existing records in database
        List<Hotspot> newHotspots = parsedHotspots.stream()
                .filter(hotspot ->
                        hotspotRepository
                                .findByLatitudeAndLongitudeAndAcqDateAndAcqTimeAndSatellite(
                                        hotspot.getLatitude(),
                                        hotspot.getLongitude(),
                                        hotspot.getAcqDate(),
                                        hotspot.getAcqTime(),
                                        hotspot.getSatellite()
                                )
                                .isEmpty()
                )
                .toList();

        log.info("Saving {} new unique hotspots to database", newHotspots.size());
        List<Hotspot> savedHotspots = hotspotRepository.saveAll(newHotspots);

        // Enrich features and run classification
        List<Hotspot> processedHotspots = new ArrayList<>();
        for (Hotspot h : savedHotspots) {
            featureEnrichmentService.enrichHotspotFeatures(h);
            if (autoClassify) {
                h = mlServiceClient.classifyHotspot(h);
            }
            processedHotspots.add(h);
        }

        return processedHotspots;
    }
}