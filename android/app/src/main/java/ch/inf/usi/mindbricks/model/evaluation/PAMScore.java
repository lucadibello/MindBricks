package ch.inf.usi.mindbricks.model.evaluation;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.model.visual.StudySession;

/**
 * PAM (Pleasure-Arousal-Motivation) Score Model
 *
 * Based on research showing that affect has three independent dimensions:
 * - Pleasure: Satisfaction and enjoyment (hedonic state)
 * - Arousal: Energy and alertness (activation state)
 * - Motivation: Willingness to continue (persistence state)
 */
@Entity(tableName = "pam_scores",
        foreignKeys = {
                @ForeignKey(
                        entity = StudySession.class,
                        parentColumns = "id",
                        childColumns = "sessionId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = SessionQuestionnaire.class,
                        parentColumns = "id",
                        childColumns = "questionnaireId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "sessionId"),
                @Index(value = "questionnaireId"),
                @Index(value = "timestamp")
        })
public class PAMScore {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long sessionId;
    private long questionnaireId;
    private long timestamp;

    // The three PAM dimensions (0-100 scale)
    private int pleasureScore;      // Satisfaction + Enthusiasm
    private int arousalScore;       // Energy + Engagement
    private int motivationScore;    // Anticipation + Enthusiasm

    // Overall composite score
    private int totalScore;

    // Derived affective state classification
    private String affectiveState;

    // For tracking changes over time
    private Integer previousTotalScore;

    // Constructors

    public PAMScore() {}

    @Ignore
    public PAMScore(long sessionId, long questionnaireId, long timestamp,
                    int pleasureScore, int arousalScore, int motivationScore) {
        this.sessionId = sessionId;
        this.questionnaireId = questionnaireId;
        this.timestamp = timestamp;
        this.pleasureScore = pleasureScore;
        this.arousalScore = arousalScore;
        this.motivationScore = motivationScore;
        this.totalScore = (pleasureScore + arousalScore + motivationScore) / 3;
        this.affectiveState = determineAffectiveState();
    }

    /**
     * Creates PAMScore from SessionQuestionnaire responses
     *
     * Mapping from 5-dimension questionnaire to 3-dimension PAM:
     * - Pleasure = satisfaction (60%) + enthusiasm (40%)
     * - Arousal = energy (50%) + engagement (50%)
     * - Motivation = anticipation (50%) + enthusiasm (50%)
     */
    public static PAMScore fromQuestionnaire(SessionQuestionnaire questionnaire) {
        int enthusiasm = questionnaire.getEnthusiasmRating();
        int energy = questionnaire.getEnergyRating();
        int engagement = questionnaire.getEngagementRating();
        int satisfaction = questionnaire.getSatisfactionRating();
        int anticipation = questionnaire.getAnticipationRating();

        // Calculate PAM dimensions (convert to 0-100 scale)
        int pleasure = calculatePleasure(satisfaction, enthusiasm);
        int arousal = calculateArousal(energy, engagement);
        int motivation = calculateMotivation(anticipation, enthusiasm);

        PAMScore pamScore = new PAMScore(
                questionnaire.getSessionId(),
                questionnaire.getId(),
                questionnaire.getTimeStamp(),
                pleasure,
                arousal,
                motivation
        );

        return pamScore;
    }

    /**
     * Pleasure = how good/bad the experience feels
     */
    private static int calculatePleasure(int satisfaction, int enthusiasm) {
        float weighted = (satisfaction * 0.6f) + (enthusiasm * 0.4f);
        return normalizeToHundred(weighted);
    }

    /**
     * Arousal = level of mental/physical activation
     */
    private static int calculateArousal(int energy, int engagement) {
        float weighted = (energy * 0.5f) + (engagement * 0.5f);
        return normalizeToHundred(weighted);
    }

    /**
     * Motivation = willingness to continue or engage further
     */
    private static int calculateMotivation(int anticipation, int enthusiasm) {
        float weighted = (anticipation * 0.5f) + (enthusiasm * 0.5f);
        return normalizeToHundred(weighted);
    }


    private static int normalizeToHundred(float ratingValue) {
        float normalized = ((ratingValue - 1.0f) / (7.0f - 1.0f)) * 100f;
        return Math.round(normalized);
    }

    private String determineAffectiveState() {
        // High arousal + high pleasure = "Energized" / "Flow State"
        if (arousalScore >= 60 && pleasureScore >= 60) {
            return "Energized";
        }
        // Low arousal + high pleasure = "Calm" / "Relaxed"
        else if (arousalScore < 40 && pleasureScore >= 60) {
            return "Calm";
        }
        // High arousal + low pleasure = "Stressed" / "Anxious"
        else if (arousalScore >= 60 && pleasureScore < 40) {
            return "Stressed";
        }
        // Low arousal + low pleasure = "Fatigued" / "Burned Out"
        else if (arousalScore < 40 && pleasureScore < 40) {
            return "Fatigued";
        }
        // Moderate levels = "Neutral"
        else {
            return "Neutral";
        }
    }

    // Threshold checks
    public boolean isBelowThreshold(int threshold) {
        return totalScore <= threshold;
    }

    public boolean isAboveThreshold(int threshold) {
        return totalScore >= threshold;
    }

    public boolean hasSharpDrop(int dropThreshold) {
        if (previousTotalScore == null) return false;
        int drop = previousTotalScore - totalScore;
        return drop >= dropThreshold;
    }


    public String getLowestDimension() {
        int minScore = Math.min(pleasureScore, Math.min(arousalScore, motivationScore));

        if (pleasureScore == minScore) return "pleasure";
        if (arousalScore == minScore) return "arousal";
        return "motivation";
    }

    public String getStateDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(affectiveState);

        if (totalScore < 20) {
            desc.append(" - Consider taking a break");
        } else if (totalScore < 40) {
            desc.append(" - You may benefit from a change of pace");
        } else if (totalScore >= 70) {
            desc.append(" - Great momentum!");
        }

        return desc.toString();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public long getQuestionnaireId() { return questionnaireId; }
    public void setQuestionnaireId(long questionnaireId) { this.questionnaireId = questionnaireId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getPleasureScore() { return pleasureScore; }
    public void setPleasureScore(int pleasureScore) {
        this.pleasureScore = pleasureScore;
        recalculateTotal();
    }

    public int getArousalScore() { return arousalScore; }
    public void setArousalScore(int arousalScore) {
        this.arousalScore = arousalScore;
        recalculateTotal();
    }

    public int getMotivationScore() { return motivationScore; }
    public void setMotivationScore(int motivationScore) {
        this.motivationScore = motivationScore;
        recalculateTotal();
    }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public String getAffectiveState() { return affectiveState; }
    public void setAffectiveState(String affectiveState) { this.affectiveState = affectiveState; }

    public Integer getPreviousTotalScore() { return previousTotalScore; }
    public void setPreviousTotalScore(Integer previousTotalScore) {
        this.previousTotalScore = previousTotalScore;
    }

    private void recalculateTotal() {
        this.totalScore = (pleasureScore + arousalScore + motivationScore) / 3;
        this.affectiveState = determineAffectiveState();
    }

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public String toString() {
        return String.format("PAMScore{total=%d, P=%d, A=%d, M=%d, state='%s'}",
                totalScore, pleasureScore, arousalScore, motivationScore, affectiveState);
    }
}