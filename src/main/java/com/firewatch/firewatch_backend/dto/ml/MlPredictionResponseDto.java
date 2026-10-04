package com.firewatch.firewatch_backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MlPredictionResponseDto {

    private String classification;

    @JsonProperty("ml_confidence")
    private Double mlConfidence;

    private String status;
    private String message;

    @JsonProperty("inference_method")
    private String inferenceMethod;

    @JsonProperty("prediction")
    private Map<String, Object> prediction;

    public MlPredictionResponseDto() {
    }

    public MlPredictionResponseDto(String classification, Double mlConfidence) {
        this.classification = classification;
        this.mlConfidence = mlConfidence;
        this.status = "SUCCESS";
        this.inferenceMethod = "ML_MODEL";
    }

    public MlPredictionResponseDto(String classification, Double mlConfidence, String status, String message) {
        this.classification = classification;
        this.mlConfidence = mlConfidence;
        this.status = status;
        this.message = message;
        this.inferenceMethod = "SUCCESS".equalsIgnoreCase(status) ? "ML_MODEL" : "RULE_BASED_FALLBACK";
    }

    public MlPredictionResponseDto(String classification, Double mlConfidence, String status, String message, String inferenceMethod) {
        this.classification = classification;
        this.mlConfidence = mlConfidence;
        this.status = status;
        this.message = message;
        this.inferenceMethod = inferenceMethod;
    }

    public String getClassification() {
        if (classification != null && !classification.isBlank()) {
            return normalizeClassification(classification);
        }
        if (prediction != null && prediction.containsKey("fire_class")) {
            Object raw = prediction.get("fire_class");
            if (raw != null) {
                return normalizeClassification(raw.toString());
            }
        }
        return null;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Double getMlConfidence() {
        if (mlConfidence != null) {
            return mlConfidence;
        }
        if (prediction != null && prediction.containsKey("confidence")) {
            Object confObj = prediction.get("confidence");
            if (confObj instanceof Number) {
                return ((Number) confObj).doubleValue();
            } else if (confObj != null) {
                try {
                    return Double.parseDouble(confObj.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    public void setMlConfidence(Double mlConfidence) {
        this.mlConfidence = mlConfidence;
    }

    public Map<String, Object> getPrediction() {
        return prediction;
    }

    public void setPrediction(Map<String, Object> prediction) {
        this.prediction = prediction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getInferenceMethod() {
        return inferenceMethod;
    }

    public void setInferenceMethod(String inferenceMethod) {
        this.inferenceMethod = inferenceMethod;
    }

    private String normalizeClassification(String raw) {
        if (raw == null) return null;
        String upper = raw.trim().toUpperCase();
        if (upper.equals("AGRICULTURAL") || upper.equals("AGRICULTURE") || upper.equals("CROP")) {
            return "AGRICULTURAL_FIRE";
        }
        if (upper.equals("FOREST") || upper.equals("WILD")) {
            return "WILDFIRE";
        }
        if (upper.equals("INDUSTRIAL")) {
            return "INDUSTRIAL_FIRE";
        }
        return upper;
    }
}
