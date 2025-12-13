package ch.inf.usi.mindbricks.ui.nav.home.city;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

public class IsometricCityView extends View {

    private List<CitySlot> slots;

    private final int tileW = 120;
    private final int tileH = 60;

    public IsometricCityView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSlots(List<CitySlot> slots) {
        this.slots = slots;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slots == null) return;

        slots.stream()
                .filter(CitySlot::isUnlocked)
                .sorted((a, b) -> (a.row + a.col) - (b.row + b.col))
                .forEach(slot -> drawSlot(canvas, slot));
    }

    private void drawSlot(Canvas canvas, CitySlot slot) {
        int x = (slot.col - slot.row) * tileW / 2 + getWidth() / 2;
        int y = (slot.col + slot.row) * tileH / 2 + 100;

        Paint paint = new Paint();
        paint.setColor(slot.getBuildingId() == null
                ? Color.argb(120, 200, 200, 200)
                : Color.GREEN);

        Path diamond = new Path();
        diamond.moveTo(x, y);
        diamond.lineTo(x + tileW / 2f, y + tileH / 2f);
        diamond.lineTo(x, y + tileH);
        diamond.lineTo(x - tileW / 2f, y + tileH / 2f);
        diamond.close();

        canvas.drawPath(diamond, paint);
    }
}
