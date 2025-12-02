package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.StudySession;

/**
 * Utility class to seed the database with initial data from JSON files in the assets folder.
 * This runs only once when the database is first created.
 */
public class DatabaseSeeder {
    private static final String TAG = "DatabaseSeeder";
    private static final String STUDY_SESSION_FILE = "initial_data/study_data.json";

    private static final Executor dbExecutor = Executors.newSingleThreadExecutor();

    public static void seedDatabase(Context context, AppDatabase database) {
        dbExecutor.execute(() -> {
            try {
                Log.d(TAG, "Seeding database...");

                if (isDatabaseEmpty(database)) {
                    Log.d(TAG, "Database is empty, proceeding with seeding...");

                    List<StudySession> sessions = loadStudySessionsFromAssets(context);

                    if(!sessions.isEmpty()){
                        database.studySessionDao().insertAll(sessions);
                        Log.d(TAG, "Successfully loaded database with " + sessions.size() + " sessions.");
                    }
                    else{
                        Log.w(TAG, "No sessions loaded from JSON file.");
                    }

                    Log.d(TAG, "Database seeding succesfull.");
                } else {
                    Log.d(TAG, "Database is not empty, skipping seeding.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during database seeding.", e);
            }
        });
    }

    private static boolean isDatabaseEmpty(AppDatabase database) {
        List<StudySession> sessions = database.studySessionDao().getAllSessions();
        return sessions == null || sessions.isEmpty();
    }

    private static List<StudySession> loadStudySessionsFromAssets(Context context) {
        List<StudySession> sessions = new ArrayList<>();

        try {
            String jsonString = readAssetFile(context, STUDY_SESSION_FILE);

            if (jsonString == null || jsonString.isEmpty()) {
                Log.w(TAG, "JSON is empty or not found: " + STUDY_SESSION_FILE);
                return sessions;
            }

            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                StudySession session = parseStudySession(jsonObject);
                if (session != null) {
                    sessions.add(session);
                }

            }

            Log.d(TAG, "Loaded: " + STUDY_SESSION_FILE + " with " + sessions.size() + " assets.");

        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }

        return sessions;
    }

    public static String readAssetFile(Context context, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to read asset: " + fileName, e);
            return null;
        }
        return stringBuilder.toString();
    }

    private static StudySession parseStudySession(JSONObject json) {
        try {
            // Create session with required fields
            StudySession session = new StudySession(
                    json.getLong("timestamp"),
                    json.getInt("durationMinutes"),
                    json.getString("tagTitle"),
                    json.getInt("tagColor")
            );

            // Set optional fields
            if (json.has("avgNoiseLevel")) {
                session.setAvgNoiseLevel((float) json.getDouble("avgNoiseLevel"));
            }
            if (json.has("avgLightLevel")) {
                session.setAvgLightLevel((float) json.getDouble("avgLightLevel"));
            }
            if (json.has("phonePickupCount")) {
                session.setPhonePickupCount(json.getInt("phonePickupCount"));
            }
            if (json.has("focusScore")) {
                session.setFocusScore((float) json.getDouble("focusScore"));
            }
            if (json.has("coinsEarned")) {
                session.setCoinsEarned(json.getInt("coinsEarned"));
            }
            if (json.has("notes")) {
                session.setNotes(json.getString("notes"));
            }

            return session;

        }
        catch (JSONException e) {
            Log.e(TAG, "Error parsing study session from JSON", e);
            return null;

        }
    }

    public static void clearDatabase(AppDatabase database) {
        dbExecutor.execute(() -> {
            try {
                Log.d(TAG, "Clearing all database data...");
                database.studySessionDao().deleteAll();
                Log.d(TAG, "Database cleared successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing database", e);
            }
        });
    }
}
