package ch.inf.usi.mindbricks.util;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.util.validators.TagValidator;

/**
 * Utility class to manage tag creation, display, and deletion.
 */
public class TagManager {

    /**
     * Callback interface for when a tag is created
     */
    public interface OnTagCreatedListener {
        void onTagCreated(Tag tag);
    }

    private final Fragment fragment;
    private final ChipGroup tagChipGroup;
    private final MaterialTextView tagEmptyState;
    private final PreferencesManager prefs;
    private final List<Tag> tags;

    /**
     * Creates a new TagManager
     *
     * @param fragment The fragment in which the tags will be loaded
     * @param tagChipGroup The ChipGroup to contain the loaded tags
     * @param tagEmptyState The TextView element to show when no tags exist
     * @param prefs PreferencesManager instance needed to store tags in the app. preferences
     */
    public TagManager(Fragment fragment, ChipGroup tagChipGroup, MaterialTextView tagEmptyState, PreferencesManager prefs) {
        this.fragment = fragment;
        this.tagChipGroup = tagChipGroup;
        this.tagEmptyState = tagEmptyState;
        this.prefs = prefs;
        this.tags = new ArrayList<>();
    }

    /**
     * Loads tags from preferences
     */
    public void loadTags() {
        tags.clear();
        tags.addAll(prefs.getUserTags());
    }

    /**
     * Gets the current list of tags
     */
    public List<Tag> getTags() {
        return tags;
    }

    /**
     * Renders the list of created tags in the chip group
     */
    public void renderTags() {
        tagChipGroup.removeAllViews();

        // if no tags -> show empty state message
        if (tags.isEmpty()) {
            tagEmptyState.setText(fragment.getString(R.string.onboarding_tags_empty_state));
            tagEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tagEmptyState.setVisibility(View.GONE);

        // render each tag as a chip
        for (Tag tag : tags) {
            Chip chip = (Chip) LayoutInflater.from(fragment.requireContext())
                    .inflate(R.layout.view_tag_chip, tagChipGroup, false);
            chip.setText(tag.getTitle());
            chip.setTextColor(fragment.requireContext().getColor(R.color.black));
            chip.setChipBackgroundColor(ColorStateList.valueOf(tag.getColor()));
            chip.setChipIconTint(ColorStateList.valueOf(tag.getColor()));
            chip.setOnCloseIconClickListener(v -> {
                tags.remove(tag);
                renderTags();
                prefs.setUserTags(tags);
            });
            tagChipGroup.addView(chip);
        }
    }

    /**
     * Shows a dialog to add a new tag (with default behavior)
     */
    public void showAddTagDialog() {
        showAddTagDialog(null, null);
    }

    /**
     * Shows a dialog to add a new tag with custom callbacks
     *
     * @param onTagCreatedListener Callback invoked when a tag is created (optional)
     * @param onCancelListener Callback invoked when dialog is cancelled (optional)
     */
    public void showAddTagDialog(OnTagCreatedListener onTagCreatedListener, Runnable onCancelListener) {
        // load the dialog view from the layout
        View dialogView = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_add_tag, null);

        // extract fields from the view
        TextInputLayout tagNameLayout = dialogView.findViewById(R.id.layoutTagName);
        TextInputEditText editTagName = dialogView.findViewById(R.id.editTagName);
        ChipGroup colorGroup = dialogView.findViewById(R.id.chipTagColors);

        // load tag color selector for each available color
        int[] palette = Tags.getTagColorPalette(fragment.requireContext());
        for (int i = 0; i < palette.length; i++) {
            Chip chip = (Chip) LayoutInflater.from(fragment.requireContext())
                    .inflate(R.layout.view_color_chip, colorGroup, false);
            chip.setId(View.generateViewId());
            chip.setChipBackgroundColor(ColorStateList.valueOf(palette[i]));
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            colorGroup.addView(chip);
        }

        // create dialog with custom view
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.onboarding_tags_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (d, which) -> {
                    if (onCancelListener != null) {
                        onCancelListener.run();
                    }
                })
                .setPositiveButton(R.string.onboarding_tags_dialog_add, null);

        // show dialog and listen for positive button click
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    // validate tag name
                    String title = readText(editTagName);
                    ValidationResult titleResult = TagValidator.validateTitle(title, fragment.requireContext());
                    if (!titleResult.isValid()) {
                        tagNameLayout.setError(titleResult.msg());
                        return;
                    }
                    tagNameLayout.setError(null);

