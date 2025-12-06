package ch.inf.usi.mindbricks.model.visual;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


/**
 * Model representing a completed study session with metrics
 */
@Entity(tableName = "study_sessions")
public class StudySession {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Timestamp of when the session started
     */
    private long timestamp;

    /**
     * Total duration of the session in minutes
     */
    private int durationMinutes;

    /**
     * Title of the tag/subject studied
     */
    private String tagTitle;

    /**
     * Color of the tag/subject studied
     */
    private int tagColor;

    // Environmental metrics

    // Average noise level during the session (RMS amplitude)
    private float avgNoiseLevel;

    /**
     * Average light level during the session (0-100)
     */
    private float avgLightLevel;

    /**
     * Number of times phone was picked up during the session
     */
    private int phonePickupCount;

    // Focus metrics

    /**
     * Calculated focus score (0-100)
     */
    private float focusScore;

    /**
     * Number of coins earned during this study session
     */
    private int coinsEarned;

    // Optional notes
    private String notes;

    public StudySession(long timestamp, int durationMinutes, String tagTitle, int tagColor) {
        this.timestamp = timestamp;
        this.durationMinutes = durationMinutes;
        this.tagTitle = tagTitle;
        this.tagColor = tagColor;
        this.avgNoiseLevel = 0;
        this.avgLightLevel = 0;
        this.phonePickupCount = 0;
        this.focusScore = 0;
        this.coinsEarned = 0;
        this.notes = "";
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getTagTitle() {
        return tagTitle;
    }

    public void setTagTitle(String tagTitle) {
        this.tagTitle = tagTitle;
    }

    public int getTagColor() {
        return tagColor;
    }

    public void setTagColor(int tagColor) {
        this.tagColor = tagColor;
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

    public int getPhonePickupCount() {
        return phonePickupCount;
    }

    public void setPhonePickupCount(int phonePickupCount) {
        this.phonePickupCount = phonePickupCount;
    }

    public float getFocusScore() {
        return focusScore;
    }

    public void setFocusScore(float focusScore) {
        this.focusScore = focusScore;
    }

    public int getCoinsEarned() {
        return coinsEarned;
    }

    public void setCoinsEarned(int coinsEarned) {
        this.coinsEarned = coinsEarned;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
