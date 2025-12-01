package ch.inf.usi.mindbricks.drivers.motion;

import android.content.Context;
import android.hardware.Sensor;

import ch.inf.usi.mindbricks.drivers.base.BaseSensor;

public class AccelerometerStrategy extends BaseSensor implements MotionSensorStrategy{
    public AccelerometerStrategy(Context ctx) {
        super(ctx, Sensor.TYPE_ACCELEROMETER);
    }
}
