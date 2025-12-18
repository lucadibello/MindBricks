package ch.inf.usi.mindbricks.util.database;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.util.questionnaire.ProductivityQuestionnaireResult;

/**
 * Utility to generate test data for the database.
 * Creates realistic study sessions with sensor logs and questionnaires for testing.
 * <p>
 * DISCLAIMER: Originally created by Marta, but way too complex. Most of the stuff was not needed.
 */
public class TestDataGenerator {

    private static final String TAG = "TestDataGenerator";

    private static final String[] SUBJECTS = {
            "Mathematics", "Physics", "Chemistry", "Biology",
            "Computer Science", "Literature", "Art", "Philosophy"
    };

    private static final int[] COLORS = {
            Color.parseColor("#EF5350"),  // Red
            Color.parseColor("#FFA726"),  // Orange
            Color.parseColor("#FFEE58"),  // Yellow
            Color.parseColor("#66BB6A"),  // Green
            Color.parseColor("#64B5F6"),  // Blue
            Color.parseColor("#BA68C8"),  // Purple
            Color.parseColor("#EC407A"),  // Pink
            Color.parseColor("#5C6BC0")   // Indigo
    };

    /**
     * Generates and inserts test study sessions into the database.
     *
     * @param context         the application context
     * @param numberOfSessions number of sessions to generate
     */
    public static void addTestSessions(Context context, int numberOfSessions) {
        AppDatabase db = AppDatabase.getInstance(context);

        new Thread(() -> {
            try {
                Log.d(TAG, "Generating " + numberOfSessions + " test sessions...");

                // Create or get tags
                long[] tagIds = createTags(db);

                // Generate and insert sessions
                Random random = new Random();
                for (int i = 0; i < numberOfSessions; i++) {
                    StudySession session = generateSession(random, tagIds);
                    long sessionId = db.studySessionDao().insert(session);

                    // Add sensor logs
                    insertSensorLogs(db, sessionId, session, random);

                    // Add questionnaire (70% chance)
                    if (random.nextDouble() < 0.70) {
                        insertQuestionnaire(db, sessionId, session, random);
                    }
                }

                Log.d(TAG, "Successfully inserted " + numberOfSessions + " test sessions");

            } catch (Exception e) {
                Log.e(TAG, "Error adding test sessions", e);
            }
        }).start();
    }

    /**
     * Clears all sessions from the database.
     *
     * @param context the application context
     */
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

    /**
     * Creates tags in the database if they don't exist.
     *
     * @param db the database instance
     * @return array of tag IDs
     */
    private static long[] createTags(AppDatabase db) {
        long[] tagIds = new long[SUBJECTS.length];
        for (int i = 0; i < SUBJECTS.length; i++) {
            Tag existingTag = db.tagDao().getTagByTitle(SUBJECTS[i]);
            if (existingTag != null) {
                tagIds[i] = existingTag.getId();
            } else {
                Tag tag = new Tag(SUBJECTS[i], COLORS[i]);
                tagIds[i] = db.tagDao().insert(tag);
            }
        }
        return tagIds;
    }

    /**
     * Generates a single study session with realistic values.
     *
     * @param random the random generator
     * @param tagIds available tag IDs
     * @return the generated study session
     */
    private static StudySession generateSession(Random random, long[] tagIds) {
        // Random date in the last 365 days
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -random.nextInt(365));
        calendar.set(Calendar.HOUR_OF_DAY, 6 + random.nextInt(18));
        calendar.set(Calendar.MINUTE, random.nextInt(60));
        calendar.set(Calendar.SECOND, 0);

        long timestamp = calendar.getTimeInMillis();
        int duration = generateDuration(random);
        long tagId = tagIds[random.nextInt(tagIds.length)];

        StudySession session = new StudySession(timestamp, duration, tagId);

