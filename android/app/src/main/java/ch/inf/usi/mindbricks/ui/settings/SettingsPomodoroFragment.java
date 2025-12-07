package ch.inf.usi.mindbricks.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.PreferencesManager;

public class SettingsPomodoroFragment extends Fragment {

    private MaterialTextView studyDurationText;
    private Slider studyDurationSlider;
    private MaterialTextView pauseDurationText;
    private Slider pauseDurationSlider;
    private MaterialTextView longPauseDurationText;
    private Slider longPauseDurationSlider;

    private PreferencesManager prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_pomodoro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = new PreferencesManager(requireContext());

        studyDurationText = view.findViewById(R.id.study_duration_text);
        studyDurationSlider = view.findViewById(R.id.study_duration_slider);
        pauseDurationText = view.findViewById(R.id.pause_duration_text);
        pauseDurationSlider = view.findViewById(R.id.pause_duration_slider);
        longPauseDurationText = view.findViewById(R.id.long_pause_duration_text);
        longPauseDurationSlider = view.findViewById(R.id.long_pause_duration_slider);

        loadSettings();
        setupListeners();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveSettings();
    }

    private void loadSettings() {
        int study = prefs.getTimerStudyDuration();
        int pause = prefs.getTimerShortPauseDuration();
        int longPause = prefs.getTimerLongPauseDuration();

        studyDurationSlider.setValue(study);
        updateLabel(studyDurationText, study);

        pauseDurationSlider.setValue(pause);
        updateLabel(pauseDurationText, pause);

        longPauseDurationSlider.setValue(longPause);
        updateLabel(longPauseDurationText, longPause);
    }

    private void setupListeners() {
        studyDurationSlider.addOnChangeListener((slider, value, fromUser) -> 
            updateLabel(studyDurationText, (int) value));

        pauseDurationSlider.addOnChangeListener((slider, value, fromUser) -> 
            updateLabel(pauseDurationText, (int) value));

        longPauseDurationSlider.addOnChangeListener((slider, value, fromUser) -> 
            updateLabel(longPauseDurationText, (int) value));
    }

    private void saveSettings() {
        prefs.setTimerStudyDuration((int) studyDurationSlider.getValue());
        prefs.setTimerShortPauseDuration((int) pauseDurationSlider.getValue());
        prefs.setTimerLongPauseDuration((int) longPauseDurationSlider.getValue());
    }

    private void updateLabel(MaterialTextView label, int minutes) {
        label.setText(String.format(Locale.getDefault(), "%d min", minutes));
    }
}