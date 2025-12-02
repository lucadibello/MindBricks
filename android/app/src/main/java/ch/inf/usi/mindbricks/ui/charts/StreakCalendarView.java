package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.model.StreakDay;

/**
 * Monthly calendar showing study streaks
 * Green = hit target,
 * Orange = partial,
 * Red = no study,
 * Blue = exceptional
 */
public class StreakCalendarView extends View {

    private Paint cellPaint;
    private Paint borderPaint;
    private Paint textPaint;
    private Paint titlePaint;
    private Paint streakTextPaint;

    private List<StreakDay> streakDays;
    private int currentStreak = 0;

    private float cellSize = 70f;
    private float cellSpacing = 8f;
    private float padding = 60f;
    private float topPadding = 150f;

    private String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    public StreakCalendarView(Context context) {
        super(context);
        init();
    }

    public StreakCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cellPaint = new Paint();
        cellPaint.setStyle(Paint.Style.FILL);
        cellPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#E0E0E0"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#212121"));
        titlePaint.setTextSize(48f);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);

        streakTextPaint = new Paint();
        streakTextPaint.setColor(Color.parseColor("#FF6F00"));
        streakTextPaint.setTextSize(40f);
        streakTextPaint.setAntiAlias(true);
        streakTextPaint.setFakeBoldText(true);
    }

    public void setData(List<StreakDay> streakDays, int currentStreak) {
        this.streakDays = streakDays;
        this.currentStreak = currentStreak;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw title
        titlePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Study Streak Calendar", padding, 60, titlePaint);

        // Draw current streak
        if (currentStreak > 0) {
            streakTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("🔥 " + currentStreak + " day streak",
                    getWidth() - padding, 60, streakTextPaint);
        }

        if (streakDays == null || streakDays.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        drawMonthName(canvas);
        drawDayHeaders(canvas);
        drawCalendar(canvas);
        drawLegend(canvas);
    }

    private void drawMonthName(Canvas canvas) {
        if (streakDays.isEmpty()) return;

        StreakDay firstDay = streakDays.get(0);
        Calendar cal = Calendar.getInstance();
        cal.set(firstDay.getYear(), firstDay.getMonth(), 1);

        String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());

        Paint monthPaint = new Paint(titlePaint);
        monthPaint.setTextSize(36f);
        monthPaint.setTextAlign(Paint.Align.CENTER);
        monthPaint.setColor(Color.parseColor("#424242"));

        canvas.drawText(monthName + " " + firstDay.getYear(),
                getWidth() / 2f, topPadding - 40, monthPaint);
    }

    private void drawDayHeaders(Canvas canvas) {
        Paint headerPaint = new Paint(textPaint);
        headerPaint.setColor(Color.parseColor("#757575"));
        headerPaint.setTextSize(28f);

        for (int i = 0; i < 7; i++) {
            float x = padding + (i * (cellSize + cellSpacing)) + cellSize / 2;
            canvas.drawText(dayNames[i], x, topPadding - 10, headerPaint);
        }
    }

    private void drawCalendar(Canvas canvas) {
        if (streakDays.isEmpty()) return;

        // Get first day of month
        StreakDay firstDay = streakDays.get(0);
        Calendar cal = Calendar.getInstance();
        cal.set(firstDay.getYear(), firstDay.getMonth(), 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday

        for (StreakDay day : streakDays) {
            int dayOfMonth = day.getDayOfMonth();
            int position = dayOfMonth - 1 + firstDayOfWeek;

            int row = position / 7;
            int col = position % 7;

            float x = padding + (col * (cellSize + cellSpacing));
            float y = topPadding + (row * (cellSize + cellSpacing));

            // Get color based on status
            int color = getColorForStatus(day.getStatus());
            cellPaint.setColor(color);

            // Draw cell
            RectF rect = new RectF(x, y, x + cellSize, y + cellSize);
            canvas.drawRoundRect(rect, 12f, 12f, cellPaint);
            canvas.drawRoundRect(rect, 12f, 12f, borderPaint);

            // Draw day number
            Paint dayTextPaint = new Paint(textPaint);

            // Adjust text color for better contrast
            if (day.getStatus() == StreakDay.StreakStatus.NONE) {
                dayTextPaint.setColor(Color.parseColor("#757575"));
            } else {
                dayTextPaint.setColor(Color.WHITE);
            }

            canvas.drawText(String.valueOf(dayOfMonth),
                    x + cellSize / 2,
                    y + cellSize / 2 + 12,
                    dayTextPaint);

            // Draw small indicator for exceptional days
            if (day.getStatus() == StreakDay.StreakStatus.EXCEPTIONAL) {
                Paint starPaint = new Paint();
                starPaint.setColor(Color.WHITE);
                starPaint.setTextSize(20f);
                starPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("⭐", x + cellSize - 12, y + 20, starPaint);
            }
        }
    }

    private void drawLegend(Canvas canvas) {
        float legendY = getHeight() - 80;
        float legendX = padding;

        Paint legendTextPaint = new Paint(textPaint);
        legendTextPaint.setColor(Color.parseColor("#757575"));
        legendTextPaint.setTextSize(24f);
        legendTextPaint.setTextAlign(Paint.Align.LEFT);

        canvas.drawText("Legend: ", legendX, legendY + 20, legendTextPaint);
        legendX += 120;

        // Draw legend items
        String[] labels = {"None", "Partial", "Target", "Exceptional"};
        StreakDay.StreakStatus[] statuses = {
                StreakDay.StreakStatus.NONE,
                StreakDay.StreakStatus.PARTIAL,
                StreakDay.StreakStatus.HIT_TARGET,
                StreakDay.StreakStatus.EXCEPTIONAL
        };

        for (int i = 0; i < labels.length; i++) {
            int color = getColorForStatus(statuses[i]);
            cellPaint.setColor(color);

            RectF rect = new RectF(legendX, legendY, legendX + 35, legendY + 35);
            canvas.drawRoundRect(rect, 6f, 6f, cellPaint);
            canvas.drawRoundRect(rect, 6f, 6f, borderPaint);

            canvas.drawText(labels[i], legendX + 45, legendY + 24, legendTextPaint);

            legendX += 160;
        }
    }

    private int getColorForStatus(StreakDay.StreakStatus status) {
        switch (status) {
            case EXCEPTIONAL:
                return Color.parseColor("#1565C0"); // Blue
            case HIT_TARGET:
                return Color.parseColor("#2E7D32"); // Green
            case PARTIAL:
                return Color.parseColor("#F57C00"); // Orange
            case NONE:
            default:
                return Color.parseColor("#EEEEEE"); // Light gray
        }
    }

    private void drawEmptyState(Canvas canvas) {
        Paint emptyPaint = new Paint(textPaint);
        emptyPaint.setColor(Color.parseColor("#757575"));
        emptyPaint.setTextSize(36f);
        emptyPaint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText("No calendar data available",
                getWidth() / 2f, getHeight() / 2f, emptyPaint);
    }
}