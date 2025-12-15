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
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;

/**
 * Utility to generate test data for the database.
 * Use this temporarily to populate your database with sample sessions.
 */
public class TestDataGenerator {

    private static final String TAG = "TestDataGenerator";

    private static final String[] SUBJECTS = {
            "Mathematics",       // Red
            "Physics",           // Orange
            "Chemistry",         // Yellow
            "Biology",           // Green
            "Computer Science",  // Blue
            "Literature",        // Purple
            "Art",               // Pink
            "Philosophy",        // Indigo
            "Music",             // Cyan
            "Languages",         // Teal
            "Economics",         // Lime
            "Psychology"         // Brown
    };

    private static final int[] COLORS = {
            Color.parseColor("#EF5350"),  // Red
            Color.parseColor("#FFA726"),  // Orange
            Color.parseColor("#FFEE58"),  // Yellow
            Color.parseColor("#66BB6A"),  // Green
            Color.parseColor("#64B5F6"),  // Blue
            Color.parseColor("#BA68C8"),  // Purple
            Color.parseColor("#EC407A"),  // Pink
            Color.parseColor("#5C6BC0"),  // Indigo
            Color.parseColor("#26C6DA"),  // Cyan
            Color.parseColor("#26A69A"),  // Teal
            Color.parseColor("#D4E157"),  // Lime
            Color.parseColor("#A1887F")   // Brown
    };


    private static final String[] NOTE_OPTIONS = {
            "Very productive session",
            "Had some distractions",
            "Completed homework assignment",
            "Good focus today",
            "Review for upcoming exam",
            "Struggled to concentrate",
            "Pomodoro technique worked well",
            "Need quieter environment next time",
            "Finished early, felt great!",
            "Coffee helped maintain focus",
            "Group study session",
            "Late night cramming",
            "Morning session - very alert",
            "Post-lunch slump",
            "Library study - very quiet",
            "Cafe study - background noise helped",
            "Home study - some interruptions",
            "Deep work block completed",
            "Reviewed flashcards",
            "Practice problems done",
            ""
    };

    /**
     * Noise level categories (RMS amplitude values)
     */
    private static final float NOISE_SILENT = 0f;           // Complete silence
    private static final float NOISE_QUIET = 200f;          // Quiet room / library
    private static final float NOISE_MODERATE = 500f;       // Normal conversation nearby
    private static final float NOISE_LOUD = 1000f;          // Busy cafe / street noise
    private static final float NOISE_VERY_LOUD = 2000f;     // Very noisy environment
    private static final float NOISE_MAX = 3000f;           // Maximum realistic value

    /**
     * Light level categories (normalized 0-100 scale)
     */
    private static final float LIGHT_DARK = 0f;             // Complete darkness
    private static final float LIGHT_DIM = 20f;             // Dim room / evening
    private static final float LIGHT_NORMAL = 50f;          // Normal indoor lighting
    private static final float LIGHT_BRIGHT = 80f;          // Well-lit room / near window
    private static final float LIGHT_MAX = 100f;            // Direct sunlight

    /**
     * Range: 0-6 representing different emotional states
     */
    private static final int EMOTION_MIN = 0;
    private static final int EMOTION_MAX = 6;

    /**
     * Range: 1-7 (Strongly Disagree to Strongly Agree)
     */
    private static final int RATING_MIN = 1;
    private static final int RATING_MAX = 7;
    private static final int RATING_NEUTRAL = 4;

