package ch.inf.usi.mindbricks.ui.nav.home;

import android.content.Context;
import android.os.Bundle;
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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class HomeFragment extends Fragment {

    private TextView timerTextView;
    private Button startSessionButton;
    private TextView coinBalanceTextView;
    private TextView sessionTitleTextView;

    private HomeViewModel homeViewModel;
    private ProfileViewModel profileViewModel;

    private NavigationLocker navigationLocker;

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
        homeViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timerTextView = view.findViewById(R.id.timer_text_view);
        startSessionButton = view.findViewById(R.id.start_stop_button);
        coinBalanceTextView = view.findViewById(R.id.coin_balance_text);

        setupObservers();

        homeViewModel.activityRecreated();

        startSessionButton.setOnClickListener(v -> {
            if (homeViewModel.currentState.getValue() != HomeViewModel.PomodoroState.IDLE) {
                confirmEndSessionDialog();
            } else {
                showDurationPickerDialog();
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

            if (!isRunning) {
                updateTimerUI(0);
            }
        });

        homeViewModel.stateTitle.observe(getViewLifecycleOwner(), title -> {
            if (sessionTitleTextView != null) {
                sessionTitleTextView.setText(title);
            }
        });

        homeViewModel.currentTime.observe(getViewLifecycleOwner(), this::updateTimerUI);

        homeViewModel.earnedCoinsEvent.observe(getViewLifecycleOwner(), amount -> {
            if (amount != null && amount > 0) {
                earnCoin(amount);
                homeViewModel.onCoinsAwarded();
            }
        });

        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                coinBalanceTextView.setText(String.valueOf(balance));
            }
        });
    }

    private void showDurationPickerDialog() {
        SessionTimerDialogFragment dialogFragment = new SessionTimerDialogFragment();
        dialogFragment.show(getChildFragmentManager(), "SessionTimerDialog");
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
        if (profileViewModel != null) {
            profileViewModel.addCoins(amount);
        }
        String message = (amount == 1) ? "+1 Coin!" : String.format(Locale.getDefault(), "+%d Coins!", amount);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
