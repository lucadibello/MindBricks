package ch.inf.usi.mindbricks.model;

/**
 * Represents a single day in the streak calendar
 */
public class StreakDay {
    private int dayOfMonth;         // 1-31
    private int month;              // 0-11 (Calendar month)
    private int year;               // e.g., 2024
    private int totalMinutes;       // Total study time
    private float avgQuality;       // Average focus score
    private StreakStatus status;    // Achievement status
    private int sessionCount;       // Number of sessions

    public enum StreakStatus {
        NONE,           // No study (red)
        PARTIAL,        // Some study but below target (orange)
        HIT_TARGET,     // Met target (green)
        EXCEPTIONAL     // Exceeded target significantly (blue)
    }

    public StreakDay() {
        this.status = StreakStatus.NONE;
    }

    public StreakDay(int dayOfMonth, int month, int year) {
        this.dayOfMonth = dayOfMonth;
        this.month = month;
        this.year = year;
        this.status = StreakStatus.NONE;
    }

    // Getters and setters
    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public float getAvgQuality() {
        return avgQuality;
    }

    public void setAvgQuality(float avgQuality) {
        this.avgQuality = avgQuality;
    }

    public StreakStatus getStatus() {
        return status;
    }

    public void setStatus(StreakStatus status) {
        this.status = status;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }
}
