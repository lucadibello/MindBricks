package ch.inf.usi.mindbricks.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TileWorldState {

    /**
     * Number of rows in the world map
     */
    private int rows;

    /**
     * Number of columns in the world map
     */
    private int cols;

    /**
     * Base tile ID for unoccupied tiles (e.g., ground tile)
     */
    private String baseTileId;

    /**
     * Map of placed tiles positioned by "row,col" -> tileId
     */
    private Map<String, String> placedTiles;

    /**
     * Map of tile placements positioned by "row,col" -> TilePlacement
     */
    private Map<String, TilePlacement> placements;

    // NOTE: Required for Gson
    @SuppressWarnings("unused")
    public TileWorldState() {}

    /**
     * Constructor method.
     *
     * @param rows Number of rows in the world
     * @param cols Number of columns in the world
     * @param baseTileId Base tile ID for unoccupied tiles
     */
    public TileWorldState(int rows, int cols, String baseTileId) {
        this(rows, cols, baseTileId, new HashMap<>(), new HashMap<>());
    }

    /**
     * Constructor method.
     *
     * @param rows Number of rows in the world
     * @param cols Number of columns in the world
     * @param baseTileId Base tile ID for unoccupied tiles
     * @param placedTiles Map of placed tiles positioned by "row,col" -> tileId
     * @param placements Map of tile placements positioned by "row,col" -> TilePlacement
     */
    public TileWorldState(int rows, int cols, String baseTileId, Map<String, String> placedTiles, Map<String, TilePlacement> placements) {
        this.rows = rows;
        this.cols = cols;
        this.baseTileId = baseTileId;
        this.placedTiles = placedTiles != null ? placedTiles : new HashMap<>();
        this.placements = placements != null ? placements : new HashMap<>();
    }

    /**
     * Get number of rows in the world
     * @return Number of rows
     */
    public int getRows() { return rows; }

    /**
     * Get number of columns in the world
     * @return Number of columns
     */
    public int getCols() { return cols; }

    /**
     * Get base tile ID for unoccupied tiles
     * @return Base tile ID
     */
    public String getBaseTileId() { return baseTileId; }

    /**
     * Get map of placed tiles
     * @return Map of placed tiles
     */
    public Map<String, String> getPlacedTiles() {
        if (placedTiles == null) placedTiles = new HashMap<>();
        return placedTiles;
    }

    /**
     * Get map of tile placements
     * @return Map of tile placements
     */
    public Map<String, TilePlacement> getPlacements() {
        if (placements == null) placements = new HashMap<>();
        return placements;
    }

    /**
     * Set base tile ID for unoccupied tiles
     * @param baseTileId Base tile ID
     */
    public void setBaseTileId(String baseTileId) { this.baseTileId = baseTileId; }

    /**
     * Get tile ID placed at the specified position
     * @param row Row index
     * @param col Column index
     * @return Tile ID at the position, or null if none
     */
    public String getPlacedTile(int row, int col) {
        if (placedTiles == null) return null;
        return placedTiles.get(key(row, col));
    }

    /**
     * Check if a tile can be placed at the specified position
     *
     * @param row Row index
     * @param col Column index
     * @param height Height of the tile
     * @param width Width of the tile
     * @return True if the tile can be placed, false otherwise
     */
    public boolean canPlace(int row, int col, int height, int width) {
        if (row < 0 || col < 0 || row + height > rows || col + width > cols) return false;
        Map<String, String> tiles = getPlacedTiles();
        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                if (tiles.containsKey(key(r, c))) return false;
            }
        }
        return true;
    }

    /**
     * Return a new TileWorldState with the specified tile placed at the given position
     *
     * @param row Row index
     * @param col Column index
     * @param tileId Tile ID to place
     * @param height Height of the tile
     * @param width Width of the tile
     * @return New TileWorldState with the tile placed
     */
    public TileWorldState withPlacement(int row, int col, String tileId, int height, int width) {
        // copy existing maps
        Map<String, String> tilesCopy = new HashMap<>(getPlacedTiles());
        Map<String, TilePlacement> placementCopy = new HashMap<>(getPlacements());

        // place the tile
        TilePlacement placement = new TilePlacement(tileId, width, height, row, col);
        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                tilesCopy.put(key(r, c), tileId);
                placementCopy.put(key(r, c), placement);
            }
        }

        // return new state
        return new TileWorldState(rows, cols, baseTileId, tilesCopy, placementCopy);
    }

    /**
     * Check if the specified position is the anchor of a tile placement
     * NOTE: anchor is the top-left position of a multi-tile placement
     *
     * @param row Row index
     * @param col Column index
     * @return True if the position is an anchor, false otherwise
     */
    public boolean isAnchor(int row, int col) {
        TilePlacement placement = getPlacements().get(key(row, col));
        return placement != null && placement.getAnchorRow() == row && placement.getAnchorCol() == col;
    }

    /**
     * Get the TilePlacement at the specified position
     *
     * @param row Row index
     * @param col Column index
     * @return TilePlacement at the position, or null if none
     */
    public TilePlacement getPlacementAt(int row, int col) {
        return getPlacements().get(key(row, col));
    }

    /**
     * Get all unique placements that would be overlapped by placing a tile at the given position
     *
     * @param row Row index
     * @param col Column index
     * @param height Height of the tile
     * @param width Width of the tile
     * @return Set of TilePlacement objects that overlap with the new placement
     */
    public Set<TilePlacement> getOverlappingPlacements(int row, int col, int height, int width) {
        Set<TilePlacement> overlapping = new HashSet<>();
        Map<String, TilePlacement> placements = getPlacements();

        // linear scan surroundings
        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                TilePlacement placement = placements.get(key(r, c));
                if (placement != null) {
                    overlapping.add(placement);
                }
            }
        }

        // return found conflicts
        return overlapping;
    }

    /**
     * Check if placing a tile would cause any collisions
     *
     * @param row Row index
     * @param col Column index
     * @param height Height of the tile
     * @param width Width of the tile
     * @return True if there are collisions, false otherwise
     */
    public boolean hasCollisions(int row, int col, int height, int width) {
        // Check bounds first
        if (row < 0 || col < 0 || row + height > rows || col + width > cols) {
            return false; // Out of bounds, not a collision
        }

        // Check for occupied cells
        return !getOverlappingPlacements(row, col, height, width).isEmpty();
    }

    /**
     * Create a new world state with specified placements removed and a new tile placed
     *
     * @param placementsToRemove Set of placements to remove
     * @param row Row index for new placement
     * @param col Column index for new placement
     * @param tileId Tile ID for new placement
     * @param height Height of new placement
     * @param width Width of new placement
     * @return New TileWorldState with removed conflicting items and new placement applied
     */
    public TileWorldState withReplacedPlacement(Set<TilePlacement> placementsToRemove,
                                                int row, int col, String tileId, int height, int width) {
        // Copy existing maps
        Map<String, String> tilesCopy = new HashMap<>(getPlacedTiles());
        Map<String, TilePlacement> placementCopy = new HashMap<>(getPlacements());

        // Remove all cells occupied by placements to remove
        for (TilePlacement toRemove : placementsToRemove) {
            for (int r = toRemove.getAnchorRow(); r < toRemove.getAnchorRow() + toRemove.getHeight(); r++) {
                for (int c = toRemove.getAnchorCol(); c < toRemove.getAnchorCol() + toRemove.getWidth(); c++) {
                    tilesCopy.remove(key(r, c));
                    placementCopy.remove(key(r, c));
                }
            }
        }

        // Place the new tile
        TilePlacement placement = new TilePlacement(tileId, width, height, row, col);
        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                tilesCopy.put(key(r, c), tileId);
                placementCopy.put(key(r, c), placement);
            }
        }

        // Return new state
        return new TileWorldState(rows, cols, baseTileId, tilesCopy, placementCopy);
    }

    /**
     * Return a new TileWorldState with the specified placement removed.
     *
     * @param placement The placement to remove
     * @return New TileWorldState with the placement removed
     */
    public TileWorldState withRemoval(TilePlacement placement) {
        if (placement == null) return this;

        // Copy existing maps
        Map<String, String> tilesCopy = new HashMap<>(getPlacedTiles());
        Map<String, TilePlacement> placementCopy = new HashMap<>(getPlacements());

        // Remove all cells occupied by the placement
        for (int r = placement.getAnchorRow(); r < placement.getAnchorRow() + placement.getHeight(); r++) {
            for (int c = placement.getAnchorCol(); c < placement.getAnchorCol() + placement.getWidth(); c++) {
                tilesCopy.remove(key(r, c));
                placementCopy.remove(key(r, c));
            }
        }

        // Return new state
        return new TileWorldState(rows, cols, baseTileId, tilesCopy, placementCopy);
    }

    /**
     * Generate a key for the specified row and column
     *
     * @param row Row index
     * @param col Column index
     * @return Key in the format "row,col"
     */
    public static String key(int row, int col) {
        return row + "," + col;
    }
}
