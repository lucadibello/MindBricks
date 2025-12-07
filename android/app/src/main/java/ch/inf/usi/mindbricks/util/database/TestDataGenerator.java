package ch.inf.usi.mindbricks.util.database;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;
import ch.inf.usi.mindbricks.model.visual.StudySession;

/**
 * Utility to generate test data for the database.
 * Use this temporarily to populate your database with sample sessions.
 */
public class TestDataGenerator {

    private static final String TAG = "TestDataGenerator";

    /**
     * Add test sessions to the database.
     * Call this from your Activity/Fragment to populate test data.
     */
    public static void addTestSessions(Context context, int numberOfSessions) {
        AppDatabase db = AppDatabase.getInstance(context);

        new Thread(() -> {
            try {
                Log.d(TAG, "Generating " + numberOfSessions + " test sessions...");

                List<StudySession> sessions = generateTestSessions(numberOfSessions);

                // Insert each session and generate fake logs for it
                for (StudySession session : sessions) {
                    long sessionId = db.studySessionDao().insert(session);
                    generateAndInsertLogs(db, sessionId, session);
                }

                Log.d(TAG, "Successfully inserted " + sessions.size() + " test sessions with logs");

            } catch (Exception e) {
                Log.e(TAG, "Error adding test sessions", e);
            }
        }).start();
    }

    private static void generateAndInsertLogs(AppDatabase db, long sessionId, StudySession session) {
        Random random = new Random();
        List<SessionSensorLog> logs = new ArrayList<>();
        
        // Generate fake stats that match what we want to see
        // We'll generate 10 logs per session to get an average
        float targetNoise = 200 + random.nextInt(1001);
        float targetLight = 30 + random.nextInt(60);
        int targetPickups = random.nextInt(6);
        
        for (int i = 0; i < 10; i++) {
            // Variate slightly around target
            float noise = Math.max(0, targetNoise + (random.nextInt(100) - 50));
            float light = Math.max(0, Math.min(100, targetLight + (random.nextInt(20) - 10)));
            
            // Distribute pickups randomly across logs
            boolean motion = i < targetPickups; 
            
            logs.add(new SessionSensorLog(
                sessionId,
                session.getTimestamp() + (i * 60000), // spread out by minutes
                noise,
                light,
                motion,
                true // face up
            ));
        }
        
        db.sessionSensorLogDao().insertAll(logs);
    }

    /**
     * Generate realistic test sessions.
     */
    private static List<StudySession> generateTestSessions(int count) {
        List<StudySession> sessions = new ArrayList<>();
        Random random = new Random();
        Calendar calendar = Calendar.getInstance();

        // Subjects for variety
        String[] subjects = {"Mathematics", "Physics", "Chemistry", "History", "English", "Computer Science"};
        int[] colors = {
                Color.rgb(33, 150, 243),   // Blue
                Color.rgb(76, 175, 80),    // Green
                Color.rgb(255, 152, 0),    // Orange
                Color.rgb(156, 39, 176),   // Purple
                Color.rgb(244, 67, 54),    // Red
                Color.rgb(0, 150, 136)     // Teal
        };

        // Generate sessions spread over last 30 days
        for (int i = 0; i < count; i++) {
            // Random day in last 30 days
            int daysAgo = random.nextInt(30);
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.DAY_OF_MONTH, -daysAgo);

            // Random hour between 8 AM and 10 PM
            int hour = 8 + random.nextInt(14);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, random.nextInt(60));
            calendar.set(Calendar.SECOND, 0);

            long timestamp = calendar.getTimeInMillis();

            // Random duration: 15-180 minutes (more realistic distribution)
            int duration;
            double rand = random.nextDouble();
            if (rand < 0.5) {
                duration = 30 + random.nextInt(30); // 50% between 30-60 min
            } else if (rand < 0.8) {
                duration = 60 + random.nextInt(60); // 30% between 60-120 min
            } else {
                duration = 15 + random.nextInt(30); // 20% short sessions 15-45 min
            }

            // Random subject
            int subjectIndex = random.nextInt(subjects.length);
            String subject = subjects[subjectIndex];
            int color = colors[subjectIndex];

            // Create session
            StudySession session = new StudySession(timestamp, duration, subject, color);

            // Add realistic metrics
            // Focus score: generally good (60-95) with some variation
            float focusScore = 60 + random.nextInt(36);
            if (random.nextDouble() < 0.2) { // 20% chance of excellent session
                focusScore = 85 + random.nextInt(16);
            }
            session.setFocusScore(focusScore);

            // Coins earned proportional to duration and focus
            int coins = (int) (duration * focusScore / 100);
            session.setCoinsEarned(coins);

            // Occasional notes
            if (random.nextDouble() < 0.3) { // 30% have notes
                String[] noteOptions = {
                        "Very productive session",
                        "Had some distractions",
                        "Completed homework assignment",
                        "Good focus today",
                        "Review for upcoming exam"
                };
                session.setNotes(noteOptions[random.nextInt(noteOptions.length)]);
            }

            sessions.add(session);
        }

        return sessions;
    }

    /**
     * Clear all sessions from database.
     */
    @SuppressWarnings("unused")
    public static void clearAllSessions(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        new Thread(() -> {
            try {
                db.studySessionDao().deleteAll();
                Log.d(TAG, "All sessions cleared");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing sessions", e);
            }
        }).start();
    }
}