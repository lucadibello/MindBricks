package ch.inf.usi.mindbricks.ui.nav.shop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.databinding.FragmentShopBinding;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class ShopFragment extends Fragment implements ShopItemAdapter.OnItemBuyClickListener {

    private FragmentShopBinding binding;
    private ProfileViewModel profileViewModel;
    private PreferencesManager prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        prefs = new PreferencesManager(requireContext());
        binding = FragmentShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                binding.coinBalanceText.setText(String.valueOf(balance));
            }
        });

        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        RecyclerView shopRecyclerView1 = binding.itemsRecyclerView1;
        RecyclerView shopRecyclerView2 = binding.itemsRecyclerView2;

        shopRecyclerView1.setScrollBarFadeDuration(0);
        shopRecyclerView2.setScrollBarFadeDuration(0);

        List<ShopItem> items = new ArrayList<>();
        items.add(new ShopItem("avatar_cool_1", "name1", 1, R.drawable.ic_avatar_placeholder));
        items.add(new ShopItem("avatar_pro_2", "name2", 2, R.drawable.ic_avatar_placeholder));
        items.add(new ShopItem("theme_dark_3", "name3", 3, R.drawable.ic_avatar_placeholder));
        items.add(new ShopItem("golden_brick", "name4", 10, R.drawable.ic_coin_icon));
        items.add(new ShopItem("another_brick", "name5", 5, R.drawable.ic_home_black_24dp));

        ShopItemAdapter adapter1 = new ShopItemAdapter(items, this);
        ShopItemAdapter adapter2 = new ShopItemAdapter(items, this);

        shopRecyclerView1.setAdapter(adapter1);
        shopRecyclerView2.setAdapter(adapter2);
    }

    @Override
    public void onItemBuyClick(ShopItem item) {
        if (prefs.getPurchasedItemIds().contains(item.id())) {
            Toast.makeText(getContext(), "You already own this item!", Toast.LENGTH_SHORT).show();
            return;
        }


        // Access the public coins LiveData field
        Integer currentCoins = profileViewModel.coins.getValue();

        if (currentCoins == null || currentCoins < item.price()) {
            Toast.makeText(getContext(), "Not enough coins to buy " + item.name(), Toast.LENGTH_SHORT).show();
            return;
        }

        // help source: https://stackoverflow.com/questions/14398392/ok-button-for-alert-dialog-on-android?

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Purchase")
                .setMessage("Buy \"" + item.name() + "\" for " + item.price() + " coins?")
                .setPositiveButton("Buy", (dialog, which) -> {
                    completePurchase(item);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completePurchase(ShopItem item) {
        boolean purchaseSuccessful = profileViewModel.spendCoins(item.price());

        if (purchaseSuccessful) {
            prefs.purchaseItem(item.id());
            Toast.makeText(getContext(), "You purchased " + item.name() + "!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
