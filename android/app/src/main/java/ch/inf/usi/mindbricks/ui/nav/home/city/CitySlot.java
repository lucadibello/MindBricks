package ch.inf.usi.mindbricks.ui.nav.home.city;

public class CitySlot {
    private final int row;
    private final int col;
    private boolean unlocked;
    private String building;

    public CitySlot(int row, int col, boolean unlocked) {
        this.row = row;
        this.col = col;
        this.unlocked = unlocked;
        this.building = null;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
}
