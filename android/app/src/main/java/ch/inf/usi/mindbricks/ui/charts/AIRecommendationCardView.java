package ch.inf.usi.mindbricks.ui.charts;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.AIRecommendation;


public class AIRecommendationCardView extends View {
    private static final String TAG = "AIRecommendationChart";

    private AIRecommendation scheduleData;

    // Paints
    private Paint blockPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private Paint gridPaint;
    private Paint confidencePaint;
    private Paint linePaint;

    // Layout dimensions
    private float leftMargin = 60f;
    private float topMargin = 60f;
    private float rightMargin = 60f;
    private float bottomMargin = 40;

    // Zoom
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3.0f;

    private static final float MIN_CHART_WIDTH = 2400f;

    // block thresholds
    private static final float BLOCK_TEXT = 100f;
    private static final float BLOCK_TIME = 140f;
    private static final int ICON_SIZE = 60 ;

    // Activity type to Y-position mapping
    private List<ActivityLayer> activityLayers;
    private List<BlockBounds> blockBounds;
    private Map<AIRecommendation.ActivityType, Bitmap> iconCache;

    public AIRecommendationCardView(Context context) {
        super(context);
        init();
    }

    public AIRecommendationCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blockPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.analytics_card_background));
        textPaint.setTextSize(spToPx(12));
        textPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.analytics_text_primary));
        labelPaint.setTextSize(spToPx(12));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.analytics_grid_line_major));
        gridPaint.setStrokeWidth(2);
        gridPaint.setStyle(Paint.Style.STROKE);

        confidencePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        confidencePaint.setColor(ContextCompat.getColor(getContext(), R.color.analytics_text_secondary));
        confidencePaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.analytics_text_secondary));

        activityLayers = new ArrayList<>();
        blockBounds = new ArrayList<>();

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        setClickable(true);

        iconCache = new HashMap<>();
    }

    public void setData(AIRecommendation data) {
        Log.d(TAG, "setData called: " + (data != null ?
                data.getActivityBlocks().size() + " blocks" : "null"));
        this.scheduleData = data;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // Use minimum width for horizontal scrolling
        int finalWidth = (int) (Math.max(width, MIN_CHART_WIDTH) * scaleFactor);

        setMeasuredDimension(finalWidth, height);
        Log.d(TAG, "onMeasure: " + finalWidth + "x" + height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.scale(scaleFactor, 1.0f);

        blockBounds.clear();

        if (scheduleData == null || scheduleData.getActivityBlocks().isEmpty()) {
            drawNoDataMessage(canvas);
            canvas.restore();
            return;
        }

        List<AIRecommendation.ActivityBlock> blocks = scheduleData.getActivityBlocks();
        Log.d(TAG, "Drawing " + blocks.size() + " activity blocks");

        float chartWidth = (getWidth() / scaleFactor) - leftMargin - rightMargin;
        float chartHeight = getHeight() - topMargin - bottomMargin;

        drawTimeGrid(canvas, chartWidth, chartHeight);
        drawHourLabels(canvas, chartWidth);
        drawStackedActivityBlocks(canvas, blocks, chartWidth, chartHeight);

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN && !scaleDetector.isInProgress()) {
            float x = event.getX() / scaleFactor;
            float y = event.getY() / scaleFactor;

            // check block bounds
            for (BlockBounds bounds : blockBounds) {
                if (bounds.contains(x, y)) {
                    showBlockDetails(bounds.block);
                    return true;
                }
            }
        }

        return true;
    }

    @SuppressLint("ResourceType")
    private void showBlockDetails(AIRecommendation.ActivityBlock block) {
        Context context = getContext();
        if (context == null) return;

        String title = block.getActivityType().getDisplayName();
        String timeRange = block.getTimeRange();
        String confidence = "-> Confidence: " + block.getConfidenceScore() + "%";
        String reason = block.getReason();

        String message = timeRange + "\n\n" +
                confidence + "\n\n" +
                "Why this recommendation?\n" + reason;

        Drawable iconDrawable = ContextCompat.getDrawable(context, getActivityIcon(block.getActivityType()));

        if (iconDrawable != null) {
            int color = labelPaint.getColor();
            iconDrawable = iconDrawable.mutate();
            iconDrawable.setTint(color);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setPositiveButton("Got it", null);
        builder.setMessage(message);
        builder.setIcon(getActivityIcon(block.getActivityType()));
        AlertDialog dialog = builder.create();
        dialog.show();

        ImageView imageView = dialog.findViewById(android.R.id.icon);
        if (imageView != null)
            imageView.setColorFilter(R.color.analytics_text_tertiary, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private int getActivityIcon(AIRecommendation.ActivityType type) {
        switch (type) {
            case DEEP_STUDY:
                return R.drawable.ic_book;
            case LIGHT_STUDY:
                return R.drawable.ic_books;
            case WORK:
                return R.drawable.ic_work;
            case EXERCISE:
                return R.drawable.ic_exercise;
            case SOCIAL:
                return R.drawable.ic_chat;
            case MEALS:
                return R.drawable.ic_coffee;
            case BREAKS:
                return R.drawable.ic_pause;
            case SLEEP:
                return R.drawable.ic_bed;
            default:
                return android.R.drawable.ic_menu_info_details;
        }
    }

    private void drawTimeGrid(Canvas canvas, float chartWidth, float chartHeight) {
        float hourWidth = chartWidth / 24f;

        // Draw vertical grid lines
        for (int hour = 0; hour <= 24; hour += 3) {
            float x = leftMargin + (hour * hourWidth);
            canvas.drawLine(x, topMargin, x, topMargin + chartHeight, gridPaint);
        }

        // Draw horizontal grid lines
        for (int i = 0; i <= 4; i++) {
            float y = topMargin + (i * chartHeight / 4f);
            canvas.drawLine(leftMargin, y, leftMargin + chartWidth, y, gridPaint);
        }
    }

    private void drawHourLabels(Canvas canvas, float chartWidth) {
        float hourWidth = chartWidth / 24f;
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Draw hour markers
        for (int hour = 0; hour < 24; hour += 2) {
            float x = leftMargin + (hour * hourWidth);
            String label = formatHour(hour);
            canvas.drawText(label, x, topMargin - 10, labelPaint);
        }
    }

    private void drawStackedActivityBlocks(Canvas canvas,
                                           List<AIRecommendation.ActivityBlock> blocks,
                                           float chartWidth, float chartHeight) {

        float hourWidth = chartWidth / 24f;

        // Assign layers to activities
        assignActivityLayers(blocks);

        // Calculate layer height
        int maxLayer = 0;
        for (ActivityLayer layer : activityLayers) {
            maxLayer = Math.max(maxLayer, layer.layer);
        }
        float layerHeight = chartHeight / (maxLayer + 2);

        boolean detail = false;
        for (int i = 0; i < blocks.size(); i++) {
            AIRecommendation.ActivityBlock block = blocks.get(i);
            ActivityLayer layerInfo = activityLayers.get(i);

            float startX = leftMargin + (block.getStartHour() * hourWidth);
            float endX = leftMargin + (block.getEndHour() * hourWidth);
            float blockWidth = endX - startX;

            float baseY = topMargin + chartHeight - ((layerInfo.layer + 1) * layerHeight);
            float blockHeight = layerHeight * 0.9f;

            int color = Color.parseColor(block.getActivityType().getHex(getContext()));
            int alpha = (int) (255 * 0.85f);
            blockPaint.setColor(color);
            blockPaint.setAlpha(alpha);

            // Draw the block
            RectF blockRect = new RectF(startX, baseY, endX, baseY + blockHeight);
            canvas.drawRoundRect(blockRect, 12, 12, blockPaint);

            // Store bounds for click detection
            blockBounds.add(new BlockBounds(blockRect, block));

            // Apply confidence overlay
            if (block.getConfidenceScore() < 80) {
                int confidenceAlpha = (int) ((100 - block.getConfidenceScore()) * 2.0f);
                confidencePaint.setAlpha(Math.min(confidenceAlpha, 120));
                canvas.drawRoundRect(blockRect, 12, 12, confidencePaint);
            }

            // Draw outline
            linePaint.setColor(darkenColor(color, 0.3f));
            linePaint.setAlpha(180);
            canvas.drawRoundRect(blockRect, 12, 12, linePaint);

            drawIcon(canvas, block, startX, blockWidth, baseY, blockHeight);
        }

        // Reset alpha
        blockPaint.setAlpha(255);
        textPaint.setAlpha(255);
    }

    private void drawIcon(Canvas canvas, AIRecommendation.ActivityBlock block,
                                  float startX, float blockWidth,
                                  float baseY, float blockHeight) {
        Bitmap icon = getIconBitmap(block.getActivityType(), ICON_SIZE);
        if (icon != null) {
            float iconX = startX + (blockWidth - ICON_SIZE) / 2f;
            float iconY = baseY + (blockHeight - ICON_SIZE) / 2f;

            textPaint.setAlpha(255);
            canvas.drawBitmap(icon, iconX, iconY, textPaint);
        }
    }

    private Bitmap getIconBitmap(AIRecommendation.ActivityType type, int size) {
        // Check cache first -> quicker access
        if (iconCache.containsKey(type)) {
            return iconCache.get(type);
        }

        int iconResId = getActivityIcon(type);
        Drawable drawable = ContextCompat.getDrawable(getContext(), iconResId);

        if (drawable == null) return null;

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        // Cache it
        iconCache.put(type, bitmap);
        return bitmap;
    }

    private void assignActivityLayers(List<AIRecommendation.ActivityBlock> blocks) {
        activityLayers.clear();
        List<AIRecommendation.ActivityType> typeOrder = new ArrayList<>();

        for (AIRecommendation.ActivityBlock block : blocks) {
            AIRecommendation.ActivityType type = block.getActivityType();

            int layer = typeOrder.indexOf(type);
            if (layer == -1) {
                layer = typeOrder.size();
                typeOrder.add(type);
            }

            activityLayers.add(new ActivityLayer(type, layer));
        }
    }

    private void drawNoDataMessage(Canvas canvas) {
        Paint messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        messagePaint.setColor(Color.GRAY);
        messagePaint.setTextSize(spToPx(16));
        messagePaint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText(
                "Complete more study sessions",
                getWidth() / 2f,
                getHeight() / 2f - 20,
                messagePaint
        );

        messagePaint.setTextSize(spToPx(14));
        canvas.drawText(
                "to get personalized schedule recommendations",
                getWidth() / 2f,
                getHeight() / 2f + 10,
                messagePaint
        );
    }

    public List<LegendItem> getLegendItems() {
        if (scheduleData == null || scheduleData.getActivityBlocks().isEmpty()) {
            return new ArrayList<>();
        }

        List<LegendItem> items = new ArrayList<>();
        List<AIRecommendation.ActivityType> uniqueTypes = new ArrayList<>();

        for (AIRecommendation.ActivityBlock block : scheduleData.getActivityBlocks()) {
            AIRecommendation.ActivityType type = block.getActivityType();
            if (!uniqueTypes.contains(type)) {
                uniqueTypes.add(type);
                items.add(new LegendItem(
                        type.getDisplayName(),
                        type.getHex(getContext())
                ));
            }
        }

        return items;
    }

    // helping classes and functions
    private String formatHour(int hour) {
        if (hour == 0) return "12:00 AM";
        if (hour < 12) return hour + ":00 AM";
        if (hour == 12) return "12:00 PM";
        return (hour - 12) + ":00 PM";
    }

    private int darkenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = (int) (Color.red(color) * (1 - factor));
        int g = (int) (Color.green(color) * (1 - factor));
        int b = (int) (Color.blue(color) * (1 - factor));
        return Color.argb(a, r, g, b);
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                getResources().getDisplayMetrics()
        );
    }

    private static class ActivityLayer {
        AIRecommendation.ActivityType type;
        int layer;

        ActivityLayer(AIRecommendation.ActivityType type, int layer) {
            this.type = type;
            this.layer = layer;
        }
    }

    private static class BlockBounds {
        RectF rect;
        AIRecommendation.ActivityBlock block;

        BlockBounds(RectF rect, AIRecommendation.ActivityBlock block) {
            this.rect = new RectF(rect);
            this.block = block;
        }

        boolean contains(float x, float y) {
            return rect.contains(x, y);
        }
    }

    public static class LegendItem {
        public final String name;
        public final String colorHex;

        public LegendItem(String name, String colorHex) {
            this.name = name;
            this.colorHex = colorHex;
        }
    }

    public String getSummaryMessage() {
        return scheduleData != null ? scheduleData.getSummaryMessage() : null;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));

            requestLayout();
            invalidate();

            return true;
        }
    }
}