package ch.inf.usi.mindbricks.ui.nav.home;

import android.app.Application;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.NotificationHelper;
import ch.inf.usi.mindbricks.util.SoundPlayer;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.service.SensorService;

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
    private long currentSessionId = -1;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public HomeViewModel(Application application) {
        super(application);
        this.notificationHelper = new NotificationHelper(application);
    }

    // The main entry point to start a new Pomodoro cycle
    // help source: https://stackoverflow.com/questions/39215947/stuck-on-trying-to-resume-paused-stopped-function-pomodoro-timer
    public void pomodoroTechnique(int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes) {
        // Prevent starting a new timer if one is already running.
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

        SoundPlayer.playSound(getApplication(), R.raw.start_session);

        // Save new session (to get its id) + start foreground service
        long startTime = System.currentTimeMillis();
        StudySession session = new StudySession(startTime, studyDurationMinutes, "General", Color.GRAY);

        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            currentSessionId = db.studySessionDao().insert(session);
            android.util.Log.d("HomeViewModel", "Session inserted with ID: " + currentSessionId);

            Intent serviceIntent = new Intent(getApplication(), SensorService.class);
            serviceIntent.setAction(SensorService.ACTION_START_SESSION);
            serviceIntent.putExtra(SensorService.EXTRA_SESSION_ID, currentSessionId);
            getApplication().startForegroundService(serviceIntent);
        });

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
                SoundPlayer.playSound(getApplication(), R.raw.end_session);
                // Stop Service and complete Session
                completeSessionAndStopService();
                notificationHelper.showNotification("Study Complete!", "Time for a well-deserved break.", 1);

                // Award 3 bonus coins at the successful end of the session
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

        // Create and start a new timer for the pause
        timer = new CountDownTimer(pauseDurationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTime.postValue(millisUntilFinished); // Update the UI with  remaining time
            }

            @Override
            public void onFinish() {
                // end the cycle if long pause
                if (isLongPause) {
                    SoundPlayer.playSound(getApplication(), R.raw.end_cycle);
                    notificationHelper.showNotification("Cycle Complete!", "Great work!", 2);
                    stopTimerAndReset();
                } else {
                    //  continue to the next study session
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
            if(currentState.getValue() == PomodoroState.STUDY){
                SoundPlayer.playSound(getApplication(), R.raw.end_session);
            }
        }
        completeSessionAndStopService();
        this.sessionCounter = 0;
        currentState.setValue(PomodoroState.IDLE);
        currentTime.setValue(0L);
    }

    private void completeSessionAndStopService() {
        // Stop service
        Intent serviceIntent = new Intent(getApplication(), SensorService.class);
        serviceIntent.setAction(SensorService.ACTION_STOP_SESSION);
        getApplication().startService(serviceIntent);

        // Store data in DB
        if (currentSessionId != -1) {
            long sessionIdToUpdate = currentSessionId;
            dbExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(getApplication());
                // Get aggregation
                float avgNoise = db.sessionSensorLogDao().getAverageNoise(sessionIdToUpdate);
                float avgLight = db.sessionSensorLogDao().getAverageLight(sessionIdToUpdate);
                int motionCount = db.sessionSensorLogDao().getMotionCount(sessionIdToUpdate);

                android.util.Log.d("HomeViewModel", "Completing session " + sessionIdToUpdate + ". Stats: Noise=" + avgNoise + ", Light=" + avgLight + ", Motion=" + motionCount);
                // TODO: Implement actual DB update for stats
            });
            currentSessionId = -1;
        }
    }

    // Resets the coin event LiveData to prevent it from re-firing
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

    @Override
    protected void onCleared() {
        super.onCleared();
        dbExecutor.shutdown();
    }
}
