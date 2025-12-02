package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.WeeklyStats;
import ch.inf.usi.mindbricks.util.visual.ChartStyleUtil;

/**
 * Custom view that displays weekly study statistics as a bar chart.
 */
public class WeeklyFocusChartView extends LinearLayout {

    private TextView titleText;
    private TextView summaryText;
    private BarChart barChart;

    // Day labels for X-axis
    private static final String[] DAY_LABELS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    public WeeklyFocusChartView(Context context) {
        super(context);
        init(context);
    }

    public WeeklyFocusChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WeeklyFocusChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Initialize the view by inflating layout and setting up chart.
     *
     * @param context Android context
     */
    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_weekly_focus_chart, this, true);

        // Find views
        titleText = findViewById(R.id.weeklyFocusTitle);
        summaryText = findViewById(R.id.weeklyFocusSummary);
        barChart = findViewById(R.id.weeklyFocusBarChart);

        // Configure the chart with default settings
        setupChart();
    }

    /**
     * Configure chart appearance and behavior.
     * This is called once during initialization.
     */
    private void setupChart() {
        // Basic chart settings
        barChart.getDescription().setEnabled(true);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(true);
        barChart.setPinchZoom(true);
        barChart.setScaleEnabled(true);
        barChart.setDoubleTapToZoomEnabled(true);
        barChart.getLegend().setEnabled(false);

        // Add some padding for better appearance
        barChart.setExtraTopOffset(10f);
        barChart.setExtraBottomOffset(10f);

        // Configure X axis (days of week)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Labels at bottom
        xAxis.setDrawGridLines(false); // No vertical grid lines
        xAxis.setGranularity(1f); // Minimum interval of 1
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setLabelCount(7); // 7 days

        // Set day labels
        xAxis.setValueFormatter(new IndexAxisValueFormatter(DAY_LABELS));

        // Configure left Y axis (minutes)
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true); // Horizontal grid lines
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setAxisMinimum(0f); // Start at 0
        leftAxis.setTextSize(12f);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setGranularity(10f); // Steps of 10 minutes

        // Format Y-axis labels to show minutes
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f min", value);
            }
        });

        // Disable right Y axis (we only need left)
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Apply standard styling
        ChartStyleUtil.applyStandardStyle(barChart, getContext());
    }

    /**
     * Update chart with new data.
     * This is the main method called from the Fragment.
     *
     * @param stats Weekly statistics data to display
     */
    public void setData(WeeklyStats stats) {
        updateSummaryText(stats);

        List<BarEntry> entries = createBarEntries(stats);

        // No data to display
        if (entries.isEmpty()) {
            showEmptyState();
            return;
        }

        // Create dataset with entries
        BarDataSet dataSet = new BarDataSet(entries, "Study Minutes");

        // Style the dataset
        styleDataSet(dataSet, stats);

        // Create BarData and set to chart
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);
        barData.setValueTextSize(10f);
        barData.setValueTextColor(Color.DKGRAY);

        // Format values on top of bars
        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Hide 0 values
                if (value == 0)
                    return "";
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        barChart.setData(barData);
        barChart.animateY(800); // 800ms animation

        // Refresh chart display
        barChart.invalidate();
    }

    private List<BarEntry> createBarEntries(WeeklyStats stats) {
        List<BarEntry> entries = new ArrayList<>();

        // Create entry for each day (0=Monday, 6=Sunday)
        for (int i = 0; i < 7; i++) {
            float minutes = stats.getDayMinutes(i);

            // Add entry even if 0, so all days show on chart
            entries.add(new BarEntry(i, minutes));
        }

        return entries;
    }

    private void styleDataSet(BarDataSet dataSet, WeeklyStats stats) {
        // Create color array for each bar
        List<Integer> colors = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            float focusScore = stats.getDayFocusScore(i);
            int color = getColorForFocusScore(focusScore);
            colors.add(color);
        }

        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.DKGRAY);

        // Enable value labels on bars
        dataSet.setDrawValues(true);
    }

    private int getColorForFocusScore(float focusScore) {
        if (focusScore >= 70) {
            return ChartStyleUtil.PROD_HIGH; // Green for high productivity
        } else if (focusScore >= 40) {
            return ChartStyleUtil.PROD_MEDIUM; // Orange for medium
        } else if (focusScore > 0) {
            return ChartStyleUtil.PROD_LOW; // Red for low
        } else {
            return Color.LTGRAY; // Gray for no data
        }
    }

    private void updateSummaryText(WeeklyStats stats) {
        int totalMinutes = stats.getTotalMinutes();
        float avgFocusScore = stats.getAverageFocusScore();

        // Convert minutes to hours and minutes
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        String summary = String.format(Locale.getDefault(),
                "Total: %dh %dm • Avg Focus: %.0f%%",
                hours, minutes, avgFocusScore);

        summaryText.setText(summary);
        summaryText.setVisibility(VISIBLE);
    }

    private void showEmptyState() {
        barChart.clear();
        barChart.setNoDataText("No study sessions this week");
        summaryText.setText("Start studying to see your weekly progress!");
        summaryText.setVisibility(VISIBLE);
    }


    public void setTitle(String title) {
        titleText.setText(title);
    }
}