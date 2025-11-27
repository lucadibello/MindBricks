package ch.inf.usi.mindbricks.ui.nav.home;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;import android.view.LayoutInflater;
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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Button startSessionButton;
    private ImageView menuIcon;
    private TextView coinBalanceTextView;

    // Shared ViewModel for coin management
    private ProfileViewModel profileViewModel;

    // Timer variables
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long lastMinuteMark = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Initialize the ViewModel by scoping it to the Activity.
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timerTextView = view.findViewById(R.id.timer_text_view);
        startSessionButton = view.findViewById(R.id.start_stop_button);
        menuIcon = view.findViewById(R.id.drawer_menu);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);

        // Observe the coin balance from the ViewModel.
        // This automatically updates the coin text view whenever the value changes.
        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                coinBalanceTextView.setText(String.valueOf(balance));
            }
        });

        // Set initial timer text
        updateTimerUI(0);

        // Button Listeners
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
     * Shows the dialog with a slider for picking the session duration.
     */
    private void showDurationPickerDialog() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_timer_session, null);

        final Slider durationSlider = dialogView.findViewById(R.id.duration_slider);
        final TextView durationText = dialogView.findViewById(R.id.duration_text);

        // Set an initial value for the duration text
        durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) durationSlider.getValue()));

        // Update the duration text whenever the slider value changes
        durationSlider.addOnChangeListener((slider, value, fromUser) -> {
            durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) value));
        });

        // Build and show the AlertDialog
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

    /**
     * Starts the countdown timer.
     * @param minutes The duration of the session in minutes.
     */
    private void startTimer(int minutes) {
        isTimerRunning = true;
        startSessionButton.setText(R.string.stop_session);
        startSessionButton.setEnabled(false); // Disable to prevent conflicts

        long durationInMillis = TimeUnit.MINUTES.toMillis(minutes);
        lastMinuteMark = minutes; // Initialize with the starting number of minutes

        countDownTimer = new CountDownTimer(durationInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerUI(millisUntilFinished);

                // Calculate the current full minute remaining
                long currentMinute = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);

                // If the current minute is less than the last one we recorded, a full minute has passed.
                if (currentMinute < lastMinuteMark) {
                    lastMinuteMark = currentMinute;
                    earnCoin();
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

        // Re-enable the button after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isTimerRunning) {
                startSessionButton.setEnabled(true);
            }
        }, 1500);
    }

    /**
     * Shows a confirmation dialog before stopping the timer.
     */
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

    /**
     * Stops the timer completely without awarding coins for the unfinished minute.
     */
    private void stopTimerAndReset() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        resetTimerState();
    }

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

    /**
     * Adds one coin using the ViewModel and shows a brief toast message.
     */
    private void earnCoin() {
        profileViewModel.addCoins(1);
        // The LiveData observer will automatically update the coin balance text.
        Toast.makeText(getContext(), "+1 Coin!", Toast.LENGTH_SHORT).show();
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
