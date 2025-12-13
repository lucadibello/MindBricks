package ch.inf.usi.mindbricks.ui.nav.home.city;

public class CitySlot {

    public final int id;
    public final int row;
    public final int col;

    private boolean unlocked;
    private Integer buildingId;

    public CitySlot(int id, int row, int col) {
        this.id = id;
        this.row = row;
        this.col = col;
        this.unlocked = false;
        this.buildingId = null;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void unlock() {
        this.unlocked = true;
    }

    public Integer getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Integer buildingId) {
        this.buildingId = buildingId;
    }
}
