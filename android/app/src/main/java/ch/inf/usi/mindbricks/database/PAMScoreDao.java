package ch.inf.usi.mindbricks.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ch.inf.usi.mindbricks.model.evaluation.PAMScore;

@Dao
public interface PAMScoreDao {

    @Insert
    long insert(PAMScore pamScore);

    @Update
    void update(PAMScore pamScore);

    @Query("SELECT * FROM pam_scores ORDER BY timestamp DESC LIMIT 1")
    PAMScore getLatestScore();

    @Query("SELECT * FROM pam_scores ORDER BY timestamp DESC LIMIT :n")
    List<PAMScore> getLastNScores(int n);


    @Query("SELECT * FROM pam_scores WHERE sessionId = :sessionId")
    List<PAMScore> getScoresForSession(long sessionId);

    @Query("SELECT * FROM pam_scores WHERE timestamp BETWEEN :startTime AND :endTime " +
            "ORDER BY timestamp ASC")
    List<PAMScore> getScoresInRange(long startTime, long endTime);


    @Query("SELECT AVG(totalScore) FROM pam_scores WHERE timestamp >= :startTime")
    Float getAverageScoreSince(long startTime);


    @Query("SELECT AVG(totalScore) FROM pam_scores WHERE timestamp BETWEEN :dayStart AND :dayEnd")
    Float getAverageScoreForDay(long dayStart, long dayEnd);


    @Query("SELECT COUNT(*) FROM (" +
            "    SELECT totalScore, " +
            "           ROW_NUMBER() OVER (ORDER BY timestamp DESC) as rn " +
            "    FROM pam_scores " +
            "    ORDER BY timestamp DESC" +
            ") WHERE totalScore <= :threshold " +
            "AND rn <= (SELECT MIN(rn) FROM (" +
            "    SELECT ROW_NUMBER() OVER (ORDER BY timestamp DESC) as rn, totalScore " +
            "    FROM pam_scores " +
            "    ORDER BY timestamp DESC" +
            ") WHERE totalScore > :threshold)")
    int getConsecutiveLowScoreCount(int threshold);

    @Query("SELECT AVG(totalScore) as avgScore, " +
            "       DATE(timestamp / 1000, 'unixepoch', 'localtime') as date " +
            "FROM pam_scores " +
            "WHERE timestamp >= :startTime " +
            "GROUP BY date " +
            "ORDER BY date DESC " +
            "LIMIT :days")
    List<DailyAverage> getDailyAverages(long startTime, int days);


    @Query("SELECT * FROM pam_scores " +
            "WHERE (SELECT totalScore FROM pam_scores ORDER BY timestamp DESC LIMIT 1 OFFSET 1) - " +
            "      totalScore >= :dropThreshold " +
            "ORDER BY timestamp DESC LIMIT 1")
    PAMScore detectSharpDrop(int dropThreshold);

    @Query("SELECT totalScore FROM pam_scores ORDER BY totalScore LIMIT 1 " +
            "OFFSET (SELECT COUNT(*) FROM pam_scores) / 2")
    Integer getBaselineScore();

    @Query("SELECT affectiveState, COUNT(*) as count, AVG(totalScore) as avgScore " +
            "FROM pam_scores " +
            "GROUP BY affectiveState")
    List<StateStatistics> getStateStatistics();

    @Query("DELETE FROM pam_scores WHERE timestamp < :cutoffTime")
    int deleteOldScores(long cutoffTime);

    @Query("SELECT * FROM pam_scores ORDER BY timestamp DESC")
    List<PAMScore> getAllScores();


    @Query("SELECT COUNT(*) FROM pam_scores")
    int getScoreCount();


    // Helper classes for query results
    class DailyAverage {
        public float avgScore;
        public String date;
    }

    class StateStatistics {
        public String affectiveState;
        public int count;
        public float avgScore;
    }
}