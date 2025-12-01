package ch.inf.usi.mindbricks.ui.onboarding;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingDoneFragment;
import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingIntroFragment;
import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingNotificationsFragment;
import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingStudyPlanFragment;
import ch.inf.usi.mindbricks.ui.onboarding.page.sensors.OnboardingSensorsFragment;
import ch.inf.usi.mindbricks.ui.onboarding.page.OnboardingUserFragment;

public class OnboardingPagerAdapter extends FragmentStateAdapter {

    public OnboardingPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case 0 -> new OnboardingIntroFragment();
            case 1 -> new OnboardingUserFragment();
            case 2 -> new OnboardingStudyPlanFragment();
            case 4 -> new OnboardingSensorsFragment();
            case 3 -> new OnboardingNotificationsFragment();
            default -> new OnboardingDoneFragment();
        };
    }

    @Override
    public int getItemCount() {
        return 6;
    }
}
