package ch.inf.usi.mindbricks.drivers;

import android.content.Context;
import android.util.Log;

import ch.inf.usi.mindbricks.drivers.base.MotionListener;
import ch.inf.usi.mindbricks.drivers.motion.AccelerometerStrategy;
import ch.inf.usi.mindbricks.drivers.motion.MotionSensorStrategy;
import ch.inf.usi.mindbricks.drivers.motion.SignificantMotionStrategy;

/**
 * This class allows to detect significant motions (i.e. user picks-up the phone).
 * If the current device does not support Significant Motion, it will fall back to
 * Accelerometer (simpler logic: if the recorded acceleration magnitude is greater than a given
 * threshold, it is considered as a significant motion).
 */
public class SignificantMotionSensor {

    private static final String TAG = "SignificantMotionSensor";
    private static SignificantMotionSensor INSTANCE;

    private final MotionSensorStrategy strategy;
    private MotionListener listener;

    private SignificantMotionSensor(Context ctx) {
        MotionSensorStrategy significantMotion = new SignificantMotionStrategy(ctx);
        if (significantMotion.isAvailable()) {
            strategy = significantMotion;
            Log.d(TAG, "Using Significant Motion Sensor");
        } else {
            strategy = new AccelerometerStrategy(ctx);
            Log.d(TAG, "Using Accelerometer Fallback for Motion");
        }
    }

    public static synchronized void initialize(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new SignificantMotionSensor(ctx.getApplicationContext());
        }
    }

    public static SignificantMotionSensor getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SignificantMotionSensor not initialized. Call initialize() first.");
        }
        return INSTANCE;
    }

    public boolean isFallback() {
        return strategy.isFallback();
    }

    public boolean isAvailable() {
        return strategy.isAvailable();
    }

    public void setListener(MotionListener listener) {
        this.listener = listener;
    }

    public void start() {
        strategy.start(listener);
    }

    public void stop() {
        strategy.stop();
    }
}
