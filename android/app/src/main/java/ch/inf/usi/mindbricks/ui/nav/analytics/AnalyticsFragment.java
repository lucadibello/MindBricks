package ch.inf.usi.mindbricks.ui.nav.analytics;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.StudySession;
import ch.inf.usi.mindbricks.ui.charts.AIRecommendationCardView;
import ch.inf.usi.mindbricks.ui.charts.DailyTimelineChartView;
import ch.inf.usi.mindbricks.ui.charts.EnergyCurveChartView;
import ch.inf.usi.mindbricks.ui.charts.GoalRingsView;
import ch.inf.usi.mindbricks.ui.charts.HourlyDistributionChartView;
import ch.inf.usi.mindbricks.ui.charts.QualityHeatmapChartView;
import ch.inf.usi.mindbricks.ui.charts.SessionHistoryAdapter;
import ch.inf.usi.mindbricks.ui.charts.StreakCalendarView;
import ch.inf.usi.mindbricks.ui.charts.WeeklyFocusChartView;
import ch.inf.usi.mindbricks.util.TestDataGenerator;

/**
 * Fragment that displays analytics and visualizations of study sessions.
 *
 */
public class AnalyticsFragment extends Fragment {
    private static final String TAG = "AnalyticsFragment";

    // ViewModel
    private AnalyticsViewModel viewModel;

    // Chart views
    private WeeklyFocusChartView weeklyFocusChart;
    private HourlyDistributionChartView hourlyDistributionChart;
    private DailyTimelineChartView dailyTimelineChart;
    private EnergyCurveChartView energyCurveChart;
    private QualityHeatmapChartView qualityHeatmapChart;
    private StreakCalendarView streakCalendarView;
    private GoalRingsView goalRingsView;
    private AIRecommendationCardView aiRecommendationView;

    // Session history
    private RecyclerView sessionHistoryRecycler;
    private SessionHistoryAdapter sessionHistoryAdapter;
    private TextView sessionCountText;

    // UI state views
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private View chartsContainer;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Tab nav
    private TabLayout tabLayout;
    private View overviewContainer;
    private View insightsContainer;
    private View historyContainer;

    // Date formatters
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModelProvider ensures same instance survives configuration changes
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        // Initialize all views
        initializeViews(view);

        // Setup tab navigation
        setupTabs();

        // Setup RecyclerView for session history
        setupRecyclerView();

        // Observe ViewModel LiveData
        observeViewModel();

        // Generate test data if database is empty
        generateTestDataIfNeeded();

