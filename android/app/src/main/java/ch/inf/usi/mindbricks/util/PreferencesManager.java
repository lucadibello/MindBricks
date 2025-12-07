package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.inf.usi.mindbricks.config.PreferencesKey;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.plan.DayHours;

public class PreferencesManager {

    private static final String PREFS_NAME = "MindBricks-Preferences";

    private final SharedPreferences preferences;
    private final Gson gson;

    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // -- Onboarding flag --
    public void setOnboardingComplete() {
        preferences.edit().putBoolean(PreferencesKey.ONBOARDING_COMPLETE.getName(), true).apply();
    }

    public boolean isOnboardingComplete() {
        return preferences.getBoolean(PreferencesKey.ONBOARDING_COMPLETE.getName(), false);
    }

    public boolean isDarkModeEnabled() {
        return preferences.getBoolean(PreferencesKey.DARK_MODE_ENABLED.getName(), false);
    }

    // -- Dark mode flag --
    public void setDarkModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(PreferencesKey.DARK_MODE_ENABLED.getName(), enabled).apply();
    }

    public boolean isNotificationEnabled() {
        return preferences.getBoolean(PreferencesKey.NOTIFICATION_ENABLED.getName(), false);
    }

    // -- Notification flag --
    public void setNotificationEnabled(boolean enabled) {
        preferences.edit().putBoolean(PreferencesKey.NOTIFICATION_ENABLED.getName(), enabled).apply();
    }

    public String getUserName() {
        return preferences.getString(PreferencesKey.USER_NAME.getName(), "");
    }

    // -- User name --
    public void setUserName(String name) {
        preferences.edit().putString(PreferencesKey.USER_NAME.getName(), name).apply();
    }

    public String getUserSurname() {
        return preferences.getString(PreferencesKey.USER_SURNAME.getName(), "");
    }

    // -- User surname --
    public void setUserSurname(String name) {
        preferences.edit().putString(PreferencesKey.USER_SURNAME.getName(), name).apply();
    }

    public String getUserEmail() {
        return preferences.getString(PreferencesKey.USER_EMAIL.getName(), "");
    }

    // -- User email --
    public void setUserEmail(String email) {
        preferences.edit().putString(PreferencesKey.USER_EMAIL.getName(), email).apply();
    }

    public String getUserFocusGoal() {
        return preferences.getString(PreferencesKey.USER_FOCUS_GOAL.getName(), "");
    }

    // -- Focus goal --
    public void setUserFocusGoal(String goal) {
        preferences.edit().putString(PreferencesKey.USER_FOCUS_GOAL.getName(), goal).apply();
    }

    public String getUserSprintLengthMinutes() {
        return preferences.getString(PreferencesKey.USER_SPRINT_LENGTH_MINUTES.getName(), "");
    }

    // -- Sprint length (minutes) --
    public void setUserSprintLengthMinutes(String minutes) {
        preferences.edit().putString(PreferencesKey.USER_SPRINT_LENGTH_MINUTES.getName(), minutes).apply();
    }

    public List<Tag> getUserTags() {
        String json = preferences.getString(PreferencesKey.USER_TAGS_JSON.getName(), "[]");
        Type type = new TypeToken<List<Tag>>() {}.getType();
        List<Tag> tags = gson.fromJson(json, type);
        return tags != null ? tags : new ArrayList<>();
    }

    // -- User tags --
    public void setUserTags(List<Tag> tags) {
        preferences.edit().putString(PreferencesKey.USER_TAGS_JSON.getName(), gson.toJson(tags)).apply();
    }

    public String getUserAvatarSeed() {
        return preferences.getString(PreferencesKey.USER_AVATAR_SEED.getName(), "");
    }

    // -- User avatar seed --
    public void setUserAvatarSeed(String seed) {
        preferences.edit().putString(PreferencesKey.USER_AVATAR_SEED.getName(), seed).apply();
    }

    /**
     * Adds the ID of a newly purchased item to the set of owned items.
     *
     * @param itemId The unique ID of the item to add.
     */
    public void purchaseItem(String itemId) {
        Set<String> purchasedIds = getPurchasedItemIds();
        purchasedIds.add(itemId);
        preferences.edit().putStringSet(PreferencesKey.USER_PURCHASED_ITEMS.getName(), purchasedIds).apply();
    }

    /**
     * Retrieves the set of IDs for all items the user has purchased.
     *
     * @return A new Set of Strings containing the IDs of purchased items. Never null.
     */
    public Set<String> getPurchasedItemIds() {
        // Retrieve the stored set. The second argument is the default value if the key is not found.
        Set<String> storedSet = preferences.getStringSet(PreferencesKey.USER_PURCHASED_ITEMS.getName(), new HashSet<>());
        // Return a new HashSet to prevent external modification of the set stored in SharedPreferences.
        return new HashSet<>(storedSet);
    }

    // -- Study plan --
    public void setStudyObjective(String objective) {
        preferences.edit().putString(PreferencesKey.STUDY_OBJECTIVE.getName(), objective).apply();
    }
    public String getStudyObjective() {
        return preferences.getString(PreferencesKey.STUDY_OBJECTIVE.getName(), "");
    }

    public void setStudyPlan(List<DayHours> plan) {
        preferences.edit().putString(PreferencesKey.STUDY_PLAN_JSON.getName(), gson.toJson(plan)).apply();
    }
    
    public boolean isStudyGoalSet() {
        return preferences.getBoolean(PreferencesKey.STUDY_GOAL_SET.getName(), false);
    }
    
    public void setStudyGoalSet(boolean isSet) {
        preferences.edit().putBoolean(PreferencesKey.STUDY_GOAL_SET.getName(), isSet).apply();
    }

    public List<DayHours> getStudyPlan() {
        String json = preferences.getString(PreferencesKey.STUDY_PLAN_JSON.getName(), "[]");
        Type type = new TypeToken<List<DayHours>>() {}.getType();
        List<DayHours> plan = gson.fromJson(json, type);
        return plan != null ? plan : new ArrayList<>();
    }

    // -- Timer Settings --
    public int getTimerStudyDuration() {
        return preferences.getInt(PreferencesKey.TIMER_STUDY_DURATION.getName(), 25);
    }

    public void setTimerStudyDuration(int minutes) {
        preferences.edit().putInt(PreferencesKey.TIMER_STUDY_DURATION.getName(), minutes).apply();
    }

    public int getTimerShortPauseDuration() {
        return preferences.getInt(PreferencesKey.TIMER_SHORT_PAUSE_DURATION.getName(), 5);
    }

    public void setTimerShortPauseDuration(int minutes) {
        preferences.edit().putInt(PreferencesKey.TIMER_SHORT_PAUSE_DURATION.getName(), minutes).apply();
    }

    public int getTimerLongPauseDuration() {
        return preferences.getInt(PreferencesKey.TIMER_LONG_PAUSE_DURATION.getName(), 15);
    }

    public void setTimerLongPauseDuration(int minutes) {
        preferences.edit().putInt(PreferencesKey.TIMER_LONG_PAUSE_DURATION.getName(), minutes).apply();
    }
}

