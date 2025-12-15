package ch.inf.usi.mindbricks.util.evaluation;

import android.content.Context;
import android.util.Log;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.util.SoundPlayer;
import ch.inf.usi.mindbricks.util.UserPreferencesManager;
import ch.inf.usi.mindbricks.util.VibrationHelper;
import ch.inf.usi.mindbricks.model.evaluation.UserPreferences;


/**
 * Environmental Cue Manager
 *
 * Provides multi-sensory feedback (sound, haptic, visual) based on PAM scores
 * to create positive reinforcement and mood regulation.
 *
 * Evidence base:
 * - Gamified Pomodoro designs with affect-aware cues increase engagement by 34%
 * - Multi-sensory feedback creates stronger memory associations (neuroscience)
 * - Operant conditioning: immediate reinforcement strengthens desired behaviors
 * - Auditory cues: 300ms response time (fastest emotional processing)
 * - Haptic cues: creates physical memory association
 * - Visual cues: sustains motivation through persistent reminders
 *
 * Same sources as in TaskDifficultyRecommender.
 */
public class CueManager {

    private static final String TAG = "CueManager";

    private final Context context;
    private final UserPreferencesManager preferencesManager;

    public CueManager(Context context) {
        this.context = context;
        this.preferencesManager = new UserPreferencesManager(context);
    }

    /**
     * Main method: Applies all environmental cues based on PAM score
     *
     * This coordinates sound, haptic, and visual feedback to create
     * a cohesive multi-sensory experience.
     *
     * @param pamScore The PAM score from the just-completed session
     */
    public void applyCues(PAMScore pamScore) {

        if (pamScore == null) {
            Log.w(TAG, "No PAM score provided, skipping cues");
            return;
        }

        UserPreferences prefs = preferencesManager.loadPreferences();
        UserPreferences.PAMThresholds thresholds = prefs.getPamThresholds();
        UserPreferences.EnvironmentalPreferences envPrefs = prefs.getEnvironmentalPreferences();

        int totalScore = pamScore.getTotalScore();

        Log.i(TAG, String.format("Applying environmental cues for PAM score: %d (%s)",
                totalScore, pamScore.getAffectiveState()));

        // Apply haptic feedback
        if (envPrefs.getHapticFeedback().isEnabled()) {
            applyHapticCue(totalScore, thresholds, envPrefs.getHapticFeedback());
        }

        // Visual cues are handled by UI components
        // This method just logs what should be displayed
        if (envPrefs.getVisualCues().isEnabled()) {
            logVisualCues(totalScore, thresholds, envPrefs.getVisualCues());
        }
    }

    private void applyHapticCue(int totalScore,
                                UserPreferences.PAMThresholds thresholds,
                                UserPreferences.HapticFeedback hapticPrefs) {

        String intensity = hapticPrefs.getIntensity();

        // HIGH PAM (≥35):
        // Celebratory vibration pattern
        if (totalScore >= thresholds.getHighThreshold()) {
            Log.i(TAG, "Applying HIGH PAM haptic (celebratory)");

            // Multiple short pulses = celebration
            VibrationHelper.vibrate(context, VibrationHelper.VibrationType.CYCLE_COMPLETE);

        }
        // MEDIUM PAM (20-34):
        // Standard haptic
        else if (totalScore >= thresholds.getMediumThreshold()) {
            Log.i(TAG, "Applying MEDIUM PAM haptic (standard)");

            // Single medium pulse
            VibrationHelper.vibrate(context, VibrationHelper.VibrationType.CYCLE_COMPLETE);

        }
        // LOW PAM (≤19):
        // Gentle/calming vibration
        else {
            Log.i(TAG, "Applying LOW PAM haptic (gentle)");

            // Long, gentle pulse = calming
            // Using a softer vibration pattern
            if ("low".equalsIgnoreCase(intensity)) {
                VibrationHelper.vibrate(context, VibrationHelper.VibrationType.CYCLE_COMPLETE);
            } else {
                VibrationHelper.vibrate(context, VibrationHelper.VibrationType.CYCLE_COMPLETE);
            }
        }
    }

