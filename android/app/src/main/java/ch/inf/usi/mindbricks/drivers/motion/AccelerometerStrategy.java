package ch.inf.usi.mindbricks.drivers.motion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import ch.inf.usi.mindbricks.drivers.base.BaseSensor;
import ch.inf.usi.mindbricks.drivers.base.MotionListener;

public class AccelerometerStrategy extends BaseSensor implements MotionSensorStrategy, SensorEventListener {
    private final static double SIGNIFICANT_MOTION_THRESHOLD_MS = 2.0;
    private final SensorManager sensorManager;
    private MotionListener listener;

    public AccelerometerStrategy(Context ctx) {
        super(ctx, Sensor.TYPE_ACCELEROMETER);
        this.sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void start(MotionListener listener) {
        this.listener = listener;
        if (isAvailable() && sensorManager != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        this.listener = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // extract acceleration values for each axes
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Compute magnitude of acceleration: mag = sqrt(x^2 + y^2 + z^2)
        double magnitude = Math.sqrt(x * x + y * y + z * z);

        // Check if the current magnitude represents a significant motion (> 2.0 m/s)
        // NOTE: as the accelerometer is subject to gravity, we need to take that into account
        if (Math.abs(magnitude - SensorManager.GRAVITY_EARTH) > SIGNIFICANT_MOTION_THRESHOLD_MS) {
            // signal movement
            if (listener != null) listener.onMotionDetected();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}