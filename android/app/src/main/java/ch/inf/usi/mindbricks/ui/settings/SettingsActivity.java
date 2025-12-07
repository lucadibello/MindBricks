package ch.inf.usi.mindbricks.ui.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import ch.inf.usi.mindbricks.R;

public class SettingsActivity extends AppCompatActivity {

    public static final String EXTRA_TAB_INDEX = "tab_index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabs = findViewById(R.id.tabs);

        SettingsPagerAdapter adapter = new SettingsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.settings_tab_profile);
            } else if (position == 1) {
                tab.setText(R.string.settings_tab_study_plan);
            } else {
                tab.setText(R.string.settings_tab_pomodoro);
            }
        }).attach();

        // Handle initial tab selection (needed for pomodoro settings from home)
        int initialTab = getIntent().getIntExtra(EXTRA_TAB_INDEX, 0);
        if (initialTab > 0 && initialTab < adapter.getItemCount()) {
            viewPager.setCurrentItem(initialTab, false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class SettingsPagerAdapter extends FragmentStateAdapter {

        public SettingsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new SettingsProfileFragment();
            } else if (position == 1) {
                return new SettingsStudyPlanFragment();
            } else {
                return new SettingsPomodoroFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
