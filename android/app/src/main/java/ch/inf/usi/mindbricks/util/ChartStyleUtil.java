package ch.inf.usi.mindbricks.util;

import android.content.Context;
import android.graphics.Color;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import java.util.Locale;

/**
 * Utility class for applying consistent styling to MPAndroidChart charts
 */
public class ChartStyleUtil {

    // Material Design color palette
    public static final int COLOR_PRIMARY = Color.rgb(33, 150, 243);      // Blue
    public static final int COLOR_SECONDARY = Color.rgb(156, 39, 176);    // Purple
    public static final int COLOR_SUCCESS = Color.rgb(76, 175, 80);       // Green
    public static final int COLOR_WARNING = Color.rgb(255, 152, 0);       // Orange
    public static final int COLOR_DANGER = Color.rgb(244, 67, 54);        // Red
    public static final int COLOR_INFO = Color.rgb(3, 169, 244);          // Light Blue

    // Productivity level colors
    public static final int PROD_LOW = COLOR_DANGER;
    public static final int PROD_MEDIUM = COLOR_WARNING;
    public static final int PROD_HIGH = COLOR_SUCCESS;

    // Chart text sizes
    private static final float TEXT_SIZE_LABEL = 10f;
    private static final float TEXT_SIZE_VALUE = 12f;
    private static final float TEXT_SIZE_LEGEND = 11f;

    /**
     * Applies standard styling to any chart
     */
    public static void applyStandardStyle(Chart<?> chart, Context context) {
        chart.setNoDataText("No data available");
        chart.setNoDataTextColor(Color.GRAY);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.animateY(800);

        // Description
        chart.getDescription().setEnabled(false);

        // Legend
        Legend legend = chart.getLegend();
        legend.setTextSize(TEXT_SIZE_LEGEND);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(8f);
        legend.setXEntrySpace(10f);
        legend.setYEntrySpace(5f);
    }

    /**
     * Styles X-axis with standard settings
     */
    public static void styleXAxis(XAxis xAxis) {
        xAxis.setTextSize(TEXT_SIZE_LABEL);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineWidth(1f);
        xAxis.setAxisLineColor(Color.LTGRAY);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setAvoidFirstLastClipping(true);
    }

    /**
     * Styles Y-axis with standard settings
     */
    public static void styleYAxis(YAxis yAxis, boolean isLeft) {
        yAxis.setTextSize(TEXT_SIZE_LABEL);
        yAxis.setDrawGridLines(true);
        yAxis.setGridLineWidth(0.5f);
        yAxis.setGridColor(Color.LTGRAY);
        yAxis.setDrawAxisLine(false);
        yAxis.setTextColor(Color.DKGRAY);
        yAxis.setGranularity(1f);

        if (!isLeft) {
            yAxis.setEnabled(false);
        }
    }

    /**
     * Gets color for productivity level
     *
     * @param productivity 0-100 scale
     */
    public static int getProductivityColor(int productivity) {
        if (productivity < 40) return PROD_LOW;
        if (productivity < 70) return PROD_MEDIUM;
        return PROD_HIGH;
    }

    /**
     * Gets color with alpha transparency
     */
    public static int getColorWithAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Creates gradient colors for charts
     *
     * @param startColor Starting color
     * @param endColor   Ending color
     * @param steps      Number of gradient steps
     */
    public static int[] createGradient(int startColor, int endColor, int steps) {
        int[] gradient = new int[steps];

        float startR = Color.red(startColor);
        float startG = Color.green(startColor);
        float startB = Color.blue(startColor);

        float endR = Color.red(endColor);
        float endG = Color.green(endColor);
        float endB = Color.blue(endColor);

        float stepR = (endR - startR) / (steps - 1);
        float stepG = (endG - startG) / (steps - 1);
        float stepB = (endB - startB) / (steps - 1);

        for (int i = 0; i < steps; i++) {
            gradient[i] = Color.rgb(
                    (int) (startR + stepR * i),
                    (int) (startG + stepG * i),
                    (int) (startB + stepB * i)
            );
        }

        return gradient;
    }

    /**
     * Returns colors from app's theme
     */
    public static int getThemeColor(Context context, int colorResId) {
        return context.getColor(colorResId);
    }

    /**
     * Format for productivity percentage display
     */
    public static String formatProductivity(float value) {
        return String.format(Locale.getDefault(), "%d%%", (int) value);
    }

    /**
     * Format for duration display
     */
    public static String formatDuration(int minutes) {
        if (minutes < 60) {
            return String.format(Locale.getDefault(), "%dm", minutes);
        } else {
            int hours = minutes / 60;
            int mins = minutes % 60;
            return String.format(Locale.getDefault(), "%dh %dm", hours, mins);
        }
    }

    /**
     * Format for time of day
     */
    public static String formatHour(int hour) {
        if (hour == 0) return "12 AM";
        if (hour < 12) return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }

    /**
     * Format for day of week
     */
    public static String formatDayOfWeek(int dayOfWeek) {
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        return days[dayOfWeek % 7];
    }
}