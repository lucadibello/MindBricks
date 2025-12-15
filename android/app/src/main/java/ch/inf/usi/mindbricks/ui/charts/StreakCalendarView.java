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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.StreakDay;

/**
 * Monthly calendar showing study streaks
 */
public class StreakCalendarView extends View {
    private OnMonthChangeListener monthChangeListener;
    private Paint cellPaint;
    private Paint borderPaint;
    private Paint textPaint;
    private Paint titlePaint;
    private Paint streakTextPaint;

    private List<StreakDay> streakDays;
    private int currentStreak = 0;

    private float cellSize = 110f;
    private float cellSpacing = 10f;
    private float padding = 50f;
    private float topPadding = 210f;

    private String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private int currentMonth;
    private int currentYear;

    private OnDayClickListener dayClickListener;
    private RectF leftArrowRect;
    private RectF rightArrowRect;

    private List<CalendarCell> calendarCells = new ArrayList<>();

    // Theme colors
    private int colorGrid;
    private int colorTextPrimary;
    private int colorTextSecondary;
    private int colorPrimary;
    private int colorStreakFire;

    public StreakCalendarView(Context context) {
        super(context);
        init();
    }

    public StreakCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        Context context = getContext();
        colorGrid = ContextCompat.getColor(context, R.color.analytics_grid_line);
        colorTextPrimary = ContextCompat.getColor(context, R.color.analytics_text_primary);
        colorTextSecondary = ContextCompat.getColor(context, R.color.analytics_text_secondary);
        colorPrimary = ContextCompat.getColor(context, R.color.analytics_accent_purple);
        colorStreakFire = ContextCompat.getColor(context, R.color.analytics_accent_orange);

