package ch.inf.usi.mindbricks.repository;


import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.database.SessionSensorLogDao;
import ch.inf.usi.mindbricks.database.StudySessionDao;
import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;
import ch.inf.usi.mindbricks.model.visual.StudySession;

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

    public LiveData<List<StudySession>> getAllSessions(int limit) {
        MutableLiveData<List<StudySession>> liveData = new MutableLiveData<>();

        dbExecutor.execute(() -> {
            List<StudySession> sessions = studySessionDao.getRecentSessions(limit);
            liveData.postValue(sessions);
        });

        return liveData;
    }

    public LiveData<List<StudySession>> getRecentSessions(int limit) {
        MutableLiveData<List<StudySession>> liveData = new MutableLiveData<>();

        dbExecutor.execute(() -> {
            List<StudySession> sessions = studySessionDao.getRecentSessions(limit);
            liveData.postValue(sessions);
        });

        return liveData;
    }

    // Add this method to StudySessionRepository
    public List<StudySession> getSessionsSinceSync(long startTime) {
        Log.d("Repository", "Sync query for sessions since: " + startTime);

        List<StudySession> sessions = studySessionDao.getSessionsSince(startTime);

        Log.d("Repository", "Query complete: " + (sessions != null ? sessions.size() : 0) + " sessions");
        return sessions;
    }

    public LiveData<List<StudySession>> getSessionsSince(long startTime) {
        MutableLiveData<List<StudySession>> liveData = new MutableLiveData<>();

        dbExecutor.execute(() -> {
            Log.d("Repository", "Querying sessions since: " + startTime);  // ADD THIS
            List<StudySession> sessions = studySessionDao.getSessionsSince(startTime);
            Log.d("Repository", "Found " + (sessions != null ? sessions.size() : 0) + " sessions");  // ADD THIS
            liveData.postValue(sessions);
        });

        return liveData;
    }
    public LiveData<List<StudySession>> getSessionsInRange(long startTime, long endTime){
        MutableLiveData<List<StudySession>> liveData = new MutableLiveData<>();

        dbExecutor.execute(() -> {
            List<StudySession> allSesions = studySessionDao.getSessionsSince(startTime);
            List<StudySession> filteredSessions = new java.util.ArrayList<>();

            for(StudySession session : allSesions){
                if(session.getTimestamp() >= startTime && session.getTimestamp() <= endTime)
                    filteredSessions.add(session);
            }

            liveData.postValue(filteredSessions);
        });
        return liveData;
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

    public List<StudySession> getRecentSessionsSync(int limit) {
        return studySessionDao.getRecentSessions(limit);
    }

    public interface InsertCallback {
        void onInserted(long sessionId);
    }
}
