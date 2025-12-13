package ch.inf.usi.mindbricks.ui.nav.home.city;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class CityViewModel extends ViewModel {

    private final CityRepository repository = new CityRepository();
    private final MutableLiveData<List<CitySlot>> slotsLiveData = new MutableLiveData<>();

    public CityViewModel() {
        slotsLiveData.setValue(repository.getSlots());
    }

    public LiveData<List<CitySlot>> getSlots() {
        return slotsLiveData;
    }

    // called from HomeViewModel time updates
    public void onStudyTimeUpdated(long millisElapsed) {
        int minutes = (int) (millisElapsed / 60000);
        int unlockedSlots = minutes / 5;

        repository.unlockSlots(unlockedSlots);
        slotsLiveData.postValue(repository.getSlots());
    }

    public void placeBuilding(CitySlot slot, int buildingId) {
        slot.setBuildingId(buildingId);
        slotsLiveData.postValue(repository.getSlots());
    }
}
