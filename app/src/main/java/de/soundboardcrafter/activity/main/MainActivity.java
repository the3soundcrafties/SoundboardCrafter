package de.soundboardcrafter.activity.main;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.about.AboutActivity;
import de.soundboardcrafter.activity.audiofile.list.AudioFileListFragment;
import de.soundboardcrafter.activity.favorites.list.FavoritesListFragment;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.activity.soundboard.list.SoundboardListFragment;
import de.soundboardcrafter.dao.TutorialDao;

/**
 * The activity with which the app is started, showing favorites, soundboards, and sounds.
 */
public class MainActivity extends AppCompatActivity
        implements SoundEventListener {
    private static final List<Page> pages =
            Lists.newArrayList(Page.FAVORITES, Page.SOUNDBOARDS, Page.SOUNDS);

    private static final String KEY_SELECTED_PAGE = "selectedPage";
    private ViewPager2 pager;
    private ScreenSlidePagerAdapter pagerAdapter;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;
    private Page selectedPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.viewPagerMain);
        pagerAdapter = new ScreenSlidePagerAdapter(this);

        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment instanceof AudioFileListFragment) {
                        ((AudioFileListFragment) fragment).stopPlaying();
                    }
                }

                @Nullable Page tmp = pagerAdapter.getPage(position);
                if (tmp != null) {
                    selectedPage = tmp;
                }

                super.onPageSelected(position);
            }
        };
        pager.registerOnPageChangeCallback(pageChangeCallback);

        pager.setAdapter(pagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabLayoutMain);
        new TabLayoutMediator(tabLayout, pager,
                (tab, position) -> tab.setText(getString(pages.get(position).title))
        ).attach();

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
    }

    @Override
    @UiThread
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    @UiThread
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.toolbar_menu_item_about) {
            startActivity(AboutActivity.newIntent(this));
            return true;
        } else if (id == R.id.toolbar_menu_item_restart_tutorial) {
            TutorialDao.getInstance(this).uncheckAll();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
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
        FAVORITES(R.string.favorites_tab_title, FavoritesListFragment::new),
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
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof SoundEventListener) {
                ((SoundEventListener) fragment).soundChanged(soundId);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (pageChangeCallback != null) {
            pager.unregisterOnPageChangeCallback(pageChangeCallback);
        }

        super.onDestroy();
    }

    @Override
    public void somethingMightHaveChanged() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof SoundEventListener) {
                ((SoundEventListener) fragment).somethingMightHaveChanged();
            }
        }
    }

    static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        ScreenSlidePagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public @NonNull
        Fragment createFragment(int position) {
            Page page = pages.get(position);
            return page.createNew.get();
        }

        Page getPage(int position) {
            return pages.get(position);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        int getIndexOf(Page selectedPage) {
            return pages.indexOf(selectedPage);
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