package ch.inf.usi.mindbricks.ui.nav.home.city;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class CityViewModel extends ViewModel {

    private final List<CitySlot> internalSlots = new ArrayList<>();
    private final MutableLiveData<List<CitySlot>> slotsLiveData = new MutableLiveData<>();

    public LiveData<List<CitySlot>> getSlots() {
        return slotsLiveData;
    }

    // Initialize a grid of slots, mark the first one as unlocked
    public void initializeSlots(int rows, int cols) {
        internalSlots.clear();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean unlocked = (r == 0 && c == 0); // first slot unlocked
                internalSlots.add(new CitySlot(r, c, unlocked));
            }
        }
        slotsLiveData.setValue(new ArrayList<>(internalSlots));
    }

    // Unlock a random locked slot
    public void unlockRandomSlot() {
        List<CitySlot> lockedSlots = new ArrayList<>();
        for (CitySlot slot : internalSlots) {
            if (!slot.isUnlocked()) lockedSlots.add(slot);
        }

        if (!lockedSlots.isEmpty()) {
            int randomIndex = (int) (Math.random() * lockedSlots.size());
            lockedSlots.get(randomIndex).setUnlocked(true);
        }

        // Trigger LiveData observer
        slotsLiveData.setValue(new ArrayList<>(internalSlots));
    }
}