        // Load data (30 days by default)
        viewModel.loadAnalyticsData(30);
    }

    private void generateTestDataIfNeeded() {
        viewModel.getSessionHistory().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null && sessions.isEmpty()) {
                // Only generate if truly empty
                TestDataGenerator.addTestSessions(requireContext(), 20);

                // Wait for insertion, then reload
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    viewModel.refreshData();
                }, 2000);  // Give it time to insert
            }
        });
    }

    /**
     * Initialize all views from the layout.
     *
     * @param view Root view
     */
    private void initializeViews(View view) {
        Log.d(TAG, "Initializing views...");

        // Tab navigation
        tabLayout = view.findViewById(R.id.analyticsTabLayout);
        overviewContainer = view.findViewById(R.id.overviewContainer);
        insightsContainer = view.findViewById(R.id.insightsContainer);
        historyContainer = view.findViewById(R.id.historyContainer);

        // Chart views
        weeklyFocusChart = view.findViewById(R.id.weeklyFocusChart);
        hourlyDistributionChart = view.findViewById(R.id.hourlyDistributionChart);
        dailyTimelineChart = view.findViewById(R.id.dailyTimelineChart);
        //energyCurveChart = view.findViewById(R.id.energyCurveChart);
        //qualityHeatmapChart = view.findViewById(R.id.qualityHeatmapChart);
        //streakCalendarView = view.findViewById(R.id.streakCalendarView);
        //goalRingsView = view.findViewById(R.id.goalRingsView);
        //aiRecommendationView = view.findViewById(R.id.aiRecommendationView);

        // History
        sessionHistoryRecycler = view.findViewById(R.id.sessionHistoryRecycler);
        sessionCountText = view.findViewById(R.id.sessionCountText);

        // UI state views
        progressBar = view.findViewById(R.id.analyticsProgressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        chartsContainer = view.findViewById(R.id.chartsContainer);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshData();
        });

        FloatingActionButton fab = view.findViewById(R.id.analyticsFilterFab);
        if (fab != null) {
            fab.setOnClickListener(v -> showFilterDialog());
        }

        Log.d(TAG, "All views initialized successfully");
    }

    /**
     * Setup tab navigation
     */
    private void setupTabs() {
        Log.d(TAG, "Setting up tabs...");

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Nothing so far
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Nothing as well
            }
        });

        // Default to Overview tab
        switchContent(0);
    }

    /**
     * Switch between different tab content
     */
    private void switchContent(int position) {
        Log.d(TAG, "Switching to tab position: " + position);

        // Hide all containers first
        overviewContainer.setVisibility(View.GONE);
        insightsContainer.setVisibility(View.GONE);
        historyContainer.setVisibility(View.GONE);

        // Show selected container
        switch (position) {
            case 0: // Overview
                Log.d(TAG, "Showing Overview tab");
                overviewContainer.setVisibility(View.VISIBLE);
                break;

            case 1: // Insights
                Log.d(TAG, "Showing Insights tab");
                insightsContainer.setVisibility(View.VISIBLE);
                break;

            case 2: // History
                Log.d(TAG, "Showing History tab");
                historyContainer.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Setup RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        // Create adapter with click listener
        sessionHistoryAdapter = new SessionHistoryAdapter(new SessionHistoryAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(StudySession session) {
                showSessionDetails(session);
            }

            @Override
            public void onSessionLongClick(StudySession session) {
                showSessionOptionsDialog(session);
            }
        });

        // Set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        sessionHistoryRecycler.setLayoutManager(layoutManager);

        // Set adapter
        sessionHistoryRecycler.setAdapter(sessionHistoryAdapter);

        sessionHistoryRecycler.addItemDecoration(
                new androidx.recyclerview.widget.DividerItemDecoration(
                        requireContext(),
                        layoutManager.getOrientation()
                )
        );

        Log.d(TAG, "RecyclerView setup complete");
    }

    /**
     * Observe all LiveData from ViewModel.
     * This is where the Fragment reacts to data changes.
     */
    private void observeViewModel() {
        Log.d("Fragment", "=== Setting up observers ===");

        // Observe view state for loading/error/success
        viewModel.getViewState().observe(getViewLifecycleOwner(), state -> {
            Log.d("Fragment", "*** ViewState changed to: " + state + " ***");
            updateUIState(state);
        });

        // Observe weekly stats
        viewModel.getWeeklyStats().observe(getViewLifecycleOwner(), stats -> {
            Log.d("Fragment", "Weekly stats received: " + (stats != null ? "Yes" : "null"));
            if (stats != null) {
                weeklyFocusChart.setData(stats);
            }
        });

        // Observe hourly distribution
        viewModel.getHourlyStats().observe(getViewLifecycleOwner(), stats -> {
            Log.d("Fragment", "Hourly stats received: " + (stats != null ? stats.size() + " items" : "null"));
            if (stats != null) {
                hourlyDistributionChart.setData(stats);
            }
        });

        // Observe daily recommendations
        viewModel.getDailyRecommendation().observe(getViewLifecycleOwner(), recommendation -> {
            Log.d("Fragment", "Recommendation received: " + (recommendation != null ? "Yes" : "null"));
            if (recommendation != null) {
                dailyTimelineChart.setData(recommendation);
            }
        });

        // Observe session history
        viewModel.getSessionHistory().observe(getViewLifecycleOwner(), sessions -> {
            Log.d("Fragment", "Session history received: " + (sessions != null ? sessions.size() + " items" : "null"));
            if (sessions != null) {
                sessionHistoryAdapter.setData(sessions);
            }
        });


        Log.d("Fragment", "=== All observers registered ===");
    }

    /**
     * Update UI based on view state.
     * Shows/hides loading, error, empty, and content views.
     *
     * @param state Current view state
     */
    private void updateUIState(AnalyticsViewModel.ViewState state) {
        Log.d("Fragment", "=== updateUIState called with: " + state + " ===");

        // Stop refresh animation if running
        swipeRefreshLayout.setRefreshing(false);

        switch (state) {
            case LOADING:
                Log.d(TAG, "Showing LOADING state");
                progressBar.setVisibility(View.VISIBLE);
                chartsContainer.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.GONE);
                break;

            case SUCCESS:
                Log.d(TAG, "Showing SUCCESS state - charts visible");
                progressBar.setVisibility(View.GONE);
                chartsContainer.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
                break;

            case EMPTY:
                Log.d(TAG, "Showing EMPTY state");
                progressBar.setVisibility(View.GONE);
                chartsContainer.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("No study sessions yet.\nStart studying to see your analytics!");
                break;

            case ERROR:
                Log.d(TAG, "Showing ERROR state");
                progressBar.setVisibility(View.GONE);
                chartsContainer.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("Error loading analytics");
                Toast.makeText(getContext(), "Error loading analytics data", Toast.LENGTH_SHORT).show();
                break;
        }

        Log.d("Fragment", "UI state update complete");
    }

    /**
     * Show detailed dialog for a study session.
     *
     * @param session Session to show
     */
    private void showSessionDetails(StudySession session) {
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_session_details, null);

        // Find views in dialog
        TextView dateText = dialogView.findViewById(R.id.sessionDetailDate);
        TextView timeText = dialogView.findViewById(R.id.sessionDetailTime);
        TextView durationText = dialogView.findViewById(R.id.sessionDetailDuration);
        TextView tagText = dialogView.findViewById(R.id.sessionDetailTag);
        TextView focusScoreText = dialogView.findViewById(R.id.sessionDetailFocusScore);
        TextView noiseText = dialogView.findViewById(R.id.sessionDetailNoise);
        TextView lightText = dialogView.findViewById(R.id.sessionDetailLight);
        TextView pickupsText = dialogView.findViewById(R.id.sessionDetailPickups);
        TextView notesText = dialogView.findViewById(R.id.sessionDetailNotes);

        // Populate with session data
        Date date = new Date(session.getTimestamp());
        dateText.setText(dateFormat.format(date));
        timeText.setText(timeFormat.format(date));
        durationText.setText(formatDuration(session.getDurationMinutes()));
        tagText.setText(session.getTagTitle());
        focusScoreText.setText(String.format(Locale.getDefault(),
                "Focus Score: %.1f%%", session.getFocusScore()));
        noiseText.setText(String.format(Locale.getDefault(),
                "Noise Level: %.1f%%", session.getAvgNoiseLevel()));
        lightText.setText(String.format(Locale.getDefault(),
                "Light Level: %.1f%%", session.getAvgLightLevel()));
        pickupsText.setText(String.format(Locale.getDefault(),
                "Phone Pickups: %d", session.getPhonePickupCount()));

        // Show notes if available
        if (session.getNotes() != null && !session.getNotes().isEmpty()) {
            notesText.setText(session.getNotes());
            notesText.setVisibility(View.VISIBLE);
        } else {
            notesText.setVisibility(View.GONE);
        }

        // Show dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Session Details")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    /**
     * Show options dialog for long-press on session.
     *
     * @param session Session to show options for
     */
    private void showSessionOptionsDialog(StudySession session) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Session Options")
                .setItems(new String[]{"View Details", "Delete Session"}, (dialog, which) -> {
                    if (which == 0) {
                        showSessionDetails(session);
                    } else if (which == 1) {
                        confirmDeleteSession(session);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show confirmation dialog before deleting session.
     *
     * @param session Session to delete
     */
    private void confirmDeleteSession(StudySession session) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Session")
                .setMessage("Are you sure you want to delete this study session? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteSession(session);
                    Toast.makeText(getContext(), "Session deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show filter dialog for date range selection.
     */
    private void showFilterDialog() {
        String[] options = {"Last 7 days", "Last 30 days", "Last 90 days", "All time"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Time Range")
                .setItems(options, (dialog, which) -> {
                    int days;
                    switch (which) {
                        case 0: days = 7; break;
                        case 1: days = 30; break;
                        case 2: days = 90; break;
                        default: days = 365 * 10; break;
                    }
                    viewModel.loadAnalyticsData(days);
                })
                .show();
    }

    /**
     * Show error message to user.
     *
     * @param message Error message
     */
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Format duration in human-readable format.
     *
     * @param totalMinutes Total minutes
     * @return Formatted string
     */
    private String formatDuration(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d hour%s %d min",
                    hours, hours == 1 ? "" : "s", minutes);
        } else {
            return String.format(Locale.getDefault(), "%d minutes", minutes);
        }
    }

    /**
     * Lifecycle: Resume - refresh data if needed.
     */
    @Override
    public void onResume() {
        super.onResume();

        // Refresh data when returning to fragment -> ended session in between etc.
        viewModel.refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }
}