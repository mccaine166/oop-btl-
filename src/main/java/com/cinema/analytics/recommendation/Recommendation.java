package com.cinema.analytics.recommendation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Recommendation {
    private String id;
    private RecommendationType type;
    private String targetId;
    private String targetName;
    private String action;
    private String reason;
    private LocalDate analysisWeekStart;
    private LocalDate analysisWeekEnd;
    private LocalDate applyWeekStart;
    private LocalDate applyWeekEnd;
    private BigDecimal confidence;
    private int priority;

    public Recommendation() {
    }

    public Recommendation(String id, RecommendationType type, String targetId, String targetName,
                         String action, String reason, LocalDate analysisWeekStart, LocalDate analysisWeekEnd,
                         LocalDate applyWeekStart, LocalDate applyWeekEnd, BigDecimal confidence, int priority) {
        this.id = id;
        this.type = type;
        this.targetId = targetId;
        this.targetName = targetName;
        this.action = action;
        this.reason = reason;
        this.analysisWeekStart = analysisWeekStart;
        this.analysisWeekEnd = analysisWeekEnd;
        this.applyWeekStart = applyWeekStart;
        this.applyWeekEnd = applyWeekEnd;
        this.confidence = confidence;
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RecommendationType getType() {
        return type;
    }

    public void setType(RecommendationType type) {
        this.type = type;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getAnalysisWeekStart() {
        return analysisWeekStart;
    }

    public void setAnalysisWeekStart(LocalDate analysisWeekStart) {
        this.analysisWeekStart = analysisWeekStart;
    }

    public LocalDate getAnalysisWeekEnd() {
        return analysisWeekEnd;
    }

    public void setAnalysisWeekEnd(LocalDate analysisWeekEnd) {
        this.analysisWeekEnd = analysisWeekEnd;
    }

    public LocalDate getApplyWeekStart() {
        return applyWeekStart;
    }

    public void setApplyWeekStart(LocalDate applyWeekStart) {
        this.applyWeekStart = applyWeekStart;
    }

    public LocalDate getApplyWeekEnd() {
        return applyWeekEnd;
    }

    public void setApplyWeekEnd(LocalDate applyWeekEnd) {
        this.applyWeekEnd = applyWeekEnd;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recommendation that = (Recommendation) o;
        return priority == that.priority && Objects.equals(id, that.id) && type == that.type && Objects.equals(targetId, that.targetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, targetId, priority);
    }
}
