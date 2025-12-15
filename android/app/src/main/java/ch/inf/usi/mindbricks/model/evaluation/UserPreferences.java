package ch.inf.usi.mindbricks.model.evaluation;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Main user preferences model that maps to the JSON configuration file.
 * This class centralizes all user-configurable schedule and behavior preferences
 */
public class UserPreferences {

    private String version;
    private String lastUpdated;

    private SleepSchedule sleepSchedule;
    private MealTimes mealTimes;
    private WorkSchedule workSchedule;
    private StudyPreferences studyPreferences;
    private BreakPreferences breakPreferences;
    private ExerciseSchedule exerciseSchedule;
    private SocialTime socialTime;
    private EnvironmentalPreferences environmentalPreferences;
    private PAMThresholds pamThresholds;
    private CalendarIntegration calendarIntegration;
    private PersonalGoals personalGoals;
    private Advanced advanced;

    public static class TimeOfDay {
        private int hour;
        private int minute;

        public TimeOfDay() {}

        public TimeOfDay(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
        }

        public int getHour() { return hour; }
        public int getMinute() { return minute; }
        public void setHour(int hour) { this.hour = hour; }
        public void setMinute(int minute) { this.minute = minute; }

        public int toMinuteOfDay() {
            return hour * 60 + minute;
        }

        public boolean isBetween(TimeOfDay start, TimeOfDay end) {
            int thisMinutes = this.toMinuteOfDay();
            int startMinutes = start.toMinuteOfDay();
            int endMinutes = end.toMinuteOfDay();

            if (startMinutes <= endMinutes) {
                return thisMinutes >= startMinutes && thisMinutes <= endMinutes;
            } else {
                // Handles overnight ranges (e.g., 23:00 to 02:00)
                return thisMinutes >= startMinutes || thisMinutes <= endMinutes;
            }
        }

        @Override
        public String toString() {
            return String.format("%02d:%02d", hour, minute);
        }
    }

    public static class SleepSchedule {
        private TimeOfDay bedtime;
        private TimeOfDay wakeupTime;
        private double targetSleepHours;
        private boolean allowFlexibility;
        private int flexibilityMinutes;

        public TimeOfDay getBedtime() { return bedtime; }
        public TimeOfDay getWakeupTime() { return wakeupTime; }
        public double getTargetSleepHours() { return targetSleepHours; }
        public boolean isAllowFlexibility() { return allowFlexibility; }
        public int getFlexibilityMinutes() { return flexibilityMinutes; }

        /**
         * Checks if given hour falls within sleep time
         */
        public boolean isSleepTime(int hour) {
            int bedtimeHour = bedtime.getHour();
            int wakeupHour = wakeupTime.getHour();

            if (bedtimeHour < wakeupHour) {
                // Normal case: sleep 22:00 to 06:00
                return hour >= bedtimeHour || hour < wakeupHour;
            } else {
                // Overnight case: sleep 23:00 to 06:00
                return hour >= bedtimeHour || hour < wakeupHour;
            }
        }
    }

    public static class MealTime {
        private int hour;
        private int minute;
        private int duration; // in minutes
        private boolean enabled;

        public int getHour() { return hour; }
        public int getMinute() { return minute; }
        public int getDuration() { return duration; }
        public boolean isEnabled() { return enabled; }

        public TimeOfDay getStartTime() {
            return new TimeOfDay(hour, minute);
        }

        public TimeOfDay getEndTime() {
            int totalMinutes = hour * 60 + minute + duration;
            return new TimeOfDay(totalMinutes / 60, totalMinutes % 60);
        }
    }

    public static class MealTimes {
        private MealTime breakfast;
        private MealTime lunch;
        private MealTime dinner;
        private SnackPreferences snacks;

        public MealTime getBreakfast() { return breakfast; }
        public MealTime getLunch() { return lunch; }
        public MealTime getDinner() { return dinner; }
        public SnackPreferences getSnacks() { return snacks; }

        public List<MealTime> getAllEnabledMeals() {
            List<MealTime> meals = new ArrayList<>();
            if (breakfast != null && breakfast.isEnabled()) meals.add(breakfast);
            if (lunch != null && lunch.isEnabled()) meals.add(lunch);
            if (dinner != null && dinner.isEnabled()) meals.add(dinner);
            return meals;
        }
    }

    public static class SnackPreferences {
        private boolean enabled;
        private List<Integer> preferredTimes;

        public boolean isEnabled() { return enabled; }
        public List<Integer> getPreferredTimes() { return preferredTimes; }
    }

    public static class WorkSchedule {
        private boolean enabled;
        private List<String> workDays;
        private TimeOfDay startTime;
        private TimeOfDay endTime;
        private boolean lunchBreakIncluded;
        private boolean allowStudyDuringWork;

