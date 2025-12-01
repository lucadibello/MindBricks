package ch.inf.usi.mindbricks.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ch.inf.usi.mindbricks.model.StudySession;

/**
 * Data Access Object for StudySession entities
 */
@Dao
public interface StudySessionDao {

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC LIMIT :limit")
    List<StudySession> getRecentSessions(int limit);

    @Query("SELECT * FROM study_sessions WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    List<StudySession> getSessionsSince(long startTime);

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    List<StudySession> getAllSessions();

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