                    // get the selected color
                    int checkedChipId = colorGroup.getCheckedChipId();
                    if (checkedChipId == View.NO_ID) {
                        Snackbar.make(fragment.requireView(), R.string.onboarding_error_tag_color_required, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    Chip selected = colorGroup.findViewById(checkedChipId);
                    ColorStateList chipBgColor = Objects.requireNonNull(selected.getChipBackgroundColor());
                    int color = chipBgColor.getDefaultColor();

                    // create tag and add to list
                    Tag newTag = new Tag(title, color);
                    tags.add(newTag);
                    prefs.setUserTags(tags);

                    // call callback if provided, otherwise render tags
                    if (onTagCreatedListener != null) {
                        onTagCreatedListener.onTagCreated(newTag);
                    } else {
                        renderTags();
                    }

                    dialog.dismiss();
                }));

        // handle dialog cancellation
        dialog.setOnCancelListener(d -> {
            if (onCancelListener != null) {
                onCancelListener.run();
            }
        });

        dialog.show();
    }

    /**
     * Static utility method to show tag creation dialog without TagManager instance
     *
     * @param fragment The fragment context
     * @param prefs PreferencesManager instance
     * @param onTagCreatedListener Callback invoked when a tag is created
     * @param onCancelListener Callback invoked when dialog is cancelled (optional)
     */
    public static void showTagCreationDialog(Fragment fragment, PreferencesManager prefs,
                                             OnTagCreatedListener onTagCreatedListener,
                                             Runnable onCancelListener) {
        // load the dialog view from the layout
        View dialogView = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_add_tag, null);

        // extract fields from the view
        TextInputLayout tagNameLayout = dialogView.findViewById(R.id.layoutTagName);
        TextInputEditText editTagName = dialogView.findViewById(R.id.editTagName);
        ChipGroup colorGroup = dialogView.findViewById(R.id.chipTagColors);

        // load tag color selector for each available color
        int[] palette = Tags.getTagColorPalette(fragment.requireContext());
        for (int i = 0; i < palette.length; i++) {
            Chip chip = (Chip) LayoutInflater.from(fragment.requireContext())
                    .inflate(R.layout.view_color_chip, colorGroup, false);
            chip.setId(View.generateViewId());
            chip.setChipBackgroundColor(ColorStateList.valueOf(palette[i]));
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            colorGroup.addView(chip);
        }

        // create dialog with custom view
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.onboarding_tags_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (d, which) -> {
                    if (onCancelListener != null) {
                        onCancelListener.run();
                    }
                })
                .setPositiveButton(R.string.onboarding_tags_dialog_add, null);

        // show dialog and listen for positive button click
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    // validate tag name
                    String title = editTagName.getText() != null ? editTagName.getText().toString().trim() : "";
                    ValidationResult titleResult = TagValidator.validateTitle(title, fragment.requireContext());
                    if (!titleResult.isValid()) {
                        tagNameLayout.setError(titleResult.msg());
                        return;
                    }
                    tagNameLayout.setError(null);

                    // get the selected color
                    int checkedChipId = colorGroup.getCheckedChipId();
                    if (checkedChipId == View.NO_ID) {
                        Snackbar.make(fragment.requireView(), R.string.onboarding_error_tag_color_required, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    Chip selected = colorGroup.findViewById(checkedChipId);
                    ColorStateList chipBgColor = Objects.requireNonNull(selected.getChipBackgroundColor());
                    int color = chipBgColor.getDefaultColor();

                    // create tag and save to preferences
                    Tag newTag = new Tag(title, color);
                    List<Tag> currentTags = prefs.getUserTags();
                    currentTags.add(newTag);
                    prefs.setUserTags(currentTags);

                    // call callback
                    if (onTagCreatedListener != null) {
                        onTagCreatedListener.onTagCreated(newTag);
                    }

                    dialog.dismiss();
                }));

        // handle dialog cancellation
        dialog.setOnCancelListener(d -> {
            if (onCancelListener != null) {
                onCancelListener.run();
            }
        });

        dialog.show();
    }

    /**
     * Reads text from a TextInputEditText
     */
    private String readText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
