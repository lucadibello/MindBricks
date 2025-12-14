package ch.inf.usi.mindbricks.util.evaluation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.util.UserPreferencesManager;
import ch.inf.usi.mindbricks.model.evaluation.UserPreferences;


/**
 * Adaptive Break Manager
 *
 * Dynamically adjusts break duration based on PAM (Pleasure-Arousal-Motivation) scores.
 *
 * Evidence base:
 * - Ghazi et al. (2023): Adaptive Pomodoro apps improve productivity by 23% and user satisfaction
 * - When PAM total ≤ 15: Extend breaks by 2-3 minutes (prevent burnout)
 * - When PAM total ≥ 30: Keep breaks short (maintain flow state)
 * - When arousal < 20: Suggest physical activity break
 */
public class BreakManager {

    private static final String TAG = "BreakAdaptationManager";

    // Minimum break duration (safety floor)
    private static final int MIN_BREAK_DURATION = 2;

    // Maximum break duration (prevent excessive breaks)
    private static final int MAX_BREAK_DURATION = 20;

    private final Context context;
    private final UserPreferencesManager preferencesManager;

    public BreakManager(Context context) {
        this.context = context;
        this.preferencesManager = new UserPreferencesManager(context);
    }

    /**
     * Calculates adaptive break duration
     *
     * @param lastPAMScore The PAM score from the just-completed session
     * @param baseBreakMinutes Default break duration from preferences
     * @param isLongBreak Whether this is a long break (every 4 sessions)
     * @return Adapted break duration in minutes
     */
    public int calculateAdaptiveBreakDuration(PAMScore lastPAMScore,
                                              int baseBreakMinutes,
                                              boolean isLongBreak) {

        // If user disabled adaptive breaks, return base duration
        if (!preferencesManager.isAdaptiveBreaksEnabled()) {
            return baseBreakMinutes;
        }

        // If no PAM score available (first session), use base duration
        if (lastPAMScore == null) {
            Log.d(TAG, "No PAM score available, using base duration: " + baseBreakMinutes);
            return baseBreakMinutes;
        }

        UserPreferences prefs = preferencesManager.loadPreferences();
        UserPreferences.PAMThresholds thresholds = prefs.getPamThresholds();
        UserPreferences.BreakPreferences breakPrefs = prefs.getBreakPreferences();

        int adaptedDuration = baseBreakMinutes;
        String reason = "Standard break";

        int totalScore = lastPAMScore.getTotalScore();
        int arousalScore = lastPAMScore.getArousalScore();

        // LOW PAM (≤ 15):
        // Extend break significantly
        if (totalScore <= thresholds.getLowThreshold()) {
            int extension = isLongBreak ? 5 : 3;
            adaptedDuration = baseBreakMinutes + extension;
            reason = "Extended due to low energy/mood (PAM: " + totalScore + ")";

            Log.i(TAG, "LOW PAM detected: " + totalScore + " - Extending break by " + extension + " min");
        }

        // HIGH PAM (≥ 35):
        // Shorten break slightly to maintain flow
        else if (totalScore >= thresholds.getHighThreshold()) {
            // Reduce by 1-2 minutes but maintain minimum break
            int reduction = isLongBreak ? 2 : 1;
            adaptedDuration = Math.max(MIN_BREAK_DURATION, baseBreakMinutes - reduction);
            reason = "Shortened to maintain flow state (PAM: " + totalScore + ")";

            Log.i(TAG, "HIGH PAM detected: " + totalScore + " - Shortening break by " + reduction + " min");
        }

        // MEDIUM-LOW PAM (16-24):
        // Moderate extension
        else if (totalScore <= thresholds.getMediumThreshold()) {
            adaptedDuration = baseBreakMinutes + 2;
            reason = "Moderately extended (PAM: " + totalScore + ")";

            Log.i(TAG, "MEDIUM-LOW PAM detected: " + totalScore + " - Adding 2 min to break");
        }

        // LOW AROUSAL (<20):
        // Physical fatigue detected
        // Add extra time and suggest movement
        if (arousalScore < 20) {
            adaptedDuration += 2;
            reason += " + Physical fatigue detected - Movement recommended";

            Log.i(TAG, "LOW AROUSAL detected: " + arousalScore + " - Adding 2 min for movement");
        }

        // Apply user-defined maximum extension limit
        int maxExtension = breakPrefs.getMaxBreakExtension();
        int maxAllowed = baseBreakMinutes + maxExtension;
        adaptedDuration = Math.min(adaptedDuration, maxAllowed);

        // Apply absolute safety bounds
        adaptedDuration = Math.max(MIN_BREAK_DURATION, adaptedDuration);
        adaptedDuration = Math.min(MAX_BREAK_DURATION, adaptedDuration);

        Log.i(TAG, String.format("Break adaptation: %d min → %d min (%s)",
                baseBreakMinutes, adaptedDuration, reason));

        return adaptedDuration;
    }

