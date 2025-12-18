package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

/**
 * Helper class for handling vibration effects.
 *
 * @author Luca Di Bello
 */
public final class VibrationHelper {

    /**
     * Private constructor to prevent instantiation.
     */
    private VibrationHelper() {
    }

    /**
     * Types of vibration effects available.
     */
    public enum VibrationType {
        SESSION_START,
        SESSION_END,
        SESSION_CANCELLED,
        CYCLE_COMPLETE,
        DESTROY_TILE,
        PLACE_TILE
    }

    /**
     * Activates the selected vibration effect.
     *
     * @param context the application context
     * @param type    the type of vibration effect to activate
     */
    public static void vibrate(Context context, VibrationType type) {
        if (context == null) {
            return;
        }

        // get vibrator service
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        try {
            // activate vibration with the selected effect
            VibrationEffect effect = getVibrationEffect(type);
            vibrator.vibrate(effect);
        } catch (Exception e) {
            // FIXME: for debugging, we need to remove this later on
            Log.e("VibrationHelper", "Error while vibrating", e);
        }
    }

    /**
     * Returns the appropriate vibration effect based on the provided type.
     *
     * @param type the type of vibration effect
     * @return the corresponding VibrationEffect
     */
    private static VibrationEffect getVibrationEffect(VibrationType type) {
        return switch (type) {
            case SESSION_START ->
                // Short, single vibration
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE);
            case SESSION_END ->
                // Medium vibration
                VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE);
            case SESSION_CANCELLED ->
                // Double tap pattern
                VibrationEffect.createWaveform(
                    new long[]{0, 100, 100, 100},
                    new int[]{0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE},
                    -1
                );
            case CYCLE_COMPLETE ->
                // Long celebratory vibration
                VibrationEffect.createWaveform(
                    new long[]{0, 200, 100, 200, 100, 400},
                    new int[]{0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE},
                    -1
                );
            case DESTROY_TILE ->
                // Medium vibration
                VibrationEffect.createOneShot(400, VibrationEffect.EFFECT_HEAVY_CLICK);
            case PLACE_TILE ->
                // Medium vibration
                VibrationEffect.createOneShot(400, VibrationEffect.EFFECT_CLICK);
        };
    }
}
