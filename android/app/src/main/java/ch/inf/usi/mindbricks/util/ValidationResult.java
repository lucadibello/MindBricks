package ch.inf.usi.mindbricks.util;

import androidx.annotation.StringRes;

/**
 * Record class that wraps validation results for easier management.
 */
public record ValidationResult(boolean isValid, @StringRes int errorResId) {
    public static ValidationResult ok() {
        return new ValidationResult(true, 0);
    }

    public static ValidationResult error(@StringRes int errorResId) {
        return new ValidationResult(false, errorResId);
    }
}
