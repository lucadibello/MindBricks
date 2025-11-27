package ch.inf.usi.mindbricks.ui.nav.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.Button; // No longer needed
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import ch.inf.usi.mindbricks.R;

public class ShopItemAdapter extends RecyclerView.Adapter<ShopItemAdapter.ShopItemViewHolder> {

    public interface OnItemBuyClickListener {
        void onItemBuyClick(ShopItem item);
    }

    private final List<ShopItem> items;
    private final OnItemBuyClickListener buyClickListener;

    public ShopItemAdapter(List<ShopItem> items, OnItemBuyClickListener listener) {
        this.items = items;
        this.buyClickListener = listener;
    }

    @NonNull
    @Override
    public ShopItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shop_item, parent, false);
        return new ShopItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopItemViewHolder holder, int position) {
        ShopItem currentItem = items.get(position);
        holder.itemName.setText(currentItem.getName());
        holder.itemPrice.setText(String.valueOf(currentItem.getPrice()));
        holder.itemImage.setImageResource(currentItem.getImageResourceId());

        // --- CHANGE IS HERE ---
        // Set the listener on the entire item view, not just a button.
        holder.itemView.setOnClickListener(v -> {
            if (buyClickListener != null) {
                buyClickListener.onItemBuyClick(currentItem);
            }
        });
        // --- END OF CHANGE ---
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ShopItemViewHolder extends RecyclerView.ViewHolder {
        public ImageView itemImage;
        public TextView itemName;
        public TextView itemPrice;
        // public Button buyButton; // No longer needed

        public ShopItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemName = itemView.findViewById(R.id.item_name);
            itemPrice = itemView.findViewById(R.id.item_price);
        }
    }
}
