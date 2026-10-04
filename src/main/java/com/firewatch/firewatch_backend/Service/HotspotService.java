package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Entity.MlFeature;
import com.firewatch.firewatch_backend.Respositry.HotspotRepository;
import com.firewatch.firewatch_backend.dto.geojson.GeoJsonFeatureCollectionDto;
import com.firewatch.firewatch_backend.dto.ml.MlPredictionRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HotspotService {

    private final HotspotRepository hotspotRepository;
    private final FeatureEnrichmentService featureEnrichmentService;
    private final MlServiceClient mlServiceClient;
    private final GeoJsonService geoJsonService;

    public HotspotService(
            HotspotRepository hotspotRepository,
            FeatureEnrichmentService featureEnrichmentService,
            MlServiceClient mlServiceClient,
            GeoJsonService geoJsonService) {
        this.hotspotRepository = hotspotRepository;
        this.featureEnrichmentService = featureEnrichmentService;
        this.mlServiceClient = mlServiceClient;
        this.geoJsonService = geoJsonService;
    }

    public Hotspot saveHotspot(Hotspot hotspot) {
        Hotspot saved = hotspotRepository.save(hotspot);
        featureEnrichmentService.enrichHotspotFeatures(saved);
        return saved;
    }

    public List<Hotspot> getAllHotspots() {
        return hotspotRepository.findAll();
    }

    public Optional<Hotspot> getHotspotById(Long id) {
        return hotspotRepository.findById(id);
    }

    public List<Hotspot> filterHotspots(
            Double minLat, Double maxLat,
            Double minLon, Double maxLon,
            LocalDate startDate, LocalDate endDate,
            String classification, Double minFrp) {
        return hotspotRepository.filterHotspots(
                minLat, maxLat, minLon, maxLon, startDate, endDate, classification, minFrp
        );
    }

    public Page<Hotspot> filterHotspotsPaged(
            Double minLat, Double maxLat,
            Double minLon, Double maxLon,
            LocalDate startDate, LocalDate endDate,
            String classification, Double minFrp,
            int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "acqDate", "acqTime"));
        return hotspotRepository.filterHotspotsPaged(
                minLat, maxLat, minLon, maxLon, startDate, endDate, classification, minFrp, pageable
        );
    }

    public GeoJsonFeatureCollectionDto getHotspotsAsGeoJson(
            Double minLat, Double maxLat,
            Double minLon, Double maxLon,
            LocalDate startDate, LocalDate endDate,
            String classification, Double minFrp) {
        List<Hotspot> hotspots = filterHotspots(
                minLat, maxLat, minLon, maxLon, startDate, endDate, classification, minFrp
        );
        return geoJsonService.toGeoJson(hotspots);
    }

    public MlPredictionRequestDto getMlPayloadForHotspot(Long id) {
        Hotspot hotspot = hotspotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hotspot not found with ID: " + id));

        MlFeature feature = featureEnrichmentService.enrichHotspotFeatures(hotspot);
        return mlServiceClient.buildMlPayload(hotspot, feature);
    }

    @Transactional
    public Hotspot classifyHotspot(Long id) {
        Hotspot hotspot = hotspotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hotspot not found with ID: " + id));

        return mlServiceClient.classifyHotspot(hotspot);
    }

    @Transactional
    public List<Hotspot> classifyAllPending() {
        List<Hotspot> unclassified = hotspotRepository.findByClassificationIsNull();
        List<Hotspot> results = new ArrayList<>();
        for (Hotspot h : unclassified) {
            results.add(mlServiceClient.classifyHotspot(h));
        }
        return results;
    }

    public void deleteHotspot(Long id) {
        hotspotRepository.deleteById(id);
    }
}