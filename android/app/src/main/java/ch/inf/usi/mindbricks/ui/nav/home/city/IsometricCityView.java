package ch.inf.usi.mindbricks.ui.nav.home.city;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.util.PreferencesManager;

public class IsometricCityView extends View {

    private List<CitySlot> slots = new ArrayList<>();
    private final Paint paintUnlocked = new Paint();
    private final Paint paintLocked = new Paint();
    private final Paint paintOutline = new Paint();
    private final Paint paintBuilding = new Paint();

    private float cellWidth;
    private float cellHeight;
    private float yOffset;

    public IsometricCityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintUnlocked.setColor(Color.GREEN);
        paintLocked.setColor(Color.RED);
        paintOutline.setColor(Color.BLACK);
        paintOutline.setStyle(Paint.Style.STROKE);
        paintOutline.setStrokeWidth(4f);
        paintBuilding.setColor(Color.BLUE);
    }

    public void setSlots(List<CitySlot> slots) {
        this.slots = slots;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (slots == null || slots.isEmpty()) return;

        int maxCol = 0, maxRow = 0;
        for (CitySlot slot : slots) {
            if (slot.getCol() > maxCol) maxCol = slot.getCol();
            if (slot.getRow() > maxRow) maxRow = slot.getRow();
        }

        cellWidth = getWidth() / (float) (maxCol + maxRow + 2) * 2f;
        cellHeight = getHeight() / (float) (maxCol + maxRow + 2) * 2f;
        yOffset = getHeight() / 10f;

        for (CitySlot slot : slots) {
            float centerX = (slot.getCol() - slot.getRow()) * cellWidth / 2 + getWidth() / 2f;
            float centerY = (slot.getCol() + slot.getRow()) * cellHeight / 2 + yOffset;
            float hw = cellWidth / 2f;
            float hh = cellHeight / 2f;

            float[] verts = {centerX, centerY - hh, centerX + hw, centerY,
                    centerX, centerY + hh, centerX - hw, centerY};

            canvas.drawVertices(Canvas.VertexMode.TRIANGLE_FAN, verts.length, verts, 0,
                    null, 0, null, 0, null, 0, 0,
                    slot.isUnlocked() ? paintUnlocked : paintLocked);

            canvas.drawLine(verts[0], verts[1], verts[2], verts[3], paintOutline);
            canvas.drawLine(verts[2], verts[3], verts[4], verts[5], paintOutline);
            canvas.drawLine(verts[4], verts[5], verts[6], verts[7], paintOutline);
            canvas.drawLine(verts[6], verts[7], verts[0], verts[1], paintOutline);

            if (slot.getBuilding() != null) {
                canvas.drawCircle(centerX, centerY, Math.min(hw, hh) / 2, paintBuilding);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        float x = event.getX();
        float y = event.getY();
        CitySlot clicked = null;

        for (CitySlot slot : slots) {
            float cx = (slot.getCol() - slot.getRow()) * cellWidth / 2 + getWidth() / 2f;
            float cy = (slot.getCol() + slot.getRow()) * cellHeight / 2 + yOffset;
            float hw = cellWidth / 2f;
            float hh = cellHeight / 2f;

            if (x >= cx - hw && x <= cx + hw && y >= cy - hh && y <= cy + hh) {
                clicked = slot;
                break;
            }
        }

        if (clicked != null && clicked.isUnlocked()) {
            showBuildingSelectionDialog(clicked);
        }

        return true;
    }

    private void showBuildingSelectionDialog(CitySlot slot) {
        Context context = getContext();
        PreferencesManager prefs = new PreferencesManager(context);
        String[] purchasedBuildings = prefs.getPurchasedItemIds().toArray(new String[0]);

        new AlertDialog.Builder(context)
                .setTitle("Select Building")
                .setItems(purchasedBuildings, (dialog, which) -> {
                    slot.setBuilding(purchasedBuildings[which]);
                    invalidate();
                })
                .show();
    }
}
