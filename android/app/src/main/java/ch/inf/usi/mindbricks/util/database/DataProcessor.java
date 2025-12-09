package ch.inf.usi.mindbricks.util.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.DailyRings;
import ch.inf.usi.mindbricks.model.visual.DateRange;
import ch.inf.usi.mindbricks.model.visual.DailyRecommendation;
import ch.inf.usi.mindbricks.model.visual.HeatmapCell;
import ch.inf.usi.mindbricks.model.visual.HourlyQuality;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.model.visual.WeeklyStats;
import ch.inf.usi.mindbricks.model.visual.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.StreakDay;
import ch.inf.usi.mindbricks.model.visual.GoalRing;

/**
 * Utility class for processing and analyzing study session data.
 * Contains methods to transform raw session data into chart-ready statistics.
 */
public class DataProcessor {

    public static WeeklyStats calculateWeeklyStats(List<StudySessionWithStats> allSessions, DateRange dateRange) {
        WeeklyStats stats = new WeeklyStats();
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        if (sessions.isEmpty()) {
            return stats;
        }

        int[] minutesPerDay = new int[7];
        float[] focusScoreSum = new float[7];
        int[] sessionCountPerDay = new int[7];

        Calendar calendar = Calendar.getInstance();

        // Process each session
        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());

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
            stats.setDayMinutes(i,0);
            stats.setDaySessionCount(i, sessionCountPerDay[i]);

            if (sessionCountPerDay[i] > 0) {
                float avgFocusScore = focusScoreSum[i] / sessionCountPerDay[i];
                stats.setDayMinutes(i, minutesPerDay[i]/sessionCountPerDay[i]);
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

    public static List<TimeSlotStats> calculateHourlyDistribution(List<StudySessionWithStats> allSessions, DateRange dateRange) {        // Create 24 time slots (one for each hour)
        List<TimeSlotStats> hourlyStats = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyStats.add(new TimeSlotStats(hour));
        }

        // Filter sessions to only include those within the date range
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        if (sessions.isEmpty()) {
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

        // Log statistics for debugging
        int activeHours = 0;
        for (TimeSlotStats stats : hourlyStats) {
            if (stats.getSessionCount() > 0) {
                activeHours++;
            }
        }
        return hourlyStats;
    }

    public static DailyRecommendation generateDailyRecommendation(List<StudySessionWithStats> allSessions, DateRange dateRange) {
        DailyRecommendation recommendation = new DailyRecommendation();
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        if (sessions.isEmpty()) {
            recommendation.setReasonSummary(
                    "Not enough data yet. Complete more sessions to get personalized recommendations.");
            recommendation.setConfidenceScore(0);
            recommendation.setRecommendedSlots(new ArrayList<>());
            return recommendation;
        }

        // Calculate hourly distribution to find best times
        List<TimeSlotStats> hourlyStats = calculateHourlyDistribution(allSessions, dateRange);

        // Find hours with sessions and rank by focus score
        List<TimeSlotStats> rankedHours = new ArrayList<>();
        for (TimeSlotStats stats : hourlyStats) {
            if (stats.getSessionCount() > 0) {
                rankedHours.add(stats);
            }
        }

        // Sort by focus score (descending) - best hours first
        Collections.sort(rankedHours, (a, b) ->
                Float.compare(b.getAverageFocusScore(), a.getAverageFocusScore()));

        // Create recommended time slots (top 3)
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

    public static List<HourlyQuality> calculateEnergyCurve(List<StudySessionWithStats> sessions) {
        List<HourlyQuality> hourlyData = new ArrayList<>();

        // Initialize 24-hour structure
        float[] totalQuality = new float[24];
        int[] sessionCounts = new int[24];

        Calendar calendar = Calendar.getInstance();

        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            totalQuality[hour] += session.getFocusScore();
            sessionCounts[hour]++;
        }

        // Calculate averages for each hour
        for (int hour = 0; hour < 24; hour++) {
            float avgQuality = sessionCounts[hour] > 0
                    ? totalQuality[hour] / sessionCounts[hour]
                    : 0;

            hourlyData.add(new HourlyQuality(hour, avgQuality, sessionCounts[hour]));
        }

        return hourlyData;
    }

    public static List<HeatmapCell> calculateQualityHeatmap(List<StudySessionWithStats> sessions, DateRange dateRange) {
        Map<String, HeatmapCell> cellMap = new HashMap<>();
        Calendar calendar = Calendar.getInstance();

        // STEP 1: Process actual session data first
        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            String key = year + "-" + month + "-" + dayOfMonth + "-" + hour;

            HeatmapCell cell = cellMap.get(key);
            if (cell == null) {
                cell = new HeatmapCell(dayOfMonth, hour, 0, 0);
                cellMap.put(key, cell);
            }

            cell.setAvgQuality(cell.getAvgQuality() + session.getFocusScore());
            cell.setSessionCount(cell.getSessionCount() + 1);
        }

        calendar.setTimeInMillis(dateRange.getStartTimestamp());
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(dateRange.getEndTimestamp());

        while (calendar.before(endCalendar) || calendar.equals(endCalendar)) {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            for (int hour = 0; hour < 24; hour++) {
                String key = year + "-" + month + "-" + dayOfMonth + "-" + hour;

                if (!cellMap.containsKey(key)) {
                    cellMap.put(key, new HeatmapCell(dayOfMonth, hour, 0, 0));
                }
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Calculate averages
        List<HeatmapCell> result = new ArrayList<>();
        for (HeatmapCell cell : cellMap.values()) {
            if (cell.getSessionCount() > 0) {
                cell.setAvgQuality(cell.getAvgQuality() / cell.getSessionCount());
            }
            result.add(cell);
        }

        return result;
    }

    public static List<StreakDay> calculateStreakCalendar(List<StudySessionWithStats> sessions,
                                                          int targetMinutes,
                                                          int month,
                                                          int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);

        // Get the number of days in this month
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        Map<String, StreakDay> dayMap = new HashMap<>();
        for (int day = 1; day <= daysInMonth; day++) {
            String key = year + "-" + month + "-" + day;
            StreakDay streakDay = new StreakDay(day, month, year);
            streakDay.setStatus(StreakDay.StreakStatus.NONE);
            dayMap.put(key, streakDay);
        }

        // Filter sessions to only this month
        List<StudySessionWithStats> monthSessions = new ArrayList<>();
        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());
            if (calendar.get(Calendar.MONTH) == month &&
                    calendar.get(Calendar.YEAR) == year) {
                monthSessions.add(session);
            }
        }

