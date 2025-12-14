package ch.inf.usi.mindbricks.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ProfilePictureManager;
import ch.inf.usi.mindbricks.util.TagManager;
import ch.inf.usi.mindbricks.util.ValidationResult;
import ch.inf.usi.mindbricks.util.validators.ProfileValidator;

public class SettingsProfileFragment extends Fragment {

    private TextInputLayout nameLayout;
    private TextInputEditText editName;
    private PreferencesManager prefs;
    private ProfilePictureManager profilePictureManager;
    private TagManager tagManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_profile, container, false);

        prefs = new PreferencesManager(requireContext());

        // initialize helper managers
        ImageView profilePicture = view.findViewById(R.id.imageProfile);
        profilePictureManager = new ProfilePictureManager(this, profilePicture, prefs);

        ChipGroup tagChipGroup = view.findViewById(R.id.chipGroupTags);
        MaterialTextView tagEmptyState = view.findViewById(R.id.textTagsEmptyState);
        tagManager = new TagManager(this, tagChipGroup, tagEmptyState, prefs);

        nameLayout = view.findViewById(R.id.layoutName);
        editName = view.findViewById(R.id.editName);
        MaterialButton addTagButton = view.findViewById(R.id.buttonAddTag);
        FloatingActionButton reloadAvatarButton = view.findViewById(R.id.buttonReloadAvatar);

        // set handler to pick photo
        MaterialButton choosePhoto = view.findViewById(R.id.buttonChoosePhoto);
        choosePhoto.setOnClickListener(v -> profilePictureManager.showPhotoSourceDialog());

        addTagButton.setOnClickListener(v -> tagManager.showAddTagDialog());

        // on refresh -> remove custom photo + generate new avatar
        reloadAvatarButton.setOnClickListener(v -> profilePictureManager.regenerateDiceBearAvatar());

        editName.setText(prefs.getUserName());

        tagManager.loadTags();
        tagManager.renderTags();
        setupFieldValidation();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profilePictureManager.loadProfilePicture();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (validateNameField()) {
            persistUserData(readText(editName));
        }
    }

    private void persistUserData(String name) {
        prefs.setUserName(name);
        prefs.setUserTags(tagManager.getTags());
    }

    private String readText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void setupFieldValidation() {
        editName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateNameField();
        });
    }

    private boolean validateNameField() {
        ValidationResult result = ProfileValidator.validateName(
                readText(editName),
                requireContext()
        );
        if (!result.isValid()) {
            nameLayout.setError(result.msg());
            return false;
        }
        nameLayout.setError(null);
        return true;
    }
}
