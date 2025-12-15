package ch.inf.usi.mindbricks.ui.nav.shop;

import android.content.ClipData;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.databinding.FragmentShopBinding;
import ch.inf.usi.mindbricks.game.TileAsset;
import ch.inf.usi.mindbricks.game.TileAssetLoader;
import ch.inf.usi.mindbricks.game.TileBitmapLoader;
import ch.inf.usi.mindbricks.game.TileGameViewModel;
import ch.inf.usi.mindbricks.game.TileType;
import ch.inf.usi.mindbricks.util.ProfileViewModel;
import ch.inf.usi.mindbricks.util.SoundPlayer;
import ch.inf.usi.mindbricks.util.VibrationHelper;

public class ShopFragment extends Fragment implements ShopItemAdapter.OnItemBuyClickListener {

    /**
     * View binding for the fragment layout.
     */
    private FragmentShopBinding binding;

    /**
     * ViewModel for managing user profile data. Needed for coin balance.
     */
    private ProfileViewModel profileViewModel;

    /**
     * ViewModel for managing tile game state. Needed for inventory and world state.
     */
    private TileGameViewModel tileGameViewModel;

    /**
     * Loader for tile assets.
     */
    private TileAssetLoader assetLoader;

    /**
     * Loader for tile bitmaps.
     */
    private TileBitmapLoader bitmapLoader;

    /**
     * Adapter for the inventory RecyclerView.
     */
    private InventoryAdapter inventoryAdapter;

    /**
     * The index of available tile assets by their ID.
     */
    private final Map<String, TileAsset> assetIndex = new HashMap<>();

    /**
     * The currently selected tile ID for placement.
     */
    private String selectedTileId;

    /**
     * Enum that defines purchase quantities based on tile type.
     */
    private enum PurchaseQuantity {
        /**
         * Single tile purchase (1 unit, default)
         */
        SINGLE(1),
        /**
         * Multiple tile purchase (3 units, for decorations)
         */
        MULTIPLE(3),

        /**
         * Bulk tile purchase (10 units, for terrain, roads, water)
         */
        BULK(10);

        /**
         * Quantity associated with the purchase type
         */
        private final int quantity;

