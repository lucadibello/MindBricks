package ch.inf.usi.mindbricks.ui.settings;

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
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.Tags;
import ch.inf.usi.mindbricks.util.ValidationResult;
import ch.inf.usi.mindbricks.util.validators.ProfileValidator;
import ch.inf.usi.mindbricks.util.validators.TagValidator;

public class SettingsProfileFragment extends Fragment {

    private static final String DICEBEAR_BASE_URL = "https://api.dicebear.com/9.x/pixel-art/png";
    private final List<Tag> tags = new ArrayList<>();
    private ImageView profilePicture;
    private TextInputLayout nameLayout;
    private TextInputLayout sprintLengthLayout;
    private TextInputEditText editName;
    private TextInputEditText editSprintLength;
    private ChipGroup tagChipGroup;
    private MaterialButton addTagButton;
    private MaterialTextView tagEmptyState;
    private FloatingActionButton reloadAvatarButton;
    private PreferencesManager prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_profile, container, false);

        prefs = new PreferencesManager(requireContext());

        profilePicture = view.findViewById(R.id.imageProfile);
        nameLayout = view.findViewById(R.id.layoutName);
        editName = view.findViewById(R.id.editName);
        sprintLengthLayout = view.findViewById(R.id.layoutSprintLength);
        editSprintLength = view.findViewById(R.id.editSprintLength);
        tagChipGroup = view.findViewById(R.id.chipGroupTags);
        addTagButton = view.findViewById(R.id.buttonAddTag);
        tagEmptyState = view.findViewById(R.id.textTagsEmptyState);
        reloadAvatarButton = view.findViewById(R.id.buttonReloadAvatar);

        addTagButton.setOnClickListener(v -> showAddTagDialog());

        reloadAvatarButton.setOnClickListener(v -> {
            String seed = generateUniqueSeed();
            prefs.setUserAvatarSeed(seed);
            loadRandomizedProfilePicture(seed);
        });

        editName.setText(prefs.getUserName());
        editSprintLength.setText(prefs.getUserSprintLengthMinutes());

        loadTagsFromPrefs();
        renderTags();
        setupFieldValidation();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String seed = prefs.getUserAvatarSeed();
        if (seed == null || seed.isEmpty()) {
            seed = generateUniqueSeed();
            prefs.setUserAvatarSeed(seed);
        }
        loadRandomizedProfilePicture(seed);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (validateNameField() && validateSprintLengthField()) {
            persistUserData(readText(editName), readText(editSprintLength));
        }
    }

    private void persistUserData(String name, String sprintLength) {
        prefs.setUserName(name);
        prefs.setUserSprintLengthMinutes(sprintLength);
        prefs.setUserTags(tags);
    }

    private String generateUniqueSeed() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append((char) ('a' + (int) (Math.random() * 26)));
        }
        return "mindbricks-" + sb;
    }

    private void loadRandomizedProfilePicture(String seed) {
        Uri avatarUri = Uri.parse(DICEBEAR_BASE_URL)
                .buildUpon()
                .appendQueryParameter("seed", seed)
                .build();

        Glide.with(this)
                .load(avatarUri)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .centerCrop()
                .into(profilePicture);
    }

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

    private boolean validateNameField() {
        ValidationResult result = ProfileValidator.validateName(readText(editName));
        if (!result.isValid()) {
            nameLayout.setError(getString(result.errorResId()));
            return false;
        }
        nameLayout.setError(null);
        return true;
    }

    private boolean validateSprintLengthField() {
        ValidationResult result = ProfileValidator.validateSprintLength(readText(editSprintLength));
        if (!result.isValid()) {
            sprintLengthLayout.setError(getString(result.errorResId()));
            return false;
        }
        sprintLengthLayout.setError(null);
        return true;
    }

    private void showAddTagDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_tag, null);
        TextInputLayout tagNameLayout = dialogView.findViewById(R.id.layoutTagName);
        TextInputEditText editTagName = dialogView.findViewById(R.id.editTagName);
        ChipGroup colorGroup = dialogView.findViewById(R.id.chipTagColors);

        int[] palette = Tags.getTagColorPalette(requireContext());
        for (int i = 0; i < palette.length; i++) {
            Chip chip = (Chip) LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_color_chip, colorGroup, false);
            chip.setId(View.generateViewId());
            chip.setChipBackgroundColor(ColorStateList.valueOf(palette[i]));
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            colorGroup.addView(chip);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_tags_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.onboarding_tags_dialog_add, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = readText(editTagName);
                    ValidationResult titleResult = TagValidator.validateTitle(title);
                    if (!titleResult.isValid()) {
                        tagNameLayout.setError(getString(titleResult.errorResId()));
                        return;
                    }
                    tagNameLayout.setError(null);

                    int checkedChipId = colorGroup.getCheckedChipId();
                    if (checkedChipId == View.NO_ID) {
                        Snackbar.make(requireView(), R.string.onboarding_error_tag_color_required, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    Chip selected = colorGroup.findViewById(checkedChipId);
                    ColorStateList chipBgColor = Objects.requireNonNull(selected.getChipBackgroundColor());
                    int color = chipBgColor.getDefaultColor();

                    tags.add(new Tag(title, color));
                    renderTags();
                    prefs.setUserTags(tags);
                    dialog.dismiss();
                }));

        dialog.show();
    }

    private void renderTags() {
        tagChipGroup.removeAllViews();
        if (tags.isEmpty()) {
            tagEmptyState.setText(getString(R.string.onboarding_tags_empty_state));
            tagEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tagEmptyState.setVisibility(View.GONE);

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

    private void loadTagsFromPrefs() {
        tags.clear();
        tags.addAll(prefs.getUserTags());
    }
}