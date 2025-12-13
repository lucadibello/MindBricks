package ch.inf.usi.mindbricks.ui.nav.home.city;

import java.util.ArrayList;
import java.util.List;

public class CityRepository {

    private final List<CitySlot> slots = new ArrayList<>();

    public CityRepository() {
        int id = 0;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                slots.add(new CitySlot(id++, row, col));
            }
        }
    }

    public List<CitySlot> getSlots() {
        return slots;
    }

    public void unlockSlots(int count) {
        for (int i = 0; i < slots.size(); i++) {
            if (i < count) {
                slots.get(i).unlock();
            }
        }
    }
}
