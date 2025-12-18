package ch.inf.usi.mindbricks.util.questionnaire;

/**
 * Utility class for calculating focus score based on questionnaire answers.
 */
final class FocusScoreCalculator {

    /**
     * Weight for the engagement rating in the focus score calculation.
     */
    private static final float WEIGHT_ENGAGEMENT = 0.35f;

    /**
     * Weight for the energy rating in the focus score calculation.
     */
    private static final float WEIGHT_ENERGY = 0.25f;

    /**
     * Weight for the satisfaction rating in the focus score calculation.
     */
    private static final float WEIGHT_SATISFACTION = 0.20f;

    /**
     * Weight for the enthusiasm rating in the focus score calculation.
     */
    private static final float WEIGHT_ENTHUSIASM = 0.15f;

    /**
     * Weight for the anticipation rating in the focus score calculation.
     */
    private static final float WEIGHT_ANTICIPATION = 0.05f;

    /**
     * Private constructor to prevent instantiation.
     */
    private FocusScoreCalculator() {
    }

    /**
     * Calculates the focus score based on the perceived productivity questionnaire answers.
     *
     * @param result The questionnaire result containing the answers.
     * @return The calculated focus score.
     */
    static float calculate(ProductivityQuestionnaireResult result) {
        // Weighted mean based on importance to perceived productivity
        float weightedScore =
                (result.engagement() * WEIGHT_ENGAGEMENT) +
                (result.energy() * WEIGHT_ENERGY) +
                (result.satisfaction() * WEIGHT_SATISFACTION) +
                (result.enthusiasm() * WEIGHT_ENTHUSIASM) +
                (result.anticipation() * WEIGHT_ANTICIPATION);

        // Formula: ((score - min) / (max - min)) * 100
        return ((weightedScore - ProductivityQuestionnaireConfig.MIN_RATING)
                /
                (ProductivityQuestionnaireConfig.MAX_RATING - ProductivityQuestionnaireConfig.MIN_RATING)) * 100f;
    }
}
