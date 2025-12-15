package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.TagUsage;

public class TagUsageChartView extends LinearLayout {

    private TextView summaryText;
    private PieChart pieChart;

    public TagUsageChartView(Context context) {
        super(context);
        init(context);
    }

    public TagUsageChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TagUsageChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_tag_usage_chart, this, true);

        summaryText = findViewById(R.id.tagUsageSummary);
        pieChart = findViewById(R.id.tagUsagePieChart);

        setupChart();
    }

    private void setupChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setDrawCenterText(true);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.setEntryLabelTextSize(0f);
        pieChart.setDrawEntryLabels(false);

        // Extra offsets to maximize chart size
        pieChart.setExtraOffsets(0f, 0f, 0f, 0f);
        pieChart.setMinOffset(10f);

        // Hole color matches background
        pieChart.setHoleColor(ContextCompat.getColor(getContext(), R.color.analytics_chart_background));
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(50);

        // Legend configuration - positioned below chart with percentages
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
        legend.setTextSize(11f);
        legend.setTextColor(ContextCompat.getColor(getContext(), R.color.analytics_text_secondary));
        legend.setXEntrySpace(8f);
        legend.setYEntrySpace(4f);
        legend.setFormSize(10f);
    }

    public void setData(List<TagUsage> tagData) {
        if (tagData == null || tagData.isEmpty()) {
            showEmptyState();
            return;
        }

        updateSummaryText(tagData);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (TagUsage data : tagData) {
            // Include percentage in the label for legend
            String label = String.format(Locale.getDefault(), "%s (%.1f%%)",
                    data.getTagTitle(), data.getPercentage());

            entries.add(new PieEntry(
                    data.getPercentage(),
                    label,
                    data.getSessionCount()
            ));
            colors.add(data.getTagColor());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(8f);
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setDrawValues(true);

        // Custom formatter: show percentage only if > 5%
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value > 5f) {
                    return String.format(Locale.getDefault(), "%.1f%%", value);
                }
                return "";
            }
        });

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        // Center text shows total
        int totalSessions = 0;
        for (TagUsage data : tagData) {
            totalSessions += data.getSessionCount();
        }
        pieChart.setCenterText(String.format(Locale.getDefault(),
                "%d\nSessions", totalSessions));
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(ContextCompat.getColor(getContext(), R.color.analytics_text_primary));

        pieChart.invalidate();
    }

    private void updateSummaryText(List<TagUsage> tagData) {
        if (tagData.isEmpty()) {
            summaryText.setText("No tag data available");
            return;
        }

        TagUsage topTag = tagData.get(0);
        summaryText.setText(String.format(Locale.getDefault(),
                "Most used: %s (%d sessions, %.1f%%)",
                topTag.getTagTitle(),
                topTag.getSessionCount(),
                topTag.getPercentage()
        ));
    }

    private void showEmptyState() {
        summaryText.setText("No tag data in this period");
        pieChart.clear();
        pieChart.setCenterText("No Data");
        pieChart.invalidate();
    }
}