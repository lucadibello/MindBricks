package ch.inf.usi.mindbricks.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import ch.inf.usi.mindbricks.model.SessionSensorLog;
import ch.inf.usi.mindbricks.model.StudySession;
import ch.inf.usi.mindbricks.util.DatabaseSeeder;


/**
 * Room database for MindBricks app
 */
@Database(entities = {StudySession.class, SessionSensorLog.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;
    public abstract StudySessionDao studySessionDao();
    public abstract SessionSensorLogDao sessionSensorLogDao();

    private static final RoomDatabase.Callback DB_CALLBACK = new RoomDatabase.Callback(){
        // called on the database thread -> safe
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db){
          super.onCreate(db);
        }

        // called every time database is opened
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db){
            super.onOpen(db);
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "mindbricks_database"
                    )
                    .addCallback(DB_CALLBACK)
                    .fallbackToDestructiveMigration()
                    .build();

            DatabaseSeeder.seedDatabase(context.getApplicationContext(), INSTANCE);
        }
        return INSTANCE;
    }

    // WARNING
    // must call getInstance() before using the database again!!!
    public static void closeDatabase(){
        if(INSTANCE != null && INSTANCE.isOpen()){
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}