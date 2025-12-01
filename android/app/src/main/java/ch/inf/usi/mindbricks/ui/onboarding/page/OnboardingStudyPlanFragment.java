package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.plan.DayHours;
import ch.inf.usi.mindbricks.model.plan.DayRow;
import ch.inf.usi.mindbricks.ui.onboarding.OnboardingStepValidator;
import ch.inf.usi.mindbricks.util.PreferencesManager;

public class OnboardingStudyPlanFragment extends Fragment implements OnboardingStepValidator {

    private PreferencesManager prefs;
    private final List<DayRow> dayRows = new ArrayList<>();
    private MaterialTextView weeklyTotal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_study_plan, container, false);

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
        persistIfComplete();
    }

    @Override
    public boolean validateStep() {
        List<DayHours> plan = new ArrayList<>();
        boolean hasErrors = false;
        for (DayRow row : dayRows) {
            row.checkBox().setError(null);
            if (!row.checkBox().isChecked()) continue;

            float hours = row.slider().getValue();
            if (hours <= 0f) {
                row.checkBox().setError(getString(R.string.onboarding_study_plan_error_hours_required));
                hasErrors = true;
                continue;
            }
            plan.add(new DayHours(row.dayKey(), hours));
        }

        if (plan.isEmpty()) {
            hasErrors = true;
            View root = getView();
            if (root != null) {
                Snackbar.make(root, R.string.onboarding_study_plan_error_day_required, Snackbar.LENGTH_SHORT).show();
            }
        }

        if (hasErrors) {
            return false;
        }

        prefs.setStudyPlan(plan);
        return true;
    }

    private void bindDayRows(View view) {
        dayRows.add(bindDayRow(view, R.id.dayMonday, R.string.onboarding_study_plan_day_monday, "monday"));
        dayRows.add(bindDayRow(view, R.id.dayTuesday, R.string.onboarding_study_plan_day_tuesday, "tuesday"));
        dayRows.add(bindDayRow(view, R.id.dayWednesday, R.string.onboarding_study_plan_day_wednesday, "wednesday"));
        dayRows.add(bindDayRow(view, R.id.dayThursday, R.string.onboarding_study_plan_day_thursday, "thursday"));
        dayRows.add(bindDayRow(view, R.id.dayFriday, R.string.onboarding_study_plan_day_friday, "friday"));
        dayRows.add(bindDayRow(view, R.id.daySaturday, R.string.onboarding_study_plan_day_saturday, "saturday"));
        dayRows.add(bindDayRow(view, R.id.daySunday, R.string.onboarding_study_plan_day_sunday, "sunday"));
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

    private void persistIfComplete() {
        List<DayHours> plan = new ArrayList<>();
        for (DayRow row : dayRows) {
            if (!row.checkBox().isChecked()) continue;
            float hours = row.slider().getValue();
            if (hours > 0f) {
                plan.add(new DayHours(row.dayKey(), hours));
            }
        }

        if (!plan.isEmpty()) {
            prefs.setStudyPlan(plan);
        }
    }

    private void updateHoursLabel(MaterialTextView label, float hours) {
        label.setText(getString(R.string.onboarding_study_plan_hours_label) + ": " + formatHours(hours));
    }

    private String formatHours(float hours) {
        if (hours == (long) hours) {
            return String.format(Locale.US, "%d h", (long) hours);
        }
        return String.format(Locale.US, "%.1f h", hours);
    }

    private void updateWeeklyTotal() {
        if (weeklyTotal == null) return;
        float total = 0f;
        for (DayRow row : dayRows) {
            if (row.checkBox().isChecked()) {
                total += row.slider().getValue();
            }
        }
        weeklyTotal.setText(getString(R.string.onboarding_study_plan_total_placeholder, formatHours(total)));
    }
}
