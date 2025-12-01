package ch.inf.usi.mindbricks.model;

/**
 * Model representing statistics for a specific hour of the day across all sessions
 */
public class TimeSlotStats {

    /**
     * Hour of the day (0-23)
     */
    private int hourOfDay;

    /**
     * Total minutes studied during this hour across all days
     */
    private int totalMinutes;

    /**
     * Average focus score for this hour across all days
     */
    private float avgFocusScore;

    /**
     * Number of sessions during this hour
     */
    private int sessionCount;

    /**
     * Average noise level during this hour across all days
     */
    private float avgNoiseLevel;

    /**
     * Average light level during this hour across all days
     */
    private float avgLightLevel; // Average light level

    public TimeSlotStats(int hourOfDay) {
        this.hourOfDay = hourOfDay;
        this.totalMinutes = 0;
        this.avgFocusScore = 0;
        this.sessionCount = 0;
        this.avgNoiseLevel = 0;
        this.avgLightLevel = 0;
    }

    public int getHourOfDay() {
        return hourOfDay;
    }

    public void setHourOfDay(int hourOfDay) {
        this.hourOfDay = hourOfDay;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public void addMinutes(int minutes) {
        this.totalMinutes += minutes;
    }

    public float getAvgFocusScore() {
        return avgFocusScore;
    }

    public void setAvgFocusScore(float avgFocusScore) {
        this.avgFocusScore = avgFocusScore;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public void incrementSessionCount() {
        this.sessionCount++;
    }

    public float getAvgNoiseLevel() {
        return avgNoiseLevel;
    }

    public void setAvgNoiseLevel(float avgNoiseLevel) {
        this.avgNoiseLevel = avgNoiseLevel;
    }

    public float getAvgLightLevel() {
        return avgLightLevel;
    }

    public void setAvgLightLevel(float avgLightLevel) {
        this.avgLightLevel = avgLightLevel;
    }
}