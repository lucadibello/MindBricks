package ch.inf.usi.mindbricks.util.database;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.inf.usi.mindbricks.model.visual.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;
import ch.inf.usi.mindbricks.repository.CalendarRepository;

/**
 * Helper class for integrating calendar events into AI schedule generation.
 *
 * This class provides methods that DataProcessor can use to:
 * - Get blocked hours from calendar events
 * - Create ActivityBlocks for calendar events
 * - Apply calendar constraints to the schedule
 *
 */
public class CalendarIntegrationHelper {

    private static final String TAG = "CalendarIntegration";

    private final CalendarRepository repository;

    public CalendarIntegrationHelper(Context context) {
        this.repository = new CalendarRepository(context);
    }

    public boolean[] getBlockedHoursForToday() {
        return repository.getBlockedHoursForDaySync(System.currentTimeMillis());
    }

    public boolean[] getBlockedHoursForDay(long timestamp) {
        return repository.getBlockedHoursForDaySync(timestamp);
    }

    public List<AIRecommendation.ActivityBlock> getCalendarBlocksForToday() {
        return getCalendarBlocksForDay(System.currentTimeMillis());
    }

    public List<AIRecommendation.ActivityBlock> getCalendarBlocksForDay(long timestamp) {
        List<AIRecommendation.ActivityBlock> blocks = new ArrayList<>();

        List<CalendarEvent> events = repository.getEventsForDaySync(timestamp);

        for (CalendarEvent event : events) {
            AIRecommendation.ActivityBlock block = createActivityBlock(event);
            if (block != null) {
                blocks.add(block);
            }
        }

        return blocks;
    }

    private AIRecommendation.ActivityBlock createActivityBlock(CalendarEvent event) {
        Calendar cal = Calendar.getInstance();

        // Get start hour
        cal.setTimeInMillis(event.getStartTime());
        int startHour = cal.get(Calendar.HOUR_OF_DAY);

        // Get end hour
        cal.setTimeInMillis(event.getEndTime());
        int endHour = cal.get(Calendar.HOUR_OF_DAY);
        int endMinute = cal.get(Calendar.MINUTE);
        if (endMinute > 0) {
            endHour++;
        }

        if (endHour <= startHour) {
            endHour = 24;
        }

        // Clamp to valid range
        startHour = Math.max(0, Math.min(23, startHour));
        endHour = Math.max(1, Math.min(24, endHour));

        String reason = "Calendar event";
        if (event.getCalendarName() != null) {
            reason += " (" + event.getCalendarName() + ")";
        }

        AIRecommendation.ActivityBlock block = new AIRecommendation.ActivityBlock(
                AIRecommendation.ActivityType.CALENDAR_EVENT,
                startHour,
                endHour,
                event.getTitle(),
                reason
        );

        return block;
    }

    /**
     * Applies calendar event constraints to an hourly activities array.
     * Call this BEFORE filling in other activity types.
     *
     */
    public int applyCalendarConstraints(AIRecommendation.ActivityType[] hourlyActivities,
                                        long timestamp) {
        boolean[] blockedHours = getBlockedHoursForDay(timestamp);
        int blockedCount = 0;

        for (int h = 0; h < 24; h++) {
            if (blockedHours[h]) {
                hourlyActivities[h] = AIRecommendation.ActivityType.CALENDAR_EVENT;
                blockedCount++;
            }
        }

        Log.d(TAG, "Blocked " + blockedCount + " hours with calendar events");
        return blockedCount;
    }

    public boolean isHourAvailable(int hour, long dayTimestamp) {
        boolean[] blockedHours = getBlockedHoursForDay(dayTimestamp);
        return hour >= 0 && hour < 24 && !blockedHours[hour];
    }

    public int getBlockedHourCount(long dayTimestamp) {
        boolean[] blockedHours = getBlockedHoursForDay(dayTimestamp);
        int count = 0;
        for (boolean blocked : blockedHours) {
            if (blocked) count++;
        }
        return count;
    }

    public List<CalendarEvent> getEventsInRange(long startTime, long endTime) {
        return repository.getEventsInRangeSync(startTime, endTime);
    }

}