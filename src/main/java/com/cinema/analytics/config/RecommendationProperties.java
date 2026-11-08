package com.cinema.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cinema.analytics.recommendation")
public class RecommendationProperties {
    private double occupancyHighThreshold = 0.75;
    private double occupancyLowThreshold = 0.45;
    private double maxDecline = -0.3;
    private double maxGrowth = 0.5;
    private double safetyFactor = 0.15;
    private int minSampleSize = 3;

    public double getOccupancyHighThreshold() {
        return occupancyHighThreshold;
    }

    public void setOccupancyHighThreshold(double occupancyHighThreshold) {
        this.occupancyHighThreshold = occupancyHighThreshold;
    }

    public double getOccupancyLowThreshold() {
        return occupancyLowThreshold;
    }

    public void setOccupancyLowThreshold(double occupancyLowThreshold) {
        this.occupancyLowThreshold = occupancyLowThreshold;
    }

    public double getMaxDecline() {
        return maxDecline;
    }

    public void setMaxDecline(double maxDecline) {
        this.maxDecline = maxDecline;
    }

    public double getMaxGrowth() {
        return maxGrowth;
    }

    public void setMaxGrowth(double maxGrowth) {
        this.maxGrowth = maxGrowth;
    }

    public double getSafetyFactor() {
        return safetyFactor;
    }

    public void setSafetyFactor(double safetyFactor) {
        this.safetyFactor = safetyFactor;
    }

    public int getMinSampleSize() {
        return minSampleSize;
    }

    public void setMinSampleSize(int minSampleSize) {
        this.minSampleSize = minSampleSize;
    }
}
