package ch.inf.usi.mindbricks.ui.nav.home;

import android.app.Application;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.concurrent.TimeUnit;
import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.util.SessionRecordingManager;

public class HomeViewModel extends AndroidViewModel {

    public enum PomodoroState {
        IDLE,
        STUDY,
        PAUSE,
        LONG_PAUSE
    }

    private final AppDatabase db;
    private final SessionRecordingManager sessionRecordingManager;
    private CountDownTimer countDownTimer;
    private StudySession currentStudySession;

    private long startTimeInMillis = 0;
    private long remainingTimeInMillis = 0;
    private int sessionCounter = 0;
    private int studyDurationMinutes = 0;
    private int pauseDurationMinutes = 0;
    private int lastMinuteAwarded = 0;

    private final MutableLiveData<Long> _currentTime = new MutableLiveData<>(0L);
    public final LiveData<Long> currentTime = _currentTime;

    private final MutableLiveData<PomodoroState> _currentState = new MutableLiveData<>(PomodoroState.IDLE);
    public final LiveData<PomodoroState> currentState = _currentState;

    private final MutableLiveData<String> _stateTitle = new MutableLiveData<>("Ready to start?");
    public final LiveData<String> stateTitle = _stateTitle;

    private final MutableLiveData<Integer> _earnedCoinsEvent = new MutableLiveData<>();
    public final LiveData<Integer> earnedCoinsEvent = _earnedCoinsEvent;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application.getApplicationContext());
        sessionRecordingManager = new SessionRecordingManager(application.getApplicationContext());
    }

    public void pomodoroTechnique(int studyMin, int pauseMin) {
        if (_currentState.getValue() != PomodoroState.IDLE) {
            return;
        }
        this.studyDurationMinutes = studyMin;
        this.pauseDurationMinutes = pauseMin;
        this.sessionCounter = 0;
        this.lastMinuteAwarded = 0;
        startStudyTimer();
    }

    private void startStudyTimer() {
        sessionCounter++;
        _currentState.setValue(PomodoroState.STUDY);
        _stateTitle.setValue("Study Session " + sessionCounter);
        startTimeInMillis = TimeUnit.MINUTES.toMillis(studyDurationMinutes);
        remainingTimeInMillis = startTimeInMillis;
        lastMinuteAwarded = 0; // Reset for the new study session

        new Thread(() -> {
            currentStudySession = new StudySession(System.currentTimeMillis(), studyDurationMinutes, "Pomodoro", android.graphics.Color.CYAN);
            long newId = db.studySessionDao().insert(currentStudySession);
            currentStudySession.setId(newId);

            new Handler(Looper.getMainLooper()).post(() -> {
                sessionRecordingManager.startSession(newId);
                startCountdown(remainingTimeInMillis);
            });
        }).start();
    }

    public void startPause(boolean isLongPause) {
        _currentState.setValue(isLongPause ? PomodoroState.LONG_PAUSE : PomodoroState.PAUSE);
        _stateTitle.setValue(isLongPause ? "Long Break" : "Short Break");

        int duration = isLongPause ? (pauseDurationMinutes * 3) : pauseDurationMinutes;
        startTimeInMillis = TimeUnit.MINUTES.toMillis(duration);
        remainingTimeInMillis = startTimeInMillis;
        startCountdown(remainingTimeInMillis);
    }

    public void stopTimerAndReset() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (currentStudySession != null) {
            sessionRecordingManager.stopSession(currentStudySession);
            currentStudySession = null;
        }

        _currentState.setValue(PomodoroState.IDLE);
        _stateTitle.setValue("Ready to start?");
        _currentTime.setValue(0L);
    }

    private void startCountdown(long duration) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeInMillis = millisUntilFinished;
                _currentTime.postValue(millisUntilFinished);

                if (_currentState.getValue() == PomodoroState.STUDY) {
                    // Calculate how many full minutes have passed.
                    long elapsedMillis = startTimeInMillis - millisUntilFinished;
                    int minutesPassed = (int) TimeUnit.MILLISECONDS.toMinutes(elapsedMillis);

                    // If a new minute has passed that we haven't awarded a coin for yet.
                    if (minutesPassed > 0 && minutesPassed > lastMinuteAwarded) {
                        lastMinuteAwarded = minutesPassed;
                        _earnedCoinsEvent.postValue(1);
                    }
                }
            }

            @Override
            public void onFinish() {
                PomodoroState finishedState = _currentState.getValue();

                if (finishedState == PomodoroState.STUDY) {
                    // Stop recording the session.
                    if (currentStudySession != null) {
                        sessionRecordingManager.stopSession(currentStudySession);
                        currentStudySession = null;
                    }

                    // Award bonus coins for completing the study block.
                    // The per-minute coin is handled by onTick, so we only give the bonus here.
                    _earnedCoinsEvent.postValue(3);

                    // Now, decide the next state (break).
                    boolean isLongBreakTime = (sessionCounter >= 3);
                    startPause(isLongBreakTime);

                } else if (finishedState == PomodoroState.PAUSE) {
                    // A short pause finished. Start the next study block.
                    startStudyTimer();

                } else if (finishedState == PomodoroState.LONG_PAUSE) {
                    // The long pause finished. The entire cycle is complete.
                    stopTimerAndReset();
                }
            }
        }.start();
    }

    public void activityRecreated() {
        if (_currentState.getValue() != PomodoroState.IDLE && remainingTimeInMillis > 0) {
            _currentState.setValue(_currentState.getValue());
            _stateTitle.setValue(_stateTitle.getValue());
            startCountdown(remainingTimeInMillis);
        }
    }

    public void onCoinsAwarded() {
        _earnedCoinsEvent.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (currentStudySession != null) {
            sessionRecordingManager.stopSession(currentStudySession);
        }
    }
}
