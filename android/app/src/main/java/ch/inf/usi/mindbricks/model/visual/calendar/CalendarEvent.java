package ch.inf.usi.mindbricks.model.visual.calendar;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a calendar event fetched from an external calendar provider.
 */
@Entity(
        tableName = "calendar_events",
        indices = {
                @Index(value = {"externalId", "calendarSource"}, unique = true),
                @Index(value = {"startTime"}),
                @Index(value = {"endTime"})
        }
)
public class CalendarEvent {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String externalId;
    private String calendarSource;
    private String title;
    private String description;
    private long startTime;
    private long endTime;
    private boolean isAllDay;
    private String location;
    private long lastSyncedAt;
    private String calendarName;
    private int color;

    // Default constructor required by Room
    public CalendarEvent(String externalId, String calendarSource, String title,
                         long startTime, long endTime, boolean isAllDay) {
        this.externalId = externalId;
        this.calendarSource = calendarSource;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isAllDay = isAllDay;
        this.lastSyncedAt = System.currentTimeMillis();
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getCalendarSource() {
        return calendarSource;
    }

    public void setCalendarSource(String calendarSource) {
        this.calendarSource = calendarSource;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isAllDay() {
        return isAllDay;
    }

    public void setAllDay(boolean allDay) {
        isAllDay = allDay;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(long lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }


    public int getDurationMinutes() {
        return (int) ((endTime - startTime) / (1000 * 60));
    }

    public boolean overlapsWithRange(long rangeStart, long rangeEnd) {
        return startTime < rangeEnd && endTime > rangeStart;
    }


    public boolean coversHour(int hour) {
        java.util.Calendar cal = java.util.Calendar.getInstance();

        // Get the start hour
        cal.setTimeInMillis(startTime);
        int startHour = cal.get(java.util.Calendar.HOUR_OF_DAY);

        // Get the end hour
        cal.setTimeInMillis(endTime);
        int endHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int endMinute = cal.get(java.util.Calendar.MINUTE);

        if (endMinute == 0 && endHour > 0) {
            endHour--;
        }

        return hour >= startHour && hour <= endHour;
    }

    @Override
    public String toString() {
        return "CalendarEvent{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", source='" + calendarSource + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", isAllDay=" + isAllDay +
                '}';
    }
}