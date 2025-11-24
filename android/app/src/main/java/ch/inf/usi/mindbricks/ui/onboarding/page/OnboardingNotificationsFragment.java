package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ch.inf.usi.mindbricks.R;

public class OnboardingNotificationsFragment extends Fragment {

    private MaterialButton button;
    private ActivityResultLauncher<String> permissionLauncher;
    private boolean hasRequested = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_notifications, container, false);

        // set listener on "enable" button
        button = view.findViewById(R.id.buttonEnableNotifications);
        button.setOnClickListener(v -> requestNotificationPermissionIfNeeded());

        // create permission launcher + change button color based on result
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    hasRequested = true;
                    updateButtonTint(granted);
                    if (!granted && !shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                        showOpenSettingsDialog();
                    }
                }
        );

        // render default state
        updateButtonTint(areNotificationsEnabled());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // update button color on resume -> user might have changed the permission settings via
        // app preferences!
        updateButtonTint(areNotificationsEnabled());
    }

    private void requestNotificationPermissionIfNeeded() {
        // check if notifications are granted
        if (areNotificationsEnabled()) {
            updateButtonTint(true);
            return;
        }

        // if not, check if we should show rationale
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
            showRationaleDialog();
        } else {
            // otherwise, launch permission manager normally (first permission request)
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /**
     * Checks whether notifications are enabled for the app.
     *
     * @return True if notifications are enabled, false otherwise.
     */
    private boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
    }

    /**
     * Show a dialog explaining why we need notifications.
     */
    private void showRationaleDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_notifications_rationale_title)
                .setMessage(R.string.onboarding_notifications_rationale_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.onboarding_notifications_allow_button,
                        (d, which) -> permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS))
                .show();
    }

    /**
     * Show a dialog that notifies the user to open the app's notification settings to
     * update their permissions.
     */
    private void showOpenSettingsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_notifications_settings_title)
                .setMessage(R.string.onboarding_notifications_settings_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.onboarding_notifications_open_settings, (dialog, which) -> openAppSettings())
                .show();
    }

    /**
     * Opens the app's notification settings using an intent.
     */
    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }

    private void updateButtonTint(boolean enabled) {
        int bg;
        int fg;

        // if enabled -> purplish color
        if (enabled) {
            bg = resolveAttrColor(com.google.android.material.R.attr.colorSecondaryContainer);
            fg = resolveAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer);
        }
        // if error -> red color
        else if (hasRequested) {
            bg = resolveAttrColor(com.google.android.material.R.attr.colorErrorContainer);
            fg = resolveAttrColor(com.google.android.material.R.attr.colorOnErrorContainer);
        }
        // if not enabled -> surface variant color (a sort of gray)
        else {
            bg = resolveAttrColor(com.google.android.material.R.attr.colorSurfaceVariant);
            fg = resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        }

        // update button colors
        button.setBackgroundTintList(ColorStateList.valueOf(bg));
        button.setTextColor(fg);
        button.setIconTint(ColorStateList.valueOf(fg));
    }

    /**
     * Resolve a theme attribute to a color.
     *
     * @param attr The attribute to resolve.
     * @return The resolved color.
     */
    private int resolveAttrColor(int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
