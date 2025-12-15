package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

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

/**
 * Custom view that displays hourly study distribution.
 *
 */
public class HourlyDistributionChartView extends LinearLayout {

    private TextView titleText;
    private TextView summaryText;
    private LineChart studyMinutesChart;
    private LineChart focusChart;

    int studyColor;
    int focusColor;
    int gridColor;

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

        summaryText = findViewById(R.id.hourlyDistributionSummary);
        studyMinutesChart = findViewById(R.id.studyMinutesChart);
        focusChart = findViewById(R.id.focusChart);

        studyColor = ContextCompat.getColor(getContext(), R.color.analytics_accent_purple);
        focusColor = ContextCompat.getColor(getContext(), R.color.analytics_accent_green);
        gridColor = ContextCompat.getColor(getContext(), R.color.analytics_grid_line);

        setupStudyMinutesChart();
        setupFocusChart();
    }

    private void setupFocusChart() {
        // Basic settings
        focusChart.setDrawGridBackground(false);
        focusChart.getDescription().setEnabled(false);
        focusChart.setPinchZoom(true);
        focusChart.setScaleEnabled(true);
        focusChart.setTouchEnabled(true);

        // Configure legend
        Legend legend = focusChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(11f);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        XAxis xAxis = focusChart.getXAxis();
        xAxis.setDrawGridLines(true);
        xAxis.setGridLineWidth(0.5f);
        xAxis.setGridColor(gridColor);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setLabelRotationAngle(-45f);

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

        YAxis leftAxis = focusChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setGridColor(gridColor);
        leftAxis.setTextSize(11f);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setGranularity(20f);

        YAxis rightAxis = focusChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void setupStudyMinutesChart() {
        // Basic settings
        studyMinutesChart.setDrawGridBackground(false);
        studyMinutesChart.getDescription().setEnabled(false);
        studyMinutesChart.setPinchZoom(true);
        studyMinutesChart.setScaleEnabled(true);
        studyMinutesChart.setTouchEnabled(true);

        // Configure legend
        Legend legend = studyMinutesChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(11f);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        XAxis xAxis = studyMinutesChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setDrawGridLines(true);
        xAxis.setGridLineWidth(0.5f);
        xAxis.setGridColor(gridColor);


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

        YAxis leftAxis = studyMinutesChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setGridColor(gridColor);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(11f);
        leftAxis.setGranularity(10f);
        leftAxis.setTextColor(Color.DKGRAY);

        YAxis rightAxis = studyMinutesChart.getAxisRight();
        rightAxis.setEnabled(false);
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

        setStudyMinutesData(activeHours);
        setFocusScoreData(activeHours);
        updateSummary(activeHours);
    }

    private void setStudyMinutesData(List<TimeSlotStats> hours) {
        LineDataSet dataSet = createMinutesDataSet(hours);
        styleMinutesDataSet(dataSet);

        LineData lineData = new LineData(dataSet);
        studyMinutesChart.setData(lineData);
        studyMinutesChart.animateX(800);
        studyMinutesChart.invalidate();
    }

    private void setFocusScoreData(List<TimeSlotStats> hours) {
        LineDataSet dataSet = createFocusDataSet(hours);
        styleFocusDataSet(dataSet);

        LineData lineData = new LineData(dataSet);
        focusChart.setData(lineData);
        focusChart.animateX(800);
        focusChart.invalidate();
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

    private void styleMinutesDataSet(LineDataSet dataSet){
        dataSet.setColor(studyColor);
        dataSet.setCircleColor(studyColor);
        dataSet.setFillColor(studyColor);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(30);
    }

    private void styleFocusDataSet(LineDataSet dataSet){
        dataSet.setColor(focusColor);
        dataSet.setCircleColor(focusColor);
        dataSet.setFillColor(focusColor);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(30);
    }

    private LineDataSet createMinutesDataSet(List<TimeSlotStats> hours) {
        List<Entry> entries = new ArrayList<>();

        for (TimeSlotStats stats : hours) {
            int hour = stats.getHourOfDay();
            float minutes = stats.getTotalMinutes();
            entries.add(new Entry(hour, minutes));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Study Minutes");

        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return String.format(Locale.getDefault(), "%.0fm", value);
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

        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        dataSet.enableDashedLine(10f, 5f, 0f);

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
        studyMinutesChart.clear();
        studyMinutesChart.setNoDataText("No study sessions recorded yet");

        focusChart.clear();
        focusChart.setNoDataText("No study sessions recorded yet");

        summaryText.setText("Complete some study sessions to see your patterns");
        summaryText.setVisibility(VISIBLE);
    }

    public void setTitle(String title) {
        titleText.setText(title);
    }
}