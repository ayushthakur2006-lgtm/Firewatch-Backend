package com.firewatch.firewatch_backend.Controller;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Service.FirmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/firms")
public class FirmsController {

    private final FirmsService firmsService;

    public FirmsController(FirmsService firmsService) {
        this.firmsService = firmsService;
    }

    // Fetch and save FIRMS hotspots
    @GetMapping("/hotspots")
    public ResponseEntity<List<Hotspot>> getHotspots(
            @RequestParam double minLon,
            @RequestParam double minLat,
            @RequestParam double maxLon,
            @RequestParam double maxLat,
            @RequestParam(required = false, defaultValue = "VIIRS_SNPP_NRT") String source,
            @RequestParam(required = false, defaultValue = "5") Integer days,
            @RequestParam(required = false, defaultValue = "true") boolean autoClassify) {

        List<Hotspot> hotspots = firmsService.fetchAndSaveHotspots(
                minLon, minLat, maxLon, maxLat, source, days, autoClassify
        );
        return ResponseEntity.ok(hotspots);
    }

    // Trigger FIRMS sync for region
    @PostMapping("/sync")
    public ResponseEntity<List<Hotspot>> syncHotspots(
            @RequestParam double minLon,
            @RequestParam double minLat,
            @RequestParam double maxLon,
            @RequestParam double maxLat,
            @RequestParam(required = false, defaultValue = "VIIRS_SNPP_NRT") String source,
            @RequestParam(required = false, defaultValue = "5") Integer days,
            @RequestParam(required = false, defaultValue = "true") boolean autoClassify) {

        List<Hotspot> hotspots = firmsService.fetchAndSaveHotspots(
                minLon, minLat, maxLon, maxLat, source, days, autoClassify
        );
        return ResponseEntity.ok(hotspots);
    }
}