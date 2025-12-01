package ch.inf.usi.mindbricks.config;

public enum PreferencesKey {
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
    USER_PURCHASED_ITEMS("user_purchased_items"),
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
