package ch.inf.usi.mindbricks.ui.nav.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.slider.Slider;

import java.util.Locale;

import ch.inf.usi.mindbricks.R;

public class SettingsFragment extends DialogFragment {

    // Declare the views in the settings layout
    private TextView studyDurationText;
    private Slider studyDurationSlider;
    private TextView pauseDurationText;
    private Slider pauseDurationSlider;

    // Declare SharedPreferences for saving the settings
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

        // Load the saved settings when the dialog is created
        loadSettings();

        // Add a listener to the study slider to update the text when the user moves it.
        studyDurationSlider.addOnChangeListener((slider, value, fromUser) -> {
            studyDurationText.setText(String.format(Locale.getDefault(), "%.0f min", value));
        });

        // Add a listener to the pause slider.
        pauseDurationSlider.addOnChangeListener((slider, value, fromUser) -> {
            pauseDurationText.setText(String.format(Locale.getDefault(), "%.0f min", value));
        });
    }

    private void loadSettings() {
        // Get the saved study duration, using 25 minutes as the default value if nothing is saved
        float savedStudyValue = sharedPreferences.getFloat(KEY_STUDY_DURATION, 25.0f);
        studyDurationSlider.setValue(savedStudyValue);
        studyDurationText.setText(String.format(Locale.getDefault(), "%.0f min", savedStudyValue));

        // Get the saved pause duration, using 5 minutes as the default
        float savedPauseValue = sharedPreferences.getFloat(KEY_PAUSE_DURATION, 5.0f);
        pauseDurationSlider.setValue(savedPauseValue);
        pauseDurationText.setText(String.format(Locale.getDefault(), "%.0f min", savedPauseValue));
    }

    private void saveSettings() {
        // Get an editor to write the new values to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_STUDY_DURATION, studyDurationSlider.getValue());
        editor.putFloat(KEY_PAUSE_DURATION, pauseDurationSlider.getValue());
        editor.apply();
    }

    @Override
    public void onStop() {
        super.onStop();
        saveSettings();
    }
}
