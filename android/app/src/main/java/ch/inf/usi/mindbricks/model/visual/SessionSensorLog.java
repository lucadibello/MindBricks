package ch.inf.usi.mindbricks.model.visual;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "session_sensor_logs",
        foreignKeys = @ForeignKey(
                entity = StudySession.class,
                parentColumns = "id",
                childColumns = "sessionId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("sessionId")}
)
public class SessionSensorLog {
    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Id of the session this log belongs to
     */
    private long sessionId;

    /**
     * Timestamp of the log
     */
    private long timestamp;

    /**
     * Current noise level at the time of the log
     */
    private float noiseLevel; // Stored as RMS amplitude

    /**
     * Current light level at the time of the log
     */
    private float lightLevel;

    /**
     * Current state of motion:
     * - True if motion was detected in this interval
     * - False otherwise
     */
    private boolean motionDetected;

    /**
     * Current state of face orientation:
     * - True if phone was face up in this interval
     * - False otherwise
     */
    private boolean isFaceUp;

    public SessionSensorLog(long sessionId, long timestamp, float noiseLevel, float lightLevel, boolean motionDetected, boolean isFaceUp) {
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.noiseLevel = noiseLevel;
        this.lightLevel = lightLevel;
        this.motionDetected = motionDetected;
        this.isFaceUp = isFaceUp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(float noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public float getLightLevel() {
        return lightLevel;
    }

    public void setLightLevel(float lightLevel) {
        this.lightLevel = lightLevel;
    }

    public boolean isMotionDetected() {
        return motionDetected;
    }

    public void setMotionDetected(boolean motionDetected) {
        this.motionDetected = motionDetected;
    }

    public boolean isFaceUp() {
        return isFaceUp;
    }

    public void setFaceUp(boolean faceUp) {
        isFaceUp = faceUp;
    }
}
