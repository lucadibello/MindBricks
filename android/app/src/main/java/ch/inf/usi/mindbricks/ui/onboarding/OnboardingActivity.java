package ch.inf.usi.mindbricks.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import ch.inf.usi.mindbricks.MainActivity;
import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.ValidationResult;
import ch.inf.usi.mindbricks.util.validators.ProfileValidator;

public class OnboardingActivity extends AppCompatActivity {

    private static final int PAGE_USER = 1;

    private ViewPager2 viewPager;
    private MaterialButton buttonSkip;
    private MaterialButton buttonNext;
    private PreferencesManager prefs;
    private OnboardingPagerAdapter pagerAdapter;
    private int currentPage = 0;
    private boolean suppressPageChangeCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // load preferences
        prefs = new PreferencesManager(this);

        // initialize views
        viewPager = findViewById(R.id.viewPagerOnboarding);
        buttonSkip = findViewById(R.id.buttonSkip);
        buttonNext = findViewById(R.id.buttonNext);

        // load pager adapter to manage onboarding steps (fragments)
        pagerAdapter = new OnboardingPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // on skip: validate that everything is verified + skip non-required steps
        buttonSkip.setOnClickListener(v -> {
            if (!validateCurrentStep()) return;
            if (!canCompleteOnboarding()) return;
            finishOnboarding();
        });

        // on next: validate + proceed to next page
        buttonNext.setOnClickListener(v -> {
            if (!validateCurrentStep()) return;
            // NOTE: proceed to next if not last page
            int pos = viewPager.getCurrentItem();
            if (pos < pagerAdapter.getItemCount() - 1) {
                viewPager.setCurrentItem(pos + 1);
            } else {
                // otherwise: finish onboarding
                if (!canCompleteOnboarding()) return;
                finishOnboarding();
            }
        });

        // page change listener (validation + bottom buttons)
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // if page if callback is disabled
                //
                // NOTE: when user changes page, but page is not valid we force the pager
                // to stay on the current page, which triggers the callback again.
                if (suppressPageChangeCallback) {
                    suppressPageChangeCallback = false;
                    currentPage = position;
                    updateBottomButtons(position);
                    return;
                }

                // validate current step
                if (position > currentPage) {
                    if (!validateStepAtPosition(currentPage)) {
                        // if not valid -> force user to stay on page!
                        suppressPageChangeCallback = true; // disable next page change callback
                        viewPager.setCurrentItem(currentPage, true);
                        return;
                    }
                }

                // if everything is okay -> go to requested page
                currentPage = position;
                updateBottomButtons(position);
            }
        });

        updateBottomButtons(viewPager.getCurrentItem());
    }

    /**
     * Updates the bottom buttons based on the current page:
     * - the skip button is hidden if it's the last page
     * - the next button is set to "Start" if it's the last page otherwise to "Next"
     *
     * @param position index of the current page
     */
    private void updateBottomButtons(int position) {
        boolean isLastPage = position == pagerAdapter.getItemCount() - 1;
        buttonNext.setText(isLastPage ? R.string.onboarding_start : R.string.onboarding_next);
        buttonSkip.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
    }

    /**
     * Triggers validation routine for the current onboarding step
     *
     * @return true if step is valid, false otherwise
     */
    private boolean validateCurrentStep() {
        return validateStepAtPosition(viewPager.getCurrentItem());
    }

    /**
     * Validates a given onboarding step
     *
     * @param position index of the onboarding step
     * @return true if step is valid or doesn't need validation, false otherwise
     */
    private boolean validateStepAtPosition(int position) {
        // get step fragment + check if it has validation capabilities
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + position);
        if (fragment instanceof OnboardingStepValidator) {
            // validate step
            return ((OnboardingStepValidator) fragment).validateStep();
        }
        // if no validation needed -> always valid
        return true;
    }

    /**
     * Checks if user has completed the onboarding
     *
     * @return true if user has completed the onboarding, false otherwise
     */
    private boolean canCompleteOnboarding() {
        // ensure user has a valid name
        ValidationResult nameResult = ProfileValidator.validateName(
                prefs.getUserName(),
                this
        );
        if (!nameResult.isValid()) {
            viewPager.setCurrentItem(PAGE_USER, true);
            Snackbar.make(viewPager, R.string.onboarding_error_profile_required, Snackbar.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Finishes the onboarding process and goes to the main activity
     */
    private void finishOnboarding() {
        // toggle flag + go to main activity
        prefs.setOnboardingComplete();

        // give some initial coins to the user to start with (ensure that this is done only once)
        if (prefs.getBalance() == 0) {
            int initialCoins = getResources().getInteger(R.integer.starting_coins);
            Log.i("OnboardingActivity", "Giving user initial coins: " + initialCoins);
            prefs.setBalance(initialCoins);
        }

        // go to main activity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
