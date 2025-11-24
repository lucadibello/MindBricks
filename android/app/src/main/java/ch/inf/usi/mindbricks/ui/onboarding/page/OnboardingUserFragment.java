package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.ui.onboarding.OnboardingStepValidator;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.Tags;
import ch.inf.usi.mindbricks.util.ValidationResult;
import ch.inf.usi.mindbricks.util.validators.ProfileValidator;
import ch.inf.usi.mindbricks.util.validators.TagValidator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OnboardingUserFragment extends Fragment implements OnboardingStepValidator {

    private ImageView profilePicture;
    private TextInputLayout nameLayout;
    private TextInputLayout sprintLengthLayout;

    private TextInputEditText editName;
    private TextInputEditText editSprintLength;
    private ChipGroup tagChipGroup;
    private MaterialButton addTagButton;
    private MaterialTextView tagEmptyState;

    private final List<Tag> tags = new ArrayList<>();
    private PreferencesManager prefs;
    private static final String DICEBEAR_BASE_URL = "https://api.dicebear.com/9.x/pixel-art/png";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_user, container, false);

        prefs = new PreferencesManager(requireContext());

        // NOTE: get fields

        // profile picture image view
        profilePicture = view.findViewById(R.id.imageProfile);

        // name + container
        nameLayout = view.findViewById(R.id.layoutName);
        editName = view.findViewById(R.id.editName);

        // sprint length + container
        sprintLengthLayout = view.findViewById(R.id.layoutSprintLength);
        editSprintLength = view.findViewById(R.id.editSprintLength);

        // tag management
        tagChipGroup = view.findViewById(R.id.chipGroupTags);
        addTagButton = view.findViewById(R.id.buttonAddTag);
        tagEmptyState = view.findViewById(R.id.textTagsEmptyState);

        // set handler to pick photo
        MaterialButton choosePhoto = view.findViewById(R.id.buttonChoosePhoto);
        choosePhoto.setOnClickListener(v -> launchPhotoPicker());

        // show dialog on "add a tag"
        addTagButton.setOnClickListener(v -> showAddTagDialog());

        // preload if already stored
        editName.setText(prefs.getUserName());
        editSprintLength.setText(prefs.getUserSprintLengthMinutes());

        // load + render the list of tags stored inside preferences
        loadTagsFromPrefs();
        renderTags();
        setupFieldValidation();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadProfilePicture();
    }

    @Override
    public void onPause() {
        // on pause: store field values
        super.onPause();
        persistUserData(readText(editName), readText(editSprintLength));
    }

    @Override
    public boolean validateStep() {
        boolean isValid = true;

        if (!validateNameField()) isValid = false;
        if (!validateSprintLengthField()) isValid = false;

        // if all valid: store the result in app preferences
        if (isValid) {
            persistUserData(readText(editName), readText(editSprintLength));
        }

        return isValid;
    }

    /**
     * Stores the user data in preferences
     * @param name User name to store
     * @param sprintLength Sprint length to store
     */
    private void persistUserData(String name, String sprintLength) {
        prefs.setUserName(name);
        prefs.setUserSprintLengthMinutes(sprintLength);
        prefs.setUserTagsJson(serializeTags());
    }

    private String generateUniqueSeed() {
        // randomize a part of the seed if no name is provided
        // NOTE: this is needed otherwise all new users would have the same
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            // generate random char in range [a-z]
            sb.append((char) ('a' + (int) (Math.random() * 26)));
        }
        return "mindbricks-" + sb;
    }

    /**
     * Loads the default user avatar from DiceBear.
     */
    private void loadProfilePicture() {
        String seed = prefs.getUserName();
        if (seed == null || seed.isEmpty()) {
            seed = generateUniqueSeed();
        }

        // build URL with unique seed
        Uri avatarUri = Uri.parse(DICEBEAR_BASE_URL)
                .buildUpon()
                .appendQueryParameter("seed", seed)
                .build();

        // load image using Glide
        Glide.with(this)
                .load(avatarUri)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .centerCrop()
                .into(profilePicture);
    }

    private void launchPhotoPicker() {
        // FIXME: we need to implement this!
    }

    /**
     * Reads the text from a {@link TextInputEditText}
     * @param editText {@link TextInputEditText} to read from
     * @return Text read from the text input
     */
    private String readText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void setupFieldValidation() {
        editName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateNameField();
        });
        editSprintLength.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateSprintLengthField();
        });
    }

    /**
     * Validates the name field ensuring that user input is not empty
     * @return true if the name is valid, false otherwise
     */
    private boolean validateNameField() {
        ValidationResult result = ProfileValidator.validateName(readText(editName));
        if (!result.isValid()) {
            nameLayout.setError(getString(result.errorResId()));
            return false;
        }
        nameLayout.setError(null);
        return true;
    }

    /**
     * Validates the sprint length field ensuring that user input is not empty and is a valid number
     * @return true if the sprint length is valid, false otherwise
     */
    private boolean validateSprintLengthField() {
        ValidationResult result = ProfileValidator.validateSprintLength(readText(editSprintLength));
        if (!result.isValid()) {
            sprintLengthLayout.setError(getString(result.errorResId()));
            return false;
        }
        sprintLengthLayout.setError(null);
        return true;
    }

    /**
     * Shows a dialog to add a new tag to the list
     *
     * NOTE: this solution is inspired from this tutorial:
     * <a href="https://www.geeksforgeeks.org/android/how-to-create-a-custom-alertdialog-in-android/">
     *     geeksforgeeks.org
     * </a>
     */
    private void showAddTagDialog() {
        // loads the dialog view from the layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_tag, null);

        // extract fields from the view
        TextInputLayout tagNameLayout = dialogView.findViewById(R.id.layoutTagName);
        TextInputEditText editTagName = dialogView.findViewById(R.id.editTagName);
        ChipGroup colorGroup = dialogView.findViewById(R.id.chipTagColors);

        // loads a tag color selector for each available colors
        int[] palette = Tags.getTagColorPalette(requireContext());
        for (int i = 0; i < palette.length; i++) {
            // load chip component view + update settings
            Chip chip = (Chip) LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_color_chip, colorGroup, false);
            chip.setId(View.generateViewId());
            chip.setChipBackgroundColor(ColorStateList.valueOf(palette[i]));
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            colorGroup.addView(chip);
        }

        // create dialog with custom view
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_tags_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.onboarding_tags_dialog_add, null);

        // show dialog + listen for positive button click
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    // validate tag name using validator utility
                    String title = readText(editTagName);
                    ValidationResult titleResult = TagValidator.validateTitle(title);
                    if (!titleResult.isValid()) {
                        tagNameLayout.setError(getString(titleResult.errorResId()));
                        return;
                    }
                    tagNameLayout.setError(null);

                    // get the selected color from the list
                    int checkedChipId = colorGroup.getCheckedChipId();
                    if (checkedChipId == View.NO_ID) {
                        // NOTE: this shouldn't be possible as the UI already enforces that a color is selected
                        // but still check for it
                        Snackbar.make(requireView(), R.string.onboarding_error_tag_color_required, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    Chip selected = colorGroup.findViewById(checkedChipId);

                    // get the color to use from the selected chip
                    ColorStateList chipBgColor = Objects.requireNonNull(selected.getChipBackgroundColor());
                    int color = chipBgColor.getDefaultColor();

                    // create tag and add it to the list + re-render
                    tags.add(new Tag(title, color));
                    renderTags();

                    // store the new tags in preferences
                    prefs.setUserTagsJson(serializeTags());
                    dialog.dismiss();
                }));

        dialog.show();
    }

    /**
     * Renders the list of created tags in the chip group
     */
    private void renderTags() {
        tagChipGroup.removeAllViews();
        // if no tags -> show a message instead
        if (tags.isEmpty()) {
            tagEmptyState.setText(getString(R.string.onboarding_tags_empty_state));
            tagEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tagEmptyState.setVisibility(View.GONE);

        // render each tag view as individual chips + add chips to the group
        for (Tag tag : tags) {
            Chip chip = (Chip) LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_tag_chip, tagChipGroup, false);
            chip.setText(tag.title());
            chip.setChipBackgroundColor(ColorStateList.valueOf(tag.color()));
            chip.setChipIconTint(ColorStateList.valueOf(tag.color()));
            chip.setOnCloseIconClickListener(v -> {
                tags.remove(tag);
                renderTags();
                prefs.setUserTagsJson(serializeTags());
            });
            tagChipGroup.addView(chip);
        }
    }

    /**
     * Loads the list of tags from the preferences as JSON entries
     */
    private void loadTagsFromPrefs() {
        tags.clear();
        try {
            JSONArray array = new JSONArray(prefs.getUserTagsJson());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Tag t = Tag.fromJson(obj);
                if (t != null) tags.add(t);
            }
        } catch (JSONException e) {
            // ignore and start empty
        }
    }

    /**
     * Serializes the list of tags into a JSON array
     * @return JSON array of tags as raw String
     */
    private String serializeTags() {
        JSONArray array = new JSONArray();
        for (Tag tag : tags) {
            array.put(tag.toJson());
        }
        return array.toString();
    }
}
