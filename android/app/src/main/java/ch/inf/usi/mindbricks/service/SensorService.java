package ch.inf.usi.mindbricks.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.MainActivity;
import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.drivers.AccelerometerSensor;
import ch.inf.usi.mindbricks.drivers.LightSensor;
import ch.inf.usi.mindbricks.drivers.MicrophoneRecorder;
import ch.inf.usi.mindbricks.drivers.SignificantMotionSensor;
import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;

public class SensorService extends Service {

    // strings to signal to start / stop the service via intents
    public static final String ACTION_START_SESSION = "ch.inf.usi.mindbricks.service.START_SESSION";
    public static final String ACTION_STOP_SESSION = "ch.inf.usi.mindbricks.service.STOP_SESSION";

    public static final String EXTRA_SESSION_ID = "session_id";
    private static final String TAG = "SensorService";
    private static final String CHANNEL_ID = "SensorServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long LOG_INTERVAL_MS = 5000;

    private long currentSessionId = -1;
    private boolean isRunning = false;

    // Sensors
    private MicrophoneRecorder microphoneRecorder;
    private LightSensor lightSensor;
    private SignificantMotionSensor significantMotionSensor;

    // Current sensor values
    private volatile float lastLightLevel = 0f;
    private volatile boolean isFaceUp = false;
    private volatile boolean motionDetectedInInterval = false;

    // Service for background operations
    private HandlerThread sensorHandlerThread;
    private Handler sensorHandler;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // Initialize sensors
        Context ctx = getApplicationContext();
        AccelerometerSensor.initialize(ctx);
        LightSensor.initialize(ctx);
        SignificantMotionSensor.initialize(ctx);

        // Get reference to singletons
        lightSensor = LightSensor.getInstance();
        significantMotionSensor = SignificantMotionSensor.getInstance();
        microphoneRecorder = new MicrophoneRecorder();

