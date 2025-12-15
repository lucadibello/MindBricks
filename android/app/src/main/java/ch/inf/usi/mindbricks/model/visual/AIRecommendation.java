package ch.inf.usi.mindbricks.model.visual;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;

public class AIRecommendation extends DailyRecommendation{

    public enum ActivityType {
        DEEP_STUDY("Deep Study", R.color.activity_deep_study),
        LIGHT_STUDY("Light Study", R.color.activity_light_study),
        WORK("Work/Tasks", R.color.activity_work),
        EXERCISE("Exercise", R.color.activity_exercise),
        SOCIAL("Social Time", R.color.activity_social),
        MEALS("Meals", R.color.activity_meals),
        BREAKS("Short Breaks", R.color.activity_breaks),
        SLEEP("Sleep", R.color.activity_sleep),
        CALENDAR_EVENT("Calendar Event", R.color.activity_calendar);

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

        public boolean isProtected() {
            return this == CALENDAR_EVENT || this == SLEEP;
        }
    }


    public static class ActivityBlock {
        private ActivityType activityType;
        private int startHour;
        private int endHour;
        private int confidenceScore;
        private String reason;

        private String eventTitle;

        public ActivityBlock(ActivityType type, int startHour, int endHour,
                             int confidence, String reason) {
            this.activityType = type;
            this.startHour = startHour;
            this.endHour = endHour;
            this.confidenceScore = confidence;
            this.reason = reason;
        }

        public ActivityBlock(ActivityType type, int startHour, int endHour,
                             String eventTitle, String reason) {
            this.activityType = type;
            this.startHour = startHour;
            this.endHour = endHour;
            this.eventTitle = eventTitle;
            this.reason = reason;
            this.confidenceScore = 100;
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

        public String getDisplayName() {
            if (activityType == ActivityType.CALENDAR_EVENT && eventTitle != null) {
                return eventTitle;
            }
            return activityType.getDisplayName();
        }
    }

    private List<ActivityBlock> activityBlocks;
    private int totalSessions;
    private float averageProductivity;
    private String summaryMessage;
    private int calendarBlockedHours;

    public AIRecommendation() {
        this.activityBlocks = new ArrayList<>();
        this.totalSessions = 0;
        this.averageProductivity = 0f;
    }

    public void addActivityBlock(ActivityBlock block) {
        activityBlocks.add(block);

        if (block.getActivityType() == ActivityType.CALENDAR_EVENT) {
            calendarBlockedHours += block.getDurationHours();
        }
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

    public int getCalendarBlockedHours() {
        return calendarBlockedHours;
    }

    public int getAvailableHours() {
        int totalBlocked = 0;
        for (ActivityBlock block : activityBlocks) {
            ActivityType type = block.getActivityType();
            if (type == ActivityType.SLEEP || type == ActivityType.CALENDAR_EVENT) {
                totalBlocked += block.getDurationHours();
            }
        }
        return 24 - totalBlocked;
    }
}