    public static void addTestSessions(Context context, int numberOfSessions) {
        AppDatabase db = AppDatabase.getInstance(context);

        new Thread(() -> {
            try {
                Log.d(TAG, "Generating " + numberOfSessions + " test sessions...");

                long[] tagIds = new long[SUBJECTS.length];
                for (int i = 0; i < SUBJECTS.length; i++) {
                    // Check if tag exists
                    Tag existingTag = db.tagDao().getTagByTitle(SUBJECTS[i]);
                    if (existingTag != null) {
                        tagIds[i] = existingTag.getId();
                    } else {
                        // Create new tag
                        Tag tag = new Tag(SUBJECTS[i], COLORS[i]);
                        tagIds[i] = db.tagDao().insert(tag);
                    }
                }

                List<StudySession> sessions = generateTestSessions(numberOfSessions, tagIds);

                // Insert each session and generate fake logs for it
                for (StudySession session : sessions) {
                    long sessionId = db.studySessionDao().insert(session);
                    generateAndInsertLogs(db, sessionId, session);
                    generateAndInsertQuestionnaire(db, sessionId, session);
                }

                Log.d(TAG, "Successfully inserted " + sessions.size() + " test sessions with logs and questionnaires");

                // Verify the insertion
                verifyInsertedData(db, sessions.size());

            } catch (Exception e) {
                Log.e(TAG, "Error adding test sessions", e);
            }
        }).start();
    }


    public static void addEdgeCaseSessions(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        new Thread(() -> {
            try {
                Log.d(TAG, "Generating edge case sessions...");

                List<StudySession> edgeCases = generateEdgeCaseSessions();

                for (StudySession session : edgeCases) {
                    long sessionId = db.studySessionDao().insert(session);

                    if (session.getDurationMinutes() > 0) {
                        generateEdgeCaseLogs(db, sessionId, session);
                    }

                    generateEdgeCaseQuestionnaire(db, sessionId, session);
                }

                Log.d(TAG, "Successfully inserted " + edgeCases.size() + " edge case sessions");

            } catch (Exception e) {
                Log.e(TAG, "Error adding edge case sessions", e);
            }
        }).start();
    }

    public static void addEnvironmentTestSessions(Context context, EnvironmentCondition condition) {
        AppDatabase db = AppDatabase.getInstance(context);

        new Thread(() -> {
            try {
                Log.d(TAG, "Generating " + condition.name() + " environment sessions...");

                List<StudySession> sessions = generateEnvironmentSessions(condition, 5);

                for (StudySession session : sessions) {
                    long sessionId = db.studySessionDao().insert(session);
                    generateEnvironmentLogs(db, sessionId, session, condition);
                    generateEnvironmentQuestionnaire(db, sessionId, session, condition);
                }

                Log.d(TAG, "Successfully inserted " + sessions.size() + " " + condition.name() + " sessions");

            } catch (Exception e) {
                Log.e(TAG, "Error adding environment sessions", e);
            }
        }).start();
    }

