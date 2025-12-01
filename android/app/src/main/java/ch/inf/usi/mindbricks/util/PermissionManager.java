package ch.inf.usi.mindbricks.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Static helpers + small wrapper for single-permission requests.
 */
public final class PermissionManager {

    private PermissionManager() {
        // NOTE: do not allow construction
    }

    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean shouldShowRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * Register a single-permission request with callbacks.
     */
    public static PermissionRequest registerSinglePermission(
            ActivityResultCaller caller,
            String permission,
            Runnable onGranted,
            Runnable onDenied
    ) {
        // create launcher to request permissions
        ActivityResultLauncher<String> launcher =
                caller.registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if (isGranted) {
                                if (onGranted != null) onGranted.run();
                            } else {
                                if (onDenied != null) onDenied.run();
                            }
                        }
                );

        // create callback wrapper + return it
        return new PermissionRequest(permission, launcher);
    }

    /**
     * Wrapper for a single-permission request.
     */
    public static final class PermissionRequest {
        private final String permission;
        private final ActivityResultLauncher<String> launcher;

        private PermissionRequest(
                String permission,
                ActivityResultLauncher<String> launcher
        ) {
            this.permission = permission;
            this.launcher = launcher;
        }

        // FIXME: maybe this class is useless. We may remove it in the future!

        /**
         * Launch the permission dialog without any checks.
         */
        public void launch() {
            launcher.launch(permission);
        }
    }
}
