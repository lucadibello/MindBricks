package ch.inf.usi.mindbricks.model;

/**
 * Represents a single cell in the quality heatmap (Hour × Day)
 */
public class HeatmapCell {
    private int dayOfMonth;     // 1-31
    private int hour;           // 0-23
    private float avgQuality;   // 0-100
    private int sessionCount;   // Number of sessions in this cell

    public HeatmapCell() {
    }

    public HeatmapCell(int dayOfMonth, int hour, float avgQuality, int sessionCount) {
        this.dayOfMonth = dayOfMonth;
        this.hour = hour;
        this.avgQuality = avgQuality;
        this.sessionCount = sessionCount;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public float getAvgQuality() {
        return avgQuality;
    }

    public void setAvgQuality(float avgQuality) {
        this.avgQuality = avgQuality;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }
}