package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.model.HourlyQuality;

/**
 * Custom view that displays the "Energy Curve" - a smooth line showing
 * study quality throughout the day
 */
public class EnergyCurveChartView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private Paint pointPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint titlePaint;

    private List<HourlyQuality> dataPoints;

    private float chartHeight;
    private float chartWidth;
    private float padding = 80;
    private float topPadding = 100;

    public EnergyCurveChartView(Context context) {
        super(context);
        init();
    }

    public EnergyCurveChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Line paint
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#4CAF50"));
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setShadowLayer(4f, 0, 2f, Color.parseColor("#40000000"));

        // Fill paint (gradient under curve)
        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        // Point paint
        pointPaint = new Paint();
        pointPaint.setColor(Color.parseColor("#2E7D32"));
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);
        pointPaint.setShadowLayer(4f, 0, 2f, Color.parseColor("#40000000"));

        // Grid paint
        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);

        // Text paint
        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#757575"));
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);

        // Title paint
        titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#212121"));
        titlePaint.setTextSize(48f);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);
    }

    public void setData(List<HourlyQuality> data) {
        this.dataPoints = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw title
        canvas.drawText("Energy Curve", padding, 60, titlePaint);

        if (dataPoints == null || dataPoints.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        chartWidth = getWidth() - (2 * padding);
        chartHeight = getHeight() - topPadding - padding;

        // Setup gradient for fill
        LinearGradient gradient = new LinearGradient(
                0, topPadding,
                0, topPadding + chartHeight,
                Color.parseColor("#804CAF50"),
                Color.parseColor("#004CAF50"),
                Shader.TileMode.CLAMP
        );
        fillPaint.setShader(gradient);

        drawGrid(canvas);
        drawAxes(canvas);
        drawFillArea(canvas);
        drawCurve(canvas);
        drawPoints(canvas);
    }

    private void drawGrid(Canvas canvas) {
        // Horizontal lines (quality levels)
        for (int i = 0; i <= 4; i++) {
            float y = topPadding + (chartHeight * i / 4);
            canvas.drawLine(padding, y, padding + chartWidth, y, gridPaint);
        }

        // Vertical lines (every 3 hours)
        for (int hour = 0; hour <= 24; hour += 3) {
            float x = padding + (chartWidth * hour / 23);
            canvas.drawLine(x, topPadding, x, topPadding + chartHeight, gridPaint);
        }
    }

    private void drawAxes(Canvas canvas) {
        // X-axis labels (hours)
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int hour = 0; hour <= 24; hour += 3) {
            float x = padding + (chartWidth * hour / 23);
            String label = String.format(Locale.getDefault(), "%02d:00", hour);
            canvas.drawText(label, x, getHeight() - 20, textPaint);
        }

        // Y-axis labels (quality %)
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 4; i++) {
            float y = topPadding + (chartHeight * i / 4) + 10;
            int quality = 100 - (i * 25);
            canvas.drawText(quality + "%", padding - 20, y, textPaint);
        }
    }

    private void drawFillArea(Canvas canvas) {
        Path fillPath = new Path();
        boolean firstPoint = true;

        for (HourlyQuality hq : dataPoints) {
            if (hq.getSessionCount() == 0) continue;

            float x = padding + (chartWidth * hq.getHour() / 23);
            float y = topPadding + (chartHeight * (100 - hq.getAvgQuality()) / 100);

            if (firstPoint) {
                fillPath.moveTo(x, y);
                firstPoint = false;
            } else {
                fillPath.lineTo(x, y);
            }
        }

        // Close the path to fill
        if (!firstPoint) {
            // Find last point's x
            HourlyQuality lastPoint = null;
            for (int i = dataPoints.size() - 1; i >= 0; i--) {
                if (dataPoints.get(i).getSessionCount() > 0) {
                    lastPoint = dataPoints.get(i);
                    break;
                }
            }

            if (lastPoint != null) {
                float lastX = padding + (chartWidth * lastPoint.getHour() / 23);
                fillPath.lineTo(lastX, topPadding + chartHeight);

                // Find first point's x
                HourlyQuality firstData = null;
                for (HourlyQuality hq : dataPoints) {
                    if (hq.getSessionCount() > 0) {
                        firstData = hq;
                        break;
                    }
                }

                if (firstData != null) {
                    float firstX = padding + (chartWidth * firstData.getHour() / 23);
                    fillPath.lineTo(firstX, topPadding + chartHeight);
                }
            }

            fillPath.close();
            canvas.drawPath(fillPath, fillPaint);
        }
    }

    private void drawCurve(Canvas canvas) {
        Path path = new Path();
        boolean firstPoint = true;

        for (HourlyQuality hq : dataPoints) {
            if (hq.getSessionCount() == 0) continue;

            float x = padding + (chartWidth * hq.getHour() / 23);
            float y = topPadding + (chartHeight * (100 - hq.getAvgQuality()) / 100);

            if (firstPoint) {
                path.moveTo(x, y);
                firstPoint = false;
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, linePaint);
    }

    private void drawPoints(Canvas canvas) {
        for (HourlyQuality hq : dataPoints) {
            if (hq.getSessionCount() == 0) continue;

            float x = padding + (chartWidth * hq.getHour() / 23);
            float y = topPadding + (chartHeight * (100 - hq.getAvgQuality()) / 100);

            // Draw outer circle
            canvas.drawCircle(x, y, 12f, pointPaint);

            // Draw inner circle (highlight)
            Paint innerPaint = new Paint(pointPaint);
            innerPaint.setColor(Color.parseColor("#81C784"));
            canvas.drawCircle(x, y, 6f, innerPaint);
        }
    }

    private void drawEmptyState(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(40f);
        String text = "No data available";
        canvas.drawText(text, getWidth() / 2f, getHeight() / 2f, textPaint);

        textPaint.setTextSize(32f);
        textPaint.setColor(Color.parseColor("#9E9E9E"));
        canvas.drawText("Complete study sessions to see your energy curve",
                getWidth() / 2f, getHeight() / 2f + 50, textPaint);
    }
}