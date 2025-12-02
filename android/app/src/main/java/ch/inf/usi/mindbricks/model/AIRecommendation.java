package ch.inf.usi.mindbricks.model;

/**
 * Represents an AI-generated recommendation card
 */
public class AIRecommendation {
    public enum RecommendationType {
        BEST_HOUR,          // "Best study hour today: 17:00"
        AVOID_HOUR,         // "Avoid 9-12: energy dip"
        TOMORROW_FORECAST,  // "Tomorrow: 13:00 looks excellent"
        STREAK_ALERT,       // "Keep it up! 7 day streak"
        QUALITY_TIP         // "Your focus peaks at 18:00"
    }

    private RecommendationType type;
    private String title;
    private String message;
    private int iconResource;
    private int backgroundColor;

    public AIRecommendation(RecommendationType type, String title, String message,
                            int iconResource, int backgroundColor) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.iconResource = iconResource;
        this.backgroundColor = backgroundColor;
    }

    // Getters and setters
    public RecommendationType getType() {
        return type;
    }

    public void setType(RecommendationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getIconResource() {
        return iconResource;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
