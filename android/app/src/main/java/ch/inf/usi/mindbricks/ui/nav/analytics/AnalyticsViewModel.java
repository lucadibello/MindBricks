package ch.inf.usi.mindbricks.ui.nav.analytics;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.visual.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.DailyRings;
import ch.inf.usi.mindbricks.model.visual.DateRange;
import ch.inf.usi.mindbricks.model.visual.GoalRing;
import ch.inf.usi.mindbricks.model.visual.HeatmapCell;
import ch.inf.usi.mindbricks.model.visual.HourlyQuality;
import ch.inf.usi.mindbricks.model.visual.StreakDay;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.TagUsage;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.model.visual.WeeklyStats;
import ch.inf.usi.mindbricks.repository.StudySessionRepository;
import ch.inf.usi.mindbricks.util.UnifiedPreferencesManager;
import ch.inf.usi.mindbricks.util.database.DataProcessor;
import ch.inf.usi.mindbricks.util.evaluation.RecommendationEngine;

/**
 * ViewModel for Analytics screen.
 */
public class AnalyticsViewModel extends AndroidViewModel {
    private static final String TAG = "AnalyticsViewModel";
    private static final boolean VERBOSE_LOGGING = true;
    private static final int MAX_HISTORY_ITEMS = 200;

    private int historyPageSize = 100;
    private int historyCurrentPage = 1;
    private List<StudySessionWithStats> allFilteredSessions;

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
    private final MutableLiveData<List<TagUsage>> tagUsageData = new MutableLiveData<>();
    private final MutableLiveData<List<AIRecommendation>> aiRecommendations = new MutableLiveData<>();

    private List<StudySessionWithStats> allSessions;
    private List<StudySessionWithStats> allHistoricalSessions;

    // ViewState for UI feedback
    private final MutableLiveData<ViewState> viewState = new MutableLiveData<>(ViewState.LOADING);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private boolean isRefreshing = false;

    // Optimized caching system
    private static class ResultCache {
        final int sessionsHash;
        final DateRange dateRange;
        final long timestamp;

        // Cached results
        WeeklyStats weeklyStats;
        List<TimeSlotStats> hourlyStats;
        AIRecommendation dailyRecommendation;
        List<HourlyQuality> energyCurve;
        List<HeatmapCell> heatmap;
        List<StreakDay> streak;
        List<DailyRings> dailyRings;
        List<AIRecommendation> aiRecommendations;
        List<StudySessionWithStats> filteredSessions;
        List<TagUsage> tagUsage;

        ResultCache(List<StudySessionWithStats> sessions, DateRange range) {
            this.sessionsHash = sessions.hashCode();
            this.dateRange = range;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid(List<StudySessionWithStats> sessions, DateRange range) {
            return sessions.hashCode() == this.sessionsHash
                    && this.dateRange.equals(range)
                    && (System.currentTimeMillis() - timestamp) < 300000; // 5 min validity
        }
    }

    private ResultCache resultCache;
    private List<StudySessionWithStats> cachedSessions;
    private long lastLoadTime = 0;

    // Debouncing for rapid updates
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingUpdate;
    private static final long DEBOUNCE_DELAY_MS = 300;


    private LiveData<List<StudySessionWithStats>> sessionsSource;
    private final Observer<List<StudySessionWithStats>> sessionsObserver = this::handleSessionsUpdate;

    private int daysToLoad = 30;

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

        if (currentDateRange != null && currentDateRange.equals(dateRange)) {
            Log.d(TAG, "Range unchanged, skipping reload: " + dateRange.getDisplayName());
            return;
        }

        Log.d(TAG, "=== Loading data for range: " + dateRange.getDisplayName() + " ===");

        currentDateRange = dateRange;
        dateRangeLiveData.setValue(currentDateRange);

        viewState.setValue(ViewState.LOADING);

        long queryStartTime = calculateQueryStartTime(dateRange);
        Log.d(TAG, "Database query start time: " + queryStartTime);

        if (sessionsSource != null) {
            sessionsSource.removeObserver(sessionsObserver);
            sessionsSource = null;
        }

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
        if (VERBOSE_LOGGING) Log.d(TAG, ">>> handleSessionsUpdate START");
        if (VERBOSE_LOGGING) Log.d(TAG, "    Sessions count: " + (sessions != null ? sessions.size() : "null"));
        if (VERBOSE_LOGGING) Log.d(TAG, "    Current range: " + (currentDateRange != null ? currentDateRange.getDisplayName() : "null"));

        if (sessions == null) {
            Log.e(TAG, "    Sessions is NULL - setting ERROR state");
            errorMessage.setValue("Error loading sessions from database");
            viewState.setValue(ViewState.ERROR);
            return;
        }

