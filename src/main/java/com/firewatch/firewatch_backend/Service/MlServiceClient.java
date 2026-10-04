package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Entity.MlFeature;
import com.firewatch.firewatch_backend.Respositry.HotspotRepository;
import com.firewatch.firewatch_backend.dto.ml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class MlServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);

    @Value("${ml.service.url:http://localhost:5000/api/predict}")
    private String mlServiceUrl;

    @Value("${ml.service.enabled:true}")
    private boolean mlServiceEnabled;

    private final RestTemplate restTemplate;
    private final SpatialPersistenceService spatialPersistenceService;
    private final FeatureEnrichmentService featureEnrichmentService;
    private final HotspotRepository hotspotRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final EvidenceExplanationService evidenceExplanationService;

    public MlServiceClient(
            RestTemplate restTemplate,
            SpatialPersistenceService spatialPersistenceService,
            FeatureEnrichmentService featureEnrichmentService,
            HotspotRepository hotspotRepository,
            RiskAssessmentService riskAssessmentService,
            EvidenceExplanationService evidenceExplanationService) {
        this.restTemplate = restTemplate;
        this.spatialPersistenceService = spatialPersistenceService;
        this.featureEnrichmentService = featureEnrichmentService;
        this.hotspotRepository = hotspotRepository;
        this.riskAssessmentService = riskAssessmentService;
        this.evidenceExplanationService = evidenceExplanationService;
    }

    // Build ML prediction payload
    public MlPredictionRequestDto buildMlPayload(Hotspot hotspot, MlFeature mlFeature) {
        if (hotspot == null) {
            return null;
        }

        FireFirmsDataDto fireData = new FireFirmsDataDto(
                hotspot.getLatitude(),
                hotspot.getLongitude(),
                hotspot.getAcqDate(),
                hotspot.getAcqTime(),
                hotspot.getBrightTi4(),
                hotspot.getBrightTi5(),
                hotspot.getFrp(),
                hotspot.getScan(),
                hotspot.getTrack()
        );

        SatelliteGeometryDto satelliteGeo = featureEnrichmentService.toSatelliteGeometryDto(mlFeature);
        LandCoverFeaturesDto landCover = featureEnrichmentService.toLandCoverFeaturesDto(mlFeature);
        SpectralFeaturesDto spectral = featureEnrichmentService.toSpectralFeaturesDto(mlFeature);

        List<HistoricalDetectionDto> historicalContext = spatialPersistenceService.getHistoricalDetectionsAround(
                hotspot.getLatitude(),
                hotspot.getLongitude(),
                hotspot.getAcqDate(),
                2.0,
                hotspot.getId()
        );

        return new MlPredictionRequestDto(
                fireData,
                satelliteGeo,
                landCover,
                spectral,
                historicalContext
        );
    }

    // Classify hotspot and evaluate decision-support intelligence
    @Transactional
    public Hotspot classifyHotspot(Hotspot hotspot) {
        if (hotspot == null) {
            return null;
        }

        MlFeature feature = featureEnrichmentService.enrichHotspotFeatures(hotspot);
        MlPredictionRequestDto payload = buildMlPayload(hotspot, feature);

        MlPredictionResponseDto prediction = callMlService(payload, hotspot);

        int historyCount = (payload != null && payload.getHistoricalContext2km() != null)
                ? payload.getHistoricalContext2km().size() : 0;
        
        java.util.Set<java.time.LocalDate> distinctDates = new java.util.HashSet<>();
        if (hotspot.getAcqDate() != null) {
            distinctDates.add(hotspot.getAcqDate());
        }
        if (payload != null && payload.getHistoricalContext2km() != null) {
            payload.getHistoricalContext2km().stream()
                    .map(HistoricalDetectionDto::getAcqDate)
                    .filter(java.util.Objects::nonNull)
                    .forEach(distinctDates::add);
        }
        int activeDays = Math.max(1, distinctDates.size());

        // Total observations in 2km window = current detection + historical detections
        int totalPersistenceCount = historyCount + 1;
        hotspot.setPersistenceCount(totalPersistenceCount);
        hotspot.setActiveDays(activeDays);

        if (prediction != null && prediction.getClassification() != null) {
            String cls = prediction.getClassification();

            // Refine generic industrial fire based on geospatial persistence
            if ("INDUSTRIAL_FIRE".equalsIgnoreCase(cls)) {
                if (historyCount >= 3) {
                    cls = "PERSISTENT_INDUSTRIAL_SOURCE";
                }
            }

            hotspot.setClassification(cls);
            hotspot.setInferenceMethod(prediction.getInferenceMethod() != null ? prediction.getInferenceMethod() : "ML_MODEL");
            hotspot.setMlConfidence(prediction.getMlConfidence());
            log.info("Hotspot ID {} classified as '{}' via {} with confidence {}",
                    hotspot.getId(), cls, hotspot.getInferenceMethod(), hotspot.getMlConfidence());
        }

        // Calculate deterministic Risk Assessment & Explainable Evidence
        RiskAssessmentService.RiskEvaluation riskEval =
                riskAssessmentService.evaluateRisk(hotspot, feature, totalPersistenceCount, activeDays);
        hotspot.setRiskScore(riskEval.getScore());
        hotspot.setRiskLevel(riskEval.getLevel());

        EvidenceExplanationService.DecisionSupportExplanation explanation =
                evidenceExplanationService.generateExplanation(
                        hotspot, feature, totalPersistenceCount, activeDays, riskEval.getScore(), riskEval.getLevel());
        hotspot.setEvidence(explanation.getEvidenceSummary());
        hotspot.setRecommendedAction(explanation.getRecommendedAction());

        return hotspotRepository.save(hotspot);
    }

    // Call ML microservice with fallback
    private MlPredictionResponseDto callMlService(MlPredictionRequestDto payload, Hotspot hotspot) {
        if (!mlServiceEnabled) {
            log.info("ML Service is disabled in application.properties. Generating baseline estimation.");
            return generateFallbackPrediction(hotspot, payload);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MlPredictionRequestDto> request = new HttpEntity<>(payload, headers);

            log.info("Sending prediction request to ML microservice at: {}", mlServiceUrl);
            ResponseEntity<MlPredictionResponseDto> response = restTemplate.exchange(
                    mlServiceUrl,
                    HttpMethod.POST,
                    request,
                    MlPredictionResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

        } catch (RestClientException ex) {
            log.warn("ML service is unreachable at {}. Error: {}. Applying fallback prediction.",
                    mlServiceUrl, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error contacting ML service: {}", ex.getMessage(), ex);
        }

        return generateFallbackPrediction(hotspot, payload);
    }

    // Rule-based fallback classification
    private MlPredictionResponseDto generateFallbackPrediction(Hotspot hotspot, MlPredictionRequestDto payload) {
        int historyCount = (payload != null && payload.getHistoricalContext2km() != null)
                ? payload.getHistoricalContext2km().size() : 0;
        double frp = hotspot.getFrp() != null ? hotspot.getFrp() : 0.0;
        double ndbi = (payload != null && payload.getSpectralFeatures() != null && payload.getSpectralFeatures().getNdbi() != null)
                ? payload.getSpectralFeatures().getNdbi() : 0.0;

        String classification;

        if (historyCount >= 3 && ndbi > 0.2) {
            classification = "PERSISTENT_INDUSTRIAL_SOURCE";
        } else if (ndbi > 0.15 && (historyCount > 0 || frp > 100.0)) {
            classification = "INDUSTRIAL_FIRE";
        } else if (payload != null && payload.getSpectralFeatures() != null &&
                   payload.getSpectralFeatures().getNdvi() != null && payload.getSpectralFeatures().getNdvi() > 0.4) {
            classification = "WILDFIRE";
        } else {
            classification = "AGRICULTURAL_FIRE";
        }

        // Do not fabricate ML probability in fallback mode
        return new MlPredictionResponseDto(
                classification,
                null,
                "FALLBACK",
                "Rule-based fallback prediction applied",
                "RULE_BASED_FALLBACK"
        );
    }
}
