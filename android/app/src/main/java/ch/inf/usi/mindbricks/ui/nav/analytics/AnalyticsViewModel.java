package ch.inf.usi.mindbricks.ui.nav.analytics;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.model.visual.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.DailyRings;
import ch.inf.usi.mindbricks.model.visual.DateRange;
import ch.inf.usi.mindbricks.model.visual.GoalRing;
import ch.inf.usi.mindbricks.model.visual.HeatmapCell;
import ch.inf.usi.mindbricks.model.visual.HourlyQuality;
import ch.inf.usi.mindbricks.model.visual.StreakDay;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.model.visual.WeeklyStats;
import ch.inf.usi.mindbricks.repository.StudySessionRepository;
import ch.inf.usi.mindbricks.util.database.DataProcessor;
import ch.inf.usi.mindbricks.util.evaluation.RecommendationEngine;

/**
 * ViewModel for Analytics screen.
 */
public class AnalyticsViewModel extends AndroidViewModel {
    private static final String TAG = "AnalyticsViewModel";

    private final StudySessionRepository repository;
    private final Executor processingExecutor;

    //date ranges
    private DateRange currentDateRange;
    private final MutableLiveData<DateRange> dateRangeLiveData = new MutableLiveData<>();

    // LiveData for different chart types
    private final MutableLiveData<WeeklyStats> weeklyStats = new MutableLiveData<>();
    private final MutableLiveData<List<TimeSlotStats>> hourlyStats = new MutableLiveData<>();
    private final MutableLiveData<AIRecommendation> dailyRecommendation = new MutableLiveData<AIRecommendation>();
    private final MutableLiveData<List<StudySessionWithStats>> sessionHistory = new MutableLiveData<>();
    private final MutableLiveData<List<HourlyQuality>> energyCurveData = new MutableLiveData<>();
    private final MutableLiveData<List<HeatmapCell>> heatmapData = new MutableLiveData<>();
    private final MutableLiveData<List<StreakDay>> streakData = new MutableLiveData<>();
    private final MutableLiveData<List<GoalRing>> goalRingsData = new MutableLiveData<>();
    private MutableLiveData<List<DailyRings>> dailyRingsHistory = new MutableLiveData<>();
    private MutableLiveData<Boolean> isRingsExpanded = new MutableLiveData<>(false);

    private final MutableLiveData<List<AIRecommendation>> aiRecommendations = new MutableLiveData<>();

    // ViewState for UI feedback
    private final MutableLiveData<ViewState> viewState = new MutableLiveData<>(ViewState.LOADING);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Cache for processed data to avoid reprocessing
    private List<StudySessionWithStats> cachedSessions;
    private long lastLoadTime = 0;
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
    private LiveData<List<StudySessionWithStats>> sessionsSource;
    private final Observer<List<StudySessionWithStats>> sessionsObserver = this::handleSessionsUpdate;

    private int daysToLoad = 30;
    private List<StudySessionWithStats> allSessions;

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new StudySessionRepository(application);

        // Separate executor for data processing to avoid blocking database operations
        this.processingExecutor = Executors.newSingleThreadExecutor();

