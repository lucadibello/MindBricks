package ch.inf.usi.mindbricks.ui.nav.analytics;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.model.DailyRecommendation;
import ch.inf.usi.mindbricks.model.StudySession;
import ch.inf.usi.mindbricks.model.TimeSlotStats;
import ch.inf.usi.mindbricks.model.WeeklyStats;
import ch.inf.usi.mindbricks.repository.StudySessionRepository;
import ch.inf.usi.mindbricks.util.DataProcessor;

/**
 * ViewModel for Analytics screen.
 */
public class AnalyticsViewModel extends AndroidViewModel {

    private final StudySessionRepository repository;
    private final Executor processingExecutor;

    // LiveData for different chart types
    // MutableLiveData allows us to update values internally
    // External classes only see LiveData (read-only)
    private final MutableLiveData<WeeklyStats> weeklyStats = new MutableLiveData<>();
    private final MutableLiveData<List<TimeSlotStats>> hourlyStats = new MutableLiveData<>();
    private final MutableLiveData<DailyRecommendation> dailyRecommendation = new MutableLiveData<>();
    private final MutableLiveData<List<StudySession>> sessionHistory = new MutableLiveData<>();

    // ViewState for UI feedback
    private final MutableLiveData<ViewState> viewState = new MutableLiveData<>(ViewState.LOADING);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Cache for processed data to avoid reprocessing
    private List<StudySession> cachedSessions;
    private long lastLoadTime = 0;
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new StudySessionRepository(application);

        // Separate executor for data processing to avoid blocking database operations
        this.processingExecutor = Executors.newSingleThreadExecutor();
    }

    public void loadAnalyticsData(int daysToLoad) {
        Log.d("ViewModel", "=== loadAnalyticsData START ===");
        Log.d("ViewModel", "Setting ViewState to LOADING");
        viewState.setValue(ViewState.LOADING);

        long startTime = System.currentTimeMillis() - (daysToLoad * 24L * 60 * 60 * 1000);
        Log.d("ViewModel", "Querying sessions since: " + startTime);

        LiveData<List<StudySession>> sessionsLiveData = repository.getSessionsSince(startTime);
        Log.d("ViewModel", "Got LiveData from repository, setting up observer");

        observeOnce(sessionsLiveData, sessions -> {
            Log.d("ViewModel", "=== observeOnce CALLBACK TRIGGERED ===");
            Log.d("ViewModel", "Sessions received: " + (sessions != null ? sessions.size() : "null"));

            if (sessions == null || sessions.isEmpty()) {
                Log.w("ViewModel", "No sessions - setting ViewState to EMPTY");
                viewState.setValue(ViewState.EMPTY);
                return;
            }

            Log.d("ViewModel", "Valid sessions found, caching and processing");
            cachedSessions = sessions;
            lastLoadTime = System.currentTimeMillis();
            processAllData(sessions);
        });

        Log.d("ViewModel", "=== loadAnalyticsData END (observer registered) ===");
    }

    private void processAllData(List<StudySession> sessions) {
        Log.d("ViewModel", "=== processAllData START ===");
        Log.d("ViewModel", "Processing " + sessions.size() + " sessions on background thread");

        processingExecutor.execute(() -> {
            try {
                Log.d("ViewModel", "Background thread started");

                Log.d("ViewModel", "Calculating weekly stats...");
                WeeklyStats weekly = DataProcessor.calculateWeeklyStats(sessions);
                weeklyStats.postValue(weekly);
                Log.d("ViewModel", "Weekly stats posted");

                Log.d("ViewModel", "Calculating hourly stats...");
                List<TimeSlotStats> hourly = DataProcessor.calculateHourlyDistribution(sessions);
                hourlyStats.postValue(hourly);
                Log.d("ViewModel", "Hourly stats posted");

                Log.d("ViewModel", "Generating recommendations...");
                DailyRecommendation recommendation = DataProcessor.generateDailyRecommendation(sessions);
                dailyRecommendation.postValue(recommendation);
                Log.d("ViewModel", "Recommendations posted");

                Log.d("ViewModel", "Posting session history...");
                sessionHistory.postValue(sessions);
                Log.d("ViewModel", "Session history posted");

                Log.d("ViewModel", "*** SETTING ViewState to SUCCESS ***");
                viewState.postValue(ViewState.SUCCESS);
                Log.d("ViewModel", "=== processAllData COMPLETE ===");

            } catch (Exception e) {
                Log.e("ViewModel", "ERROR in processAllData: " + e.getMessage(), e);
                errorMessage.postValue("Error processing data: " + e.getMessage());
                viewState.postValue(ViewState.ERROR);
            }
        });
    }

    public void loadWeeklyStats() {
        if (cachedSessions != null && isCacheValid()) {
            processingExecutor.execute(() -> {
                WeeklyStats stats = DataProcessor.calculateWeeklyStats(cachedSessions);
                weeklyStats.postValue(stats);
            });
        } else {
            loadAnalyticsData(30);
        }
    }

    public void loadHourlyDistribution() {
        if (cachedSessions != null && isCacheValid()) {
            processingExecutor.execute(() -> {
                List<TimeSlotStats> stats = DataProcessor.calculateHourlyDistribution(cachedSessions);
                hourlyStats.postValue(stats);
            });
        } else {
            loadAnalyticsData(30);
        }
    }

    public void loadRecentSessions(int limit) {
        LiveData<List<StudySession>> sessionsLiveData = repository.getRecentSessions(limit);

        observeOnce(sessionsLiveData, sessions -> {
            sessionHistory.setValue(sessions);
        });
    }


    public void refreshData() {
        cachedSessions = null;
        lastLoadTime = 0;
        loadAnalyticsData(30);
    }

    public void deleteSession(StudySession session) {
        repository.deleteSession(session, this::refreshData);
    }

    // Getters for LiveData (read-only access from UI)
    public LiveData<WeeklyStats> getWeeklyStats() {
        return weeklyStats;
    }

    public LiveData<List<TimeSlotStats>> getHourlyStats() {
        return hourlyStats;
    }

    public LiveData<DailyRecommendation> getDailyRecommendation() {
        return dailyRecommendation;
    }

    public LiveData<List<StudySession>> getSessionHistory() {
        return sessionHistory;
    }

    public LiveData<ViewState> getViewState() {
        return viewState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    private boolean isCacheValid() {
        return cachedSessions != null &&
                (System.currentTimeMillis() - lastLoadTime) < CACHE_VALIDITY_MS;
    }

    private <T> void observeOnce(LiveData<T> liveData, OnDataCallback<T> callback) {
        Log.d("ViewModel", "observeOnce called");

        liveData.observeForever(new Observer<T>() {
            @Override
            public void onChanged(T data) {
                Log.d("ViewModel", "observeOnce - data received: " + (data != null));
                liveData.removeObserver(this);
                callback.onData(data);
            }
        });
    }

    private interface OnDataCallback<T> {
        void onData(T data);
    }

    public enum ViewState {
        LOADING,
        SUCCESS,
        ERROR,
        EMPTY
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        // Executor shutdown is handled automatically by garbage collection
        // But we can explicitly shut it down for cleaner code
        if (processingExecutor instanceof java.util.concurrent.ExecutorService) {
            ((java.util.concurrent.ExecutorService) processingExecutor).shutdown();
        }
    }
}