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

import ch.inf.usi.mindbricks.model.AIRecommendation;

/**
 * Displays "AI-generated" recommendation cards
 */
public class AIRecommendationCardView extends View {

    private Paint cardPaint;
    private Paint titlePaint;
    private Paint messagePaint;
    private Paint iconPaint;

    private List<AIRecommendation> recommendations;

    private float cardHeight = 150f;
    private float cardSpacing = 20f;
    private float padding = 40f;
    private float cardPadding = 30f;

    public AIRecommendationCardView(Context context) {
        super(context);
        init();
    }

    public AIRecommendationCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cardPaint = new Paint();
        cardPaint.setStyle(Paint.Style.FILL);
        cardPaint.setAntiAlias(true);

        titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#212121"));
        titlePaint.setTextSize(36f);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);

        messagePaint = new Paint();
        messagePaint.setColor(Color.parseColor("#424242"));
        messagePaint.setTextSize(30f);
        messagePaint.setAntiAlias(true);

        iconPaint = new Paint();
        iconPaint.setTextSize(48f);
        iconPaint.setAntiAlias(true);
    }

    public void setData(List<AIRecommendation> recommendations) {
        this.recommendations = recommendations;

        // Adjust view height based on number of cards
        if (recommendations != null) {
            int height = (int) ((recommendations.size() * (cardHeight + cardSpacing)) + padding * 2);
            setMinimumHeight(height);
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (recommendations == null || recommendations.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        float currentY = padding;

        for (AIRecommendation rec : recommendations) {
            drawCard(canvas, rec, currentY);
            currentY += cardHeight + cardSpacing;
        }
    }

    private void drawCard(Canvas canvas, AIRecommendation rec, float y) {
        float cardWidth = getWidth() - (2 * padding);

        // Draw card background
        cardPaint.setColor(rec.getBackgroundColor());
        RectF cardRect = new RectF(padding, y, padding + cardWidth, y + cardHeight);
        canvas.drawRoundRect(cardRect, 20f, 20f, cardPaint);

        // Draw icon
        String icon = getIconForType(rec.getType());
        iconPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(icon, padding + cardPadding, y + cardPadding + 40, iconPaint);

        // Draw title
        titlePaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(rec.getTitle(), padding + cardPadding + 70, y + cardPadding + 35, titlePaint);

        // Draw message (word wrap if needed)
        messagePaint.setTextAlign(Paint.Align.LEFT);
        drawMultilineText(canvas, rec.getMessage(),
                padding + cardPadding + 70,
                y + cardPadding + 75,
                cardWidth - cardPadding - 80,
                messagePaint);
    }

    private void drawMultilineText(Canvas canvas, String text, float x, float y,
                                   float maxWidth, Paint paint) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float lineY = y;
        float lineHeight = paint.getTextSize() + 10;

        for (String word : words) {
            String testLine = line.toString() + word + " ";
            float testWidth = paint.measureText(testLine);

            if (testWidth > maxWidth && line.length() > 0) {
                canvas.drawText(line.toString(), x, lineY, paint);
                line = new StringBuilder(word + " ");
                lineY += lineHeight;
            } else {
                line.append(word).append(" ");
            }
        }

        if (line.length() > 0) {
            canvas.drawText(line.toString(), x, lineY, paint);
        }
    }

    private String getIconForType(AIRecommendation.RecommendationType type) {
        // we can use something else, it just felt nice ->
        // TODO: Discuss detail
        switch (type) {
            case BEST_HOUR:
                return "⚡";
            case AVOID_HOUR:
                return "⚠️";
            case TOMORROW_FORECAST:
                return "🔮";
            case STREAK_ALERT:
                return "🔥";
            case QUALITY_TIP:
                return "💡";
            default:
                return "ℹ️";
        }
    }

    private void drawEmptyState(Canvas canvas) {
        Paint emptyPaint = new Paint(messagePaint);
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setColor(Color.parseColor("#757575"));

        canvas.drawText("No recommendations yet",
                getWidth() / 2f, getHeight() / 2f, emptyPaint);

        emptyPaint.setTextSize(24f);
        canvas.drawText("Complete more sessions to get personalized insights",
                getWidth() / 2f, getHeight() / 2f + 40, emptyPaint);
    }
}