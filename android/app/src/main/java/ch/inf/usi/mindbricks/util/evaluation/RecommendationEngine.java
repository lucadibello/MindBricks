package ch.inf.usi.mindbricks.util.evaluation;

import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.util.database.CalendarIntegrationHelper;
import ch.inf.usi.mindbricks.model.evaluation.UserPreferences;
import ch.inf.usi.mindbricks.model.visual.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;
import ch.inf.usi.mindbricks.util.UserPreferencesManager;
import ch.inf.usi.mindbricks.util.database.DataProcessor;

/**
 * Adaptive AI Recommendation Engine
 *
 * This is the central class that integrates:
 * - User preferences from JSON configuration
 * - PAM score analysis and trends
 * - Calendar events and constraints
 * - Historical productivity patterns
 * - All adaptive managers (Break, Task Difficulty, Affective Feedback, Environmental Cues)
 *
 */
public class RecommendationEngine {

    private static final String TAG = "AdaptiveAIEngine";
    private final UserPreferencesManager preferencesManager;
    private final AppDatabase database;
    private final CalendarIntegrationHelper calendarHelper;

    public RecommendationEngine(Context context) {
        this.preferencesManager = new UserPreferencesManager(context);
        this.database = AppDatabase.getInstance(context);
        this.calendarHelper = new CalendarIntegrationHelper(context);
    }

    public AIRecommendation generateAdaptiveSchedule(List<StudySessionWithStats> allSessions,
                                                     long targetDate) {

        Log.i(TAG, "Generating adaptive schedule for date: " + targetDate);

        AIRecommendation schedule = new AIRecommendation();
        UserPreferences prefs = preferencesManager.loadPreferences();

        List<CalendarEvent> calendarEvents = calendarHelper.getEventsInRange(getStartOfDay(targetDate), getEndOfDay(targetDate));
        AIRecommendation.ActivityType[] hourlyActivities = new AIRecommendation.ActivityType[24];

        applyCalendarConstraints(hourlyActivities, calendarEvents, prefs);
        applyUserSchedulePreferences(hourlyActivities, prefs, targetDate);

        float avgProductivity = analyzeProductivityPatterns(allSessions, hourlyActivities, schedule);

        applyAdaptiveRecommendations(hourlyActivities, allSessions, prefs);
        fillRemainingSlots(hourlyActivities, prefs);
        convertToActivityBlocks(schedule, hourlyActivities, avgProductivity);

        String summary = generateSummaryMessage(schedule, prefs, calendarEvents);
        schedule.setSummaryMessage(summary);

        Log.i(TAG, "Adaptive schedule generated successfully");
        return schedule;
    }

