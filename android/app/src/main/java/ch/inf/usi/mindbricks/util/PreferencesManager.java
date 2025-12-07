package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.inf.usi.mindbricks.config.PreferencesKey;
import ch.inf.usi.mindbricks.model.plan.DayHours;

public class PreferencesManager {

    private static final String PREFS_NAME = "MindBricks-Preferences";

    private final SharedPreferences preferences;

    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setOnboardingComplete() {
        preferences.edit().putBoolean(PreferencesKey.ONBOARDING_COMPLETE.getName(), true).apply();
    }

    public boolean isOnboardingComplete() {
        return preferences.getBoolean(PreferencesKey.ONBOARDING_COMPLETE.getName(), false);
    }


    public String getUserName() {
        return preferences.getString(PreferencesKey.USER_NAME.getName(), "");
    }


    public void setUserName(String name) {
        preferences.edit().putString(PreferencesKey.USER_NAME.getName(), name).apply();
    }

    public String getUserSprintLengthMinutes() {
        return preferences.getString(PreferencesKey.USER_SPRINT_LENGTH_MINUTES.getName(), "");
    }

    public void setUserSprintLengthMinutes(String minutes) {
        preferences.edit().putString(PreferencesKey.USER_SPRINT_LENGTH_MINUTES.getName(), minutes).apply();
    }

    public String getUserTagsJson() {
        return preferences.getString(PreferencesKey.USER_TAGS_JSON.getName(), "[]");
    }

    public void setUserTagsJson(String tagsJson) {
        preferences.edit().putString(PreferencesKey.USER_TAGS_JSON.getName(), tagsJson).apply();
    }

    public String getUserAvatarSeed() {
        return preferences.getString(PreferencesKey.USER_AVATAR_SEED.getName(), "");
    }

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
     * @return A new Set of Strings containing the IDs of purchased items
     */
    public Set<String> getPurchasedItemIds() {
        // Retrieve the stored set. The second argument is the default value if the key is not found
        Set<String> storedSet = preferences.getStringSet(PreferencesKey.USER_PURCHASED_ITEMS.getName(), new HashSet<>());
        // Return a new HashSet to prevent modification of the set stored in SharedPreferences.
        return new HashSet<>(storedSet);
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
        }
        return plan;
    }
}
