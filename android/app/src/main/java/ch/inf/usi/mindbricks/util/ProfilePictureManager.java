package ch.inf.usi.mindbricks.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;

import ch.inf.usi.mindbricks.R;

/**
 * Utility class to manage profile picture (selection, camera, and display).
 * NOTE: Handles both custom photos and DiceBear avatars.
 */
public class ProfilePictureManager {

    private static final String DICEBEAR_BASE_URL = "https://api.dicebear.com/9.x/pixel-art/png";
    private final Fragment fragment;
    private final ImageView imageView;
    private final PreferencesManager prefs;
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;
    private final ActivityResultLauncher<Uri> cameraLauncher;
    private final ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri pendingCameraUri;

    /**
     * Creates a new ProfilePictureManager
     *
     * @param fragment The fragment where the profile picture will be loaded
     * @param imageView The ImageView to display the profile picture
     * @param prefs PreferencesManager instance used to store the profile picture data in the app. preferences
     */
    public ProfilePictureManager(Fragment fragment, ImageView imageView, PreferencesManager prefs) {
        this.fragment = fragment;
        this.imageView = imageView;
        this.prefs = prefs;

        // register photo picker launcher
        this.photoPickerLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        saveAndLoadProfilePicture(uri);
                    }
                }
        );

        // register camera launcher
        this.cameraLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && pendingCameraUri != null) {
                        saveAndLoadProfilePicture(pendingCameraUri);
                    }
                    pendingCameraUri = null;
                }
        );

        // register permission listener for fragment
        this.cameraPermissionLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // permission granted -> launch camera
                        launchCameraIntent();
                    } else {
                        // permission denied -> check if we should show rationale
                        handlePermissionDenied();
                    }
                }
        );
    }

    /**
     * Shows a dialog to choose between camera and gallery
     */
    public void showPhotoSourceDialog() {
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.profile_add_picture_dialog_title)
                .setItems(new String[]{
                        fragment.getString(R.string.profile_add_picture_dialog_camera_option_title),
                        fragment.getString(R.string.profile_add_picture_dialog_camera_option_gallery)
                }, (dialog, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else {
                        launchGallery();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Checks camera permission and launches camera if granted
     */
    private void launchCamera() {
        // check if user granted permission
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // if granted -> open camera to take photo
            launchCameraIntent();
        } else {
            // check if we should show rationale
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    fragment.requireActivity(), Manifest.permission.CAMERA)) {
                // user denied before, show explanation
                showPermissionRationale();
            } else {
                // first time or "don't ask again" - request permission
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        }
    }

    /**
     * Shows a dialog explaining why camera permission is needed
     */
    private void showPermissionRationale() {
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.profile_add_picture_camera_permission_rationale_dialog_title)
                .setMessage(R.string.profile_add_picture_camera_permission_rationale_message)
                .setPositiveButton(R.string.profile_add_picture_camera_permission_grant_button_title, (dialog, which) -> {
                    // user wants to give permission now -> retry
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Handles permission denial - either shows message or directs to settings
     */
    private void handlePermissionDenied() {
        // check if user selected "don't ask again"
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                fragment.requireActivity(), Manifest.permission.CAMERA)) {
            // user denied with "don't ask again" - direct to settings
            showSettingsDialog();
        } else {
            // simple denial - show message
            Snackbar.make(fragment.requireView(),
                    R.string.profile_add_picture_camera_permission_denied_first_time_message,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a dialog allowing the user to open app settings to change camera permissions
     */
    private void showSettingsDialog() {
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.profile_add_picture_camera_permission_required_title)
                .setMessage(R.string.profile_add_picture_camera_permission_denied_second_time_message)
                .setPositiveButton(R.string.profile_add_picture_camera_permission_denied_second_time_message, (dialog, which) -> {
                    // open app settings -> let user change permissions manually
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", fragment.requireContext().getPackageName(), null);
                    intent.setData(uri);
                    fragment.startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Launches the device camera to take a photo
     */
    private void launchCameraIntent() {
        try {
            Context context = fragment.requireContext();
            File photoFile = File.createTempFile(
                    "profile_",
                    ".jpg",
                    context.getCacheDir()
            );
            pendingCameraUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    photoFile
            );
            cameraLauncher.launch(pendingCameraUri);
        } catch (IOException e) {
            Log.e("ProfilePictureManager", "IOException: " + e.getMessage());
        }
    }

    /**
     * Launches the photo picker to select from gallery
     */
    private void launchGallery() {
        photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    /**
     * Saves the URI and loads the profile picture
     */
    private void saveAndLoadProfilePicture(Uri uri) {
        // Source: https://developer.android.com/training/data-storage/shared/documents-files#persist-permissions
        try {
            // persist permission to access the URI -> we'll load it later
            fragment.requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException e) {
            Log.e("ProfilePictureManager", "SecurityException: " + e.getMessage());
        }

        // save URI and load the selected image
        prefs.setUserAvatarUri(uri.toString());
        loadLocalProfilePicture(uri);
    }

    /**
     * Loads the profile picture, preferring custom photo over DiceBear avatar
     */
    public void loadProfilePicture() {
        String avatarUri = prefs.getUserAvatarUri();
        if (avatarUri != null && !avatarUri.isEmpty()) {
            loadLocalProfilePicture(Uri.parse(avatarUri));
        } else {
            String seed = prefs.getUserAvatarSeed();
            if (seed == null || seed.isEmpty()) {
                seed = generateUniqueSeed();
                prefs.setUserAvatarSeed(seed);
            }
            loadDiceBearAvatar(seed);
        }
    }

    /**
     * Removes the custom photo and generates a new DiceBear avatar
     */
    public void regenerateDiceBearAvatar() {
        prefs.setUserAvatarUri(null);
        String seed = generateUniqueSeed();
        prefs.setUserAvatarSeed(seed);
        loadDiceBearAvatar(seed);
    }

    /**
     * Loads a DiceBear avatar with the given seed
     */
    private void loadDiceBearAvatar(String seed) {
        Uri avatarUri = Uri.parse(DICEBEAR_BASE_URL)
                .buildUpon()
                .appendQueryParameter("seed", seed)
                .build();

        Glide.with(fragment)
                .load(avatarUri)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(imageView);
    }

    /**
     * Loads a local profile picture from the given URI
     */
    private void loadLocalProfilePicture(Uri uri) {
        Glide.with(fragment)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(imageView);
    }

    /**
     * Generates a unique random seed for DiceBear avatars
     */
    private String generateUniqueSeed() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append((char) ('a' + (int) (Math.random() * 26)));
        }
        return "mindbricks-" + sb;
    }
}