    private void applyCalendarConstraints(AIRecommendation.ActivityType[] hourlyActivities,
                                          List<CalendarEvent> events,
                                          UserPreferences prefs) {

        if (!prefs.getCalendarIntegration().isEnabled() || events == null) {
            return;
        }

        UserPreferences.CalendarIntegration calConfig = prefs.getCalendarIntegration();

        for (CalendarEvent event : events) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(event.getStartTime());

            int startHour = cal.get(Calendar.HOUR_OF_DAY);
            cal.setTimeInMillis(event.getEndTime());
            int endHour = cal.get(Calendar.HOUR_OF_DAY);

            // Add buffer time
            if (calConfig.getBufferBeforeEvent() > 0) {
                startHour = Math.max(0, startHour - (calConfig.getBufferBeforeEvent() / 60));
            }
            if (calConfig.getBufferAfterEvent() > 0) {
                endHour = Math.min(23, endHour + (calConfig.getBufferAfterEvent() / 60));
            }

            // Mark these hours as calendar events
            for (int h = startHour; h <= endHour && h < 24; h++) {
                hourlyActivities[h] = AIRecommendation.ActivityType.CALENDAR_EVENT;
            }

            Log.i(TAG, String.format("Calendar block: %d:00-%d:00 (%s)",
                    startHour, endHour, event.getTitle()));
        }
    }

    private void applyUserSchedulePreferences(AIRecommendation.ActivityType[] hourlyActivities,
                                              UserPreferences prefs,
                                              long targetDate) {

        // Apply sleep schedule
        UserPreferences.SleepSchedule sleep = prefs.getSleepSchedule();
        if (sleep != null) {
            for (int h = 0; h < 24; h++) {
                if (hourlyActivities[h] == null && sleep.isSleepTime(h)) {
                    hourlyActivities[h] = AIRecommendation.ActivityType.SLEEP;
                }
            }
        }

        // Apply meal times
        UserPreferences.MealTimes meals = prefs.getMealTimes();
        if (meals != null) {
            List<UserPreferences.MealTime> enabledMeals = meals.getAllEnabledMeals();
            for (UserPreferences.MealTime meal : enabledMeals) {
                int hour = meal.getHour();
                if (hour >= 0 && hour < 24 && hourlyActivities[hour] == null) {
                    hourlyActivities[hour] = AIRecommendation.ActivityType.MEALS;
                }
            }
        }

        // Apply work schedule
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(targetDate);
        String dayOfWeek = getDayOfWeekString(cal);

        UserPreferences.WorkSchedule work = prefs.getWorkSchedule();
        if (work != null && work.isEnabled() && work.isWorkDay(dayOfWeek)) {
            int startHour = work.getStartTime().getHour();
            int endHour = work.getEndTime().getHour();

            for (int h = startHour; h < endHour && h < 24; h++) {
                // Only block if not allowing study during work
                if (!work.isAllowStudyDuringWork() && hourlyActivities[h] == null) {
                    hourlyActivities[h] = AIRecommendation.ActivityType.WORK;
                }
            }
        }

        // Apply exercise schedule
        UserPreferences.ExerciseSchedule exercise = prefs.getExerciseSchedule();
        if (exercise != null && exercise.isEnabled() && exercise.getPreferredTimes() != null) {
            for (UserPreferences.ExerciseBlock block : exercise.getPreferredTimes()) {
                int hour = block.getHour();
                if (hour >= 0 && hour < 24 && hourlyActivities[hour] == null) {
                    hourlyActivities[hour] = AIRecommendation.ActivityType.EXERCISE;
                }
            }
        }

        // Apply social time
        UserPreferences.SocialTime social = prefs.getSocialTime();
        if (social != null && social.isEnabled() && social.isProtectFromStudy()) {
            List<Integer> socialHours = social.getPreferredHours();
            if (socialHours != null && social.getPreferredDays().contains(dayOfWeek.toUpperCase())) {
                for (int hour : socialHours) {
                    if (hour >= 0 && hour < 24 && hourlyActivities[hour] == null) {
                        hourlyActivities[hour] = AIRecommendation.ActivityType.SOCIAL;
                    }
                }
            }
        }
    }

    private float analyzeProductivityPatterns(List<StudySessionWithStats> allSessions,
                                              AIRecommendation.ActivityType[] hourlyActivities,
                                              AIRecommendation schedule) {

        List<TimeSlotStats> hourlyStats = DataProcessor.calculateHourlyDistribution(
                allSessions,
                null
        );

        float totalProductivity = 0;
        int hoursWithData = 0;

        for (TimeSlotStats stats : hourlyStats) {
            if (stats.getSessionCount() > 0) {
                totalProductivity += stats.getAverageFocusScore();
                hoursWithData++;
            }
        }

        float avgProductivity = hoursWithData > 0 ? (totalProductivity / hoursWithData) : 50f;
        schedule.setAverageProductivity(avgProductivity);
        schedule.setTotalSessions(allSessions.size());

        // Identify high-productivity hours
        for (TimeSlotStats stats : hourlyStats) {
            int hour = stats.getHourOfDay();
            if (hour >= 0 && hour < 24 && hourlyActivities[hour] == null) {

                // High productivity (top 30%)
                // Deep Study
                if (stats.getAverageFocusScore() >= avgProductivity * 1.2f) {
                    hourlyActivities[hour] = AIRecommendation.ActivityType.DEEP_STUDY;
                }
                // Good productivity (above average)
                // Light Study
                else if (stats.getAverageFocusScore() >= avgProductivity) {
                    hourlyActivities[hour] = AIRecommendation.ActivityType.LIGHT_STUDY;
                }
            }
        }

        return avgProductivity;
    }

    private void applyAdaptiveRecommendations(AIRecommendation.ActivityType[] hourlyActivities,
                                              List<StudySessionWithStats> allSessions,
                                              UserPreferences prefs) {

        // Get recent PAM scores
        List<PAMScore> recentPAM = database.pamScoreDao().getLastNScores(7);

        if (recentPAM == null || recentPAM.isEmpty()) {
            return; // No PAM data yet
        }

        // Analyze PAM trends
        TaskDifficultyRecommender.TaskRecommendation taskRec =
                TaskDifficultyRecommender.analyzeRecentSessions(recentPAM, prefs.getPamThresholds());

        // If user is struggling (consecutive low sessions), reduce deep study hours
        if (taskRec.getAction() == TaskDifficultyRecommender.RecommendationAction.REDUCE_DIFFICULTY ||
                taskRec.getAction() == TaskDifficultyRecommender.RecommendationAction.TAKE_LONGER_BREAK) {

            // Convert some DEEP_STUDY to LIGHT_STUDY
            for (int h = 0; h < 24; h++) {
                if (hourlyActivities[h] == AIRecommendation.ActivityType.DEEP_STUDY) {
                    hourlyActivities[h] = AIRecommendation.ActivityType.LIGHT_STUDY;
                }
            }

            Log.i(TAG, "Adaptive: Reduced deep study hours due to PAM trends");
        }

        // Check daily PAM average
        Float todayAvg = database.pamScoreDao().getAverageScoreForDay(
                getStartOfDay(System.currentTimeMillis()),
                getEndOfDay(System.currentTimeMillis())
        );

        if (todayAvg != null && todayAvg < prefs.getPamThresholds().getLowThreshold()) {
            // Low PAM day → Add more breaks
            addMoreBreaks(hourlyActivities);
            Log.i(TAG, "Adaptive: Added extra breaks due to low daily PAM average");
        }
    }

    private void fillRemainingSlots(AIRecommendation.ActivityType[] hourlyActivities,
                                    UserPreferences prefs) {

        UserPreferences.StudyPreferences studyPrefs = prefs.getStudyPreferences();

        for (int h = 0; h < 24; h++) {
            if (hourlyActivities[h] == null) {

                // Morning (6-12)
                if (h >= 6 && h < 12) {
                    if (studyPrefs != null &&
                            studyPrefs.getPreferredStudyTimes().getMorning().isEnabled() &&
                            h >= studyPrefs.getPreferredStudyTimes().getMorning().getStartHour() &&
                            h < studyPrefs.getPreferredStudyTimes().getMorning().getEndHour()) {
                        hourlyActivities[h] = AIRecommendation.ActivityType.LIGHT_STUDY;
                    } else {
                        hourlyActivities[h] = AIRecommendation.ActivityType.BREAKS;
                    }
                }
                // Afternoon (12-18)
                else if (h >= 12 && h < 18) {
                    if (studyPrefs != null &&
                            studyPrefs.getPreferredStudyTimes().getAfternoon().isEnabled() &&
                            h >= studyPrefs.getPreferredStudyTimes().getAfternoon().getStartHour() &&
                            h < studyPrefs.getPreferredStudyTimes().getAfternoon().getEndHour()) {
                        hourlyActivities[h] = AIRecommendation.ActivityType.LIGHT_STUDY;
                    } else {
                        hourlyActivities[h] = AIRecommendation.ActivityType.WORK;
                    }
                }
                // Evening (18-24)
                else if (h >= 18 && h < 24) {
                    if (studyPrefs != null &&
                            studyPrefs.getPreferredStudyTimes().getEvening().isEnabled() &&
                            h >= studyPrefs.getPreferredStudyTimes().getEvening().getStartHour() &&
                            h < studyPrefs.getPreferredStudyTimes().getEvening().getEndHour()) {
                        hourlyActivities[h] = AIRecommendation.ActivityType.LIGHT_STUDY;
                    } else {
                        hourlyActivities[h] = AIRecommendation.ActivityType.SOCIAL;
                    }
                }
                // Late night/Early morning (0-6)
                else {
                    hourlyActivities[h] = AIRecommendation.ActivityType.SLEEP;
                }
            }
        }
    }

    private void convertToActivityBlocks(AIRecommendation schedule,
                                         AIRecommendation.ActivityType[] hourlyActivities,
                                         float avgProductivity) {

        AIRecommendation.ActivityType currentActivity = hourlyActivities[0];
        int blockStart = 0;

        for (int h = 1; h < 24; h++) {
            if (hourlyActivities[h] != currentActivity) {
                // End current block, start new one
                schedule.addActivityBlock(new AIRecommendation.ActivityBlock(
                        currentActivity,
                        blockStart,
                        h,
                        calculateConfidence(currentActivity),
                        getActivityReason(currentActivity, avgProductivity)
                ));

                currentActivity = hourlyActivities[h];
                blockStart = h;
            }
        }

        // Add final block
        schedule.addActivityBlock(new AIRecommendation.ActivityBlock(
                currentActivity,
                blockStart,
                24,
                calculateConfidence(currentActivity),
                getActivityReason(currentActivity, avgProductivity)
        ));
    }

    private String generateSummaryMessage(AIRecommendation schedule,
                                          UserPreferences prefs,
                                          List<CalendarEvent> events) {

        int totalSessions = schedule.getTotalSessions();
        int availableHours = schedule.getAvailableHours();
        int calendarBlocked = schedule.getCalendarBlockedHours();

        StringBuilder summary = new StringBuilder();

        if (totalSessions == 0) {
            summary.append("Start tracking sessions to get personalized recommendations!");
        } else {
            summary.append(String.format("Based on %d sessions, ", totalSessions));

            if (availableHours < 6) {
                summary.append(String.format("you have %d hours available today (busy day!). ", availableHours));
            } else {
                summary.append(String.format("you have %d hours available for focused work. ", availableHours));
            }

            if (calendarBlocked > 0) {
                summary.append(String.format("%d hours blocked by calendar events. ", calendarBlocked));
            }
        }

        List<PAMScore> recentScores = database.pamScoreDao().getLastNScores(3);
        if (recentScores != null && !recentScores.isEmpty()) {
            float avgRecent = 0;
            for (PAMScore score : recentScores) {
                avgRecent += score.getTotalScore();
            }
            avgRecent /= recentScores.size();

            if (avgRecent < prefs.getPamThresholds().getLowThreshold()) {
                summary.append("Your recent energy has been low - schedule extra breaks today.");
            } else if (avgRecent >= prefs.getPamThresholds().getHighThreshold()) {
                summary.append("You're in great form - make the most of your peak hours!");
            }
        }

        return summary.toString();
    }


    private void addMoreBreaks(AIRecommendation.ActivityType[] hourlyActivities) {
        // Convert every other LIGHT_STUDY to BREAKS
        for (int h = 0; h < 24; h += 2) {
            if (hourlyActivities[h] == AIRecommendation.ActivityType.LIGHT_STUDY) {
                hourlyActivities[h] = AIRecommendation.ActivityType.BREAKS;
            }
        }
    }

    private int calculateConfidence(AIRecommendation.ActivityType activity) {
        // Calendar events and sleep are 100% confident
        if (activity.isProtected()) {
            return 100;
        }
        // Study activities based on data have medium confidence
        if (activity == AIRecommendation.ActivityType.DEEP_STUDY ||
                activity == AIRecommendation.ActivityType.LIGHT_STUDY) {
            return 70;
        }
        // Other activities are lower confidence
        return 50;
    }

    private String getActivityReason(AIRecommendation.ActivityType activity, float avgProductivity) {
        switch (activity) {
            case DEEP_STUDY:
                return "Peak productivity period based on your history";
            case LIGHT_STUDY:
                return "Good time for learning and review";
            case WORK:
                return "Scheduled work time";
            case EXERCISE:
                return "Scheduled exercise time";
            case SOCIAL:
                return "Social/relaxation time";
            case MEALS:
                return "Meal time";
            case BREAKS:
                return "Rest period";
            case SLEEP:
                return "Sleep schedule";
            case CALENDAR_EVENT:
                return "Calendar commitment";
            default:
                return "Recommended activity";
        }
    }

    private String getDayOfWeekString(Calendar cal) {
        String[] days = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        return days[cal.get(Calendar.DAY_OF_WEEK) - 1];
    }

    private long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getEndOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}