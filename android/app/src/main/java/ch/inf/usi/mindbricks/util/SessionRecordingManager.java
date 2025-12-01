package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.drivers.LightSensor;
import ch.inf.usi.mindbricks.drivers.MicrophoneRecorder;
import ch.inf.usi.mindbricks.drivers.SignificantMotionSensor;
import ch.inf.usi.mindbricks.model.SessionSensorLog;
import ch.inf.usi.mindbricks.model.StudySession;

public class SessionRecordingManager {

    // general constants
    private static final String TAG = "SessionRecordingManager";
    private static final int SAMPLE_RATE = 1; // in seconds (1 sample per second)
    private static final int EMPTY_SESSION_ID = -1;

    // References to utility instances
    private final Context context;
    private final MicrophoneRecorder microphoneRecorder;
    private final LightSensor lightSensor;
    private final SignificantMotionSensor motionSensor;
    private final AppDatabase db;
    private ScheduledExecutorService executor;

    // information about current study session
    private long currentSessionId = EMPTY_SESSION_ID;
    private long sessionStartTime;

    // sensor data for current iteration
    private float currentLightLevel = 0;
    private boolean isFaceUp = false;
    // NOTE: volatile as this flag is accessed by different threads at the same time
    private volatile boolean motionDetectedInInterval = false;
    private int totalPickups = 0;
    private List<SessionSensorLog> logBuffer; // list of collected logs

    // sensor listeners - Removed as they are now encapsulated
    // Accelerometer for orientation check - Removed

    public SessionRecordingManager(Context context) {
        // store context
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(context);
        this.microphoneRecorder = new MicrophoneRecorder();

        // Initialize sensor singletons if not already
        LightSensor.initialize(context);
        SignificantMotionSensor.initialize(context);

        // Store light sensor and motion sensor instances
        this.lightSensor = LightSensor.getInstance();
        this.motionSensor = SignificantMotionSensor.getInstance();
    }

    public void startSession(long sessionId) {
        Log.d(TAG, "Starting new session with ID: " + sessionId);

        // store basic information about current study session
        this.currentSessionId = sessionId;
        this.sessionStartTime = System.currentTimeMillis();
        this.logBuffer = new ArrayList<>();

        // clear counters / state flags
        this.totalPickups = 0;
        this.motionDetectedInInterval = false;

        // start recording noise levels using microphone
        try {
            microphoneRecorder.startRecording();
            Log.d(TAG, "Microphone recording started.");
        } catch (SecurityException e) {
            Log.e(TAG, "Microphone permission missing", e);
        }

        // set up listener for significant motions
        motionSensor.setListener(() -> {
            motionDetectedInInterval = true;
            totalPickups++; // Naive total count
        });

        // start sensor monitoring
        // a) motion sensor
        motionSensor.start();
        Log.d(TAG, "Motion sensor monitoring started.");
        // b) light sensor (which handles orientation internally)
        startLightMonitoring();

        // Start sampling loop (one sample per second)
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::sampleSensors, SAMPLE_RATE, SAMPLE_RATE, TimeUnit.SECONDS);
    }

    /**
     * Stops a running study session, stopping all sensors and recording. Furthermore, all generated
     * study session logs are saved to the database for further analysis.
     * @param session Study session to update with statistics about the environment of the session.
     */
    public void stopSession(StudySession session) {
        Log.d(TAG, "Stopping session with ID: " + currentSessionId);
        // shutdown + signal Java Garbage Collector to clean up executor
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }

        // stop all sensors
        microphoneRecorder.stopRecording();
        motionSensor.stop();
        stopLightMonitoring();
        Log.d(TAG, "All sensors stopped.");

        // Calculate averages and update session
        float sumNoise = 0;
        float sumLight = 0;
        int count = logBuffer.size();

        // compute avg. noise level / light level
        for (SessionSensorLog log : logBuffer) {
            sumNoise += log.getNoiseLevel();
            sumLight += log.getLightLevel();
        }
        if (count > 0) {
            session.setAvgNoiseLevel(sumNoise / count);
            session.setAvgLightLevel(sumLight / count);
        }

        // store # of phone pick ups
        session.setPhonePickupCount(totalPickups);

        // Store study logs to db
        new Thread(() -> {
            db.sessionSensorLogDao().insertAll(logBuffer);
            db.studySessionDao().update(session); // Update the main session with stats
        }).start();

        // Set empty id to signal that the session is stopped
        currentSessionId = EMPTY_SESSION_ID;
    }

    private void startLightMonitoring() {
        Log.d(TAG, "Starting light monitoring.");
        lightSensor.start((lightLevel, isFaceUp) -> {
            this.currentLightLevel = lightLevel;
            this.isFaceUp = isFaceUp;
        });
    }

    /**
     * Cleanup sensor listeners and unregister them from SensorManager.
     */
    private void stopLightMonitoring() {
        Log.d(TAG, "Stopping light monitoring.");
        lightSensor.stop();
    }

    /**
     * This method samples the light sensor, microphone and whether the phone has been
     * picked-up by the user during the interval, creating a log entry for this time step.
     */
    private void sampleSensors() {
        if (currentSessionId == EMPTY_SESSION_ID) return;

        // read noise level from recorder
        float noise = (float) microphoneRecorder.getCurrentAmplitude();

        // Create + record log entry for this time step
        SessionSensorLog log = new SessionSensorLog(
                currentSessionId,
                System.currentTimeMillis(),
                noise,
                currentLightLevel,
                motionDetectedInInterval,
                isFaceUp
        );
        logBuffer.add(log);

        // Log everything for debugging
        Log.v(TAG, String.format("Sampled - Noise: %.2f, Light: %.2f, Motion: %b, FaceUp: %b",
                noise, currentLightLevel, motionDetectedInInterval, isFaceUp));
        
        // Reset interval flags to force recalculation during next step
        motionDetectedInInterval = false;
    }
}
