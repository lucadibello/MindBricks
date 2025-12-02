package ch.inf.usi.mindbricks.model.visual;

/**
 * Model representing weekly study statistics.
 * Contains aggregated data for 7 days (Monday-Sunday).
 */
public class WeeklyStats {

    // Arrays for 7 days (index 0 = Monday, 6 = Sunday)
    private final int[] dayMinutes = new int[7];
    private final float[] dayFocusScores = new float[7];
    private final int[] daySessionCounts = new int[7];

    private int totalMinutes;
    private float averageFocusScore;
    private int totalSessions;

    /**
     * Default constructor initializes arrays with zeros.
     */
    public WeeklyStats() {
        this.totalMinutes = 0;
        this.averageFocusScore = 0;
        this.totalSessions = 0;
    }

    public void setDayMinutes(int dayIndex, int minutes) {
        if (dayIndex >= 0 && dayIndex < 7) {
            dayMinutes[dayIndex] = minutes;
        }
    }

    public int getDayMinutes(int dayIndex) {
        if (dayIndex >= 0 && dayIndex < 7) {
            return dayMinutes[dayIndex];
        }
        return 0;
    }


    public void setDayFocusScore(int dayIndex, float score) {
        if (dayIndex >= 0 && dayIndex < 7) {
            dayFocusScores[dayIndex] = score;
        }
    }

    public float getDayFocusScore(int dayIndex) {
        if (dayIndex >= 0 && dayIndex < 7) {
            return dayFocusScores[dayIndex];
        }
        return 0;
    }

    public void setDaySessionCount(int dayIndex, int count) {
        if (dayIndex >= 0 && dayIndex < 7) {
            daySessionCounts[dayIndex] = count;
        }
    }

    public int getDaySessionCount(int dayIndex) {
        if (dayIndex >= 0 && dayIndex < 7) {
            return daySessionCounts[dayIndex];
        }
        return 0;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public float getAverageFocusScore() {
        return averageFocusScore;
    }

    public void setAverageFocusScore(float averageFocusScore) {
        this.averageFocusScore = averageFocusScore;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public boolean isEmpty() {
        return totalSessions == 0;
    }

    public int getMostProductiveDay() {
        int bestDay = 0;
        float bestScore = dayFocusScores[0];

        for (int i = 1; i < 7; i++) {
            if (dayFocusScores[i] > bestScore) {
                bestScore = dayFocusScores[i];
                bestDay = i;
            }
        }

        return bestDay;
    }

    public static String getDayName(int dayIndex) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday",
                "Friday", "Saturday", "Sunday"};
        if (dayIndex >= 0 && dayIndex < 7) {
            return days[dayIndex];
        }
        return "Unknown";
    }
}