package ch.inf.usi.mindbricks.ui.settings;

import android.os.Bundle;
import android.util.Log;
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
import ch.inf.usi.mindbricks.model.plan.DayKey;
import ch.inf.usi.mindbricks.model.plan.DayRow;
import ch.inf.usi.mindbricks.util.Hours;
import ch.inf.usi.mindbricks.util.PreferencesManager;

/**
 * Fragment for the study plan settings.
 */
public class SettingsStudyPlanFragment extends Fragment {

    /**
     * Preferences manager for saving and retrieving study plan data.
     */
    private PreferencesManager prefs;

    /**
     * List of day rows representing the study plan.
     */
    private final DayRow[] dayRows = new DayRow[7];

    /**
     * TextView to display the total hours for the week.
     */
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

    /**
     * Binds the day rows to the views in the layout.
     *
     * @param view The root view of the fragment.
     */
    private void bindDayRows(View view) {
        // get container for each day row
        dayRows[0] = (bindDayRow(view.findViewById(R.id.dayMonday), R.string.weekday_monday, DayKey.MONDAY));

        dayRows[1] = (bindDayRow(view.findViewById(R.id.dayTuesday), R.string.weekday_tuesday, DayKey.TUESDAY));
        dayRows[2] = (bindDayRow(view.findViewById(R.id.dayWednesday), R.string.weekday_wednesday, DayKey.WEDNESDAY));
        dayRows[3] = (bindDayRow(view.findViewById(R.id.dayThursday), R.string.weekday_thursday, DayKey.THURSDAY));
        dayRows[4] = (bindDayRow(view.findViewById(R.id.dayFriday), R.string.weekday_friday, DayKey.FRIDAY));
        dayRows[5] = (bindDayRow(view.findViewById(R.id.daySaturday), R.string.weekday_saturday, DayKey.SATURDAY));
        dayRows[6] = (bindDayRow(view.findViewById(R.id.daySunday), R.string.weekday_sunday, DayKey.SUNDAY));
    }

    /**
     * Binds a single day row to the views in the layout.
     *
     * @param container The container view for the day row.
     * @param label The label for the day row.
     * @param key The key for the day row.
     * @return The bound day row.
     */
    private DayRow bindDayRow(View container, int label, DayKey key) {
        MaterialCheckBox checkBox = container.findViewById(R.id.checkDay);
        Slider slider = container.findViewById(R.id.sliderHours);
        MaterialTextView hoursLabel = container.findViewById(R.id.textDayHours);

        checkBox.setText(label);
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

        return new DayRow(key, checkBox, slider, hoursLabel);
    }

    private void restorePlanFromPreferences() {
        List<DayHours> plan = prefs.getStudyPlan();
        for (DayHours dayHours : plan) {
            DayRow row = dayRows[dayHours.dayKey().getIndex()];

            if (row != null) {
                row.checkBox().setChecked(true);
                row.slider().setEnabled(true);
                row.slider().setValue(dayHours.hours());
                updateHoursLabel(row.hoursLabel(), dayHours.hours());
            } else {
                Log.w(getClass().getSimpleName(), "No day found for key: " + dayHours.dayKey());
            }
        }
        updateWeeklyTotal();
    }

    /**
     * Persists the study plan to the preferences.
     */
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

    /**
     * Updates the hours label text.
     *
     * @param label The label view to update.
     * @param hours The hours value to display.
     */
    private void updateHoursLabel(MaterialTextView label, float hours) {
        label.setText(getString(R.string.settings_plan_day_hours_count_label) + ": " + Hours.formatHours(hours));
    }

    /**
     * Updates the total hours for the week.
     */
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
