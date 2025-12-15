package ch.inf.usi.mindbricks.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.util.database.DatabaseSeeder;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;
import ch.inf.usi.mindbricks.database.dao.PAMScoreDao;

/**
 * Room database for MindBricks app
 */
@Database(entities = {
        StudySession.class,
        SessionSensorLog.class,
        SessionQuestionnaire.class,
        CalendarEvent.class,
        Tag.class,
        PAMScore.class
        },
        version = 7,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;
    private static Context appContext;

    public abstract TagDao tagDao();
    public abstract StudySessionDao studySessionDao();
    public abstract SessionSensorLogDao sessionSensorLogDao();
    public abstract SessionQuestionnaireDao sessionQuestionnaireDao();
    public abstract CalendarEventDao calendarEventDao();
    public abstract PAMScoreDao pamScoreDao();

    private static final RoomDatabase.Callback DB_CALLBACK = new RoomDatabase.Callback(){
        // called on the database thread -> safe
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db){
            super.onCreate(db);
            if (INSTANCE != null && appContext != null){
                DatabaseSeeder.seedDatabaseOnCreate(appContext, INSTANCE);
            }
        }

        // called every time database is opened
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db){
            super.onOpen(db);
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            appContext = context.getApplicationContext();

            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "mindbricks_database"
                    )
                    .addCallback(DB_CALLBACK)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}