    @SuppressLint("DefaultLocale")
    public String getBreakDurationExplanation(PAMScore lastPAMScore,
                                              int baseBreakMinutes,
                                              int adaptedDuration) {

        if (lastPAMScore == null || baseBreakMinutes == adaptedDuration) {
            return "Standard break duration";
        }

        int totalScore = lastPAMScore.getTotalScore();
        int difference = adaptedDuration - baseBreakMinutes;

        if (difference > 0) {
            // Break was extended
            if (totalScore <= 15) {
                return String.format("Break extended to %d min - Your energy is low. Take time to recharge! 🔋",
                        adaptedDuration);
            } else if (lastPAMScore.getArousalScore() < 20) {
                return String.format("Break extended to %d min - Physical fatigue detected. Try a short walk! 🚶",
                        adaptedDuration);
            } else {
                return String.format("Break extended to %d min - You deserve a longer rest! ☕",
                        adaptedDuration);
            }
        } else if (difference < 0) {
            // Break was shortened
            return String.format("Break shortened to %d min - You're in the zone! Stay focused! 🔥",
                    adaptedDuration);
        } else {
            return String.format("%d min break - Keep up the good work! 👍", adaptedDuration);
        }
    }

    public String[] suggestBreakActivities(PAMScore lastPAMScore) {

        if (lastPAMScore == null) {
            return new String[]{"Stretch", "Hydrate", "Rest eyes"};
        }

        UserPreferences prefs = preferencesManager.loadPreferences();

        int arousalScore = lastPAMScore.getArousalScore();
        int pleasureScore = lastPAMScore.getPleasureScore();
        int motivationScore = lastPAMScore.getMotivationScore();

        // Low arousal = need physical activation
        if (arousalScore < 25) {
            return new String[]{
                    "Take a 5-minute walk",
                    "Do light stretching exercises",
                    "Step outside for fresh air",
                    "Do jumping jacks or desk exercises"
            };
        }

        // Low pleasure = need mood boost
        if (pleasureScore < 25) {
            return new String[]{
                    "Listen to your favorite music",
                    "Watch a short funny video",
                    "Call a friend briefly",
                    "Enjoy a favorite snack"
            };
        }

        // Low motivation = need inspiration/perspective
        if (motivationScore < 25) {
            return new String[]{
                    "Review your progress and goals",
                    "Read motivational quotes",
                    "Visualize completing your task",
                    "Remind yourself why this matters"
            };
        }

        // High PAM = maintain state with light activities
        if (lastPAMScore.getTotalScore() >= 60) {
            return new String[]{
                    "Quick stretch",
                    "Hydrate",
                    "Deep breathing (2 minutes)",
                    "Look away from screen (20-20-20 rule)"
            };
        }

        // Default balanced activities
        return new String[]{
                "Stretch and move around",
                "Drink water",
                "Rest your eyes",
                "Practice mindful breathing"
        };
    }

    public boolean shouldSuggestBreathingExercise(PAMScore lastPAMScore) {
        if (lastPAMScore == null) return false;

        return lastPAMScore.getArousalScore() >= 60 &&
                lastPAMScore.getPleasureScore() < 40;
    }

    public boolean shouldSuggestPhysicalActivity(PAMScore lastPAMScore) {
        if (lastPAMScore == null) return false;

        return lastPAMScore.getArousalScore() < 25;
    }

    public boolean shouldSuggestEnvironmentChange(PAMScore lastPAMScore) {
        if (lastPAMScore == null) return false;

        return lastPAMScore.getPleasureScore() < 20 &&
                lastPAMScore.getTotalScore() < 25;
    }
}