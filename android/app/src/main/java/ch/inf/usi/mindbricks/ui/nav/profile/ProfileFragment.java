package ch.inf.usi.mindbricks.ui.nav.profile;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.databinding.FragmentProfileBinding;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private static final String DICEBEAR_BASE_URL = "https://api.dicebear.com/9.x/pixel-art/png";
    private final List<PurchasedItem> allShopItems = new ArrayList<>();
    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private PreferencesManager prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        prefs = new PreferencesManager(requireContext());
        binding = FragmentProfileBinding.inflate(inflater, container, false);


        allShopItems.add(new PurchasedItem("avatar_cool_1", "Cool Blue Avatar", R.drawable.ic_avatar_placeholder));
        allShopItems.add(new PurchasedItem("avatar_pro_2", "Pro Red Avatar", R.drawable.ic_avatar_placeholder));
        allShopItems.add(new PurchasedItem("theme_dark_3", "Dark Theme", R.drawable.ic_chip_check));

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                binding.profileCoinBalance.setText(String.valueOf(balance));
            }
        });

        loadAndDisplayUserData();
        setupPurchasedItemsList();
    }

    private void loadAndDisplayUserData() {
        binding.profileUserName.setText(prefs.getUserName());
        String sprintLength = prefs.getUserSprintLengthMinutes();
        binding.profileSprintLength.setText(String.format("%s minutes", sprintLength));

        String avatarSeed = prefs.getUserAvatarSeed();
        if (avatarSeed != null && !avatarSeed.isEmpty()) {
            loadRandomizedProfilePicture(avatarSeed);
        }

        loadAndRenderTags();
    }

    private void loadRandomizedProfilePicture(String seed) {
        Uri avatarUri = Uri.parse(DICEBEAR_BASE_URL)
                .buildUpon()
                .appendQueryParameter("seed", seed)
                .build();

        Glide.with(this)
                .load(avatarUri)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .centerCrop()
                .into(binding.profileImageView);
    }

    private void loadAndRenderTags() {
        binding.profileTagsChipGroup.removeAllViews();
        List<Tag> tags = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getUserTagsJson());
            for (int i = 0; i < array.length(); i++) {
                Tag t = Tag.fromJson(array.getJSONObject(i));
                if (t != null) tags.add(t);
            }
        } catch (JSONException e) {
            // this should never happen
            throw new RuntimeException(e);
        }

        if (tags.isEmpty()) {
            binding.profileTagsEmptyState.setVisibility(View.VISIBLE);
            binding.profileTagsChipGroup.setVisibility(View.GONE);
        } else {
            binding.profileTagsEmptyState.setVisibility(View.GONE);
            binding.profileTagsChipGroup.setVisibility(View.VISIBLE);
            for (Tag tag : tags) {
                Chip chip = new Chip(requireContext());
                chip.setText(tag.title());
                chip.setChipBackgroundColor(ColorStateList.valueOf(tag.color()));
                binding.profileTagsChipGroup.addView(chip);
            }
        }
    }

    private void setupPurchasedItemsList() {
        // Get the set of IDs for items the user has purchased.
        Set<String> purchasedIds = prefs.getPurchasedItemIds();

        //  Filter the master list of all shop items to get only the ones the user owns.
        List<PurchasedItem> userOwnedItems = new ArrayList<>();
        for (PurchasedItem shopItem : allShopItems) {
            if (purchasedIds.contains(shopItem.id())) {
                userOwnedItems.add(shopItem);
            }
        }

        //  Check if the user's inventory is empty and update the UI visibility.
        if (userOwnedItems.isEmpty()) {
            // If empty, hide the list and show the "empty" text
            binding.purchasedItemsRecyclerView.setVisibility(View.GONE);
            binding.purchasedItemsEmptyState.setVisibility(View.VISIBLE);
        } else {
            // If not empty, show the list and hide the "empty" text.
            binding.purchasedItemsRecyclerView.setVisibility(View.VISIBLE);
            binding.purchasedItemsEmptyState.setVisibility(View.GONE);

            // Create and set the adapter for the RecyclerView.
            PurchasedItemsAdapter adapter = new PurchasedItemsAdapter(userOwnedItems);
            binding.purchasedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.purchasedItemsRecyclerView.setAdapter(adapter);

            //  To prevent the RecyclerView from being scrollable inside the NestedScrollView
            binding.purchasedItemsRecyclerView.setNestedScrollingEnabled(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
