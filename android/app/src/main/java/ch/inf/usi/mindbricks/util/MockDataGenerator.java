package ch.inf.usi.mindbricks.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import ch.inf.usi.mindbricks.model.StudySession;

/**
 * Generates mock study session data for testing and demonstration
 */
public class MockDataGenerator {

    private static final Random random = new Random();

    private static final String[] TAG_TITLES = {
            "Mathematics", "Physics", "Programming", "History", "Literature",
            "Biology", "Chemistry", "Art", "Music", "Languages"
    };

    private static final int[] TAG_COLORS = {
            0xFFF44336, // Red
            0xFFFB8C00, // Orange
            0xFFFBC02D, // Yellow
            0xFF4CAF50, // Green
            0xFF2196F3, // Blue
            0xFF9C27B0  // Purple
    };

    /**
     * Generate mock study sessions
     *
     * @param count Number of sessions to generate
     * @return List of mock study sessions
     */
    public static List<StudySession> generateMockSessions(int count) {
        List<StudySession> sessions = new ArrayList<>();

        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < count; i++) {
            // Random session within last 60 days
            long daysAgo = random.nextInt(60);
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_MONTH, -(int) daysAgo);

            // Random hour between 6 AM and 11 PM
            int hour = 6 + random.nextInt(17);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, random.nextInt(60));

            long timestamp = cal.getTimeInMillis();

            // Random duration between 15-120 minutes
            int duration = 15 + random.nextInt(106);

            // Random tag
            String tagTitle = TAG_TITLES[random.nextInt(TAG_TITLES.length)];
            int tagColor = TAG_COLORS[random.nextInt(TAG_COLORS.length)];

            StudySession session = new StudySession(timestamp, duration, tagTitle, tagColor);

            // Generate realistic metrics based on time of day
            session.setAvgNoiseLevel(generateNoiseLevel(hour));
            session.setAvgLightLevel(generateLightLevel(hour));
            session.setPhonePickupCount(random.nextInt(8));
            session.setFocusScore(generateFocusScore(hour, session.getPhonePickupCount()));
            session.setCoinsEarned(duration / 60); // 1 coin per minute

            // Occasional notes
            if (random.nextFloat() < 0.3) { // 30% chance of having notes
                session.setNotes(generateRandomNote());
            }

            sessions.add(session);
        }

        // Sort by timestamp (newest first)
        sessions.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        return sessions;
    }

    /**
     * Generate realistic noise level based on time of day
     */
    private static float generateNoiseLevel(int hour) {
        float baseNoise;

        if (hour >= 6 && hour < 9) {
            baseNoise = 40 + random.nextFloat() * 30; // Morning: moderate
        } else if (hour >= 9 && hour < 17) {
            baseNoise = 50 + random.nextFloat() * 30; // Day: higher
        } else if (hour >= 17 && hour < 22) {
            baseNoise = 45 + random.nextFloat() * 25; // Evening: moderate-high
        } else {
            baseNoise = 20 + random.nextFloat() * 20; // Night: low
        }

        return Math.min(100, Math.max(0, baseNoise));
    }

    /**
     * Generate realistic light level based on time of day
     */
    private static float generateLightLevel(int hour) {
        float baseLight;

        if (hour >= 6 && hour < 8) {
            baseLight = 40 + random.nextFloat() * 30; // Dawn: moderate
        } else if (hour >= 8 && hour < 18) {
            baseLight = 70 + random.nextFloat() * 30; // Day: bright
        } else if (hour >= 18 && hour < 21) {
            baseLight = 50 + random.nextFloat() * 30; // Dusk: moderate
        } else {
            baseLight = 20 + random.nextFloat() * 30; // Night: low
        }

        return Math.min(100, Math.max(0, baseLight));
    }

    /**
     * Generate realistic focus score
     */
    private static float generateFocusScore(int hour, int pickupCount) {
        float baseScore;

        // Morning and evening tend to have better focus
        if (hour >= 7 && hour < 10) {
            baseScore = 75 + random.nextFloat() * 20; // Morning: high
        } else if (hour >= 10 && hour < 14) {
            baseScore = 60 + random.nextFloat() * 25; // Midday: moderate
        } else if (hour >= 14 && hour < 17) {
            baseScore = 55 + random.nextFloat() * 25; // Afternoon: moderate-low
        } else if (hour >= 17 && hour < 21) {
            baseScore = 70 + random.nextFloat() * 20; // Evening: high
        } else {
            baseScore = 65 + random.nextFloat() * 25; // Other: moderate
        }

        // Reduce score based on phone pickups
        baseScore -= pickupCount * 5;

        return Math.min(100, Math.max(20, baseScore));
    }

    /**
     * Generate random study notes
     */
    private static String generateRandomNote() {
        String[] notes = {
                "Very productive session! Made good progress.",
                "Struggled with focus in the beginning but got better.",
                "Great session, minimal distractions.",
                "Had to deal with some noise but managed well.",
                "Feeling accomplished!",
                "Need to review this material again tomorrow.",
                "Completed all planned tasks for today.",
                "Coffee helped a lot during this session.",
                "Found this topic really interesting.",
                "A bit tired but pushed through."
        };

        return notes[random.nextInt(notes.length)];
    }
}