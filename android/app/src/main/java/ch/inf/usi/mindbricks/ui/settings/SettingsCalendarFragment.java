package ch.inf.usi.mindbricks.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.drivers.calendar.CalendarDriver;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarSyncService;
import ch.inf.usi.mindbricks.ui.settings.dialog.CalendarPermissionDialog;


/**
 * Settings fragment for managing calendar integrations.
 *
 */
public class SettingsCalendarFragment extends Fragment {

    private CalendarSyncService syncService;

    // UI Elements
    private MaterialTextView textDeviceStatus;
    private MaterialButton buttonDeviceCalendar;
    private MaterialTextView textEventCount;
    private MaterialTextView textLastSync;
    private MaterialButton buttonSyncNow;
    private LinearProgressIndicator progressSync;

    // Permission launcher
    private ActivityResultLauncher<String> calendarPermissionLauncher;

    // Settings launcher
    private ActivityResultLauncher<Intent> settingsLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        syncService = CalendarSyncService.getInstance(requireContext());

        calendarPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::onPermissionResult
        );

        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Check if permission was granted in settings
                    if (hasCalendarPermission()) {
                        onDeviceCalendarConnected();
                    }
                    updateUI();
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_calendar_sync, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        textDeviceStatus = view.findViewById(R.id.textDeviceStatus);
        buttonDeviceCalendar = view.findViewById(R.id.buttonDeviceCalendar);
        textEventCount = view.findViewById(R.id.textEventCount);
        textLastSync = view.findViewById(R.id.textLastSync);
        buttonSyncNow = view.findViewById(R.id.buttonSyncNow);
        progressSync = view.findViewById(R.id.progressSync);

        // Set up click listeners
        buttonDeviceCalendar.setOnClickListener(v -> handleDeviceCalendarClick());
        buttonSyncNow.setOnClickListener(v -> handleSyncNowClick());

        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private boolean hasCalendarPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void onPermissionResult(boolean granted) {
        if (granted) {
            // Permission granted!
            onDeviceCalendarConnected();
        } else {
            // Permission denied - check if we can ask again
            boolean canAskAgain = shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR);

            if (canAskAgain) {
                // User denied but didn't select "Don't ask again"
                Toast.makeText(requireContext(),
                        R.string.settings_calendar_permission_denied,
                        Toast.LENGTH_LONG).show();
            } else {
                // User selected "Don't ask again" - must go to settings
                showGoToSettingsDialog();
            }
        }
        updateUI();
    }


    private void handleDeviceCalendarClick() {
        if (syncService.isDriverAuthenticated("device")) {
            // Already connected - show disconnect dialog
            showDisconnectDialog("device", getString(R.string.settings_calendar_device));
        } else if (hasCalendarPermission()) {
            // Permission already granted
            onDeviceCalendarConnected();
        } else {
            // Need to request permission - show our rationale dialog first
            showDeviceCalendarPermissionDialog();
        }
    }

    /**
     * Shows our custom rationale dialog explaining why we need calendar access.
     */
    private void showDeviceCalendarPermissionDialog() {
        CalendarPermissionDialog.show(
                getParentFragmentManager(),
                CalendarPermissionDialog.CalendarType.DEVICE,
                new CalendarPermissionDialog.PermissionCallback() {
                    @Override
                    public void onPermissionAccepted() {
                        // User clicked "Allow Access" - now request the actual system permission
                        requestCalendarPermission();
                    }

                    @Override
                    public void onPermissionDeclined() {
                        // User clicked "Not Now"
                        Toast.makeText(requireContext(),
                                R.string.permission_calendar_declined_message,
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void requestCalendarPermission() {
        // Always try to launch the permission request
        // The system will either show the dialog or immediately deny
        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR);
    }

    private void showGoToSettingsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.permission_settings_title)
                .setMessage(R.string.permission_settings_message)
                .setPositiveButton(R.string.permission_settings_button, (dialog, which) -> {
                    openAppSettings();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        settingsLauncher.launch(intent);
    }

    private void onDeviceCalendarConnected() {
        Toast.makeText(requireContext(),
                R.string.settings_calendar_device_connected,
                Toast.LENGTH_SHORT).show();
        updateUI();
        triggerSync("device");
    }

    private void showDisconnectDialog(String sourceName, String displayName) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_calendar_disconnect_title)
                .setMessage(getString(R.string.settings_calendar_disconnect_message, displayName))
                .setPositiveButton(R.string.settings_calendar_disconnect, (dialog, which) -> {
                    syncService.disconnectDriver(sourceName);
                    Toast.makeText(requireContext(),
                            getString(R.string.settings_calendar_disconnected, displayName),
                            Toast.LENGTH_SHORT).show();
                    updateUI();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void handleSyncNowClick() {
        List<CalendarDriver> authenticated = syncService.getAuthenticatedDrivers();
        if (authenticated.isEmpty()) {
            Toast.makeText(requireContext(),
                    R.string.settings_calendar_no_calendars,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressSync.setVisibility(View.VISIBLE);
        buttonSyncNow.setEnabled(false);

        syncService.syncAll(result -> {
            requireActivity().runOnUiThread(() -> {
                progressSync.setVisibility(View.GONE);
                buttonSyncNow.setEnabled(true);

                if (result.hasErrors()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.settings_calendar_sync_partial,
                                    result.totalEvents, result.errors.size()),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.settings_calendar_sync_success, result.totalEvents),
                            Toast.LENGTH_SHORT).show();
                }

                updateUI();
            });
        });
    }

    private void triggerSync(String sourceName) {
        progressSync.setVisibility(View.VISIBLE);
        buttonSyncNow.setEnabled(false);

        syncService.syncDriver(sourceName, result -> {
            requireActivity().runOnUiThread(() -> {
                progressSync.setVisibility(View.GONE);
                buttonSyncNow.setEnabled(true);
                updateUI();
            });
        });
    }

    private void updateUI() {
        if (!isAdded() || getContext() == null) return;

        // Update Device Calendar status
        updateDriverUI(
                "device",
                textDeviceStatus,
                buttonDeviceCalendar
        );

        // Update sync info
        updateSyncInfo();
    }

    private void updateDriverUI(String sourceName, MaterialTextView statusText, MaterialButton button) {
        List<CalendarSyncService.DriverInfo> drivers = syncService.getDriverInfoList();

        for (CalendarSyncService.DriverInfo info : drivers) {
            if (info.sourceName.equals(sourceName)) {
                if (info.isConnected) {
                    // Connected state
                    String statusStr;
                    statusStr = getString(R.string.settings_calendar_connected);

                    if (info.lastSyncTime > 0) {
                        statusStr += " • " + info.getLastSyncTimeString();
                    }

                    statusText.setText(statusStr);
                    statusText.setTextColor(ContextCompat.getColor(requireContext(),
                            R.color.analytics_text_primary));
                    button.setText(R.string.settings_calendar_disconnect);
                } else {
                    // Not connected state
                    statusText.setText(R.string.settings_calendar_not_connected);
                    statusText.setTextColor(ContextCompat.getColor(requireContext(),
                            android.R.color.darker_gray));
                    button.setText(R.string.settings_calendar_connect);
                }
                break;
            }
        }
    }

    private void updateSyncInfo() {
        Executors.newSingleThreadExecutor().execute(() -> {
            int eventCount = syncService.getRepository().getTotalEventCountSync();

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;

                textEventCount.setText(String.valueOf(eventCount));

                // Find most recent sync time across all sources
                long mostRecentSync = 0;
                List<CalendarSyncService.DriverInfo> drivers = syncService.getDriverInfoList();
                for (CalendarSyncService.DriverInfo info : drivers) {
                    if (info.lastSyncTime > mostRecentSync) {
                        mostRecentSync = info.lastSyncTime;
                    }
                }

                if (mostRecentSync > 0) {
                    long ageMs = System.currentTimeMillis() - mostRecentSync;
                    String ageStr = formatSyncAge(ageMs);
                    textLastSync.setText(getString(R.string.settings_calendar_last_sync, ageStr));
                } else {
                    textLastSync.setText(R.string.settings_calendar_last_sync_never);
                }
            });
        });
    }

    private String formatSyncAge(long ageMs) {
        long ageMinutes = ageMs / (1000 * 60);

        if (ageMinutes < 1) return getString(R.string.settings_calendar_just_now);
        if (ageMinutes < 60) return getString(R.string.settings_calendar_minutes_ago, (int) ageMinutes);

        long ageHours = ageMinutes / 60;
        if (ageHours < 24) return getString(R.string.settings_calendar_hours_ago, (int) ageHours);

        long ageDays = ageHours / 24;
        return getString(R.string.settings_calendar_days_ago, (int) ageDays);
    }
}