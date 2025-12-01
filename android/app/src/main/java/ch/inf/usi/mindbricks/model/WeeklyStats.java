package ch.inf.usi.mindbricks.model;

/**
 * Model representing statistics for a single day in weekly view
 */
public class WeeklyStats {

    /**
     * The 3 letter abbreviation of the day of the week (e.g. "Mon", "Tue", etc.)
     */
    private String dayLabel;

    /**
     * Numeric value representing the day of the week (1 = Sunday, 2 = Monday, etc.)
     */
    private int dayOfWeek;

    /**
     * Total study time in minutes for the day
     */
    private int totalMinutes;

    /**
     * Average focus score for this day in range [0,100]
     */
    private float avgFocusScore;

    /**
     * Total number of sessions for this day
     */
    private int sessionCount;

    /**
     * Timestamp of the first session of this day
     */
    private long date;

    public WeeklyStats(String dayLabel, int dayOfWeek, long date) {
        this.dayLabel = dayLabel;
        this.dayOfWeek = dayOfWeek;
        this.date = date;
        this.totalMinutes = 0;
        this.avgFocusScore = 0;
        this.sessionCount = 0;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public void setDayLabel(String dayLabel) {
        this.dayLabel = dayLabel;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
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

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    /**
     * Get total study time in hours (formatted)
     */
    public float getTotalHours() {
        return totalMinutes / 60f;
    }
}