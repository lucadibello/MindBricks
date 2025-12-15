package ch.inf.usi.mindbricks.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;

@Dao
public interface SessionQuestionnaireDao {

    @Insert
    long insert(SessionQuestionnaire questionnaire);

    @Update
    void update(SessionQuestionnaire questionnaire);

    @Delete
    void delete(SessionQuestionnaire questionnaire);

    @Query("SELECT * FROM session_questionnaires WHERE sessionId = :sessionId LIMIT 1")
    SessionQuestionnaire getQuestionnaireBySessionId(long sessionId);

    @Query("SELECT * FROM session_questionnaires WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    List<SessionQuestionnaire> getQuestionnairesInRange(long startTime, long endTime);

    @Query("SELECT * FROM session_questionnaires WHERE sessionId = :sessionId")
    SessionQuestionnaire getQuestionnaireForSession(long sessionId);

    @Query("SELECT * FROM session_questionnaires WHERE sessionId = :sessionId")
    LiveData<SessionQuestionnaire> getQuestionnaireForSessionLive(long sessionId);

    @Query("SELECT * FROM session_questionnaires ORDER BY timeStamp DESC LIMIT :limit")
    LiveData<List<SessionQuestionnaire>> getRecentQuestionnaires(int limit);

    @Query("SELECT * FROM session_questionnaires ORDER BY timeStamp DESC")
    LiveData<List<SessionQuestionnaire>> getAllQuestionnaires();

    @Query("SELECT * FROM session_questionnaires WHERE answeredDetailedQuestions = 1")
    LiveData<List<SessionQuestionnaire>> getDetailedQuestionnaires();

    @Query("SELECT * FROM session_questionnaires WHERE answeredDetailedQuestions = 1 ORDER BY timeStamp DESC")
    LiveData<List<SessionQuestionnaire>> getDetailedQuestionnairesOrdered();

    @Query("SELECT AVG(enthusiasmRating) FROM session_questionnaires WHERE answeredDetailedQuestions = 1")
    LiveData<Float> getAverageEnthusiasm();

    @Query("SELECT AVG(energyRating) FROM session_questionnaires WHERE answeredDetailedQuestions = 1")
    LiveData<Float> getAverageEnergy();

    @Query("SELECT AVG(engagementRating) FROM session_questionnaires WHERE answeredDetailedQuestions = 1")
    LiveData<Float> getAverageEngagement();

    @Query("SELECT AVG(satisfactionRating) FROM session_questionnaires WHERE answeredDetailedQuestions = 1")
    LiveData<Float> getAverageSatisfaction();

    @Query("SELECT AVG(anticipationRating) FROM session_questionnaires WHERE answeredDetailedQuestions = 1")
    LiveData<Float> getAverageAnticipation();

    @Query("SELECT COUNT(*) FROM session_questionnaires")
    LiveData<Integer> getTotalQuestionnaireCount();

    @Query("SELECT COUNT(*) FROM session_questionnaires WHERE answeredDetailedQuestions = 1")
    LiveData<Integer> getDetailedQuestionnaireCount();

    @Query("DELETE FROM session_questionnaires")
    void deleteAll();
}