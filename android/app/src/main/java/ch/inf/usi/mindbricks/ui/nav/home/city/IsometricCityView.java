package ch.inf.usi.mindbricks.ui.nav.home.city;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import ch.inf.usi.mindbricks.game.TileAsset;
import ch.inf.usi.mindbricks.game.TileBitmapLoader;
import ch.inf.usi.mindbricks.game.TilePlacement;
import ch.inf.usi.mindbricks.game.TileWorldState;

public class IsometricCityView extends View {

    /**
     * Paint object for drawing grid outlines.
     */
    private final Paint gridOutline = new Paint();

    /**
     * Paint object for filling the base tile area.
     */
    private final Paint baseFill = new Paint();

    /**
     * Paint object for highlighting the drop area during drag.
     */
    private final Paint dragHighlightPaint = new Paint();

    /**
     * Current state of the tile world (grid and placements).
     */
    private TileWorldState worldState;

    /**
     * Index of available tile assets by their IDs.
     */
    private Map<String, TileAsset> assetIndex = new HashMap<>();

    /**
     * Loader for tile bitmaps.
     */
    private TileBitmapLoader bitmapLoader;

    /**
     * Listener for tile drop events.
     */
    private OnTileDropListener onTileDropListener;

    /**
     * Top Y coordinate of the exclusion zone (bottom sheet).
     */
    private float exclusionZoneTopY = Float.MAX_VALUE;

    /**
     * Width of each tile in pixels.
     */
    private float tileWidth;

    /**
     * Height of each tile in pixels.
     */
    private float tileHeight;

    /**
     * Origin coordinates for drawing the grid.
     */
    private float originX;

    /**
     * Origin Y coordinate for drawing the grid.
     */
    private float originY;

    /**
     * Current scale factor for zooming.
     * Default increased to start more zoomed-in for better view of multi-cell buildings.
     */
    private float scaleFactor = (MAX_SCALE - MIN_SCALE) * 0.5f;

    /**
     * Minimum allowed scale factor.
     * Increased to prevent excessive zoom-out that makes buildings too small.
     */
    private static final float MIN_SCALE = 2.0f;

    /**
     * Maximum allowed scale factor.
     */
    private static final float MAX_SCALE = 10f;

    /**
     * Multiplicative factory for zoom sensitivity (lower = less)
     */
    private static final float ZOOM_SENSITIVITY_FACTOR = 1f;

    /**
     * Margin for clamping pan to prevent the map from going too far off-screen.
     */
    private static final float PAN_MARGIN_DP = 50f;

    /**
     * Current pan offsets for panning the view.
     */
    private float panX = 0f;

    /**
     * Current pan Y offset for panning the view.
     */
    private float panY = 0f;

    /**
     * Last touch X coordinate (for panning).
     */
    private float lastTouchX;

    /**
     * Last touch Y coordinate (for panning).
     */
    private float lastTouchY;

    /**
     * Scale gesture detector for handling pinch-to-zoom gestures.
     */
    private final ScaleGestureDetector scaleGestureDetector;

    // Drag state tracking
    private String draggingTileId = null;
    private int draggingRow = -1;
    private int draggingCol = -1;

    /**
     * Constructor method.
     * @param context Context of the view
     * @param attrs Attribute set from XML
     */
    public IsometricCityView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // set up paint for grid outline
        gridOutline.setColor(Color.BLACK);
        gridOutline.setStyle(Paint.Style.STROKE);
        gridOutline.setStrokeWidth(2f);
        gridOutline.setAlpha(0); // by default - transparent

        // set base fill color (fill any gaps in the grid)
        baseFill.setColor(Color.argb(255, 205, 230, 200));

        // set drag highlight color (orange, semi-transparent)
        dragHighlightPaint.setColor(Color.argb(128, 255, 165, 0));
        dragHighlightPaint.setStyle(Paint.Style.FILL);

        // initialize scale gesture detector (pinch-to-zoom support)
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float oldScale = scaleFactor;
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                // update scale factor based on gesture + apply sensitivity
                float scaleFactorChange = 1.0f + (detector.getScaleFactor() - 1.0f) * ZOOM_SENSITIVITY_FACTOR;
                float newScale = oldScale * scaleFactorChange;