        public boolean isEnabled() { return enabled; }
        public List<String> getWorkDays() { return workDays; }
        public TimeOfDay getStartTime() { return startTime; }
        public TimeOfDay getEndTime() { return endTime; }
        public boolean isLunchBreakIncluded() { return lunchBreakIncluded; }
        public boolean isAllowStudyDuringWork() { return allowStudyDuringWork; }

        public boolean isWorkDay(String dayOfWeek) {
            return workDays != null && workDays.contains(dayOfWeek.toUpperCase());
        }

        public boolean isWorkTime(int hour, String dayOfWeek) {
            if (!enabled || !isWorkDay(dayOfWeek)) return false;

            int startHour = startTime.getHour();
            int endHour = endTime.getHour();

            return hour >= startHour && hour < endHour;
        }
    }

    public static class StudyTimeBlock {
        private boolean enabled;
        private int startHour;
        private int endHour;
        private String energyLevel;

        public boolean isEnabled() { return enabled; }
        public int getStartHour() { return startHour; }
        public int getEndHour() { return endHour; }
        public String getEnergyLevel() { return energyLevel; }
    }

    public static class StudyPreferences {
        private PreferredStudyTimes preferredStudyTimes;
        private int maxDailyStudyHours;
        private int minBreakBetweenSessions;
        private int preferredSessionLength;

        // "morning", "afternoon", "evening"
        private String deepWorkPreference;

        public PreferredStudyTimes getPreferredStudyTimes() { return preferredStudyTimes; }
        public int getMaxDailyStudyHours() { return maxDailyStudyHours; }
        public int getMinBreakBetweenSessions() { return minBreakBetweenSessions; }
        public int getPreferredSessionLength() { return preferredSessionLength; }
        public String getDeepWorkPreference() { return deepWorkPreference; }
    }

    public static class PreferredStudyTimes {
        private StudyTimeBlock morning;
        private StudyTimeBlock afternoon;
        private StudyTimeBlock evening;

        public StudyTimeBlock getMorning() { return morning; }
        public StudyTimeBlock getAfternoon() { return afternoon; }
        public StudyTimeBlock getEvening() { return evening; }

        public List<StudyTimeBlock> getEnabledBlocks() {
            List<StudyTimeBlock> blocks = new ArrayList<>();
            if (morning != null && morning.isEnabled()) blocks.add(morning);
            if (afternoon != null && afternoon.isEnabled()) blocks.add(afternoon);
            if (evening != null && evening.isEnabled()) blocks.add(evening);
            return blocks;
        }
    }

    public static class BreakPreferences {
        private int shortBreakDuration;
        private int longBreakDuration;
        private boolean allowAdaptiveBreaks;
        private int maxBreakExtension;
        private List<String> preferredBreakActivities;

        public int getShortBreakDuration() { return shortBreakDuration; }
        public int getLongBreakDuration() { return longBreakDuration; }
        public boolean isAllowAdaptiveBreaks() { return allowAdaptiveBreaks; }
        public int getMaxBreakExtension() { return maxBreakExtension; }
        public List<String> getPreferredBreakActivities() { return preferredBreakActivities; }
    }

    public static class ExerciseBlock {
        private int hour;
        private int minute;
        private int duration;
        private String type;

        public int getHour() { return hour; }
        public int getMinute() { return minute; }
        public int getDuration() { return duration; }
        public String getType() { return type; }
    }

    public static class ExerciseSchedule {
        private boolean enabled;
        private List<ExerciseBlock> preferredTimes;
        private int minWeeklyHours;
        private boolean flexibleScheduling;

        public boolean isEnabled() { return enabled; }
        public List<ExerciseBlock> getPreferredTimes() { return preferredTimes; }
        public int getMinWeeklyHours() { return minWeeklyHours; }
        public boolean isFlexibleScheduling() { return flexibleScheduling; }
    }

    public static class SocialTime {
        private boolean enabled;
        private List<String> preferredDays;
        private List<Integer> preferredHours;
        private boolean protectFromStudy;

        public boolean isEnabled() { return enabled; }
        public List<String> getPreferredDays() { return preferredDays; }
        public List<Integer> getPreferredHours() { return preferredHours; }
        public boolean isProtectFromStudy() { return protectFromStudy; }

        public boolean isSocialTime(int hour, String dayOfWeek) {
            if (!enabled) return false;
            return (preferredDays == null || preferredDays.contains(dayOfWeek.toUpperCase())) &&
                    (preferredHours == null || preferredHours.contains(hour));
        }
    }

    public static class SoundNotifications {
        private boolean enabled;
        private double volume;
        private String highPAMSound;
        private String lowPAMSound;

        public boolean isEnabled() { return enabled; }
        public double getVolume() { return volume; }
        public String getHighPAMSound() { return highPAMSound; }
        public String getLowPAMSound() { return lowPAMSound; }
    }

    public static class HapticFeedback {
        private boolean enabled;
        private String intensity;

        public boolean isEnabled() { return enabled; }
        public String getIntensity() { return intensity; }
    }