        /**
         * Constructor for PurchaseQuantity enum.
         * @param qty Quantity of tiles to purchase
         */
        PurchaseQuantity(int qty) {
            this.quantity = qty;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // create view models
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        tileGameViewModel = new ViewModelProvider(requireActivity()).get(TileGameViewModel.class);

        // create loaders
        assetLoader = new TileAssetLoader(requireContext());
        bitmapLoader = new TileBitmapLoader(requireContext());

        // inflate view
        binding = FragmentShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // observe profile coin balance
        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                binding.coinBalanceText.setText(String.valueOf(balance));
            }
        });

        // load every available asset in the game
        List<TileAsset> assets = assetLoader.loadAvailableAssets();
        for (TileAsset asset : assets) {
            assetIndex.put(asset.id(), asset);
        }

        // setup UI
        setupBottomSheet();
        setupInventoryRecycler();
        setupCityView();
        buildShopSections(assets);

        // setup observers
        setupObservers();
    }

    /**
     * Setup the bottom sheet behavior for the shop UI.
     */
    private void setupBottomSheet() {
        // create bottom sheet
        BottomSheetBehavior<MaterialCardView> bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setPeekHeight(dpToPx(250));
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setExpandedOffset(dpToPx(100)); // avoid hiding entire view

        // Add callback to update exclusion zone when bottom sheet moves
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                updateExclusionZone();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                updateExclusionZone();
            }
        });

        // Initial update
        binding.bottomSheet.post(this::updateExclusionZone);
    }

    /**
     * Setup the inventory RecyclerView and its adapter.
     * SOURCE: <a href="https://developer.android.com/develop/ui/views/layout/recyclerview">RecyclerView<a>
     */
    private void setupInventoryRecycler() {
        RecyclerView inventoryRecycler = binding.inventoryRecyclerView;
        inventoryRecycler.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));

        // create inventory adapter with interaction listener
        inventoryAdapter = new InventoryAdapter(bitmapLoader, new InventoryAdapter.OnInventoryInteractionListener() {
            @Override
            public void onTileSelected(TileAsset asset) {
                selectedTileId = asset.id();
                inventoryAdapter.setSelectedTileId(selectedTileId);
                Toast.makeText(requireContext(), "Selected " + asset.displayName() + " for placement.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTileDragRequested(TileAsset asset, View view) {
                selectedTileId = asset.id();
                inventoryAdapter.setSelectedTileId(selectedTileId);
                startDragForAsset(asset, view);
            }
        });

        // set adapter
        inventoryRecycler.setAdapter(inventoryAdapter);
    }

    /**
     * Setup observers for inventory and world state changes.
     */
    private void setupObservers() {
        // observe inventory changes and inventory list in UI
        tileGameViewModel.getInventory().observe(getViewLifecycleOwner(), inventory -> {
            List<InventoryAdapter.InventoryEntry> entries = new ArrayList<>();
            if (inventory != null) {
                for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                    TileAsset asset = assetIndex.get(entry.getKey());
                    if (asset == null) continue;

                    // add inventory entry to the list of items
                    entries.add(new InventoryAdapter.InventoryEntry(asset, entry.getValue()));
                }
            }

            // update UI
            binding.inventoryEmptyText.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
            inventoryAdapter.submitList(entries);
        });

        // observe world state changes and update city view
        tileGameViewModel.getWorldState().observe(getViewLifecycleOwner(), state -> {
            binding.cityView.setWorldState(state);
        });
    }

    /**
     * Build the shop sections dynamically based on available tile assets.
     *
     * @param assets List of available tile assets
     */
    private void buildShopSections(List<TileAsset> assets) {
        if (getContext() == null) return;

        // clear existing sections
        binding.shopSectionsContainer.removeAllViews();

        // group assets by category
        Map<String, List<TileAsset>> grouped = new LinkedHashMap<>();

        // Pre-defined shop sections: Terrain, Infrastructure, Nature, Resources, Buildings, Decorations
        String[] order = new String[]{
                "Terrain - Ground",
                "Terrain - Water",
                "Infrastructure - Roads",
                "Infrastructure - Terrain",
                "Nature - Trees",
                "Nature - Bushes",
                "Nature - Rocks",
                "Resources - Gold",
                "Resources - Wood",
                "Resources - Tools",
                "Buildings",
                "Decorations",
                "Props"
        };

        // initialize empty lists for each category in order
        for (String key : order) grouped.put(key, new ArrayList<>());

        // for each asset, determine its category and add to the appropriate list
        for (TileAsset asset : assets) {
            String category = categoryOf(asset);
            if (!grouped.containsKey(category)) {
                grouped.put(category, new ArrayList<>());
            }
            Objects.requireNonNull(grouped.get(category)).add(asset);
        }

        // create UI sections for each category
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (Map.Entry<String, List<TileAsset>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            View sectionView = inflater.inflate(R.layout.shop_section, binding.shopSectionsContainer, false);
            TextView title = sectionView.findViewById(R.id.section_title);
            RecyclerView recyclerView = sectionView.findViewById(R.id.section_recycler);
            title.setText(entry.getKey());
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
            recyclerView.setAdapter(new ShopItemAdapter(entry.getValue(), bitmapLoader, this));
            binding.shopSectionsContainer.addView(sectionView);
        }
    }

    /**
     * Determine the category of a tile asset for grouping in the shop.
     *
     * @param asset Tile asset
     * @return Category name
     */
    private String categoryOf(TileAsset asset) {
        String path = asset.assetPath().toLowerCase(Locale.US);

        // simple heuristic based on type or path keywords as fallback
        if (asset.type() == TileType.ROAD) return "Infrastructure - Roads";
        if (asset.type() == TileType.WATER || path.contains("water")) return "Terrain - Water";
        if (path.contains("tree")) return "Nature - Trees";
        if (path.contains("bush")) return "Nature - Bushes";
        if (path.contains("rock")) return "Nature - Rocks";
        if (asset.type() == TileType.BUILDING) return "Buildings";
        if (path.contains("gold")) return "Resources - Gold";
        if (path.contains("wood")) return "Resources - Wood";
        if (path.contains("tool")) return "Resources - Tools";
        if (path.contains("prop")) return "Props";
        if (asset.type() == TileType.TERRAIN) return "Terrain - Ground";
        if (asset.type() == TileType.DECORATION) return "Decorations";

        // general fallback
        return "Miscellaneous";
    }

    /**
     * Setup the city view for tile placement.
     */
    private void setupCityView() {
        binding.cityView.setTileAssets(assetIndex, bitmapLoader);
        binding.cityView.setOnTileDropListener(this::handleTilePlacement);
    }

    /**
     * Handle the placement of a tile in the city view.
     *
     * @param row the row index where the tile is to be placed
     * @param col the column index where the tile is to be placed
     * @param tileId the identifier of the tile being placed
     */
    private void handleTilePlacement(int row, int col, String tileId) {
        if (TextUtils.isEmpty(tileId)) return;
        Map<String, Integer> inventory = tileGameViewModel.getInventory().getValue();
        int quantity = inventory != null ? inventory.getOrDefault(tileId, 0) : 0;
        if (quantity <= 0) {
            Toast.makeText(requireContext(), "You don't have that tile in your inventory.", Toast.LENGTH_SHORT).show();
            return;
        }

        // get the tile asset
        TileAsset asset = assetIndex.get(tileId);
        if (asset == null) {
            Toast.makeText(requireContext(), "Unknown tile type.", Toast.LENGTH_SHORT).show();
            return;
        }

        // get the size of the tile to place
        int[] size = asset.getSize();
        int height = size[0];
        int width = size[1];

        // check if placement is within bounds
        if (!tileGameViewModel.isWithinBounds(row, col, height, width)) {
            Toast.makeText(requireContext(), "Can't place tile there - outside grid bounds.", Toast.LENGTH_SHORT).show();
            return;
        }

        // check for collisions
        boolean hasCollisions = tileGameViewModel.hasCollisions(row, col, height, width);

        if (hasCollisions) {
            // show dialog asking user if they want to replace existing buildings
            int destructionCount = tileGameViewModel.getDestructionCount(row, col, height, width);
            showReplacementDialog(row, col, tileId, height, width, destructionCount);
        } else {
            // no collisions, place normally
            attemptPlacement(row, col, tileId, height, width);
        }
    }

    /**
     * Show a dialog asking the user to replace existing buildings
     *
     * @param row Row index
     * @param col Column index
     * @param tileId Tile ID to place
     * @param height Height of placement
     * @param width Width of placement
     * @param destructionCount Number of buildings that will be destroyed
     */
    private void showReplacementDialog(int row, int col, String tileId, int height, int width, int destructionCount) {
        String message = destructionCount == 1
                ? "This will destroy 1 existing building. Continue?"
                : "This will destroy " + destructionCount + " existing buildings. Continue?";

        new AlertDialog.Builder(requireContext())
                .setTitle("Replace Buildings?")
                .setMessage(message)
                .setPositiveButton("Replace", (dialog, which) -> {
                    // remove items from map
                    attemptPlacementWithReplacement(row, col, tileId, height, width);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Attempt to place a tile normally (no collisions)
     *
     * @param row Row index
     * @param col Column index
     * @param tileId Tile ID
     * @param height Height of placement
     * @param width Width of placement
     */
    private void attemptPlacement(int row, int col, String tileId, int height, int width) {
        boolean placed = tileGameViewModel.placeTile(row, col, tileId, height, width);
        if (!placed) {
            Toast.makeText(requireContext(), "Couldn't place tile there.", Toast.LENGTH_SHORT).show();
            return;
        }

        // consume from inventory
        tileGameViewModel.consumeFromInventory(tileId);

        // notify user
        VibrationHelper.vibrate(requireContext(), VibrationHelper.VibrationType.PLACE_TILE);
        SoundPlayer.playSound(requireContext(), R.raw.purchase);
    }

    /**
     * Attempt to place a tile with replacement
     *
     * @param row Row index
     * @param col Column index
     * @param tileId Tile ID
     * @param height Height of placement
     * @param width Width of placement
     */
    private void attemptPlacementWithReplacement(int row, int col, String tileId, int height, int width) {
        boolean placed = tileGameViewModel.placeTileWithReplacement(row, col, tileId, height, width);
        if (!placed) {
            Toast.makeText(requireContext(), "Couldn't place tile there.", Toast.LENGTH_SHORT).show();
            return;
        }

        // consume from inventory
        tileGameViewModel.consumeFromInventory(tileId);

        // notify user with feedback
        Toast.makeText(requireContext(), "Tile placed! Buildings destroyed.", Toast.LENGTH_SHORT).show();
        SoundPlayer.playSound(getContext(), R.raw.purchase);

        // vibrate to indicate destruction
        VibrationHelper.vibrate(requireContext(), VibrationHelper.VibrationType.DESTROY_TILE);
    }

    /**
     * Handle the event when a user clicks to buy an item in the shop.
     *
     * @param item The tile asset being purchased
     */
    @Override
    public void onItemBuyClick(TileAsset item) {
        // get current coin balance
        Integer currentCoins = profileViewModel.coins.getValue();

        // check if enough coins
        if (currentCoins == null || currentCoins < item.price()) {
            Toast.makeText(getContext(), "Not enough coins to buy " + item.displayName(), Toast.LENGTH_SHORT).show();
            return;
        }

        // show confirmation dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Purchase")
                .setMessage("Buy \"" + item.displayName() + "\" for " + item.price() + " coins?")
                .setPositiveButton("Buy", (dialog, which) -> {
                    // complete the purchase
                    completePurchase(item);
                    SoundPlayer.playSound(getContext(), R.raw.purchase);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Complete the purchase of a tile asset.
     *
     * @param item The tile asset being purchased
     */
    private void completePurchase(TileAsset item) {
        // pay for the item
        boolean purchaseSuccessful = profileViewModel.spendCoins(item.price());

        // NOTE: this should never fail as we already checked for sufficient balance
        if (purchaseSuccessful) {
            int quantity = purchaseQuantity(item);
            tileGameViewModel.addToInventory(item.id(), quantity);
            Toast.makeText(getContext(), "You purchased " + quantity + " x " + item.displayName() + "!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start a drag-and-drop operation for the given tile asset.
     *
     * @param asset The tile asset being dragged
     * @param view  The view representing the tile asset
     */
    private void startDragForAsset(TileAsset asset, View view) {
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
        view.startDragAndDrop(ClipData.newPlainText("tileId", asset.id()), shadow, asset.id(), 0);
    }

    /**
     * Convert density-independent pixels (dp) to pixels (px) -> needed for setting bottom sheet peek height.
     *
     * @param dp Value in dp
     * @return Value in px
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Determine the quantity of tiles to add to inventory upon purchase, based on tile type.
     *
     * @param asset Tile asset being purchased
     * @return Quantity to add to inventory
     */
    private int purchaseQuantity(TileAsset asset) {
        if (asset.type() == TileType.ROAD || asset.type() == TileType.TERRAIN || asset.type() == TileType.WATER) {
            return PurchaseQuantity.BULK.quantity;
        }
        if (asset.type() == TileType.DECORATION) {
            return PurchaseQuantity.MULTIPLE.quantity;
        }
        return PurchaseQuantity.SINGLE.quantity;
    }

    /**
     * Flag to track if the initial map positioning has been done.
     */
    private boolean initialPositioningDone = false;

    /**
     * Update the exclusion zone in the city view based on the current bottom sheet position.
     */
    private void updateExclusionZone() {
        if (binding == null) return;

        // Get the bottom sheet Y coordinate
        int[] location = new int[2];
        binding.bottomSheet.getLocationOnScreen(location);
        float bottomSheetTopY = location[1];

        // Get city Y coordinate
        int[] cityLocation = new int[2];
        binding.cityView.getLocationOnScreen(cityLocation);
        float cityViewTopY = cityLocation[1];

        // Compute deltaY
        float relativeTopY = bottomSheetTopY - cityViewTopY;

        // Update exclusion zone
        binding.cityView.setExclusionZoneTopY(relativeTopY);

        // Perform initial centering if needed
        if (!initialPositioningDone && relativeTopY > 0) {
            binding.cityView.recenterMap();
            initialPositioningDone = true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
