package ch.inf.usi.mindbricks.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.drivers.calendar.CalendarDriver;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarSyncService;
import ch.inf.usi.mindbricks.util.SoundPlayer;


public class SettingsActivity extends AppCompatActivity {

    public static final String EXTRA_TAB_INDEX = "tab_index";
    private CalendarSyncService syncService;

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
            } else if (position == 2){
                tab.setText(R.string.settings_tab_pomodoro);
            }
            else if (position == 3){
                tab.setText(R.string.settings_tab_calendar);
            }
            else{
                tab.setText(R.string.settings_tab_debug);
            }
        }).attach();

        // Handle initial tab selection (needed for pomodoro settings from home)
        int initialTab = getIntent().getIntExtra(EXTRA_TAB_INDEX, 0);
        if (initialTab > 0 && initialTab < adapter.getItemCount()) {
            viewPager.setCurrentItem(initialTab, false);
        }

        syncService = CalendarSyncService.getInstance(this);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            // save_settings sound when leaving the settigs activity
            SoundPlayer.playSound(this, R.raw.save_settings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void connectGoogleCalendar() {
        syncService.authenticateDriver("google", this, new CalendarDriver.AuthCallback() {
            @Override
            public void onAuthSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this,
                            "Google Calendar connected!", Toast.LENGTH_SHORT).show();
                    updateUI();
                });
            }

            @Override
            public void onAuthFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this,
                            "Failed: " + error, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onAuthCancelled() {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this,
                            "Sign-in cancelled", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Disconnect a calendar
    private void disconnectCalendar(String sourceName) {
        syncService.disconnectDriver(sourceName);
        updateUI();
    }

    // Refresh UI to show connection status
    private void updateUI() {
        List<CalendarSyncService.DriverInfo> drivers = syncService.getDriverInfoList();
        for (CalendarSyncService.DriverInfo info : drivers) {
            Log.d("Calendar", info.displayName +
                    " - Connected: " + info.isConnected +
                    " - Last sync: " + info.getLastSyncTimeString());
        }
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
            } else if(position == 2){
                return new SettingsPomodoroFragment();
            }
            else if(position == 3) {
                return new SettingsCalendarFragment();
            }
            else{
                return new SettingsDebugFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
