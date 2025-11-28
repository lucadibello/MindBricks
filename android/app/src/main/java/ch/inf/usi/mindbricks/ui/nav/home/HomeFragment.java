package ch.inf.usi.mindbricks.ui.nav.home;

// ADDED: Import the Context class for onAttach
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Button startSessionButton;
    private TextView coinBalanceTextView;

    private ProfileViewModel profileViewModel;

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

    // ADDED: A reference to the navigation locker interface
    private NavigationLocker navigationLocker;

    /**
     * ADDED: Get a reference to the hosting Activity as a NavigationLocker.
     * This is called before onCreateView.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationLocker) {
            navigationLocker = (NavigationLocker) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement NavigationLocker");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timerTextView = view.findViewById(R.id.timer_text_view);
        startSessionButton = view.findViewById(R.id.start_stop_button);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);

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
        durationSlider.addOnChangeListener((slider, value, fromUser) -> {
            durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) value));
        });

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

    private void startTimer(int minutes) {
        // Disable navigation when the timer starts
        if (navigationLocker != null) {
            navigationLocker.setNavigationEnabled(false);
        }

        isTimerRunning = true;
        startSessionButton.setText(R.string.stop_session);
        startSessionButton.setEnabled(false);

        long durationInMillis = TimeUnit.MINUTES.toMillis(minutes);
        final int[] minutesCompleted = {0};

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
                    // This method also handles re-enabling the navigation
                    resetTimerState();
                })
                .setCancelable(false)
                .show();
    }

    private void stopTimerAndReset() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        resetTimerState();
    }

    /**
     * UPDATED: Resets all timer-related UI and state variables, and re-enables navigation.
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
    }

    /**
     * ADDED: Clean up the reference to the activity to prevent memory leaks.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        navigationLocker = null;
    }
}
