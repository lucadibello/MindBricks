package ch.inf.usi.mindbricks.util;

import androidx.annotation.Nullable;

/**
 * Record class that wraps validation results for easier management.
 */
public record ValidationResult(boolean isValid, @Nullable String msg) {
    public static ValidationResult ok() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult error (String msg) {
        return new ValidationResult(false, msg);
    }
}
