package ch.inf.usi.mindbricks.ui.nav.shop.city;

/**
 * Listener interface for handling tile drop events on the city grid.
 */
public interface OnTileDropListener {
    /**
     * Callback method invoked when a tile is dropped on the city grid.
     *
     * @param row Row index where the tile was dropped
     * @param col Column index where the tile was dropped
     * @param tileId Identifier of the dropped tile
     */
    void onTileDropped(int row, int col, String tileId);
}