        // Initialize with default range: Last 30 days
        this.currentDateRange = DateRange.lastNDays(30);
        this.dateRangeLiveData.setValue(currentDateRange);
    }


    // loading data
    public void loadData() {
        loadDataForRange(currentDateRange);
    }

    public void loadLastNDays(int days) {
        DateRange range = DateRange.lastNDays(days);
        loadDataForRange(range);
    }

    public void loadMonth(int year, int month) {
        DateRange range = DateRange.forMonth(year, month);
        loadDataForRange(range);
    }

    public void loadCurrentMonth() {
        DateRange range = DateRange.currentMonth();
        loadDataForRange(range);
    }

    public void loadAllTime() {
        DateRange range = DateRange.allTime();
        loadDataForRange(range);
    }

    public void loadDataForRange(DateRange dateRange) {
        if (dateRange == null) {
            Log.e(TAG, "Cannot load data with null DateRange");
            return;
        }

        // Check if range actually changed to avoid unnecessary work
        if (currentDateRange != null && currentDateRange.equals(dateRange)) {
            Log.d(TAG, "Range unchanged, skipping reload: " + dateRange.getDisplayName());
            return;
        }

        Log.d(TAG, "=== Loading data for range: " + dateRange.getDisplayName() + " ===");

        currentDateRange = dateRange;
        dateRangeLiveData.setValue(currentDateRange);

        viewState.setValue(ViewState.LOADING);

        long startTime = System.currentTimeMillis() - (daysToLoad * 24L * 60 * 60 * 1000);
        long queryStartTime = calculateQueryStartTime(dateRange);

        Log.d(TAG, "Database query start time: " + queryStartTime);

        if (sessionsSource != null) {
            sessionsSource.removeObserver(sessionsObserver);
        }

        sessionsSource = repository.getSessionsSince(startTime);
        sessionsSource = repository.getSessionsSince(queryStartTime);
        sessionsSource.observeForever(sessionsObserver);
    }

    // calculation

    private long calculateQueryStartTime(DateRange range) {
        // For ALL_TIME, query from epoch
        if (range.getRangeType() == DateRange.RangeType.ALL_TIME) {
            return 0L;
        }

        long bufferMs = 7L * 24 * 60 * 60 * 1000;
        long queryStart = range.getStartTimestamp() - bufferMs;

        Log.d(TAG, "Query start: " + new Date(queryStart) +
                " (Range start: " + new Date(range.getStartTimestamp()) + ")");

        return Math.max(0, queryStart);
    }

    // navigation
    public void previousMonth() {
        if (currentDateRange.getRangeType() != DateRange.RangeType.SPECIFIC_MONTH) {
            Log.w(TAG, "previousMonth() only works for SPECIFIC_MONTH ranges");
            return;
        }

        try {
            DateRange prevMonth = currentDateRange.previousMonth();
            loadDataForRange(prevMonth);
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "Error navigating to previous month", e);
        }
    }

    public void nextMonth() {
        if (currentDateRange.getRangeType() != DateRange.RangeType.SPECIFIC_MONTH) {
            Log.w(TAG, "nextMonth() only works for SPECIFIC_MONTH ranges");
            return;
        }

        DateRange nextMonth = currentDateRange.nextMonth();
        loadDataForRange(nextMonth);
    }

    private void handleSessionsUpdate(List<StudySessionWithStats> sessions) {
        Log.d(TAG, "Sessions updated from database: " +
                (sessions != null ? sessions.size() + " sessions" : "null"));

        if (sessions == null) {
            errorMessage.setValue("Error loading sessions from database");
            viewState.setValue(ViewState.ERROR);
            return;
        }

        if (sessions.isEmpty()) {
            sessionHistory.setValue(sessions);
            viewState.setValue(ViewState.EMPTY);
            return;
        }

        // Cache the sessions
        cachedSessions = sessions;
        lastLoadTime = System.currentTimeMillis();

        // Store all sessions for processing
        allSessions = sessions;

        // Filter sessions for current range
        List<StudySessionWithStats> filteredSessions = DataProcessor.filterSessionsInRange(
                sessions, currentDateRange
        );

        if (filteredSessions.isEmpty()) {
            Log.w(TAG, "No sessions in current range: " + currentDateRange.getDisplayName());
            sessionHistory.setValue(filteredSessions);
            viewState.setValue(ViewState.EMPTY);
            return;
        }

        processAllData(allSessions, currentDateRange);
    }

    private void processAllData(List<StudySessionWithStats> sessions, DateRange dateRange) {
        Log.d(TAG, "=== processAllData START ===");
        Log.d(TAG, "All sessions count: " + (allSessions != null ? allSessions.size() : "null"));
        Log.d(TAG, "Date range: " + dateRange.getDisplayName());

        processingExecutor.execute(() -> {
            try {
                // Filter first to check if we have data
                List<StudySessionWithStats> filtered = DataProcessor.filterSessionsInRange(
                        allSessions, dateRange);

                Log.d(TAG, "Filtered sessions: " + filtered.size());

                if (filtered.isEmpty()) {
                    sessionHistory.postValue(filtered);
                    viewState.postValue(ViewState.EMPTY);
                    return;
                }

                // Process data
                WeeklyStats weekly = DataProcessor.calculateWeeklyStats(allSessions, dateRange);
                weeklyStats.postValue(weekly);

                List<TimeSlotStats> hourly = DataProcessor.calculateHourlyDistribution(allSessions, dateRange);
                hourlyStats.postValue(hourly);

                RecommendationEngine engine = new RecommendationEngine(getApplication());
                AIRecommendation adaptiveSchedule = engine.generateAdaptiveSchedule(
                        allSessions,
                        System.currentTimeMillis()
                );

                dailyRecommendation.postValue(adaptiveSchedule);
                Log.d(TAG, "Recommendations computed");


                List<HourlyQuality> energyCurve = DataProcessor.calculateEnergyCurve(filtered);
                energyCurveData.postValue(energyCurve);

                List<HeatmapCell> heatmap = DataProcessor.calculateQualityHeatmap(filtered, dateRange);
                heatmapData.postValue(heatmap);

                Calendar cal = Calendar.getInstance();
                int currentMonth = cal.get(Calendar.MONTH);
                int currentYear = cal.get(Calendar.YEAR);

                List<StreakDay> streak = DataProcessor.calculateStreakCalendar(
                        allSessions,
                        60,
                        currentMonth,
                        currentYear
                );
                streakData.postValue(streak);

                /*
                List<StudySessionWithStats> todaySessions = DataProcessor.filterSessionsInRange(
                        allSessions, DateRange.lastNDays(1));
                List<GoalRing> rings = DataProcessor.calculateGoalRings(context, todaySessions, 120, 70);
                goalRingsData.postValue(rings);
                */

                List<DailyRings> history = DataProcessor.calculateDailyRingsHistory(
                        getApplication(),
                        allSessions,
                        currentDateRange,
                        30,
                        5
                );
                dailyRingsHistory.postValue(history);

                List<AIRecommendation> recommendations = new ArrayList<>();
                recommendations.add(DataProcessor.generateAIRecommendations(filtered, getApplication().getApplicationContext(), dateRange));
                aiRecommendations.postValue(recommendations);

                sessionHistory.postValue(filtered);
                viewState.postValue(ViewState.SUCCESS);

                // Set SUCCESS only after all data is posted
                viewState.postValue(ViewState.SUCCESS);

            } catch (Exception e) {
                Log.e(TAG, "ERROR in processAllData", e);
                errorMessage.postValue("Error processing data: " + e.getMessage());
                viewState.postValue(ViewState.ERROR);
            }
        });
    }

    public void deleteSession(StudySessionWithStats session) {
        repository.deleteSession(session.session, this::refreshData);
    }

    public void refreshData() {
        Log.d(TAG, "Refreshing data (cache invalidated)");
        cachedSessions = null;
        lastLoadTime = 0;
        loadData();
    }

    public void setRingsExpanded(boolean expanded) {
        isRingsExpanded.setValue(expanded);
    }

    // getters
    public LiveData<DateRange> getDateRange() {
        return dateRangeLiveData;
    }

    public DateRange getCurrentDateRange() {
        return currentDateRange;
    }

    // Getters for LiveData
    public LiveData<WeeklyStats> getWeeklyStats() {
        return weeklyStats;
    }

    public LiveData<List<TimeSlotStats>> getHourlyStats() {
        return hourlyStats;
    }

    public LiveData<AIRecommendation> getDailyRecommendation() {
        return dailyRecommendation;
    }

    public LiveData<List<HourlyQuality>> getEnergyCurveData() {
        return energyCurveData;
    }

    public LiveData<List<HeatmapCell>> getHeatmapData() {
        return heatmapData;
    }

    public LiveData<List<StreakDay>> getStreakData() {
        return streakData;
    }

    public LiveData<List<GoalRing>> getGoalRingsData() {
        return goalRingsData;
    }

    public LiveData<List<AIRecommendation>> getAiRecommendations() {
        return aiRecommendations;
    }

    public LiveData<List<StudySessionWithStats>> getSessionHistory() {
        return sessionHistory;
    }

    public LiveData<ViewState> getViewState() {
        return viewState;
    }

    public LiveData<List<DailyRings>> getDailyRingsHistory() {
        return dailyRingsHistory;
    }

    public LiveData<Boolean> isRingsExpanded() {
        return isRingsExpanded;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public enum ViewState {
        LOADING, SUCCESS, ERROR, EMPTY
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        Log.d(TAG, "ViewModel cleared");

        if (sessionsSource != null) {
            sessionsSource.removeObserver(sessionsObserver);
        }
    }
}
