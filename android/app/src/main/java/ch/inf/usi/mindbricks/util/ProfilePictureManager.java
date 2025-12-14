package ch.inf.usi.mindbricks.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
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
                        // permission denied -> show message
                        Snackbar.make(fragment.requireView(),
                                R.string.camera_permission_denied,
                                Snackbar.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Shows a dialog to choose between camera and gallery
     */
    public void showPhotoSourceDialog() {
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.profile_photo_source_title)
                .setItems(new String[]{
                        fragment.getString(R.string.profile_photo_camera),
                        fragment.getString(R.string.profile_photo_gallery)
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
     * FIXME: we should also show the rationale if the user denied first time
     */
    private void launchCamera() {
        // check if user granted permission
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // if granted -> open camera to take photo
            launchCameraIntent();
        } else {
            // if not granted -> request + listen for result using registered callback
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
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
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .centerCrop()
                .into(imageView);
    }

    /**
     * Loads a local profile picture from the given URI
     */
    private void loadLocalProfilePicture(Uri uri) {
        Glide.with(fragment)
                .load(uri)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .centerCrop()
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
