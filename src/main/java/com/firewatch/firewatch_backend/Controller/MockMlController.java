package com.firewatch.firewatch_backend.Controller;

import com.firewatch.firewatch_backend.dto.ml.MlPredictionRequestDto;
import com.firewatch.firewatch_backend.dto.ml.MlPredictionResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEVELOPMENT & TEST ONLY: Rule-Based Mock Prediction Endpoint.
 *
 * WARNING: This controller is an offline testing stub used solely for local development
 * when the Python FastAPI ML microservice is offline. It is NEVER used as the production AI inference model.
 * The production AI pipeline is served by the Python FastAPI microservice (FireWatch V3 HistGradientBoosting).
 */
@RestController
@RequestMapping("/api/ml")
@Profile({"dev", "test", "mock"})
public class MockMlController {

    private static final Logger log = LoggerFactory.getLogger(MockMlController.class);

    // Development/test-only mock prediction endpoint
    @PostMapping("/mock-predict")
    public ResponseEntity<MlPredictionResponseDto> mockPredict(@RequestBody MlPredictionRequestDto request) {
        log.info("Received prediction request in Mock ML endpoint for fire at ({}, {})",
                request.getFireFirmsData() != null ? request.getFireFirmsData().getLatitude() : null,
                request.getFireFirmsData() != null ? request.getFireFirmsData().getLongitude() : null);

        int historyCount = request.getHistoricalContext2km() != null ? request.getHistoricalContext2km().size() : 0;
        double frp = (request.getFireFirmsData() != null && request.getFireFirmsData().getFrp() != null)
                ? request.getFireFirmsData().getFrp() : 10.0;
        double ndbi = (request.getSpectralFeatures() != null && request.getSpectralFeatures().getNdbi() != null)
                ? request.getSpectralFeatures().getNdbi() : 0.0;
        double ndvi = (request.getSpectralFeatures() != null && request.getSpectralFeatures().getNdvi() != null)
                ? request.getSpectralFeatures().getNdvi() : 0.0;

        String classification;

        if (historyCount >= 3 && ndbi > 0.2) {
            classification = "PERSISTENT_INDUSTRIAL_SOURCE";
        } else if (ndbi > 0.15 && (historyCount > 0 || frp > 100.0)) {
            classification = "INDUSTRIAL_FIRE";
        } else if (ndvi > 0.4) {
            classification = "WILDFIRE";
        } else {
            classification = "AGRICULTURAL_FIRE";
        }

        MlPredictionResponseDto response = new MlPredictionResponseDto(
                classification,
                null,
                "MOCK_TEST",
                "MOCK / TEST / DEVELOPMENT ONLY — offline development testing stub",
                "RULE_BASED_MOCK"
        );

        return ResponseEntity.ok(response);
    }
}