        for (StudySessionWithStats session : monthSessions) {
            calendar.setTimeInMillis(session.getTimestamp());

            int day = calendar.get(Calendar.DAY_OF_MONTH);
            String key = year + "-" + month + "-" + day;

            StreakDay streakDay = dayMap.get(key);
            if (streakDay != null) {
                // Accumulate session data
                streakDay.setTotalMinutes(streakDay.getTotalMinutes() + session.getDurationMinutes());
                streakDay.setAvgQuality(streakDay.getAvgQuality() + session.getFocusScore());
                streakDay.setSessionCount(streakDay.getSessionCount() + 1);
            }
        }

        // Calculate status for each day
        List<StreakDay> result = new ArrayList<>();
        for (StreakDay day : dayMap.values()) {
            if (day.getSessionCount() > 0) {
                day.setAvgQuality(day.getAvgQuality() / day.getSessionCount());
            }

            if (day.getTotalMinutes() == 0) {
                day.setStatus(StreakDay.StreakStatus.NONE);
            } else if (day.getTotalMinutes() < targetMinutes * 0.5) {
                day.setStatus(StreakDay.StreakStatus.PARTIAL);
            } else if (day.getTotalMinutes() >= targetMinutes * 1.5) {
                day.setStatus(StreakDay.StreakStatus.EXCEPTIONAL);
            } else {
                day.setStatus(StreakDay.StreakStatus.HIT_TARGET);
            }

            result.add(day);
        }

        Collections.sort(result, (a, b) -> {
            if (a.getYear() != b.getYear()) return a.getYear() - b.getYear();
            if (a.getMonth() != b.getMonth()) return a.getMonth() - b.getMonth();
            return a.getDayOfMonth() - b.getDayOfMonth();
        });