        // Generate focus score and coins
        float focusScore = 40 + random.nextFloat() * 60; // 40-100
        session.setFocusScore(focusScore);
        session.setCoinsEarned((int) (duration * focusScore / 100));

        // Add notes (30% chance)
        if (random.nextDouble() < 0.30) {
            session.setNotes("Test session #" + random.nextInt(1000));
        }

        return session;
    }

    /**
     * Generates a realistic session duration.
     *
     * @param random the random generator
     * @return duration in minutes
     */
    private static int generateDuration(Random random) {
        double rand = random.nextDouble();
        if (rand < 0.3) {
            return 5 + random.nextInt(20);      // Short: 5-25 min
        } else if (rand < 0.7) {
            return 25 + random.nextInt(35);     // Medium: 25-60 min
        } else {
            return 60 + random.nextInt(60);     // Long: 60-120 min
        }
    }

    /**
     * Inserts sensor logs for a session.
     *
     * @param db        the database instance
     * @param sessionId the session ID
     * @param session   the study session
     * @param random    the random generator
     */
    private static void insertSensorLogs(AppDatabase db, long sessionId, StudySession session, Random random) {
        List<SessionSensorLog> logs = new ArrayList<>();

        // Generate logs (1 per 5 minutes approximately)
        int logCount = Math.max(3, session.getDurationMinutes() / 5);

        // Base values with some variation
        float baseNoise = random.nextFloat() * 1000;    // 0-1000 noise level
        float baseLight = random.nextFloat() * 100;     // 0-100 light level

        for (int i = 0; i < logCount; i++) {
            float noise = Math.max(0, baseNoise + (random.nextFloat() * 200 - 100));
            float light = Math.max(0, Math.min(100, baseLight + (random.nextFloat() * 20 - 10)));
            boolean motion = random.nextDouble() < 0.15; // 15% chance of motion
            boolean faceUp = random.nextDouble() < 0.85; // 85% face up

            logs.add(new SessionSensorLog(
                    sessionId,
                    session.getTimestamp() + (i * 5 * 60000L),  // Every 5 minutes
                    noise,
                    light,
                    motion,
                    faceUp
            ));
        }

        db.sessionSensorLogDao().insertAll(logs);
    }

    /**
     * Inserts a questionnaire for a session.
     *
     * @param db        the database instance
     * @param sessionId the session ID
     * @param session   the study session
     * @param random    the random generator
     */
    private static void insertQuestionnaire(AppDatabase db, long sessionId, StudySession session, Random random) {
        // Generate emotion based on focus score
        int emotion = Math.min(6, Math.max(0, (int) (session.getFocusScore() / 100 * 6) + random.nextInt(3) - 1));

        SessionQuestionnaire questionnaire;

        // 60% chance of detailed questionnaire
        if (random.nextDouble() < 0.60) {
            // Generate correlated ratings (1-7 scale)
            int baseRating = 3 + (int) (session.getFocusScore() / 100 * 3);

            int enthusiasm = Math.clamp(baseRating + random.nextInt(3) - 1, 1, 7);
            int energy = Math.clamp(baseRating + random.nextInt(3) - 1, 1, 7);
            int engagement = Math.clamp(baseRating + random.nextInt(3) - 1, 1, 7);
            int satisfaction = Math.clamp(baseRating + random.nextInt(3) - 1, 1, 7);
            int anticipation = Math.clamp(baseRating + random.nextInt(3) - 1, 1, 7);

            // create wrapper
            ProductivityQuestionnaireResult result = new ProductivityQuestionnaireResult(
                    enthusiasm, energy, engagement, satisfaction, anticipation
            );

            // create questionnaire
            questionnaire = SessionQuestionnaire.from(sessionId, emotion, result);
        } else {
            // create questionnaire without perceived productivity questions
            questionnaire = SessionQuestionnaire.from(sessionId, emotion, null);
        }

        db.sessionQuestionnaireDao().insert(questionnaire);
    }
}
