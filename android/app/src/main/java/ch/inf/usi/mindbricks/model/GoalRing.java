package ch.inf.usi.mindbricks.model;

/**
 * Represents a single achievement ring (Apple Activity style)
 */
public class GoalRing {
    private String title;
    private float current;          // Current value
    private float target;           // Target value
    private int color;              // Ring color
    private String unit;

    public GoalRing(String title, float current, float target, int color, String unit) {
        this.title = title;
        this.current = current;
        this.target = target;
        this.color = color;
        this.unit = unit;
    }

    public float getProgress() {
        if (target == 0) return 0;
        return Math.min((current / target) * 100, 100);
    }

    public boolean isAchieved() {
        return current >= target;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getCurrent() {
        return current;
    }

    public void setCurrent(float current) {
        this.current = current;
    }

    public float getTarget() {
        return target;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
