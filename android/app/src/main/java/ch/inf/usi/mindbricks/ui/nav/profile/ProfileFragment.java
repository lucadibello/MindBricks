package ch.inf.usi.mindbricks.ui.nav.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.databinding.FragmentProfileBinding;
import ch.inf.usi.mindbricks.model.Tag;
import ch.inf.usi.mindbricks.ui.settings.SettingsActivity;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private static final String DICEBEAR_BASE_URL = "https://api.dicebear.com/9.x/pixel-art/png";
    private final List<PurchasedItem> allShopItems = new ArrayList<>();
    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private PreferencesManager prefs;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create the launcher for picking an image from the gallery
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            requireActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                            prefs.setUserAvatarUri(uri.toString());
                            loadLocalProfilePicture(uri);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(requireContext(), "Failed to save image.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        // create the launcher to ask he user for permission to access the gallery
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        Toast.makeText(requireContext(), "Permission is required to select a profile picture.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        prefs = new PreferencesManager(requireContext());
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        allShopItems.add(new PurchasedItem("building_1", "house1", R.drawable.house1));
        allShopItems.add(new PurchasedItem("building_2", "house2", R.drawable.house2));
        allShopItems.add(new PurchasedItem("building_3", "house3", R.drawable.ic_avatar_placeholder));
        allShopItems.add(new PurchasedItem("building_4", "house4", R.drawable.ic_coin_icon));
        allShopItems.add(new PurchasedItem("building_5", "house5", R.drawable.ic_home_black_24dp));

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

        binding.buttonSettings.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        binding.profileImageView.setOnClickListener(v -> handleImageClick());

        setupPurchasedItemsList();
    }

    private void handleImageClick() {
        // WHen profile image is clicked it asks for permission to read the gallery
        String permission = Manifest.permission.READ_MEDIA_IMAGES;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*"); // to open the ui to the user that shows the gallery
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndDisplayUserData();
    }

    private void loadAndDisplayUserData() {
        binding.profileUserName.setText(prefs.getUserName());

        // if AvatarUri is present use that, if not use the AvatarSeed
        String localAvatarUri = prefs.getUserAvatarUri();
        if (localAvatarUri != null && !localAvatarUri.isEmpty()) {
            loadLocalProfilePicture(Uri.parse(localAvatarUri));
        } else {
            String avatarSeed = prefs.getUserAvatarSeed();
            if (avatarSeed != null && !avatarSeed.isEmpty()) {
                loadRandomizedProfilePicture(avatarSeed);
            }
        }

        loadAndRenderTags();
    }

    private void loadLocalProfilePicture(Uri imageUri) {
        Glide.with(this)
                .load(imageUri)
                .apply(RequestOptions.circleCropTransform()) // to crop the image like the placeholder
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(binding.profileImageView);
    }

    private void loadRandomizedProfilePicture(String seed) {
        Uri avatarUri = Uri.parse(DICEBEAR_BASE_URL)
                .buildUpon()
                .appendQueryParameter("seed", seed)
                .build();

        Glide.with(this)
                .load(avatarUri)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(binding.profileImageView);
    }

    private void loadAndRenderTags() {
        binding.profileTagsChipGroup.removeAllViews();
        List<Tag> tags = prefs.getUserTags();

        if (tags.isEmpty()) {
            binding.profileTagsEmptyState.setVisibility(View.VISIBLE);
            binding.profileTagsChipGroup.setVisibility(View.GONE);
        } else {
            binding.profileTagsEmptyState.setVisibility(View.GONE);
            binding.profileTagsChipGroup.setVisibility(View.VISIBLE);
            for (Tag tag : tags) {
                Chip chip = new Chip(requireContext());
                chip.setText(tag.getTitle());
                chip.setChipBackgroundColor(ColorStateList.valueOf(tag.getColor()));
                binding.profileTagsChipGroup.addView(chip);
            }
        }
    }

    private void setupPurchasedItemsList() {
        Set<String> purchasedIds = prefs.getPurchasedItemIds();

        List<PurchasedItem> userOwnedItems = new ArrayList<>();
        for (PurchasedItem shopItem : allShopItems) {
            if (purchasedIds.contains(shopItem.id())) {
                userOwnedItems.add(shopItem);
            }
        }

        if (userOwnedItems.isEmpty()) {
            binding.purchasedItemsRecyclerView.setVisibility(View.GONE);
            binding.purchasedItemsEmptyState.setVisibility(View.VISIBLE);
        } else {
            binding.purchasedItemsRecyclerView.setVisibility(View.VISIBLE);
            binding.purchasedItemsEmptyState.setVisibility(View.GONE);

            PurchasedItemsAdapter adapter = new PurchasedItemsAdapter(userOwnedItems);
            binding.purchasedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.purchasedItemsRecyclerView.setAdapter(adapter);

            binding.purchasedItemsRecyclerView.setNestedScrollingEnabled(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
