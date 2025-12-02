package ch.inf.usi.mindbricks;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import ch.inf.usi.mindbricks.databinding.ActivityMainBinding;
import ch.inf.usi.mindbricks.ui.nav.NavigationLocker;

public class MainActivity extends AppCompatActivity implements NavigationLocker {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    public void setNavigationEnabled(boolean enabled) {
        if (binding != null) {
            for (int i = 0; i < binding.navView.getMenu().size(); i++) {
                binding.navView.getMenu().getItem(i).setEnabled(enabled);
            }
        }
    }
}