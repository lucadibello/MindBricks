package ch.inf.usi.mindbricks.ui.nav.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;
import ch.inf.usi.mindbricks.ui.nav.home.city.CityViewModel;
import ch.inf.usi.mindbricks.ui.nav.home.city.IsometricCityView;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.DetailedQuestionsDialogFragment;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.EmotionSelectDialogFragment;
import ch.inf.usi.mindbricks.ui.settings.SettingsActivity;
import ch.inf.usi.mindbricks.util.evaluation.FocusScoreCalculator;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ProfileViewModel;
import ch.inf.usi.mindbricks.util.TagManager;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private TextView stateLabel;
    private Spinner tagSpinner;
    private Button startSessionButton;
    private TextView coinBalanceTextView;
    private ImageView settingsIcon;

    private HomeViewModel homeViewModel;
    private ProfileViewModel profileViewModel;

    private NavigationLocker navigationLocker;

    private List<ImageView> sessionDots;
    private ConstraintLayout sessionDotsLayout;

    private ActivityResultLauncher<String> audioPermissionLauncher;
    private ActivityResultLauncher<String> motionPermissionLauncher;

    private IsometricCityView cityView;
    private CityViewModel cityViewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // hosting activity implements the NavigationLocker interface
        if (context instanceof NavigationLocker) {
            navigationLocker = (NavigationLocker) context;
        } else {
            throw new RuntimeException(context + " must implement NavigationLocker");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register motion permission launcher
        // Starts session regardless of outcome (not granted = no sensor data won't be collected)
        motionPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> startDefaultSession()
        );

        // Register audio permission launcher
        // (After audio permission, request motion permission)
        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> motionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        // If idle, refresh timer display in case settings changed
        if (homeViewModel.currentState.getValue() == HomeViewModel.PomodoroState.IDLE &&
                homeViewModel.currentTime.getValue() == 0L) {
            PreferencesManager prefs = new PreferencesManager(requireContext());
            updateTimerUI(TimeUnit.MINUTES.toMillis(prefs.getTimerStudyDuration()));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        // Initialize the shared ViewModel for the user's profile data.
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timerTextView = view.findViewById(R.id.timer_text_view);
        stateLabel = view.findViewById(R.id.state_label);
        tagSpinner = view.findViewById(R.id.tag_spinner);
        startSessionButton = view.findViewById(R.id.start_stop_button);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);
        settingsIcon = view.findViewById(R.id.settings_icon);
        cityView = view.findViewById(R.id.cityView);

        // Initialize cityViewModel before any observer uses it
        cityViewModel = new ViewModelProvider(this).get(CityViewModel.class);

        setupTagSpinner();

        // Find the container for the dots
        sessionDotsLayout = view.findViewById(R.id.session_dots_layout);

        // Initialize the list of session dot ImageViews
        sessionDots = new ArrayList<>();
        sessionDots.add(view.findViewById(R.id.dot1));
        sessionDots.add(view.findViewById(R.id.dot2));
        sessionDots.add(view.findViewById(R.id.dot3));
        sessionDots.add(view.findViewById(R.id.dot4));

        // Click listener to open settings activity
        settingsIcon.setOnClickListener(v -> {
            // Only open settings if enabled (not during focus session)
            if (settingsIcon.isEnabled()) {
                Intent intent = new Intent(requireContext(), SettingsActivity.class);
                // force to select the Pomodoro tab at the start
                intent.putExtra(SettingsActivity.EXTRA_TAB_INDEX, 2);
                startActivity(intent);
            }
        });

        PreferencesManager prefs = new PreferencesManager(requireContext());
        int defaultStudyDurationMinutes = prefs.getTimerStudyDuration();
        updateTimerUI(TimeUnit.MINUTES.toMillis(defaultStudyDurationMinutes));

        setupObservers();

        homeViewModel.activityRecreated();

        startSessionButton.setOnClickListener(v -> {
            if (homeViewModel.currentState.getValue() != HomeViewModel.PomodoroState.IDLE) {
                confirmEndSessionDialog();
            } else {
                HomeViewModel.NextPhase nextPhase = homeViewModel.nextPhase.getValue();
                int sessionCounter = homeViewModel.getSessionCounter();

                // If we're continuing an existing cycle (after a break), just continue
                if (sessionCounter > 0) {
                    startNextPhase();
                }
                // If we're starting a break, continue to the next phase
                else if (nextPhase == HomeViewModel.NextPhase.SHORT_BREAK ||
                        nextPhase == HomeViewModel.NextPhase.LONG_BREAK) {
                    startNextPhase();
                }
                // Otherwise, start new cycle
                else {
                    PreferencesManager prefsListener = new PreferencesManager(requireContext());
                    boolean isFirstTime = prefsListener.isFirstSession();

                    // check if both permissions are already granted
                    boolean hasAudio = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED;
                    boolean hasMotion = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                            == PackageManager.PERMISSION_GRANTED;

                    if (hasAudio && hasMotion) {
                        startDefaultSession();
                    } else if (isFirstTime) {
                        prefsListener.setFirstSession(false);
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                    } else {
                        startDefaultSession();
                    }
                }
            }
        });

        // TODO: REMOVE THIS BEFORE SUBMISSION!!! ONLY FOR TESTING
        Button testButton = view.findViewById(R.id.test_questionnaire_button);
        if (testButton != null) {
            testButton.setOnClickListener(v -> showEmotionDialog(999L));
        }

        // Initialize slots
        cityViewModel.initializeSlots(5, 5);

        // Observe LiveData to update the view
        cityViewModel.getSlots().observe(getViewLifecycleOwner(), slots -> {
            cityView.setSlots(slots);
        });

        // Unlock a random slot every minute of study
        homeViewModel.studyElapsedTime.observe(getViewLifecycleOwner(), elapsedMillis -> {
            int minutes = (int) (elapsedMillis / 60000);
            if (minutes > 0 && elapsedMillis % 60000 < 1000) {
                cityViewModel.unlockRandomSlot();
            }
        });
    }

    private void setupObservers() {
        homeViewModel.currentState.observe(getViewLifecycleOwner(), state -> {
            boolean isRunning = state != HomeViewModel.PomodoroState.IDLE;
            boolean isStudying = state == HomeViewModel.PomodoroState.STUDY;

            // Update the button text and appearance based on state
            updateButtonForState(state, homeViewModel.nextPhase.getValue());

            // Lock the bottom navigation during study session
            navigationLocker.setNavigationEnabled(!isStudying);

            // Lock the settings icon during study session with visual feedback
            settingsIcon.setEnabled(!isStudying);
            settingsIcon.setAlpha(isStudying ? 0.5f : 1.0f);

            if (isRunning) {
                startSessionButton.setEnabled(false);
                new Handler(Looper.getMainLooper()).postDelayed(() -> startSessionButton.setEnabled(true), 1500);
            } else {
                startSessionButton.setEnabled(true);
            }
            updateSessionDots();
        });

        // Observe the next phase to update button and dots when it changes
        homeViewModel.nextPhase.observe(getViewLifecycleOwner(), nextPhase -> {
            if (homeViewModel.currentState.getValue() == HomeViewModel.PomodoroState.IDLE) {
                updateButtonForState(homeViewModel.currentState.getValue(), nextPhase);
                updateSessionDots();
            }
        });

        // Observe events for earning coins
        homeViewModel.earnedCoinsEvent.observe(getViewLifecycleOwner(), amount -> {
            if (amount != null && amount > 0) {
                earnCoin(amount);
                homeViewModel.onCoinsAwarded();
            }
        });

        homeViewModel.currentTime.observe(getViewLifecycleOwner(), millis -> {
            if (millis == 0L && homeViewModel.currentState.getValue() == HomeViewModel.PomodoroState.IDLE) {
                PreferencesManager prefs = new PreferencesManager(requireContext());
                updateTimerUI(TimeUnit.MINUTES.toMillis(prefs.getTimerStudyDuration()));
            } else {
                updateTimerUI(millis);
            }
        });

        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) coinBalanceTextView.setText(String.valueOf(balance));
        });

        homeViewModel.showQuestionnaireEvent.observe(getViewLifecycleOwner(), sessionId -> {
            if (sessionId != null && sessionId > 0) {
                showEmotionDialog(sessionId);
                homeViewModel.showQuestionnaireEvent.setValue(null);
            }
        });
    }

    private void startDefaultSession() {
        PreferencesManager prefs = new PreferencesManager(requireContext());
        int studyDuration = prefs.getTimerStudyDuration();
        int shortPauseDuration = prefs.getTimerShortPauseDuration();
        int longPauseDuration = prefs.getTimerLongPauseDuration();

        Tag selectedTag = (Tag) tagSpinner.getSelectedItem();
        homeViewModel.pomodoroTechnique(studyDuration, shortPauseDuration, longPauseDuration, selectedTag);
    }

    // Continues to the next phase (break or focus) based on current state
    private void startNextPhase() {
        PreferencesManager prefs = new PreferencesManager(requireContext());

        // Retrieve study settings
        int studyDuration = prefs.getTimerStudyDuration();
        int shortPauseDuration = prefs.getTimerShortPauseDuration();
        int longPauseDuration = prefs.getTimerLongPauseDuration();

        // Continue to the next phase
        homeViewModel.continueToNextPhase(studyDuration, shortPauseDuration, longPauseDuration);
    }

    // Updates the color and width of the session indicator dots based on the current state
    private void updateSessionDots() {
        if (homeViewModel == null || sessionDots == null || sessionDotsLayout == null) return;

        TransitionManager.beginDelayedTransition(sessionDotsLayout);

        int currentSession = homeViewModel.getSessionCounter();
        HomeViewModel.PomodoroState currentState = homeViewModel.currentState.getValue();

        for (ImageView dot : sessionDots) {
            dot.setImageResource(R.drawable.dot_inactive);
            ViewGroup.LayoutParams params = dot.getLayoutParams();
            params.width = (int) (12 * getResources().getDisplayMetrics().density);
            dot.setLayoutParams(params);
        }

        // Show completed sessions based on current state
        if (currentSession > 0 && currentSession <= sessionDots.size()) {
            if (currentState == HomeViewModel.PomodoroState.STUDY) {
                // During focus: show current session as active (pill shape)
                ImageView activeDot = sessionDots.get(currentSession - 1);
                activeDot.setImageResource(R.drawable.dot_active);

                // Change dot width to pill shape
                ViewGroup.LayoutParams params = activeDot.getLayoutParams();
                params.width = (int) (32 * getResources().getDisplayMetrics().density);
                activeDot.setLayoutParams(params);

                // Show all previous sessions as completed
                for (int i = 0; i < currentSession - 1; i++) {
                    sessionDots.get(i).setImageResource(R.drawable.dot_active);
                }
            } else {
                // During break, idle, or waiting for next session: show all completed sessions
                for (int i = 0; i < currentSession; i++) {
                    sessionDots.get(i).setImageResource(R.drawable.dot_active);
                }
            }
        }
    }


    // Updates the button text and style based on current state
    private void updateButtonForState(HomeViewModel.PomodoroState state, HomeViewModel.NextPhase nextPhase) {
        if (state == null) state = HomeViewModel.PomodoroState.IDLE;
        if (nextPhase == null) nextPhase = HomeViewModel.NextPhase.FOCUS;

        switch (state) {
            case IDLE:
                stateLabel.setVisibility(View.GONE);
                switch (nextPhase) {
                    case FOCUS:
                        startSessionButton.setText(R.string.start_session);
                        startSessionButton.setBackgroundTintList(
                                getResources().getColorStateList(R.color.analytics_accent_blue, null));
                        break;
                    case SHORT_BREAK:
                        startSessionButton.setText("Start Break");
                        startSessionButton.setBackgroundTintList(
                                getResources().getColorStateList(R.color.analytics_accent_green, null));
                        break;
                    case LONG_BREAK:
                        startSessionButton.setText("Start Long Break");
                        startSessionButton.setBackgroundTintList(
                                getResources().getColorStateList(R.color.analytics_accent_purple, null));
                        break;
                }
                break;

            case STUDY:
                startSessionButton.setText("End Focus");
                startSessionButton.setBackgroundTintList(
                        getResources().getColorStateList(R.color.md_theme_error, null));
                stateLabel.setText("Focusing...");
                stateLabel.setTextColor(getResources().getColor(R.color.md_theme_error, null));
                stateLabel.setVisibility(View.VISIBLE);
                break;

            case PAUSE:
                startSessionButton.setText("Skip Break");
                startSessionButton.setBackgroundTintList(
                        getResources().getColorStateList(R.color.analytics_accent_green, null));
                stateLabel.setText("Short Break");
                stateLabel.setTextColor(getResources().getColor(R.color.analytics_accent_green, null));
                stateLabel.setVisibility(View.VISIBLE);
                break;

            case LONG_PAUSE:
                startSessionButton.setText("Skip Long Break");
                startSessionButton.setBackgroundTintList(
                        getResources().getColorStateList(R.color.analytics_accent_purple, null));
                stateLabel.setText("Long Break");
                stateLabel.setTextColor(getResources().getColor(R.color.analytics_accent_purple, null));
                stateLabel.setVisibility(View.VISIBLE);
                break;
        }
    }

    // Shows a confirmation dialog before stopping an active session
    private void confirmEndSessionDialog() {
        HomeViewModel.PomodoroState currentState = homeViewModel.currentState.getValue();
        String title;
        String message;
        String skipButtonText;

        if (currentState == HomeViewModel.PomodoroState.STUDY) {
            title = "Focus Session Options";
            message = "You're currently in a focus session. What would you like to do?";
            skipButtonText = "Skip to Break";
        } else if (currentState == HomeViewModel.PomodoroState.LONG_PAUSE) {
            title = "Long Break Options";
            message = "You're on a long break. What would you like to do?";
            skipButtonText = "End Break";
        } else {
            title = "Break Options";
            message = "You're on a break. What would you like to do?";
            skipButtonText = "Skip to Focus";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(skipButtonText, (dialog, which) -> {
                    homeViewModel.skipCurrentStep();
                    Toast.makeText(getContext(), "Skipped to next step", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null);

        // Only show "End Entire Cycle" option if not in long pause
        // NOTE: after the long pause, we have already finished the cycle. Useless
        if (currentState != HomeViewModel.PomodoroState.LONG_PAUSE) {
            builder.setNeutralButton("End Entire Cycle", (dialog, which) -> {
                homeViewModel.stopTimerAndReset();
                Toast.makeText(getContext(), "Pomodoro cycle stopped.", Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
    }

    private void updateTimerUI(long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(minutes);
        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void earnCoin(int amount) {
        if (profileViewModel != null) profileViewModel.addCoins(amount);
        String message = (amount == 1) ? "+1 Coin!" : String.format(Locale.getDefault(), "+%d Coins!", amount);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showEmotionDialog(long sessionId) {
        EmotionSelectDialogFragment dialog = new EmotionSelectDialogFragment();
        dialog.setListener((emotionIndex, wantsDetailedQuestions) -> {
            if (wantsDetailedQuestions) showDetailedQuestionnaire(sessionId, emotionIndex);
            else saveQuickQuestionnaire(sessionId, emotionIndex);
        });
        dialog.show(getChildFragmentManager(), "emotion_dialog");
    }

    private void showDetailedQuestionnaire(long sessionId, int emotionIndex) {
        // FIXME: we need to use a new view rather than a dialog (a bit messy)
        DetailedQuestionsDialogFragment dialog = DetailedQuestionsDialogFragment.newInstance(emotionIndex);
        dialog.setListener(new DetailedQuestionsDialogFragment.OnQuestionnaireCompleteListener() {
            @Override
            public void onQuestionnaireComplete(int emotion, int enthusiasm, int energy,
                                                int engagement, int satisfaction, int anticipation) {
                saveDetailedQuestionnaire(sessionId, emotion, enthusiasm, energy,
                        engagement, satisfaction, anticipation);
            }

            @Override
            public void onQuestionnaireSkipped(int emotionIndex) {
                saveQuickQuestionnaire(sessionId, emotionIndex);
            }
        });
        dialog.show(getChildFragmentManager(), "detailed_questionnaire");
    }

    private void saveQuickQuestionnaire(long sessionId, int emotionIndex) {
        SessionQuestionnaire questionnaire = new SessionQuestionnaire();
        questionnaire.setSessionId(sessionId);
        questionnaire.setTimeStamp(System.currentTimeMillis());
        questionnaire.setInitialEmotion(emotionIndex);
        questionnaire.setAnsweredDetailedQuestions(false);
        homeViewModel.saveQuestionnaireResponse(questionnaire);
    }

    private void saveDetailedQuestionnaire(long sessionId, int emotionIndex,
                                           int enthusiasm, int energy, int engagement, int satisfaction, int anticipation) {
        SessionQuestionnaire questionnaire = new SessionQuestionnaire();
        questionnaire.setSessionId(sessionId);
        questionnaire.setTimeStamp(System.currentTimeMillis());
        questionnaire.setInitialEmotion(emotionIndex);
        questionnaire.setAnsweredDetailedQuestions(true);
        questionnaire.setEnthusiasmRating(enthusiasm);
        questionnaire.setEnergyRating(energy);
        questionnaire.setEngagementRating(engagement);
        questionnaire.setSatisfactionRating(satisfaction);
        questionnaire.setAnticipationRating(anticipation);

        // Calculate focus score based on questionnaire responses
        float focusScore = FocusScoreCalculator.calculate(enthusiasm, energy, engagement, satisfaction, anticipation);

        // Save questionnaire and update session focus score
        homeViewModel.saveQuestionnaireResponse(questionnaire, sessionId, focusScore);
    }

    private void setupTagSpinner() {
        PreferencesManager prefs = new PreferencesManager(requireContext());

        // Load tags on background thread
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

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
            Tag finalDefaultTag = defaultTag;
            requireActivity().runOnUiThread(() -> {
                tags.add(0, finalDefaultTag);

                // Add special "Create New Tag" option (last)
                Tag createNewTag = new Tag("+ Create New Tag", getResources().getColor(R.color.analytics_accent_green, null));
                tags.add(createNewTag);

                // setup spinner items - one component for each tag
                TagSpinnerAdapter adapter = new TagSpinnerAdapter(requireContext(), tags);
                tagSpinner.setAdapter(adapter);
                tagSpinner.setSelection(0, false); // select "No tag" by default

                // Handle tag selection
                tagSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        Tag selectedTag = (Tag) parent.getItemAtPosition(position);
                        // Check if user selected "Create New Tag"
                        if (selectedTag.getTitle().equals("+ Create New Tag")) {
                            showAddTagDialog();
                        }
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                        // Do nothing
                    }
                });
            });
        }).start();
    }

    private void showAddTagDialog() {
        PreferencesManager prefs = new PreferencesManager(requireContext());

        // build + trigger dialog using TagManager
        TagManager.showTagCreationDialog(
                this,
                prefs,
                newTag -> {
                    // recreate tag spinner to include new tag
                    setupTagSpinner();

                    // Find and select the new tag
                    for (int i = 0; i < tagSpinner.getCount(); i++) {
                        Tag tag = (Tag) tagSpinner.getItemAtPosition(i);
                        if (tag.getTitle().equals(newTag.getTitle()) && tag.getColor() == newTag.getColor()) {
                            tagSpinner.setSelection(i);
                            break;
                        }
                    }
                },
                () -> tagSpinner.setSelection(1) // reset to "no tag" on cancel
        );
    }
}
