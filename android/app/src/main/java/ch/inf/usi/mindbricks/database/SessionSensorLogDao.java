package ch.inf.usi.mindbricks.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;

@Dao
public interface SessionSensorLogDao {

    /**
     * Insert a batch of sensor logs (noise stored as RMS amplitude).
     */
    @Insert
    void insertAll(List<SessionSensorLog> logs);

    /**
     * Load all logs for a session, ordered by timestamp (noise returned as RMS amplitude).
     */
    @Query("SELECT * FROM session_sensor_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    LiveData<List<SessionSensorLog>> getLogsForSession(long sessionId);
}
