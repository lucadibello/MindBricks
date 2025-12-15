package ch.inf.usi.mindbricks.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.database.CalendarEventDao;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;

/**
 * Repository class for CalendarEvent data access.
 */
public class CalendarRepository {

    private static final String TAG = "CalendarRepository";

    private final CalendarEventDao calendarEventDao;
    private final Executor dbExecutor;

    public CalendarRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        calendarEventDao = db.calendarEventDao();
        dbExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Saves a list of calendar events to the database.
     * Uses upsert to handle duplicates gracefully.
     *
     * @param events List of events to save
     * @param callback Optional callback when operation completes
     */
    public void saveEvents(List<CalendarEvent> events, SaveCallback callback) {
        dbExecutor.execute(() -> {
            try {
                calendarEventDao.upsertAll(events);
                Log.d(TAG, "Saved " + events.size() + " events");
                if (callback != null) {
                    callback.onSuccess(events.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving events", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public void saveEvents(List<CalendarEvent> events) {
        saveEvents(events, null);
    }

    public void saveEvent(CalendarEvent event) {
        dbExecutor.execute(() -> {
            try {
                calendarEventDao.upsert(event);
                Log.d(TAG, "Saved event: " + event.getTitle());
            } catch (Exception e) {
                Log.e(TAG, "Error saving event", e);
            }
        });
    }

    /**
     * Deletes all events from a specific source.
     * Use before re-syncing to ensure clean state.
     */
    public void deleteEventsBySource(String source, Runnable onComplete) {
        dbExecutor.execute(() -> {
            try {
                calendarEventDao.deleteBySource(source);
                Log.d(TAG, "Deleted all events from source: " + source);
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting events by source", e);
            }
        });
    }

    /**
     * Deletes events that are older than the specified number of days.
     */
    public void deleteOldEvents(int daysOld) {
        dbExecutor.execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -daysOld);
            long cutoff = cal.getTimeInMillis();

            calendarEventDao.deleteEventsBefore(cutoff);
            Log.d(TAG, "Deleted events before: " + cutoff);
        });
    }

    /**
     * Cleans up stale events that weren't updated in the last sync.
     * Call this after syncing with the current sync timestamp.
     */
    public void cleanupStaleEvents(String source, long syncTimestamp) {
        dbExecutor.execute(() -> {
            calendarEventDao.deleteStaleEvents(source, syncTimestamp);
            Log.d(TAG, "Cleaned up stale events for source: " + source);
        });
    }

    /**
     * Gets events within a time range synchronously.
     * Call from a background thread!
     */
    public List<CalendarEvent> getEventsInRangeSync(long startTime, long endTime) {
        return calendarEventDao.getEventsInRange(startTime, endTime);
    }

    /**
     * Gets events for today synchronously.
     * Call from a background thread!
     */
    public List<CalendarEvent> getEventsForTodaySync() {
        long[] dayRange = getDayRange(System.currentTimeMillis());
        return calendarEventDao.getEventsForDay(dayRange[0], dayRange[1]);
    }

    /**
     * Gets events for a specific date synchronously.
     * Call from a background thread!
     *
     * @param timestamp Any timestamp within the desired day
     */
    public List<CalendarEvent> getEventsForDaySync(long timestamp) {
        long[] dayRange = getDayRange(timestamp);
        return calendarEventDao.getEventsForDay(dayRange[0], dayRange[1]);
    }

    /**
     * Gets all events from a specific source synchronously.
     */
    public List<CalendarEvent> getEventsBySourceSync(String source) {
        return calendarEventDao.getEventsBySource(source);
    }

    /**
     * Gets total event count synchronously.
     */
    public int getTotalEventCountSync() {
        return calendarEventDao.getTotalEventCount();
    }

    /**
     * Observes events within a time range.
     * Updates automatically when database changes.
     */
    public LiveData<List<CalendarEvent>> observeEventsInRange(long startTime, long endTime) {
        return calendarEventDao.observeEventsInRange(startTime, endTime);
    }

    /**
     * Observes events for today.
     * Note: The range is fixed at creation time, so this won't update at midnight.
     * For a day-aware observer, consider using a MediatorLiveData.
     */
    public LiveData<List<CalendarEvent>> observeEventsForToday() {
        long[] dayRange = getDayRange(System.currentTimeMillis());
        return calendarEventDao.observeEventsInRange(dayRange[0], dayRange[1]);
    }

    /**
     * Observes all calendar events.
     */
    public LiveData<List<CalendarEvent>> observeAllEvents() {
        return calendarEventDao.observeAllEvents();
    }

    /**
     * Observes the total count of calendar events.
     */
    public LiveData<Integer> observeEventCount() {
        return calendarEventDao.observeEventCount();
    }

    /**
     * Calculates which hours (0-23) are blocked by calendar events for a given day.
     * Used by DataProcessor to integrate calendar events into the AI schedule.
     *
     * @param timestamp Any timestamp within the desired day
     * @return Array of 24 booleans, true if that hour has a calendar event
     */
    public boolean[] getBlockedHoursForDaySync(long timestamp) {
        boolean[] blockedHours = new boolean[24];

        List<CalendarEvent> events = getEventsForDaySync(timestamp);

        for (CalendarEvent event : events) {
            if (event.isAllDay()) {
                // All-day events block the entire day
                for (int h = 0; h < 24; h++) {
                    blockedHours[h] = true;
                }
            } else {
                // Mark individual hours covered by this event
                for (int h = 0; h < 24; h++) {
                    if (event.coversHour(h)) {
                        blockedHours[h] = true;
                    }
                }
            }
        }

        return blockedHours;
    }

    /**
     * Gets the start and end timestamps for a day containing the given timestamp.
     *
     * @param timestamp Any timestamp within the desired day
     * @return Array of [dayStart, dayEnd] timestamps
     */
    private long[] getDayRange(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        // Set to start of day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dayStart = cal.getTimeInMillis();

        // Set to end of day
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long dayEnd = cal.getTimeInMillis();

        return new long[]{dayStart, dayEnd};
    }

    public interface SaveCallback {
        void onSuccess(int count);
        void onError(Exception e);
    }
}