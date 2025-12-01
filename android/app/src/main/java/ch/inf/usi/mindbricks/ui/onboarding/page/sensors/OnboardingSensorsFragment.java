package ch.inf.usi.mindbricks.ui.onboarding.page.sensors;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.drivers.LightSensor;
import ch.inf.usi.mindbricks.drivers.SignificantMotionSensor;
import ch.inf.usi.mindbricks.ui.onboarding.OnboardingStepValidator;
import ch.inf.usi.mindbricks.util.PermissionManager;

public class OnboardingSensorsFragment extends Fragment implements OnboardingStepValidator {

    private OnboardingSensorsViewModel viewModel;

    private PermissionManager.PermissionRequest micPermissionRequest;
    private boolean isMicPermissionDirty = false;

    private View rootView;
    private MaterialButton micButton;
    private MaterialButton lightInfoButton;
    private MaterialButton pickupButton;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // create view model
        viewModel = new ViewModelProvider(
                requireActivity(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(OnboardingSensorsViewModel.class);

        // Create permission manager for microphone
        micPermissionRequest = PermissionManager.registerSinglePermission(
                this, Manifest.permission.RECORD_AUDIO,
                // on granted callback
                () -> {
                    viewModel.setHasRecordingPermission(true);
                    System.out.println("Microphone access granted");
                },
                // on denied callback
                () -> {
                    viewModel.setHasRecordingPermission(false);
                    System.out.println("Microphone access denied");
                    boolean canShowRationale = PermissionManager.shouldShowRationale(requireActivity(), Manifest.permission.RECORD_AUDIO);
                    if (canShowRationale) {
                        showMicRationaleDialog();
                    } else {
                        showMicSettingsDialog();
                    }
                }
        );

        // initialize sensors
        LightSensor.initialize(requireContext());
        SignificantMotionSensor.initialize(requireContext());

        rootView = inflater.inflate(R.layout.fragment_onboarding_sensors, container, false);

        micButton = rootView.findViewById(R.id.buttonEnableMicrophone);
        lightInfoButton = rootView.findViewById(R.id.buttonLightInfo);
        pickupButton = rootView.findViewById(R.id.buttonEnablePickup);
        MaterialButton micInfoButton = rootView.findViewById(R.id.buttonMicBackgroundInfo);

        // setup on click listeners
        micButton.setOnClickListener(v -> requestMicrophoneAccess());
        lightInfoButton.setOnClickListener(v -> showLightInfo());
        pickupButton.setOnClickListener(v -> showPickupInfo());
        micInfoButton.setOnClickListener(v -> showMicRationaleDialog());

        // prime state with current permission + sensors
        boolean hasMicPermission = PermissionManager.hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO);
        viewModel.setHasRecordingPermission(hasMicPermission);

        boolean hasLightSensor = hasLightSensor();
        viewModel.getHasLightSensor().setValue(hasLightSensor);
        if (!hasLightSensor) {
            lightInfoButton.setEnabled(false);
            lightInfoButton.setText(R.string.onboarding_sensors_light_unavailable);
        }

        boolean hasPickupSensor = hasSignificantMotionSensor();
        viewModel.getHasSignificantMotionSensor().setValue(hasPickupSensor);
        if (!hasPickupSensor) {
            pickupButton.setEnabled(false);
            pickupButton.setText(R.string.onboarding_sensors_pickup_unavailable);
        } else if (SignificantMotionSensor.getInstance().isFallback()) {
            // show info if using accelerometer fallback
            pickupButton.setEnabled(false);
            pickupButton.setText(R.string.onboarding_sensors_pickup_fallback_in_use);
        }

        // setup listeners for sensor availability / permissions
        viewModel.getHasRecordingPermission().observe(getViewLifecycleOwner(), hasPermission -> {
            if (hasPermission != null && hasPermission) {
                tintAsEnabled(micButton);
            } else if (isMicPermissionDirty) {
                tintAsError(micButton);
            } else {
                tintAsDefault(micButton);
            }
        });

        viewModel.getHasAcknowledgedPickup().observe(getViewLifecycleOwner(), acknowledged -> {
            if (acknowledged != null && acknowledged) {
                tintAsEnabled(pickupButton);
            } else {
                tintAsDefault(pickupButton);
            }
        });

        // initial tint states
        if (hasMicPermission) {
            tintAsEnabled(micButton);
        } else {
            tintAsDefault(micButton);
        }
        tintAsDefault(pickupButton);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // on resume -> update the sensor state
        // NOTE: this is useful when the user changes the permissions in the app preferences,
        // and then comes back to the app
        refreshSensorState();
    }

    @Override
    public boolean validateStep() {
        Boolean hasMicPermission = viewModel.getHasRecordingPermission().getValue();
        if (hasMicPermission == null || !hasMicPermission) {
            Snackbar.make(rootView, R.string.onboarding_error_microphone_required, Snackbar.LENGTH_SHORT).show();
            return false;
        }

        Boolean hasPickupSensor = viewModel.getHasSignificantMotionSensor().getValue();
        if (hasPickupSensor != null && !hasPickupSensor) {
            Snackbar.make(rootView, R.string.onboarding_sensors_pickup_unavailable, Snackbar.LENGTH_SHORT).show();
        }

        return true;
    }

