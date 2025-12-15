package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.GoalRing;

/**
 * Apple Activity Rings style visualization for daily goals
 * Updated to clearly display daily targets in legend
 */
public class GoalRingsView extends View {

    private Paint ringPaint;
    private Paint backgroundPaint;
    private Paint textPaint;
    private Paint titlePaint;
    private Paint labelPaint;

    private List<GoalRing> rings;

    private float centerX;
    private float centerY;
    private float ringStrokeWidth = 35f;
    private float ringSpacing = 15f;

    // Theme colors
    private int colorBackground;
    private int colorTextPrimary;
    private int colorTextSecondary;

    boolean isCompactView = false;

    private int colorOut;
    private int colorMid;
    private int colorIn;

    public GoalRingsView(Context context) {
        super(context);
        init();
    }

    public GoalRingsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        Context context = getContext();
        colorBackground = ContextCompat.getColor(context, R.color.goal_ring_background);
        colorTextPrimary = ContextCompat.getColor(context, R.color.analytics_text_primary);
        colorTextSecondary = ContextCompat.getColor(context, R.color.analytics_text_secondary);
        colorOut = ContextCompat.getColor(context, R.color.goal_ring_focus);
        colorMid = ContextCompat.getColor(context, R.color.goal_ring_quality);
        colorIn = ContextCompat.getColor(context, R.color.goal_ring_time);

        ringPaint = new Paint();
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(ringStrokeWidth);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setAntiAlias(true);

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(ringStrokeWidth);
        backgroundPaint.setColor(colorBackground);
        backgroundPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(colorTextPrimary);
        textPaint.setTextSize(45f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        titlePaint = new Paint();
        titlePaint.setColor(colorTextSecondary);
        titlePaint.setTextSize(40f);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);

        labelPaint = new Paint();
        labelPaint.setColor(colorTextSecondary);
        labelPaint.setTextSize(35f);
        labelPaint.setAntiAlias(true);
        labelPaint.setTextAlign(Paint.Align.LEFT);

