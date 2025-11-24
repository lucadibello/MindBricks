package ch.inf.usi.mindbricks.util.validators;

import android.text.TextUtils;

import java.util.Objects;
import java.util.regex.Pattern;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.ValidationResult;

/**
 * Utility class that provides validation rules for user profile
 * data (name, sprint length, tags) for reusability across the application
 */
public final class ProfileValidator {

    /**
     * The validation pattern for names.
     * (Source: <a href="https://stackoverflow.com/questions/888838/regular-expression-for-validating-names-and-surnames">stackoverflow</a>)
     */
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L} .'-]+$");

    private ProfileValidator() {
        // not instantiable
    }

    /**
     * Validates the name field, ensuring it is not empty.
     * @param name The name to validate
     * @return A ValidationResult indicating the validation result
     */
    public static ValidationResult validateName(String name) {
        if (TextUtils.isEmpty(name == null ? "" : name.trim())) {
            return ValidationResult.error(R.string.validation_error_name_required);
        }
        assert name != null; // after the previous check -> this is enforced!
        String normalized = name.trim();
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            return ValidationResult.error(R.string.validation_error_name_format);
        }
        return ValidationResult.ok();
    }

    /**
     * Validates the sprint length field, ensuring it is a valid integer greater than zero.
     * @param sprintLength The sprint length to validate
     * @return A ValidationResult indicating the validation result
     */
    public static ValidationResult validateSprintLength(String sprintLength) {
        if (TextUtils.isEmpty(sprintLength == null ? "" : sprintLength.trim())) {
            return ValidationResult.error(R.string.validation_error_sprint_required);
        }
        try {
            Objects.requireNonNull(sprintLength, "Sprint length cannot be null");
            int sprintMinutes = Integer.parseInt(sprintLength.trim());
            if (sprintMinutes <= 0) {
                return ValidationResult.error(R.string.validation_error_sprint_invalid);
            }
        } catch (NumberFormatException e) {
            return ValidationResult.error(R.string.validation_error_sprint_invalid);
        }
        return ValidationResult.ok();
    }
}
