package ch.inf.usi.mindbricks.model.plan;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

public record DayRow(String dayKey, MaterialCheckBox checkBox, Slider slider,
                     MaterialTextView hoursLabel) {
}
