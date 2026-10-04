package com.firewatch.firewatch_backend.Service;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Respositry.HotspotRepository;
import com.firewatch.firewatch_backend.dto.ml.HistoricalDetectionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SpatialPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(SpatialPersistenceService.class);
    private static final double DEFAULT_RADIUS_KM = 2.0;

    private final HotspotRepository hotspotRepository;

    public SpatialPersistenceService(HotspotRepository hotspotRepository) {
        this.hotspotRepository = hotspotRepository;
    }

    // Find historical detections within radius, excluding the current hotspot if persisted
    public List<HistoricalDetectionDto> getHistoricalDetectionsAround(
            Double targetLat,
            Double targetLon,
            LocalDate currentDate,
            Double radiusKm,
            Long excludeId) {

        if (targetLat == null || targetLon == null) {
            return new ArrayList<>();
        }

        double radius = (radiusKm != null && radiusKm > 0) ? radiusKm : DEFAULT_RADIUS_KM;
        LocalDate searchDate = currentDate != null ? currentDate : LocalDate.now();

        try {
            List<Hotspot> nearbyHotspots = hotspotRepository.findHistoricalNearbyHotspots(
                    targetLat,
                    targetLon,
                    searchDate,
                    radius,
                    excludeId
            );

            log.debug("Found {} historical detections within {} km of ({}, {})",
                    nearbyHotspots.size(), radius, targetLat, targetLon);

            return nearbyHotspots.stream()
                    .filter(h -> excludeId == null || !excludeId.equals(h.getId()))
                    .map(h -> new HistoricalDetectionDto(
                            h.getLatitude(),
                            h.getLongitude(),
                            h.getAcqDate(),
                            h.getFrp() != null ? h.getFrp() : 0.0
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("Failed to query historical nearby hotspots for ({}, {}): {}",
                    targetLat, targetLon, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<HistoricalDetectionDto> getHistoricalDetectionsAround(
            Double targetLat,
            Double targetLon,
            LocalDate currentDate,
            Double radiusKm) {
        return getHistoricalDetectionsAround(targetLat, targetLon, currentDate, radiusKm, null);
    }
}
