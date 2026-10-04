package com.firewatch.firewatch_backend.Controller;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Service.HotspotService;
import com.firewatch.firewatch_backend.dto.geojson.GeoJsonFeatureCollectionDto;
import com.firewatch.firewatch_backend.dto.ml.MlPredictionRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hotspots")
public class HotspotController {

    private final HotspotService hotspotService;

    public HotspotController(HotspotService hotspotService) {
        this.hotspotService = hotspotService;
    }

    
    private String normalizeBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    //Search and filter fire hotspots with optional pagination
    @GetMapping
    public ResponseEntity<?> getHotspots(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String classification,
            @RequestParam(required = false) Double minFrp,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        classification = normalizeBlank(classification);
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);

        if (page != null && size != null) {
            Page<Hotspot> pagedResult = hotspotService.filterHotspotsPaged(
                    minLat, maxLat, minLon, maxLon, start, end, classification, minFrp, page, size
            );
            return ResponseEntity.ok(pagedResult);
        }

        List<Hotspot> listResult = hotspotService.filterHotspots(
                minLat, maxLat, minLon, maxLon, start, end, classification, minFrp
        );
        return ResponseEntity.ok(listResult);
    }

    //GIS GeoJSON Endpoint for direct Map overlays (Leaflet, MapLibre, Mapbox)
    @GetMapping("/geojson")
    public ResponseEntity<GeoJsonFeatureCollectionDto> getHotspotsGeoJson(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String classification,
            @RequestParam(required = false) Double minFrp) {

        classification = normalizeBlank(classification);
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);

        GeoJsonFeatureCollectionDto geoJson = hotspotService.getHotspotsAsGeoJson(
                minLat, maxLat, minLon, maxLon, start, end, classification, minFrp
        );
        return ResponseEntity.ok(geoJson);
    }

    //Get single hotspot by ID
    @GetMapping("/{id}")
    public ResponseEntity<Hotspot> getHotspotById(@PathVariable Long id) {
        return hotspotService.getHotspotById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //Inspect the exact ML input payload assembled for ML model
    @GetMapping("/{id}/ml-payload")
    public ResponseEntity<MlPredictionRequestDto> getMlPayload(@PathVariable Long id) {
        MlPredictionRequestDto payload = hotspotService.getMlPayloadForHotspot(id);
        return ResponseEntity.ok(payload);
    }

    //Trigger ML classification for a specific hotspot
    @PostMapping("/{id}/classify")
    public ResponseEntity<Hotspot> classifyHotspot(@PathVariable Long id) {
        Hotspot classified = hotspotService.classifyHotspot(id);
        return ResponseEntity.ok(classified);
    }

    //Batch classify all pending unclassified hotspots
    @PostMapping("/classify-all")
    public ResponseEntity<List<Hotspot>> classifyAllPending() {
        List<Hotspot> results = hotspotService.classifyAllPending();
        return ResponseEntity.ok(results);
    }

    //Create/save a manual hotspot
    @PostMapping
    public ResponseEntity<Hotspot> createHotspot(@RequestBody Hotspot hotspot) {
        Hotspot saved = hotspotService.saveHotspot(hotspot);
        return ResponseEntity.ok(saved);
    }

    
    // Delete a hotspot by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotspot(@PathVariable Long id) {
        hotspotService.deleteHotspot(id);
        return ResponseEntity.noContent().build();
    }
}