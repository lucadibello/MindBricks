package ch.inf.usi.mindbricks.util;

import android.content.Context;import android.content.SharedPreferences;

// ADDED: Imports for Set and HashSet
import java.util.HashSet;
import java.util.Set;

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

        // ADDED: Key for storing the set of purchased item IDs
        USER_PURCHASED_ITEMS("user_purchased_items");

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

    // ⭐️ --- START: ADDED METHODS FOR PURCHASED ITEMS --- ⭐️

    /**
     * Adds the ID of a newly purchased item to the set of owned items.
     * @param itemId The unique ID of the item to add.
     */
    public void purchaseItem(String itemId) {
        Set<String> purchasedIds = getPurchasedItemIds();
        purchasedIds.add(itemId);
        preferences.edit().putStringSet(PreferencesKey.USER_PURCHASED_ITEMS.getName(), purchasedIds).apply();
    }

    /**
     * Retrieves the set of IDs for all items the user has purchased.
     * @return A new Set of Strings containing the IDs of purchased items. Never null.
     */
    public Set<String> getPurchasedItemIds() {
        // Retrieve the stored set. The second argument is the default value if the key is not found.
        Set<String> storedSet = preferences.getStringSet(PreferencesKey.USER_PURCHASED_ITEMS.getName(), new HashSet<>());
        // Return a new HashSet to prevent external modification of the set stored in SharedPreferences.
        return new HashSet<>(storedSet);
    }

    // ⭐️ --- END: ADDED METHODS FOR PURCHASED ITEMS --- ⭐️
}