        cellPaint = new Paint();
        cellPaint.setStyle(Paint.Style.FILL);
        cellPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(colorGrid);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        titlePaint = new Paint();
        titlePaint.setColor(colorTextPrimary);
        titlePaint.setTextSize(48f);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);

        streakTextPaint = new Paint();
        streakTextPaint.setColor(colorStreakFire);
        streakTextPaint.setTextSize(40f);
        streakTextPaint.setAntiAlias(true);
        streakTextPaint.setFakeBoldText(true);
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.dayClickListener = listener;
    }

    public void setData(List<StreakDay> streakDays) {
        this.streakDays = streakDays;

        if (streakDays != null && !streakDays.isEmpty()) {
            StreakDay firstDay = streakDays.get(0);
            this.currentMonth = firstDay.getMonth();
            this.currentYear = firstDay.getYear();
        } else {
            Calendar cal = Calendar.getInstance();
            this.currentMonth = cal.get(Calendar.MONTH);
            this.currentYear = cal.get(Calendar.YEAR);
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (leftArrowRect != null && leftArrowRect.contains(x, y)) {
                goToPreviousMonth();
                return true;
            }

            if (rightArrowRect != null && rightArrowRect.contains(x, y)) {
                goToNextMonth();
                return true;
            }

            for (CalendarCell cell : calendarCells) {
                if (cell.contains(x, y)) {
                    if (dayClickListener != null) {
                        dayClickListener.onDayClick(cell.day);
                    }
                    return true;
                }
            }
        }

        return super.onTouchEvent(event);
    }

    public void goToPreviousMonth() {
        currentMonth--;
        if (currentMonth < 0) {
            currentMonth = 11;
            currentYear--;
        }
        if (monthChangeListener != null) {
            monthChangeListener.onMonthChanged(currentMonth, currentYear);
        }
        requestMonthData();
    }

    public void goToNextMonth() {
        currentMonth++;
        if (currentMonth > 11) {
            currentMonth = 0;
            currentYear++;
        }
        if (monthChangeListener != null) {
            monthChangeListener.onMonthChanged(currentMonth, currentYear);
        }
        requestMonthData();
    }

    private void requestMonthData() {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        calendarCells.clear();

        if (currentStreak > 0) {
            streakTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("🔥 " + currentStreak + " day streak",
                    getWidth() - padding, 50, streakTextPaint);
        }

        drawMonthNavigation(canvas);

        if (streakDays == null || streakDays.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        drawDayHeaders(canvas);
        drawCalendar(canvas);
        drawLegend(canvas);
    }

    private void drawMonthNavigation(Canvas canvas) {
        float navY = 85f;

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());

        Paint monthPaint = new Paint(titlePaint);
        monthPaint.setTextSize(55f);
        monthPaint.setTextAlign(Paint.Align.CENTER);
        monthPaint.setColor(colorTextPrimary);

        canvas.drawText(monthName + " " + currentYear,
                getWidth() / 2f, navY, monthPaint);

        float arrowSize = 35f;
        float leftArrowX = 60f;

        Paint arrowPaint = new Paint();
        arrowPaint.setColor(colorPrimary);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setAntiAlias(true);

        navY = 65f;
        leftArrowRect = new RectF(
                leftArrowX - 20,
                navY - arrowSize/2 - 20,
                leftArrowX + arrowSize + 20,
                navY + arrowSize/2 + 20
        );

        android.graphics.Path leftPath = new android.graphics.Path();
        leftPath.moveTo(leftArrowX + arrowSize, navY - arrowSize/2);
        leftPath.lineTo(leftArrowX, navY);
        leftPath.lineTo(leftArrowX + arrowSize, navY + arrowSize/2);
        leftPath.close();
        canvas.drawPath(leftPath, arrowPaint);

        float rightArrowX = getWidth() - 60f - arrowSize;

        rightArrowRect = new RectF(
                rightArrowX - 20,
                navY - arrowSize/2 - 20,
                rightArrowX + arrowSize + 20,
                navY + arrowSize/2 + 20
        );

        android.graphics.Path rightPath = new android.graphics.Path();
        rightPath.moveTo(rightArrowX, navY - arrowSize/2);
        rightPath.lineTo(rightArrowX + arrowSize, navY);
        rightPath.lineTo(rightArrowX, navY + arrowSize/2);
        rightPath.close();
        canvas.drawPath(rightPath, arrowPaint);
    }

    private void drawDayHeaders(Canvas canvas) {
        Paint headerPaint = new Paint(textPaint);
        headerPaint.setColor(colorTextSecondary);
        headerPaint.setTextSize(38f);

        float calendarWidth = (7 * cellSize) + (6 * cellSpacing);
        float startX = (getWidth() - calendarWidth) / 2f;

        for (int i = 0; i < 7; i++) {
            float x = startX + (i * (cellSize + cellSpacing)) + cellSize / 2;
            canvas.drawText(dayNames[i], x, topPadding - 40, headerPaint);
        }
    }

    private void drawCalendar(Canvas canvas) {
        if (streakDays.isEmpty()) return;

        StreakDay firstDay = streakDays.get(0);
        Calendar cal = Calendar.getInstance();
        cal.set(firstDay.getYear(), firstDay.getMonth(), 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;

        for (StreakDay day : streakDays) {
            int dayOfMonth = day.getDayOfMonth();
            int position = dayOfMonth - 1 + firstDayOfWeek;

            int row = position / 7;
            int col = position % 7;

            float calendarWidth = (7 * cellSize) + (6 * cellSpacing);
            float startX = (getWidth() - calendarWidth) / 2f;

            float x = startX + (col * (cellSize + cellSpacing));
            float y = topPadding + (row * (cellSize + cellSpacing));

            int color = getColorForStatus(day.getStatus());
            cellPaint.setColor(color);

            RectF rect = new RectF(x, y, x + cellSize, y + cellSize);
            canvas.drawRoundRect(rect, 12f, 12f, cellPaint);
            canvas.drawRoundRect(rect, 12f, 12f, borderPaint);

            calendarCells.add(new CalendarCell(new RectF(rect), day));

            Paint dayTextPaint = new Paint(textPaint);
            dayTextPaint.setTextSize(36f);

            if (day.getStatus() == StreakDay.StreakStatus.NONE) {
                dayTextPaint.setColor(colorTextSecondary);
            } else {
                dayTextPaint.setColor(Color.WHITE);
            }

            canvas.drawText(String.valueOf(dayOfMonth),
                    x + cellSize / 2,
                    y + cellSize / 2 + 14,
                    dayTextPaint);

            if (day.getStatus() == StreakDay.StreakStatus.EXCEPTIONAL) {
                Paint starPaint = new Paint();
                starPaint.setColor(Color.WHITE);
                starPaint.setTextSize(24f);
                starPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("⭐", x + cellSize - 15, y + 22, starPaint);
            }
        }
    }

    private void drawLegend(Canvas canvas) {
        float legendY = getHeight() - 50;

        float totalLegendWidth = 4 * 160f;
        float legendX = (getWidth() - totalLegendWidth) / 2f;

        Paint legendTextPaint = new Paint(textPaint);
        legendTextPaint.setColor(colorTextSecondary);
        legendTextPaint.setTextSize(35f);
        legendTextPaint.setTextAlign(Paint.Align.LEFT);

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

            RectF rect = new RectF(legendX - 100, legendY, legendX - 100 + 40, legendY + 40);
            canvas.drawRoundRect(rect, 10f, 10f, cellPaint);
            canvas.drawRoundRect(rect, 10f, 10f, borderPaint);

            canvas.drawText(labels[i], legendX - 100 + 65, legendY + 28, legendTextPaint);
            legendX += 200;
        }
    }

    private int getColorForStatus(StreakDay.StreakStatus status) {
        Context context = getContext();

        switch (status) {
            case EXCEPTIONAL:
                return ContextCompat.getColor(context, R.color.chart_scale_7);
            case HIT_TARGET:
                return ContextCompat.getColor(context, R.color.chart_scale_4);
            case PARTIAL:
                return ContextCompat.getColor(context, R.color.chart_scale_2);
            case NONE:
            default:
                return ContextCompat.getColor(context, R.color.chart_scale_0);
        }
    }

    private void drawEmptyState(Canvas canvas) {
        Paint emptyPaint = new Paint(textPaint);
        emptyPaint.setColor(ContextCompat.getColor(getContext(), R.color.empty_state_text));
        emptyPaint.setTextSize(36f);
        emptyPaint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText("No calendar data available",
                getWidth() / 2f, getHeight() / 2f, emptyPaint);
    }

    public void setOnMonthChangeListener(OnMonthChangeListener listener) {
        this.monthChangeListener = listener;
    }

    public interface OnMonthChangeListener {
        void onMonthChanged(int month, int year);
    }

    private static class CalendarCell {
        RectF rect;
        StreakDay day;

        CalendarCell(RectF rect, StreakDay day) {
            this.rect = rect;
            this.day = day;
        }

        boolean contains(float x, float y) {
            return rect.contains(x, y);
        }
    }

    public interface OnDayClickListener {
        void onDayClick(StreakDay day);
    }

    public OnDayClickListener getOnDayClickListener() {
        return dayClickListener;
    }

    public OnMonthChangeListener getOnMonthChangeListener() {
        return monthChangeListener;
    }

}