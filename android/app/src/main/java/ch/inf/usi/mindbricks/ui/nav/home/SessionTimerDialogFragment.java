package ch.inf.usi.mindbricks.ui.nav.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import java.util.Locale;

import ch.inf.usi.mindbricks.R;

public class SessionTimerDialogFragment extends DialogFragment {

    private Slider durationSlider;
    private TextView durationText;
    private Button startTimerButton;
    private HomeViewModel homeViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Correctly initialize the AndroidViewModel using its factory
        // NOTE: We get the ViewModel from the HOSTING FRAGMENT (this), not the activity.
        homeViewModel = new ViewModelProvider(requireParentFragment(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        return inflater.inflate(R.layout.dialog_timer_session, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize all the UI components from the view
        durationSlider = view.findViewById(R.id.duration_slider);
        durationText = view.findViewById(R.id.duration_text);
        startTimerButton = view.findViewById(R.id.start_stop_button);

        // Safely set the initial text based on the slider's default value
        int initialValue = (int) durationSlider.getValue();
        durationText.setText(String.format(Locale.getDefault(), "%d minutes", initialValue));

        // Set the listener for when the slider value changes
        durationSlider.addOnChangeListener((slider, value, fromUser) -> {
            durationText.setText(String.format(Locale.getDefault(), "%d minutes", (int) value));
        });

        // Set the click listener for the start button
        startTimerButton.setOnClickListener(v -> {
            int durationInMinutes = (int) durationSlider.getValue();
            if (durationInMinutes > 0) {
                // Define the pause duration
                int pauseInMinutes = 5;

                // Call the correct method on the ViewModel
                homeViewModel.pomodoroTechnique(durationInMinutes, pauseInMinutes);

                // Close the dialog
                dismiss();
            } else {
                Toast.makeText(getContext(), "Please select a duration greater than 0.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set the dialog window dimensions
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
