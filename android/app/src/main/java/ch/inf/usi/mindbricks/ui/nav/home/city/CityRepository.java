package ch.inf.usi.mindbricks.ui.nav.home.city;

import java.util.ArrayList;
import java.util.List;

public class CityRepository {

    private final List<CitySlot> slots = new ArrayList<>();

    public CityRepository() {
        // Initialize a default 5x5 grid
        initSlots(5, 5);
    }

    public void initSlots(int rows, int cols) {
        slots.clear();
        int id = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                boolean unlocked = (id == 0); // base slot always unlocked
                slots.add(new CitySlot(row, col, unlocked));
            }
        }
    }

    public List<CitySlot> getSlots() {
        return slots;
    }

    public void unlockSlots(int count) {
        for (int i = 0; i < slots.size() && i < count; i++) {
            slots.get(i).setUnlocked(true);
        }
    }
}
