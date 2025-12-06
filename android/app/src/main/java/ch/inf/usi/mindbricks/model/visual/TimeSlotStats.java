package ch.inf.usi.mindbricks.model.visual;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Model representing statistics for a specific time slot (hour of day).
 * Used for hourly distribution analysis.
 */
@SuppressWarnings("unused")
public class TimeSlotStats {

    private final int hourOfDay; // 0-23
    private int totalMinutes;
    private int sessionCount;
    private float averageFocusScore;
    private float averageNoiseLevel;
    private float averageLightLevel;

    public TimeSlotStats(int hourOfDay) {
        this.hourOfDay = hourOfDay;
        this.totalMinutes = 0;
        this.sessionCount = 0;
        this.averageFocusScore = 0;
        this.averageNoiseLevel = 0;
        this.averageLightLevel = 0;
    }

    public TimeSlotStats(int hourOfDay, int totalMinutes, int sessionCount,
                         float averageFocusScore, float averageNoiseLevel,
                         float averageLightLevel) {
        this.hourOfDay = hourOfDay;
        this.totalMinutes = totalMinutes;
        this.sessionCount = sessionCount;
        this.averageFocusScore = averageFocusScore;
        this.averageNoiseLevel = averageNoiseLevel;
        this.averageLightLevel = averageLightLevel;
    }

    // Getters and Setters

    public int getHourOfDay() {
        return hourOfDay;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public float getAverageFocusScore() {
        return averageFocusScore;
    }

    public void setAverageFocusScore(float averageFocusScore) {
        this.averageFocusScore = averageFocusScore;
    }

    public float getAverageNoiseLevel() {
        return averageNoiseLevel;
    }

    public void setAverageNoiseLevel(float averageNoiseLevel) {
        this.averageNoiseLevel = averageNoiseLevel;
    }

    public float getAverageLightLevel() {
        return averageLightLevel;
    }

    public void setAverageLightLevel(float averageLightLevel) {
        this.averageLightLevel = averageLightLevel;
    }

    /**
     * Add a session's data to this time slot.
     * Updates averages automatically.
     *
     * @param minutes Duration in minutes
     * @param focusScore Focus score (0-100)
     * @param noiseLevel Noise level (RMS amplitude)
     * @param lightLevel Light level (0-100)
     */
    public void addSession(int minutes, float focusScore, float noiseLevel, float lightLevel) {
        // Calculate new averages using weighted approach
        float totalWeight = sessionCount;
        float newWeight = 1;

        if (sessionCount > 0) {
            averageFocusScore = (averageFocusScore * totalWeight + focusScore * newWeight)
                    / (totalWeight + newWeight);
            averageNoiseLevel = (averageNoiseLevel * totalWeight + noiseLevel * newWeight)
                    / (totalWeight + newWeight);
            averageLightLevel = (averageLightLevel * totalWeight + lightLevel * newWeight)
                    / (totalWeight + newWeight);
        } else {
            averageFocusScore = focusScore;
            averageNoiseLevel = noiseLevel;
            averageLightLevel = lightLevel;
        }

        totalMinutes += minutes;
        sessionCount++;
    }


    public boolean isEmpty() {
        return sessionCount == 0;
    }

    /**
     * Get productivity rating for this time slot.
     *
     * @return Rating string: "High", "Medium", or "Low"
     */
    public String getProductivityRating() {
        if (averageFocusScore >= 70) {
            return "High";
        } else if (averageFocusScore >= 40) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    /**
     * Format hour as readable time string.
     *
     * @return Formatted time (e.g., "2:00 PM")
     */
    public String getFormattedTime() {
        if (hourOfDay == 0) {
            return "12:00 AM";
        } else if (hourOfDay < 12) {
            return hourOfDay + ":00 AM";
        } else if (hourOfDay == 12) {
            return "12:00 PM";
        } else {
            return (hourOfDay - 12) + ":00 PM";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US, "TimeSlot[hour=%d, minutes=%d, sessions=%d, focus=%.1f]",
                hourOfDay, totalMinutes, sessionCount, averageFocusScore);
    }
}
