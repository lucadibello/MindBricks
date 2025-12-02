package ch.inf.usi.mindbricks.model.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing daily study recommendations.
 * Contains suggested time slots and reasoning based on historical performance.
 */
public class DailyRecommendation {

    private List<TimeSlot> recommendedSlots;
    private String reasonSummary;
    private int confidenceScore; // 0-100

    public DailyRecommendation() {
        this.recommendedSlots = new ArrayList<>();
        this.reasonSummary = "";
        this.confidenceScore = 0;
    }

    /**
     * Full constructor.
     */
    public DailyRecommendation(List<TimeSlot> recommendedSlots, String reasonSummary, int confidenceScore) {
        this.recommendedSlots = recommendedSlots;
        this.reasonSummary = reasonSummary;
        this.confidenceScore = confidenceScore;
    }

    // --> Getters and Setters
    public List<TimeSlot> getRecommendedSlots() {
        return recommendedSlots;
    }

    public void setRecommendedSlots(List<TimeSlot> recommendedSlots) {
        this.recommendedSlots = recommendedSlots;
    }

    public String getReasonSummary() {
        return reasonSummary;
    }

    public void setReasonSummary(String reasonSummary) {
        this.reasonSummary = reasonSummary;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(int confidenceScore) {
        this.confidenceScore = Math.max(0, Math.min(100, confidenceScore));
    }

    public static class TimeSlot {
        private String startTime;
        private String endTime;
        private int productivityScore; // 0-100
        private String reason;

        public TimeSlot() {
            this.startTime = "";
            this.endTime = "";
            this.productivityScore = 0;
            this.reason = "";
        }

        public TimeSlot(String startTime, String endTime, int productivityScore, String reason) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.productivityScore = productivityScore;
            this.reason = reason;
        }

        // Getters and Setters

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public float getExpectedFocusScore(){
            return productivityScore;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getLabel() {
            return getStartTime() + " - " + getEndTime();
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public int getProductivityScore() {
            return productivityScore;
        }

        public void setProductivityScore(int productivityScore) {
            this.productivityScore = Math.max(0, Math.min(100, productivityScore));
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getTimeRange() {
            return startTime + " - " + endTime;
        }

        public String getProductivityLevel() {
            if (productivityScore >= 70) {
                return "High";
            } else if (productivityScore >= 40) {
                return "Medium";
            } else {
                return "Low";
            }
        }

        @Override
        public String toString() {
            return String.format("TimeSlot[%s, score=%d, %s]",
                    getTimeRange(), productivityScore, reason);
        }
    }
}