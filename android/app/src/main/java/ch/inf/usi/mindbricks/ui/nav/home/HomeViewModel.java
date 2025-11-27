package ch.inf.usi.mindbricks.ui.nav.home;

import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.TimeUnit;

public class HomeViewModel extends ViewModel {

    private CountDownTimer countDownTimer;

    // To hold the formatted time string (e.g., "24:59")
    private final MutableLiveData<String> _timeString = new MutableLiveData<>("00:00");
    public LiveData<String> timeString = _timeString;

    // To hold the coin count
    private final MutableLiveData<Integer> _coinCount = new MutableLiveData<>(0);
    public LiveData<Integer> coinCount = _coinCount;

    // To know if the timer is running
    private final MutableLiveData<Boolean> _isTimerRunning = new MutableLiveData<>(false);
    public LiveData<Boolean> isTimerRunning = _isTimerRunning;

    public void startTimer(int minutes) {
        long durationInMillis = minutes * 60 * 1000;
        _isTimerRunning.setValue(true);

        countDownTimer = new CountDownTimer(durationInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Format the time and update LiveData
                String formattedTime = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))
                );
                _timeString.setValue(formattedTime);

                // Award a coin every minute (approximately)
                if (TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60 == 0 && millisUntilFinished > 1000) {
                    addCoin();
                }
            }

            @Override
            public void onFinish() {
                _timeString.setValue("00:00");
                _isTimerRunning.setValue(false);
                addCoin(); // Award one last coin on completion
            }
        }.start();
    }

    public void addCoin() {
        Integer currentCoins = _coinCount.getValue();
        if (currentCoins != null) {
            _coinCount.setValue(currentCoins + 1);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Prevent memory leaks
        }
    }
}
