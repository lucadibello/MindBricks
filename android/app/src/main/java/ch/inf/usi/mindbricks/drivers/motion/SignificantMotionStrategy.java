package ch.inf.usi.mindbricks.drivers.motion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.util.Log;

import ch.inf.usi.mindbricks.drivers.base.BaseSensor;
import ch.inf.usi.mindbricks.drivers.base.MotionListener;

public class SignificantMotionStrategy extends BaseSensor implements MotionSensorStrategy {
    private static final String TAG = "SignificantMotionStrategy";
    private final SensorManager sensorManager;
    private TriggerEventListener triggerListener;
    private MotionListener listener;

    public SignificantMotionStrategy(Context ctx) {
        super(ctx, Sensor.TYPE_SIGNIFICANT_MOTION);
        this.sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void start(MotionListener listener) {
        this.listener = listener;
        if (isAvailable() && sensorManager != null) {
            triggerListener = new TriggerEventListener() {
                @Override
                public void onTrigger(TriggerEvent event) {
                    Log.d(TAG, "Significant Motion Triggered");

                    // NOTE: access SignificantMotion variables to get the configured listener
                    MotionListener currentListener = SignificantMotionStrategy.this.listener;
                    if (currentListener != null) {
                        currentListener.onMotionDetected();
                    }

                    // Re-request trigger (significant motion = trigger once and stops)
                    sensorManager.requestTriggerSensor(this, sensor);
                }
            };
            // start the trigger listener listener by requesting the sensor the first time
            sensorManager.requestTriggerSensor(triggerListener, sensor);
        }
    }

    @Override
    public void stop() {
        // remove sensor from trigger listener
        if (sensorManager != null && triggerListener != null) {
            sensorManager.cancelTriggerSensor(triggerListener, sensor);
        }
        // signal GC to clean up
        triggerListener = null;
        listener = null;
    }
}