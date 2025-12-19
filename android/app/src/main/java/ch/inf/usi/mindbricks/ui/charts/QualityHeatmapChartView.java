package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.HeatmapCell;

/**
 * Custom view to display a quality heatmap chart.
 *
 * @author Marta Šafářová
 */
public class QualityHeatmapChartView extends View {


    /**
     * Paint for heatmap cells.
     */
    private Paint cellPaint;

    /**
     * Paint for cell borders.
     */
    private Paint borderPaint;

    /**
     * Paint for text labels.
     */
    private Paint textPaint;

    /**
     * List of heatmap cells to display.
     */
    private List<HeatmapCell> cells;

    /**
     * Cell dimensions and spacing.
     */
    private final float cellSize = 40f;

    /**
     * Spacing between cells.
     */
    private final float cellSpacing = 4f;

    /**
     * Padding around the heatmap.
     */
    private final float padding = 80f;

    /**
     * Top padding for hour labels.
     */
    private final float topPadding = 100f;

    /**
     * Default constructor.
     *
     * @param context The context
     */
    public QualityHeatmapChartView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor with attributes.
     *
     * @param context The context
     * @param attrs   The attribute set
     */
    public QualityHeatmapChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (cells == null || cells.isEmpty()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int minDay = getMinDay();
        int maxDay = getMaxDay();
        int dayCount = maxDay - minDay + 1;

        int requiredWidth = (int) (padding * 2 + (dayCount * (cellSize + cellSpacing)));

        int requiredHeight = (int) (topPadding + (24 * (cellSize + cellSpacing)));

        setMeasuredDimension(requiredWidth, requiredHeight);
    }

    /**
     * Initialize paints and colors.
     */
    private void init() {
        Context context = getContext();
        // Theme colors
        int colorGrid = ContextCompat.getColor(context, R.color.analytics_grid_line_major);
        int colorTextPrimary = ContextCompat.getColor(context, R.color.analytics_text_primary);
        int colorTextSecondary = ContextCompat.getColor(context, R.color.analytics_text_secondary);

        cellPaint = new Paint();
        cellPaint.setStyle(Paint.Style.FILL);
        cellPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(colorGrid);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1f);
        borderPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(colorTextSecondary);
        textPaint.setTextSize(28f);
        textPaint.setAntiAlias(true);

