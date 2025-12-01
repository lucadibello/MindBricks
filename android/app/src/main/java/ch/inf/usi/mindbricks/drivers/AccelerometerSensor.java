package ch.inf.usi.mindbricks.drivers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import ch.inf.usi.mindbricks.drivers.base.BaseSensor;
import ch.inf.usi.mindbricks.drivers.base.OrientationListener;

public class AccelerometerSensor extends BaseSensor implements SensorEventListener {

    private static final String TAG = "AccelerometerSensor";
    private static final double FACE_UP_THRESHOLD = 2f;
    private static AccelerometerSensor INSTANCE;
    private final SensorManager sensorManager;
    private OrientationListener listener;

    private AccelerometerSensor(Context ctx) {
        super(ctx, Sensor.TYPE_ACCELEROMETER);
        this.sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
    }

    public static synchronized void initialize(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new AccelerometerSensor(ctx.getApplicationContext());
        }
    }

    public static AccelerometerSensor getInstance(Context ctx) {
        if (INSTANCE == null) {
            initialize(ctx);
        }
        return INSTANCE;
    }

    public void startOrientationMonitoring(OrientationListener listener) {
        this.listener = listener;
        if (isAvailable() && sensorManager != null) {
            Log.d(TAG, "Starting Orientation monitoring");
            sensorManager.registerListener(this, getSensor(), SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopOrientationMonitoring() {
        if (sensorManager != null) {
            Log.d(TAG, "Stopping Orientation monitoring");
            sensorManager.unregisterListener(this);
        }
        this.listener = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float z = event.values[2];
        // trivial check:
        // - when phone facing up: z ~= gravity (9.81)
        // - if z is in range: [gravity - THRESHOLD; gravity + THRESHOLD] then we know it's facing upward!
        boolean isFaceUp = z > SensorManager.GRAVITY_EARTH - FACE_UP_THRESHOLD
                && z < SensorManager.GRAVITY_EARTH + FACE_UP_THRESHOLD;

        // notify listener if found
        if (listener != null) {
            listener.onOrientationChanged(isFaceUp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
