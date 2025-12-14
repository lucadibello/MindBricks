package ch.inf.usi.mindbricks.model.visual;

import androidx.room.Embedded;

import java.util.Calendar;

public class StudySessionWithStats {
    @Embedded
    public StudySession session;

    public float avgNoiseLevel;
    public float avgLightLevel;
    public int phonePickupCount;

    // Tag information (from tags table)
    public String tagTitle;
    public int tagColor;

    // Constructor needed by room to build the object
    public StudySessionWithStats(StudySession session, float avgNoiseLevel, float avgLightLevel, int phonePickupCount, String tagTitle, int tagColor) {
        this.session = session;
        this.avgNoiseLevel = avgNoiseLevel;
        this.avgLightLevel = avgLightLevel;
        this.phonePickupCount = phonePickupCount;
        this.tagTitle = tagTitle;
        this.tagColor = tagColor;
    }

    // getters delegating to session
    public long getId() { return session.getId(); }
    public long getTimestamp() { return session.getTimestamp(); }
    public int getDurationMinutes() { return session.getDurationMinutes(); }
    public String getTagTitle() { return tagTitle; }
    public int getTagColor() { return tagColor; }
    public float getFocusScore() { return session.getFocusScore(); }
    public int getCoinsEarned() { return session.getCoinsEarned(); }
    public String getNotes() { return session.getNotes(); }

    // getters for metrics
    public float getAvgNoiseLevel() { return avgNoiseLevel; }
    public float getAvgLightLevel() { return avgLightLevel; }
    public int getPhonePickupCount() { return phonePickupCount; }

    //getters for time
    public int getYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getTimestamp());
        return calendar.get(Calendar.YEAR);
    }

    public int getMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getTimestamp());

        return calendar.get(Calendar.MONTH) + 1;
    }

    public int getDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getTimestamp());
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public int getHourOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getTimestamp());
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public int getDayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getTimestamp());
        return calendar.get(Calendar.DAY_OF_WEEK);
    }
}
