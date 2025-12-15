package ch.inf.usi.mindbricks.util.evaluation;

/**
 * Utility class for calculating focus score based on questionnaire answers.
 */
public final class FocusScoreCalculator {

    // Weights for each questionnaire dimension
    private static final float WEIGHT_ENGAGEMENT = 0.35f;    // Engagement - most important
    private static final float WEIGHT_ENERGY = 0.25f;        // Alertness and vitality
    private static final float WEIGHT_SATISFACTION = 0.20f;  // Quality indicator
    private static final float WEIGHT_ENTHUSIASM = 0.15f;    // Motivation
    private static final float WEIGHT_ANTICIPATION = 0.05f;  // Sustainability

    // Rating scale bounds (rating 1-7 but focus score is 0-100)
    private static final float MIN_RATING = 1.0f;
    private static final float MAX_RATING = 7.0f;

    private FocusScoreCalculator() {
        // Prevent instantiation
    }

    public static float calculate(int enthusiasm, int energy, int engagement,
                                  int satisfaction, int anticipation) {
        // Weighted mean based on importance to perceived productivity
        float weightedScore =
                (engagement * WEIGHT_ENGAGEMENT) +
                (energy * WEIGHT_ENERGY) +
                (satisfaction * WEIGHT_SATISFACTION) +
                (enthusiasm * WEIGHT_ENTHUSIASM) +
                (anticipation * WEIGHT_ANTICIPATION);

        // Formula: ((score - min) / (max - min)) * 100
        return ((weightedScore - MIN_RATING) / (MAX_RATING - MIN_RATING)) * 100f;
    }

    /**
     * Gets a textual description of the focus score.
     * FIXME: this can be used in dialogs in the "analytics" fragment. To be used later.
     *
     * @param focusScore The calculated focus score (0-100)
     * @return A descriptive label for the score
     */
    public static String getScoreDescription(float focusScore) {
        if (focusScore >= 90) {
            return "Exceptional";
        } else if (focusScore >= 75) {
            return "Very Good";
        } else if (focusScore >= 60) {
            return "Good";
        } else if (focusScore >= 40) {
            return "Moderate";
        } else {
            return "Low";
        }
    }
}