        Paint titlePaint = new Paint();
        titlePaint.setColor(colorTextPrimary);
        titlePaint.setTextSize(48f);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);
    }

    /**
     * Set the data for the heatmap.
     *
     * @param cells List of HeatmapCell data
     */
    public void setData(List<HeatmapCell> cells) {
        this.cells = cells;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (cells == null || cells.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        drawHourLabels(canvas);
        drawDayLabels(canvas);
        drawHeatmap(canvas);
    }

    /**
     * Draw the hour labels on the left side of the heatmap.
     *
     * @param canvas The canvas to draw on
     */
    private void drawHourLabels(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.RIGHT);

        for (int hour = 0; hour < 24; hour += 2) {
            float y = topPadding + (hour * (cellSize + cellSpacing)) + cellSize / 2 + 10;
            String label = String.format(Locale.getDefault(), "%02d:00", hour);
            canvas.drawText(label, padding - 10, y, textPaint);
        }
    }

    /**
     * Draw the day labels at the top of the heatmap.
     *
     * @param canvas The canvas to draw on
     */
    private void drawDayLabels(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Get unique days from cells
        int minDay = Integer.MAX_VALUE;
        int maxDay = Integer.MIN_VALUE;

        for (HeatmapCell cell : cells) {
            if (cell.getDayOfMonth() < minDay) minDay = cell.getDayOfMonth();
            if (cell.getDayOfMonth() > maxDay) maxDay = cell.getDayOfMonth();
        }

        int dayCount = maxDay - minDay + 1;

        for (int i = 0; i < dayCount; i++) {
            int day = minDay + i;
            float x = padding + (i * (cellSize + cellSpacing)) + cellSize / 2;
            canvas.drawText(String.valueOf(day), x, topPadding - 20, textPaint);
        }
    }

    /**
     * Draw the heatmap cells on the canvas.
     *
     * @param canvas The canvas to draw on
     */
    private void drawHeatmap(Canvas canvas) {
        int minDay = getMinDay();
        int maxDay = getMaxDay();

        // Draw all hours (0-23) for each day in range
        for (int day = minDay; day <= maxDay; day++) {
            for (int hour = 0; hour < 24; hour++) {
                // Find cell data for this day/hour combination
                HeatmapCell cellData = findCell(day, hour);

                int dayIndex = day - minDay;

                float x = padding + (dayIndex * (cellSize + cellSpacing));
                float y = topPadding + (hour * (cellSize + cellSpacing));

                int color;
                // If no data or no sessions, draw empty cell
                if (cellData == null || cellData.getSessionCount() == 0) {
                    cellPaint.setAlpha(10);
                    borderPaint.setAlpha(10);
                    color = ContextCompat.getColor(getContext(), R.color.analytics_grid_line_major);
                } else {
                    cellPaint.setAlpha(255);
                    borderPaint.setAlpha(255);
                    color = getColorForQuality(cellData.getAvgQuality());
                }

                cellPaint.setColor(color);

                // Draw cell
                RectF rect = new RectF(x, y, x + cellSize, y + cellSize);
                canvas.drawRoundRect(rect, 8f, 8f, cellPaint);
                canvas.drawRoundRect(rect, 8f, 8f, borderPaint);
            }
        }
    }

    /**
     * Find cell data for a specific day and hour combination.
     *
     * @param dayOfMonth The day to search for
     * @param hourOfDay The hour to search for
     * @return The matching HeatmapCell or null if not found
     */
    private HeatmapCell findCell(int dayOfMonth, int hourOfDay) {
        if (cells == null) return null;

        for (HeatmapCell cell : cells) {
            if (cell.getDayOfMonth() == dayOfMonth && cell.getHourOfDay() == hourOfDay) {
                return cell;
            }
        }
        return null;
    }

    /**
     * Get color based on quality score.
     *
     * @param quality The quality score (0-100)
     * @return The corresponding color
     */
    private int getColorForQuality(float quality) {
        Context context = getContext();

        if (quality >= 85) {
            return ContextCompat.getColor(context, R.color.chart_scale_7);
        }
        else if (quality >= 70) {
            return ContextCompat.getColor(context, R.color.chart_scale_5);
        }
        else if (quality >= 50) {
            return ContextCompat.getColor(context, R.color.chart_scale_3);
        }
        else if (quality >= 30) {
            return ContextCompat.getColor(context, R.color.chart_scale_2);
        }
        else {
            return ContextCompat.getColor(context, R.color.chart_scale_0);
        }
    }

    /**
     * Get the minimum day from the cells.
     *
     * @return The minimum day of month
     */
    private int getMinDay() {
        if (cells == null || cells.isEmpty()) return 1;

        int min = Integer.MAX_VALUE;
        for (HeatmapCell cell : cells) {
            if (cell.getDayOfMonth() < min) {
                min = cell.getDayOfMonth();
            }
        }
        return min;
    }

    /**
     * Get the maximum day from the cells.
     *
     * @return The maximum day of month
     */
    private int getMaxDay() {
        if (cells == null || cells.isEmpty()) return 1;

        int max = Integer.MIN_VALUE;
        for (HeatmapCell cell : cells) {
            if (cell.getDayOfMonth() > max) {
                max = cell.getDayOfMonth();
            }
        }
        return max;
    }

    /**
     * Draw empty state message when no data is available.
     *
     * @param canvas Canvas to draw on
     */
    private void drawEmptyState(Canvas canvas) {
        Paint emptyPaint = new Paint(textPaint);
        emptyPaint.setColor(ContextCompat.getColor(getContext(), R.color.empty_state_text));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setTextSize(40f);

        String text = "No heatmap data";
        canvas.drawText(text, getWidth() / 2f, getHeight() / 2f, emptyPaint);
    }
}