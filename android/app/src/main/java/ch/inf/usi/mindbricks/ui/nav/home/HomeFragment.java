package ch.inf.usi.mindbricks.ui.nav.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;
import ch.inf.usi.mindbricks.ui.nav.home.helper.HomeFragmentHelper;
import ch.inf.usi.mindbricks.ui.settings.SettingsActivity;
import ch.inf.usi.mindbricks.util.PreferencesManager;

public class HomeFragment extends HomeFragmentHelper {

    private TextView timerTextView;
    private TextView stateLabel;
    private Spinner tagSpinner;
    private Button startSessionButton;
    private TextView coinBalanceTextView;
    private ImageView settingsIcon;
    private NavigationLocker navigationLocker;
    private List<ImageView> sessionDots;
    private ConstraintLayout sessionDotsLayout;

    private ActivityResultLauncher<String> audioPermissionLauncher;
    private ActivityResultLauncher<String> motionPermissionLauncher;

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
        motionPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> startDefaultSession()
        );

        // Register audio permission launcher
        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> motionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        );
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
        sessionDotsLayout = view.findViewById(R.id.session_dots_layout);

        // setup tag selector
        setupTagSpinner(tagSpinner);

        // Initialize the list of session dot ImageViews
        sessionDots = new ArrayList<>();
        sessionDots.add(view.findViewById(R.id.dot1));
        sessionDots.add(view.findViewById(R.id.dot2));
        sessionDots.add(view.findViewById(R.id.dot3));
        sessionDots.add(view.findViewById(R.id.dot4));

        // Click listener to open profile/settings
        settingsIcon.setOnClickListener(v -> {
            if (settingsIcon.isEnabled()) {
                Intent intent = new Intent(requireContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        // Load profile avatar
        profilePictureManager.loadProfilePicture();

        int defaultStudyDurationMinutes = prefs.getTimerStudyDuration();
        updateTimerText(
                timerTextView,
                TimeUnit.MINUTES.toMillis(defaultStudyDurationMinutes)
        );

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
    }

    @Override
    public void onResume() {
        super.onResume();

        // If idle, refresh timer display in case settings changed
        if (homeViewModel.currentState.getValue() == HomeViewModel.PomodoroState.IDLE) {
            // NOTE: on first load, timerTextView is still null
            if (timerTextView != null) updateTimerText(timerTextView, TimeUnit.MINUTES.toMillis(prefs.getTimerStudyDuration()));
        }

        // Reload avatar in case it changed
        if (profilePictureManager != null) {
            profilePictureManager.loadProfilePicture();
        }
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
            // if idle -> show text
            if (homeViewModel.currentState.getValue() == HomeViewModel.PomodoroState.IDLE) {
                updateTimerText(timerTextView, TimeUnit.MINUTES.toMillis(prefs.getTimerStudyDuration()));
            }
            // if running -> show remaining millis
            else {
                updateTimerText(timerTextView, millis);
            }
        });

        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) coinBalanceTextView.setText(String.valueOf(balance));
        });

        homeViewModel.showQuestionnaireEvent.observe(getViewLifecycleOwner(), sessionId -> {
            if (sessionId != null) {
                showQuestionnaires(sessionId);
                homeViewModel.showQuestionnaireEvent.setValue(null);
            }
        });
    }

    private void startDefaultSession() {
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

    private void earnCoin(int amount) {
        if (profileViewModel != null) profileViewModel.addCoins(amount);
        String message = (amount == 1) ? "+1 Coin!" : String.format(Locale.getDefault(), "+%d Coins!", amount);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
