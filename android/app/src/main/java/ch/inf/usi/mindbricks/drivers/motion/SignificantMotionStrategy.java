package ch.inf.usi.mindbricks.drivers.motion;

import android.content.Context;
import android.hardware.Sensor;

import ch.inf.usi.mindbricks.drivers.base.BaseSensor;

public class SignificantMotionStrategy extends BaseSensor implements MotionSensorStrategy {
    public SignificantMotionStrategy(Context ctx) {
        super(ctx, Sensor.TYPE_SIGNIFICANT_MOTION);
    }
}