        if (sessions.isEmpty()) {
            Log.w(TAG, "    Sessions is EMPTY - setting EMPTY state");
            sessionHistory.setValue(sessions);
            viewState.setValue(ViewState.EMPTY);
            return;
        }

        // Cache the sessions
        cachedSessions = sessions;
        lastLoadTime = System.currentTimeMillis();
        allSessions = sessions;
        allHistoricalSessions = new ArrayList<>(sessions);

        if (VERBOSE_LOGGING) Log.d(TAG, "    Moving to background thread for filtering...");

        // Move ALL processing to background thread
        processingExecutor.execute(() -> {
            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Background thread started");

            try {
                // Filter sessions for current range
                if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Filtering sessions...");
                List<StudySessionWithStats> filteredSessions = DataProcessor.filterSessionsInRange(
                        sessions, currentDateRange
                );

                if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Filtered count: " + filteredSessions.size());

                if (filteredSessions.isEmpty()) {
                    Log.w(TAG, "    [BG] No sessions in range - posting EMPTY state");
                    sessionHistory.postValue(filteredSessions);
                    viewState.postValue(ViewState.EMPTY);
                    return;
                }

                if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Starting data processing...");
                processAllDataInBackground(sessions, currentDateRange, filteredSessions);

            } catch (Exception e) {
                Log.e(TAG, "    [BG] ERROR in handleSessionsUpdate background", e);
                errorMessage.postValue("Error processing data: " + e.getMessage());
                viewState.postValue(ViewState.ERROR);
            }
        });

        if (VERBOSE_LOGGING) Log.d(TAG, "<<< handleSessionsUpdate END (background work queued)");
    }

