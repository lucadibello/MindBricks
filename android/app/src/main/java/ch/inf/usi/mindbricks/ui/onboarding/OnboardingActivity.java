package ch.inf.usi.mindbricks.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import ch.inf.usi.mindbricks.MainActivity;
import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.PreferencesManager;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button buttonSkip;
    private Button buttonNext;
    private PreferencesManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        prefs = new PreferencesManager(this);

        viewPager = findViewById(R.id.viewPagerOnboarding);
        buttonSkip = findViewById(R.id.buttonSkip);
        buttonNext = findViewById(R.id.buttonNext);

        viewPager.setAdapter(new OnboardingPagerAdapter(this));

        buttonSkip.setOnClickListener(v -> finishOnboarding());

        buttonNext.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();
            if (pos < 2) {
                viewPager.setCurrentItem(pos + 1);
            } else {
                finishOnboarding();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 2) {
                    buttonNext.setText("Start");
                    buttonSkip.setVisibility(View.GONE);
                } else {
                    buttonNext.setText("Next");
                    buttonSkip.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void finishOnboarding() {
        // toggle flag + go to main activity
        prefs.setOnboardingComplete();

        // you probably want to also save user data before this from page 1
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}