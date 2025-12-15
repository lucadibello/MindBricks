package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
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

/**
 * Custom view that displays weekly study statistics as a bar chart.
 */
public class WeeklyFocusChartView extends LinearLayout {

    private TextView titleText;
    private TextView summaryText;
    private BarChart barChart;

    int colorHigh;
    int colorMed;
    int colorLow;
    int colorGray;
    int colorZero;

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
        summaryText = findViewById(R.id.weeklyFocusSummary);
        barChart = findViewById(R.id.weeklyFocusBarChart);

        // Configure the chart with default settings
        setupChart();

        colorHigh = ContextCompat.getColor(context, R.color.analytics_accent_purple);
        colorMed = ContextCompat.getColor(context, R.color.analytics_accent_blue);
        colorLow = ContextCompat.getColor(context, R.color.analytics_accent_green);
        colorZero = ContextCompat.getColor(context, R.color.analytics_accent_red);
        colorGray = ContextCompat.getColor(context, R.color.analytics_text_disabled);
    }

    /**
     * Configure chart appearance and behavior.
     * This is called once during initialization.
     */
    private void setupChart() {
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setLabelCount(7);

        // Set day labels
        xAxis.setValueFormatter(new IndexAxisValueFormatter(DAY_LABELS));

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(12f);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setGranularity(10f);

        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f min", value);
            }
        });

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    public void setData(WeeklyStats stats) {
        updateSummaryText(stats);

        List<BarEntry> entries = createBarEntries(stats);

        if (entries.isEmpty()) {
            showEmptyState();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Study Minutes");
        styleDataSet(dataSet, stats);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);
        barData.setValueTextSize(10f);
        barData.setValueTextColor(Color.DKGRAY);

        barData.setValueFormatter(new WeeklyBarValueFormatter(stats));

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.setExtraBottomOffset(20f);

        setupCustomLegend();

        barChart.animateY(800);
        barChart.invalidate();
    }

    private List<BarEntry> createBarEntries(WeeklyStats stats) {
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            float minutes = stats.getDayMinutes(i);
            entries.add(new BarEntry(i, minutes));
        }

        return entries;
    }

    private void setupCustomLegend() {
        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);
        legend.setTextColor(Color.DKGRAY);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(10f);
        legend.setXEntrySpace(10f);
        legend.setYEntrySpace(6f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);

        List<LegendEntry> entries = new ArrayList<>();

        LegendEntry high = new LegendEntry();
        high.label = "High Focus (≥80%)";
        high.formColor = colorHigh;
        high.form = Legend.LegendForm.SQUARE;
        entries.add(high);

        LegendEntry med = new LegendEntry();
        med.label = "Good Focus (≥60%)";
        med.formColor = colorMed;
        med.form = Legend.LegendForm.SQUARE;
        entries.add(med);

        LegendEntry low = new LegendEntry();
        low.label = "Medium Focus (≥40%)";
        low.formColor = colorLow;
        low.form = Legend.LegendForm.SQUARE;
        entries.add(low);

        LegendEntry veryLow = new LegendEntry();
        veryLow.label = "Low Focus (<40%)";
        veryLow.formColor = colorZero;
        veryLow.form = Legend.LegendForm.SQUARE;
        entries.add(veryLow);

        legend.setCustom(entries);
    }

    private void styleDataSet(BarDataSet dataSet, WeeklyStats stats) {
        List<Integer> colors = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            float focusScore = stats.getDayFocusScore(i);
            int color = getColorForFocusScore(focusScore);
            colors.add(color);
        }

        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.DKGRAY);

        dataSet.setDrawValues(true);
    }

    private int getColorForFocusScore(float focusScore) {
        if (focusScore >= 80) {
            return colorHigh;
        } else if (focusScore >= 60) {
            return colorMed;
        } else if (focusScore >= 40) {
            return colorLow;
        } else if(focusScore >= 0) {
            return colorZero;
        }
        else return colorGray;
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

    private class WeeklyBarValueFormatter extends ValueFormatter {
        private final WeeklyStats stats;

        public WeeklyBarValueFormatter(WeeklyStats stats) {
            this.stats = stats;
        }

        @Override
        public String getFormattedValue(float value) {
            if (value == 0) return "";

            int dayIndex = -1;
            for (int i = 0; i < 7; i++) {
                if (Math.abs(stats.getDayMinutes(i) - value) < 0.1f) {
                    dayIndex = i;
                    break;
                }
            }

            if (dayIndex == -1) return String.format(Locale.getDefault(), "%.0f", value);

            float focusScore = stats.getDayFocusScore(dayIndex);
            return String.format(Locale.getDefault(),
                    "%.0fm\n",
                    value);
        }
    }
}