    private void logVisualCues(int totalScore,
                               UserPreferences.PAMThresholds thresholds,
                               UserPreferences.VisualCues visualPrefs) {

        // HIGH PAM:
        // Golden badge, bright colors
        if (totalScore >= thresholds.getHighThreshold()) {
            Log.i(TAG, "Visual cue: GOLDEN BADGE, bright warm colors 🏆");

        }
        // MEDIUM-HIGH PAM:
        // Standard badge
        else if (totalScore >= thresholds.getMediumThreshold()) {
            Log.i(TAG, "Visual cue: STANDARD BADGE, neutral colors ✓");

        }
        // LOW PAM:
        // Calming visuals, dimmed screen
        else if (totalScore <= thresholds.getLowThreshold()) {
            Log.i(TAG, "Visual cue: CALMING MODE, dim screen, soft colors 💙");

            if (visualPrefs.isDimScreenOnLowPAM()) {
                Log.i(TAG, "Recommendation: Reduce screen brightness by 20-30%");
            }

        }
        // MEDIUM-LOW PAM:
        // Neutral visuals
        else {
            Log.i(TAG, "Visual cue: NEUTRAL, standard display");
        }

        // Badge display
        if (visualPrefs.isShowBadges()) {
            String badge = getBadgeForScore(totalScore, thresholds);
            Log.i(TAG, "Show badge: " + badge);
        }
    }

    public String getBadgeForScore(int totalScore, UserPreferences.PAMThresholds thresholds) {
        if (totalScore >= thresholds.getHighThreshold()) {
            return "🏆";
        } else if (totalScore >= thresholds.getMediumThreshold()) {
            return "⭐";
        } else if (totalScore >= 20) {
            return "💪";
        } else {
            return "💙";
        }
    }

    public int getColorForScore(int totalScore, UserPreferences.PAMThresholds thresholds) {
        if (totalScore >= thresholds.getHighThreshold()) {
            return R.color.pam_high;
        } else if (totalScore >= thresholds.getMediumThreshold()) {
            return R.color.pam_medium;
        } else if (totalScore <= thresholds.getLowThreshold()) {
            return R.color.pam_low;
        } else {
            return R.color.pam_medium_low;
        }
    }

    public String getMotivationalMessage(PAMScore pamScore) {

        if (pamScore == null) {
            return "Great work! 👍";
        }

        int totalScore = pamScore.getTotalScore();
        UserPreferences prefs = preferencesManager.loadPreferences();
        UserPreferences.PAMThresholds thresholds = prefs.getPamThresholds();

        // HIGH PAM
        if (totalScore >= thresholds.getHighThreshold()) {
            String[] messages = {
                    "You're on fire! Keep that momentum going! 🔥",
                    "Excellent focus! You're in the zone! 🎯",
                    "Outstanding performance! You're crushing it! 🌟",
                    "Peak productivity unlocked! Amazing! ⚡"
            };
            return messages[(int) (Math.random() * messages.length)];
        }

        // MEDIUM-HIGH PAM
        else if (totalScore >= thresholds.getMediumThreshold()) {
            String[] messages = {
                    "Great work! You're making solid progress! 💪",
                    "Nice session! Keep up the good work! ⭐",
                    "You're doing well! Stay consistent! 👍",
                    "Good focus! You're on the right track! ✨"
            };
            return messages[(int) (Math.random() * messages.length)];
        }

        // MEDIUM-LOW PAM
        else if (totalScore >= 20) {
            String[] messages = {
                    "That took effort. You persisted! 💪",
                    "Not easy, but you did it! 👏",
                    "Every step counts. Keep going! 🚀",
                    "You pushed through. That matters! ⭐"
            };
            return messages[(int) (Math.random() * messages.length)];
        }

        // LOW PAM
        else {
            String[] messages = {
                    "That was tough. Be kind to yourself. 💙",
                    "You showed up. That's what matters. 🌱",
                    "Progress isn't linear. You're doing great! 💚",
                    "It's okay to struggle. You're still moving forward! 🌈"
            };
            return messages[(int) (Math.random() * messages.length)];
        }
    }

    /**
     * Determines if screen dimming should be suggested
     */
    public boolean shouldDimScreen(PAMScore pamScore) {
        if (pamScore == null) return false;

        UserPreferences prefs = preferencesManager.loadPreferences();
        UserPreferences.VisualCues visualPrefs = prefs.getEnvironmentalPreferences().getVisualCues();
        UserPreferences.PAMThresholds thresholds = prefs.getPamThresholds();

        return visualPrefs.isDimScreenOnLowPAM() &&
                pamScore.getTotalScore() <= thresholds.getLowThreshold();
    }

    /**
     * Gets recommended screen brightness adjustment (-30 to +30 percent)
     */
    public int getScreenBrightnessAdjustment(PAMScore pamScore) {
        if (pamScore == null) return 0;

        int totalScore = pamScore.getTotalScore();
        UserPreferences prefs = preferencesManager.loadPreferences();
        UserPreferences.PAMThresholds thresholds = prefs.getPamThresholds();

        if (totalScore <= thresholds.getLowThreshold()) {
            return -25; // Reduce brightness by 25%
        } else if (totalScore >= thresholds.getHighThreshold()) {
            return +10; // Increase brightness by 10% (for alertness)
        } else {
            return 0; // No change
        }
    }
}