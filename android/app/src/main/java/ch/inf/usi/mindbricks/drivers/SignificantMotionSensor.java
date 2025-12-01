package ch.inf.usi.mindbricks.drivers;

import android.content.Context;
import android.hardware.Sensor;

import ch.inf.usi.mindbricks.drivers.base.BaseSensor;
import ch.inf.usi.mindbricks.drivers.motion.AccelerometerStrategy;
import ch.inf.usi.mindbricks.drivers.motion.MotionSensorStrategy;
import ch.inf.usi.mindbricks.drivers.motion.SignificantMotionStrategy;

/**
 *
 */
public class SignificantMotionSensor {

    private static SignificantMotionSensor INSTANCE;

    private final MotionSensorStrategy strategy;

    private SignificantMotionSensor(Context ctx) {
        MotionSensorStrategy significantMotion = new SignificantMotionStrategy(ctx);
        if (significantMotion.isAvailable()) {
            strategy = significantMotion;
        } else {
            strategy = new AccelerometerStrategy(ctx);
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

    public boolean isAvailable() {
        return strategy.isAvailable();
    }

    public boolean isFallback() {
        return strategy.isFallback();
    }

    public Sensor getSensor() {
        return strategy.getSensor();
    }

    public boolean isUsingAccelerometerFallback() {
        return strategy instanceof AccelerometerStrategy;
    }
}
