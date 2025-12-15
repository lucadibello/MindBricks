package ch.inf.usi.mindbricks.util.evaluation;

import android.annotation.SuppressLint;
import android.util.Log;

import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.model.evaluation.UserPreferences;

/**
 * Affective Feedback Manager
 *
 * Detects sharp drops in PAM scores and triggers immediate interventions
 * to prevent emotional cascades and task abandonment.
 *
 * Evidence base:
 * - Adaptive feedback systems reduce anxiety by 27% when interventions
 *   are delivered immediately after affect deterioration
 * - Sharp drops (≥15 points) predict task abandonment within 2 sessions
 * - Timely emotional regulation interventions improve task persistence by 31%
 *
 * This manager implements "just-in-time" affective support, providing
 * interventions at the moment when they're most effective.
 *
 * Same sources as in TaskDifficultyRecommender.
 */
public class FeedbackManager {

    private static final String TAG = "FeedbackManager";

    public enum InterventionType {
        NONE("No intervention needed"),
        BREATHING_EXERCISE("Guided breathing exercise"),
        ENCOURAGING_MESSAGE("Motivational message"),
        SUGGEST_BREAK("Take an immediate break"),
        SUGGEST_ENVIRONMENT_CHANGE("Change study location"),
        SUGGEST_TASK_SWITCH("Switch to a different task"),
        STRESS_ALERT("Stress management needed");

        private final String description;

        InterventionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class FeedbackIntervention {
        private InterventionType type;
        private String title;
        private String message;
        private String actionButton;
        private int urgencyLevel;

        public FeedbackIntervention(InterventionType type, String title,
                                    String message, String actionButton) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.actionButton = actionButton;
            this.urgencyLevel = 5;
        }

        public InterventionType getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getActionButton() { return actionButton; }
        public int getUrgencyLevel() { return urgencyLevel; }

        public void setUrgencyLevel(int level) {
            this.urgencyLevel = Math.max(0, Math.min(10, level));
        }

        public boolean shouldShowImmediately() {
            return urgencyLevel >= 7;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("FeedbackIntervention{type=%s, urgency=%d, title='%s'}",
                    type, urgencyLevel, title);
        }
    }

    /**
     * Main detection method: Analyzes PAM score changes and generates interventions
     *
     * @param previousScore PAM score from the previous session (can be null)
     * @param currentScore PAM score from the just-completed session
     * @param thresholds User's PAM thresholds from preferences
     * @return FeedbackIntervention object with recommended action
     */
    public static FeedbackIntervention detectAndRecommend(PAMScore previousScore,
                                                          PAMScore currentScore,
                                                          UserPreferences.PAMThresholds thresholds) {

        // If no current score, no intervention needed
        if (currentScore == null) {
            return createNoIntervention();
        }

        // If no previous score, check absolute levels only
        if (previousScore == null) {
            return checkAbsoluteLevels(currentScore, thresholds);
        }

        // Calculate drop
        int drop = previousScore.getTotalScore() - currentScore.getTotalScore();

        Log.i(TAG, String.format("PAM change: %d → %d (drop: %d)",
                previousScore.getTotalScore(), currentScore.getTotalScore(), drop));

        // CRITICAL DROP (≥25 points):
        // Immediate intervention needed
        if (drop >= 25) {
            return generateCriticalDropIntervention(currentScore, drop);
        }

        // SHARP DROP (≥15 points):
        // Intervention recommended
        if (drop >= thresholds.getSharpDropThreshold()) {
            return generateSharpDropIntervention(currentScore, drop);
        }

        // MODERATE DROP (10-14 points):
        // Gentle intervention
        if (drop >= 10) {
            return generateModerateDropIntervention(currentScore, drop);
        }

        // No significant drop, check absolute levels
        return checkAbsoluteLevels(currentScore, thresholds);
    }

    @SuppressLint("DefaultLocale")
    private static FeedbackIntervention generateCriticalDropIntervention(PAMScore currentScore, int drop) {

        FeedbackIntervention intervention;

        // Determine based on which dimension dropped most
        String lowestDim = currentScore.getLowestDimension();

        if ("arousal".equals(lowestDim) && currentScore.getArousalScore() < 15) {
            // Severe fatigue
            intervention = new FeedbackIntervention(
                    InterventionType.SUGGEST_BREAK,
                    "You're Exhausted",
                    String.format("Your energy dropped by %d points. This is a sign of severe fatigue. " +
                            "Please take a 15-20 minute break now.", drop),
                    "Take Break Now"
            );
            intervention.setUrgencyLevel(9);

        } else if ("pleasure".equals(lowestDim) && currentScore.getPleasureScore() < 15) {
            // Severe frustration/dissatisfaction
            intervention = new FeedbackIntervention(
                    InterventionType.SUGGEST_TASK_SWITCH,
                    "This Isn't Working",
                    String.format("Your satisfaction dropped by %d points. This task may be too frustrating " +
                            "right now. Consider switching to something else.", drop),
                    "Switch Task"
            );
            intervention.setUrgencyLevel(8);

        } else {
            // General critical drop
            intervention = new FeedbackIntervention(
                    InterventionType.STRESS_ALERT,
                    "Significant Stress Detected",
                    String.format("Your wellbeing score dropped by %d points. This is concerning. " +
                            "Please take a break and reassess your approach.", drop),
                    "Take Care Break"
            );
            intervention.setUrgencyLevel(9);
        }

        return intervention;
    }


