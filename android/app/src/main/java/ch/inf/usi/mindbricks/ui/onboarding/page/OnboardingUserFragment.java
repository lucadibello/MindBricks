package ch.inf.usi.mindbricks.ui.onboarding.page;

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
import ch.inf.usi.mindbricks.ui.onboarding.OnboardingStepValidator;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ProfilePictureManager;
import ch.inf.usi.mindbricks.util.TagManager;
import ch.inf.usi.mindbricks.util.ValidationResult;
import ch.inf.usi.mindbricks.util.validators.ProfileValidator;

public class OnboardingUserFragment extends Fragment implements OnboardingStepValidator {

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
        View view = inflater.inflate(R.layout.fragment_onboarding_user, container, false);

        prefs = new PreferencesManager(requireContext());

        // initialize helper managers
        ImageView profilePicture = view.findViewById(R.id.imageProfile);
        profilePictureManager = new ProfilePictureManager(this, profilePicture, prefs);

        ChipGroup tagChipGroup = view.findViewById(R.id.chipGroupTags);
        MaterialTextView tagEmptyState = view.findViewById(R.id.textTagsEmptyState);
        tagManager = new TagManager(this, tagChipGroup, tagEmptyState, prefs);

        // name + container
        nameLayout = view.findViewById(R.id.layoutName);
        editName = view.findViewById(R.id.editName);

        // tag management
        MaterialButton addTagButton = view.findViewById(R.id.buttonAddTag);
        FloatingActionButton reloadAvatarButton = view.findViewById(R.id.buttonReloadAvatar);

        // set handler to pick photo
        MaterialButton choosePhoto = view.findViewById(R.id.buttonChoosePhoto);
        choosePhoto.setOnClickListener(v -> profilePictureManager.showPhotoSourceDialog());

        // show dialog on "add a tag"
        addTagButton.setOnClickListener(v -> tagManager.showAddTagDialog());

        // on refresh -> remove custom photo + generate new avatar
        reloadAvatarButton.setOnClickListener(v -> profilePictureManager.regenerateDiceBearAvatar());

        // preload if already stored
        editName.setText(prefs.getUserName());

        // load + render the list of tags stored inside preferences
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
        prefs.setUserTags(tagManager.getTags());
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
