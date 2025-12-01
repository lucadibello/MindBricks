package ch.inf.usi.mindbricks.drivers.base;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class BaseSensor {

    private final Sensor sensor;
    private final boolean isSensorAvailable;

    protected BaseSensor(Context ctx, int sensorType) {
        SensorManager sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            throw new IllegalStateException("SensorManager not available");
        }
        sensor = sensorManager.getDefaultSensor(sensorType);
        isSensorAvailable = (sensor != null);
    }

    public Sensor getSensor() {
        return sensor;
    }

    public boolean isAvailable() {
        return isSensorAvailable;
    }
}
