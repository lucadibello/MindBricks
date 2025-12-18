package ch.inf.usi.mindbricks.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.plan.DayHours;
import ch.inf.usi.mindbricks.model.plan.DayRow;
import ch.inf.usi.mindbricks.util.Hours;
import ch.inf.usi.mindbricks.util.PreferencesManager;

public class SettingsStudyPlanFragment extends Fragment {

    private PreferencesManager prefs;
    private final List<DayRow> dayRows = new ArrayList<>();
    private MaterialTextView weeklyTotal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_study_plan, container, false);

        prefs = new PreferencesManager(requireContext());
        weeklyTotal = view.findViewById(R.id.textWeeklyTotal);

        bindDayRows(view);
        restorePlanFromPreferences();
        updateWeeklyTotal();

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        persistPlan();
    }

    private void bindDayRows(View view) {
        dayRows.add(bindDayRow(view, R.id.dayMonday, R.string.weekday_monday, "monday"));
        dayRows.add(bindDayRow(view, R.id.dayTuesday, R.string.weekday_tuesday, "tuesday"));
        dayRows.add(bindDayRow(view, R.id.dayWednesday, R.string.weekday_wednesday, "wednesday"));
        dayRows.add(bindDayRow(view, R.id.dayThursday, R.string.weekday_thursday, "thursday"));
        dayRows.add(bindDayRow(view, R.id.dayFriday, R.string.weekday_friday, "friday"));
        dayRows.add(bindDayRow(view, R.id.daySaturday, R.string.weekday_saturday, "saturday"));
        dayRows.add(bindDayRow(view, R.id.daySunday, R.string.weekday_sunday, "sunday"));
    }

    private DayRow bindDayRow(View root, int containerId, int labelRes, String dayKey) {
        View container = root.findViewById(containerId);
        MaterialCheckBox checkBox = container.findViewById(R.id.checkDay);
        Slider slider = container.findViewById(R.id.sliderHours);
        MaterialTextView hoursLabel = container.findViewById(R.id.textDayHours);

        checkBox.setText(labelRes);
        updateHoursLabel(hoursLabel, slider.getValue());

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            slider.setEnabled(isChecked);
            if (!isChecked) {
                checkBox.setError(null);
                slider.setValue(0f);
                updateHoursLabel(hoursLabel, 0f);
                updateWeeklyTotal();
            }
        });
        
        slider.addOnChangeListener((s, value, fromUser) -> {
            updateHoursLabel(hoursLabel, value);
            if (value > 0f && !checkBox.isChecked()) {
                checkBox.setChecked(true);
            }
            if (value > 0f) {
                checkBox.setError(null);
            }
            updateWeeklyTotal();
        });

        return new DayRow(dayKey, checkBox, slider, hoursLabel);
    }

    private void restorePlanFromPreferences() {
        List<DayHours> plan = prefs.getStudyPlan();
        for (DayHours dayHours : plan) {
            DayRow row = findRow(dayHours.dayKey());
            if (row != null) {
                row.checkBox().setChecked(true);
                row.slider().setEnabled(true);
                row.slider().setValue(dayHours.hours());
                updateHoursLabel(row.hoursLabel(), dayHours.hours());
            }
        }
        updateWeeklyTotal();
    }

    private DayRow findRow(String dayKey) {
        for (DayRow row : dayRows) {
            if (row.dayKey().equals(dayKey)) {
                return row;
            }
        }
        return null;
    }

    private void persistPlan() {
        List<DayHours> plan = new ArrayList<>();
        for (DayRow row : dayRows) {
            if (!row.checkBox().isChecked()) continue;
            float hours = row.slider().getValue();
            if (hours > 0f) {
                plan.add(new DayHours(row.dayKey(), hours));
            } else {
                // If a day is checked but has 0 hours, uncheck it to reflect that it's not part of the plan
                row.checkBox().setChecked(false);
            }
        }
        prefs.setStudyPlan(plan);
        prefs.setStudyGoalSet(!plan.isEmpty());
    }

    private void updateHoursLabel(MaterialTextView label, float hours) {
        label.setText(getString(R.string.settings_plan_day_hours_count_label) + ": " + Hours.formatHours(hours));
    }

    private void updateWeeklyTotal() {
        if (weeklyTotal == null) return;
        float total = 0f;
        for (DayRow row : dayRows) {
            if (row.checkBox().isChecked()) {
                total += row.slider().getValue();
            }
        }
        weeklyTotal.setText(getString(R.string.settings_plan_weekly_total_label, Hours.formatHours(total)));
    }
}
