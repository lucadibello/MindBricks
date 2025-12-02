package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.util.visual.ChartStyleUtil;

/**
 * Custom view that displays hourly study distribution.
 *
 */
public class HourlyDistributionChartView extends LinearLayout {

    private TextView titleText;
    private TextView summaryText;
    private LineChart lineChart;

    public HourlyDistributionChartView(Context context) {
        super(context);
        init(context);
    }

    public HourlyDistributionChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HourlyDistributionChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_hourly_distribution_chart, this, true);

        titleText = findViewById(R.id.hourlyDistributionTitle);
        summaryText = findViewById(R.id.hourlyDistributionSummary);
        lineChart = findViewById(R.id.hourlyDistributionLineChart);

        setupChart();
    }

    private void setupChart() {
        // Basic settings
        lineChart.getDescription().setEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setPinchZoom(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDoubleTapToZoomEnabled(true);
        lineChart.setTouchEnabled(true); // Allow touch to see values

        // Configure legend
        Legend legend = lineChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(11f);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        // Configure X axis (hours)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setLabelRotationAngle(-45f);

        // Set hour labels (0-23)
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int hour = (int) value;
                if (hour < 0 || hour > 23) return "";

                // Format as 12-hour time
                if (hour == 0) return "12AM";
                if (hour < 12) return hour + "AM";
                if (hour == 12) return "12PM";
                return (hour - 12) + "PM";
            }
        });

        // Configure Y axes
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(11f);
        leftAxis.setTextColor(Color.DKGRAY);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Apply standard styling
        ChartStyleUtil.applyStandardStyle(lineChart, getContext());
    }

    public void setData(List<TimeSlotStats> hourlyStats) {
        if (hourlyStats == null || hourlyStats.isEmpty()) {
            showEmptyState();
            return;
        }

        List<TimeSlotStats> activeHours = filterActiveHours(hourlyStats);

        if (activeHours.isEmpty()) {
            showEmptyState();
            return;
        }

        // Create datasets for minutes and focus score
        LineDataSet minutesDataSet = createMinutesDataSet(activeHours);
        LineDataSet focusDataSet = createFocusDataSet(activeHours);

        // Create line data with both datasets
        LineData lineData = new LineData(minutesDataSet, focusDataSet);
        lineChart.setData(lineData);
        updateSummary(activeHours);

        // Animate
        lineChart.animateX(800);
        lineChart.invalidate();
    }

    private List<TimeSlotStats> filterActiveHours(List<TimeSlotStats> allHours) {
        List<TimeSlotStats> active = new ArrayList<>();

        for (TimeSlotStats stats : allHours) {
            if (stats.getSessionCount() > 0) {
                active.add(stats);
            }
        }

        return active;
    }

    private LineDataSet createMinutesDataSet(List<TimeSlotStats> hours) {
        List<Entry> entries = new ArrayList<>();

        for (TimeSlotStats stats : hours) {
            int hour = stats.getHourOfDay();
            float minutes = stats.getTotalMinutes();
            entries.add(new Entry(hour, minutes));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Study Minutes");

        // Style the line
        dataSet.setColor(ChartStyleUtil.COLOR_PRIMARY);
        dataSet.setCircleColor(ChartStyleUtil.COLOR_PRIMARY);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curves
        dataSet.setCubicIntensity(0.2f);

        // Fill under line
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ChartStyleUtil.COLOR_PRIMARY);
        dataSet.setFillAlpha(30); // Semi-transparent

        // Format values
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        return dataSet;
    }

    private LineDataSet createFocusDataSet(List<TimeSlotStats> hours) {
        List<Entry> entries = new ArrayList<>();

        for (TimeSlotStats stats : hours) {
            int hour = stats.getHourOfDay();
            float focusScore = stats.getAverageFocusScore();
            entries.add(new Entry(hour, focusScore));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Focus Score");

        // Style differently from minutes line
        dataSet.setColor(ChartStyleUtil.COLOR_SUCCESS);
        dataSet.setCircleColor(ChartStyleUtil.COLOR_SUCCESS);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        // Dashed line for distinction
        dataSet.enableDashedLine(10f, 5f, 0f);

        // Format values
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return String.format(Locale.getDefault(), "%.0f%%", value);
            }
        });

        return dataSet;
    }

    private void updateSummary(List<TimeSlotStats> hours) {
        // Find hour with highest focus score
        TimeSlotStats bestHour = hours.get(0);
        for (TimeSlotStats stats : hours) {
            if (stats.getAverageFocusScore() > bestHour.getAverageFocusScore()) {
                bestHour = stats;
            }
        }

        // Format hour as readable time
        String bestTime = formatHour(bestHour.getHourOfDay());

        String summary = String.format(Locale.getDefault(),
                "Best focus time: %s (%.0f%% focus)",
                bestTime, bestHour.getAverageFocusScore());

        summaryText.setText(summary);
        summaryText.setVisibility(VISIBLE);
    }

    private String formatHour(int hour) {
        if (hour == 0) return "12:00 AM";
        if (hour < 12) return hour + ":00 AM";
        if (hour == 12) return "12:00 PM";
        return (hour - 12) + ":00 PM";
    }

    private void showEmptyState() {
        lineChart.clear();
        lineChart.setNoDataText("No study sessions recorded yet");
        summaryText.setText("Complete some study sessions to see your patterns");
        summaryText.setVisibility(VISIBLE);
    }


    public void setTitle(String title) {
        titleText.setText(title);
    }
}