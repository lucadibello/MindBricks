package ch.inf.usi.mindbricks.util;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.AIRecommendation;
import ch.inf.usi.mindbricks.model.GoalRing;
import ch.inf.usi.mindbricks.model.HeatmapCell;
import ch.inf.usi.mindbricks.model.HourlyQuality;
import ch.inf.usi.mindbricks.model.StreakDay;
import ch.inf.usi.mindbricks.model.StudySession;

/*
* Utility class for processing study session data into analytics metrics
 */
public class AnalyticsProcessor {

    // Constants for goal calculations
    private static final int DEFAULT_DAILY_TARGET_MINUTES = 120;
    private static final float EXCEPTIONAL_MULTIPLIER = 1.5f;
    private static final float QUALITY_THRESHOLD = 70.0f;
    private static final int TARGET_COUNT = 4;

    public static List<HourlyQuality> computeHourlyQuality(List<StudySession> sessions) {
        // Create map: hour -> list of focus scores
        Map<Integer, List<Float>> hourlyScores = new HashMap<>();

        for (StudySession session : sessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(session.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            if (!hourlyScores.containsKey(hour)) {
                hourlyScores.put(hour, new ArrayList<>());
            }
            hourlyScores.get(hour).add(session.getFocusScore());
        }

        // Compute averages for each hour
        List<HourlyQuality> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            List<Float> scores = hourlyScores.get(hour);

            HourlyQuality hq = new HourlyQuality();
            hq.setHour(hour);

            if (scores != null && !scores.isEmpty()) {
                float sum = 0;
                for (float score : scores) {
                    sum += score;
                }
                hq.setAvgQuality(sum / scores.size());
                hq.setSessionCount(scores.size());
            } else {
                hq.setAvgQuality(0);
                hq.setSessionCount(0);
            }

            result.add(hq);
        }

        return result;
    }

    public static List<HeatmapCell> computeHeatmapData(List<StudySession> sessions, int daysToShow) {
        // Create map: (day, hour) -> list of focus scores
        Map<String, List<Float>> cellScores = new HashMap<>();

        Calendar now = Calendar.getInstance();
        int currentDay = now.get(Calendar.DAY_OF_MONTH);

        for (StudySession session : sessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(session.getTimestamp());

            int day = cal.get(Calendar.DAY_OF_MONTH);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            // Only include if within range
            if (Math.abs(currentDay - day) <= daysToShow) {
                String key = day + "-" + hour;

                if (!cellScores.containsKey(key)) {
                    cellScores.put(key, new ArrayList<>());
                }
                cellScores.get(key).add(session.getFocusScore());
            }
        }

        // Compute averages for each cell
        List<HeatmapCell> result = new ArrayList<>();

        for (int day = Math.max(1, currentDay - daysToShow); day <= currentDay; day++) {
            for (int hour = 0; hour < 24; hour++) {
                String key = day + "-" + hour;
                List<Float> scores = cellScores.get(key);

                HeatmapCell cell = new HeatmapCell();
                cell.setDayOfMonth(day);
                cell.setHour(hour);

                if (scores != null && !scores.isEmpty()) {
                    float sum = 0;
                    for (float score : scores) {
                        sum += score;
                    }
                    cell.setAvgQuality(sum / scores.size());
                    cell.setSessionCount(scores.size());
                } else {
                    cell.setAvgQuality(0);
                    cell.setSessionCount(0);
                }

                result.add(cell);
            }
        }

        return result;
    }

    public static List<StreakDay> computeStreakCalendar(List<StudySession> sessions, int targetMinutes) {
        // Group sessions by date
        Map<String, List<StudySession>> dailySessions = new HashMap<>();

        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        for (StudySession session : sessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(session.getTimestamp());

            if (cal.get(Calendar.MONTH) == currentMonth &&
                    cal.get(Calendar.YEAR) == currentYear) {

                int day = cal.get(Calendar.DAY_OF_MONTH);
                String key = String.valueOf(day);

                if (!dailySessions.containsKey(key)) {
                    dailySessions.put(key, new ArrayList<>());
                }
                dailySessions.get(key).add(session);
            }
        }

        // Compute streak days
        List<StreakDay> result = new ArrayList<>();
        int daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            StreakDay streakDay = new StreakDay(day, currentMonth, currentYear);

            String key = String.valueOf(day);
            List<StudySession> daySessions = dailySessions.get(key);

            if (daySessions != null && !daySessions.isEmpty()) {
                int totalMinutes = 0;
                float totalQuality = 0;

                for (StudySession session : daySessions) {
                    totalMinutes += session.getDurationMinutes();
                    totalQuality += session.getFocusScore();
                }

                float avgQuality = totalQuality / daySessions.size();

                streakDay.setTotalMinutes(totalMinutes);
                streakDay.setAvgQuality(avgQuality);
                streakDay.setSessionCount(daySessions.size());

                // Determine status
                if (totalMinutes >= targetMinutes * EXCEPTIONAL_MULTIPLIER && avgQuality >= QUALITY_THRESHOLD) {
                    streakDay.setStatus(StreakDay.StreakStatus.EXCEPTIONAL);
                } else if (totalMinutes >= targetMinutes) {
                    streakDay.setStatus(StreakDay.StreakStatus.HIT_TARGET);
                } else if (totalMinutes > 0) {
                    streakDay.setStatus(StreakDay.StreakStatus.PARTIAL);
                } else {
                    streakDay.setStatus(StreakDay.StreakStatus.NONE);
                }
            } else {
                streakDay.setStatus(StreakDay.StreakStatus.NONE);
            }

            result.add(streakDay);
        }