        return result;
    }

    public static List<DailyRings> calculateDailyRingsHistory(
            Context context,
            List<StudySessionWithStats> allSessions,
            DateRange dateRange,
            int dailyMinutesTarget,
            float dailyFocusTarget) {

        List<DailyRings> result = new ArrayList<>();

        if (allSessions == null || allSessions.isEmpty()) {
            return createEmptyDailyRings(context, dateRange, dailyMinutesTarget, dailyFocusTarget);
        }

        // Group sessions by date
        Map<String, List<StudySessionWithStats>> sessionsByDate = new HashMap<>();

        for (StudySessionWithStats session : allSessions) {
            if (!dateRange.contains(session.getTimestamp())) {
                continue;
            }

            @SuppressLint("DefaultLocale") String dateKey = String.format("%d-%02d-%02d",
                    session.getYear(),
                    session.getMonth(),
                    session.getDayOfMonth());

            sessionsByDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(session);
        }

        // Get date range boundaries
        Calendar startCal = getStartCalendar(dateRange);
        Calendar endCal = getEndCalendar(dateRange);

        // Create DailyRings for each day in range (newest first)
        Calendar currentCal = (Calendar) endCal.clone();

        while (!currentCal.before(startCal)) {
            int year = currentCal.get(Calendar.YEAR);
            int month = currentCal.get(Calendar.MONTH) + 1;
            int day = currentCal.get(Calendar.DAY_OF_MONTH);

            LocalDate date = LocalDate.of(year, month, day);
            @SuppressLint("DefaultLocale") String dateKey = String.format( "%d-%02d-%02d", year, month, day);

            // Get sessions for this day (or empty list)
            List<StudySessionWithStats> sessionsForDay = sessionsByDate.getOrDefault(dateKey, new ArrayList<>());

            // Calculate rings for this day
            List<GoalRing> rings = calculateGoalRings(
                    context,
                    sessionsForDay,
                    dailyMinutesTarget,
                    dailyFocusTarget
            );

            DailyRings dayData = new DailyRings(date, rings);
            result.add(dayData);

            // Move to previous day
            currentCal.add(Calendar.DAY_OF_MONTH, -1);
        }

        return result;
    }
    
    private static Calendar getStartCalendar(DateRange dateRange) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateRange.getStartTimestamp());

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private static Calendar getEndCalendar(DateRange dateRange) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateRange.getEndTimestamp());

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);

        return calendar;
    }


    private static List<DailyRings> createEmptyDailyRings(
            Context context,
            DateRange dateRange,
            int dailyMinutesTarget,
            float dailyFocusTarget) {

        List<DailyRings> result = new ArrayList<>();

        Calendar startCal = getStartCalendar(dateRange);
        Calendar endCal = getEndCalendar(dateRange);

        // Start from end date and go backwards to start date
        Calendar currentCal = (Calendar) endCal.clone();

        while (!currentCal.before(startCal)) {
            int year = currentCal.get(Calendar.YEAR);
            int month = currentCal.get(Calendar.MONTH) + 1;
            int day = currentCal.get(Calendar.DAY_OF_MONTH);

            LocalDate date = LocalDate.of(year, month, day);

            List<GoalRing> emptyRings = calculateGoalRings(
                    context,
                    new ArrayList<>(),
                    dailyMinutesTarget,
                    dailyFocusTarget
            );

            DailyRings dayData = new DailyRings(date, emptyRings);
            result.add(dayData);

            currentCal.add(Calendar.DAY_OF_MONTH, -1);
        }

        return result;
    }

    public static List<GoalRing> calculateGoalRings(
            Context context,
            List<StudySessionWithStats> sessions,
            int dailyMinutesTarget,
            float dailyFocusTarget) {

        List<GoalRing> rings = new ArrayList<>();

        // Calculate totals from sessions
        int totalMinutes = 0;
        float totalFocus = 0;
        int sessionCount = 0;

        for (StudySessionWithStats session : sessions) {
            totalMinutes += session.getDurationMinutes();
            totalFocus += session.getFocusScore();
            sessionCount++;
        }

        float avgFocus = sessionCount > 0 ? totalFocus / sessionCount : 0;

        rings.add(new GoalRing(
                "Study Time",
                totalMinutes,
                dailyMinutesTarget,
                "min"
        ));

        rings.add(new GoalRing(
                "Focus Quality",
                avgFocus,
                dailyFocusTarget,
                "%"
        ));

        // TODO -> setup target due to the questionare
        rings.add(new GoalRing(
                "Sessions",
                sessionCount,
                5,
                "sessions"
        ));

        return rings;
    }

    //"ai" generation
    public static AIRecommendation generateAIRecommendations(
            List<StudySessionWithStats> allSessions, DateRange dateRange) {

        AIRecommendation schedule = new AIRecommendation();

        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);
        schedule.setTotalSessions(sessions.size());

        if (sessions.isEmpty()) {
            schedule.setSummaryMessage("Not enough data. Complete more study sessions for personalized schedule.");
            return schedule;
        }

        // Calculate hourly productivity stats
        List<TimeSlotStats> hourlyStats = calculateHourlyDistribution(allSessions, dateRange);

        // Find productive and unproductive hours
        List<HourScore> hourScores = new ArrayList<>();
        for (TimeSlotStats stats : hourlyStats) {
            if (stats.getSessionCount() > 0) {
                hourScores.add(new HourScore(
                        stats.getHourOfDay(),
                        stats.getAverageFocusScore(),
                        stats.getSessionCount()
                ));
            }
        }

        Collections.sort(hourScores, (a, b) ->
                Float.compare(b.focusScore, a.focusScore));

        // Calculate average productivity
        float avgProductivity = 0;
        for (HourScore score : hourScores) {
            avgProductivity += score.focusScore;
        }
        if (!hourScores.isEmpty()) {
            avgProductivity /= hourScores.size();
            schedule.setAverageProductivity(avgProductivity);
        }

        // Generate activity blocks based on patterns
        generateActivityBlocks(schedule, hourScores, avgProductivity);

        // Generate summary message
        String summary = generateScheduleSummary(schedule, hourScores);
        schedule.setSummaryMessage(summary);

        return schedule;
    }

    private static void generateActivityBlocks(AIRecommendation schedule,
                                               List<HourScore> hourScores,
                                               float avgProductivity) {

        AIRecommendation.ActivityType[] hourlyActivities =
                new AIRecommendation.ActivityType[24];

        // TODO add values to be customizable
        // Default schedule based on typical patterns
        // Sleep hours
        for (int h = 0; h < 6; h++) {
            hourlyActivities[h] = AIRecommendation.ActivityType.SLEEP;
        }
        for (int h = 23; h < 24; h++) {
            hourlyActivities[h] = AIRecommendation.ActivityType.SLEEP;
        }

        // Meal times
        hourlyActivities[7] = AIRecommendation.ActivityType.MEALS;
        hourlyActivities[12] = AIRecommendation.ActivityType.MEALS;
        hourlyActivities[19] = AIRecommendation.ActivityType.MEALS;


        //  TODO based on the questions the ifs will be deterministic.
        // -> adding a json with the setup and reading it from it?
        // -> setting up as static variables in Dataprocessor on creation?

        // Now override based on productivity
        for (HourScore score : hourScores) {
            int hour = score.hour;

            // Skip sleep and meal hours
            if (hour >= 0 && hour < 6) continue;
            if (hour == 23) continue;
            if (hour == 7 || hour == 12 || hour == 19) continue;

            // High productivity hours -> top 30% -> Deep Study
            if (score.focusScore >= avgProductivity * 1.2f) {
                hourlyActivities[hour] = AIRecommendation.ActivityType.DEEP_STUDY;
            }
            // Good productivity -> above average -> Light Study or Work
            else if (score.focusScore >= avgProductivity) {
                // Alternate between light study and work
                if (hour % 2 == 0) {
                    hourlyActivities[hour] = AIRecommendation.ActivityType.LIGHT_STUDY;
                } else {
                    hourlyActivities[hour] = AIRecommendation.ActivityType.WORK;
                }
            }

            // Below average productivity -> Other activities
            else {
                // Morning/afternoon low productivity -> Exercise
                if (hour >= 6 && hour <= 11 && hourlyActivities[hour] == null) {
                    hourlyActivities[hour] = AIRecommendation.ActivityType.EXERCISE;
                }
                // Evening low productivity -> Social
                else if (hour >= 18 && hour <= 22) {
                    //TODO replace with calendar activities
                    hourlyActivities[hour] = AIRecommendation.ActivityType.SOCIAL;
                }
                // Otherwise -> Breaks
                else if (hourlyActivities[hour] == null) {
                    hourlyActivities[hour] = AIRecommendation.ActivityType.BREAKS;
                }
            }
        }

        // Fill remaining null slots with sensible defaults
        for (int h = 6; h < 23; h++) {
            if (hourlyActivities[h] == null) {
                if (h >= 8 && h <= 10) {
                    hourlyActivities[h] = AIRecommendation.ActivityType.WORK;
                } else if (h >= 13 && h <= 17) {
                    hourlyActivities[h] = AIRecommendation.ActivityType.LIGHT_STUDY;
                } else if (h >= 20 && h <= 22) {
                    hourlyActivities[h] = AIRecommendation.ActivityType.SOCIAL;
                } else {
                    hourlyActivities[h] = AIRecommendation.ActivityType.BREAKS;
                }
            }
        }

        // Convert hour array into activity blocks -> merge consecutive same activities
        AIRecommendation.ActivityType currentActivity = hourlyActivities[0];
        int blockStart = 0;

        for (int h = 1; h < 24; h++) {
            if (hourlyActivities[h] != currentActivity) {
                int confidence = calculateBlockConfidence(hourScores, blockStart, h, currentActivity);
                String reason = generateBlockReason(currentActivity, blockStart, h, hourScores, avgProductivity);

                schedule.addActivityBlock(new AIRecommendation.ActivityBlock(
                        currentActivity, blockStart, h, confidence, reason
                ));

                currentActivity = hourlyActivities[h];
                blockStart = h;
            }
        }

        // Add final block
        int confidence = calculateBlockConfidence(hourScores, blockStart, 24, currentActivity);
        String reason = generateBlockReason(currentActivity, blockStart, 24, hourScores, avgProductivity);
        schedule.addActivityBlock(new AIRecommendation.ActivityBlock(
                currentActivity, blockStart, 24, confidence, reason
        ));
    }

    private static int calculateBlockConfidence(List<HourScore> hourScores,
                                                int startHour, int endHour,
                                                AIRecommendation.ActivityType activity) {
        // TODO based on questinnare add confidence

        // For study activities, confidence based on actual session data
        if (activity == AIRecommendation.ActivityType.DEEP_STUDY ||
                activity == AIRecommendation.ActivityType.LIGHT_STUDY) {

            int sessionsInBlock = 0;
            for (HourScore score : hourScores) {
                if (score.hour >= startHour && score.hour < endHour) {
                    sessionsInBlock += score.sessionCount;
                }
            }
            return Math.min(100, sessionsInBlock * 15); // 15 points per session
        }

        // For other activities, moderate confidence
        return 60;
    }

    private static String generateScheduleSummary(AIRecommendation schedule,
                                                  List<HourScore> hourScores) {
        if (hourScores.isEmpty()) {
            return "Start tracking your study sessions to get personalized recommendations!";
        }

        // Find best study hours
        List<Integer> bestHours = new ArrayList<>();
        for (int i = 0; i < Math.min(3, hourScores.size()); i++) {
            bestHours.add(hourScores.get(i).hour);
        }

        Collections.sort(bestHours);

        StringBuilder summary = new StringBuilder();
        summary.append("Based on ").append(schedule.getTotalSessions())
                .append(" sessions, your peak focus hours are ");

        for (int i = 0; i < bestHours.size(); i++) {
            int hour = bestHours.get(i);
            if (hour == 0) summary.append("12 AM");
            else if (hour < 12) summary.append(hour).append(" AM");
            else if (hour == 12) summary.append("12 PM");
            else summary.append(hour - 12).append(" PM");

            if (i < bestHours.size() - 2) summary.append(", ");
            else if (i == bestHours.size() - 2) summary.append(" and ");
        }

        summary.append(". Schedule deep work during these times!");

        return summary.toString();
    }

    private static String generateBlockReason(AIRecommendation.ActivityType activity,
                                              int startHour, int endHour,
                                              List<HourScore> hourScores,
                                              float avgProductivity) {

        switch (activity) {
            case DEEP_STUDY:
                float maxFocus = 0;
                for (HourScore score : hourScores) {
                    if (score.hour >= startHour && score.hour < endHour) {
                        maxFocus = Math.max(maxFocus, score.focusScore);
                    }
                }
                return String.format("Peak focus period (%.0f%% productivity)", maxFocus);

            case LIGHT_STUDY:
                return "Good productivity, suitable for learning";

            case WORK:
                return "Productive time for structured tasks";

            case EXERCISE:
                return "Lower focus period, ideal for physical activity";

            case SOCIAL:
                return "Evening relaxation time";

            case MEALS:
                return "Regular meal time";

            case BREAKS:
                return "Rest period to maintain energy";

            case SLEEP:
                return "Restorative sleep for optimal performance";

            default:
                return "Recommended activity";
        }
    }

    // helpers
    public static List<StudySessionWithStats> filterSessionsInRange(List<StudySessionWithStats> allSessions, DateRange dateRange) {
        if (allSessions == null || allSessions.isEmpty()) {
            return new ArrayList<>();
        }

        if (dateRange == null) {
            return new ArrayList<>(allSessions);
        }

        List<StudySessionWithStats> filtered = new ArrayList<>();
        int outOfRange = 0;

        for (StudySessionWithStats session : allSessions) {
            if (dateRange.contains(session.getTimestamp())) {
                filtered.add(session);
            } else {
                outOfRange++;
            }
        }

        return filtered;
    }

    public static int calculateTotalMinutes(List<StudySessionWithStats> allSessions, DateRange dateRange) {
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        int total = 0;
        for (StudySessionWithStats session : sessions) {
            total += session.getDurationMinutes();
        }

        return total;
    }

    public static float calculateAverageFocusScore(List<StudySessionWithStats> allSessions,
                                                   DateRange dateRange) {
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        if (sessions.isEmpty()) {
            return 0;
        }

        float sum = 0;
        for (StudySessionWithStats session : sessions) {
            sum += session.getFocusScore();
        }

        return sum / sessions.size();
    }

    public static Map<String, List<StudySessionWithStats>> groupSessionsByTag(List<StudySessionWithStats> allSessions,
                                                                     DateRange dateRange) {
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);
        Map<String, List<StudySessionWithStats>> grouped = new HashMap<>();

        for (StudySessionWithStats session : sessions) {
            String tag = session.getTagTitle();
            if (tag == null || tag.isEmpty()) {
                tag = "Untagged";
            }

            if (!grouped.containsKey(tag)) {
                grouped.put(tag, new ArrayList<>());
            }
            grouped.get(tag).add(session);
        }

        return grouped;
    }

    public static int calculateLongestStreak(List<StudySessionWithStats> allSessions, DateRange dateRange) {
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        if (sessions.isEmpty()) {
            return 0;
        }

        List<StudySessionWithStats> sortedSessions = new ArrayList<>(sessions);
        Collections.sort(sortedSessions, (a, b) ->
                Long.compare(a.getTimestamp(), b.getTimestamp()));

        int longestStreak = 1;
        int currentStreak = 1;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(sortedSessions.get(0).getTimestamp());
        int lastDayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        int lastYear = cal.get(Calendar.YEAR);

        for (int i = 1; i < sortedSessions.size(); i++) {
            cal.setTimeInMillis(sortedSessions.get(i).getTimestamp());
            int currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR);
            int currentYear = cal.get(Calendar.YEAR);

            if (currentYear == lastYear && currentDayOfYear == lastDayOfYear + 1) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else if (currentYear == lastYear + 1 && currentDayOfYear == 1 &&
                    lastDayOfYear == 365) {
                // Year boundary case (Dec 31 -> Jan 1)
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

    private static class HourScore {
        int hour;
        float focusScore;
        int sessionCount;

        HourScore(int hour, float focusScore, int sessionCount) {
            this.hour = hour;
            this.focusScore = focusScore;
            this.sessionCount = sessionCount;
        }
    }


}