        isCompactView = false;
    }

    public void setData(List<GoalRing> rings, boolean compact) {
        this.rings = rings;
        this.isCompactView = compact;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rings == null || rings.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        centerX = getWidth() / 2f;
        centerY = (getHeight() / 2f);

        if (!isCompactView) {
            drawFull(canvas);
        } else {
            drawCompact(canvas);
        }
    }

    private void drawFull(Canvas canvas) {
        drawRings(canvas, 250f, 35f, 15f);
        drawCenterText(canvas, 72f);
        drawLabels(canvas);
    }

    private void drawCompact(Canvas canvas) {
        drawRings(canvas, 125f, 17.5f, 7.5f);
        drawCenterText(canvas, 36f);
    }

    private void drawRings(Canvas canvas, float baseRadius, float strokeWidth, float spacing) {
        float originalStrokeWidth = ringPaint.getStrokeWidth();
        ringPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setStrokeWidth(strokeWidth);

        float offset = isCompactView ? 20 : 180f;

        for (int i = 0; i < rings.size(); i++) {
            GoalRing ring = rings.get(i);
            float radius = baseRadius - (i * (strokeWidth + spacing));

            // Draw background ring
            RectF bgRect = new RectF(
                    centerX - radius, centerY - radius - offset,
                    centerX + radius, centerY + radius - offset
            );
            canvas.drawArc(bgRect, -90, 360, false, backgroundPaint);

            // Draw progress ring
            float sweepAngle = (ring.getProgress() / 100f) * 360f;

            // Set color based on ring position
            switch (i) {
                case 0:
                    ring.setColor(colorIn);
                    ringPaint.setColor(colorIn);
                    break;
                case 1:
                    ring.setColor(colorMid);
                    ringPaint.setColor(colorMid);
                    break;
                case 2:
                    ring.setColor(colorOut);
                    ringPaint.setColor(colorOut);
                    break;
                default:
                    ringPaint.setColor(colorOut);
                    break;
            }

            RectF rect = new RectF(
                    centerX - radius, centerY - radius - offset,
                    centerX + radius, centerY + radius - offset
            );
            canvas.drawArc(rect, -90, sweepAngle, false, ringPaint);
        }

        ringPaint.setStrokeWidth(originalStrokeWidth);
        backgroundPaint.setStrokeWidth(originalStrokeWidth);
    }

    private void drawCenterText(Canvas canvas, float textSize) {
        if (rings.isEmpty()) return;

        float offset = isCompactView ? 10 : 180;

        // Calculate overall completion
        float totalProgress = 0;
        for (GoalRing ring : rings) {
            totalProgress += ring.getProgress();
        }
        float avgProgress = totalProgress / rings.size();

        // Draw percentage
        String percentText = String.format(Locale.getDefault(), "%.0f%%", avgProgress);
        textPaint.setTextSize(textSize);
        textPaint.setColor(colorTextPrimary);
        canvas.drawText(percentText, centerX, centerY - offset, textPaint);

        if (!isCompactView) {
            Paint subtitlePaint = new Paint(textPaint);
            subtitlePaint.setTextSize(textSize / 2);
            subtitlePaint.setColor(colorTextSecondary);
            canvas.drawText("Complete", centerX, centerY - 130, subtitlePaint);
        }
    }

    private void drawLabels(Canvas canvas) {
        if (isCompactView)
            return;

        float startY = centerY + 150f;
        float lineHeight = 110f;

        for (int i = 0; i < rings.size(); i++) {
            GoalRing ring = rings.get(i);
            float y = startY + (i * lineHeight);

            // Draw color indicator
            Paint colorPaint = new Paint();
            colorPaint.setColor(ring.getColor());
            colorPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(80, y - 5, 15, colorPaint);

            // Draw label (ring title)
            String label = ring.getTitle();
            labelPaint.setTextAlign(Paint.Align.LEFT);
            labelPaint.setTextSize(32f);
            labelPaint.setColor(colorTextPrimary);
            canvas.drawText(label, 120, y - 10, labelPaint);

            // Draw current progress (bold, primary color)
            String currentText = String.format(Locale.getDefault(),
                    "%.0f %s", ring.getCurrent(), ring.getUnit());
            labelPaint.setTextSize(32f);
            labelPaint.setColor(colorTextPrimary);
            labelPaint.setFakeBoldText(true);
            canvas.drawText(currentText, 120, y + 25, labelPaint);
            labelPaint.setFakeBoldText(false);

            // Draw target (goal) below current in lighter color
            String targetText = String.format(Locale.getDefault(),
                    "Goal: %.0f %s", ring.getTarget(), ring.getUnit());
            labelPaint.setTextSize(30f);
            labelPaint.setColor(colorTextSecondary);
            canvas.drawText(targetText, 120, y + 55, labelPaint);

            // Draw achievement icon or progress percentage on right side
            if (ring.isAchieved()) {
                Paint iconPaint = new Paint();
                iconPaint.setColor(ring.getColor());
                iconPaint.setTextSize(40f);
                iconPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("✓", getWidth() - 80, y + 10, iconPaint);
            } else {
                // Draw progress percentage
                Paint percentPaint = new Paint();
                percentPaint.setColor(colorTextSecondary);
                percentPaint.setTextSize(40f);
                percentPaint.setTextAlign(Paint.Align.RIGHT);
                String percentText = String.format(Locale.getDefault(), "%.0f%%", ring.getProgress());
                canvas.drawText(percentText, getWidth() - 80, y + 10, percentPaint);
            }
        }
    }

    private void drawEmptyState(Canvas canvas) {
        Paint emptyPaint = new Paint(textPaint);
        emptyPaint.setColor(ContextCompat.getColor(getContext(), R.color.empty_state_text));
        emptyPaint.setTextSize(36f);

        canvas.drawText("No goals to display", getWidth() / 2f, getHeight() / 2f, emptyPaint);
    }
}