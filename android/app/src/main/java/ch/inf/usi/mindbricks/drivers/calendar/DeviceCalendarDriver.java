package ch.inf.usi.mindbricks.drivers.calendar;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.drivers.calendar.CalendarDriver;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;

/**
 * CalendarDriver implementation that reads from the Android device's
 * built-in Calendar Provider (CalendarContract).
 */
public class DeviceCalendarDriver implements CalendarDriver {

    private static final String TAG = "DeviceCalendarDriver";
    private static final String SOURCE_NAME = "device";
    private static final String DISPLAY_NAME = "Device Calendar";

    private final Context context;

    // Columns to fetch from the Events table
    private static final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.CALENDAR_COLOR
    };

    // Column indices for the projection
    private static final int IDX_ID = 0;
    private static final int IDX_TITLE = 1;
    private static final int IDX_DESCRIPTION = 2;
    private static final int IDX_DTSTART = 3;
    private static final int IDX_DTEND = 4;
    private static final int IDX_ALL_DAY = 5;
    private static final int IDX_LOCATION = 6;
    private static final int IDX_CALENDAR_ID = 7;
    private static final int IDX_CALENDAR_NAME = 8;
    private static final int IDX_CALENDAR_COLOR = 9;

    public DeviceCalendarDriver(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean isAuthenticated() {
        return hasRequiredPermissions(context);
    }

    @Override
    public void authenticate(Activity activity, AuthCallback callback) {
        // Device calendar uses runtime permissions, not OAuth
        // The activity should request permissions and call the callback
        if (hasRequiredPermissions(context)) {
            callback.onAuthSuccess();
        } else {
            // Caller needs to request permissions using ActivityCompat.requestPermissions()
            // and call callback based on the result

            //TODO popup with permission
            callback.onAuthFailure("Calendar permission required. Please grant access in app settings.");
        }
    }

    @Override
    public void signOut(Context context) {}

    @Override
    public List<CalendarEvent> fetchEvents(long startTime, long endTime) throws CalendarSyncException {
        Log.d(TAG, "Fetching events from " + startTime + " to " + endTime);

        // Check permissions first
        if (!hasRequiredPermissions(context)) {
            throw new CalendarSyncException(
                    "Calendar permission not granted",
                    CalendarSyncException.ErrorType.PERMISSION_DENIED
            );
        }

        List<CalendarEvent> events = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        // Build the query
        Uri uri = CalendarContract.Events.CONTENT_URI;

        // Events that overlap with our time range
        // An event overlaps if it starts before our end AND ends after our start
        String selection = "(" + CalendarContract.Events.DTSTART + " < ?) AND " +
                "(" + CalendarContract.Events.DTEND + " > ?)";
        String[] selectionArgs = new String[]{
                String.valueOf(endTime),
                String.valueOf(startTime)
        };

        String sortOrder = CalendarContract.Events.DTSTART + " ASC";

        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, EVENT_PROJECTION, selection, selectionArgs, sortOrder);

            if (cursor == null) {
                Log.w(TAG, "Query returned null cursor");
                return events;
            }

            Log.d(TAG, "Found " + cursor.getCount() + " events");

            while (cursor.moveToNext()) {
                try {
                    CalendarEvent event = cursorToCalendarEvent(cursor);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing event row", e);
                    // Continue to next event rather than failing entirely
                }
            }

        } catch (SecurityException e) {
            throw new CalendarSyncException(
                    "Calendar permission denied",
                    CalendarSyncException.ErrorType.PERMISSION_DENIED,
                    e
            );
        } catch (Exception e) {
            throw new CalendarSyncException(
                    "Failed to query calendar: " + e.getMessage(),
                    CalendarSyncException.ErrorType.API_ERROR,
                    e
            );
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "Successfully fetched " + events.size() + " events");
        return events;
    }

    private CalendarEvent cursorToCalendarEvent(Cursor cursor) {
        // Get the event ID to use as external ID
        long eventId = cursor.getLong(IDX_ID);
        String externalId = String.valueOf(eventId);

        String title = cursor.getString(IDX_TITLE);
        if (title == null || title.trim().isEmpty()) {
            title = "(No title)";
        }

        String description = cursor.getString(IDX_DESCRIPTION);

        // Get times
        long dtStart = cursor.getLong(IDX_DTSTART);
        long dtEnd = cursor.getLong(IDX_DTEND);

        // Handle events with no end time (use start + 1 hour as default)
        if (dtEnd == 0 || dtEnd < dtStart) {
            dtEnd = dtStart + (60 * 60 * 1000); // 1 hour default
        }

        // Check if all-day event
        boolean allDay = cursor.getInt(IDX_ALL_DAY) == 1;

        // Get optional fields
        String location = cursor.getString(IDX_LOCATION);
        String calendarName = cursor.getString(IDX_CALENDAR_NAME);
        int color = cursor.getInt(IDX_CALENDAR_COLOR);

        // Create the CalendarEvent
        CalendarEvent event = new CalendarEvent(
                externalId,
                SOURCE_NAME,
                title,
                dtStart,
                dtEnd,
                allDay
        );

        event.setDescription(description);
        event.setLocation(location);
        event.setCalendarName(calendarName);
        event.setColor(color);
        event.setLastSyncedAt(System.currentTimeMillis());

        return event;
    }

    @Override
    public boolean requiresPermissions() {
        return true;
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{Manifest.permission.READ_CALENDAR};
    }

    @Override
    public boolean hasRequiredPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    public List<DeviceCalendarInfo> getAvailableCalendars() {
        List<DeviceCalendarInfo> calendars = new ArrayList<>();

        if (!hasRequiredPermissions(context)) {
            return calendars;
        }

        String[] projection = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.VISIBLE
        };

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String name = cursor.getString(1);
                    String account = cursor.getString(2);
                    int color = cursor.getInt(3);
                    boolean visible = cursor.getInt(4) == 1;

                    calendars.add(new DeviceCalendarInfo(id, name, account, color, visible));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return calendars;
    }

    public static class DeviceCalendarInfo {
        public final long id;
        public final String displayName;
        public final String accountName;
        public final int color;
        public final boolean visible;

        public DeviceCalendarInfo(long id, String displayName, String accountName, int color, boolean visible) {
            this.id = id;
            this.displayName = displayName;
            this.accountName = accountName;
            this.color = color;
            this.visible = visible;
        }
    }
}