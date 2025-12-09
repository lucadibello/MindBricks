package ch.inf.usi.mindbricks.model.visual;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;

public class AIRecommendation {

    public enum ActivityType {
        DEEP_STUDY("Deep Study", R.color.activity_deep_study),
        LIGHT_STUDY("Light Study", R.color.activity_light_study),
        WORK("Work/Tasks", R.color.activity_work),
        EXERCISE("Exercise", R.color.activity_exercise),
        SOCIAL("Social Time", R.color.activity_social),
        MEALS("Meals", R.color.activity_meals),
        BREAKS("Short Breaks", R.color.activity_breaks),
        SLEEP("Sleep", R.color.activity_sleep);

        private final String displayName;
        private final int colorResId;

        ActivityType(String displayName, int colorResId) {
            this.displayName = displayName;
            this.colorResId = colorResId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getHex(Context ctx) {
            int color = ctx.getColor(colorResId);
            return String.format("#%06X", (0xFFFFFF & color));
        }
    }


    public static class ActivityBlock {
        private ActivityType activityType;
        private int startHour;
        private int endHour;
        private int confidenceScore;
        private String reason;

        public ActivityBlock(ActivityType type, int startHour, int endHour,
                             int confidence, String reason) {
            this.activityType = type;
            this.startHour = startHour;
            this.endHour = endHour;
            this.confidenceScore = confidence;
            this.reason = reason;
        }

        public ActivityType getActivityType() {
            return activityType;
        }

        public int getStartHour() {
            return startHour;
        }

        public int getEndHour() {
            return endHour;
        }

        public int getConfidenceScore() {
            return confidenceScore;
        }

        public String getReason() {
            return reason;
        }

        public int getDurationHours() {
            return endHour - startHour;
        }

        public String getTimeRange() {
            return formatHour(startHour) + " - " + formatHour(endHour);
        }

        private String formatHour(int hour) {
            if (hour == 0) return "12 AM";
            if (hour < 12) return hour + " AM";
            if (hour == 12) return "12 PM";
            return (hour - 12) + " PM";
        }
    }

    private List<ActivityBlock> activityBlocks;
    private int totalSessions;
    private float averageProductivity;
    private String summaryMessage;

    public AIRecommendation() {
        this.activityBlocks = new ArrayList<>();
        this.totalSessions = 0;
        this.averageProductivity = 0f;
    }

    public void addActivityBlock(ActivityBlock block) {
        activityBlocks.add(block);
    }

    public List<ActivityBlock> getActivityBlocks() {
        return activityBlocks;
    }

    // Getters and Setters
    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int total) {
        this.totalSessions = total;
    }

    public float getAverageProductivity() {
        return averageProductivity;
    }

    public void setAverageProductivity(float avg) {
        this.averageProductivity = avg;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }

    public void setSummaryMessage(String message) {
        this.summaryMessage = message;
    }
}