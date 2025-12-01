package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.content.SharedPreferences;

import ch.inf.usi.mindbricks.model.plan.DayHours;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PreferencesManager {

    private static final String PREFS_NAME = "MindBricks-Preferences";

    private enum PreferencesKey {
        ONBOARDING_COMPLETE("onboarding_complete"),
        DARK_MODE_ENABLED("dark_mode_enabled"),
        NOTIFICATION_ENABLED("notification_enabled"),
        USER_NAME("user_name"),
        USER_SURNAME("user_surname"),
        USER_EMAIL("user_email"),
        USER_FOCUS_GOAL("user_focus_goal"),
        USER_SPRINT_LENGTH_MINUTES("user_sprint_length_minutes"),
        USER_TAGS_JSON("user_tags_json"),
        USER_AVATAR_SEED("user_avatar_seed"),
        STUDY_OBJECTIVE("study_objective"),
        STUDY_PLAN_JSON("study_plan_json");



        private final String name;
        PreferencesKey(String name) {
           this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final SharedPreferences preferences;

    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // -- Onboarding flag --
    public void setOnboardingComplete() {
        preferences.edit().putBoolean(PreferencesKey.ONBOARDING_COMPLETE.getName(), true).apply();
    }
    public boolean isOnboardingComplete() {
        return preferences.getBoolean(PreferencesKey.ONBOARDING_COMPLETE.getName(), false);
    }

    // -- Dark mode flag --
    public void setDarkModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(PreferencesKey.DARK_MODE_ENABLED.getName(), enabled).apply();
    }
    public boolean isDarkModeEnabled() {
        return preferences.getBoolean(PreferencesKey.DARK_MODE_ENABLED.getName(), false);
    }

    // -- Notification flag --
    public void setNotificationEnabled(boolean enabled) {
        preferences.edit().putBoolean(PreferencesKey.NOTIFICATION_ENABLED.getName(), enabled).apply();
    }
    public boolean isNotificationEnabled() {
        return preferences.getBoolean(PreferencesKey.NOTIFICATION_ENABLED.getName(), false);
    }

    // -- User name --
    public void setUserName(String name) {
        preferences.edit().putString(PreferencesKey.USER_NAME.getName(), name).apply();
    }
    public String getUserName() {
        return preferences.getString(PreferencesKey.USER_NAME.getName(), "");
    }

    // -- User surname --
    public void setUserSurname(String name) {
        preferences.edit().putString(PreferencesKey.USER_SURNAME.getName(), name).apply();
    }
    public String getUserSurname() {
        return preferences.getString(PreferencesKey.USER_SURNAME.getName(), "");
    }

    // -- User email --
    public void setUserEmail(String email) {
        preferences.edit().putString(PreferencesKey.USER_EMAIL.getName(), email).apply();
    }
    public String getUserEmail() {
        return preferences.getString(PreferencesKey.USER_EMAIL.getName(), "");
    }

    // -- Focus goal --
    public void setUserFocusGoal(String goal) {
        preferences.edit().putString(PreferencesKey.USER_FOCUS_GOAL.getName(), goal).apply();
    }
    public String getUserFocusGoal() {
        return preferences.getString(PreferencesKey.USER_FOCUS_GOAL.getName(), "");
    }

    // -- Sprint length (minutes) --
    public void setUserSprintLengthMinutes(String minutes) {
        preferences.edit().putString(PreferencesKey.USER_SPRINT_LENGTH_MINUTES.getName(), minutes).apply();
    }
    public String getUserSprintLengthMinutes() {
        return preferences.getString(PreferencesKey.USER_SPRINT_LENGTH_MINUTES.getName(), "");
    }

    // -- User tags --
    public void setUserTagsJson(String tagsJson) {
        preferences.edit().putString(PreferencesKey.USER_TAGS_JSON.getName(), tagsJson).apply();
    }
    public String getUserTagsJson() {
        return preferences.getString(PreferencesKey.USER_TAGS_JSON.getName(), "[]");
    }

    // -- User avatar seed --
    public void setUserAvatarSeed(String seed) {
        preferences.edit().putString(PreferencesKey.USER_AVATAR_SEED.getName(), seed).apply();
    }
    public String getUserAvatarSeed() {
        return preferences.getString(PreferencesKey.USER_AVATAR_SEED.getName(), "");
    }

    // -- Study plan --
    public void setStudyObjective(String objective) {
        preferences.edit().putString(PreferencesKey.STUDY_OBJECTIVE.getName(), objective).apply();
    }
    public String getStudyObjective() {
        return preferences.getString(PreferencesKey.STUDY_OBJECTIVE.getName(), "");
    }

    public void setStudyPlan(List<DayHours> plan) {
        JSONArray array = new JSONArray();
        for (DayHours dayHours : plan) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("day", dayHours.dayKey());
                obj.put("hours", dayHours.hours());
                array.put(obj);
            } catch (JSONException ignored) {
                // should not happen with valid keys
            }
        }
        preferences.edit().putString(PreferencesKey.STUDY_PLAN_JSON.getName(), array.toString()).apply();
    }

    public List<DayHours> getStudyPlan() {
        List<DayHours> plan = new ArrayList<>();
        String json = preferences.getString(PreferencesKey.STUDY_PLAN_JSON.getName(), "[]");
        if (json.isEmpty()) return plan;

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject entry = array.getJSONObject(i);
                String day = entry.optString("day", "");
                float hours = (float) entry.optDouble("hours", 0);
                if (!day.isEmpty() && hours > 0) {
                    plan.add(new DayHours(day, hours));
                }
            }
        } catch (JSONException ignored) {
            // ignore malformed stored data
        }
        return plan;
    }
}
