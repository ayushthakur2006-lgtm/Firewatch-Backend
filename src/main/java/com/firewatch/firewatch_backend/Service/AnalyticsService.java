package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Respositry.HotspotRepository;
import com.firewatch.firewatch_backend.dto.analytics.AnalyticsSummaryDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final HotspotRepository hotspotRepository;

    public AnalyticsService(HotspotRepository hotspotRepository) {
        this.hotspotRepository = hotspotRepository;
    }

    public AnalyticsSummaryDto getSummary() {
        long total = hotspotRepository.count();
        long classified = hotspotRepository.countClassified();
        long unclassified = hotspotRepository.countUnclassified();
        long highRisk = hotspotRepository.countHighRisk(100.0);
        long persistentCount = hotspotRepository.countPersistent();
        Double avgFrp = hotspotRepository.getAverageFrp();
        Double maxFrp = hotspotRepository.getMaxFrp();

        Map<String, Long> classificationCounts = new HashMap<>();
        List<Object[]> grouped = hotspotRepository.countByClassificationGrouped();
        for (Object[] row : grouped) {
            String classification = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            if (classification != null) {
                classificationCounts.put(classification, count);
            }
        }

        return new AnalyticsSummaryDto(
                total,
                classified,
                unclassified,
                highRisk,
                persistentCount,
                avgFrp != null ? Math.round(avgFrp * 100.0) / 100.0 : 0.0,
                maxFrp != null ? Math.round(maxFrp * 100.0) / 100.0 : 0.0,
                classificationCounts
        );
    }

    public List<Hotspot> getHighRiskHotspots(Double minFrp) {
        double threshold = minFrp != null ? minFrp : 100.0;
        List<Hotspot> all = hotspotRepository.findAll();
        // Return hotspots that meet either the FRP threshold OR have HIGH / CRITICAL riskLevel (riskScore >= 70)
        return all.stream()
                .filter(h -> (h.getFrp() != null && h.getFrp() >= threshold) ||
                             (h.getRiskScore() != null && h.getRiskScore() >= 70) ||
                             "HIGH".equalsIgnoreCase(h.getRiskLevel()) ||
                             "CRITICAL".equalsIgnoreCase(h.getRiskLevel()))
                .sorted((a, b) -> {
                    int scoreA = a.getRiskScore() != null ? a.getRiskScore() : (int) Math.min(100, (a.getFrp() != null ? a.getFrp() : 0));
                    int scoreB = b.getRiskScore() != null ? b.getRiskScore() : (int) Math.min(100, (b.getFrp() != null ? b.getFrp() : 0));
                    return Integer.compare(scoreB, scoreA);
                })
                .toList();
    }
}
