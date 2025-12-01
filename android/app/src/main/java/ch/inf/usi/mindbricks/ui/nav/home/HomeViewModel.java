package ch.inf.usi.mindbricks.ui.nav.home;

import android.os.CountDownTimer;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.TimeUnit;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> _timeString = new MutableLiveData<>("00:00");
    private final MutableLiveData<Integer> _coinCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> _isTimerRunning = new MutableLiveData<>(false);
    private CountDownTimer countDownTimer;

    public void startTimer(int minutes) {
        long durationInMillis = minutes * 60L * 1000L;
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

                // Award a coin every minute
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
