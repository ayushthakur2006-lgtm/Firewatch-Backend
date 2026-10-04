package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Entity.MlFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RiskAssessmentService {

    @Value("${firewatch.risk.weight.thermal:0.25}")
    private double weightThermal;

    @Value("${firewatch.risk.weight.persistence:0.25}")
    private double weightPersistence;

    @Value("${firewatch.risk.weight.spatial:0.20}")
    private double weightSpatial;

    @Value("${firewatch.risk.weight.detection-model-evidence:${firewatch.risk.weight.ml-confidence:0.20}}")
    private double weightDetectionModelEvidence;

    @Value("${firewatch.risk.weight.environmental:0.10}")
    private double weightEnvironmental;

    public static class RiskEvaluation {
        private final int score;
        private final String level;
        private final Map<String, Double> factorScores;

        public RiskEvaluation(int score, String level, Map<String, Double> factorScores) {
            this.score = score;
            this.level = level;
            this.factorScores = factorScores;
        }

        public int getScore() {
            return score;
        }

        public String getLevel() {
            return level;
        }

        public Map<String, Double> getFactorScores() {
            return factorScores;
        }
    }

    public RiskEvaluation evaluateRisk(Hotspot hotspot, MlFeature mlFeature, int persistenceCount, int activeDays) {
        if (hotspot == null) {
            return new RiskEvaluation(10, "LOW", new HashMap<>());
        }

        Map<String, Double> factors = new HashMap<>();

        // 1. Thermal Intensity Factor (0 - 100)
        double frp = hotspot.getFrp() != null ? hotspot.getFrp() : 0.0;
        double ti4 = hotspot.getBrightTi4() != null ? hotspot.getBrightTi4() : 300.0;
        double thermalScore = 0.0;
        if (frp >= 200.0) {
            thermalScore = 100.0;
        } else if (frp >= 100.0) {
            thermalScore = 75.0 + ((frp - 100.0) / 100.0) * 25.0;
        } else if (frp >= 40.0) {
            thermalScore = 50.0 + ((frp - 40.0) / 60.0) * 25.0;
        } else {
            thermalScore = Math.min(50.0, (frp / 40.0) * 50.0);
        }
        if (ti4 > 360.0) {
            thermalScore = Math.min(100.0, thermalScore + 10.0);
        }
        factors.put("thermalIntensity", thermalScore);

        // 2. Temporal Persistence Factor (0 - 100)
        double persistenceScore = 0.0;
        if (persistenceCount >= 5 || activeDays >= 4) {
            persistenceScore = 95.0;
        } else if (persistenceCount >= 3 || activeDays >= 3) {
            persistenceScore = 80.0;
        } else if (persistenceCount >= 2 || activeDays >= 2) {
            persistenceScore = 60.0;
        } else {
            persistenceScore = 20.0; // Isolated detection
        }
        factors.put("persistence", persistenceScore);

        // 3. Built-up / Spatial Context Factor (0 - 100) — independent of ML classification
        double spatialScore = 30.0; // Baseline rural/open context
        if (mlFeature != null && mlFeature.getNdbi() != null) {
            double ndbi = mlFeature.getNdbi();
            if (ndbi >= 0.25) {
                spatialScore = 85.0; // High impervious / built-up surface density
            } else if (ndbi >= 0.10) {
                spatialScore = 65.0; // Moderate built-up infrastructure footprint
            } else if (ndbi >= -0.05) {
                spatialScore = 45.0; // Transitional / low built-up footprint
            } else {
                spatialScore = 25.0; // Natural vegetated / water surface
            }
        }
        factors.put("builtUpSpatialContext", spatialScore);
        factors.put("spatialContext", spatialScore); // backward compatibility alias

        // 4. Detection & Model Evidence Factor (0 - 100)
        double confScore;
        if (hotspot.getMlConfidence() != null && hotspot.getMlConfidence() > 0.0) {
            confScore = Math.max(0.0, Math.min(100.0, hotspot.getMlConfidence() * 100.0));
        } else {
            // When ML model is not run (rule-based fallback), use empirical NASA FIRMS detection confidence as detection evidence
            String firmsConf = hotspot.getConfidence() != null ? hotspot.getConfidence().trim().toLowerCase() : "";
            if ("h".equals(firmsConf) || "high".equals(firmsConf)) {
                confScore = 80.0;
            } else if ("n".equals(firmsConf) || "nominal".equals(firmsConf)) {
                confScore = 60.0;
            } else if ("l".equals(firmsConf) || "low".equals(firmsConf)) {
                confScore = 40.0;
            } else {
                try {
                    confScore = Math.max(0.0, Math.min(100.0, Double.parseDouble(firmsConf)));
                } catch (NumberFormatException e) {
                    confScore = 50.0;
                }
            }
        }
        factors.put("detectionAndModelEvidence", confScore);
        factors.put("detectionConfidence", confScore); // backward compatibility alias

        // 5. Environmental & Fuel Context (0 - 100)
        double envScore = 30.0;
        if (mlFeature != null) {
            Double bare = mlFeature.getBare();
            Double ndvi = mlFeature.getNdvi();
            Double ndwi = mlFeature.getNdwi();
            Double grass = mlFeature.getGrass();

            if (ndvi != null && ndvi > 0.4) {
                envScore += 25.0; // High vegetative fuel load
            } else if (ndvi != null && ndvi > 0.2) {
                envScore += 15.0;
            }

            if (ndwi != null && ndwi < -0.15) {
                envScore += 20.0; // Significant moisture deficit
            }

            if (bare != null && bare > 0.4) {
                envScore += 10.0;
            } else if (grass != null && grass > 0.3) {
                envScore += 15.0;
            }
        }
        envScore = Math.min(100.0, envScore);
        factors.put("environmentalContext", envScore);

        // Composite Weighted Score
        double composite = (thermalScore * weightThermal)
                + (persistenceScore * weightPersistence)
                + (spatialScore * weightSpatial)
                + (confScore * weightDetectionModelEvidence)
                + (envScore * weightEnvironmental);

        int finalScore = (int) Math.round(Math.max(0.0, Math.min(100.0, composite)));

        // Categorize Risk Level
        String level;
        if (finalScore >= 85) {
            level = "CRITICAL";
        } else if (finalScore >= 70) {
            level = "HIGH";
        } else if (finalScore >= 40) {
            level = "MODERATE";
        } else {
            level = "LOW";
        }

        return new RiskEvaluation(finalScore, level, factors);
    }

    public double getWeightDetectionModelEvidence() {
        return weightDetectionModelEvidence;
    }

    public void setWeightDetectionModelEvidence(double weightDetectionModelEvidence) {
        this.weightDetectionModelEvidence = weightDetectionModelEvidence;
    }

    public double getWeightMlConfidence() {
        return weightDetectionModelEvidence;
    }

    public void setWeightMlConfidence(double weightMlConfidence) {
        this.weightDetectionModelEvidence = weightMlConfidence;
    }
}
