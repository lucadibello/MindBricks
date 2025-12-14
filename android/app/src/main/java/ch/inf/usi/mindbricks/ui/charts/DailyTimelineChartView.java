package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.DailyRecommendation;

/**
 * Custom view that displays recommended study times for the day
 */
public class DailyTimelineChartView extends LinearLayout {

    private TextView titleText;
    private TextView summaryText;
    private TextView confidenceText;
    private LinearLayout slotsContainer;

    public DailyTimelineChartView(Context context) {
        super(context);
        init(context);
    }

    public DailyTimelineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DailyTimelineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_daily_timeline_chart, this, true);

        titleText = findViewById(R.id.dailyTimelineTitle);
        summaryText = findViewById(R.id.dailyTimelineSummary);
        confidenceText = findViewById(R.id.dailyTimelineConfidence);
        slotsContainer = findViewById(R.id.dailyTimelineSlotsContainer);
    }

    public void setData(AIRecommendation recommendation) {
        if (recommendation == null) {
            summaryText.setText("No recommendations available");
            summaryText.setTextColor(ContextCompat.getColor(getContext(), R.color.empty_state_text));
            slotsContainer.removeAllViews();
            confidenceText.setText("");
            return;
        }

        summaryText.setText(recommendation.getReasonSummary());
        summaryText.setTextColor(ContextCompat.getColor(getContext(), R.color.analytics_text_secondary));
        confidenceText.setText(String.format("Confidence: %d%%", recommendation.getConfidenceScore()));
        confidenceText.setTextColor(ContextCompat.getColor(getContext(), R.color.analytics_text_tertiary));

        // Clear previous slots
        slotsContainer.removeAllViews();

        // Add recommended time slots
        for (DailyRecommendation.TimeSlot slot : recommendation.getRecommendedSlots()) {
            addTimeSlotView(slot);
        }
    }

    private void addTimeSlotView(DailyRecommendation.TimeSlot slot) {
        LinearLayout slotView = (LinearLayout) LayoutInflater.from(getContext())
                .inflate(R.layout.item_time_slot, slotsContainer, false);

        TextView labelText = slotView.findViewById(R.id.slotLabel);
        TextView timeText = slotView.findViewById(R.id.slotTime);
        TextView scoreText = slotView.findViewById(R.id.slotScore);

        labelText.setText(slot.getLabel());
        timeText.setText(slot.getTimeRange());
        scoreText.setText(String.format("%.0f%% focus expected", slot.getExpectedFocusScore()));

        // Color code by score using theme colors
        int color;
        if (slot.getExpectedFocusScore() >= 80) {
            color = ContextCompat.getColor(getContext(), R.color.analytics_accent_green);
        } else if (slot.getExpectedFocusScore() >= 60) {
            color = ContextCompat.getColor(getContext(), R.color.analytics_accent_yellow);
        } else {
            color = ContextCompat.getColor(getContext(), R.color.analytics_accent_orange);
        }

        // Apply semi-transparent background
        slotView.setBackgroundColor(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)));

        slotsContainer.addView(slotView);
    }
}