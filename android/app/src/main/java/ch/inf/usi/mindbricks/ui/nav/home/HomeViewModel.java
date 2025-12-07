package ch.inf.usi.mindbricks.ui.nav.home;

import android.app.Application;
import android.os.CountDownTimer;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.concurrent.TimeUnit;
import ch.inf.usi.mindbricks.util.NotificationHelper;

public class HomeViewModel extends AndroidViewModel {

    public enum PomodoroState {
        IDLE,
        STUDY,
        PAUSE,
        LONG_PAUSE
    }

    private CountDownTimer timer;

    public final MutableLiveData<Long> currentTime = new MutableLiveData<>(0L);
    public final MutableLiveData<PomodoroState> currentState = new MutableLiveData<>(PomodoroState.IDLE);
    public final MutableLiveData<Integer> earnedCoinsEvent = new MutableLiveData<>();

    private int sessionCounter = 0;
    private final NotificationHelper notificationHelper;

    public HomeViewModel(Application application) {
        super(application);
        this.notificationHelper = new NotificationHelper(application);
    }

    // The main entry point to start a new Pomodoro cycle
    //help source: https://stackoverflow.com/questions/39215947/stuck-on-trying-to-resume-paused-stopped-function-pomodoro-timer
    public void pomodoroTechnique(int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes) {
        if (currentState.getValue() != PomodoroState.IDLE) {
            return;
        }
        // Reset the session counter at the beginning of a new cycle
        this.sessionCounter = 0;
        // Start the first study session
        startStudySession(studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
    }

    // Starts a study session
    private void startStudySession(int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes) {
        this.sessionCounter++;
        currentState.setValue(PomodoroState.STUDY); // Set the state to STUDY
        long studyDurationMillis = TimeUnit.MINUTES.toMillis(studyDurationMinutes);

        timer = new CountDownTimer(studyDurationMillis, 1000) {
            private long lastMinute;

            @Override
            public void onTick(long millisUntilFinished) {
                currentTime.postValue(millisUntilFinished);

                // Award 1 coin for each completed minute of studying.
                long currentMinute = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                if (lastMinute > currentMinute) {
                    earnedCoinsEvent.postValue(1);
                }
                lastMinute = currentMinute; // Update the last minute tracker
            }

            @Override
            public void onFinish() {
                notificationHelper.showNotification("Study Complete!", "Time for a well-deserved break.", 1);
                // Award 3 bonus coins at the end of the session
                earnedCoinsEvent.postValue(3);

                // Decide whether to start a short pause or a long pause
                if (sessionCounter < 4) {
                    startPauseSession(false, studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
                } else {
                    startPauseSession(true, studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
                }
            }
        }.start();
    }

    // Starts a pause session
    // help source: https://www.reddit.com/r/developersIndia/comments/v5b06t/i_built_a_pomodoro_timer_to_demonstrate_how_a/
    private void startPauseSession(boolean isLongPause, int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes) {
        long pauseDurationMillis;
        if (isLongPause) {
            currentState.setValue(PomodoroState.LONG_PAUSE);
            pauseDurationMillis = TimeUnit.MINUTES.toMillis(longPauseDurationMinutes);
        } else {
            currentState.setValue(PomodoroState.PAUSE);
            pauseDurationMillis = TimeUnit.MINUTES.toMillis(pauseDurationMinutes);
        }

        // Create and start a new countdown timer for the pause
        timer = new CountDownTimer(pauseDurationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTime.postValue(millisUntilFinished); // Update the UI with  remaining time
            }

            @Override
            public void onFinish() {
                // end the cycle if longpause
                if (isLongPause) {
                    notificationHelper.showNotification("Cycle Complete!", "Great work. Ready for the next round?", 2);
                    stopTimerAndReset();
                } else {
                    //  continue to the next study session.
                    notificationHelper.showNotification("Break's Over!", "Time to get back to studying.", 3);
                    startStudySession(studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
                }
            }
        }.start();
    }

    // Stops the timer and resets the state to IDLE
    public void stopTimerAndReset() {
        if (timer != null) {
            timer.cancel();
        }
        this.sessionCounter = 0;
        currentState.setValue(PomodoroState.IDLE);
        currentTime.setValue(0L);
    }

    public void onCoinsAwarded() {
        earnedCoinsEvent.setValue(null);
    }

    // Resets the timer display if the activity is recreated while the timer is idle
    public void activityRecreated() {
        if (currentState.getValue() == PomodoroState.IDLE) {
            currentTime.setValue(0L);
        }
    }

    public int getSessionCounter() {
        return sessionCounter;
    }
}
