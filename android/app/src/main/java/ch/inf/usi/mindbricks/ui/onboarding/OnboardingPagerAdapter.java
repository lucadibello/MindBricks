package ch.inf.usi.mindbricks.ui.onboarding;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingDoneFragment;
import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingIntroFragment;
import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingNotificationsFragment;
import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingSensorsFragment;
import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingUserFragment;

public class OnboardingPagerAdapter extends FragmentStateAdapter {

    public OnboardingPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new OnboardingIntroFragment();
            case 1:
                return new OnboardingUserFragment();
            case 2:
                return new OnboardingSensorsFragment();
            case 3:
                return new OnboardingNotificationsFragment();
            case 4:
            default:
                return new OnboardingDoneFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
