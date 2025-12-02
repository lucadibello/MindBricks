package ch.inf.usi.mindbricks.ui.nav.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;
import ch.inf.usi.mindbricks.util.PermissionManager;
import ch.inf.usi.mindbricks.util.PermissionManager.PermissionRequest;
import ch.inf.usi.mindbricks.util.ProfileViewModel;
import ch.inf.usi.mindbricks.util.SessionRecordingManager;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Button startSessionButton;
    private TextView coinBalanceTextView;

    private ProfileViewModel profileViewModel;

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private PermissionRequest micPermissionRequest;
    private Integer pendingDurationMinutes = null;
    private SessionRecordingManager sessionRecordingManager;
    private StudySession currentStudySession;
    private AppDatabase db;
    private NavigationLocker navigationLocker;

    /**
     * Get a reference to the hosting Activity as a NavigationLocker.
     * This is called before onCreateView.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationLocker) {
            navigationLocker = (NavigationLocker) context;
        } else {
            throw new RuntimeException(context + " must implement NavigationLocker");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // initialize db + session recorder
        db = AppDatabase.getInstance(requireContext());
        sessionRecordingManager = new SessionRecordingManager(requireContext());

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timerTextView = view.findViewById(R.id.timer_text_view);
        startSessionButton = view.findViewById(R.id.start_stop_button);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);

        micPermissionRequest = PermissionManager.registerSinglePermission(
                this,
                Manifest.permission.RECORD_AUDIO,
                () -> {
                    if (pendingDurationMinutes != null) {
                        startTimerInternal(pendingDurationMinutes);
                        pendingDurationMinutes = null;
                    }
                },
                () -> {
                    pendingDurationMinutes = null;
                    Toast.makeText(getContext(), "Microphone permission is required to record ambient noise.", Toast.LENGTH_SHORT).show();
                }
        );

        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                coinBalanceTextView.setText(String.valueOf(balance));
            }
        });

        updateTimerUI(0);

        startSessionButton.setOnClickListener(v -> {
            if (isTimerRunning) {
                confirmEndSessionDialog();
            } else {
                showDurationPickerDialog();
            }
        });
    }

    private void showDurationPickerDialog() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_timer_session, null);

        final Slider durationSlider = dialogView.findViewById(R.id.duration_slider);
        final TextView durationText = dialogView.findViewById(R.id.duration_text);

        durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) durationSlider.getValue()));
        durationSlider.addOnChangeListener((slider, value, fromUser) ->
                durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) value)));

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("Set Session Duration")
                .setPositiveButton("Start", (dialog, which) -> {
                    int durationInMinutes = (int) durationSlider.getValue();
                    if (durationInMinutes > 0) {
                        startTimer(durationInMinutes);
                    } else {
                        Toast.makeText(getContext(), "Please select a duration greater than 0.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @SuppressLint("MissingPermission")
    private void startTimer(int minutes) {
        if (!PermissionManager.hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
            pendingDurationMinutes = minutes;
            if (micPermissionRequest != null) {
                micPermissionRequest.launch();
            } else {
                Toast.makeText(getContext(), "Microphone permission required to record ambient noise.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        startTimerInternal(minutes);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startTimerInternal(int minutes) {
        // Disable navigation when the timer starts
        if (navigationLocker != null) {
            navigationLocker.setNavigationEnabled(false);
        }

        isTimerRunning = true;
        startSessionButton.setText(R.string.stop_session);
        startSessionButton.setEnabled(false);

        long durationInMillis = TimeUnit.MINUTES.toMillis(minutes);
        final int[] minutesCompleted = {0};

        // Create and insert current study session in DB + retrieve its ID for later
        new Thread(() -> {
            currentStudySession = new StudySession(
                    System.currentTimeMillis(),
                    minutes,
                    "General", // FIXME: move default tag somewhere else. Hard-coded currently
                    android.graphics.Color.GRAY // // FIXME: move also the default color somewhere else
            );
            long newId = db.studySessionDao().insert(currentStudySession);
            currentStudySession.setId(newId); // store id in order to correctly append logs

            // Start recording manage
            new Handler(Looper.getMainLooper()).post(() -> sessionRecordingManager.startSession(newId));
        }).start();


        countDownTimer = new CountDownTimer(durationInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerUI(millisUntilFinished);

                long elapsedMillis = durationInMillis - millisUntilFinished;
                int elapsedMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(elapsedMillis);
                if (elapsedMinutes > minutesCompleted[0]) {
                    minutesCompleted[0] = elapsedMinutes;
                    earnCoin(1);
                }
            }

            @Override
            public void onFinish() {
                if (minutes > minutesCompleted[0]) {
                    earnCoin(1);
                }
                stopTimerAndReset();
                showSessionCompleteDialog();
            }
        }.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isTimerRunning) {
                startSessionButton.setEnabled(true);
            }
        }, 1500);
    }

    private void confirmEndSessionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("End Session?")
                .setMessage("Are you sure you want to end the current session early? You will not get a coin for the current minute.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    stopTimerAndReset();
                    Toast.makeText(getContext(), "Session ended.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSessionCompleteDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Session Complete!")
                .setMessage("Great focus! You've earned 3 bonus coins for completing the session.")
                .setPositiveButton("Awesome!", (dialog, which) -> {
                    earnCoin(3);
                })
                .setCancelable(false)
                .show();
    }

    private void stopTimerAndReset() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Stop recording and store data in db
        if (currentStudySession != null) {
            sessionRecordingManager.stopSession(currentStudySession);
            currentStudySession = null; // Clear current session
        }

        // reset timer to default state
        resetTimerState();
    }

    /**
     * Resets all timer-related UI and state variables, and re-enables navigation.
     */
    private void resetTimerState() {
        //  Re-enable navigation when the timer is reset
        if (navigationLocker != null) {
            navigationLocker.setNavigationEnabled(true);
        }

        isTimerRunning = false;
        startSessionButton.setText(R.string.start_session);
        startSessionButton.setEnabled(true);
        updateTimerUI(0);
    }

    private void updateTimerUI(long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(minutes);
        String timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerTextView.setText(timeString);
    }

    private void earnCoin(int amount) {
        profileViewModel.addCoins(amount);
        String message = (amount == 1) ? "+1 Coin!" : String.format(Locale.getDefault(), "+%d Coins!", amount);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // if destroying the fragment, stop recording and store data in db
        if (currentStudySession != null) {
            sessionRecordingManager.stopSession(currentStudySession);
        }
    }
}
