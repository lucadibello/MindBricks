package ch.inf.usi.mindbricks.ui.nav.analytics;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.model.visual.DailyRecommendation;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.model.visual.WeeklyStats;
import ch.inf.usi.mindbricks.repository.StudySessionRepository;
import ch.inf.usi.mindbricks.util.database.DataProcessor;

/**
 * ViewModel for Analytics screen.
 */
public class AnalyticsViewModel extends AndroidViewModel {

    private final StudySessionRepository repository;
    private final Executor processingExecutor;

    // LiveData for different chart types
    private final MutableLiveData<WeeklyStats> weeklyStats = new MutableLiveData<>();
    private final MutableLiveData<List<TimeSlotStats>> hourlyStats = new MutableLiveData<>();
    private final MutableLiveData<DailyRecommendation> dailyRecommendation = new MutableLiveData<>();
    private final MutableLiveData<List<StudySessionWithStats>> sessionHistory = new MutableLiveData<>();

    // ViewState for UI feedback
    private final MutableLiveData<ViewState> viewState = new MutableLiveData<>(ViewState.LOADING);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Cache for processed data to avoid reprocessing
    private List<StudySessionWithStats> cachedSessions;
    private long lastLoadTime = 0;
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
    private LiveData<List<StudySessionWithStats>> sessionsSource;
    private final Observer<List<StudySessionWithStats>> sessionsObserver = this::handleSessionsUpdate;

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new StudySessionRepository(application);
        this.processingExecutor = Executors.newSingleThreadExecutor();
    }

    public void loadAnalyticsData(int daysToLoad) {
        Log.d("ViewModel", "=== loadAnalyticsData START ===");
        viewState.setValue(ViewState.LOADING);

        long startTime = System.currentTimeMillis() - (daysToLoad * 24L * 60 * 60 * 1000);

        if (sessionsSource != null) {
            sessionsSource.removeObserver(sessionsObserver);
        }

        sessionsSource = repository.getSessionsSince(startTime);
        sessionsSource.observeForever(sessionsObserver);
    }

    private void handleSessionsUpdate(List<StudySessionWithStats> sessions) {
        if (sessions == null) {
            errorMessage.setValue("Error loading sessions");
            viewState.setValue(ViewState.ERROR);
            return;
        }

        if (sessions.isEmpty()) {
            sessionHistory.setValue(sessions);
            viewState.setValue(ViewState.EMPTY);
            return;
        }

        cachedSessions = sessions;
        lastLoadTime = System.currentTimeMillis();
        processAllData(sessions);
    }

    private void processAllData(List<StudySessionWithStats> sessions) {
        processingExecutor.execute(() -> {
            try {
                WeeklyStats weekly = DataProcessor.calculateWeeklyStats(sessions);
                weeklyStats.postValue(weekly);

                List<TimeSlotStats> hourly = DataProcessor.calculateHourlyDistribution(sessions);
                hourlyStats.postValue(hourly);

                DailyRecommendation recommendation = DataProcessor.generateDailyRecommendation(sessions);
                dailyRecommendation.postValue(recommendation);

                sessionHistory.postValue(sessions);
                viewState.postValue(ViewState.SUCCESS);

            } catch (Exception e) {
                Log.e("ViewModel", "ERROR in processAllData: " + e.getMessage(), e);
                errorMessage.postValue("Error processing data: " + e.getMessage());
                viewState.postValue(ViewState.ERROR);
            }
        });
    }

    public void refreshData() {
        cachedSessions = null;
        lastLoadTime = 0;
        loadAnalyticsData(30);
    }

    public void deleteSession(StudySessionWithStats session) {
        repository.deleteSession(session.session, this::refreshData);
    }

    // Getters for LiveData
    public LiveData<WeeklyStats> getWeeklyStats() { return weeklyStats; }
    public LiveData<List<TimeSlotStats>> getHourlyStats() { return hourlyStats; }
    public LiveData<DailyRecommendation> getDailyRecommendation() { return dailyRecommendation; }
    public LiveData<List<StudySessionWithStats>> getSessionHistory() { return sessionHistory; }
    public LiveData<ViewState> getViewState() { return viewState; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public enum ViewState {
        LOADING, SUCCESS, ERROR, EMPTY
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (sessionsSource != null) {
            sessionsSource.removeObserver(sessionsObserver);
        }
    }
}
