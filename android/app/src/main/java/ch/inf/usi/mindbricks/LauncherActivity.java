package ch.inf.usi.mindbricks;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ch.inf.usi.mindbricks.ui.onboarding.OnboardingActivity;
import ch.inf.usi.mindbricks.util.PreferencesManager;

public class LauncherActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load permission manager
        PreferencesManager prefManager = new PreferencesManager(this);

        // Use intent to notify which activity to load
        Intent intent;
        if (!prefManager.isOnboardingComplete()) {
            intent = new Intent(this, OnboardingActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        // start activity
        startActivity(intent);
        finish(); // close activity, no "back" button available
    }
}
