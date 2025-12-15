package ch.inf.usi.mindbricks.util;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ch.inf.usi.mindbricks.R;

public class ProfileViewModel extends AndroidViewModel {

    private final PreferencesManager preferencesManager;

    // _coins is the private mutable LiveData
    private final MutableLiveData<Integer> _coins = new MutableLiveData<>();
    // coins is the public immutable LiveData that UI will observe
    public final LiveData<Integer> coins = _coins;

    public ProfileViewModel(Application application) {
        super(application);
        preferencesManager = new PreferencesManager(application);

        // Load the saved coin balance when the ViewModel is created
        int saved = preferencesManager.getBalance();
        if (saved == 0) {
            int initialCoins = application.getResources().getInteger(R.integer.starting_coins);
            saveCoins(initialCoins);
        }
        _coins.setValue(saved);
    }

    public void addCoins(int amount) {
        int currentBalance = _coins.getValue() != null ? _coins.getValue() : 0;
        int newBalance = currentBalance + amount;
        _coins.setValue(newBalance);
        // Save the new balance to SharedPreferences
        saveCoins(newBalance);
    }

    public boolean spendCoins(int amount) {
        int currentBalance = _coins.getValue() != null ? _coins.getValue() : 0;
        if (currentBalance >= amount) {
            int newBalance = currentBalance - amount;
            _coins.setValue(newBalance);
            saveCoins(newBalance);
            return true; // Purchase successful
        }
        return false; // Not enough coins
    }

    private void saveCoins(int balance) {
        preferencesManager.setBalance(balance);
    }
}
