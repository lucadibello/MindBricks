package ch.inf.usi.mindbricks.util.validators;

import android.content.Context;
import android.text.TextUtils;

import java.util.regex.Pattern;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.ValidationResult;

/**
 * Validation rules for user-defined tags (titles).
 */
public final class TagValidator {

    /**
     * The validation pattern for tag names.
     * (Source: <a href="https://stackoverflow.com/questions/888838/regular-expression-for-validating-names-and-surnames">stackoverflow</a>)
     */
    private static final Pattern TITLE_PATTERN =
            Pattern.compile("^[\\p{L} .'-]+$");

    private TagValidator() {
        // no instances
    }

    public static ValidationResult validateTitle(String title, Context ctx) {
        String normalized = title == null ? "" : title.trim();
        if (TextUtils.isEmpty(normalized)) {
            return ValidationResult.error(ctx.getString(R.string.onboarding_error_tag_name_required));
        }

        // Get max length from shared resource
        int maxTagLength = ctx.getResources().getInteger(R.integer.tag_name_max_length);
        if (normalized.length() > maxTagLength) {
            return ValidationResult.error(ctx.getString(R.string.validation_error_tag_name_too_long, maxTagLength));
        }

        if (!TITLE_PATTERN.matcher(normalized).matches()) {
            return ValidationResult.error(ctx.getString(R.string.validation_error_tag_name_format));
        }
        return ValidationResult.ok();
    }
}
