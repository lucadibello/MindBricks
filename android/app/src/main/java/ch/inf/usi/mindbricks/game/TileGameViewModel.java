package ch.inf.usi.mindbricks.game;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Map;
import java.util.Set;

public class TileGameViewModel extends AndroidViewModel {

    /**
     * Default number of rows and columns for the tile world
     */
    private static final int DEFAULT_ROWS = 40;

    /**
     * Default number of columns for the tile world
     */
    private static final int DEFAULT_COLS = 40;

    /**
     * Default base tile for the world (grass tile, bottom left in the tileset)
     */
    private static final String DEFAULT_BASE_TILE = "tiles/terrain/tiles/Tileset_Ground.png#96";

    /**
     * Manages the inventory of tiles available to the player.
     */
    private final TileInventoryManager inventoryManager;

    /**
     * Repository for loading and saving the tile world state.
     */
    private final TileWorldRepository worldRepository;

    /**
     * LiveData holding the current inventory of tiles.
     */
    private final MutableLiveData<Map<String, Integer>> inventory = new MutableLiveData<>();

    /**
     * LiveData holding the current state of the tile world.
     */
    private final MutableLiveData<TileWorldState> worldState = new MutableLiveData<>();

    /**
     * Constructor method.
     * @param application Application context
     */
    public TileGameViewModel(@NonNull Application application) {
        super(application);

        // initialize managers and repositories
        inventoryManager = new TileInventoryManager(application.getApplicationContext());
        worldRepository = new TileWorldRepository(application.getApplicationContext());

        // load initial data
        inventory.setValue(inventoryManager.loadInventory());
        worldState.setValue(worldRepository.loadWorldState(DEFAULT_ROWS, DEFAULT_COLS, DEFAULT_BASE_TILE));
    }

    /**
     * Get the current inventory of tiles.
     * @return LiveData containing a map of tile IDs to their counts
     */
    public LiveData<Map<String, Integer>> getInventory() {
        return inventory;
    }

    /**
     * Get the current state of the tile world.
     * @return LiveData containing the tile world state
     */
    public LiveData<TileWorldState> getWorldState() {
        return worldState;
    }

    /**
     * Add tiles to the inventory.
     * @param tileId Tile identifier
     * @param amount Number of tiles to add
     */
    public void addToInventory(String tileId, int amount) {
        if (tileId == null || amount <= 0) return;

        // get inventory and update count
        Map<String, Integer> current = inventory.getValue();
        assert current != null; // should never be null

        // get count from inventory
        int existing = current.getOrDefault(tileId, 0);

        // update inventory + save inside preferences
        current.put(tileId, existing + amount);
        inventory.setValue(current);
        inventoryManager.addToInventory(tileId, amount);
    }

    /**
     * Consume a tile from the inventory.
     * @param tileId Tile identifier
     */
    public void consumeFromInventory(String tileId) {
        Map<String, Integer> current =  inventory.getValue();
        assert current != null; // should never be null

        // get count from inventory
        int existing = current.getOrDefault(tileId, 0);
        // NOTE: this should never happen -> bug in the UI
        if (existing <= 0) throw new IllegalStateException("Trying to consume tile not in inventory: " + tileId);

        // compute new count
        if (existing == 1) current.remove(tileId);
        else current.put(tileId, existing - 1);

        // update inventory
        inventory.setValue(current);
        boolean status = inventoryManager.consumeFromInventory(tileId);
        // NOTE: this should never happen -> bug in the UI
        if (!status) throw new IllegalStateException("Failed to consume tile from inventory: " + tileId);
    }

    /**
     * Place a tile in the world at the given position if possible.
     *
     * @param row Row index
     * @param col Column index
     * @param tileId Tile identifier
     * @param height Number of rows the tile occupies
     * @param width Number of columns the tile occupies
     * @return True if the tile was placed successfully, false otherwise
     */
    public boolean placeTile(int row, int col, String tileId, int height, int width) {
        // get state
        TileWorldState state = worldState.getValue();
        if (state == null || tileId == null) return false;

        // ensure we can place the tile at (row,col) with given dimensions
        if (!state.canPlace(row, col, height, width)) return false;

        // generate new world state with the tile placed
        TileWorldState updated = state.withPlacement(row, col, tileId, height, width);
        worldState.setValue(updated);
        worldRepository.saveWorld(updated);

        return true;
    }

    /**
     * Check if placing a tile would cause collisions with existing placements
     *
     * @param row Row index
     * @param col Column index
     * @param height Number of rows the tile occupies
     * @param width Number of columns the tile occupies
     * @return True if there are collisions, false otherwise (or if out of bounds)
     */
    public boolean hasCollisions(int row, int col, int height, int width) {
        TileWorldState state = worldState.getValue();
        if (state == null) return false;
        return state.hasCollisions(row, col, height, width);
    }

    /**
     * Check if placement is within bounds
     *
     * @param row Row index
     * @param col Column index
     * @param height Number of rows the tile occupies
     * @param width Number of columns the tile occupies
     * @return True if within bounds, false otherwise
     */
    public boolean isWithinBounds(int row, int col, int height, int width) {
        TileWorldState state = worldState.getValue();
        if (state == null) return false;
        return row >= 0 && col >= 0 &&
               row + height <= state.getRows() &&
               col + width <= state.getCols();
    }

    /**
     * Get the number of buildings that would be destroyed by placing a tile
     *
     * @param row Row index
     * @param col Column index
     * @param height Number of rows the tile occupies
     * @param width Number of columns the tile occupies
     * @return Number of unique buildings that would be destroyed
     */
    public int getDestructionCount(int row, int col, int height, int width) {
        TileWorldState state = worldState.getValue();
        if (state == null) return 0;
        return state.getOverlappingPlacements(row, col, height, width).size();
    }

    /**
     * Place a tile by replacing/destroying existing placements
     *
     * @param row Row index
     * @param col Column index
     * @param tileId Tile identifier
     * @param height Number of rows the tile occupies
     * @param width Number of columns the tile occupies
     * @return True if the tile was placed successfully, false otherwise
     */
    public boolean placeTileWithReplacement(int row, int col, String tileId, int height, int width) {
        TileWorldState state = worldState.getValue();
        if (state == null || tileId == null) return false;

        // Check bounds
        if (!isWithinBounds(row, col, height, width)) return false;

        // Get overlapping placements
        Set<TilePlacement> overlapping = state.getOverlappingPlacements(row, col, height, width);

        // Create new state with replacements
        TileWorldState updated = state.withReplacedPlacement(overlapping, row, col, tileId, height, width);
        worldState.setValue(updated);
        worldRepository.saveWorld(updated);

        return true;
    }

    /**
     * Remove a placement from the world
     *
     * @param placement The placement to remove
     */
    public void removePlacement(TilePlacement placement) {
        TileWorldState state = worldState.getValue();
        if (state == null || placement == null) return;

        TileWorldState updated = state.withRemoval(placement);
        worldState.setValue(updated);
        worldRepository.saveWorld(updated);
    }
}