                // clamp scale factor to min/max limits
                newScale = Math.max(MIN_SCALE, Math.min(newScale, MAX_SCALE));

                // Apply new scale
                scaleFactor = newScale;

                // Adjust pan to zoom towards the focus point
                // Logic: (focusX - panX) / oldScale == (focusX - newPanX) / newScale
                float scaleRatio = newScale / oldScale;
                panX = focusX - (focusX - panX) * scaleRatio;
                panY = focusY - (focusY - panY) * scaleRatio;
                
                // Constrain panning after zoom
                clampPan();

                // force redraw of canvas
                invalidate();
                return true;
            }
        });

        // set drag listener for tile dropping
        setOnDragListener(this::handleDrag);
    }

    public void setTileAssets(Map<String, TileAsset> assetIndex, TileBitmapLoader loader) {
        if (assetIndex != null) this.assetIndex = assetIndex;
        this.bitmapLoader = loader;
        // redraw using loader
        invalidate();
    }

    /**
     * Set the current world state to be displayed.
     * @param worldState The world state object to render
     */
    public void setWorldState(@Nullable TileWorldState worldState) {
        this.worldState = worldState;
        // FIXME: is this really needed?
        requestLayout();
        // redraw canvas using new world state
        invalidate();
    }

    /**
     * Set the listener for tile drop events.
     * @param listener The listener to notify on tile drops
     */
    public void setOnTileDropListener(OnTileDropListener listener) {
        this.onTileDropListener = listener;
    }

    /**
     * Set the exclusion zone top y coordinate. Drops within this area will be ignored
     *
     * @param topY The Y coordinate of the exclusion zone in screen coordinates
     */
    public void setExclusionZoneTopY(float topY) {
        this.exclusionZoneTopY = topY;
    }

    /**
     * Recenter the map in the visible area (above the exclusion zone).
     */
    public void recenterMap() {
        if (worldState == null || getWidth() == 0 || getHeight() == 0) return;

        // recompute geometry
        computeGeometry();

        // Calculate visible height (view height - covered bottom part)
        float visibleHeight = (exclusionZoneTopY < getHeight()) ? exclusionZoneTopY : getHeight();

        // compute true center
        float gridCenterX = getWidth() / 2f;
        float gridCenterY = getHeight() / 2f;

        // Target center on screen (center of the visible area)
        float targetCenterX = getWidth() / 2f;
        float targetCenterY = visibleHeight / 2f;

        // recompute correct pan to center
        panX = targetCenterX - gridCenterX * scaleFactor;
        panY = targetCenterY - gridCenterY * scaleFactor;

        // Ensure the new pan values are within bounds
        clampPan();

        // redraw
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        // notify size change
        super.onSizeChanged(w, h, old_w, old_h);
        // recompute geometry (fit new size)
        computeGeometry();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (worldState == null) return;

        // draw the grid
        computeGeometry();

        // draw tiles
        canvas.save(); // push default state
        canvas.translate(panX, panY); // apply transformations
        canvas.scale(scaleFactor, scaleFactor); // apply scaling

        // get base tile bitmap
        Bitmap baseTile = resolveBaseBitmap();
        if (baseTile == null) {
            Log.e("IsometricCityView", "Base tile ID '" + worldState.getBaseTileId() + "' could not be resolved. Impossible to draw base tile.");
        }

        // Determine drop zone bounds if dragging
        int dropStartRow = -1, dropEndRow = -1;
        int dropStartCol = -1, dropEndCol = -1;

        if (draggingTileId != null && draggingRow != -1 && draggingCol != -1) {
            TileAsset asset = assetIndex.get(draggingTileId);
            if (asset != null) {
                int[] size = asset.getSize();
                int h = size[0];
                int w = size[1];
                dropStartRow = draggingRow;
                dropEndRow = draggingRow + h - 1;
                dropStartCol = draggingCol;
                dropEndCol = draggingCol + w - 1;
            }
        }

        // Pass 1: Draw base tiles (ground) for all cells
        for (int r = 0; r < worldState.getRows(); r++) {
            for (int c = 0; c < worldState.getCols(); c++) {
                // identify tile rectangle (cell of the grid)
                float left = originX + c * tileWidth;
                float top = originY + r * tileHeight;
                float right = left + tileWidth;
                float bottom = top + tileHeight;

                // draw base fill
                canvas.drawRect(left, top, right, bottom, baseFill);

                // draw base tile
                if (baseTile != null) {
                    drawGridBitmap(canvas, baseTile, left, top, tileWidth, tileHeight, 1.0f);
                }

                // Check if we need to highlight this cell
                boolean isDropZone = r >= dropStartRow && r <= dropEndRow && c >= dropStartCol && c <= dropEndCol;
                boolean isDragging = draggingTileId != null;
                boolean isOccupied = worldState.getPlacementAt(r, c) != null;

                // Draw drag highlight if this cell is within the drop zone OR (dragging is active AND cell is occupied)
                if (isDropZone || (isDragging && isOccupied)) {
                    canvas.drawRect(left, top, right, bottom, dragHighlightPaint);
                }
            }
        }

        // Pass 2: Draw placed objects and grid outlines
        for (int r = 0; r < worldState.getRows(); r++) {
            for (int c = 0; c < worldState.getCols(); c++) {
                // identify tile rectangle (cell of the grid)
                float left = originX + c * tileWidth;
                float top = originY + r * tileHeight;
                float right = left + tileWidth;
                float bottom = top + tileHeight;

                // get tile at this position of the grid
                TilePlacement placement = worldState.getPlacementAt(r, c);

                // check if this cell is the anchor of a placed tile (avoid drawing same tile multiple times)
                boolean isAnchor = placement != null && worldState.isAnchor(r, c);

                // draw only the anchor cell of the tile placement
                if (placement != null && isAnchor) {
                    // adjust with scaling
                    float targetW = tileWidth * placement.getWidth();
                    float targetH = tileHeight * placement.getHeight();

                    // load bitmap with expected size
                    Bitmap placed = resolveBitmap(
                        placement.getTileId(),
                        (int) targetW,
                        (int) targetH
                    );

                    // draw bitmap
                    drawGridBitmap(canvas, placed, left, top, targetW, targetH, 0.98f);
                }

                // draw grid outline overlay
                canvas.drawRect(left, top, right, bottom, gridOutline);
            }
        }

        // restore default state (pop transformations/scaling)
        canvas.restore();
    }

    /**
     * Compute the geometry of the grid based on the current view size and world state.
     */
    private void computeGeometry() {
        // compute number of rows and columns
        int rows = worldState != null ? worldState.getRows() : 8;
        int cols = worldState != null ? worldState.getCols() : 8;

        // compute tile size to fit the view
        float availableW = getWidth();
        float availableH = getHeight();

        // pick the smaller dimension to ensure the grid fits (square tiles)
        tileWidth = Math.min(availableW / cols, availableH / rows);
        //noinspection SuspiciousNameCombination
        tileHeight = tileWidth;

        // compute origin to center the grid
        float contentW = cols * tileWidth;
        float contentH = rows * tileHeight;
        originX = (availableW - contentW) / 2f;
        originY = (availableH - contentH) / 2f;
    }

    /**
     * Convert screen coordinates to grid coordinates.
     *
     * @param x x coordinate (col)
     * @param y y coordinate (row)
     * @return int array with [row, col] or null if out of bounds
     */
    @Nullable
    private int[] screenToGrid(float x, float y) {
        if (worldState == null) return null;
        // account for pan and scale
        float unscaledX = (x - panX) / scaleFactor;
        float unscaledY = (y - panY) / scaleFactor;

        // compute relative to origin
        float relX = unscaledX - originX;
        float relY = unscaledY - originY;

        // compute grid coordinates
        int c = (int) Math.floor(relX / tileWidth);
        int r = (int) Math.floor(relY / tileHeight);

        // check bounds -> if out of bounds return null
        if (r < 0 || c < 0 || r >= worldState.getRows() || c >= worldState.getCols()) return null;

        // return grid coordinates
        return new int[]{r, c};
    }

    /**
     * Get the bitmap for the given tile ID, loading it if necessary.
     *
     * @param tileId The tile identifier
     * @param width Width of the bitmap in pixels
     * @param height Height of the bitmap in pixels
     * @return The loaded bitmap
     */
    private Bitmap resolveBitmap(@NonNull String tileId, int width, int height) {
        // ensure that user has set the bitmap loader
        if (bitmapLoader == null) throw new IllegalStateException("BitmapLoader not set. This must be called after setTileAssets().");

        // get the asset for the given tile ID
        TileAsset asset = assetIndex.get(tileId);
        if (asset == null) throw new IllegalArgumentException("Tile ID '" + tileId + "' not found in asset index.");

        // load and return the bitmap (lazy-loaded and cached inside the loader)
        return bitmapLoader.getBitmap(asset, width, height);
    }

    /**
     * Resolve the bitmap for the base tile of the world.
     * @return The base tile bitmap, or null if not found
     */
    @Nullable
    private Bitmap resolveBaseBitmap() {
        return resolveBitmap(worldState.getBaseTileId(), (int) tileWidth, (int) tileHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        // get action
        final int action = event.getActionMasked();

        // detect if a finger is lifted while dragging
        int pointerIndex = -1;
        if (action == MotionEvent.ACTION_POINTER_UP) {
            pointerIndex = event.getActionIndex(); // get index of lifting finger
        }

        // Calculate the center point of all fingers
        float sumX = 0, sumY = 0;
        int count = 0;
        for (int i = 0; i < event.getPointerCount(); i++) {
            // skip lifting finger
            if (i == pointerIndex) continue;

            // get position of finger (x,y) for all fingers
            sumX += event.getX(i);
            sumY += event.getY(i);
            count++;
        }

        // compute centroid of zoom (focusX, focusY) as the mean position of all fingers
        final float focusX = count > 0 ? sumX / count : 0;
        final float focusY = count > 0 ? sumY / count : 0;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                // reset anchor to current focus point to avoid jumping around
                lastTouchX = focusX;
                lastTouchY = focusY;
                break;

            case MotionEvent.ACTION_MOVE:
                // Only pan if not zooming
                if (!scaleGestureDetector.isInProgress()) {
                    // compute delta
                    final float dx = focusX - lastTouchX;
                    final float dy = focusY - lastTouchY;

                    // apply delta to pan
                    panX += dx;
                    panY += dy;

                    // clamp panning to avoid going off screen
                    clampPan();

                    // force redraw
                    invalidate();
                }

                // Always update last touch
                lastTouchX = focusX;
                lastTouchY = focusY;
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * Clamp panX and panY to keep the map within view bounds.
     */
    private void clampPan() {
        if (worldState == null) return;

        float panMarginPx = PAN_MARGIN_DP * getResources().getDisplayMetrics().density;
        float gridW = worldState.getCols() * tileWidth * scaleFactor;
        float gridH = worldState.getRows() * tileHeight * scaleFactor;
        float viewW = getWidth();
        float viewH = getHeight();

        // horizontal clamping
        if (gridW < viewW) {
            // If content fits in view = recenter
            float targetScreenLeft = (viewW - gridW) / 2f;
            panX = targetScreenLeft - originX * scaleFactor;
        } else {
            // Content larger than view = limit panning
            float currentScreenLeft = originX * scaleFactor + panX;
            float minScreenLeft = viewW - gridW - panMarginPx;
            float maxScreenLeft = panMarginPx;
            if (currentScreenLeft < minScreenLeft) panX = minScreenLeft - originX * scaleFactor;
            if (currentScreenLeft > maxScreenLeft) panX = maxScreenLeft - originX * scaleFactor;
        }

        // vertical clamping
        float visibleH = Math.min(exclusionZoneTopY, viewH);
        if (gridH < visibleH) {
            // If content fits in visible height = recenter
            float targetScreenTop = (visibleH - gridH) / 2f;
            panY = targetScreenTop - originY * scaleFactor;
        } else {
            // Content larger than visible height = limit panning
            float currentScreenTop = originY * scaleFactor + panY;
            float minScreenTop = visibleH - gridH - panMarginPx;
            float maxScreenTop = panMarginPx;
            if (currentScreenTop < minScreenTop) panY = minScreenTop - originY * scaleFactor;
            if (currentScreenTop > maxScreenTop) panY = maxScreenTop - originY * scaleFactor;
        }
    }

    /**
     * Draw a bitmap centered and scaled within the target rectangle.
     *
     * @param canvas Canvas to draw on
     * @param bitmap Bitmap to draw
     * @param left left margin
     * @param top top margin
     * @param targetWidth effective width
     * @param targetHeight effective height
     * @param fitFactor scaling factor to apply within the target area
     */
    private void drawGridBitmap(Canvas canvas, Bitmap bitmap, float left, float top, float targetWidth, float targetHeight, float fitFactor) {
        if (bitmap == null) throw new IllegalArgumentException("Bitmap cannot be null");

        // compute scale to fit within target area
        float scale = Math.min(targetWidth / bitmap.getWidth(), targetHeight / bitmap.getHeight()) * fitFactor;
        float drawW = bitmap.getWidth() * scale;
        float drawH = bitmap.getHeight() * scale;

        // center the bitmap within the target area
        float dx = left + (targetWidth - drawW) / 2f;
        float dy = top + (targetHeight - drawH) / 2f;

        // draw the bitmap in the computed rectangle
        canvas.drawBitmap(bitmap, null, new RectF(dx, dy, dx + drawW, dy + drawH), null);
    }

    /**
     * Handle drag events for tile dropping.
     *
     * @param view the view receiving the drag event
     * @param event the drag event
     * @return true if the event was handled, false otherwise
     */
    private boolean handleDrag(View view, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED: // accept all drag events
                // Get the tile ID from localState
                if (event.getLocalState() instanceof String) {
                    draggingTileId = (String) event.getLocalState();
                }
                // make sure to change the alpha of the grid outline to indicate drop target
                gridOutline.setAlpha(50);
                invalidate();
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                // detect which cell we are hovering
                int[] pos = screenToGrid(event.getX(), event.getY());

                // if detected -> update state
                if (pos != null) {
                    if (draggingRow != pos[0] || draggingCol != pos[1]) {
                        draggingRow = pos[0];
                        draggingCol = pos[1];
                        invalidate();
                    }
                }
                // if not detected -> reset
                else {
                    if (draggingRow != -1 || draggingCol != -1) {
                        draggingRow = -1;
                        draggingCol = -1;
                        invalidate();
                    }
                }
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                draggingRow = -1;
                draggingCol = -1;
                invalidate();
                return true;

            case DragEvent.ACTION_DROP: // handle tile drop
                if (onTileDropListener == null) return false; // ignore if no listener is set

                // validate clip data -> we want to have exactly one item
                if (event.getClipData() == null || event.getClipData().getItemCount() != 1) return false;

                // get tile ID from clip data
                String tileId = String.valueOf(event.getClipData().getItemAt(0).getText());

                // check if drop location is inside the exclusion zone
                if (event.getY() >= exclusionZoneTopY) {
                    // cancel drop + reset state
                    resetDragState();
                    return false;
                }

                // convert screen coordinates to grid coordinates
                int[] grid = screenToGrid(event.getX(), event.getY());

                boolean handled = false;
                if (grid != null) {
                    onTileDropListener.onTileDropped(grid[0], grid[1], tileId);
                    handled = true;
                }

                resetDragState();
                return handled;

            case DragEvent.ACTION_DRAG_ENDED:
                resetDragState();
                return true;

            default:
                return true;
        }
    }

    private void resetDragState() {
        draggingTileId = null;
        draggingRow = -1;
        draggingCol = -1;
        gridOutline.setAlpha(0);
        invalidate();
    }
}
