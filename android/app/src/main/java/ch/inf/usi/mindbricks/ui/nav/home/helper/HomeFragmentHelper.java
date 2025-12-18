package ch.inf.usi.mindbricks.ui.nav.home.helper;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.ui.nav.home.HomeViewModel;
import ch.inf.usi.mindbricks.ui.nav.home.adapters.TagSpinnerAdapter;
import ch.inf.usi.mindbricks.util.ProfileViewModel;
import ch.inf.usi.mindbricks.util.questionnaire.ProductivityQuestionnaireResult;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.ProductivityQuestionsDialogFragment;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.EmotionSelectDialogFragment;
import ch.inf.usi.mindbricks.util.AppExecutor;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ProfilePictureManager;
import ch.inf.usi.mindbricks.util.TagManager;

/**
 * Helper class for HomeFragment.
 *
 * @author Luca Di Bello
 */
public class HomeFragmentHelper extends Fragment {

    /**
     * Preferences manager for the app.
     */
    protected PreferencesManager prefs;

    /**
     * Database instance.
     */
    protected AppDatabase db;

    /**
     * Profile picture manager for the app.
     */
    protected ProfilePictureManager profilePictureManager;

    /**
     * View model for the home fragment.
     */
    protected HomeViewModel homeViewModel;

    /**
     * View model for the profile fragment.
     */
    protected ProfileViewModel profileViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create view models
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // load preferences + db
        Context ctx = requireContext();
        prefs = new PreferencesManager(ctx);
        db = AppDatabase.getInstance(ctx);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // create profile picture manager
        ImageView settingsIcon = view.findViewById(R.id.settings_icon);
        profilePictureManager = new ProfilePictureManager(this, settingsIcon, prefs);
    }

    /**
     * Sets up the tag spinner.
     *
     * @param tagSpinner spinner to set up
     */
    protected void setupTagSpinner(Spinner tagSpinner) {
        // Load tags on background thread
        AppExecutor.getInstance().execute(
                () -> {
                    // Ensure default "No tag" exists
                    Tag defaultTag = db.tagDao().getTagByTitle("No tag");
                    if (defaultTag == null) {
                        defaultTag = new Tag("No tag", android.graphics.Color.GRAY);
                        long defaultTagId = db.tagDao().insert(defaultTag);
                        defaultTag.setId(defaultTagId);
                    }

                    // Load user tags from preferences (will eventually migrate to database)
                    List<Tag> tags = new ArrayList<>(prefs.getUserTags());

                    // Add default tag at the beginning
                    tags.add(0, defaultTag);

                    // Add special "Create New Tag" option (last)
                    Tag createNewTag = new Tag("+ Create New Tag", getResources().getColor(R.color.analytics_accent_green, null));
                    tags.add(createNewTag);

                    // setup spinner items - one component for each tag
                    TagSpinnerAdapter adapter = new TagSpinnerAdapter(requireContext(), tags);
                    tagSpinner.setAdapter(adapter);
                    tagSpinner.setSelection(0, false); // select "No tag" by default

                    // Handle tag selection
                    tagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            // check if the selected item is the "Create New Tag" option
                            Tag selectedTag = (Tag) parent.getItemAtPosition(position);
                            if (selectedTag.getTitle().equals("+ Create New Tag")) {
                                showAddTagDialog(tagSpinner);
                            }
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {
                            // Do nothing
                        }
                    });
                });

    }

    /**
     * Shows the dialog for creating a new tag.
     *
     * @param tagSpinner spinner to update
     */
    protected void showAddTagDialog(Spinner tagSpinner) {
        // build + trigger dialog using TagManager
        TagManager.showTagCreationDialog(
                this,
                prefs,

                // on create callback
                newTag -> {
                    // reload tag spinner
                    setupTagSpinner(tagSpinner);

                    // find + set new tag
                    for (int i = 0; i < tagSpinner.getCount(); i++) {
                        Tag tag = (Tag) tagSpinner.getItemAtPosition(i);
                        if (tag.getTitle().equals(newTag.getTitle()) && tag.getColor() == newTag.getColor()) {
                            tagSpinner.setSelection(i);
                            break;
                        }
                    }
                },
                // on cancel callback
                () -> tagSpinner.setSelection(0) // reset to "no tag" on cancel
        );
    }

    /**
     * Shows the emotion and productivity questionnaires.
     *
     * @param sessionId id of the session
     */
    protected void showQuestionnaires(long sessionId) {
        EmotionSelectDialogFragment emotionQuestionnaireDialog = new EmotionSelectDialogFragment();
        // if user selected an emotion -> trigger also second questionnaire
        emotionQuestionnaireDialog.setOnEmotionSelectedListener((emotionIndex) -> {
            ProductivityQuestionsDialogFragment detailedQuestionnaireDialog = ProductivityQuestionsDialogFragment.create();
            detailedQuestionnaireDialog.setListener(
                    new ProductivityQuestionsDialogFragment.OnQuestionnaireActionListener() {
                        @Override
                        public void onQuestionnaireComplete(ProductivityQuestionnaireResult result) {
                            saveQuestionnaires(sessionId, emotionIndex, result);
                        }

                        @Override
                        public void onQuestionnaireSkipped() {
                            saveEmotionQuestionnaire(sessionId, emotionIndex);
                        }
                    }
            );
        });
        emotionQuestionnaireDialog.show(getChildFragmentManager(), "emotion_dialog");
    }

    /**
     * Updates the timer text view.
     *
     * @param timerTextView text view to update
     * @param millisUntilFinished time left in milliseconds
     */
    protected void updateTimerText(@NonNull TextView timerTextView, long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(minutes);
        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    /**
     * Saves the emotion questionnaire.
     *
     * @param sessionId ID of the session to save the questionnaire for
     * @param emotionRating PAM rating of the emotion (0-6)
     */
    private void saveEmotionQuestionnaire(long sessionId, int emotionRating) {
        saveQuestionnaires(sessionId, emotionRating, null);
    }

    /**
     * Saves the both the emotion and perceived productivity questionnaires.
     *
     * @param sessionId ID of the session to save the questionnaire for
     * @param emotionRating PAM rating of the emotion (0-6)
     * @param productivityResult productivityResult of the perceived productivity questionnaire
     */
    private void saveQuestionnaires(long sessionId, int emotionRating,
                                   @Nullable ProductivityQuestionnaireResult productivityResult) {
        // build model for SessionQuestionnaire from the collected results
        SessionQuestionnaire questionnaire = SessionQuestionnaire.from(
                sessionId, emotionRating, productivityResult
        );

        // register the questionnaire
        homeViewModel.saveQuestionnaireResponse(questionnaire);
    }
}
