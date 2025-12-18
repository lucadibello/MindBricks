package ch.inf.usi.mindbricks.ui.nav.home;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.service.SensorService;
import ch.inf.usi.mindbricks.util.AppExecutor;
import ch.inf.usi.mindbricks.util.NotificationHelper;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.SoundPlayer;
import ch.inf.usi.mindbricks.util.VibrationHelper;
import ch.inf.usi.mindbricks.util.evaluation.BreakManager;

/**
 * ViewModel for the Home screen managing Pomodoro timer cycles.
 * Handles study sessions, breaks, and the overall Pomodoro flow.
 */
public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel";

    // ==================== LiveData State ====================

    /**
     * Current remaining time in milliseconds
     */
    public final MutableLiveData<Long> currentTime = new MutableLiveData<>(0L);

    /**
     * Current phase of the Pomodoro timer
     */
    public final MutableLiveData<Phase> currentPhase = new MutableLiveData<>(Phase.IDLE);

    /**
     * Next phase to transition to when user starts from IDLE
     */
    public final MutableLiveData<Phase> nextPhase = new MutableLiveData<>(Phase.FOCUS);

    /**
     * Event for awarding coins (value = amount, null = already handled)
     */
    public final MutableLiveData<Integer> earnedCoinsEvent = new MutableLiveData<>();

    /**
     * Elapsed time during current study session in milliseconds
     */
    public final MutableLiveData<Long> studyElapsedTime = new MutableLiveData<>(0L);

    /**
     * Event to show questionnaire dialog (value = sessionId, null = already shown)
     */
    public final MutableLiveData<Long> showQuestionnaireEvent = new MutableLiveData<>();

    // ==================== Private State ====================

    private final NotificationHelper notificationHelper;
    private CountDownTimer timer;
    private int currentPomodoroStep = 0; // 1-4 for sessions in current cycle
    private long currentSessionId = -1;
    private long currentSessionStartTime = 0;

    // ==================== Constructor ====================

    public HomeViewModel(Application application) {
        super(application);
        this.notificationHelper = new NotificationHelper(application);
    }

    /**
     * Starts a new Pomodoro cycle from the beginning.
     * Must be in IDLE state.
     *
     * @param studyDurationMinutes     duration of each study session
     * @param shortBreakDurationMinutes duration of short breaks
     * @param longBreakDurationMinutes  duration of long break
     * @param tag                       tag for the study session
     */
    public void pomodoroTechnique(int studyDurationMinutes, int shortBreakDurationMinutes,
                                  int longBreakDurationMinutes, @NonNull Tag tag) {
        if (currentPhase.getValue() != Phase.IDLE) {
            throw new IllegalStateException("Cannot start new cycle while one is in progress");
        }

        this.currentPomodoroStep = 0;
        startStudySession(studyDurationMinutes, shortBreakDurationMinutes, longBreakDurationMinutes, tag);
    }

    /**
     * Continues to the next phase in the cycle (study or break).
     * Must be in IDLE state with nextPhase set.
     *
     * @param studyDurationMinutes     duration of study sessions
     * @param shortBreakDurationMinutes duration of short breaks
     * @param longBreakDurationMinutes  duration of long break
     */
    public void continueToNextPhase(int studyDurationMinutes, int shortBreakDurationMinutes,
                                    int longBreakDurationMinutes) {
        if (currentPhase.getValue() != Phase.IDLE) {
            return;
        }

        Phase phase = nextPhase.getValue();
        if (phase == null) {
            phase = Phase.FOCUS;
        }

        // Get tag from preferences for continuing sessions
        PreferencesManager prefs = new PreferencesManager(getApplication());
        Tag tag = new Tag("Continued Session", 0xFF64B5F6); // Default tag

        switch (phase) {
            case FOCUS:
                startStudySession(studyDurationMinutes, shortBreakDurationMinutes, longBreakDurationMinutes, tag);
                break;
            case SHORT_BREAK:
                startBreakSession(false, shortBreakDurationMinutes, longBreakDurationMinutes);
                break;
            case LONG_BREAK:
                startBreakSession(true, shortBreakDurationMinutes, longBreakDurationMinutes);
                break;
        }
    }

    /**
     * Skips the current phase and moves to the next one (or IDLE state ready for next phase).
     */
    public void skipCurrentStep() {
        if (timer != null) {
            timer.cancel();
        }

        Phase phase = currentPhase.getValue();
        if (phase == null || phase == Phase.IDLE) {
            return;
        }

        PreferencesManager prefs = new PreferencesManager(getApplication());
        int studyDuration = prefs.getTimerStudyDuration();
        int shortBreakDuration = prefs.getTimerShortPauseDuration();
        int longBreakDuration = prefs.getTimerLongPauseDuration();

        switch (phase) {
            case FOCUS:
                handleStudySkip(shortBreakDuration, longBreakDuration);
                break;

            case SHORT_BREAK:
            case LONG_BREAK:
                handleBreakSkip(phase, studyDuration);
                break;
        }
    }

    /**
     * Stops the current cycle and resets to IDLE state.
     */
    public void stopTimerAndReset() {
        if (timer != null) {
            timer.cancel();
            if (currentPhase.getValue() == Phase.FOCUS) {
                SoundPlayer.playSound(getApplication(), R.raw.end_session);
                VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.SESSION_CANCELLED);
            }
        }

        completeCurrentSession();
        resetToIdle();
    }

    /**
     * Saves a questionnaire response to the database.
     *
     * @param questionnaire the questionnaire to save
     */
    public void saveQuestionnaireResponse(SessionQuestionnaire questionnaire) {
        AppExecutor.getInstance().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            long id = db.sessionQuestionnaireDao().insert(questionnaire);
            Log.d(TAG, "Questionnaire saved with ID: " + id);
        });
    }

    /**
     * Saves a questionnaire response and updates the session's focus score.
     *
     * @param questionnaire the questionnaire to save
     * @param sessionId     the session ID to update
     * @param focusScore    the calculated focus score
     */
    public void saveQuestionnaireResponse(SessionQuestionnaire questionnaire, long sessionId, float focusScore) {
        AppExecutor.getInstance().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            long id = db.sessionQuestionnaireDao().insert(questionnaire);
            db.studySessionDao().updateFocusScore(sessionId, focusScore);
            Log.d(TAG, "Questionnaire saved with ID: " + id + ", focus score updated for session " + sessionId);
        });
    }

    /**
     * Clears the coins awarded event after it has been handled.
     */
    public void onCoinsAwarded() {
        earnedCoinsEvent.setValue(null);
    }

    /**
     * Called when the activity is recreated to refresh the timer display.
     */
    public void activityRecreated() {
        if (currentPhase.getValue() == Phase.IDLE) {
            currentTime.setValue(0L);
        }
    }

    /**
     * Gets the current session counter (1-4 for sessions in current cycle).
     *
     * @return the session counter
     */
    public int getSessionCounter() {
        return currentPomodoroStep;
    }

    /**
     * Starts a new study session.
     */
    private void startStudySession(int studyDurationMinutes, int shortBreakDurationMinutes,
                                   int longBreakDurationMinutes, @NonNull Tag tag) {
        currentPomodoroStep++;
        nextPhase.setValue(Phase.FOCUS);
        currentPhase.setValue(Phase.FOCUS);

        // User feedback
        SoundPlayer.playSound(getApplication(), R.raw.start_session);
        VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.SESSION_START);

        // Create and save study session
        long studyDurationMillis = TimeUnit.MINUTES.toMillis(studyDurationMinutes);
        currentSessionStartTime = System.currentTimeMillis();

        StudySession session = new StudySession(currentSessionStartTime, studyDurationMinutes, tag.getId());

        boolean hasMicPermission = ContextCompat.checkSelfPermission(getApplication(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        AppExecutor.getInstance().execute(() -> {
            currentSessionId = AppDatabase.getInstance(getApplication())
                    .studySessionDao().insert(session);
            Log.d(TAG, "Study session started with ID: " + currentSessionId);

            // Start sensor service if permission granted
            if (hasMicPermission) {
                startSensorService(currentSessionId);
            }
        });

        // Start countdown timer
        startStudyTimer(studyDurationMillis, shortBreakDurationMinutes, longBreakDurationMinutes);
    }

    /**
     * Creates and starts the countdown timer for a study session.
     */
    private void startStudyTimer(long durationMillis, int shortBreakMinutes, int longBreakMinutes) {
        timer = new CountDownTimer(durationMillis, 1000) {
            private long lastMinute = -1;

            @Override
            public void onTick(long millisUntilFinished) {
                currentTime.postValue(millisUntilFinished);
                studyElapsedTime.postValue(durationMillis - millisUntilFinished);

                // Award 1 coin per minute elapsed
                long currentMinute = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                if (lastMinute > currentMinute) {
                    earnedCoinsEvent.postValue(1);
                }
                lastMinute = currentMinute;
            }

            @Override
            public void onFinish() {
                handleStudyCompletion(shortBreakMinutes, longBreakMinutes);
            }
        };
        timer.start();
    }

    /**
     * Handles the completion of a study session.
     */
    private void handleStudyCompletion(int shortBreakMinutes, int longBreakMinutes) {
        SoundPlayer.playSound(getApplication(), R.raw.end_session);
        VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.SESSION_END);

        completeCurrentSession();

        notificationHelper.showNotification("Study Complete!", "Time for a well-deserved break.", 1);
        earnedCoinsEvent.postValue(3); // Bonus coins for completing session

        // Trigger questionnaire
        if (currentSessionId != -1) {
            showQuestionnaireEvent.postValue(currentSessionId);
        }

        // Start break (long break after 4 sessions)
        boolean isLongBreak = currentPomodoroStep >= 4;
        startBreakSession(isLongBreak, shortBreakMinutes, longBreakMinutes);
    }

    /**
     * Handles skipping a study session.
     */
    private void handleStudySkip(int shortBreakMinutes, int longBreakMinutes) {
        SoundPlayer.playSound(getApplication(), R.raw.end_session);
        VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.SESSION_CANCELLED);

        completeCurrentSession();

        // Set up for next break but don't start it
        currentPhase.setValue(Phase.IDLE);
        if (currentPomodoroStep < 4) {
            nextPhase.setValue(Phase.SHORT_BREAK);
            currentTime.setValue(TimeUnit.MINUTES.toMillis(shortBreakMinutes));
        } else {
            nextPhase.setValue(Phase.LONG_BREAK);
            currentTime.setValue(TimeUnit.MINUTES.toMillis(longBreakMinutes));
        }
    }

    /**
     * Starts a break session (short or long).
     */
    private void startBreakSession(boolean isLongBreak, int shortBreakMinutes, int longBreakMinutes) {
        AppExecutor.getInstance().execute(() -> {
            // Calculate adaptive break duration based on PAM score
            PAMScore lastPAM = AppDatabase.getInstance(getApplication())
                    .pamScoreDao()
                    .getLatestScore();

            int baseBreakMinutes = isLongBreak ? longBreakMinutes : shortBreakMinutes;
            BreakManager breakManager = new BreakManager(getApplication());
            int adaptedBreakMinutes = breakManager.calculateAdaptiveBreakDuration(
                    lastPAM, baseBreakMinutes, isLongBreak
            );

            // Show explanation if break was adjusted
            if (adaptedBreakMinutes != baseBreakMinutes) {
                String explanation = breakManager.getBreakDurationExplanation(
                        lastPAM, baseBreakMinutes, adaptedBreakMinutes
                );
                Toast.makeText(getApplication(), explanation, Toast.LENGTH_LONG).show();
            }

            // Update state and start timer
            currentPhase.postValue(isLongBreak ? Phase.LONG_BREAK : Phase.SHORT_BREAK);
            startBreakTimer(adaptedBreakMinutes, isLongBreak);
        });
    }

    /**
     * Creates and starts the countdown timer for a break.
     */
    private void startBreakTimer(int breakMinutes, boolean isLongBreak) {
        long breakDurationMillis = TimeUnit.MINUTES.toMillis(breakMinutes);

        timer = new CountDownTimer(breakDurationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTime.postValue(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                handleBreakCompletion(isLongBreak);
            }
        };
        timer.start();
    }

    /**
     * Handles the completion of a break.
     */
    private void handleBreakCompletion(boolean isLongBreak) {
        if (isLongBreak) {
            // Long break completes the entire cycle
            SoundPlayer.playSound(getApplication(), R.raw.end_cycle);
            VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.CYCLE_COMPLETE);
            notificationHelper.showNotification("Cycle Complete!", "Great work!", 2);
            resetToIdle();
        } else {
            // Short break - ready for next study session
            SoundPlayer.playSound(getApplication(), R.raw.end_session);
            VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.SESSION_END);
            notificationHelper.showNotification("Break's Over!", "Time to get back to studying.", 3);

            // Set up for next study session but don't start it
            currentPhase.setValue(Phase.IDLE);
            nextPhase.setValue(Phase.FOCUS);

            PreferencesManager prefs = new PreferencesManager(getApplication());
            currentTime.setValue(TimeUnit.MINUTES.toMillis(prefs.getTimerStudyDuration()));
        }
    }

    /**
     * Handles skipping a break.
     */
    private void handleBreakSkip(Phase phase, int studyDuration) {
        // If skipping long break, reset cycle
        if (phase == Phase.LONG_BREAK) {
            SoundPlayer.playSound(getApplication(), R.raw.end_cycle);
            VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.CYCLE_COMPLETE);
            currentPomodoroStep = 0;
        }

        // Prepare for next study session but don't start it
        currentPhase.setValue(Phase.IDLE);
        nextPhase.setValue(Phase.FOCUS);
        currentTime.setValue(TimeUnit.MINUTES.toMillis(studyDuration));
    }

    /**
     * Completes the current study session and stops the sensor service.
     */
    private void completeCurrentSession() {
        stopSensorService();

        if (currentSessionId != -1) {
            final long sessionId = currentSessionId;
            final long startTime = currentSessionStartTime;

            AppExecutor.getInstance().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(getApplication());

                // Calculate actual duration
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int durationMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(elapsedMillis);

                // Ensure at least 1 minute if > 30 seconds
                if (durationMinutes == 0 && elapsedMillis > 30000) {
                    durationMinutes = 1;
                }

                // Update session duration
                db.studySessionDao().updateDuration(sessionId, durationMinutes);

                // Log session statistics
                float avgNoise = db.sessionSensorLogDao().getAverageNoise(sessionId);
                float avgLight = db.sessionSensorLogDao().getAverageLight(sessionId);
                int motionCount = db.sessionSensorLogDao().getMotionCount(sessionId);

                Log.d(TAG, String.format("Session %d completed: %dm, Noise=%.1f, Light=%.1f, Motion=%d",
                        sessionId, durationMinutes, avgNoise, avgLight, motionCount));
            });

            currentSessionId = -1;
            currentSessionStartTime = 0;
        }
    }

    /**
     * Resets the ViewModel to IDLE state.
     */
    private void resetToIdle() {
        currentPomodoroStep = 0;
        currentPhase.setValue(Phase.IDLE);
        nextPhase.setValue(Phase.FOCUS);
        currentTime.setValue(0L);
        studyElapsedTime.setValue(0L);
    }

    /**
     * Starts the sensor service for the given session.
     */
    private void startSensorService(long sessionId) {
        Intent serviceIntent = new Intent(getApplication(), SensorService.class);
        serviceIntent.setAction(SensorService.ACTION_START_SESSION);
        serviceIntent.putExtra(SensorService.EXTRA_SESSION_ID, sessionId);
        getApplication().startForegroundService(serviceIntent);
    }

    /**
     * Stops the sensor service.
     */
    private void stopSensorService() {
        Intent serviceIntent = new Intent(getApplication(), SensorService.class);
        serviceIntent.setAction(SensorService.ACTION_STOP_SESSION);
        getApplication().startService(serviceIntent);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
     * Represents the current phase in a Pomodoro cycle.
     */
    public enum Phase {
        /**
         * Idle state, waiting to start a new cycle.
         */
        IDLE,

        /**
         * Current study session in progress.
         */
        FOCUS,

        /**
         * Short break in progress.
         */
        SHORT_BREAK,

        /**
         * Long break in progress.
         */
        LONG_BREAK
    }
}
