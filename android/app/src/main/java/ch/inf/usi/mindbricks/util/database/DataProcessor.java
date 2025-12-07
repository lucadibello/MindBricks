package ch.inf.usi.mindbricks.util.database;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.inf.usi.mindbricks.model.visual.DailyRecommendation;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.model.visual.WeeklyStats;

/**
 * Utility class for processing and analyzing study session data.
 * Contains methods to transform raw session data into chart-ready statistics.
 */
public class DataProcessor {
    public static WeeklyStats calculateWeeklyStats(List<StudySessionWithStats> sessions) {
        WeeklyStats stats = new WeeklyStats();

        if (sessions == null || sessions.isEmpty()) {
            return stats;
        }

        // Arrays to accumulate data per day
        int[] minutesPerDay = new int[7];
        float[] focusScoreSum = new float[7];
        int[] sessionCountPerDay = new int[7];

        Calendar calendar = Calendar.getInstance();

        // Process each session
        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());

            // Get day of week (Calendar.MONDAY = 2, so adjust to 0-6)
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int dayIndex = convertCalendarDayToIndex(dayOfWeek);

            // Accumulate data
            minutesPerDay[dayIndex] += session.getDurationMinutes();
            focusScoreSum[dayIndex] += session.getFocusScore();
            sessionCountPerDay[dayIndex]++;
        }

        // Calculate averages and set data
        int totalMinutes = 0;
        float totalFocusScore = 0;
        int totalSessions = 0;
        int daysWithSessions = 0;

        for (int i = 0; i < 7; i++) {
            stats.setDayMinutes(i, minutesPerDay[i]);
            stats.setDaySessionCount(i, sessionCountPerDay[i]);

            if (sessionCountPerDay[i] > 0) {
                float avgFocusScore = focusScoreSum[i] / sessionCountPerDay[i];
                stats.setDayFocusScore(i, avgFocusScore);

                totalMinutes += minutesPerDay[i];
                totalFocusScore += avgFocusScore;
                totalSessions += sessionCountPerDay[i];
                daysWithSessions++;
            } else {
                stats.setDayFocusScore(i, 0);
            }
        }

        stats.setTotalMinutes(totalMinutes);
        stats.setTotalSessions(totalSessions);

        if (daysWithSessions > 0) {
            stats.setAverageFocusScore(totalFocusScore / daysWithSessions);
        }

        return stats;
    }

    public static List<TimeSlotStats> calculateHourlyDistribution(List<StudySessionWithStats> sessions) {
        // Create 24 time slots (one for each hour)
        List<TimeSlotStats> hourlyStats = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyStats.add(new TimeSlotStats(hour));
        }

        if (sessions == null || sessions.isEmpty()) {
            return hourlyStats;
        }

        Calendar calendar = Calendar.getInstance();

        // Process each session
        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            // Add session data to the appropriate hour slot
            TimeSlotStats hourSlot = hourlyStats.get(hour);
            hourSlot.addSession(
                    session.getDurationMinutes(),
                    session.getFocusScore(),
                    session.getAvgNoiseLevel(),
                    session.getAvgLightLevel()
            );
        }

        return hourlyStats;
    }

    public static DailyRecommendation generateDailyRecommendation(List<StudySessionWithStats> sessions) {
        DailyRecommendation recommendation = new DailyRecommendation();

        if (sessions == null || sessions.isEmpty()) {
            recommendation.setReasonSummary("Not enough data yet. Complete more sessions to get personalized recommendations.");
            recommendation.setConfidenceScore(0);
            recommendation.setRecommendedSlots(new ArrayList<>());
            return recommendation;
        }

        // Calculate hourly distribution to find best times
        List<TimeSlotStats> hourlyStats = calculateHourlyDistribution(sessions);

        // Find top 3 most productive hours
        List<TimeSlotStats> rankedHours = new ArrayList<>();
        for (TimeSlotStats stats : hourlyStats) {
            if (stats.getSessionCount() > 0) {
                rankedHours.add(stats);
            }
        }

        // Sort by focus score (descending)
        Collections.sort(rankedHours, (a, b) ->
                Float.compare(b.getAverageFocusScore(), a.getAverageFocusScore()));

        // Create recommended time slots
        List<DailyRecommendation.TimeSlot> recommendedSlots = new ArrayList<>();
        int slotsToRecommend = Math.min(3, rankedHours.size());

        for (int i = 0; i < slotsToRecommend; i++) {
            TimeSlotStats hourStat = rankedHours.get(i);
            DailyRecommendation.TimeSlot slot = new DailyRecommendation.TimeSlot();
            slot.setStartTime(hourStat.getFormattedTime());
            slot.setEndTime(getEndTime(hourStat.getHourOfDay()));
            slot.setProductivityScore((int) hourStat.getAverageFocusScore());
            slot.setReason(generateSlotReason(hourStat));
            recommendedSlots.add(slot);
        }

        recommendation.setRecommendedSlots(recommendedSlots);

        // Generate summary
        if (!rankedHours.isEmpty()) {
            TimeSlotStats bestHour = rankedHours.get(0);
            String summary = String.format(
                    "Based on %d sessions, you're most productive around %s with %.0f%% focus.",
                    sessions.size(),
                    bestHour.getFormattedTime(),
                    bestHour.getAverageFocusScore()
            );
            recommendation.setReasonSummary(summary);

            int confidence = Math.min(100, (sessions.size() * 10));
            recommendation.setConfidenceScore(confidence);
        }

        return recommendation;
    }


    public static List<StudySessionWithStats> getRecentSessions(List<StudySessionWithStats> allSessions, int days) {
        if (allSessions == null || allSessions.isEmpty()) {
            return new ArrayList<>();
        }

        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
        List<StudySessionWithStats> recent = new ArrayList<>();

        for (StudySessionWithStats session : allSessions) {
            if (session.getTimestamp() >= cutoffTime) {
                recent.add(session);
            }
        }

        return recent;
    }

    public static int calculateTotalMinutes(List<StudySessionWithStats> sessions) {
        int total = 0;
        if (sessions != null) {
            for (StudySessionWithStats session : sessions) {
                total += session.getDurationMinutes();
            }
        }
        return total;
    }

    public static float calculateAverageFocusScore(List<StudySessionWithStats> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }

        float sum = 0;
        for (StudySessionWithStats session : sessions) {
            sum += session.getFocusScore();
        }

        return sum / sessions.size();
    }

    public static Map<String, List<StudySessionWithStats>> groupSessionsByTag(List<StudySessionWithStats> sessions) {
        Map<String, List<StudySessionWithStats>> grouped = new HashMap<>();

        if (sessions != null) {
            for (StudySessionWithStats session : sessions) {
                String tag = session.getTagTitle();
                if (!grouped.containsKey(tag)) {
                    grouped.put(tag, new ArrayList<>());
                }
                grouped.get(tag).add(session);
            }
        }

        return grouped;
    }

    public static int calculateLongestStreak(List<StudySessionWithStats> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }

        // Sort sessions by timestamp
        List<StudySessionWithStats> sorted = new ArrayList<>(sessions);
        Collections.sort(sorted, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

        Calendar calendar = Calendar.getInstance();
        int currentStreak = 1;
        int longestStreak = 1;

        calendar.setTimeInMillis(sorted.get(0).getTimestamp());
        int lastDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        int lastYear = calendar.get(Calendar.YEAR);

        for (int i = 1; i < sorted.size(); i++) {
            calendar.setTimeInMillis(sorted.get(i).getTimestamp());
            int currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
            int currentYear = calendar.get(Calendar.YEAR);

            // Check if consecutive day
            if (currentYear == lastYear && currentDayOfYear == lastDayOfYear + 1) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else if (currentYear == lastYear + 1 && currentDayOfYear == 1 && lastDayOfYear == 365) {
                // Handle year transition
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else if (currentDayOfYear != lastDayOfYear) {
                // Streak broken
                currentStreak = 1;
            }

            lastDayOfYear = currentDayOfYear;
            lastYear = currentYear;
        }

        return longestStreak;
    }

    // ---> Helper methods
    private static int convertCalendarDayToIndex(int calendarDay) {
        switch (calendarDay) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
            default: return 0;
        }
    }

    private static String getEndTime(int hour) {
        int endHour = (hour + 1) % 24;
        if (endHour == 0) {
            return "12:00 AM";
        } else if (endHour < 12) {
            return endHour + ":00 AM";
        } else if (endHour == 12) {
            return "12:00 PM";
        } else {
            return (endHour - 12) + ":00 PM";
        }
    }

    private static String generateSlotReason(TimeSlotStats hourStat) {
        float focusScore = hourStat.getAverageFocusScore();
        int sessionCount = hourStat.getSessionCount();

        String productivity;
        if (focusScore >= 80) {
            productivity = "excellent";
        } else if (focusScore >= 60) {
            productivity = "good";
        } else {
            productivity = "moderate";
        }

        return String.format(
                "Based on %d session%s with %s focus (%.0f%%)",
                sessionCount,
                sessionCount == 1 ? "" : "s",
                productivity,
                focusScore
        );
    }
}
