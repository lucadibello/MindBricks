package ch.inf.usi.mindbricks.ui.nav.home;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.model.evaluation.UserPreferences;
import ch.inf.usi.mindbricks.util.NotificationHelper;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.SoundPlayer;
import ch.inf.usi.mindbricks.util.UserPreferencesManager;
import ch.inf.usi.mindbricks.util.VibrationHelper;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.model.questionnare.SessionQuestionnaire;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.service.SensorService;
import ch.inf.usi.mindbricks.util.evaluation.BreakManager;
import ch.inf.usi.mindbricks.util.evaluation.CueManager;
import ch.inf.usi.mindbricks.util.evaluation.FeedbackManager;
import ch.inf.usi.mindbricks.util.evaluation.TaskDifficultyRecommender;

public class HomeViewModel extends AndroidViewModel {

    public enum PomodoroState {
        IDLE,
        STUDY,
        PAUSE,
        LONG_PAUSE
    }

    public enum NextPhase {
        FOCUS,
        SHORT_BREAK,
        LONG_BREAK
    }

    private CountDownTimer timer;

    public final MutableLiveData<Long> currentTime = new MutableLiveData<>(0L);
    public final MutableLiveData<PomodoroState> currentState = new MutableLiveData<>(PomodoroState.IDLE);
    public final MutableLiveData<NextPhase> nextPhase = new MutableLiveData<>(NextPhase.FOCUS);
    public final MutableLiveData<Integer> earnedCoinsEvent = new MutableLiveData<>();

    public final MutableLiveData<Long> studyElapsedTime = new MutableLiveData<>(0L);

    private int sessionCounter = 0;
    private Tag currentSessionTag;
    private final NotificationHelper notificationHelper;
    private long currentSessionId = -1;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public final MutableLiveData<Long> showQuestionnaireEvent = new MutableLiveData<>();

    public HomeViewModel(Application application) {
        super(application);
        this.notificationHelper = new NotificationHelper(application);
    }

    // The main entry point to start a new Pomodoro cycle
    // help source: https://stackoverflow.com/questions/39215947/stuck-on-trying-to-resume-paused-stopped-function-pomodoro-timer
    public void pomodoroTechnique(int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes, Tag tag) {
        if (currentState.getValue() != PomodoroState.IDLE) {
            return;
        }

        this.currentSessionTag = tag;

        boolean hasMicPermission = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        if (!hasMicPermission) {
            android.util.Log.w("HomeViewModel", "Microphone permission not granted. SensorService will not be started.");
        }

        this.sessionCounter = 0;
        nextPhase.setValue(NextPhase.FOCUS);
        // Start the first study session
        startStudySession(studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes, hasMicPermission);
    }

