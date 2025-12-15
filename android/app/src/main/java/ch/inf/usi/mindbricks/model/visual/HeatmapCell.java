package ch.inf.usi.mindbricks.model.visual;

public class HeatmapCell {
    private int year;
    private int month;          // 0-11 (Calendar.MONTH)
    private int dayOfMonth;     // 1-31
    private int dayOfWeek;      // 1-7 (Calendar.DAY_OF_WEEK)
    private int hourOfDay;      // 0-23
    private long timestamp;     // Start of hour timestamp
    private int sessionCount;   // Number of sessions in this hour
    private float avgQuality;   // Average focus score for this hour
    private int totalMinutes;   // Total study minutes in this hour

    public HeatmapCell() {
    }

    public HeatmapCell(int dayOfWeek, int hourOfDay) {
        this.dayOfWeek = dayOfWeek;
        this.hourOfDay = hourOfDay;
    }

    // Getters and setters
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getHourOfDay() {
        return hourOfDay;
    }

    public void setHourOfDay(int hourOfDay) {
        this.hourOfDay = hourOfDay;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public float getAvgQuality() {
        return avgQuality;
    }

    public void setAvgQuality(float avgQuality) {
        this.avgQuality = avgQuality;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    /**
     * Get formatted date string for this cell
     */
    public String getDateString() {
        return String.format("%d-%02d-%02d", year, month + 1, dayOfMonth);
    }

    /**
     * Get formatted hour string (e.g., "2 PM", "14:00")
     */
    public String getHourString() {
        if (hourOfDay == 0) {
            return "12 AM";
        } else if (hourOfDay < 12) {
            return hourOfDay + " AM";
        } else if (hourOfDay == 12) {
            return "12 PM";
        } else {
            return (hourOfDay - 12) + " PM";
        }
    }

    /**
     * Get 24-hour format string
     */
    public String getHourString24() {
        return String.format("%02d:00", hourOfDay);
    }
}