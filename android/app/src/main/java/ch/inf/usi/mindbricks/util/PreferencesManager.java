package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREFS_NAME = "MindBricks-Preferences";

    private enum PreferencesKey {
        ONBOARDING_COMPLETE("onboarding_complete"),
        DARK_MODE_ENABLED("dark_mode_enabled"),
        NOTIFICATION_ENABLED("notification_enabled"),
        USER_NAME("user_name"),
        USER_SURNAME("user_surname"),
        USER_EMAIL("user_email");



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
}
