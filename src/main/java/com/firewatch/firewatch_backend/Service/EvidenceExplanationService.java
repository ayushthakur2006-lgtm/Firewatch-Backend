package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Entity.MlFeature;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates transparent, verifiable evidence and operational decision-support recommendations.
 *
 * Rules:
 * - Evidence must ONLY reference actual available data.
 * - If a feature is estimated or fallback, it is explicitly disclosed.
 * - Recommendations avoid hyperbolic claims ("confirmed explosion", "accident confirmed")
 *   and provide actionable operator guidance ("prioritize verification", "review industrial cluster").
 */
@Service
public class EvidenceExplanationService {

    public static class DecisionSupportExplanation {
        private final String evidenceSummary;
        private final List<String> evidenceBullets;
        private final String recommendedAction;

        public DecisionSupportExplanation(String evidenceSummary, List<String> evidenceBullets, String recommendedAction) {
            this.evidenceSummary = evidenceSummary;
            this.evidenceBullets = evidenceBullets;
            this.recommendedAction = recommendedAction;
        }

        public String getEvidenceSummary() {
            return evidenceSummary;
        }

        public List<String> getEvidenceBullets() {
            return evidenceBullets;
        }

        public String getRecommendedAction() {
            return recommendedAction;
        }
    }

    public DecisionSupportExplanation generateExplanation(
            Hotspot hotspot,
            MlFeature mlFeature,
            int persistenceCount,
            int activeDays,
            int riskScore,
            String riskLevel) {

        List<String> bullets = new ArrayList<>();
        double frp = hotspot.getFrp() != null ? hotspot.getFrp() : 0.0;
        double ti4 = hotspot.getBrightTi4() != null ? hotspot.getBrightTi4() : 300.0;
        String cls = hotspot.getClassification() != null ? hotspot.getClassification() : "UNCLASSIFIED";
        double conf = hotspot.getMlConfidence() != null ? hotspot.getMlConfidence() : 0.0;

        // 1. Thermal Evidence
        if (frp >= 100.0) {
            bullets.add(String.format("High thermal intensity observed: %.1f MW radiative power with brightness temp %.1f K", frp, ti4));
        } else if (frp >= 30.0) {
            bullets.add(String.format("Moderate thermal signature detected: %.1f MW radiative power", frp));
        } else {
            bullets.add(String.format("Low-to-moderate thermal output: %.1f MW", frp));
        }

        // 2. Persistence / Temporal Evidence
        if (persistenceCount >= 3) {
            bullets.add(String.format("Historical spatial recurrence: %d satellite detections across %d distinct active days within a 2 km monitoring radius", persistenceCount, activeDays));
        } else if (persistenceCount == 2) {
            bullets.add("Historical spatial recurrence: 2 distinct satellite passes detected thermal activity within a 2 km monitoring radius");
        } else {
            bullets.add("Isolated thermal anomaly: no prior satellite detections recorded within 2 km monitoring radius during recent window");
        }

        // 3. Built-up & Spectral Context (Honest with Provenance)
        if (mlFeature != null) {
            String spectralProv = mlFeature.getSpectralProvenance();
            Double ndbi = mlFeature.getNdbi();
            Double ndvi = mlFeature.getNdvi();

            if ("SENTINEL_DERIVED".equals(spectralProv)) {
                if (ndbi != null && ndbi > 0.15) {
                    bullets.add(String.format("Sentinel-2 multispectral evidence: Elevated built-up index (NDBI %.2f) indicates impervious/built-up surface area (not definitive proof of industrial facility)", ndbi));
                } else if (ndvi != null && ndvi > 0.35) {
                    bullets.add(String.format("Sentinel-2 multispectral evidence: Significant canopy/vegetation index (NDVI %.2f)", ndvi));
                } else {
                    bullets.add(String.format("Sentinel-2 verified spectral context: NDVI %.2f, NDBI %.2f", ndvi != null ? ndvi : 0.0, ndbi != null ? ndbi : 0.0));
                }
            } else {
                bullets.add("Spectral context: Sentinel-2 pass unavailable/cloud-covered; surface indices estimated from VIIRS thermal band difference (not direct Sentinel spectral measurements)");
            }

            String landProv = mlFeature.getLandCoverProvenance();
            if ("COPERNICUS_DERIVED".equals(landProv)) {
                bullets.add("Land cover: Copernicus/ESA WorldCover 10m global classification confirms surface type");
            }
        }

        // 4. ML Model / Inference Assessment
        if ("RULE_BASED_FALLBACK".equalsIgnoreCase(hotspot.getInferenceMethod()) || hotspot.getMlConfidence() == null || hotspot.getMlConfidence() <= 0.0) {
            bullets.add(String.format("Classification: %s (Derived via rule-based fallback; model probability not available)", formatClassName(cls)));
        } else {
            bullets.add(String.format("AI classification model: %s (%.1f%% predicted model probability)", formatClassName(cls), conf * 100.0));
        }

        // Formulate Action Recommendation based on Risk Level & Classification
        String action;
        if ("CRITICAL".equalsIgnoreCase(riskLevel)) {
            if (cls.toUpperCase().contains("INDUSTRIAL")) {
                action = "Prioritize immediate human verification: cross-reference facility operations log, review flare stack schedule, and inspect optical imagery from next satellite pass.";
            } else {
                action = "Prioritize immediate verification with regional ground monitoring teams due to extreme thermal power output.";
            }
        } else if ("HIGH".equalsIgnoreCase(riskLevel)) {
            if (cls.toUpperCase().contains("PERSISTENT")) {
                action = "Flagged as active persistent thermal source. Confirm if location matches authorized industrial flare or thermal power installation.";
            } else {
                action = "Prioritize operator verification; compare location with nearby industrial infrastructure and local environmental fire alerts.";
            }
        } else if ("MODERATE".equalsIgnoreCase(riskLevel)) {
            action = "Review event against historical activity in cluster. Monitor next scheduled VIIRS pass for recurrence.";
        } else {
            action = "Continue routine automated satellite monitoring. No immediate intervention required.";
        }

        String summary = String.join(" • ", bullets);

        return new DecisionSupportExplanation(summary, bullets, action);
    }

    private String formatClassName(String raw) {
        if (raw == null) return "Unknown";
        return switch (raw.toUpperCase()) {
            case "INDUSTRIAL_FIRE" -> "Likely Industrial Fire";
            case "PERSISTENT_INDUSTRIAL_SOURCE" -> "Likely Persistent Industrial Thermal Source";
            case "WILDFIRE" -> "Likely Wildfire / Forest Fire";
            case "AGRICULTURAL_FIRE" -> "Likely Agricultural Stubble Burning";
            default -> raw.replace('_', ' ');
        };
    }
}
