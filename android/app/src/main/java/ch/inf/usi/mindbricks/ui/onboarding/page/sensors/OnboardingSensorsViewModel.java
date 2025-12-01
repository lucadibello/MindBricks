package ch.inf.usi.mindbricks.ui.onboarding.page.sensors;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class OnboardingSensorsViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> hasLightSensor = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> hasSignificantMotionSensor = new MutableLiveData<>(false);

    // permissions / acknowledgements
    private MutableLiveData<Boolean> hasRecordingPermission = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> hasAcknowledgedPickup = new MutableLiveData<>(false);


    public OnboardingSensorsViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<Boolean> getHasLightSensor() {
        return hasLightSensor;
    }

    public MutableLiveData<Boolean> getHasSignificantMotionSensor() {
        return hasSignificantMotionSensor;
    }

    public MutableLiveData<Boolean> getHasRecordingPermission() {
        return hasRecordingPermission;
    }

    public void setHasRecordingPermission(boolean b) {
        hasRecordingPermission.setValue(b);
    }

    public MutableLiveData<Boolean> getHasAcknowledgedPickup() {
        return hasAcknowledgedPickup;
    }

    public void setHasAcknowledgedPickup(boolean value) {
        hasAcknowledgedPickup.setValue(value);
    }
}
