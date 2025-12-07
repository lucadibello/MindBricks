package ch.inf.usi.mindbricks.model.visual;

import androidx.room.Embedded;

public class StudySessionWithStats {
    @Embedded
    public StudySession session;

    public float avgNoiseLevel;
    public float avgLightLevel;
    public int phonePickupCount;

    // Constructor needed by room to build the object
    public StudySessionWithStats(StudySession session, float avgNoiseLevel, float avgLightLevel, int phonePickupCount) {
        this.session = session;
        this.avgNoiseLevel = avgNoiseLevel;
        this.avgLightLevel = avgLightLevel;
        this.phonePickupCount = phonePickupCount;
    }
    
    // getters delegating to session
    public long getId() { return session.getId(); }
    public long getTimestamp() { return session.getTimestamp(); }
    public int getDurationMinutes() { return session.getDurationMinutes(); }
    public String getTagTitle() { return session.getTagTitle(); }
    public int getTagColor() { return session.getTagColor(); }
    public float getFocusScore() { return session.getFocusScore(); }
    public int getCoinsEarned() { return session.getCoinsEarned(); }
    public String getNotes() { return session.getNotes(); }
    
    // getters for metrics
    public float getAvgNoiseLevel() { return avgNoiseLevel; }
    public float getAvgLightLevel() { return avgLightLevel; }
    public int getPhonePickupCount() { return phonePickupCount; }
}
