package ch.inf.usi.mindbricks.database;

import android.app.Application;
import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;

/**
 * Room database for MindBricks app
 *
 * @author Marta Šafářová
 * Refactored by
 * @author Luca Di Bello
 */
@Database(entities = {
        StudySession.class,
        SessionSensorLog.class,
        SessionQuestionnaire.class,
        CalendarEvent.class,
        Tag.class,
        PAMScore.class
},
        version = 1
)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Singleton instance of the database.
     */
    private static AppDatabase INSTANCE;

    /**
     * Returns the singleton instance of the database.
     *
     * @param context the application context
     * @return the singleton instance of the database
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            // get app. context to have a global context common to all activities
            Context appContext = context.getApplicationContext();
            INSTANCE = Room.databaseBuilder(
                            appContext,
                            AppDatabase.class,
                            "mindbricks_database"
                    )
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build();
        }
        return INSTANCE;
    }

    /**
     * Returns the DAO for the Tag entity.
     *
     * @return the DAO for the Tag entity
     */
    public abstract TagDao tagDao();

    /**
     * Returns the DAO for the StudySession entity.
     *
     * @return the DAO for the StudySession entity
     */
    public abstract StudySessionDao studySessionDao();

    /**
     * Returns the DAO for the SessionSensorLog entity.
     *
     * @return the DAO for the SessionSensorLog entity
     */
    public abstract SessionSensorLogDao sessionSensorLogDao();

    /**
     * Returns the DAO for the SessionQuestionnaire entity.
     *
     * @return the DAO for the SessionQuestionnaire entity
     */
    public abstract SessionQuestionnaireDao sessionQuestionnaireDao();

    /**
     * Returns the DAO for the CalendarEvent entity.
     *
     * @return the DAO for the CalendarEvent entity
     */
    public abstract CalendarEventDao calendarEventDao();

    /**
     * Returns the DAO for the PAMScore entity.
     *
     * @return the DAO for the PAMScore entity
     */
    public abstract PAMScoreDao pamScoreDao();
}