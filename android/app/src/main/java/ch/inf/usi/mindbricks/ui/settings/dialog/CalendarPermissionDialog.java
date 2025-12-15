package ch.inf.usi.mindbricks.ui.settings.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import ch.inf.usi.mindbricks.R;

/**
 * Dialog that explains why calendar permission is needed before requesting it.
 */
public class CalendarPermissionDialog extends DialogFragment {

    private static final String TAG = "CalendarPermissionDialog";
    private static final String ARG_CALENDAR_TYPE = "calendar_type";

    public enum CalendarType {
        DEVICE
    }

    public interface PermissionCallback {
        void onPermissionAccepted();
        void onPermissionDeclined();
    }

    private CalendarType calendarType;
    private PermissionCallback callback;

    public static void show(FragmentManager fragmentManager,
                            CalendarType type,
                            PermissionCallback callback) {
        CalendarPermissionDialog dialog = new CalendarPermissionDialog();
        dialog.calendarType = type;
        dialog.callback = callback;

        Bundle args = new Bundle();
        args.putString(ARG_CALENDAR_TYPE, type.name());
        dialog.setArguments(args);

        dialog.show(fragmentManager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_MindBricks_Dialog);

        if (getArguments() != null) {
            String typeName = getArguments().getString(ARG_CALENDAR_TYPE, CalendarType.DEVICE.name());
            calendarType = CalendarType.valueOf(typeName);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_calendar_permission, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        ImageView iconView = view.findViewById(R.id.iconCalendar);
        MaterialTextView titleText = view.findViewById(R.id.textTitle);
        MaterialTextView descriptionText = view.findViewById(R.id.textDescription);
        MaterialTextView benefit1 = view.findViewById(R.id.textBenefit1);
        MaterialTextView benefit2 = view.findViewById(R.id.textBenefit2);
        MaterialTextView benefit3 = view.findViewById(R.id.textBenefit3);
        MaterialButton allowButton = view.findViewById(R.id.buttonAllow);
        MaterialButton declineButton = view.findViewById(R.id.buttonDecline);

        configureForCalendarType(iconView, titleText, descriptionText, benefit1, benefit2, benefit3);

        // Set click listeners
        allowButton.setOnClickListener(v -> {
            dismiss();
            if (callback != null) {
                callback.onPermissionAccepted();
            }
        });

        declineButton.setOnClickListener(v -> {
            dismiss();
            if (callback != null) {
                callback.onPermissionDeclined();
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set dialog width
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void configureForCalendarType(ImageView icon,
                                          MaterialTextView title,
                                          MaterialTextView description,
                                          MaterialTextView benefit1,
                                          MaterialTextView benefit2,
                                          MaterialTextView benefit3) {
        Context context = requireContext();

        switch (calendarType) {
            case DEVICE:
                icon.setImageResource(R.drawable.ic_calendar);
                icon.setColorFilter(context.getColor(R.color.activity_deep_study));
                title.setText(R.string.permission_calendar_device_title);
                description.setText(R.string.permission_calendar_device_description);
                benefit1.setText(R.string.permission_calendar_benefit_schedule);
                benefit2.setText(R.string.permission_calendar_benefit_conflicts);
                benefit3.setText(R.string.permission_calendar_benefit_smart);
                break;
        }
    }
}