package ch.inf.usi.mindbricks.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;

/**
 * Data Access Object for StudySession entities
 */
@Dao
public interface StudySessionDao {

    @Query("SELECT s.*, " +
           "COALESCE(AVG(l.noiseLevel), 0) as avgNoiseLevel, " +
           "COALESCE(AVG(l.lightLevel), 0) as avgLightLevel, " +
           "COALESCE(SUM(CASE WHEN l.motionDetected THEN 1 ELSE 0 END), 0) as phonePickupCount " +
           "FROM study_sessions s " +
           "LEFT JOIN session_sensor_logs l ON s.id = l.sessionId " +
           "GROUP BY s.id " +
           "ORDER BY s.timestamp DESC LIMIT :limit")
    List<StudySessionWithStats> getRecentSessions(int limit);

    @Query("SELECT s.*, " +
           "COALESCE(AVG(l.noiseLevel), 0) as avgNoiseLevel, " +
           "COALESCE(AVG(l.lightLevel), 0) as avgLightLevel, " +
           "COALESCE(SUM(CASE WHEN l.motionDetected THEN 1 ELSE 0 END), 0) as phonePickupCount " +
           "FROM study_sessions s " +
           "LEFT JOIN session_sensor_logs l ON s.id = l.sessionId " +
           "GROUP BY s.id " +
           "ORDER BY s.timestamp DESC LIMIT :limit")
    LiveData<List<StudySessionWithStats>> observeRecentSessions(int limit);

    @Query("SELECT s.*, " +
           "COALESCE(AVG(l.noiseLevel), 0) as avgNoiseLevel, " +
           "COALESCE(AVG(l.lightLevel), 0) as avgLightLevel, " +
           "COALESCE(SUM(CASE WHEN l.motionDetected THEN 1 ELSE 0 END), 0) as phonePickupCount " +
           "FROM study_sessions s " +
           "LEFT JOIN session_sensor_logs l ON s.id = l.sessionId " +
           "WHERE s.timestamp >= :startTime " +
           "GROUP BY s.id " +
           "ORDER BY s.timestamp DESC")
    List<StudySessionWithStats> getSessionsSince(long startTime);

    @Query("SELECT s.*, " +
           "COALESCE(AVG(l.noiseLevel), 0) as avgNoiseLevel, " +
           "COALESCE(AVG(l.lightLevel), 0) as avgLightLevel, " +
           "COALESCE(SUM(CASE WHEN l.motionDetected THEN 1 ELSE 0 END), 0) as phonePickupCount " +
           "FROM study_sessions s " +
           "LEFT JOIN session_sensor_logs l ON s.id = l.sessionId " +
           "WHERE s.timestamp >= :startTime " +
           "GROUP BY s.id " +
           "ORDER BY s.timestamp DESC")
    LiveData<List<StudySessionWithStats>> observeSessionsSince(long startTime);

    @Query("SELECT s.*, " +
           "COALESCE(AVG(l.noiseLevel), 0) as avgNoiseLevel, " +
           "COALESCE(AVG(l.lightLevel), 0) as avgLightLevel, " +
           "COALESCE(SUM(CASE WHEN l.motionDetected THEN 1 ELSE 0 END), 0) as phonePickupCount " +
           "FROM study_sessions s " +
           "LEFT JOIN session_sensor_logs l ON s.id = l.sessionId " +
           "GROUP BY s.id " +
           "ORDER BY s.timestamp DESC")
    List<StudySessionWithStats> getAllSessions();

    @Insert
    long insert(StudySession session);

    @Insert
    List<Long> insertAll(List<StudySession> sessions);

    @Delete
    void delete(StudySession session);

    @Query("DELETE FROM study_sessions")
    void deleteAll();

    @Update
    void update(StudySession session);
}