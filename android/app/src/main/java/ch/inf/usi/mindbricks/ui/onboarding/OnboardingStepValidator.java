package ch.inf.usi.mindbricks.ui.onboarding;

/**
 * Interface implemented by onboarding fragments to ensure each step is valid.
 */
public interface OnboardingStepValidator {

    /**
     * @return true if the user can move away from this step, otherwise false.
     */
    boolean validateStep();
}
