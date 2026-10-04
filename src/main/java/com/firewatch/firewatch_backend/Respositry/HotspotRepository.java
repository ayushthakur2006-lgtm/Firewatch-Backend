package com.firewatch.firewatch_backend.Respositry;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HotspotRepository extends JpaRepository<Hotspot, Long> {

    Optional<Hotspot> findByLatitudeAndLongitudeAndAcqDateAndAcqTimeAndSatellite(
            Double latitude,
            Double longitude,
            LocalDate acqDate,
            LocalTime acqTime,
            String satellite
    );

    List<Hotspot> findByClassificationIsNull();

    List<Hotspot> findByClassification(String classification);

    @Query("""
        SELECT h FROM Hotspot h
        WHERE (:minLat IS NULL OR h.latitude >= :minLat)
          AND (:maxLat IS NULL OR h.latitude <= :maxLat)
          AND (:minLon IS NULL OR h.longitude >= :minLon)
          AND (:maxLon IS NULL OR h.longitude <= :maxLon)
          AND (:startDate IS NULL OR h.acqDate >= :startDate)
          AND (:endDate IS NULL OR h.acqDate <= :endDate)
          AND (:classification IS NULL OR h.classification = :classification)
          AND (:minFrp IS NULL OR h.frp >= :minFrp)
        ORDER BY h.acqDate DESC, h.acqTime DESC
    """)
    List<Hotspot> filterHotspots(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("classification") String classification,
            @Param("minFrp") Double minFrp
    );

    @Query("""
        SELECT h FROM Hotspot h
        WHERE (:minLat IS NULL OR h.latitude >= :minLat)
          AND (:maxLat IS NULL OR h.latitude <= :maxLat)
          AND (:minLon IS NULL OR h.longitude >= :minLon)
          AND (:maxLon IS NULL OR h.longitude <= :maxLon)
          AND (:startDate IS NULL OR h.acqDate >= :startDate)
          AND (:endDate IS NULL OR h.acqDate <= :endDate)
          AND (:classification IS NULL OR h.classification = :classification)
          AND (:minFrp IS NULL OR h.frp >= :minFrp)
    """)
    Page<Hotspot> filterHotspotsPaged(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("classification") String classification,
            @Param("minFrp") Double minFrp,
            Pageable pageable
    );

    @Query(value = """
        SELECT * FROM hotspots h
        WHERE (:excludeId IS NULL OR h.id != :excludeId)
          AND h.acq_date <= :currentDate
          AND (
            6371.0 * 2.0 * ASIN(
              SQRT(
                POWER(SIN(RADIANS(h.latitude - :targetLat) / 2.0), 2) +
                COS(RADIANS(:targetLat)) * COS(RADIANS(h.latitude)) *
                POWER(SIN(RADIANS(h.longitude - :targetLon) / 2.0), 2)
              )
            )
          ) <= :radiusKm
        ORDER BY h.acq_date DESC
    """, nativeQuery = true)
    List<Hotspot> findHistoricalNearbyHotspots(
            @Param("targetLat") Double targetLat,
            @Param("targetLon") Double targetLon,
            @Param("currentDate") LocalDate currentDate,
            @Param("radiusKm") Double radiusKm,
            @Param("excludeId") Long excludeId
    );

    default List<Hotspot> findHistoricalNearbyHotspots(
            Double targetLat,
            Double targetLon,
            LocalDate currentDate,
            Double radiusKm) {
        return findHistoricalNearbyHotspots(targetLat, targetLon, currentDate, radiusKm, null);
    }

    @Query("SELECT COUNT(h) FROM Hotspot h WHERE h.classification IS NOT NULL")
    long countClassified();

    @Query("SELECT COUNT(h) FROM Hotspot h WHERE h.classification IS NULL")
    long countUnclassified();

    @Query("SELECT COUNT(h) FROM Hotspot h WHERE (h.riskScore IS NOT NULL AND h.riskScore >= 70) OR (h.frp IS NOT NULL AND h.frp >= :threshold)")
    long countHighRisk(@Param("threshold") Double threshold);

    @Query("SELECT COUNT(h) FROM Hotspot h WHERE h.persistenceCount >= 3 OR UPPER(h.classification) LIKE '%PERSISTENT%'")
    long countPersistent();

    @Query("SELECT AVG(h.frp) FROM Hotspot h WHERE h.frp IS NOT NULL")
    Double getAverageFrp();

    @Query("SELECT MAX(h.frp) FROM Hotspot h WHERE h.frp IS NOT NULL")
    Double getMaxFrp();

    @Query("SELECT h.classification, COUNT(h) FROM Hotspot h WHERE h.classification IS NOT NULL GROUP BY h.classification")
    List<Object[]> countByClassificationGrouped();
}