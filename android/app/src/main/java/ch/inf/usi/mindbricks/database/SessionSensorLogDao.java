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
     * Insert a single sensor log.
     */
    @Insert
    void insert(SessionSensorLog log);


    @Query("SELECT * FROM session_sensor_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    LiveData<List<SessionSensorLog>> getLogsForSession(long sessionId);

    // Queries for study session metrics

    @Query("SELECT AVG(noiseLevel) FROM session_sensor_logs WHERE sessionId = :sessionId")
    float getAverageNoise(long sessionId);

    @Query("SELECT AVG(lightLevel) FROM session_sensor_logs WHERE sessionId = :sessionId")
    float getAverageLight(long sessionId);

    @Query("SELECT COUNT(*) FROM session_sensor_logs WHERE sessionId = :sessionId AND motionDetected = 1")
    int getMotionCount(long sessionId);

    @Query("DELETE FROM session_sensor_logs WHERE timestamp < :cutoffTime")
    int deleteLogsOlderThan(long cutoffTime);

    @Query("SELECT COUNT(*) FROM session_sensor_logs")
    int getTotalLogCount();

    @Query("SELECT MIN(timestamp) FROM session_sensor_logs")
    Long getOldestLogTimestamp();
}