    @SuppressLint("DefaultLocale")
    private static FeedbackIntervention generateSharpDropIntervention(PAMScore currentScore, int drop) {

        FeedbackIntervention intervention;

        int arousal = currentScore.getArousalScore();
        int pleasure = currentScore.getPleasureScore();

        // High arousal + low pleasure = Stress/Anxiety
        if (arousal >= 60 && pleasure < 40) {
            intervention = new FeedbackIntervention(
                    InterventionType.BREATHING_EXERCISE,
                    "Stress Detected",
                    String.format("Your stress level increased (drop: %d points). " +
                            "A 2-minute breathing exercise can help you regain calm.", drop),
                    "Start Breathing Exercise"
            );
            intervention.setUrgencyLevel(7);

            // Low arousal (fatigue)
        } else if (arousal < 25) {
            intervention = new FeedbackIntervention(
                    InterventionType.SUGGEST_BREAK,
                    "Energy Depleted",
                    String.format("Your energy dropped significantly (%d points). " +
                            "Take a 10-minute break with some physical movement.", drop),
                    "Take Movement Break"
            );
            intervention.setUrgencyLevel(7);

            // Low pleasure (frustration)
        } else if (pleasure < 30) {
            intervention = new FeedbackIntervention(
                    InterventionType.ENCOURAGING_MESSAGE,
                    "Feeling Frustrated?",
                    String.format("That last session was tough (drop: %d points). " +
                            "Remember: difficulty is temporary. You're making progress!", drop),
                    "Keep Going"
            );
            intervention.setUrgencyLevel(6);

        } else {
            intervention = new FeedbackIntervention(
                    InterventionType.SUGGEST_BREAK,
                    "Take a Breather",
                    String.format("Your wellbeing dropped (%d points). " +
                            "A short break might help you reset.", drop),
                    "Take Short Break"
            );
            intervention.setUrgencyLevel(6);
        }

        return intervention;
    }

    private static FeedbackIntervention generateModerateDropIntervention(PAMScore currentScore, int drop) {

        // Gentle encouragement
        FeedbackIntervention intervention = new FeedbackIntervention(
                InterventionType.ENCOURAGING_MESSAGE,
                "You've Got This! 💪",
                String.format("That session was a bit harder than usual. " +
                        "Take a moment to acknowledge your effort. Small steps add up!"),
                "Continue"
        );
        intervention.setUrgencyLevel(4);

        return intervention;
    }


    private static FeedbackIntervention checkAbsoluteLevels(PAMScore currentScore,
                                                            UserPreferences.PAMThresholds thresholds) {

        int totalScore = currentScore.getTotalScore();

        // Very low absolute score
        if (totalScore <= thresholds.getLowThreshold()) {

            String lowestDim = currentScore.getLowestDimension();

            if ("arousal".equals(lowestDim)) {
                return new FeedbackIntervention(
                        InterventionType.SUGGEST_ENVIRONMENT_CHANGE,
                        "Low Energy Detected",
                        "Your energy is very low. Consider changing your environment or taking a walk.",
                        "Change Environment"
                );
            } else {
                return new FeedbackIntervention(
                        InterventionType.ENCOURAGING_MESSAGE,
                        "Tough Session",
                        "That was a challenging session. Be kind to yourself - you're doing your best!",
                        "OK"
                );
            }
        }

        // High stress (high arousal + low pleasure)
        if (currentScore.getArousalScore() >= 60 && currentScore.getPleasureScore() < 40) {
            FeedbackIntervention intervention = new FeedbackIntervention(
                    InterventionType.BREATHING_EXERCISE,
                    "Feeling Stressed?",
                    "You seem tense. Try a quick 2-minute breathing exercise to calm your mind.",
                    "Start Exercise"
            );
            intervention.setUrgencyLevel(5);
            return intervention;
        }

        return createNoIntervention();
    }

    private static FeedbackIntervention createNoIntervention() {
        return new FeedbackIntervention(
                InterventionType.NONE,
                "",
                "",
                ""
        );
    }

    public static String getEncouragingMessage(PAMScore score) {

        if (score == null) {
            return "You're doing great! Keep up the good work!";
        }

        int totalScore = score.getTotalScore();

        if (totalScore >= 70) {
            return "Outstanding! You're in an excellent flow state! 🌟";
        } else if (totalScore >= 50) {
            return "Great work! You're making solid progress! 💪";
        } else if (totalScore >= 30) {
            return "You're doing well. Every step forward counts! 👍";
        } else if (totalScore >= 20) {
            return "That was tough, but you persisted. That takes strength! 🔥";
        } else {
            return "Be kind to yourself. Progress isn't always linear. You've got this! 💙";
        }
    }

    public static String[] getBreathingExerciseSteps() {
        return new String[]{
                "Find a comfortable position",
                "Close your eyes or soften your gaze",
                "Breathe in slowly through your nose for 4 counts",
                "Hold your breath for 4 counts",
                "Exhale slowly through your mouth for 4 counts",
                "Hold empty lungs for 4 counts",
                "Repeat this cycle 4 times",
                "Notice how you feel now"
        };
    }
}