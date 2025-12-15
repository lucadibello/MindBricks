package ch.inf.usi.mindbricks.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;


@Dao
public interface CalendarEventDao {
    @Insert
    long insert(CalendarEvent event);

    @Insert
    void insertAll(List<CalendarEvent> events);

    /**
     * Insert or replace an event if one with the same externalId + calendarSource exists.
     * This is the primary method for syncing - ensures no duplicates.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(CalendarEvent event);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<CalendarEvent> events);


    @Update
    void update(CalendarEvent event);

    @Delete
    void delete(CalendarEvent event);

    /**
     * Delete all events from a specific calendar source.
     * Useful when user disconnects a calendar provider.
     */
    @Query("DELETE FROM calendar_events WHERE calendarSource = :source")
    void deleteBySource(String source);

    /**
     * Delete events that haven't been synced since the given timestamp.
     * Useful for cleaning up events that were deleted from the source calendar.
     *
     * @param source The calendar source to clean
     * @param syncTimestamp Events with lastSyncedAt before this will be deleted
     */
    @Query("DELETE FROM calendar_events WHERE calendarSource = :source AND lastSyncedAt < :syncTimestamp")
    void deleteStaleEvents(String source, long syncTimestamp);

    /**
     * Delete events that ended before the given timestamp.
     * Useful for periodic cleanup of old events.
     */
    @Query("DELETE FROM calendar_events WHERE endTime < :timestamp")
    void deleteEventsBefore(long timestamp);

    /**
     * Delete all calendar events.
     */
    @Query("DELETE FROM calendar_events")
    void deleteAll();


    /**
     * Get all events within a time range.
     * This is the primary query for schedule generation.
     *
     * An event is included if it overlaps with the range at all:
     * - Event starts before range ends AND
     * - Event ends after range starts
     */
    @Query("SELECT * FROM calendar_events " +
            "WHERE startTime < :endTime AND endTime > :startTime " +
            "ORDER BY startTime ASC")
    List<CalendarEvent> getEventsInRange(long startTime, long endTime);

    /**
     * Get events for a specific day.
     * Convenience method that wraps getEventsInRange.
     *
     * @param dayStart Start of the day (midnight) as timestamp
     * @param dayEnd End of the day (23:59:59.999) as timestamp
     */
    @Query("SELECT * FROM calendar_events " +
            "WHERE startTime < :dayEnd AND endTime > :dayStart " +
            "ORDER BY startTime ASC")
    List<CalendarEvent> getEventsForDay(long dayStart, long dayEnd);

    /**
     * Get all events from a specific calendar source.
     */
    @Query("SELECT * FROM calendar_events WHERE calendarSource = :source ORDER BY startTime ASC")
    List<CalendarEvent> getEventsBySource(String source);

    /**
     * Find an event by its external ID and source.
     * Useful for checking if an event already exists before inserting.
     */
    @Query("SELECT * FROM calendar_events WHERE externalId = :externalId AND calendarSource = :source LIMIT 1")
    CalendarEvent findByExternalId(String externalId, String source);

    /**
     * Get the count of events from each source.
     * Useful for displaying sync status in UI.
     */
    @Query("SELECT calendarSource, COUNT(*) as count FROM calendar_events GROUP BY calendarSource")
    List<SourceEventCount> getEventCountBySource();

    /**
     * Get the most recent sync timestamp for a source.
     * Useful for determining when to sync again.
     */
    @Query("SELECT MAX(lastSyncedAt) FROM calendar_events WHERE calendarSource = :source")
    Long getLastSyncTime(String source);

    /**
     * Get total count of calendar events.
     */
    @Query("SELECT COUNT(*) FROM calendar_events")
    int getTotalEventCount();

    /**
     * Observe events within a time range.
     * UI components can observe this to automatically update when events change.
     */
    @Query("SELECT * FROM calendar_events " +
            "WHERE startTime < :endTime AND endTime > :startTime " +
            "ORDER BY startTime ASC")
    LiveData<List<CalendarEvent>> observeEventsInRange(long startTime, long endTime);

    /**
     * Observe all events from all sources.
     */
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    LiveData<List<CalendarEvent>> observeAllEvents();

    /**
     * Observe event count for sync status display.
     */
    @Query("SELECT COUNT(*) FROM calendar_events")
    LiveData<Integer> observeEventCount();


    class SourceEventCount {
        public String calendarSource;
        public int count;
    }
}