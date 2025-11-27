package ch.inf.usi.mindbricks.ui.nav.home;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // Required for ViewModel

import com.google.android.material.slider.Slider;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
// Make sure this import path matches where you placed your ProfileViewModel
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Button startSessionButton;
    private ImageView menuIcon;
    private TextView coinBalanceTextView;

    // Timer variables
    private int seconds = 0;
    private boolean isRunning = false;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // Declare the shared ViewModel
    private ProfileViewModel profileViewModel;
    private CoinManager coinManager;

    // Timer variables
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long lastMinuteMark = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Initialize the ViewModel by scoping it to the Activity.
        // This is the key to sharing it with other fragments.
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // We no longer need to inflate the view here since onViewCreated will handle it with binding
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find all the views
        coinManager = new CoinManager(requireActivity().getApplicationContext());
        timerTextView = view.findViewById(R.id.timer_text_view);
        startSessionButton = view.findViewById(R.id.start_stop_button);
        menuIcon = view.findViewById(R.id.drawer_menu);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);

        // CORRECTED: Observe the public 'coins' LiveData field.
        // This block will now automatically update the UI whenever the coin balance changes.
        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                coinBalanceTextView.setText(String.valueOf(balance));
            }
        });

        // Set up the button click listeners
        startStopButton.setOnClickListener(v -> handleStartStop());
        updateCoinDisplay();
        updateTimerUI(0); // Set initial timer text to 00:00

        // --- Button Listeners ---
        startSessionButton.setOnClickListener(v -> {
            if (isTimerRunning) {
                confirmEndSessionDialog();
            } else {
                showDurationPickerDialog();
            }
        });

        menuIcon.setOnClickListener(v -> {
            DrawerLayout drawer = requireActivity().findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(GravityCompat.END);
            }
        });
    }

    /**
     * Shows the new dialog with a slider for picking the session duration.
     */
    private void showDurationPickerDialog() {
        // Inflate the custom layout
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_timer_session, null);

        final Slider durationSlider = dialogView.findViewById(R.id.duration_slider);
        final TextView durationText = dialogView.findViewById(R.id.duration_text);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            stopTimer();
        });
        // Set an initial value for the duration text
        durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) durationSlider.getValue()));

        // Update the duration text whenever the slider value changes
        durationSlider.addOnChangeListener((slider, value, fromUser) -> {
            durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) value));
        });

        // Build the AlertDialog
        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Start", (dialog, which) -> {
                    int durationInMinutes = (int) durationSlider.getValue();
                    startTimer(durationInMinutes);
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * Starts the countdown timer.
     * @param minutes The duration of the session in minutes.
     */
    private void startTimer(int minutes) {
        isTimerRunning = true;
        startSessionButton.setText(R.string.stop_session);
        startSessionButton.setEnabled(false);

        long durationInMillis = (long) minutes * 60 * 1000;
        lastMinuteMark = minutes; // Initialize the last minute mark

        countDownTimer = new CountDownTimer(durationInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerUI(millisUntilFinished);

                // Calculate the current minute å
                long currentMinute = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);

                // If we've crossed into a new minute that's less than the last one we recorded...
                if (currentMinute < lastMinuteMark) { // <<< THE PROBLEM IS HERE
                    lastMinuteMark = currentMinute; // Update the mark
                    earnCoin(); // Award a coin
                }
            }


            @Override
            public void onFinish() {
                Toast.makeText(getContext(), "Session Complete!", Toast.LENGTH_LONG).show();
                // Award the final coin for the last minute of study
                earnCoin();
                resetTimerState();
            }
        }.start();

        // Re-enable the button after a short delay to prevent accidental double-clicks
        new Handler(requireActivity().getMainLooper()).postDelayed(() -> {
            if (isTimerRunning) { // Check if timer wasn't cancelled in the meantime
                startSessionButton.setEnabled(true);
            }
        }, 1500);
    }

    /**
     * Stops the timer, calculates coins, resets the UI, and updates the ViewModel.
     * Shows a confirmation dialog before stopping the timer.
     */
    private void confirmEndSessionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("End Session?")
                .setMessage("Are you sure you want to end the current session?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    stopTimerAndReset();
                    Toast.makeText(getContext(), "Session ended.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

        if (coinsEarned > 0) {
            // Use the ViewModel to add coins.
            profileViewModel.addCoins(coinsEarned);
            Toast.makeText(getContext(), "You earned " + coinsEarned + " coins!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Session ended. Study for at least a minute to earn coins.", Toast.LENGTH_LONG).show();
    /**
     * Stops the timer completely without awarding coins.
     */
    private void stopTimerAndReset() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        resetTimerState();
    }

        timerHandler.removeCallbacks(timerRunnable);
        seconds = 0;
        isRunning = false;
        startStopButton.setText(R.string.start_session);
        updateTimerUI();
    /**
     * Resets all timer-related UI and state variables.
     */
    private void resetTimerState() {
        isTimerRunning = false;
        startSessionButton.setText(R.string.start_session);
        startSessionButton.setEnabled(true);
        updateTimerUI(0);
    }

    /**
     * Updates the timer TextView with the properly formatted time from milliseconds.
     */
    private void updateTimerUI(long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(minutes);
        String timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerTextView.setText(timeString);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Always clean up handlers to prevent memory leaks
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
    /**
     * Adds one coin and shows a brief toast message.
     */
    private void earnCoin() {
        coinManager.addCoins(1);
        updateCoinDisplay();
        Toast.makeText(getContext(), "+1 Coin!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Gets the current balance from CoinManager and sets the text.
     */
    private void updateCoinDisplay() {
        if (coinBalanceTextView != null) {
            int balance = coinManager.getCoinBalance();
            coinBalanceTextView.setText(String.valueOf(balance));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel the timer to prevent memory leaks when the view is destroyed
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
