package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.model.GoalRing;

/**
 * Apple Activity Rings style visualization for daily goals
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

    public GoalRingsView(Context context) {
        super(context);
        init();
    }

    public GoalRingsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ringPaint = new Paint();
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(ringStrokeWidth);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setAntiAlias(true);

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(ringStrokeWidth);
        backgroundPaint.setColor(Color.parseColor("#E0E0E0"));
        backgroundPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#212121"));
        textPaint.setTextSize(48f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#212121"));
        titlePaint.setTextSize(48f);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);

        labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#757575"));
        labelPaint.setTextSize(28f);
        labelPaint.setAntiAlias(true);
        labelPaint.setTextAlign(Paint.Align.LEFT);
    }

    public void setData(List<GoalRing> rings) {
        this.rings = rings;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw title
        titlePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Today's Goals", 60, 60, titlePaint);

        if (rings == null || rings.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        centerX = getWidth() / 2f;
        centerY = (getHeight() / 2f) + 50;

        drawRings(canvas);
        drawCenterText(canvas);
        drawLabels(canvas);
    }

    private void drawRings(Canvas canvas) {
        float baseRadius = 180f;

        for (int i = 0; i < rings.size(); i++) {
            GoalRing ring = rings.get(i);
            float radius = baseRadius - (i * (ringStrokeWidth + ringSpacing));

            // Draw background ring
            RectF bgRect = new RectF(
                    centerX - radius, centerY - radius,
                    centerX + radius, centerY + radius
            );
            canvas.drawArc(bgRect, -90, 360, false, backgroundPaint);

            // Draw progress ring
            float sweepAngle = (ring.getProgress() / 100f) * 360f;
            ringPaint.setColor(ring.getColor());

            RectF rect = new RectF(
                    centerX - radius, centerY - radius,
                    centerX + radius, centerY + radius
            );
            canvas.drawArc(rect, -90, sweepAngle, false, ringPaint);
        }
    }

    private void drawCenterText(Canvas canvas) {
        if (rings.isEmpty()) return;

        // Calculate overall completion
        float totalProgress = 0;
        for (GoalRing ring : rings) {
            totalProgress += ring.getProgress();
        }
        float avgProgress = totalProgress / rings.size();

        // Draw percentage
        String percentText = String.format(Locale.getDefault(), "%.0f%%", avgProgress);
        textPaint.setTextSize(72f);
        canvas.drawText(percentText, centerX, centerY + 20, textPaint);

        // Draw subtitle
        Paint subtitlePaint = new Paint(textPaint);
        subtitlePaint.setTextSize(28f);
        subtitlePaint.setColor(Color.parseColor("#757575"));
        canvas.drawText("Complete", centerX, centerY + 60, subtitlePaint);
    }

    private void drawLabels(Canvas canvas) {
        float startY = centerY + 250;
        float lineHeight = 70f;

        for (int i = 0; i < rings.size(); i++) {
            GoalRing ring = rings.get(i);
            float y = startY + (i * lineHeight);

            // Draw color indicator
            Paint colorPaint = new Paint();
            colorPaint.setColor(ring.getColor());
            colorPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(80, y - 20, 15, colorPaint);

            // Draw label
            String label = ring.getTitle();
            labelPaint.setTextAlign(Paint.Align.LEFT);
            labelPaint.setTextSize(32f);
            labelPaint.setColor(Color.parseColor("#212121"));
            canvas.drawText(label, 120, y - 10, labelPaint);

            // Draw progress text
            String progressText = String.format(Locale.getDefault(),
                    "%.0f / %.0f %s", ring.getCurrent(), ring.getTarget(), ring.getUnit());
            labelPaint.setTextSize(28f);
            labelPaint.setColor(Color.parseColor("#757575"));
            canvas.drawText(progressText, 120, y + 25, labelPaint);

            // Draw achievement icon
            if (ring.isAchieved()) {
                Paint iconPaint = new Paint();
                iconPaint.setColor(ring.getColor());
                iconPaint.setTextSize(36f);
                iconPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("✓", getWidth() - 80, y, iconPaint);
            }
        }
    }

    private void drawEmptyState(Canvas canvas) {
        Paint emptyPaint = new Paint(textPaint);
        emptyPaint.setColor(Color.parseColor("#757575"));
        emptyPaint.setTextSize(36f);

        canvas.drawText("No goals to display", centerX, getHeight() / 2f, emptyPaint);
    }
}