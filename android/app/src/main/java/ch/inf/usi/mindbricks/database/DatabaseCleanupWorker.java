package ch.inf.usi.mindbricks.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.database.SessionSensorLogDao;

/**
 * Background worker to periodically clean up old sensor logs
 * to prevent database bloat.
 *
 * Sensor logs are captured every 5 seconds during sessions,
 * which creates ~17,280 rows per day of active use.
 * This worker keeps only the last 90 days of logs.
 */
public class DatabaseCleanupWorker extends Worker {

    private static final String TAG = "DatabaseCleanupWorker";
    private static final long RETENTION_DAYS = 90;

    public DatabaseCleanupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting database cleanup");

        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            SessionSensorLogDao dao = db.sessionSensorLogDao();

            // Calculate cutoff time (90 days ago)
            long cutoffTime = System.currentTimeMillis() - (RETENTION_DAYS * 24 * 60 * 60 * 1000L);

            // Log current state
            int totalLogsBefore = dao.getTotalLogCount();
            Long oldestTimestamp = dao.getOldestLogTimestamp();

            Log.d(TAG, "Database state before cleanup:");
            Log.d(TAG, "  Total logs: " + totalLogsBefore);
            Log.d(TAG, "  Oldest log: " + (oldestTimestamp != null ?
                    new java.util.Date(oldestTimestamp) : "none"));
            Log.d(TAG, "  Cutoff date: " + new java.util.Date(cutoffTime));

            // Delete old logs
            int deletedCount = dao.deleteLogsOlderThan(cutoffTime);

            // Log results
            int totalLogsAfter = dao.getTotalLogCount();
            Log.d(TAG, "Cleanup complete:");
            Log.d(TAG, "  Deleted: " + deletedCount + " logs");
            Log.d(TAG, "  Remaining: " + totalLogsAfter + " logs");
            Log.d(TAG, "  Space saved: ~" + (deletedCount * 32) + " bytes");

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error during database cleanup", e);
            return Result.retry();
        }
    }
}