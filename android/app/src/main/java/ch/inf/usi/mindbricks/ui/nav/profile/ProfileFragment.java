package ch.inf.usi.mindbricks.ui.nav.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // ADDED: Required for ViewModel

// Make sure this import path is correct for your project structure
import ch.inf.usi.mindbricks.util.ProfileViewModel;
import ch.inf.usi.mindbricks.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment { // Removed 'implements View.OnClickListener' as it's unused in the provided code

    private FragmentProfileBinding binding;

    // ADDED: Declare the shared ViewModel
    private ProfileViewModel profileViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // ADDED: Initialize the ViewModel by scoping it to the Activity
        // This ensures you get the same instance as HomeFragment and ShopFragment
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Inflate the layout using view binding
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        // Get the root view from the binding object
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ADDED: Observe the shared coin balance
        // This will automatically update the TextView whenever the coins change
        profileViewModel.coins.observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                // Use the binding object to access the TextView and set the text
                // Replace 'profileCoinBalance' with the actual ID of your TextView
                binding.profileCoinBalance.setText(String.valueOf(balance));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Set binding to null to avoid memory leaks
        binding = null;
    }

    // This onClick method was in your original code. You can keep it if you plan to
    // set this fragment as a click listener for any views.
    // public void onClick(View v) {
    //     // Do something if v == someIdWeAreInterestedIn
    // }
}
