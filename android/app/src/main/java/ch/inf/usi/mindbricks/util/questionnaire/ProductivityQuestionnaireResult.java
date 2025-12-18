package ch.inf.usi.mindbricks.util.questionnaire;

/**
 * Represents the result of a detailed questionnaire.
 *
 * @author Luca Di Bello
 *
 * @param enthusiasm Rating for enthusiasm
 * @param energy Rating for energy
 * @param engagement Rating for engagement
 * @param satisfaction Rating for satisfaction
 * @param anticipation Rating for anticipation
 */
public record ProductivityQuestionnaireResult(
        int enthusiasm,
        int energy,
        int engagement,
        int satisfaction,
        int anticipation
) {
    /**
     * Constructs a new ProductivityQuestionnaireResult object.
     * <p>
     * Every
     *
     * @param enthusiasm Rating for enthusiasm
     * @param energy Rating for energy
     * @param engagement Rating for engagement
     * @param satisfaction Rating for satisfaction
     * @param anticipation Rating for anticipation
     */
    public ProductivityQuestionnaireResult {
        // ensure every field has a valid value
        String[] names = {"enthusiasm", "engagement", "energy", "satisfaction", "anticipation"};
        int[] values = {enthusiasm, engagement, energy, satisfaction, anticipation};
        for (int i = 0; i < names.length; i++) {
            // ensure that every value in the correct range
            if (values[i] < ProductivityQuestionnaireConfig.MIN_RATING ||
                    values[i] > ProductivityQuestionnaireConfig.MAX_RATING) {
                throw new IllegalArgumentException(names[i] + " must be between 1 and 5");
            }
        }
    }

    /**
     * Calculates the focus score based on the questionnaire answers.
     *
     * @return The calculated focus score in the range [0, 100].
     */
    public float getFocusScore() {
        return FocusScoreCalculator.calculate(this);
    }
}
