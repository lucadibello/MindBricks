package ch.inf.usi.mindbricks.model.visual.calendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.drivers.calendar.CalendarDriver;
import ch.inf.usi.mindbricks.drivers.calendar.DeviceCalendarDriver;
import ch.inf.usi.mindbricks.repository.CalendarRepository;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;


/**
 * Service that orchestrates calendar synchronization.
 *
 * - Manages registered CalendarDriver instances
 * - Coordinates sync operations between drivers and repository
 * - Tracks sync status and last sync times
 * - Provides a clean API for the rest of the app
 */

public class CalendarSyncService {

    private static final String TAG = "CalendarSyncService";
    private static final String PREFS_NAME = "calendar_sync_prefs";
    private static final String PREF_LAST_SYNC_PREFIX = "last_sync_";

    // Singleton instance
    private static CalendarSyncService instance;

    private final Context context;
    private final CalendarRepository repository;
    private final Executor syncExecutor;
    private final SharedPreferences prefs;

    // Registered drivers
    private final Map<String, CalendarDriver> drivers = new HashMap<>();

    // Sync configuration
    private int syncRangeBackwardDays = 7;   // How far back to sync
    private int syncRangeForwardDays = 30;   // How far forward to sync

    public static synchronized CalendarSyncService getInstance(Context context) {
        if (instance == null) {
            instance = new CalendarSyncService(context.getApplicationContext());
        }
        return instance;
    }

    private CalendarSyncService(Context context) {
        this.context = context;
        this.repository = new CalendarRepository(context);
        this.syncExecutor = Executors.newSingleThreadExecutor();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Register default drivers
        registerDefaultDrivers();
    }

    private void registerDefaultDrivers() {
        // Device Calendar
        registerDriver(new DeviceCalendarDriver(context));

        Log.d(TAG, "Registered " + drivers.size() + " calendar drivers");
    }

    public void registerDriver(CalendarDriver driver) {
        drivers.put(driver.getSourceName(), driver);
        Log.d(TAG, "Registered driver: " + driver.getSourceName());
    }

    public CalendarDriver getDriver(String sourceName) {
        return drivers.get(sourceName);
    }

    public List<CalendarDriver> getAllDrivers() {
        return new ArrayList<>(drivers.values());
    }

    public List<CalendarDriver> getAuthenticatedDrivers() {
        List<CalendarDriver> authenticated = new ArrayList<>();
        for (CalendarDriver driver : drivers.values()) {
            if (driver.isAuthenticated()) {
                authenticated.add(driver);
            }
        }
        return authenticated;
    }

    public List<DriverInfo> getDriverInfoList() {
        List<DriverInfo> infoList = new ArrayList<>();
        for (CalendarDriver driver : drivers.values()) {
            infoList.add(new DriverInfo(
                    driver.getSourceName(),
                    driver.getDisplayName(),
                    driver.isAuthenticated(),
                    getLastSyncTime(driver.getSourceName())
            ));
        }
        return infoList;
    }

    public boolean isDriverAuthenticated(String sourceName) {
        CalendarDriver driver = drivers.get(sourceName);
        return driver != null && driver.isAuthenticated();
    }

    public void authenticateDriver(String sourceName, Activity activity, CalendarDriver.AuthCallback callback) {
        CalendarDriver driver = drivers.get(sourceName);
        if (driver == null) {
            callback.onAuthFailure("Unknown calendar source: " + sourceName);
            return;
        }

        driver.authenticate(activity, new CalendarDriver.AuthCallback() {
            @Override
            public void onAuthSuccess() {
                Log.d(TAG, "Authentication successful for: " + sourceName);
                // Trigger initial sync after successful authentication
                syncDriver(sourceName, null);
                callback.onAuthSuccess();
            }

            @Override
            public void onAuthFailure(String error) {
                Log.e(TAG, "Authentication failed for " + sourceName + ": " + error);
                callback.onAuthFailure(error);
            }

            @Override
            public void onAuthCancelled() {
                Log.d(TAG, "Authentication cancelled for: " + sourceName);
                callback.onAuthCancelled();
            }
        });
    }

    public void disconnectDriver(String sourceName) {
        CalendarDriver driver = drivers.get(sourceName);
        if (driver != null) {
            driver.signOut(context);
            repository.deleteEventsBySource(sourceName, null);
            clearLastSyncTime(sourceName);
            Log.d(TAG, "Disconnected driver: " + sourceName);
        }
    }

    public void syncAll(SyncCallback callback) {
        syncExecutor.execute(() -> {
            Log.d(TAG, "Starting sync for all authenticated drivers");

            List<CalendarDriver> authenticatedDrivers = getAuthenticatedDrivers();
            if (authenticatedDrivers.isEmpty()) {
                Log.d(TAG, "No authenticated drivers, skipping sync");
                if (callback != null) {
                    callback.onSyncComplete(new SyncResult(0, 0, new ArrayList<>()));
                }
                return;
            }

            SyncResult totalResult = new SyncResult(0, 0, new ArrayList<>());

            for (CalendarDriver driver : authenticatedDrivers) {
                SyncResult driverResult = syncDriverInternal(driver);
                totalResult = totalResult.merge(driverResult);
            }

            Log.d(TAG, "Sync complete. Total events: " + totalResult.totalEvents);
            if (callback != null) {
                callback.onSyncComplete(totalResult);
            }
        });
    }

