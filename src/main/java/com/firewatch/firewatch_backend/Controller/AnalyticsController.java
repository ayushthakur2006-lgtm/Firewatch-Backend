package com.firewatch.firewatch_backend.Controller;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Service.AnalyticsService;
import com.firewatch.firewatch_backend.dto.analytics.AnalyticsSummaryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // Summary statistics for dashboard metrics
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> getSummary() {
        AnalyticsSummaryDto summary = analyticsService.getSummary();
        return ResponseEntity.ok(summary);
    }

    // High-risk thermal events
    @GetMapping("/high-risk")
    public ResponseEntity<List<Hotspot>> getHighRiskHotspots(
            @RequestParam(required = false, defaultValue = "100.0") Double minFrp) {
        List<Hotspot> highRisk = analyticsService.getHighRiskHotspots(minFrp);
        return ResponseEntity.ok(highRisk);
    }
}