    public static void verifyDatabase(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        new Thread(() -> {
            try {

                List<StudySessionWithStats> sessions = db.studySessionDao().getAllSessions();
                Log.d(TAG, "Total sessions in database: " + (sessions != null ? sessions.size() : 0));

                if (sessions == null || sessions.isEmpty()) {
                    Log.w(TAG, "No sessions found in database!");
                    return;
                }

                int totalMinutes = 0;
                float totalFocusScore = 0;
                int totalCoins = 0;
                int questionnairesWithDetails = 0;
                int totalQuestionnaires = 0;

                for (StudySessionWithStats session : sessions) {
                    totalMinutes += session.getDurationMinutes();
                    totalFocusScore += session.getFocusScore();
                    totalCoins += session.getCoinsEarned();

                    float avgNoise = db.sessionSensorLogDao().getAverageNoise(session.getId());
                    float avgLight = db.sessionSensorLogDao().getAverageLight(session.getId());
                    int motionCount = db.sessionSensorLogDao().getMotionCount(session.getId());

                    SessionQuestionnaire questionnaire = db.sessionQuestionnaireDao()
                            .getQuestionnaireForSession(session.getId());

                    String questionnaireInfo = "none";
                    if (questionnaire != null) {
                        totalQuestionnaires++;
                        if (questionnaire.isAnsweredDetailedQuestions()) {
                            questionnairesWithDetails++;
                            questionnaireInfo = String.format("detailed (emotion=%d, avg=%.1f)",
                                    questionnaire.getInitialEmotion(),
                                    questionnaire.getAverageRating());
                        } else {
                            questionnaireInfo = String.format("quick (emotion=%d)",
                                    questionnaire.getInitialEmotion());
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error verifying database", e);
            }
        }).start();
    }

    public static void clearAllSessions(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        new Thread(() -> {
            try {
                db.studySessionDao().deleteAll();
                Log.d(TAG, "All sessions cleared (questionnaires cascade deleted)");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing sessions", e);
            }
        }).start();
    }

    /**
     * Predefined environment conditions for targeted testing
     */
    public enum EnvironmentCondition {
        QUIET_LIBRARY,      // Low noise, moderate light, minimal pickups
        BUSY_CAFE,          // High noise, bright light, some pickups
        DARK_ROOM,          // Low noise, very low light, minimal pickups
        OUTDOOR_PARK,       // Moderate noise, very bright, some pickups
        HOME_EVENING,       // Low-moderate noise, dim light, some pickups
        COMMUTE             // High noise, variable light, many pickups (face down)
    }

    /**
     * Generate realistic test sessions with full variety.
     */
    private static List<StudySession> generateTestSessions(int count, long[] tagIds) {
        List<StudySession> sessions = new ArrayList<>();
        Random random = new Random();
        Calendar calendar = Calendar.getInstance();

        // Generate sessions spread over last 30 days
        for (int i = 0; i < count; i++) {
            int daysAgo = random.nextInt(365);
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.DAY_OF_MONTH, -daysAgo);

            int hour = 6 + random.nextInt(18);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, random.nextInt(60));
            calendar.set(Calendar.SECOND, 0);

            long timestamp = calendar.getTimeInMillis();

            int duration = generateRealisticDuration(random);

            // Random subject with matching color
            int subjectIndex = random.nextInt(SUBJECTS.length);
            String subject = SUBJECTS[subjectIndex];
            int color = COLORS[subjectIndex];
            // Random tag
            long tagId = tagIds[random.nextInt(tagIds.length)];

            // Create session
            StudySession session = new StudySession(timestamp, duration, tagId);

            float focusScore = generateRealisticFocusScore(random);
            session.setFocusScore(focusScore);

            int coins = (int) (duration * focusScore / 100);
            session.setCoinsEarned(coins);

            // Notes with 35% probability
            if (random.nextDouble() < 0.35) {
                session.setNotes(NOTE_OPTIONS[random.nextInt(NOTE_OPTIONS.length)]);
            }

            sessions.add(session);
        }

        return sessions;
    }

    private static List<StudySession> generateEdgeCaseSessions() {
        List<StudySession> sessions = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        Random random = new Random();

        // Edge Case 1: Minimum duration (1 minute)
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Tag tag = new Tag("Quick Review", COLORS[0]);
        StudySession minDuration = new StudySession(calendar.getTimeInMillis(), 1, tag.getId());
        minDuration.setFocusScore(50f);
        minDuration.setCoinsEarned(0);
        minDuration.setNotes("Very short session");
        sessions.add(minDuration);

        // Edge Case 2: Maximum realistic duration (8 hours = 480 minutes)
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        StudySession maxDuration = new StudySession(calendar.getTimeInMillis(), 480, (new Tag("Marathon Study", COLORS[1])).getId());
        maxDuration.setFocusScore(65f);
        maxDuration.setCoinsEarned(312);
        maxDuration.setNotes("Full day study marathon");
        sessions.add(maxDuration);

        // Edge Case 3: Zero focus score
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        StudySession zeroFocus = new StudySession(calendar.getTimeInMillis(), 30, (new Tag("Distracted Session", COLORS[2])).getId());
        zeroFocus.setFocusScore(0f);
        zeroFocus.setCoinsEarned(0);
        zeroFocus.setNotes("Couldn't focus at all");
        sessions.add(zeroFocus);

        // Edge Case 4: Perfect focus score (100%)
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        StudySession perfectFocus = new StudySession(calendar.getTimeInMillis(), 60, (new Tag("Perfect Focus", COLORS[3])).getId());
        perfectFocus.setFocusScore(100f);
        perfectFocus.setCoinsEarned(60);
        perfectFocus.setNotes("Best session ever!");
        sessions.add(perfectFocus);

        // Edge Case 5: Midnight session (00:00)
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        StudySession midnightSession = new StudySession(calendar.getTimeInMillis(), 45, (new Tag("Midnight Cramming", COLORS[4])).getId());
        midnightSession.setFocusScore(55f);
        midnightSession.setCoinsEarned(25);
        sessions.add(midnightSession);

        // Edge Case 6: Noon session (12:00)
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        StudySession noonSession = new StudySession(calendar.getTimeInMillis(), 30, (new Tag("Lunch Break Study", COLORS[5])).getId());
        noonSession.setFocusScore(70f);
        noonSession.setCoinsEarned(21);
        sessions.add(noonSession);

        // Edge Case 7: Session with empty notes
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        StudySession emptyNotes = new StudySession(calendar.getTimeInMillis(), 25, (new Tag("Regular Session", COLORS[6]).getId()));
        emptyNotes.setFocusScore(75f);
        emptyNotes.setCoinsEarned(19);
        emptyNotes.setNotes("");
        sessions.add(emptyNotes);

        // Edge Case 8: Session with very long notes
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        StudySession longNotes = new StudySession(calendar.getTimeInMillis(), 90, (new Tag("Detailed Session", COLORS[7])).getId());
        longNotes.setFocusScore(80f);
        longNotes.setCoinsEarned(72);
        longNotes.setNotes("This was a very productive session where I covered chapters 5-8. " +
                "I found the material challenging but manageable. Need to review section 6.3 again tomorrow.");
        sessions.add(longNotes);

        // Edge Case 9: All subjects (use each subject once)
        for (int i = 0; i < SUBJECTS.length; i++) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            StudySession subjectSession = new StudySession(
                    calendar.getTimeInMillis(),
                    30 + random.nextInt(60),
                    (new Tag(SUBJECTS[i], COLORS[i])).getId()
            );
            subjectSession.setFocusScore(60 + random.nextInt(40));
            subjectSession.setCoinsEarned((int) (subjectSession.getDurationMinutes() * subjectSession.getFocusScore() / 100));
            sessions.add(subjectSession);
        }

        return sessions;
    }

