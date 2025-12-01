package ch.inf.usi.mindbricks.util;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ProfileViewModel extends AndroidViewModel {

    // Using SharedPreferences to save coins even when the app is closed.
    private static final String PREFS_NAME = "user_profile";
    private static final String COINS_KEY = "coin_balance";

    private final android.content.SharedPreferences sharedPreferences;

    // _coins is the private mutable LiveData
    private final MutableLiveData<Integer> _coins = new MutableLiveData<>();
    // coins is the public immutable LiveData that UI will observe
    public final LiveData<Integer> coins = _coins;

    public ProfileViewModel(Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        // Load the saved coin balance when the ViewModel is created
        _coins.setValue(sharedPreferences.getInt(COINS_KEY, 0)); // Default to 0
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
        sharedPreferences.edit().putInt(COINS_KEY, balance).apply();
    }
}