    public void syncDriver(String sourceName, SyncCallback callback) {
        CalendarDriver driver = drivers.get(sourceName);
        if (driver == null) {
            if (callback != null) {
                List<String> errors = new ArrayList<>();
                errors.add("Unknown source: " + sourceName);
                callback.onSyncComplete(new SyncResult(0, 0, errors));
            }
            return;
        }

        if (!driver.isAuthenticated()) {
            if (callback != null) {
                List<String> errors = new ArrayList<>();
                errors.add("Not authenticated: " + sourceName);
                callback.onSyncComplete(new SyncResult(0, 0, errors));
            }
            return;
        }

        syncExecutor.execute(() -> {
            SyncResult result = syncDriverInternal(driver);
            if (callback != null) {
                callback.onSyncComplete(result);
            }
        });
    }

    private SyncResult syncDriverInternal(CalendarDriver driver) {
        String sourceName = driver.getSourceName();
        Log.d(TAG, "Syncing driver: " + sourceName);

        List<String> errors = new ArrayList<>();
        int eventCount = 0;

        try {
            // Calculate sync range
            long[] syncRange = calculateSyncRange();
            long startTime = syncRange[0];
            long endTime = syncRange[1];

            // Record sync start time (for cleanup of deleted events)
            long syncTimestamp = System.currentTimeMillis();

            // Fetch events from the driver
            List<CalendarEvent> events = driver.fetchEvents(startTime, endTime);
            eventCount = events.size();

            if (!events.isEmpty()) {
                for (CalendarEvent event : events) {
                    event.setLastSyncedAt(syncTimestamp);
                }

                repository.saveEvents(events);
                repository.cleanupStaleEvents(sourceName, syncTimestamp);
            }

            // Record last sync time
            saveLastSyncTime(sourceName);

            Log.d(TAG, "Synced " + eventCount + " events from " + sourceName);

        } catch (CalendarDriver.CalendarSyncException e) {
            Log.e(TAG, "Sync error for " + sourceName, e);
            errors.add(sourceName + ": " + e.getMessage());

            if (e.isAuthError()) {
                errors.add("Please reconnect " + driver.getDisplayName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error syncing " + sourceName, e);
            errors.add(sourceName + ": " + e.getMessage());
        }

        return new SyncResult(eventCount, errors.isEmpty() ? 1 : 0, errors);
    }

    private long[] calculateSyncRange() {
        Calendar cal = Calendar.getInstance();

        // Start: X days ago at midnight
        cal.add(Calendar.DAY_OF_YEAR, -syncRangeBackwardDays);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();

        // End: Y days forward at end of day
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, syncRangeForwardDays);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endTime = cal.getTimeInMillis();

        return new long[]{startTime, endTime};
    }

    public long getLastSyncTime(String sourceName) {
        return prefs.getLong(PREF_LAST_SYNC_PREFIX + sourceName, 0);
    }

    private void saveLastSyncTime(String sourceName) {
        prefs.edit()
                .putLong(PREF_LAST_SYNC_PREFIX + sourceName, System.currentTimeMillis())
                .apply();
    }

    private void clearLastSyncTime(String sourceName) {
        prefs.edit()
                .remove(PREF_LAST_SYNC_PREFIX + sourceName)
                .apply();
    }

    public boolean needsSync(String sourceName, int maxAgeMinutes) {
        long lastSync = getLastSyncTime(sourceName);
        if (lastSync == 0) return true;

        long ageMs = System.currentTimeMillis() - lastSync;
        return ageMs > (maxAgeMinutes * 60 * 1000L);
    }

    public void setSyncRangeBackwardDays(int days) {
        this.syncRangeBackwardDays = days;
    }

    public void setSyncRangeForwardDays(int days) {
        this.syncRangeForwardDays = days;
    }

    public CalendarRepository getRepository() {
        return repository;
    }

    public static class DriverInfo {
        public final String sourceName;
        public final String displayName;
        public final boolean isConnected;
        public final long lastSyncTime;

        public DriverInfo(String sourceName, String displayName, boolean isConnected,
                          long lastSyncTime) {
            this.sourceName = sourceName;
            this.displayName = displayName;
            this.isConnected = isConnected;
            this.lastSyncTime = lastSyncTime;
        }

        public String getLastSyncTimeString() {
            if (lastSyncTime == 0) return "Never";

            long ageMs = System.currentTimeMillis() - lastSyncTime;
            long ageMinutes = ageMs / (1000 * 60);

            if (ageMinutes < 1) return "Just now";
            if (ageMinutes < 60) return ageMinutes + " min ago";

            long ageHours = ageMinutes / 60;
            if (ageHours < 24) return ageHours + " hr ago";

            long ageDays = ageHours / 24;
            return ageDays + " day" + (ageDays > 1 ? "s" : "") + " ago";
        }
    }

    public static class SyncResult {
        public final int totalEvents;
        public final int successfulSources;
        public final List<String> errors;

        public SyncResult(int totalEvents, int successfulSources, List<String> errors) {
            this.totalEvents = totalEvents;
            this.successfulSources = successfulSources;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public SyncResult merge(SyncResult other) {
            List<String> mergedErrors = new ArrayList<>(this.errors);
            mergedErrors.addAll(other.errors);
            return new SyncResult(
                    this.totalEvents + other.totalEvents,
                    this.successfulSources + other.successfulSources,
                    mergedErrors
            );
        }
    }

    public interface SyncCallback {
        void onSyncComplete(SyncResult result);
    }
}