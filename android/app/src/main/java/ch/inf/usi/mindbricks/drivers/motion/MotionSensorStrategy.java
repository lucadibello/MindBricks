package ch.inf.usi.mindbricks.drivers.motion;

import ch.inf.usi.mindbricks.drivers.base.MotionListener;

public interface MotionSensorStrategy {
    /**
     * Check if the sensor is available on the device.
     *
     * @return true if available, false otherwise
     */
    boolean isAvailable();

    /**
     * Check if this strategy is a fallback (i.e., not SignificantMotionStrategy).
     *
     * @return True if this is a fallback strategy, false otherwise
     */
    default boolean isFallback() {
        return !(this instanceof SignificantMotionStrategy);
    }

    /**
     * Start monitoring for motion.
     *
     * @param listener The listener to notify when motion is detected.
     */
    void start(MotionListener listener);

    /**
     * Stop monitoring for motion.
     */
    void stop();
}