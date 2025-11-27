package ch.inf.usi.mindbricks.ui.nav.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import ch.inf.usi.mindbricks.R;

public class SessionTimerDialogFragment extends DialogFragment {

    public Slider durationSlider;
    public TextView durationText;
    public Button startTimerButton;
    public HomeViewModel homeViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_timer_session, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // Find views
        durationSlider = view.findViewById(R.id.duration_slider);
        durationText = view.findViewById(R.id.duration_text);

        // Listener for the slider
        durationSlider.addOnChangeListener((slider, value, fromUser) -> {
            // Update the text view as the slider moves
            durationText.setText((int) value + " minutes");
        });

        // Listener for the start button
        startTimerButton.setOnClickListener(v -> {
            int durationInMinutes = (int) durationSlider.getValue();
            homeViewModel.startTimer(durationInMinutes);
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Optional: Make the dialog wider
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
