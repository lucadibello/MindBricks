package ch.inf.usi.mindbricks.ui.nav.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.widget.Spinner;
import ch.inf.usi.mindbricks.model.Tag;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;
import ch.inf.usi.mindbricks.ui.nav.home.city.CityViewModel;
import ch.inf.usi.mindbricks.ui.nav.home.city.IsometricCityView;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.DetailedQuestionsDialogFragment;
import ch.inf.usi.mindbricks.ui.nav.home.questionnare.EmotionSelectDialogFragment;
import ch.inf.usi.mindbricks.ui.settings.SettingsActivity;
import ch.inf.usi.mindbricks.util.PermissionManager;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Spinner tagSpinner;
    private Button startSessionButton;
    private TextView coinBalanceTextView;

    private HomeViewModel homeViewModel;
    private ProfileViewModel profileViewModel;

    private NavigationLocker navigationLocker;

    private List<ImageView> sessionDots;
    private ConstraintLayout sessionDotsLayout;

    private PermissionManager.PermissionRequest audioPermissionRequest;
    private PermissionManager.PermissionRequest motionPermissionRequest;

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
        // Its job is to finally start the session, regardless of the outcome.
        motionPermissionRequest = PermissionManager.registerSinglePermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION,
                this::startDefaultSession,
                this::startDefaultSession
                // in both cases start as default, so if permission is denied it will just don't work
        );

        // Its job is to trigger the next request in the chain.
        audioPermissionRequest = PermissionManager.registerSinglePermission(
                this,
                Manifest.permission.RECORD_AUDIO,
                () -> motionPermissionRequest.launch(),
                () -> motionPermissionRequest.launch()
                // in both cases start as default, so if permission is denied it will just don't work
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
        tagSpinner = view.findViewById(R.id.tag_spinner);
        startSessionButton = view.findViewById(R.id.start_stop_button);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);
        ImageView settingsIcon = view.findViewById(R.id.settings_icon);
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
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            // force to select the Pomodoro tab at the start
            intent.putExtra(SettingsActivity.EXTRA_TAB_INDEX, 2);
            startActivity(intent);
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
                PreferencesManager prefsListener = new PreferencesManager(requireContext());
                boolean isFirstTime = prefsListener.isFirstSession();

                boolean hasAudio = PermissionManager.hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO);
                boolean hasMotion = PermissionManager.hasPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION);

                if (hasAudio && hasMotion) {
                    startDefaultSession();
                } else if (isFirstTime) {
                    prefsListener.setFirstSession(false);
                    audioPermissionRequest.launch();
                    motionPermissionRequest.launch();
                } else {
                    startDefaultSession();
                }
            }
        });

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
            if (minutes > 0 && elapsedMillis % 60000 < 1000) { // trigger once per minute
                cityViewModel.unlockRandomSlot();
            }
        });
    }

    private void setupObservers() {
        homeViewModel.currentState.observe(getViewLifecycleOwner(), state -> {
            boolean isRunning = state != HomeViewModel.PomodoroState.IDLE;
            startSessionButton.setText(isRunning ? R.string.stop_session : R.string.start_session);
            navigationLocker.setNavigationEnabled(state != HomeViewModel.PomodoroState.STUDY);

            if (isRunning) {
                startSessionButton.setEnabled(false);
                new Handler(Looper.getMainLooper()).postDelayed(() -> startSessionButton.setEnabled(true), 1500);
            } else {
                startSessionButton.setEnabled(true);
            }
            updateSessionDots();
        });

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

        if (currentState == HomeViewModel.PomodoroState.STUDY && currentSession > 0 && currentSession <= sessionDots.size()) {
            ImageView activeDot = sessionDots.get(currentSession - 1);
            activeDot.setImageResource(R.drawable.dot_active);
            ViewGroup.LayoutParams params = activeDot.getLayoutParams();
            params.width = (int) (32 * getResources().getDisplayMetrics().density);
            activeDot.setLayoutParams(params);
        }
    }

    private void confirmEndSessionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("End Cycle?")
                .setMessage("Are you sure you want to stop the current Pomodoro cycle?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    homeViewModel.stopTimerAndReset();
                    Toast.makeText(getContext(), "Pomodoro cycle stopped.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        DetailedQuestionsDialogFragment dialog = DetailedQuestionsDialogFragment.newInstance(emotionIndex);
        dialog.setListener((emotion, enthusiasm, energy, engagement, satisfaction, anticipation) -> {
            saveDetailedQuestionnaire(sessionId, emotion, enthusiasm, energy, engagement, satisfaction, anticipation);
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
        homeViewModel.saveQuestionnaireResponse(questionnaire);
    }

    private void setupTagSpinner() {
        PreferencesManager prefs = new PreferencesManager(requireContext());
        List<Tag> tags = prefs.getUserTags();
        tags.add(new Tag("No tag", android.graphics.Color.GRAY));
        tagSpinner.setAdapter(new TagSpinnerAdapter(requireContext(), tags));
    }
}
