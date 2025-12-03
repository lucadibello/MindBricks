package ch.inf.usi.mindbricks.ui.nav.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.slider.Slider;

import java.util.Locale;

import ch.inf.usi.mindbricks.R;

public class SettingsFragment extends DialogFragment {

    // Declare all the views
    private TextView studyDurationText;
    private Slider studyDurationSlider;
    private TextView pauseDurationText;
    private Slider pauseDurationSlider;
    private Button saveButton;

    private SharedPreferences sharedPreferences;
    public static final String PREFS_NAME = "TimerSettings";
    public static final String KEY_STUDY_DURATION = "StudyDuration";
    public static final String KEY_PAUSE_DURATION = "PauseDuration";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        studyDurationText = view.findViewById(R.id.study_duration_text);
        studyDurationSlider = view.findViewById(R.id.study_duration_slider);
        pauseDurationText = view.findViewById(R.id.pause_duration_text);
        pauseDurationSlider = view.findViewById(R.id.pause_duration_slider);
        saveButton = view.findViewById(R.id.save_settings_button);

        // Load previously saved values
        loadSettings();

        studyDurationSlider.addOnChangeListener((slider, value, fromUser) -> {
            studyDurationText.setText(String.format(Locale.getDefault(), "%.0f min", value));
        });

        pauseDurationSlider.addOnChangeListener((slider, value, fromUser) -> {
            pauseDurationText.setText(String.format(Locale.getDefault(), "%.0f min", value));
        });

        // Set a click listener on the new save button
        saveButton.setOnClickListener(v -> {
            // Save the current slider values to SharedPreferences
            saveSettings();

            // Show a confirmation message to the user
            Toast.makeText(getContext(), "Settings saved!", Toast.LENGTH_SHORT).show();

            // Close the dialog
            dismiss();
        });
    }

    private void loadSettings() {
        // Load the saved study duration, or default to 25 if not found.
        float savedStudyValue = sharedPreferences.getFloat(KEY_STUDY_DURATION, 25.0f);
        studyDurationSlider.setValue(savedStudyValue);
        studyDurationText.setText(String.format(Locale.getDefault(), "%.0f min", savedStudyValue));

        // Load the saved pause duration, or default to 5.
        float savedPauseValue = sharedPreferences.getFloat(KEY_PAUSE_DURATION, 5.0f);
        pauseDurationSlider.setValue(savedPauseValue);
        pauseDurationText.setText(String.format(Locale.getDefault(), "%.0f min", savedPauseValue));
    }

    private void saveSettings() {
        // Use an editor to save the current slider values
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_STUDY_DURATION, studyDurationSlider.getValue());
        editor.putFloat(KEY_PAUSE_DURATION, pauseDurationSlider.getValue());
        editor.apply();
    }


}
