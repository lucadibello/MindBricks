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

        // Determine grid size dynamically
        int maxCol = 0, maxRow = 0;
        for (CitySlot slot : slots) {
            if (slot.getCol() > maxCol) maxCol = slot.getCol();
            if (slot.getRow() > maxRow) maxRow = slot.getRow();
        }

        float slotWidth = (float) getWidth() / (maxCol + 1);
        float slotHeight = (float) getHeight() / (maxRow + 1);

        for (CitySlot slot : slots) {
            float left = slot.getCol() * slotWidth;
            float top = slot.getRow() * slotHeight;
            float right = left + slotWidth;
            float bottom = top + slotHeight;

            // Draw slot fill (green if unlocked, red if locked)
            canvas.drawRect(left, top, right, bottom, slot.isUnlocked() ? paintUnlocked : paintLocked);
            // Draw slot outline
            canvas.drawRect(left, top, right, bottom, paintOutline);
        }
    }
}
