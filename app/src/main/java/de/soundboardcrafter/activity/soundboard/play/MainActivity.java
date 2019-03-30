package de.soundboardcrafter.activity.soundboard.play;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import de.soundboardcrafter.R;

/**
 * The main activity, showing the soundboards.
 */
public class MainActivity extends FragmentActivity {
    private ViewPager pager;
    private PagerAdapter pagerAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        pagerAdapter =
                new ScreenSlidePagerAdapter(
                        getSupportFragmentManager());
        pager = (ViewPager) findViewById(R.id.viewPager);
        pager.setAdapter(pagerAdapter);
    }


    class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            return new SoundboardFragment();
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "OBJECT " + (position + 1);
        }


    }
}
