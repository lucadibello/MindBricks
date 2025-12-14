package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.ui.onboarding.OnboardingStepValidator;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.Tags;
import ch.inf.usi.mindbricks.util.ValidationResult;
import ch.inf.usi.mindbricks.util.validators.ProfileValidator;
import ch.inf.usi.mindbricks.util.validators.TagValidator;

public class OnboardingUserFragment extends Fragment implements OnboardingStepValidator {

    private static final String DICEBEAR_BASE_URL = "https://api.dicebear.com/9.x/pixel-art/png";
    private final List<Tag> tags = new ArrayList<>();
    private ImageView profilePicture;
    private TextInputLayout nameLayout;
    private TextInputEditText editName;
    private ChipGroup tagChipGroup;
    private MaterialTextView tagEmptyState;
    private PreferencesManager prefs;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_user, container, false);

        prefs = new PreferencesManager(requireContext());

        // register photo picker launcher
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        // persist permission to access the URI
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                        // save URI and load the selected image
                        prefs.setUserAvatarUri(uri.toString());
                        loadLocalProfilePicture(uri);
                    }
                }
        );

        // NOTE: get fields

        // profile picture image view
        profilePicture = view.findViewById(R.id.imageProfile);

        // name + container
        nameLayout = view.findViewById(R.id.layoutName);
        editName = view.findViewById(R.id.editName);

        // tag management
        tagChipGroup = view.findViewById(R.id.chipGroupTags);
        MaterialButton addTagButton = view.findViewById(R.id.buttonAddTag);
        tagEmptyState = view.findViewById(R.id.textTagsEmptyState);
        FloatingActionButton reloadAvatarButton = view.findViewById(R.id.buttonReloadAvatar);

        // set handler to pick photo
        MaterialButton choosePhoto = view.findViewById(R.id.buttonChoosePhoto);
        choosePhoto.setOnClickListener(v -> launchPhotoPicker());

        // show dialog on "add a tag"
        addTagButton.setOnClickListener(v -> showAddTagDialog());

        // on refresh -> remove custom photo + generate new avatar
        reloadAvatarButton.setOnClickListener(v -> {
            // clear custom avatar URI to revert to DiceBear
            prefs.setUserAvatarUri(null);
            String seed = generateUniqueSeed();
            prefs.setUserAvatarSeed(seed);
            loadRandomizedProfilePicture(seed);
        });

        // preload if already stored
        editName.setText(prefs.getUserName());

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

    /**
     * Loads the profile picture, preferring custom photo over DiceBear avatar
     */
    private void loadProfilePicture() {
        // check if user has a custom avatar URI
        String avatarUri = prefs.getUserAvatarUri();
        if (avatarUri != null && !avatarUri.isEmpty()) {
            loadLocalProfilePicture(Uri.parse(avatarUri));
        } else {
            // fall back to DiceBear avatar
            String seed = prefs.getUserAvatarSeed();
            if (seed == null || seed.isEmpty()) {
                seed = generateUniqueSeed();
                prefs.setUserAvatarSeed(seed);
            }
            loadRandomizedProfilePicture(seed);
        }
    }

    @Override
    public void onPause() {
        // on pause: store field values
        super.onPause();
        persistUserData(readText(editName));
    }

    @Override
    public boolean validateStep() {
        boolean isValid = validateNameField();

        // if all valid: store the result in app preferences
        if (isValid) {
            persistUserData(readText(editName));
        }

        return isValid;
    }

    /**
     * Stores the user data in preferences
     *
     * @param name User name to store
     */
    private void persistUserData(String name) {
        prefs.setUserName(name);
        prefs.setUserTags(tags);
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
    private void loadRandomizedProfilePicture(String seed) {
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

    /**
     * Loads a local profile picture from the given URI
     */
    private void loadLocalProfilePicture(Uri uri) {
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .centerCrop()
                .into(profilePicture);
    }

    /**
     * Launches the system gallery to select a profile picture
     */
    private void launchPhotoPicker() {
        photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    /**
     * Reads the text from a {@link TextInputEditText}
     *
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
    }

    /**
     * Validates the name field ensuring that user input is not empty
     *
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
     * Shows a dialog to add a new tag to the list
     * <p>
     * NOTE: this solution is inspired from this tutorial:
     * <a href="https://www.geeksforgeeks.org/android/how-to-create-a-custom-alertdialog-in-android/">
     * geeksforgeeks.org
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
                    prefs.setUserTags(tags);
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
            chip.setTextColor(requireContext().getColor(R.color.black));
            chip.setChipBackgroundColor(ColorStateList.valueOf(tag.color()));
            chip.setChipIconTint(ColorStateList.valueOf(tag.color()));
            chip.setOnCloseIconClickListener(v -> {
                tags.remove(tag);
                renderTags();
                prefs.setUserTags(tags);
            });
            tagChipGroup.addView(chip);
        }
    }

    /**
     * Loads the list of tags from the preferences
     */
    private void loadTagsFromPrefs() {
        tags.clear();
        tags.addAll(prefs.getUserTags());
    }
}