package ch.inf.usi.mindbricks.ui.nav.shop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.databinding.FragmentShopBinding;

public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView shopRecyclerView1 = binding.itemsRecyclerView1;
        RecyclerView shopRecyclerView2 = binding.itemsRecyclerView2;

        // Sample data for the shop
        List<ShopItem> items = new ArrayList<>();
        items.add(new ShopItem("Cool Brick", 100, R.drawable.ic_home_black_24dp));
        items.add(new ShopItem("Fancy Brick", 250, R.drawable.ic_dashboard_black_24dp));
        items.add(new ShopItem("Golden Brick", 500, R.drawable.ic_notifications_black_24dp));
        items.add(new ShopItem("Basic Brick", 50, R.drawable.ic_coin_icon));
        items.add(new ShopItem("Another Brick", 120, R.drawable.ic_home_black_24dp));


        // Create an instance of adapter
        ShopItemAdapter adapter1 = new ShopItemAdapter(items);
        ShopItemAdapter adapter2 = new ShopItemAdapter(items);


        // Attach the adapter to the RecyclerView
        shopRecyclerView1.setAdapter(adapter1);
        shopRecyclerView2.setAdapter(adapter2);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
