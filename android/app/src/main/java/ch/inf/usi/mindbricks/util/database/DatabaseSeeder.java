package ch.inf.usi.mindbricks.util.database;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.visual.SessionSensorLog;
import ch.inf.usi.mindbricks.model.visual.StudySession;

/**
 * Utility class to seed the database with initial data from JSON files in the assets folder.
 * This runs only once when the database is first created.
 */
public class DatabaseSeeder {
    private static final String TAG = "DatabaseSeeder";
    private static final String STUDY_SESSION_FILE = "initial_data/study_data.json";

    public static void seedDatabaseOnCreate(Context context, AppDatabase database) {
        try {
            Log.d(TAG, "Seeding database on creation...");

            // create default tag
            createDefaultTag(database);

            // Load sessions and create tags first
            Map<String, Long> tagMap = new HashMap<>();
            List<StudySession> sessions = loadStudySessionsFromAssets(context, database, tagMap);

            if (!sessions.isEmpty()) {
                for (StudySession session : sessions) {
                    long id = database.studySessionDao().insert(session);
                    generateAndInsertLogs(database, id, session);
                }
                Log.d(TAG, "Successfully seeded " + sessions.size() + " sessions and " + tagMap.size() + " tags.");
            }
            else {
                Log.w(TAG, "No sessions loaded from JSON file.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during database seeding.", e);
        }

    }

    /**
     * Creates the default "No tag" tag if it doesn't exist.
     * @return The ID of the default tag
     */
    private static long createDefaultTag(AppDatabase database) {
        // Check if default tag already exists
        Tag existingTag = database.tagDao().getTagByTitle("No tag");
        if (existingTag != null) {
            Log.d(TAG, "Default tag already exists with ID: " + existingTag.getId());
            return existingTag.getId();
        }

        // Create default tag with gray color
        Tag defaultTag = new Tag("No tag", 0xFF808080);
        long tagId = database.tagDao().insert(defaultTag);
        Log.d(TAG, "Created default tag with ID: " + tagId);
        return tagId;
    }

    private static void generateAndInsertLogs(AppDatabase db, long sessionId, StudySession session) {
        Random random = new Random();
        List<SessionSensorLog> logs = new ArrayList<>();
        
        // Generate fake stats
        float targetNoise = 200 + random.nextInt(1001);
        float targetLight = 30 + random.nextInt(60);
        int targetPickups = random.nextInt(6);
        
        for (int i = 0; i < 10; i++) {
            float noise = Math.max(0, targetNoise + (random.nextInt(100) - 50));
            float light = Math.max(0, Math.min(100, targetLight + (random.nextInt(20) - 10)));
            boolean motion = i < targetPickups; 
            
            logs.add(new SessionSensorLog(
                sessionId,
                session.getTimestamp() + (i * 60000), 
                noise,
                light,
                motion,
                true
            ));
        }
        db.sessionSensorLogDao().insertAll(logs);
    }

    private static List<StudySession> loadStudySessionsFromAssets(Context context, AppDatabase database, Map<String, Long> tagMap) {
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
                StudySession session = parseStudySession(jsonObject, database, tagMap);
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

    private static StudySession parseStudySession(JSONObject json, AppDatabase database, Map<String, Long> tagMap) {
        try {
            // Get or create tag
            Long tagId;

            if (json.has("tagTitle") && json.has("tagColor")) {
                String tagTitle = json.getString("tagTitle");
                int tagColor = json.getInt("tagColor");

                tagId = tagMap.get(tagTitle);
                if (tagId == null) {
                    // Tag doesn't exist yet, create it
                    Tag tag = new Tag(tagTitle, tagColor);
                    tagId = database.tagDao().insert(tag);
                    tagMap.put(tagTitle, tagId);
                    Log.d(TAG, "Created new tag: " + tagTitle + " with ID: " + tagId);
                }
            } else {
                // No tag info in JSON, use default "No tag"
                Tag defaultTag = database.tagDao().getTagByTitle("No tag");
                if (defaultTag != null) {
                    tagId = defaultTag.getId();
                } else {
                    // Create default tag if it doesn't exist
                    tagId = createDefaultTag(database);
                }
                Log.d(TAG, "Using default tag for session (no tag info in JSON)");
            }

            // Create session with tag reference
            StudySession session = new StudySession(
                    json.getLong("timestamp"),
                    json.getInt("durationMinutes"),
                    tagId
            );

            // Set optional fields
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

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing study session from JSON", e);
            return null;

        }
    }
}
