package com.firewatch.firewatch_backend.Respositry;

import com.firewatch.firewatch_backend.Entity.Hotspot;
import com.firewatch.firewatch_backend.Entity.MlFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MlFeatureRepository extends JpaRepository<MlFeature, Long> {

    Optional<MlFeature> findByHotspotId(Long hotspotId);

    Optional<MlFeature> findByHotspot(Hotspot hotspot);
}