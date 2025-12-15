package ch.inf.usi.mindbricks.model.visual;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents goal rings data for a single day
 */
public class DailyRings {
    private LocalDate date;
    private String displayDate;
    private List<GoalRing> rings;
    private boolean isToday;
    private boolean isEmpty;
    private int totalMinutes;
    private int sessionCount;

    public DailyRings() {
        this.rings = new ArrayList<>();
    }

    public DailyRings(LocalDate date, List<GoalRing> rings) {
        this.date = date;
        this.rings = rings != null ? rings : new ArrayList<>();
        this.isToday = date.equals(LocalDate.now());
        this.isEmpty = this.rings.isEmpty() || getTotalMinutes() == 0;
        formatDisplayDate();
    }
    private void formatDisplayDate() {
        if (date == null) {
            displayDate = "";
            return;
        }

        LocalDate now = LocalDate.now();
        if (date.equals(now)) {
            displayDate = "Today";
        } else if (date.equals(now.minusDays(1))) {
            displayDate = "Yesterday";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            displayDate = date.format(formatter);
        }
    }

    private int getTotalMinutes() {
        if (rings == null || rings.isEmpty()) {
            return 0;
        }
        // Assuming first ring is Study Time
        return (int) rings.get(0).getCurrent();
    }

    // Getters and setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
        this.isToday = date.equals(LocalDate.now());
        formatDisplayDate();
    }

    public String getDisplayDate() {
        return displayDate;
    }

    public void setDisplayDate(String displayDate) {
        this.displayDate = displayDate;
    }

    public List<GoalRing> getRings() {
        return rings;
    }

    public void setRings(List<GoalRing> rings) {
        this.rings = rings;
        this.isEmpty = rings == null || rings.isEmpty() || getTotalMinutes() == 0;
    }

    public boolean isToday() {
        return isToday;
    }

    public void setToday(boolean today) {
        isToday = today;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty(boolean empty) {
        isEmpty = empty;
    }

    public int getSessionCount() {
        if (rings != null && rings.size() >= 3) {
            return (int) rings.get(2).getCurrent();
        }
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public String getSummary() {
        if (isEmpty) {
            return "No study sessions";
        }
        int minutes = getTotalMinutes();
        int sessions = getSessionCount();
        return String.format("%d min • %d session%s", minutes, sessions, sessions != 1 ? "s" : "");
    }
}