        // Create handler for periodic sampling in background
        sensorHandlerThread = new HandlerThread("SensorServiceThread");
        sensorHandlerThread.start();
        sensorHandler = new Handler(sensorHandlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_SESSION.equals(action)) {
                long sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1);
                if (sessionId != -1) {
                    startSession(sessionId);
                } else {
                    Log.e(TAG, "Starting session failed: invalid session ID");
                }
            } else if (ACTION_STOP_SESSION.equals(action)) {
                stopSession();
            }
        }
        return START_STICKY;
    }

    private void startSession(long sessionId) {
        if (isRunning) {
            Log.w(TAG, "Session already running, ignoring start request");
            sendSessionErrorBroadcast("Session already in progress");
            return;
        }


        if (sessionId <= 0) {
            Log.e(TAG, "Invalid session ID: " + sessionId);
            sendSessionErrorBroadcast("Invalid session ID");
            stopSelf();
            return;
        }

        Log.d(TAG, "Starting session: " + sessionId);
        currentSessionId = sessionId;

        // Start Foreground Service
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Monitoring focus session...")
                .setSmallIcon(R.mipmap.ic_launcher_round) // FIXME: use app icon once created
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        // sources:
        // - https://github.com/landomen/ForegroundServiceSamples
        // - https://gist.github.com/Venryx/e1f772b4c05b2da08e118ccd5cc162ff?permalink_comment_id=3971817#file-foregroundservice-java
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            if (Build.VERSION.SDK_INT >= 34) {
               type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            }
            startForeground(NOTIFICATION_ID, notification, type);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // Start Sensors
        try {
            startSensors();
            isRunning = true;
            sensorHandler.post(logRunnable);

            Log.d(TAG, "Session started successfully");

        } catch (SecurityException e) {
            Log.e(TAG, "Failed to start sensors - missing permissions", e);
            isRunning = false;
            currentSessionId = -1;
            stopForeground(true);
            stopSelf();
            sendSessionErrorBroadcast("Missing sensor permissions: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting sensors", e);
            isRunning = false;
            currentSessionId = -1;
            stopForeground(true);
            stopSelf();
            sendSessionErrorBroadcast("Failed to start session: " + e.getMessage());
        }
    }

    private void sendSessionErrorBroadcast(String errorMessage) {
        Intent errorIntent = new Intent("ch.inf.usi.mindbricks.SESSION_ERROR");
        errorIntent.putExtra("error_message", errorMessage);
        errorIntent.putExtra("session_id", currentSessionId);
        sendBroadcast(errorIntent);
    }

    private void stopSession() {
        if (!isRunning) return;

        Log.d(TAG, "Stopping session");
        isRunning = false;

        // Stop Sensors
        stopSensors();

        // Stop Loop
        sensorHandler.removeCallbacks(logRunnable);

        stopForeground(true);
        stopSelf();
    }

    private void startSensors() {
        Log.d(TAG, "Attempting to start sensors...");

        // Start recording noise
        try {
            microphoneRecorder.startRecording();
            Log.d(TAG, "MicrophoneRecorder started.");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission missing for microphone", e);
        }

        // Start recording light level
        lightSensor.start((level, faceUp) -> {
            if (lastLightLevel != level || isFaceUp != faceUp) {
                Log.d(TAG, "LightSensor update: level=" + level + ", faceUp=" + faceUp);
                lastLightLevel = level;
                isFaceUp = faceUp;
            }
        });
        Log.d(TAG, "LightSensor started.");
        
        // Detect phone pickups
        significantMotionSensor.setListener(() -> {
            Log.d(TAG, "Significant motion detected event!");
            motionDetectedInInterval = true;
        });
        significantMotionSensor.start();
        Log.d(TAG, "SignificantMotionSensor started.");
    }

    private void stopSensors() {
        Log.d(TAG, "Attempting to stop sensors...");
        microphoneRecorder.stopRecording();
        Log.d(TAG, "MicrophoneRecorder stopped.");
        lightSensor.stop();
        Log.d(TAG, "LightSensor stopped.");
        significantMotionSensor.stop();
        Log.d(TAG, "SignificantMotionSensor stopped.");
    }

    private final Runnable logRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning || currentSessionId <= 0) {
                Log.w(TAG, "Log runnable stopped: isRunning=" + isRunning + ", sessionId=" + currentSessionId);
                return;
            }

            try {
                // Sample collected data
                long timestamp = System.currentTimeMillis();
                float noise = (float) microphoneRecorder.getCurrentAmplitude();
                float light = lastLightLevel;
                boolean faceUp = isFaceUp;
                boolean motion = motionDetectedInInterval;

                Log.v(TAG, String.format("Sensor data collected - Noise: %.2f, Light: %.2f, FaceUp: %b, Motion: %b",
                        noise, light, faceUp, motion));

                // Reset interval flags
                motionDetectedInInterval = false;

                // Log sample to DB
                SessionSensorLog log = new SessionSensorLog(
                        currentSessionId,
                        timestamp,
                        noise,
                        light,
                        motion,
                        faceUp
                );

                dbExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    db.sessionSensorLogDao().insert(log);
                    Log.d(TAG, "Sensor log inserted into DB for session: " + currentSessionId);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error in logging loop", e);
            } finally {
                // Trigger also next iteration
                if (isRunning) {
                    sensorHandler.postDelayed(this, LOG_INTERVAL_MS);
                }
            }
        }
    };

    private void createNotificationChannel() {
        // create channel for notifications
        // Source: https://gist.github.com/Venryx/e1f772b4c05b2da08e118ccd5cc162ff?permalink_comment_id=3971817#file-foregroundservice-java-L67-L78
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Focus Session Sensor Service",
                    NotificationManager.IMPORTANCE_LOW
            );

            // get notification manager
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy called");

        stopSession(); // Ensure everything is stopped

        // Gracefully shutdown executor with timeout
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                Log.w(TAG, "Executor didn't terminate in time, forcing shutdown");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted during executor shutdown", e);
            dbExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Shutdown handler thread with timeout
        if (sensorHandlerThread != null) {
            sensorHandlerThread.quitSafely();
            try {
                sensorHandlerThread.join(3000); // Wait max 3 seconds
                Log.d(TAG, "Handler thread terminated successfully");
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted waiting for handler thread", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
