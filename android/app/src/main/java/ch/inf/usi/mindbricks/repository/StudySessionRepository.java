package ch.inf.usi.mindbricks.repository;


import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.database.SessionSensorLogDao;
import ch.inf.usi.mindbricks.database.StudySessionDao;
import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;

/**
 * Repository class that provides a clean API for data access.
 * This abstracts the database from the rest of the app.
 */
public class StudySessionRepository {
    private final StudySessionDao studySessionDao;
    private final SessionSensorLogDao sessionSensorLogDao;
    private final Executor dbExecutor;


    public StudySessionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        studySessionDao = db.studySessionDao();
        sessionSensorLogDao = db.sessionSensorLogDao();
        dbExecutor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<StudySessionWithStats>> getAllSessions(int limit) {
        return studySessionDao.observeRecentSessions(limit);
    }

    public LiveData<List<StudySessionWithStats>> getRecentSessions(int limit) {
        return studySessionDao.observeRecentSessions(limit);
    }

    // Add this method to StudySessionRepository
    public List<StudySessionWithStats> getSessionsSinceSync(long startTime) {
        Log.d("Repository", "Sync query for sessions since: " + startTime);

        List<StudySessionWithStats> sessions = studySessionDao.getSessionsSince(startTime);

        Log.d("Repository", "Query complete: " + (sessions != null ? sessions.size() : 0) + " sessions");
        return sessions;
    }

    public LiveData<List<StudySessionWithStats>> getSessionsSince(long startTime) {
        Log.d("Repository", "Querying sessions since: " + startTime);
        return studySessionDao.observeSessionsSince(startTime);
    }
    public LiveData<List<StudySessionWithStats>> getSessionsInRange(long startTime, long endTime){
        return studySessionDao.observeSessionsInRange(startTime, endTime);
    }

    public List<StudySessionWithStats> getSessionsInRangeSync(long startTime, long endTime) {
        return studySessionDao.getSessionsInRangeSync(startTime, endTime);
    }

    public LiveData<List<SessionSensorLog>> getSensorLogsForSession(long sessionId) {
        return sessionSensorLogDao.getLogsForSession(sessionId);
    }

    public void insertSession(StudySession session, InsertCallback callback) {
        dbExecutor.execute(() -> {
            long sessionId = studySessionDao.insert(session);

            // If callback provided, return the ID on main thread
            if (callback != null) {
                // Post to main thread for UI operations
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onInserted(sessionId);
                });
            }
        });
    }

    public void updateSession(StudySession session, Runnable callback) {
        dbExecutor.execute(() -> {
            studySessionDao.update(session);

            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(callback);
            }
        });
    }

    public void deleteSession(StudySession session, Runnable callback) {
        dbExecutor.execute(() -> {
            studySessionDao.delete(session);

            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(callback);
            }
        });
    }

    public void deleteAllSessions(Runnable callback) {
        dbExecutor.execute(() -> {
            studySessionDao.deleteAll();

            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(callback);
            }
        });
    }

    public List<StudySessionWithStats> getRecentSessionsSync(int limit) {
        if (limit == Integer.MAX_VALUE) {
            Log.d("StudySessionRepository", "Loading ALL sessions (no limit)");
            return studySessionDao.getRecentSessions(10000);
        }

        if (limit > 500) {
            Log.w("StudySessionRepository", "Requested limit " + limit + " exceeds maximum 500, capping");
            limit = 500;
        }
        if (limit < 1) {
            Log.w("StudySessionRepository", "Invalid limit " + limit + ", using default 50");
            limit = 50;
        }
        return studySessionDao.getRecentSessions(limit);
    }

    public interface InsertCallback {
        void onInserted(long sessionId);
    }
}
