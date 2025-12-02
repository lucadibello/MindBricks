package ch.inf.usi.mindbricks.model;

/*
 * Represents average study quality for a specific hour of the day
 */
public class HourlyQuality {
    private int hour;           // 0-23
    private float avgQuality;   // 0-100 (average focus score)
    private int sessionCount;   // Number of sessions in this hour

    public HourlyQuality() {
    }

    public HourlyQuality(int hour, float avgQuality, int sessionCount) {
        this.hour = hour;
        this.avgQuality = avgQuality;
        this.sessionCount = sessionCount;
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