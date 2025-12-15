package ch.inf.usi.mindbricks;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import ch.inf.usi.mindbricks.databinding.ActivityMainBinding;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import java.util.concurrent.TimeUnit;
import ch.inf.usi.mindbricks.database.DatabaseCleanupWorker;

public class MainActivity extends AppCompatActivity implements NavigationLocker {

    private ActivityMainBinding binding;
    private boolean isNavigationEnabled = true;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Add touch listener to show toast when navigation is locked
        binding.navView.setOnTouchListener((v, event) -> {
            if (!isNavigationEnabled && event.getAction() == MotionEvent.ACTION_DOWN) {
                Toast.makeText(MainActivity.this,
                        "Navigation locked during focus session",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        schedulePeriodicCleanup();

    }

    @Override
    public void setNavigationEnabled(boolean enabled) {
        isNavigationEnabled = enabled;
        if (binding != null) {
            for (int i = 0; i < binding.navView.getMenu().size(); i++) {
                binding.navView.getMenu().getItem(i).setEnabled(enabled);
            }
            // Visual feedback: change opacity and elevation
            binding.navView.setAlpha(enabled ? 1.0f : 0.5f);
            binding.navView.setElevation(enabled ?
                    getResources().getDimension(R.dimen.bottom_nav_elevation) : 0f);
        }
    }

    private void schedulePeriodicCleanup() {
        PeriodicWorkRequest cleanupWork = new PeriodicWorkRequest.Builder(
                DatabaseCleanupWorker.class,
                7, TimeUnit.DAYS  // Run weekly
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "database_cleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupWork
        );

        Log.d("MainActivity", "Database cleanup worker scheduled");
    }
}