package ch.inf.usi.mindbricks.drivers;

import android.content.Context;
import android.hardware.Sensor;

import ch.inf.usi.mindbricks.drivers.base.BaseSensor;

public class LightSensor extends BaseSensor {

    private static LightSensor INSTANCE;

    private LightSensor(Context ctx) {
        super(ctx, Sensor.TYPE_LIGHT);
    }

    public static synchronized void initialize(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new LightSensor(ctx.getApplicationContext());
        }
    }

    public static LightSensor getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("LightSensor instance not initialized. Call initialize() first.");
        }
        return INSTANCE;
    }
}