    public void loadAllSessionsForCalendar(OnCalendarDataLoadedCallback callback) {
        processingExecutor.execute(() -> {
            try {
                // Get absolutely everything from database
                List<StudySessionWithStats> allSessionsEver = repository.getRecentSessionsSync(Integer.MAX_VALUE);

                Log.d(TAG, "Calendar data loaded: " + allSessionsEver.size() + " total sessions");

                // Callback with complete dataset
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onDataLoaded(allSessionsEver);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading calendar data", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onDataLoaded(new ArrayList<>());
                });
            }
        });
    }

    private void processAllDataInBackground(List<StudySessionWithStats> allSessions,
                                            DateRange dateRange,
                                            List<StudySessionWithStats> filteredSessions) {
        if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] >>> processAllDataInBackground START");
        if (VERBOSE_LOGGING) Log.d(TAG, "    [BG]     All sessions: " + (allSessions != null ? allSessions.size() : "null"));
        if (VERBOSE_LOGGING) Log.d(TAG, "    [BG]     Filtered: " + filteredSessions.size());
        if (VERBOSE_LOGGING) Log.d(TAG, "    [BG]     Range: " + dateRange.getDisplayName());

        try {
            // Check cache first
            if (resultCache != null && resultCache.isValid(allSessions, dateRange)) {
                if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] ✅ Using CACHED results");

                weeklyStats.postValue(resultCache.weeklyStats);
                hourlyStats.postValue(resultCache.hourlyStats);
                dailyRecommendation.postValue(resultCache.dailyRecommendation);
                energyCurveData.postValue(resultCache.energyCurve);
                heatmapData.postValue(resultCache.heatmap);
                streakData.postValue(resultCache.streak);
                dailyRingsHistory.postValue(resultCache.dailyRings);
                aiRecommendations.postValue(resultCache.aiRecommendations);
                sessionHistory.postValue(resultCache.filteredSessions);
                tagUsageData.postValue(resultCache.tagUsage);

                viewState.postValue(ViewState.SUCCESS);

                if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] <<< processAllDataInBackground END (cached)");
                return;
            }

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Cache miss - computing new results");

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Computing weekly stats...");
            WeeklyStats weekly = DataProcessor.calculateWeeklyStats(allSessions, dateRange);
            weeklyStats.postValue(weekly);

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Computing hourly stats...");
            List<TimeSlotStats> hourly = DataProcessor.calculateHourlyDistribution(allSessions, dateRange);
            hourlyStats.postValue(hourly);

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Computing AI recommendation...");
            RecommendationEngine engine = new RecommendationEngine(getApplication());
            AIRecommendation adaptiveSchedule = engine.generateAdaptiveSchedule(
                    allSessions,
                    System.currentTimeMillis()
            );
            dailyRecommendation.postValue(adaptiveSchedule);

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Computing energy curve...");
            List<HourlyQuality> energyCurve = DataProcessor.calculateEnergyCurve(filteredSessions);
            energyCurveData.postValue(energyCurve);

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Computing heatmap...");
            List<HeatmapCell> heatmap = DataProcessor.calculateQualityHeatmap(filteredSessions, dateRange);
            heatmapData.postValue(heatmap);

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Computing streak calendar...");
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

            // Daily Rings - Cap for ALL_TIME
            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Computing daily rings...");
            DateRange ringsDateRange = dateRange;
            if (dateRange.getRangeType() == DateRange.RangeType.ALL_TIME) {
                ringsDateRange = DateRange.lastNDays(90);
                if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] ALL_TIME detected, capping rings to last 90 days");
            }

            UnifiedPreferencesManager unifiedPrefs = new UnifiedPreferencesManager(getApplication());
            int dailyMinutesTarget = unifiedPrefs.getDailyStudyMinutesGoal();
            float dailyFocusTarget = unifiedPrefs.getTargetFocusScore();

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Using goals from preferences: " + dailyMinutesTarget + " min, " + dailyFocusTarget + "% focus");

            List<DailyRings> history = DataProcessor.calculateDailyRingsHistory(
                    getApplication(),
                    filteredSessions,
                    ringsDateRange,
                    dailyMinutesTarget,
                    1
            );
            dailyRingsHistory.postValue(history);

            List<StudySessionWithStats> historyToShow = filteredSessions;
            if (filteredSessions.size() > MAX_HISTORY_ITEMS) {
                Log.w(TAG, "    [BG] Capping history from " + filteredSessions.size() +
                        " to " + MAX_HISTORY_ITEMS + " most recent sessions");

                List<StudySessionWithStats> sortedSessions = new ArrayList<>(filteredSessions);
                Collections.sort(sortedSessions, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                historyToShow = sortedSessions.subList(0, MAX_HISTORY_ITEMS);
            }

            // Tag Usage
            if (VERBOSE_LOGGING)
                Log.d(TAG, "    [BG] Computing tag usage...");
            List<TagUsage> tagUsage = DataProcessor.calculateTagUsage(allSessions, dateRange, 20);
            tagUsageData.postValue(tagUsage);

            // Store complete filtered list for pagination
            allFilteredSessions = new ArrayList<>(filteredSessions);
            Collections.sort(allFilteredSessions, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            historyCurrentPage = 1;

            int endIndex = Math.min(historyPageSize, allFilteredSessions.size());
            List<StudySessionWithStats> firstPage = allFilteredSessions.subList(0, endIndex);

            sessionHistory.postValue(firstPage);

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Caching results...");
            resultCache = new ResultCache(allSessions, dateRange);
            resultCache.weeklyStats = weekly;
            resultCache.hourlyStats = hourly;
            resultCache.dailyRecommendation = adaptiveSchedule;
            resultCache.energyCurve = energyCurve;
            resultCache.heatmap = heatmap;
            resultCache.streak = streak;
            resultCache.dailyRings = history;
            resultCache.filteredSessions = filteredSessions;
            resultCache.tagUsage = tagUsage;

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] Posting SUCCESS state");
            viewState.postValue(ViewState.SUCCESS);

            if (VERBOSE_LOGGING) Log.d(TAG, "    [BG] <<< processAllDataInBackground END (success)");

        } catch (Exception e) {
            Log.e(TAG, "    [BG] ❌ ERROR in processAllDataInBackground", e);
            errorMessage.postValue("Error processing data: " + e.getMessage());
            viewState.postValue(ViewState.ERROR);
        }
    }

    public void deleteSession(StudySessionWithStats session) {
        repository.deleteSession(session.session, this::refreshData);
    }

    public void refreshData() {
        if (isRefreshing) {
            Log.d(TAG, "Refresh already in progress, skipping");
            return;
        }

        Log.d(TAG, "Refreshing data (cache invalidated)");
        isRefreshing = true;

        cachedSessions = null;
        resultCache = null;

        if (sessionsSource != null) {
            sessionsSource.removeObserver(sessionsObserver);
            sessionsSource = null;
        }

        if (currentDateRange != null) {
            // Force reload by temporarily clearing currentDateRange
            DateRange rangeToReload = currentDateRange;
            currentDateRange = null;  // This will allow loadDataForRange to proceed
            loadDataForRange(rangeToReload);
        }

        // Reset flag after a short delay
        debounceHandler.postDelayed(() -> isRefreshing = false, 1000);
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

    public LiveData<List<TagUsage>> getTagUsageData() {
        return tagUsageData;
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

        if (pendingUpdate != null) {
            debounceHandler.removeCallbacks(pendingUpdate);
        }

        resultCache = null;
        cachedSessions = null;
        allSessions = null;
    }

    public interface OnCalendarDataLoadedCallback {
        void onDataLoaded(List<StudySessionWithStats> allSessions);
    }
}
