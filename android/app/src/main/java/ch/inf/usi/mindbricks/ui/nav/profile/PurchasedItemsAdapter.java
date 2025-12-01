package ch.inf.usi.mindbricks.ui.nav.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ch.inf.usi.mindbricks.databinding.ItemPurchasedAssetBinding;

/**
 * An adapter for the RecyclerView in the ProfileFragment.
 * It takes a list of PurchasedItem objects and binds them to the
 * item_purchased_asset.xml layout for display.
 */
public class PurchasedItemsAdapter extends RecyclerView.Adapter<PurchasedItemsAdapter.ItemViewHolder> {

    // The list of items that the adapter will display.
    private final List<PurchasedItem> items;

    /**
     * Constructor for the adapter.
     * @param items The list of PurchasedItem objects to be displayed.
     */
    public PurchasedItemsAdapter(List<PurchasedItem> items) {
        this.items = items;
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     * This is where we inflate the item's layout.
     */
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout using ViewBinding for type-safety and convenience.
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemPurchasedAssetBinding binding = ItemPurchasedAssetBinding.inflate(inflater, parent, false);
        return new ItemViewHolder(binding);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the ViewHolder to reflect the item at the given position.
     */
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        // Get the data model (PurchasedItem) for the current row.
        PurchasedItem currentItem = items.get(position);
        // Bind the data to the ViewHolder.
        holder.bind(currentItem);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     */
    @Override
    public int getItemCount() {
        // The adapter needs to know the total size of the list.
        return items.size();
    }

    /**
     * The ViewHolder class. It describes an item view and metadata about its place within the RecyclerView.
     * This is a static inner class for better performance and encapsulation.
     */
    static class ItemViewHolder extends RecyclerView.ViewHolder {

        // Using ViewBinding provides direct and null-safe access to the views
        // defined in item_purchased_asset.xml (e.g., purchasedItemName, purchasedItemImage).
        private final ItemPurchasedAssetBinding binding;

        public ItemViewHolder(ItemPurchasedAssetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * A helper method to populate the views in the ViewHolder with data from a PurchasedItem object.
         * @param item The PurchasedItem object containing the data for this view.
         */
        public void bind(PurchasedItem item) {
            binding.purchasedItemName.setText(item.getName());
            binding.purchasedItemImage.setImageResource(item.getImageResId());
        }
    }
}
