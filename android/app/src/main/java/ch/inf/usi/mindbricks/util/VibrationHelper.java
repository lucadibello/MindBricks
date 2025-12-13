package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public final class VibrationHelper {

    private VibrationHelper() {
    }

    public enum VibrationType {
        SESSION_START,
        SESSION_END,
        SESSION_CANCELLED,
        CYCLE_COMPLETE
    }

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
            e.printStackTrace();
        }
    }

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
        };
    }
}
