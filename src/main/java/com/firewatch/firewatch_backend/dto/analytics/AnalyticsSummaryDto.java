package com.firewatch.firewatch_backend.dto.analytics;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsSummaryDto {

    private long totalHotspots;
    private long classifiedCount;
    private long unclassifiedCount;
    private long highRiskAlertCount;
    private long persistentCount;
    private Double avgFrp;
    private Double maxFrp;
    private Map<String, Long> classificationCounts = new HashMap<>();

    public AnalyticsSummaryDto() {
    }

    public AnalyticsSummaryDto(long totalHotspots, long classifiedCount, long unclassifiedCount,
                               long highRiskAlertCount, Double avgFrp, Double maxFrp,
                               Map<String, Long> classificationCounts) {
        this(totalHotspots, classifiedCount, unclassifiedCount, highRiskAlertCount, 0L, avgFrp, maxFrp, classificationCounts);
    }

    public AnalyticsSummaryDto(long totalHotspots, long classifiedCount, long unclassifiedCount,
                               long highRiskAlertCount, long persistentCount, Double avgFrp, Double maxFrp,
                               Map<String, Long> classificationCounts) {
        this.totalHotspots = totalHotspots;
        this.classifiedCount = classifiedCount;
        this.unclassifiedCount = unclassifiedCount;
        this.highRiskAlertCount = highRiskAlertCount;
        this.persistentCount = persistentCount;
        this.avgFrp = avgFrp;
        this.maxFrp = maxFrp;
        this.classificationCounts = classificationCounts != null ? classificationCounts : new HashMap<>();
    }

    public long getTotalHotspots() {
        return totalHotspots;
    }

    public void setTotalHotspots(long totalHotspots) {
        this.totalHotspots = totalHotspots;
    }

    public long getClassifiedCount() {
        return classifiedCount;
    }

    public void setClassifiedCount(long classifiedCount) {
        this.classifiedCount = classifiedCount;
    }

    public long getUnclassifiedCount() {
        return unclassifiedCount;
    }

    public void setUnclassifiedCount(long unclassifiedCount) {
        this.unclassifiedCount = unclassifiedCount;
    }

    public long getHighRiskAlertCount() {
        return highRiskAlertCount;
    }

    public void setHighRiskAlertCount(long highRiskAlertCount) {
        this.highRiskAlertCount = highRiskAlertCount;
    }

    public long getPersistentCount() {
        return persistentCount;
    }

    public void setPersistentCount(long persistentCount) {
        this.persistentCount = persistentCount;
    }

    public Double getAvgFrp() {
        return avgFrp;
    }

    public void setAvgFrp(Double avgFrp) {
        this.avgFrp = avgFrp;
    }

    public Double getMaxFrp() {
        return maxFrp;
    }

    public void setMaxFrp(Double maxFrp) {
        this.maxFrp = maxFrp;
    }

    public Map<String, Long> getClassificationCounts() {
        return classificationCounts;
    }

    public void setClassificationCounts(Map<String, Long> classificationCounts) {
        this.classificationCounts = classificationCounts;
    }
}
