package ch.inf.usi.mindbricks.model.visual;

import androidx.annotation.NonNull;

/**
 * Represents tag usage statistics for pie chart visualization
 */
public class TagUsage implements Comparable<TagUsage> {

    private final String tagTitle;
    private final int tagColor;
    private final int sessionCount;
    private final int totalMinutes;
    private final float percentage;

    public TagUsage(String tagTitle, int tagColor, int sessionCount, int totalMinutes, float percentage) {
        this.tagTitle = tagTitle;
        this.tagColor = tagColor;
        this.sessionCount = sessionCount;
        this.totalMinutes = totalMinutes;
        this.percentage = percentage;
    }

    public String getTagTitle() {
        return tagTitle;
    }

    public int getTagColor() {
        return tagColor;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public float getPercentage() {
        return percentage;
    }

    @Override
    public int compareTo(@NonNull TagUsage other) {
        return Integer.compare(other.sessionCount, this.sessionCount);
    }
}