        return result;
    }

    public static int computeCurrentStreak(List<StreakDay> streakDays) {
        if (streakDays == null || streakDays.isEmpty()) {
            return 0;
        }

        int streak = 0;

        // Count backwards from today
        for (int i = streakDays.size() - 1; i >= 0; i--) {
            StreakDay day = streakDays.get(i);

            if (day.getStatus() != StreakDay.StreakStatus.NONE) {
                streak++;
            } else {
                break; // Streak broken
            }
        }

        return streak;
    }

    public static List<GoalRing> computeGoalRings(List<StudySession> todaySessions, int targetMinutes) {
        List<GoalRing> rings = new ArrayList<>();

        // Calculate totals
        int totalMinutes = 0;
        float totalQuality = 0;
        int qualitySessionCount = 0;

        for (StudySession session : todaySessions) {
            totalMinutes += session.getDurationMinutes();

            if (session.getFocusScore() >= QUALITY_THRESHOLD) {
                totalQuality += session.getDurationMinutes();
                qualitySessionCount++;
            }
        }

        // Ring 1: Study Time
        rings.add(new GoalRing(
                "Study Time",
                totalMinutes,
                targetMinutes,
                Color.parseColor("#4CAF50"), // Green
                "min"
        ));

        // Ring 2: Quality Time (high-focus sessions)
        rings.add(new GoalRing(
                "Quality Time",
                totalQuality,
                targetMinutes * 0.7f, // 70% of target should be quality
                Color.parseColor("#2196F3"), // Blue
                "min"
        ));

        // Ring 3: Session Count
        rings.add(new GoalRing(
                "Sessions",
                todaySessions.size(),
                TARGET_COUNT,
                Color.parseColor("#FF9800"), // Orange
                "sessions"
        ));

        return rings;
    }

    public static List<AIRecommendation> generateRecommendations(
            List<StudySession> sessions,
            List<HourlyQuality> hourlyQuality) {

        List<AIRecommendation> recommendations = new ArrayList<>();

        if (sessions.isEmpty() || hourlyQuality.isEmpty()) {
            return recommendations;
        }

        // Find best hour
        HourlyQuality bestHour = null;
        float maxQuality = 0;

        for (HourlyQuality hq : hourlyQuality) {
            if (hq.getSessionCount() > 0 && hq.getAvgQuality() > maxQuality) {
                maxQuality = hq.getAvgQuality();
                bestHour = hq;
            }
        }

        if (bestHour != null) {
            recommendations.add(new AIRecommendation(
                    AIRecommendation.RecommendationType.BEST_HOUR,
                    "Peak Performance",
                    String.format(Locale.getDefault(),
                            "Your focus peaks at %d:00 (%.0f%% quality)",
                            bestHour.getHour(), bestHour.getAvgQuality()),
                    R.drawable.ic_trending_up,
                    Color.parseColor("#E8F5E9")
            ));
        }

        // Find worst hour (to avoid)
        HourlyQuality worstHour = null;
        float minQuality = 100;

        for (HourlyQuality hq : hourlyQuality) {
            if (hq.getSessionCount() > 1 && hq.getAvgQuality() < minQuality) {
                minQuality = hq.getAvgQuality();
                worstHour = hq;
            }
        }

        if (worstHour != null && minQuality < 50) {
            recommendations.add(new AIRecommendation(
                    AIRecommendation.RecommendationType.AVOID_HOUR,
                    "Low Energy Alert",
                    String.format(Locale.getDefault(),
                            "Avoid studying around %d:00 - energy dips to %.0f%%",
                            worstHour.getHour(), worstHour.getAvgQuality()),
                    R.drawable.ic_warning,
                    Color.parseColor("#FFF3E0")
            ));
        }

        // Check recent streak
        Calendar now = Calendar.getInstance();
        Calendar weekAgo = Calendar.getInstance();
        weekAgo.add(Calendar.DAY_OF_MONTH, -7);

        int recentDays = 0;
        for (StudySession session : sessions) {
            if (session.getTimestamp() >= weekAgo.getTimeInMillis()) {
                recentDays++;
            }
        }

        if (recentDays >= 7) {
            recommendations.add(new AIRecommendation(
                    AIRecommendation.RecommendationType.STREAK_ALERT,
                    "Streak Master! 🔥",
                    String.format(Locale.getDefault(),
                            "You've studied %d days this week. Keep it up!", recentDays),
                    R.drawable.ic_star,
                    Color.parseColor("#E3F2FD")
            ));
        }

        // Quality tip
        float avgQuality = 0;
        for (StudySession session : sessions) {
            avgQuality += session.getFocusScore();
        }
        avgQuality /= sessions.size();

        if (avgQuality >= QUALITY_THRESHOLD) {
            recommendations.add(new AIRecommendation(
                    AIRecommendation.RecommendationType.QUALITY_TIP,
                    "Quality Focus",
                    String.format(Locale.getDefault(),
                            "Your average focus is %.0f%% - excellent work!", avgQuality),
                    R.drawable.ic_check_circle,
                    Color.parseColor("#F3E5F5")
            ));
        }

        return recommendations;
    }

    public static List<StudySession> getTodaySessions(List<StudySession> allSessions) {
        List<StudySession> todaySessions = new ArrayList<>();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        long startOfDay = today.getTimeInMillis();

        for (StudySession session : allSessions) {
            if (session.getTimestamp() >= startOfDay) {
                todaySessions.add(session);
            }
        }

        return todaySessions;
    }
}
