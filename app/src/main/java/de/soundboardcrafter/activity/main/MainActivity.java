package de.soundboardcrafter.activity.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.audiofile.list.AudioFileListFragment;
import de.soundboardcrafter.activity.game.list.GameListFragment;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.activity.soundboard.list.SoundboardListFragment;

/**
 * The main activity, showing the soundboards.
 */
public class MainActivity extends AppCompatActivity implements SoundEventListener {
    private static final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1024;
    private static final String KEY_SELECTED_PAGE = "selectedPage";
    private ViewPager pager;
    private ScreenSlidePagerAdapter pagerAdapter;
    private Page selectedPage;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.viewPagerMain);
        pager.clearOnPageChangeListeners();
        TabLayout tabLayout = findViewById(R.id.tabLayoutMain);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                pagerAdapter.getAudioFileListFragment()
                        .ifPresent(AudioFileListFragment::stopPlaying);

                @Nullable Page tmp = pagerAdapter.getPage(position);
                if (tmp != null) {
                    selectedPage = tmp;
                }
            }
        });

        tabLayout.setupWithViewPager(pager);
        if (savedInstanceState != null) {
            @Nullable String selectedPage = savedInstanceState.getString(KEY_SELECTED_PAGE);
            this.selectedPage = selectedPage != null ? Page.valueOf(selectedPage) : null;
        }

        if (selectedPage == null) {
            selectedPage = Page.SOUNDBOARDS;
        }
        int index = pagerAdapter.getIndexOf(selectedPage);
        pager.setCurrentItem(index != -1 ? index : 0, false);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            return;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        updateUI();
    }

    private void updateUI() {
        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
        }
    }

    public enum Page {
        GAMES(R.string.games_tab_title, GameListFragment::new),
        SOUNDBOARDS(R.string.soundboards_tab_title, MainActivity::createSoundboardListFragment),
        SOUNDS(R.string.sounds_tab_title, MainActivity::createAudioFileListFragment);

        final int title;
        final Supplier<Fragment> createNew;

        Page(int title, Supplier<Fragment> createNew) {
            this.title = title;
            this.createNew = createNew;
        }
    }

    private static SoundboardListFragment createSoundboardListFragment() {
        return SoundboardListFragment.createFragment();
    }

    private static AudioFileListFragment createAudioFileListFragment() {
        return AudioFileListFragment.createFragment();
    }

    @Override
    public void soundChanged(UUID soundId) {
        for (Fragment fragment : pagerAdapter) {
            if (fragment instanceof SoundEventListener) {
                ((SoundEventListener) fragment).soundChanged(soundId);
            }
        }
    }

    @Override
    public void somethingMightHaveChanged() {
        for (Fragment fragment : pagerAdapter) {
            if (fragment instanceof SoundEventListener) {
                ((SoundEventListener) fragment).somethingMightHaveChanged();
            }
        }
    }

    class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter
            implements Iterable<Fragment> {
        private final List<Page> pages =
                Lists.newArrayList(Page.GAMES, Page.SOUNDBOARDS, Page.SOUNDS);

        private final ArrayList<Fragment> registeredFragments =
                Lists.newArrayList(null, null, null);

        ScreenSlidePagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @Override
        public @NonNull
        Fragment getItem(int position) {
            Page page = pages.get(position);
            return page.createNew.get();
        }

        @Override
        public @NonNull
        Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.set(position, fragment);
            return fragment;
        }

        Page getPage(int position) {
            return pages.get(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return getString(pages.get(position).title);
        }

        @Override
        public int getCount() {
            return pages.size();
        }

        int getIndexOf(Page selectedPage) {
            return pages.indexOf(selectedPage);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            registeredFragments.set(position, null);
            super.destroyItem(container, position, object);
        }

        @NonNull
        Optional<AudioFileListFragment> getAudioFileListFragment() {
            return registeredFragments.stream()
                    .filter(f -> f instanceof AudioFileListFragment)
                    .map(AudioFileListFragment.class::cast)
                    .findAny();
        }


        @Override
        public @NonNull
        Iterator<Fragment> iterator() {
            return registeredFragments.stream()
                    .filter(Objects::nonNull).iterator();
        }

    }

    @Override
    @UiThread
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // User denied. Stop the app.
                finishAndRemoveTask();
                return;
            }

            // We don't need other permissions, so start reading data.
            somethingMightHaveChanged();
        }
    }

    @Override
    @UiThread
    public void onResume() {
        super.onResume();
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        int selectedTab = pager.getCurrentItem();
        @Nullable Page selectedPage = pagerAdapter.getPage(selectedTab);
        @Nullable String selectedPageName = selectedPage != null ?
                selectedPage.name() : null;

        outState.putString(KEY_SELECTED_PAGE, selectedPageName);
    }


}