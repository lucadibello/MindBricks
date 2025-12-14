package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import ch.inf.usi.mindbricks.model.evaluation.UserPreferences;

/**
 * Manages loading, saving, and accessing user preferences from JSON configuration.
 * Handles both default preferences and user-customized settings.
 */
public class UserPreferencesManager {

    private static final String TAG = "UserPreferencesManager";
    private static final String PREFERENCES_FILE = "user_preferences.json";
    private static final String DEFAULT_PREFERENCES_ASSET = "default_user_preferences.json";

    private final Context context;
    private UserPreferences cachedPreferences;

    public UserPreferencesManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public UserPreferences loadPreferences() {
        // Return cached if available
        if (cachedPreferences != null) {
            return cachedPreferences;
        }

        // Try loading custom preferences first
        File prefsFile = new File(context.getFilesDir(), PREFERENCES_FILE);

        if (prefsFile.exists()) {
            try {
                String json = readFromFile(prefsFile);
                cachedPreferences = UserPreferences.fromJson(json);
                Log.i(TAG, "Loaded user preferences from storage");
                return cachedPreferences;
            } catch (IOException | JsonSyntaxException e) {
                Log.e(TAG, "Error loading user preferences, falling back to defaults", e);
            }
        }

        // Load defaults from assets
        cachedPreferences = loadDefaultPreferences();

        // Save defaults to storage for future customization
        savePreferences(cachedPreferences);

        return cachedPreferences;
    }

    private UserPreferences loadDefaultPreferences() {
        try {
            InputStream is = context.getAssets().open(DEFAULT_PREFERENCES_ASSET);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            Log.i(TAG, "Loaded default preferences from assets");
            return UserPreferences.fromJson(json);

        } catch (IOException | JsonSyntaxException e) {
            Log.e(TAG, "Error loading default preferences, using hardcoded defaults", e);
            return UserPreferences.createDefault();
        }
    }

    public boolean savePreferences(UserPreferences preferences) {
        try {
            File prefsFile = new File(context.getFilesDir(), PREFERENCES_FILE);
            String json = preferences.toJson();

            writeToFile(prefsFile, json);

            // Update cache
            cachedPreferences = preferences;

            Log.i(TAG, "User preferences saved successfully");
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error saving user preferences", e);
            return false;
        }
    }

    public void resetToDefaults() {
        cachedPreferences = loadDefaultPreferences();
        savePreferences(cachedPreferences);
        Log.i(TAG, "Preferences reset to defaults");
    }

    public boolean updatePreferenceSection(String section, Object newValue) {
        UserPreferences prefs = loadPreferences();

        // TODO: Implement granular updates if needed
        // Use reflection or manual updates based on section name

        return savePreferences(prefs);
    }

    public void clearCache() {
        cachedPreferences = null;
    }

    public boolean hasCustomPreferences() {
        File prefsFile = new File(context.getFilesDir(), PREFERENCES_FILE);
        return prefsFile.exists();
    }

    public String exportPreferences() {
        UserPreferences prefs = loadPreferences();
        return prefs.toJson();
    }

    public boolean importPreferences(String json) {
        try {
            UserPreferences prefs = UserPreferences.fromJson(json);
            return savePreferences(prefs);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Invalid JSON format for import", e);
            return false;
        }
    }

    public int getShortBreakDuration() {
        UserPreferences prefs = loadPreferences();
        return prefs.getBreakPreferences() != null ?
                prefs.getBreakPreferences().getShortBreakDuration() : 5;
    }

    public int getLongBreakDuration() {
        UserPreferences prefs = loadPreferences();
        return prefs.getBreakPreferences() != null ?
                prefs.getBreakPreferences().getLongBreakDuration() : 15;
    }

    public boolean isAdaptiveBreaksEnabled() {
        UserPreferences prefs = loadPreferences();
        return prefs.getBreakPreferences() != null &&
                prefs.getBreakPreferences().isAllowAdaptiveBreaks();
    }

    public UserPreferences.PAMThresholds getPAMThresholds() {
        UserPreferences prefs = loadPreferences();
        return prefs.getPamThresholds();
    }

    public boolean isSleepTime(int hour) {
        UserPreferences prefs = loadPreferences();
        return prefs.getSleepSchedule() != null &&
                prefs.getSleepSchedule().isSleepTime(hour);
    }

    public boolean isWorkTime(int hour, String dayOfWeek) {
        UserPreferences prefs = loadPreferences();
        UserPreferences.WorkSchedule work = prefs.getWorkSchedule();
        return work != null && work.isWorkTime(hour, dayOfWeek);
    }

    // Helper methods for file I/O
    private String readFromFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[(int) file.length()];
        fis.read(buffer);
        fis.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }

    private void writeToFile(File file, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(content.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }
}