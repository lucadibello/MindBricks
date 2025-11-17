package ch.inf.usi.mindbricks.ui.onboarding.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import ch.inf.usi.mindbricks.R;

public class OnboardingNotificationsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_notifications, container, false);

        MaterialButton button = view.findViewById(R.id.buttonEnableNotifications);
        button.setOnClickListener(v -> requestNotificationPermissionIfNeeded());

        return view;
    }

    private void requestNotificationPermissionIfNeeded() {
        System.err.println("This needs to be implemented.");
    }
}