    // Continues to the next phase based on the current nextPhase value
    public void continueToNextPhase(int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes) {
        if (currentState.getValue() != PomodoroState.IDLE) {
            return;
        }

        NextPhase phase = nextPhase.getValue();
        if (phase == null) phase = NextPhase.FOCUS;

        switch (phase) {
            case FOCUS:
                boolean hasMicPermission = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED;
                startStudySession(studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes, hasMicPermission);
                break;
            case SHORT_BREAK:
                startPauseSession(false, studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
                break;
            case LONG_BREAK:
                startPauseSession(true, studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes);
                break;
        }
    }


    // Starts a study session
    private void startStudySession(int studyDurationMinutes, int pauseDurationMinutes, int longPauseDurationMinutes, boolean startSensorService) {
        this.sessionCounter++;
        currentState.setValue(PomodoroState.STUDY);
        long studyDurationMillis = TimeUnit.MINUTES.toMillis(studyDurationMinutes);

        SoundPlayer.playSound(getApplication(), R.raw.start_session);
        VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.SESSION_START);

        long startTime = System.currentTimeMillis();
        Long tagId = (currentSessionTag != null) ? currentSessionTag.getId() : null;

        StudySession session = new StudySession(startTime, studyDurationMinutes, tagId);

        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            currentSessionId = db.studySessionDao().insert(session);
            android.util.Log.d("HomeViewModel", "Session inserted with ID: " + currentSessionId);

            if(startSensorService) {
                Intent serviceIntent = new Intent(getApplication(), SensorService.class);
                serviceIntent.setAction(SensorService.ACTION_START_SESSION);
                serviceIntent.putExtra(SensorService.EXTRA_SESSION_ID, currentSessionId);
                getApplication().startForegroundService(serviceIntent);
            }
        });

        timer = new CountDownTimer(studyDurationMillis, 1000) {
            private long lastMinute;

            @Override
            public void onTick(long millisUntilFinished) {
                currentTime.postValue(millisUntilFinished);
                studyElapsedTime.postValue(studyDurationMillis - millisUntilFinished);

                long currentMinute = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                if (lastMinute > currentMinute) {
                    earnedCoinsEvent.postValue(1);
                }
                lastMinute = currentMinute;
            }

            @Override
            public void onFinish() {
                SoundPlayer.playSound(getApplication(), R.raw.end_session);
                VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.SESSION_END);
                // Stop Service and complete Session
                completeSessionAndStopService();
                notificationHelper.showNotification("Study Complete!", "Time for a well-deserved break.", 1);
                earnedCoinsEvent.postValue(3);

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
        int baseBreakMinutes = isLongPause ? longPauseDurationMinutes : pauseDurationMinutes;

        PAMScore lastPAM = AppDatabase.getInstance(getApplication())
                .pamScoreDao()
                .getLatestScore();

        BreakManager breakManager = new BreakManager(getApplication());
        int adaptedBreakMinutes = breakManager.calculateAdaptiveBreakDuration(
                lastPAM, baseBreakMinutes, isLongPause
        );

        long pauseDurationMillis = TimeUnit.MINUTES.toMillis(adaptedBreakMinutes);

        // Show explanation if break was adjusted
        if (adaptedBreakMinutes != baseBreakMinutes) {
            String explanation = breakManager.getBreakDurationExplanation(
                    lastPAM, baseBreakMinutes, adaptedBreakMinutes
            );

            Toast.makeText(getApplication(), explanation, Toast.LENGTH_LONG).show();
        }
        timer = new CountDownTimer(pauseDurationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTime.postValue(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                if (isLongPause) {
                    SoundPlayer.playSound(getApplication(), R.raw.end_cycle);
                    VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.CYCLE_COMPLETE);
                    notificationHelper.showNotification("Cycle Complete!", "Great work!", 2);
                    stopTimerAndReset();
                } else {
                    notificationHelper.showNotification("Break's Over!", "Time to get back to studying.", 3);
                    boolean hasMicPermission = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED;
                    startStudySession(studyDurationMinutes, pauseDurationMinutes, longPauseDurationMinutes, hasMicPermission);
                }
            }
        }.start();
    }

    // Skips the current step and proceeds to the next one in the cycle
    public void skipCurrentStep() {
        if (timer != null) {
            timer.cancel();
        }

        PomodoroState state = currentState.getValue();
        if (state == null) state = PomodoroState.IDLE;

        // Get the stored durations from preferences
        PreferencesManager prefs = new PreferencesManager(getApplication());
        int studyDuration = prefs.getTimerStudyDuration();
        int shortPauseDuration = prefs.getTimerShortPauseDuration();
        int longPauseDuration = prefs.getTimerLongPauseDuration();

        switch (state) {
            case STUDY:
                // Complete the current study session and prepare for break
                SoundPlayer.playSound(getApplication(), R.raw.end_session);
                VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.SESSION_CANCELLED);
                completeSessionAndStopService();

                // Set up for next phase but don't start timer
                currentState.setValue(PomodoroState.IDLE);
                if (sessionCounter < 4) {
                    // Prepare for short break
                    nextPhase.setValue(NextPhase.SHORT_BREAK);
                    currentTime.setValue(TimeUnit.MINUTES.toMillis(shortPauseDuration));
                } else {
                    // Prepare for long break
                    nextPhase.setValue(NextPhase.LONG_BREAK);
                    currentTime.setValue(TimeUnit.MINUTES.toMillis(longPauseDuration));
                }
                break;

            case PAUSE:
            case LONG_PAUSE:
                // If it was a long pause, reset the cycle first (before state change)
                if (state == PomodoroState.LONG_PAUSE) {
                    SoundPlayer.playSound(getApplication(), R.raw.end_cycle);
                    VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.CYCLE_COMPLETE);
                    sessionCounter = 0;
                }

                // Prepare for next study session but don't start timer
                currentState.setValue(PomodoroState.IDLE);
                nextPhase.setValue(NextPhase.FOCUS);
                currentTime.setValue(TimeUnit.MINUTES.toMillis(studyDuration));
                break;

            default:
                // Already idle, do nothing
                break;
        }
    }

    // Stops the timer and resets the state to IDLE
    public void stopTimerAndReset() {
        if (timer != null) {
            timer.cancel();
            if(currentState.getValue() == PomodoroState.STUDY){
                SoundPlayer.playSound(getApplication(), R.raw.end_session);
                VibrationHelper.vibrate(getApplication(), VibrationHelper.VibrationType.SESSION_CANCELLED);
            }
        }
        completeSessionAndStopService();
        this.sessionCounter = 0;
        currentState.setValue(PomodoroState.IDLE);
        nextPhase.setValue(NextPhase.FOCUS);
        currentTime.setValue(0L);
        studyElapsedTime.setValue(0L);
    }

    public void saveQuestionnaireResponse(SessionQuestionnaire questionnaire) {
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            long id = db.sessionQuestionnaireDao().insert(questionnaire);
            android.util.Log.d("HomeViewModel", "Questionnaire saved with ID: " + id);
        });
    }

    public void saveQuestionnaireResponse(SessionQuestionnaire questionnaire, long sessionId, float focusScore) {
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            // Save questionnaire
            long id = db.sessionQuestionnaireDao().insert(questionnaire);
            android.util.Log.d("HomeViewModel", "Questionnaire saved with ID: " + id + ", Focus Score: " + focusScore);

            // Update session with calculated focus score
            db.studySessionDao().updateFocusScore(sessionId, focusScore);
            android.util.Log.d("HomeViewModel", "Session " + sessionId + " updated with focus score: " + focusScore);
        });
    }

    private void completeSessionAndStopService() {
        Intent serviceIntent = new Intent(getApplication(), SensorService.class);
        serviceIntent.setAction(SensorService.ACTION_STOP_SESSION);
        getApplication().startService(serviceIntent);

        if (currentSessionId != -1) {
            long sessionIdToUpdate = currentSessionId;
            dbExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(getApplication());
                float avgNoise = db.sessionSensorLogDao().getAverageNoise(sessionIdToUpdate);
                float avgLight = db.sessionSensorLogDao().getAverageLight(sessionIdToUpdate);
                int motionCount = db.sessionSensorLogDao().getMotionCount(sessionIdToUpdate);

                android.util.Log.d("HomeViewModel", "Completing session " + sessionIdToUpdate + ". Stats: Noise=" + avgNoise + ", Light=" + avgLight + ", Motion=" + motionCount);
            });
            currentSessionId = -1;
        }
    }

    public void onCoinsAwarded() {
        earnedCoinsEvent.setValue(null);
    }

    public void activityRecreated() {
        if (currentState.getValue() == PomodoroState.IDLE) {
            currentTime.setValue(0L);
        }
    }

    public int getSessionCounter() {
        return sessionCounter;
    }

    public void onQuestionnaireCompleted(SessionQuestionnaire questionnaire) {
        PAMScore pamScore = PAMScore.fromQuestionnaire(questionnaire);

        PAMScore previousPAM = AppDatabase.getInstance(getApplication())
                .pamScoreDao()
                .getLatestScore();

        if (previousPAM != null) {
            pamScore.setPreviousTotalScore(previousPAM.getTotalScore());
        }

        new Thread(() -> {
            long pamId = AppDatabase.getInstance(getApplication())
                    .pamScoreDao()
                    .insert(pamScore);
        }).start();

        CueManager envManager = new CueManager(getApplication());
        envManager.applyCues(pamScore);

        UserPreferencesManager prefsManager = new UserPreferencesManager(getApplication());
        UserPreferences.PAMThresholds thresholds = prefsManager.getPAMThresholds();

        FeedbackManager.FeedbackIntervention intervention =
                FeedbackManager.detectAndRecommend(previousPAM, pamScore, thresholds);


        List<PAMScore> last5Scores = AppDatabase.getInstance(getApplication())
                .pamScoreDao()
                .getLastNScores(5);

        TaskDifficultyRecommender.TaskRecommendation taskRec =
                TaskDifficultyRecommender.analyzeRecentSessions(last5Scores, thresholds);

        if (taskRec.getAction() != TaskDifficultyRecommender.RecommendationAction.MAINTAIN_CURRENT) {
            showTaskRecommendationDialog(taskRec);
        }
    }

    private void showAffectiveInterventionDialog(FeedbackManager.FeedbackIntervention intervention) {

        // Example using AlertDialog:
        new AlertDialog.Builder(getApplication().getApplicationContext())
                .setTitle(intervention.getTitle())
                .setMessage(intervention.getMessage())
                .setPositiveButton(intervention.getActionButton(), (dialog, which) -> {
                    if (intervention.getType() == FeedbackManager.InterventionType.BREATHING_EXERCISE) {
                        FeedbackManager.getBreathingExerciseSteps();
                    }
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void showTaskRecommendationDialog(TaskDifficultyRecommender.TaskRecommendation taskRec) {
        StringBuilder message = new StringBuilder(taskRec.getReason() + "\n\nSuggestions:\n");
        for (String suggestion : taskRec.getSpecificSuggestions()) {
            message.append("• ").append(suggestion).append("\n");
        }

        new AlertDialog.Builder(getApplication().getApplicationContext())
                .setTitle("Adjustment Recommended")
                .setMessage(message.toString())
                .setPositiveButton("Got it", null)
                .show();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        dbExecutor.shutdown();
    }
}
