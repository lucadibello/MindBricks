package ch.inf.usi.mindbricks.model.questionnare;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import ch.inf.usi.mindbricks.model.visual.StudySession;

@Entity(tableName = "session_questionnaires",
        foreignKeys = @ForeignKey(
                entity = StudySession.class,
                parentColumns = "id",
                childColumns = "sessionId",

                // Delete questionnaire if session is deleted
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "sessionId")})

public class SessionQuestionnaire {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long sessionId;
    private long timeStamp;

    // PAM emotion -> 0-6 scale
    private int initialEmotion;

    // detailed questionnaire
    private boolean answeredDetailedQuestions;
    private Integer enthusiasmRating;
    private Integer energyRating;
    private Integer engagementRating;
    private Integer satisfactionRating;
    private Integer anticipationRating;

    public SessionQuestionnaire(){};

    public SessionQuestionnaire(long sessionId, long timestamp, int initialEmotion) {
        this.sessionId = sessionId;
        this.timeStamp = timestamp;
        this.initialEmotion = initialEmotion;
        this.answeredDetailedQuestions = false;
    }

    public SessionQuestionnaire(long sessionId, long timestamp, int initialEmotion,
                                int enthusiasmRating, int energyRating, int engagementRating,
                                int satisfactionRating, int anticipationRating) {
        this.sessionId = sessionId;
        this.timeStamp = timestamp;
        this.initialEmotion = initialEmotion;
        this.answeredDetailedQuestions = true;
        this.enthusiasmRating = enthusiasmRating;
        this.energyRating = energyRating;
        this.engagementRating = engagementRating;
        this.satisfactionRating = satisfactionRating;
        this.anticipationRating = anticipationRating;
    }

    // getters and setters
    public long getId() {
        return id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getInitialEmotion() {
        return initialEmotion;
    }

    public boolean isAnsweredDetailedQuestions() {
        return answeredDetailedQuestions;
    }

    public Integer getEnthusiasmRating() {
        return enthusiasmRating;
    }

    public Integer getEnergyRating() {
        return energyRating;
    }

    public Integer getEngagementRating() {
        return engagementRating;
    }

    public Integer getSatisfactionRating() {
        return satisfactionRating;
    }

    public Integer getAnticipationRating() {
        return anticipationRating;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public void setTimeStamp(long timestamp) {
        this.timeStamp = timestamp;
    }

    public void setInitialEmotion(int initialEmotion) {
        this.initialEmotion = initialEmotion;
    }

    public void setAnsweredDetailedQuestions(boolean answeredDetailedQuestions) {
        this.answeredDetailedQuestions = answeredDetailedQuestions;
    }

    public void setEnthusiasmRating(Integer enthusiasmRating) {
        this.enthusiasmRating = enthusiasmRating;
    }

    public void setEnergyRating(Integer energyRating) {
        this.energyRating = energyRating;
    }

    public void setEngagementRating(Integer engagementRating) {
        this.engagementRating = engagementRating;
    }

    public void setSatisfactionRating(Integer satisfactionRating) {
        this.satisfactionRating = satisfactionRating;
    }

    public void setAnticipationRating(Integer anticipationRating) {
        this.anticipationRating = anticipationRating;
    }

    // Further use in the graphs -> prepared for future use
    public boolean hasCompleteRatings() {
        return answeredDetailedQuestions &&
                enthusiasmRating != null &&
                energyRating != null &&
                engagementRating != null &&
                satisfactionRating != null &&
                anticipationRating != null;
    }

    public float getAverageRating() {
        if (!hasCompleteRatings()) {
            return -1f;
        }
        return (enthusiasmRating + energyRating + engagementRating +
                satisfactionRating + anticipationRating) / 5f;
    }

    @NonNull
    @Override
    public String toString() {
        return "SessionQuestionnaire{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", timestamp=" + timeStamp +
                ", initialEmotion=" + initialEmotion +
                ", answeredDetailedQuestions=" + answeredDetailedQuestions +
                ", enthusiasmRating=" + enthusiasmRating +
                ", energyRating=" + energyRating +
                ", engagementRating=" + engagementRating +
                ", satisfactionRating=" + satisfactionRating +
                ", anticipationRating=" + anticipationRating +
                '}';
    }
}