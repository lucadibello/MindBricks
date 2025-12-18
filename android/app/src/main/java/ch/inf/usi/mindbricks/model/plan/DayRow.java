package ch.inf.usi.mindbricks.model.plan;

import androidx.annotation.NonNull;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

public record DayRow(DayKey dayKey,
                     @NonNull MaterialCheckBox checkBox,
                     @NonNull Slider slider,
                     @NonNull MaterialTextView hoursLabel) {
}
