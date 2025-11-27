package ch.inf.usi.mindbricks.ui.nav.shop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // ADDED

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // ADDED
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.databinding.FragmentShopBinding;
import ch.inf.usi.mindbricks.util.ProfileViewModel; // ADDED

// CHANGED: Implement the listener interface
public class ShopFragment extends Fragment implements ShopItemAdapter.OnItemBuyClickListener {

    private FragmentShopBinding binding;
    // ADDED: Declare the ViewModel
    private ProfileViewModel profileViewModel;
    private List<ShopItem> items; // ADDED: Made items a class member

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // ADDED: Initialize the ViewModel
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        binding = FragmentShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ADDED: Observe the coin balance and update the UI
        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                // Use view binding to set the text. Assumes ID is `shop_coin_balance`
                binding.coinBalanceText.setText(String.valueOf(balance));
            }
        });

        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        RecyclerView shopRecyclerView1 = binding.itemsRecyclerView1;
        RecyclerView shopRecyclerView2 = binding.itemsRecyclerView2;

        // Sample data for the shop
        items = new ArrayList<>();
        items.add(new ShopItem("Cool Brick", 1, R.drawable.ic_home_black_24dp));
        items.add(new ShopItem("Fancy Brick", 2, R.drawable.ic_dashboard_black_24dp));
        items.add(new ShopItem("Golden Brick", 3, R.drawable.ic_notifications_black_24dp));
        items.add(new ShopItem("Basic Brick", 4, R.drawable.ic_coin_icon));
        items.add(new ShopItem("Another Brick", 5, R.drawable.ic_home_black_24dp));

        // Create an instance of the adapter and pass 'this' as the listener
        ShopItemAdapter adapter1 = new ShopItemAdapter(items, this);
        ShopItemAdapter adapter2 = new ShopItemAdapter(items, this);

        // Attach the adapter to the RecyclerView
        shopRecyclerView1.setAdapter(adapter1);
        shopRecyclerView2.setAdapter(adapter2);
    }

    @Override
    public void onItemBuyClick(ShopItem item) {
        // Use the ViewModel to attempt to spend coins
        boolean purchaseSuccessful = profileViewModel.spendCoins(item.getPrice());

        // Show feedback to the user
        if (purchaseSuccessful) {
            Toast.makeText(getContext(), "You purchased " + item.getName() + "!", Toast.LENGTH_SHORT).show();
            // Here you could add logic to add the item to a user's inventory
        } else {
            Toast.makeText(getContext(), "Not enough coins to buy " + item.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
