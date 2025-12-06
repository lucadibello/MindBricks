package ch.inf.usi.mindbricks.drivers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import ch.inf.usi.mindbricks.drivers.base.BaseSensor;

public class LightSensor extends BaseSensor implements SensorEventListener {

    private static final String TAG = "LightSensor";
    private static LightSensor INSTANCE;
    private final SensorManager sensorManager;

    private final AccelerometerSensor accelerometerSensor;
    private boolean isFaceUp = false;
    private float currentLightLevel = 0;

    private LightSensorListener listener;

    private LightSensor(Context ctx) {
        super(ctx, Sensor.TYPE_LIGHT);
        this.sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometerSensor = AccelerometerSensor.getInstance(ctx);
    }

    public static synchronized void initialize(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new LightSensor(ctx.getApplicationContext());
        }
        AccelerometerSensor.initialize(ctx);
    }

    public static LightSensor getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("LightSensor instance not initialized. Call initialize() first.");
        }
        return INSTANCE;
    }

    public void start(LightSensorListener listener) {
        this.listener = listener;
        if (isAvailable() && sensorManager != null) {
            Log.d(TAG, "Starting LightSensor monitoring");

            // register light sensor listener
            sensorManager.registerListener(this, getSensor(), SensorManager.SENSOR_DELAY_NORMAL);

            // start accelerometer sensor orientation monitoring service
            accelerometerSensor.startOrientationMonitoring(isFaceUp -> {
                // on orientation change: update internal flag + notify listener
                this.isFaceUp = isFaceUp;
                notifyListener();
            });
        }
    }

    public void stop() {
        if (sensorManager != null) {
            Log.d(TAG, "Stopping LightSensor monitoring");
            sensorManager.unregisterListener(this);
        }
        if (accelerometerSensor != null) {
            accelerometerSensor.stopOrientationMonitoring();
        }
        this.listener = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Read raw light level from sensor
        float rawLightLevel = event.values[0];
        
        // Normalize to 0-100
        float maxRange = getSensor().getMaximumRange();
        if (maxRange == 0) maxRange = 40000f; // Safety fallback
        float normalizedLight = Math.min(100f, (rawLightLevel / maxRange) * 100f);

        // Logic: record light state only when screen is facing upwards.
        if (isFaceUp) {
            currentLightLevel = normalizedLight;
            Log.v(TAG, "Light sensor (face up): " + currentLightLevel);
        } else {
            // When face down, keep the last valid value (do not reset to 0)
            Log.v(TAG, "Light sensor (face down): " + currentLightLevel);
        }

        // notify parent
        notifyListener();
    }

    /**
     * Notify listener about current light level and phone orientation (face up / face down)
     */
    private void notifyListener() {
        if (listener != null) {
            listener.onLightLevelChanged(currentLightLevel, isFaceUp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public interface LightSensorListener {
        void onLightLevelChanged(float lightLevel, boolean isFaceUp);
    }
}