package ch.inf.usi.mindbricks.ui.nav.home.city;

public class CitySlot {
    private final int row;
    private final int col;
    private boolean unlocked;

    public CitySlot(int row, int col, boolean unlocked) {
        this.row = row;
        this.col = col;
        this.unlocked = unlocked;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}
