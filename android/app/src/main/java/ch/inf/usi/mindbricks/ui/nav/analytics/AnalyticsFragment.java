package ch.inf.usi.mindbricks.ui.analytics;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.database.StudySessionDao;
import ch.inf.usi.mindbricks.model.DailyRecommendation;
import ch.inf.usi.mindbricks.model.StudySession;
import ch.inf.usi.mindbricks.model.TimeSlotStats;
import ch.inf.usi.mindbricks.model.WeeklyStats;
import ch.inf.usi.mindbricks.ui.charts.DailyTimelineChartView;
import ch.inf.usi.mindbricks.ui.charts.HourlyDistributionChartView;
import ch.inf.usi.mindbricks.ui.charts.SessionHistoryAdapter;
import ch.inf.usi.mindbricks.ui.charts.WeeklyFocusChartView;
import ch.inf.usi.mindbricks.util.DataProcessor;
import ch.inf.usi.mindbricks.util.MockDataGenerator;

/**
 * Activity that displays comprehensive study analytics
 */
public class AnalyticsActivity extends AppCompatActivity {

    private DailyTimelineChartView dailyTimelineChart;
    private WeeklyFocusChartView weeklyFocusChart;
    private HourlyDistributionChartView hourlyDistributionChart;
    private RecyclerView sessionHistoryRecycler;
    private SessionHistoryAdapter sessionAdapter;
    private ProgressBar loadingProgress;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_analytics);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        dailyTimelineChart = findViewById(R.id.dailyTimelineChart);
        weeklyFocusChart = findViewById(R.id.weeklyFocusChart);
        hourlyDistributionChart = findViewById(R.id.hourlyDistributionChart);
        sessionHistoryRecycler = findViewById(R.id.sessionHistoryRecycler);
        loadingProgress = findViewById(R.id.loadingProgress);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        // Handle session click - show detail view
        sessionAdapter = new SessionHistoryAdapter(this::showSessionDetails);

        sessionHistoryRecycler.setLayoutManager(new LinearLayoutManager(this));
        sessionHistoryRecycler.setAdapter(sessionAdapter);
    }

    private void loadData() {
        loadingProgress.setVisibility(View.VISIBLE);

        // Load data in background thread
        new Thread(() -> {
            List<StudySession> allSessions = loadSessionsFromDatabase();
            List<StudySession> recentSessions = DataProcessor.getRecentSessions(allSessions, 30);

            DailyRecommendation dailyRec = DataProcessor.generateDailyRecommendation(recentSessions);
            List<WeeklyStats> weeklyStats = DataProcessor.calculateWeeklyStats(recentSessions);
            List<TimeSlotStats> hourlyStats = DataProcessor.calculateHourlyStats(recentSessions);

            runOnUiThread(() -> {
                updateCharts(dailyRec, weeklyStats, hourlyStats);
                sessionAdapter.setData(recentSessions);
                loadingProgress.setVisibility(View.GONE);
            });
        }).start();
    }

    private void updateCharts(DailyRecommendation dailyRec,
                              List<WeeklyStats> weeklyStats,
                              List<TimeSlotStats> hourlyStats) {
        dailyTimelineChart.setData(dailyRec);
        weeklyFocusChart.setData(weeklyStats);
        hourlyDistributionChart.setData(hourlyStats);
    }

    /**
     * Load sessions from database.
     * Falls back to mock data if database is empty (for testing).
     */
    private List<StudySession> loadSessionsFromDatabase() {
        AppDatabase db = AppDatabase.getInstance(this);
        StudySessionDao dao = db.studySessionDao();

        // Get sessions from last 90 days
        long ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);
        List<StudySession> sessions = dao.getSessionsSince(ninetyDaysAgo);

        // Fallback to mock data if database is empty (for testing/demo)
        if (sessions.isEmpty()) {
            sessions = MockDataGenerator.generateMockSessions(50);
            // Optionally save mock data to database
            List<StudySession> finalSessions = sessions;
            new Thread(() -> dao.insertAll(finalSessions)).start();
        }

        return sessions;
    }

    /**
     * Show detailed information about a study session
     */
    private void showSessionDetails(StudySession session) {
        // Create a dialog view with session details
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_session_details, null);

        // Populate views
        android.widget.TextView dateText = dialogView.findViewById(R.id.sessionDate);
        android.widget.TextView durationText = dialogView.findViewById(R.id.sessionDuration);
        android.widget.TextView focusScoreText = dialogView.findViewById(R.id.sessionFocusScore);
        android.widget.TextView notesText = dialogView.findViewById(R.id.sessionNotes);

        // Format and set data
        dateText.setText(formatDate(session.getTimestamp()));
        durationText.setText(formatDuration(session.getDurationMinutes()));
        focusScoreText.setText(String.format(Locale.getDefault(),
                "Focus Score: %.1f%%", session.getFocusScore()));

        if (session.getNotes() != null && !session.getNotes().isEmpty()) {
            notesText.setText(session.getNotes());
            notesText.setVisibility(View.VISIBLE);
        } else {
            notesText.setVisibility(View.GONE);
        }

        // Show dialog
        new AlertDialog.Builder(this)
                .setTitle("Session Details")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Delete", (dialog, which) -> confirmDeleteSession(session))
                .show();
    }

    private void confirmDeleteSession(StudySession session) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Session")
                .setMessage("Are you sure you want to delete this study session?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from database
                    new Thread(() -> {
                        AppDatabase db = AppDatabase.getInstance(this);
                        db.studySessionDao().delete(session);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Session deleted", Toast.LENGTH_SHORT).show();
                            loadData(); // Reload data
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "MMM dd, yyyy 'at' HH:mm",
                Locale.getDefault()
        );
        return sdf.format(new Date(timestamp));
    }

    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, mins);
        }
        return String.format(Locale.getDefault(), "%dm", mins);
    }
}