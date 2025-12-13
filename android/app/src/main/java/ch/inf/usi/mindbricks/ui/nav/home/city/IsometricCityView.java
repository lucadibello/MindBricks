package ch.inf.usi.mindbricks.ui.nav.home.city;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class IsometricCityView extends View {

    private List<CitySlot> slots = new ArrayList<>();
    private final Paint paintUnlocked = new Paint();
    private final Paint paintLocked = new Paint();
    private final Paint paintOutline = new Paint();

    public IsometricCityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintUnlocked.setColor(Color.GREEN);
        paintLocked.setColor(Color.RED);
        paintOutline.setColor(Color.BLACK);
        paintOutline.setStyle(Paint.Style.STROKE);
        paintOutline.setStrokeWidth(4f);
    }


    public void setSlots(List<CitySlot> slots) {
        this.slots = slots;
        invalidate(); // redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slots == null || slots.isEmpty()) return;

        // Determine grid size
        int maxCol = 0, maxRow = 0;
        for (CitySlot slot : slots) {
            if (slot.getCol() > maxCol) maxCol = slot.getCol();
            if (slot.getRow() > maxRow) maxRow = slot.getRow();
        }

        float cellWidth = getWidth() / (float) (maxCol + maxRow + 2);
        float cellHeight = getHeight() / (float) (maxCol + maxRow + 2);

        for (CitySlot slot : slots) {
            // Convert grid to isometric coordinates
            float isoX = (slot.getCol() - slot.getRow()) * cellWidth / 2 + getWidth() / 2f;
            float isoY = (slot.getCol() + slot.getRow()) * cellHeight / 2;

            // Draw diamond shape
            float halfWidth = cellWidth / 2f;
            float halfHeight = cellHeight / 2f;

            float[] points = {
                    isoX, isoY - halfHeight,      // top
                    isoX + halfWidth, isoY,       // right
                    isoX, isoY + halfHeight,      // bottom
                    isoX - halfWidth, isoY        // left
            };

            // Fill the diamond
            Paint paint = slot.isUnlocked() ? paintUnlocked : paintLocked;
            canvas.drawPath(createDiamondPath(points), paint);

            // Draw outline
            canvas.drawPath(createDiamondPath(points), paintOutline);
        }
    }

    private android.graphics.Path createDiamondPath(float[] points) {
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(points[0], points[1]);
        path.lineTo(points[2], points[3]);
        path.lineTo(points[4], points[5]);
        path.lineTo(points[6], points[7]);
        path.close();
        return path;
    }

}