    private int resolveAttrColor(int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void requestMicrophoneAccess() {
        System.out.println("Requesting microphone access");
        isMicPermissionDirty = true;

        if (PermissionManager.hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
            viewModel.setHasRecordingPermission(true);
            tintAsEnabled(micButton);
            return;
        }

        if (PermissionManager.shouldShowRationale(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
            showMicRationaleDialog();
            return;
        }

        micPermissionRequest.launch();
    }

    /**
     * Refreshes the "allow" buttons colors based on the current state of the permissions.
     */
    private void refreshSensorState() {
        boolean hasMicPermission = PermissionManager.hasPermission(requireContext(), Manifest.permission.RECORD_AUDIO);
        viewModel.setHasRecordingPermission(hasMicPermission);
        applyMicTint(hasMicPermission);

        boolean hasLightSensor = hasLightSensor();
        viewModel.getHasLightSensor().setValue(hasLightSensor);
        if (!hasLightSensor) {
            lightInfoButton.setEnabled(false);
            lightInfoButton.setText(R.string.onboarding_sensors_light_unavailable);
        } else {
            lightInfoButton.setEnabled(true);
            lightInfoButton.setText(R.string.onboarding_enable_light_info);
        }

        boolean hasPickupSensor = hasSignificantMotionSensor();
        viewModel.getHasSignificantMotionSensor().setValue(hasPickupSensor);
        if (!hasPickupSensor) {
            pickupButton.setEnabled(false);
            pickupButton.setText(R.string.onboarding_sensors_pickup_unavailable);
            applyPickupTint(false);
        }
        applyPickupTint(true);
    }

    private boolean hasLightSensor() {
        return LightSensor.getInstance().isAvailable();
    }

    private boolean hasSignificantMotionSensor() {
        return SignificantMotionSensor.getInstance().isAvailable();
    }


    /**
     * Applies the success tint to the button (show success when permission granted)
     * @param button  The button to tint.
     */
    private void tintAsEnabled(MaterialButton button) {
        int bg = resolveAttrColor(com.google.android.material.R.attr.colorPrimaryContainer);
        int fg = resolveAttrColor(com.google.android.material.R.attr.colorOnPrimaryContainer);
        button.setBackgroundTintList(ColorStateList.valueOf(bg));
        button.setTextColor(fg);
        button.setIconTint(ColorStateList.valueOf(fg));
    }

    /**
     * Applies the error tint to the button (show error when permission not granted)
     * @param button  The button to tint.
     */
    private void tintAsError(MaterialButton button) {
        int bg = resolveAttrColor(com.google.android.material.R.attr.colorErrorContainer);
        int fg = resolveAttrColor(com.google.android.material.R.attr.colorOnErrorContainer);
        button.setBackgroundTintList(ColorStateList.valueOf(bg));
        button.setTextColor(fg);
        button.setIconTint(ColorStateList.valueOf(fg));
    }

    /**
     * Applies the default tint to the button (to reset the state)
     * @param button  The button to tint.
     */
    private void tintAsDefault(MaterialButton button) {
        int bg = resolveAttrColor(com.google.android.material.R.attr.colorSurfaceVariant);
        int fg = resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        button.setBackgroundTintList(ColorStateList.valueOf(bg));
        button.setTextColor(fg);
        button.setIconTint(ColorStateList.valueOf(fg));
    }

    /**
     * Applies the light tint to the microphone "allow" button.
     * @param hasPermission  Whether the user has granted the microphone permission.
     */
    private void applyMicTint(Boolean hasPermission) {
        if (hasPermission != null && hasPermission) {
            tintAsEnabled(micButton);
        } else if (isMicPermissionDirty) {
            tintAsError(micButton);
        } else {
            tintAsDefault(micButton);
        }
    }

    private void applyPickupTint(Boolean acknowledged) {
        if (acknowledged != null && acknowledged) {
            tintAsEnabled(pickupButton);
        } else {
            tintAsDefault(pickupButton);
        }
    }

    private void showLightInfo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_sensors_light_title)
                .setMessage(R.string.onboarding_sensors_light_body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showPickupInfo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_sensors_pickup_info_title)
                .setMessage(R.string.onboarding_sensors_pickup_info_body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Shows a dialog that asks the user to grant the microphone permission.
     */
    private void showMicRationaleDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_sensors_microphone_rationale_title)
                .setMessage(R.string.onboarding_sensors_microphone_rationale)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.onboarding_sensors_microphone_allow_button, (d, which) -> micPermissionRequest.launch())
                .show();
    }

    /**
     * Shows a dialog that asks the user to open the app settings to grant the microphone permission.
     * <p>
     * NOTE: happens when the user denied access to mic (initial request + rationale)
     */
    private void showMicSettingsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.onboarding_sensors_microphone_settings_title)
                .setMessage(R.string.onboarding_sensors_microphone_settings_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.onboarding_sensors_microphone_open_settings, (dialog, which) -> openAppSettings())
                .show();
    }

    /**
     * Opens the app settings screen to allow the user to grant the microphone permission.
     */
    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }
}
