package ch.inf.usi.mindbricks.ui.nav.home;

import android.content.DialogInterface;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // Required for ViewModel

import java.util.Locale;

import ch.inf.usi.mindbricks.R;
// Make sure this import path matches where you placed your ProfileViewModel
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Button startStopButton;
    private ImageView menuIcon;
    private TextView coinBalanceTextView;

    // Timer variables
    private int seconds = 0;
    private boolean isRunning = false;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // Declare the shared ViewModel
    private ProfileViewModel profileViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Initialize the ViewModel by scoping it to the Activity.
        // This is the key to sharing it with other fragments.
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find all the views
        timerTextView = view.findViewById(R.id.timer_text_view);
        startStopButton = view.findViewById(R.id.start_stop_button);
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

        menuIcon.setOnClickListener(v -> {
            DrawerLayout drawer = requireActivity().findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(GravityCompat.END);
            }
        });
    }

    /**
     * Handles the logic for the main Start/Stop button.
     */
    private void handleStartStop() {
        if (isRunning) {
            checkEndedSession();
        } else {
            startTimer();
        }
    }

    /**
     * Displays a confirmation dialog before stopping the session.
     */
    public void checkEndedSession() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("End Session?");
        builder.setMessage("Are you sure you want to end the current study session?");

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            stopTimer();
        });

        builder.setNegativeButton("Abort", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(getContext(), "Session continued", Toast.LENGTH_SHORT).show();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Starts the timer and updates the UI.
     */
    private void startTimer() {
        isRunning = true;
        startStopButton.setText(R.string.stop_session);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                seconds++;
                updateTimerUI();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    /**
     * Stops the timer, calculates coins, resets the UI, and updates the ViewModel.
     */
    private void stopTimer() {
        if (!isRunning) return;

        int minutesStudied = seconds / 60;
        int coinsEarned = minutesStudied;

        if (coinsEarned > 0) {
            // Use the ViewModel to add coins.
            profileViewModel.addCoins(coinsEarned);
            Toast.makeText(getContext(), "You earned " + coinsEarned + " coins!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Session ended. Study for at least a minute to earn coins.", Toast.LENGTH_LONG).show();
        }

        timerHandler.removeCallbacks(timerRunnable);
        seconds = 0;
        isRunning = false;
        startStopButton.setText(R.string.start_session);
        updateTimerUI();
    }

    /**
     * Updates the timer TextView with the properly formatted time.
     */
    private void updateTimerUI() {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        String timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
        timerTextView.setText(timeString);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Always clean up handlers to prevent memory leaks
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}