    public static class VisualCues {
        private boolean enabled;
        private boolean dimScreenOnLowPAM;
        private boolean showBadges;

        public boolean isEnabled() { return enabled; }
        public boolean isDimScreenOnLowPAM() { return dimScreenOnLowPAM; }
        public boolean isShowBadges() { return showBadges; }
    }

    public static class EnvironmentalPreferences {
        private SoundNotifications soundNotifications;
        private HapticFeedback hapticFeedback;
        private VisualCues visualCues;

        public SoundNotifications getSoundNotifications() { return soundNotifications; }
        public HapticFeedback getHapticFeedback() { return hapticFeedback; }
        public VisualCues getVisualCues() { return visualCues; }
    }

    public static class PAMThresholds {
        private int lowThreshold;
        private int mediumThreshold;
        private int highThreshold;
        private int sharpDropThreshold;
        private int consecutiveLowSessionsAlert;

        public int getLowThreshold() { return lowThreshold; }
        public int getMediumThreshold() { return mediumThreshold; }
        public int getHighThreshold() { return highThreshold; }
        public int getSharpDropThreshold() { return sharpDropThreshold; }
        public int getConsecutiveLowSessionsAlert() { return consecutiveLowSessionsAlert; }
    }

    public static class CalendarIntegration {
        private boolean enabled;
        private boolean respectCalendarEvents;
        private int bufferBeforeEvent;
        private int bufferAfterEvent;
        private boolean autoBlockExams;
        private boolean autoBlockMeetings;

        public boolean isEnabled() { return enabled; }
        public boolean isRespectCalendarEvents() { return respectCalendarEvents; }
        public int getBufferBeforeEvent() { return bufferBeforeEvent; }
        public int getBufferAfterEvent() { return bufferAfterEvent; }
        public boolean isAutoBlockExams() { return autoBlockExams; }
        public boolean isAutoBlockMeetings() { return autoBlockMeetings; }
    }

    public static class PersonalGoals {
        private int dailyStudyMinutes;
        private int weeklyStudySessions;
        private int maintainMinPAM;
        private int targetFocusScore;

        public int getDailyStudyMinutes() { return dailyStudyMinutes; }
        public int getWeeklyStudySessions() { return weeklyStudySessions; }
        public int getMaintainMinPAM() { return maintainMinPAM; }
        public int getTargetFocusScore() { return targetFocusScore; }
    }

    public static class Advanced {
        private boolean enableMachineLearning;
        private int dataRetentionDays;
        private boolean syncWithCloud;
        private boolean privacyMode;

        public boolean isEnableMachineLearning() { return enableMachineLearning; }
        public int getDataRetentionDays() { return dataRetentionDays; }
        public boolean isSyncWithCloud() { return syncWithCloud; }
        public boolean isPrivacyMode() { return privacyMode; }
    }

    // Main getters
    public String getVersion() { return version; }
    public String getLastUpdated() { return lastUpdated; }
    public SleepSchedule getSleepSchedule() { return sleepSchedule; }
    public MealTimes getMealTimes() { return mealTimes; }
    public WorkSchedule getWorkSchedule() { return workSchedule; }
    public StudyPreferences getStudyPreferences() { return studyPreferences; }
    public BreakPreferences getBreakPreferences() { return breakPreferences; }
    public ExerciseSchedule getExerciseSchedule() { return exerciseSchedule; }
    public SocialTime getSocialTime() { return socialTime; }
    public EnvironmentalPreferences getEnvironmentalPreferences() { return environmentalPreferences; }
    public PAMThresholds getPamThresholds() { return pamThresholds; }
    public CalendarIntegration getCalendarIntegration() { return calendarIntegration; }
    public PersonalGoals getPersonalGoals() { return personalGoals; }
    public Advanced getAdvanced() { return advanced; }

    public static UserPreferences createDefault() {
        // Return the schema defaults - this will be populated from JSON
        UserPreferences prefs = new UserPreferences();
        prefs.version = "1.0";

        // Initialize with safe defaults
        prefs.sleepSchedule = new SleepSchedule();
        prefs.sleepSchedule.bedtime = new TimeOfDay(23, 0);
        prefs.sleepSchedule.wakeupTime = new TimeOfDay(6, 0);
        prefs.sleepSchedule.targetSleepHours = 7.5;

        prefs.breakPreferences = new BreakPreferences();
        prefs.breakPreferences.shortBreakDuration = 5;
        prefs.breakPreferences.longBreakDuration = 15;
        prefs.breakPreferences.allowAdaptiveBreaks = true;

        prefs.pamThresholds = new PAMThresholds();
        prefs.pamThresholds.lowThreshold = 15;
        prefs.pamThresholds.mediumThreshold = 25;
        prefs.pamThresholds.highThreshold = 35;

        return prefs;
    }

    public static UserPreferences fromJson(String json) throws JsonSyntaxException {
        Gson gson = new Gson();
        return gson.fromJson(json, UserPreferences.class);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}