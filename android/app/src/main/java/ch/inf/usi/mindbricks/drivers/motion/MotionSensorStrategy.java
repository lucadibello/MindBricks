package ch.inf.usi.mindbricks.drivers.motion;

import android.hardware.Sensor;

public interface MotionSensorStrategy {
    /**
     * Check if the sensor is available on the device.
     * @return true if available, false otherwise
     */
    boolean isAvailable();

    /**
     * Get the underlying sensor instance.
     * @return the Sensor instance
     */
    Sensor getSensor();

    /**
     * Check if this strategy is a fallback (i.e., not SignificantMotionStrategy).
     * @return True if this is a fallback strategy, false otherwise
     */
    default boolean isFallback() {
        return !(this instanceof SignificantMotionStrategy);
    }
}
