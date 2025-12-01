package ch.inf.usi.mindbricks.ui.nav.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ch.inf.usi.mindbricks.databinding.ItemPurchasedAssetBinding;
import ch.inf.usi.mindbricks.ui.nav.shop.ItemViewHolder;

/**
 * An adapter for the RecyclerView in the ProfileFragment.
 * It takes a list of PurchasedItem objects and binds them to their layout for display.
 */
public class PurchasedItemsAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    /**
     * The list of items that the adapter will display.
     */
    private final List<PurchasedItem> items;

    /**
     * Constructor for the adapter.
     *
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
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemPurchasedAssetBinding binding = ItemPurchasedAssetBinding.inflate(inflater, parent, false);
        // pass binding to the ViewHolder
        return new ItemViewHolder(binding);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the ViewHolder to reflect the item at the given position.
     */
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        // Get the data model for the current row
        PurchasedItem currentItem = items.get(position);
        // Bind the data to the ViewHolder
        holder.bind(currentItem);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }
}