    private static List<StudySession> generateEnvironmentSessions(EnvironmentCondition condition, int count) {
        List<StudySession> sessions = new ArrayList<>();
        Random random = new Random();
        Calendar calendar = Calendar.getInstance();

        String locationNote = getEnvironmentNote(condition);

        for (int i = 0; i < count; i++) {
            int daysAgo = random.nextInt(30);
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.DAY_OF_MONTH, -daysAgo);

            int subjectIndex = random.nextInt(SUBJECTS.length);
            int duration = 30 + random.nextInt(90);

            StudySession session = new StudySession(
                    calendar.getTimeInMillis(),
                    duration,
                    (new Tag(SUBJECTS[subjectIndex],
                    COLORS[subjectIndex])).getId()
            );

            // Focus score influenced by environment
            float focusScore = getEnvironmentFocusScore(condition, random);
            session.setFocusScore(focusScore);
            session.setCoinsEarned((int) (duration * focusScore / 100));
            session.setNotes(locationNote);

            sessions.add(session);
        }

        return sessions;
    }

    private static void generateAndInsertLogs(AppDatabase db, long sessionId, StudySession session) {
        Random random = new Random();
        List<SessionSensorLog> logs = new ArrayList<>();

        // Determine session environment profile randomly
        float baseNoise = random.nextFloat() * NOISE_MAX;
        float baseLight = random.nextFloat() * LIGHT_MAX;
        int targetPickups = random.nextInt(11);
        boolean primaryFaceUp = random.nextDouble() < 0.7;

        // Number of logs based on session duration (roughly 1 per minute, with variation)
        int logCount = Math.max(5, session.getDurationMinutes() / 6 + random.nextInt(5));

        for (int i = 0; i < logCount; i++) {
            // Add variation around base values
            float noise = Math.max(0, baseNoise + (random.nextFloat() * 400 - 200));
            float light = Math.max(0, Math.min(LIGHT_MAX, baseLight + (random.nextFloat() * 30 - 15)));

            boolean motion = random.nextDouble() < ((double) targetPickups / logCount);

            boolean faceUp = random.nextDouble() < 0.85 ? primaryFaceUp : !primaryFaceUp;

            logs.add(new SessionSensorLog(
                    sessionId,
                    session.getTimestamp() + (i * 60000L),  // 1 log per minute
                    noise,
                    light,
                    motion,
                    faceUp
            ));
        }

        db.sessionSensorLogDao().insertAll(logs);
    }

    private static void generateEdgeCaseLogs(AppDatabase db, long sessionId, StudySession session) {
        Random random = new Random();
        List<SessionSensorLog> logs = new ArrayList<>();

        int logCount = Math.max(3, session.getDurationMinutes() / 10);

        for (int i = 0; i < logCount; i++) {
            // Alternate between extreme values
            float noise, light;
            boolean motion, faceUp;

            switch (i % 5) {
                case 0: // Silent and dark
                    noise = NOISE_SILENT;
                    light = LIGHT_DARK;
                    motion = false;
                    faceUp = true;
                    break;
                case 1: // Very loud and bright
                    noise = NOISE_MAX;
                    light = LIGHT_MAX;
                    motion = true;
                    faceUp = true;
                    break;
                case 2: // Moderate values, face down
                    noise = NOISE_MODERATE;
                    light = LIGHT_NORMAL;
                    motion = false;
                    faceUp = false;
                    break;
                case 3: // Quiet but bright
                    noise = NOISE_QUIET;
                    light = LIGHT_BRIGHT;
                    motion = true;
                    faceUp = true;
                    break;
                default: // Random extreme
                    noise = random.nextBoolean() ? NOISE_SILENT : NOISE_VERY_LOUD;
                    light = random.nextBoolean() ? LIGHT_DARK : LIGHT_MAX;
                    motion = random.nextBoolean();
                    faceUp = random.nextBoolean();
                    break;
            }

            logs.add(new SessionSensorLog(
                    sessionId,
                    session.getTimestamp() + (i * 60000L),
                    noise,
                    light,
                    motion,
                    faceUp
            ));
        }

        db.sessionSensorLogDao().insertAll(logs);
    }

    private static void generateEnvironmentLogs(AppDatabase db, long sessionId,
                                                StudySession session, EnvironmentCondition condition) {
        Random random = new Random();
        List<SessionSensorLog> logs = new ArrayList<>();

        float baseNoise, baseLight;
        double pickupProbability;
        boolean typicalFaceUp;

        switch (condition) {
            case QUIET_LIBRARY:
                baseNoise = NOISE_QUIET;
                baseLight = LIGHT_NORMAL;
                pickupProbability = 0.05;
                typicalFaceUp = true;
                break;
            case BUSY_CAFE:
                baseNoise = NOISE_LOUD;
                baseLight = LIGHT_BRIGHT;
                pickupProbability = 0.15;
                typicalFaceUp = true;
                break;
            case DARK_ROOM:
                baseNoise = NOISE_QUIET;
                baseLight = LIGHT_DIM;
                pickupProbability = 0.05;
                typicalFaceUp = true;
                break;
            case OUTDOOR_PARK:
                baseNoise = NOISE_MODERATE;
                baseLight = LIGHT_MAX;
                pickupProbability = 0.2;
                typicalFaceUp = true;
                break;
            case HOME_EVENING:
                baseNoise = NOISE_QUIET + random.nextFloat() * 200;
                baseLight = LIGHT_DIM + random.nextFloat() * 20;
                pickupProbability = 0.1;
                typicalFaceUp = true;
                break;
            case COMMUTE:
                baseNoise = NOISE_LOUD;
                baseLight = LIGHT_NORMAL;
                pickupProbability = 0.4;
                typicalFaceUp = false;
                break;
            default:
                baseNoise = NOISE_MODERATE;
                baseLight = LIGHT_NORMAL;
                pickupProbability = 0.1;
                typicalFaceUp = true;
        }

        int logCount = Math.max(5, session.getDurationMinutes() / 6);

        for (int i = 0; i < logCount; i++) {
            float noise = Math.max(0, baseNoise + (random.nextFloat() * 100 - 50));
            float light = Math.max(0, Math.min(LIGHT_MAX, baseLight + (random.nextFloat() * 20 - 10)));
            boolean motion = random.nextDouble() < pickupProbability;
            boolean faceUp = random.nextDouble() < 0.9 ? typicalFaceUp : !typicalFaceUp;

            logs.add(new SessionSensorLog(
                    sessionId,
                    session.getTimestamp() + (i * 60000L),
                    noise,
                    light,
                    motion,
                    faceUp
            ));
        }

        db.sessionSensorLogDao().insertAll(logs);
    }

    private static void generateAndInsertQuestionnaire(AppDatabase db, long sessionId, StudySession session) {
        Random random = new Random();

        // 70% chance of having a questionnaire
        if (random.nextDouble() > 0.70) {
            return;
        }

        int emotion = generateCorrelatedEmotion(session.getFocusScore(), random);

        // 60% chance of detailed questionnaire, 40% quick
        boolean detailed = random.nextDouble() < 0.60;

        SessionQuestionnaire questionnaire;

        if (detailed) {
            int enthusiasm = generateCorrelatedRating(emotion, session.getFocusScore(), random);
            int energy = generateCorrelatedRating(emotion, session.getFocusScore(), random);
            int engagement = generateCorrelatedRating(emotion, session.getFocusScore(), random);
            int satisfaction = generateCorrelatedRating(emotion, session.getFocusScore(), random);
            int anticipation = generateCorrelatedRating(emotion, session.getFocusScore(), random);

            questionnaire = new SessionQuestionnaire(
                    sessionId,
                    session.getTimestamp() + session.getDurationMinutes() * 60000L, // End of session
                    emotion,
                    enthusiasm,
                    energy,
                    engagement,
                    satisfaction,
                    anticipation
            );
        } else {
            questionnaire = new SessionQuestionnaire(
                    sessionId,
                    session.getTimestamp() + session.getDurationMinutes() * 60000L,
                    emotion
            );
        }

        db.sessionQuestionnaireDao().insert(questionnaire);
    }


    private static void generateEdgeCaseQuestionnaire(AppDatabase db, long sessionId, StudySession session) {
        Random random = new Random();
        SessionQuestionnaire questionnaire;

        // Rotate through different edge cases
        int caseType = (int) (sessionId % 8);

        switch (caseType) {
            case 0: // Minimum emotion, all minimum ratings
                questionnaire = new SessionQuestionnaire(
                        sessionId,
                        session.getTimestamp() + session.getDurationMinutes() * 60000L,
                        EMOTION_MIN,
                        RATING_MIN, RATING_MIN, RATING_MIN, RATING_MIN, RATING_MIN
                );
                break;

            case 1: // Maximum emotion, all maximum ratings
                questionnaire = new SessionQuestionnaire(
                        sessionId,
                        session.getTimestamp() + session.getDurationMinutes() * 60000L,
                        EMOTION_MAX,
                        RATING_MAX, RATING_MAX, RATING_MAX, RATING_MAX, RATING_MAX
                );
                break;

            case 2: // Neutral emotion, all neutral ratings
                questionnaire = new SessionQuestionnaire(
                        sessionId,
                        session.getTimestamp() + session.getDurationMinutes() * 60000L,
                        3, // Middle of 0-6
                        RATING_NEUTRAL, RATING_NEUTRAL, RATING_NEUTRAL, RATING_NEUTRAL, RATING_NEUTRAL
                );
                break;

            case 3: // Quick questionnaire only (no detailed ratings)
                questionnaire = new SessionQuestionnaire(
                        sessionId,
                        session.getTimestamp() + session.getDurationMinutes() * 60000L,
                        random.nextInt(EMOTION_MAX + 1)
                );
                break;

            case 4: // Mixed ratings - some high, some low
                questionnaire = new SessionQuestionnaire(
                        sessionId,
                        session.getTimestamp() + session.getDurationMinutes() * 60000L,
                        4,
                        RATING_MAX, RATING_MIN, RATING_MAX, RATING_MIN, RATING_NEUTRAL
                );
                break;

            case 5: // High emotion but low ratings (conflicting)
                questionnaire = new SessionQuestionnaire(
                        sessionId,
                        session.getTimestamp() + session.getDurationMinutes() * 60000L,
                        EMOTION_MAX,
                        RATING_MIN + 1, RATING_MIN + 1, RATING_MIN, RATING_MIN + 1, RATING_MIN
                );
                break;

            case 6: // Low emotion but high ratings (conflicting)
                questionnaire = new SessionQuestionnaire(
                        sessionId,
                        session.getTimestamp() + session.getDurationMinutes() * 60000L,
                        EMOTION_MIN,
                        RATING_MAX - 1, RATING_MAX, RATING_MAX - 1, RATING_MAX, RATING_MAX - 1
                );
                break;

            default: // Random values across full range
                questionnaire = new SessionQuestionnaire(
                        sessionId,
                        session.getTimestamp() + session.getDurationMinutes() * 60000L,
                        random.nextInt(EMOTION_MAX + 1),
                        RATING_MIN + random.nextInt(RATING_MAX),
                        RATING_MIN + random.nextInt(RATING_MAX),
                        RATING_MIN + random.nextInt(RATING_MAX),
                        RATING_MIN + random.nextInt(RATING_MAX),
                        RATING_MIN + random.nextInt(RATING_MAX)
                );
                break;
        }

        db.sessionQuestionnaireDao().insert(questionnaire);
    }

    private static void generateEnvironmentQuestionnaire(AppDatabase db, long sessionId,
                                                         StudySession session, EnvironmentCondition condition) {
        Random random = new Random();

        int baseEmotion;
        int baseRating;

        switch (condition) {
            case QUIET_LIBRARY:
                baseEmotion = 4 + random.nextInt(3);
                baseRating = 5 + random.nextInt(3);
                break;
            case BUSY_CAFE:
                baseEmotion = 2 + random.nextInt(4);
                baseRating = 3 + random.nextInt(4);
                break;
            case DARK_ROOM:
                baseEmotion = 3 + random.nextInt(3);
                baseRating = 4 + random.nextInt(3);
                break;
            case OUTDOOR_PARK:
                baseEmotion = 3 + random.nextInt(4);
                baseRating = 4 + random.nextInt(4);
                break;
            case HOME_EVENING:
                baseEmotion = 2 + random.nextInt(4);
                baseRating = 3 + random.nextInt(4);
                break;
            case COMMUTE:
                baseEmotion = 1 + random.nextInt(4);
                baseRating = 2 + random.nextInt(4);
                break;
            default:
                baseEmotion = 3;
                baseRating = 4;
        }

        int emotion = Math.min(EMOTION_MAX, Math.max(EMOTION_MIN, baseEmotion));
        int enthusiasm = Math.min(RATING_MAX, Math.max(RATING_MIN, baseRating + random.nextInt(2) - 1));
        int energy = Math.min(RATING_MAX, Math.max(RATING_MIN, baseRating + random.nextInt(2) - 1));
        int engagement = Math.min(RATING_MAX, Math.max(RATING_MIN, baseRating + random.nextInt(2) - 1));
        int satisfaction = Math.min(RATING_MAX, Math.max(RATING_MIN, baseRating + random.nextInt(2) - 1));
        int anticipation = Math.min(RATING_MAX, Math.max(RATING_MIN, baseRating + random.nextInt(2) - 1));

        SessionQuestionnaire questionnaire = new SessionQuestionnaire(
                sessionId,
                session.getTimestamp() + session.getDurationMinutes() * 60000L,
                emotion,
                enthusiasm,
                energy,
                engagement,
                satisfaction,
                anticipation
        );

        db.sessionQuestionnaireDao().insert(questionnaire);
    }

    private static int generateRealisticDuration(Random random) {
        double rand = random.nextDouble();

        if (rand < 0.15) {
            // 15%: Very short sessions (5-20 min)
            return 5 + random.nextInt(16);
        } else if (rand < 0.45) {
            // 30%: Short sessions (20-45 min)
            return 20 + random.nextInt(26);
        } else if (rand < 0.75) {
            // 30%: Medium sessions (45-90 min)
            return 45 + random.nextInt(46);
        } else if (rand < 0.92) {
            // 17%: Long sessions (90-150 min)
            return 90 + random.nextInt(61);
        } else {
            // 8%: Very long sessions (150-240 min)
            return 150 + random.nextInt(91);
        }
    }

    private static float generateRealisticFocusScore(Random random) {
        double rand = random.nextDouble();

        if (rand < 0.05) {
            // 5%: Poor focus (0-30)
            return random.nextFloat() * 30;
        } else if (rand < 0.20) {
            // 15%: Below average (30-50)
            return 30 + random.nextFloat() * 20;
        } else if (rand < 0.50) {
            // 30%: Average (50-70)
            return 50 + random.nextFloat() * 20;
        } else if (rand < 0.85) {
            // 35%: Good (70-85)
            return 70 + random.nextFloat() * 15;
        } else {
            // 15%: Excellent (85-100)
            return 85 + random.nextFloat() * 15;
        }
    }

    private static float getEnvironmentFocusScore(EnvironmentCondition condition, Random random) {
        float baseFocus;

        switch (condition) {
            case QUIET_LIBRARY:
                baseFocus = 75 + random.nextFloat() * 20;
                break;
            case BUSY_CAFE:
                baseFocus = 50 + random.nextFloat() * 35;
                break;
            case DARK_ROOM:
                baseFocus = 65 + random.nextFloat() * 25;
                break;
            case OUTDOOR_PARK:
                baseFocus = 55 + random.nextFloat() * 30;
                break;
            case HOME_EVENING:
                baseFocus = 60 + random.nextFloat() * 30;
                break;
            case COMMUTE:
                baseFocus = 30 + random.nextFloat() * 40;
                break;
            default:
                baseFocus = 50 + random.nextFloat() * 40;
        }

        return Math.min(100, Math.max(0, baseFocus));
    }


    private static String getEnvironmentNote(EnvironmentCondition condition) {
        switch (condition) {
            case QUIET_LIBRARY:
                return "Library study - very quiet and focused";
            case BUSY_CAFE:
                return "Cafe study - background noise present";
            case DARK_ROOM:
                return "Evening study - dim lighting";
            case OUTDOOR_PARK:
                return "Outdoor study - bright and fresh air";
            case HOME_EVENING:
                return "Home study - evening session";
            case COMMUTE:
                return "Commute study - on the go";
            default:
                return "";
        }
    }


    private static int generateCorrelatedEmotion(float focusScore, Random random) {
        // Map focus score (0-100) to emotion tendency (0-6)
        float emotionBase = (focusScore / 100f) * 6;

        // Add some randomness (-1 to +1)
        int emotion = Math.round(emotionBase) + random.nextInt(3) - 1;

        // Clamp to valid range
        return Math.min(EMOTION_MAX, Math.max(EMOTION_MIN, emotion));
    }


    private static int generateCorrelatedRating(int emotion, float focusScore, Random random) {
        // Combine emotion (0-6) and focus (0-100) to get base rating
        float emotionFactor = (emotion / 6f) * 3;
        float focusFactor = (focusScore / 100f) * 3;

        float baseRating = RATING_MIN + emotionFactor + focusFactor;

        // Add some randomness (-1 to +1)
        int rating = Math.round(baseRating) + random.nextInt(3) - 1;

        // Clamp to valid range
        return Math.min(RATING_MAX, Math.max(RATING_MIN, rating));
    }


    private static void verifyInsertedData(AppDatabase db, int expectedCount) {
        List<StudySessionWithStats> sessions = db.studySessionDao().getAllSessions();
        int actualCount = sessions != null ? sessions.size() : 0;

        if (actualCount >= expectedCount) {
            Log.d(TAG, "Verification passed: " + actualCount + " sessions in database");
        } else {
            Log.w(TAG, "Verification: Expected at least " + expectedCount +
                    " sessions, found " + actualCount);
        }
    }
}