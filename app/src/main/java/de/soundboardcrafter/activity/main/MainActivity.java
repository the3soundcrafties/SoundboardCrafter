package de.soundboardcrafter.activity.main;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.about.AboutActivity;
import de.soundboardcrafter.activity.audiofile.list.AudioFileListFragment;
import de.soundboardcrafter.activity.common.PermissionUtil;
import de.soundboardcrafter.activity.favorites.list.FavoritesListFragment;
import de.soundboardcrafter.activity.settings.SettingsActivity;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.activity.soundboard.list.SoundboardListFragment;
import de.soundboardcrafter.dao.TutorialDao;

/**
 * The activity with which the app is started, showing favorites (when enabled), soundboards, and
 * sounds.
 */
public class MainActivity extends AppCompatActivity
        implements SoundEventListener {
    private static final int REQUEST_PERMISSION_TO_READ_AUDIO_FILE = 1;

    private static final String KEY_SELECTED_PAGE = "selectedPage";

    private List<Page> pages;

    private ViewPager2 pager;
    private ScreenSlidePagerAdapter pagerAdapter;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;
    private Page selectedPage;

    @Nullable
    private OnBackPressedCallback onBackPressed;

    @NonNull
    private OnBackPressedCallback getBackToSoundboardsCallback() {
        return new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                selectSoundboardsTab();
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pages = calcPages();

        pager = findViewById(R.id.viewPagerMain);
        pagerAdapter = new ScreenSlidePagerAdapter(this);

        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                stopPlayingInAudioListFragment();

                @Nullable Page newSelectedPage = pagerAdapter.getPage(position);
                if (newSelectedPage != null) {
                    setSelectedPage(newSelectedPage);

                    if (selectedPage == Page.SOUNDS) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this,
                                PermissionUtil.calcPermissionToReadAudioFiles())
                                != PackageManager.PERMISSION_GRANTED) {
                            requestReadExternalPermission();
                        }
                    }
                }
            }
        };
        pager.registerOnPageChangeCallback(pageChangeCallback);

        pager.setAdapter(pagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabLayoutMain);
        new TabLayoutMediator(tabLayout, pager,
                (tab, position) -> tab.setText(getString(pages.get(position).title))).attach();

        backButtonLeadsToSoundboards();

        if (savedInstanceState != null) {
            @Nullable String savedSelectedPageString =
                    savedInstanceState.getString(KEY_SELECTED_PAGE);
            if (savedSelectedPageString != null) {
                final Page selectedPage = Page.valueOf(savedSelectedPageString);
                setSelectedPage(pages.contains(selectedPage) ? selectedPage : null);
            } else {
                setSelectedPage(null);
            }
        }

        if (selectedPage == null) {
            setSelectedPage(Page.SOUNDBOARDS);
        }
        int index = pagerAdapter.getIndexOf(selectedPage);
        pager.setCurrentItem(index != -1 ? index : 0, false);
    }

    private void backButtonLeadsToSoundboards() {
        // This callback will only be called when the activity is at least STARTED.
        onBackPressed = getBackToSoundboardsCallback();
        getOnBackPressedDispatcher().addCallback(this, onBackPressed);
    }

    public void selectSoundboardsTab() {
        setSelectedPage(Page.SOUNDBOARDS);
        int index = pagerAdapter.getIndexOf(selectedPage);
        pager.setCurrentItem(index, true);
    }

    private void stopPlayingInAudioListFragment() {
        @Nullable
        AudioFileListFragment audioFileListFragment = getAudioFileListFragment();

        if (audioFileListFragment != null) {
            audioFileListFragment.stopPlaying();
        }
    }

    @Nullable
    private AudioFileListFragment getAudioFileListFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof AudioFileListFragment) {
                return (AudioFileListFragment) fragment;
            }
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_TO_READ_AUDIO_FILE) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted
                PermissionUtil.setShouldShowStatus(this,
                        PermissionUtil.calcPermissionToReadAudioFiles());
                showPermissionRationale();
                return;
            }

            onPermissionReadExternalStorageGranted();
        }
    }

    private void showPermissionRationale() {
        PermissionUtil.showYourSoundsPermissionDialog(this, this::onSoundPermissionsRationaleOk,
                this::onPermissionReadExternalStorageNotGrantedUserGivesUp);
    }

    private void onPermissionReadExternalStorageGranted() {
        if (selectedPage == Page.SOUNDS) {
            AudioFileListFragment audioFileListFragment = getAudioFileListFragment();
            if (audioFileListFragment != null) {
                audioFileListFragment.loadAudioFiles();
            }
        }
    }

    private void onPermissionReadExternalStorageNotGrantedUserGivesUp() {
        if (pager.getCurrentItem() == pagerAdapter.getIndexOf(Page.SOUNDS)) {
            // Reset Page to Soundboards
            pager.setCurrentItem(pagerAdapter.getIndexOf(Page.SOUNDBOARDS));
        }
    }

    private void onSoundPermissionsRationaleOk() {
        if (PermissionUtil.androidDoesNotShowPermissionPopupsAnymore(this,
                PermissionUtil.calcPermissionToReadAudioFiles())) {
            startActivity(PermissionUtil.buildAppSettingsIntent());
        } else {
            requestReadExternalPermission();
        }
    }

    @UiThread
    private void requestReadExternalPermission() {
        requestPermissions(new String[]{PermissionUtil.calcPermissionToReadAudioFiles()},
                REQUEST_PERMISSION_TO_READ_AUDIO_FILE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final ImmutableList<Page> necessaryPages = calcPages();
        if (!pages.equals(necessaryPages)) {
            pages = necessaryPages;
            pagerAdapter.notifyDataSetChanged();
        }
    }

    @NonNull
    private ImmutableList<Page> calcPages() {
        ImmutableList.Builder<Page> res = ImmutableList.builder();

        if (useFavorites()) {
            res.add(Page.FAVORITES);
        }

        res.add(Page.SOUNDBOARDS, Page.SOUNDS);

        return res.build();
    }

    private boolean useFavorites() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getBoolean("useFavorites", false);
        // Defined in preferences.xml.
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
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
        } else if (id == R.id.toolbar_menu_item_settings) {
            startActivity(SettingsActivity.newIntent(this));
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
        FAVORITES(1, R.string.favorites_tab_title, FavoritesListFragment::new),
        SOUNDBOARDS(2, R.string.soundboards_tab_title, MainActivity::createSoundboardListFragment),
        SOUNDS(3, R.string.sounds_tab_title, MainActivity::createAudioFileListFragment);

        final int id;
        final int title;
        final Supplier<Fragment> createNew;

        Page(int id, int title, Supplier<Fragment> createNew) {
            this.id = id;
            this.title = title;
            this.createNew = createNew;
        }

        public long getId() {
            return id;
        }
    }

    @NonNull
    @Contract(" -> new")
    private static SoundboardListFragment createSoundboardListFragment() {
        return SoundboardListFragment.createFragment();
    }

    @NonNull
    @Contract(" -> new")
    private static AudioFileListFragment createAudioFileListFragment() {
        return AudioFileListFragment.newInstance();
    }

    @Override
    public void soundChanged(UUID soundId) {
        postSoundEvent(soundEventListener -> soundEventListener.soundChanged(soundId));
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
        postSoundEvent(SoundEventListener::somethingMightHaveChanged);
    }

    private void postSoundEvent(Consumer<SoundEventListener> postEvent) {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof SoundEventListener) {
                postEvent.accept((SoundEventListener) fragment);
            }
        }
    }

    class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        ScreenSlidePagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public @NonNull
        Fragment createFragment(int position) {
            Page page = pages.get(position);
            return page.createNew.get();
        }

        // https://developer.android.com/training/animation/vp2-migration :
        // "If you are using ViewPager2 to page through a mutable collection, you must also
        // override getItemId()"
        @Override
        public long getItemId(int position) {
            @Nullable final Page page = getPage(position);
            if (page == null) {
                return RecyclerView.NO_ID;
            }

            return page.getId();
        }

        // FragmentStateAdapter: "When overriding, also override containsItem(long)"
        @Override
        public boolean containsItem(long itemId) {
            for (Page page : pages) {
                if (page.getId() == itemId) {
                    return true;
                }
            }

            return false;
        }

        @Nullable
        Page getPage(int position) {
            if (position < 0 || position >= pages.size()) {
                return null;
            }

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
        @Nullable String selectedPageName = selectedPage != null ? selectedPage.name() : null;

        outState.putString(KEY_SELECTED_PAGE, selectedPageName);
    }

    private void setSelectedPage(@Nullable Page selectedPage) {
        this.selectedPage = selectedPage;
        if (onBackPressed != null) {
            onBackPressed.setEnabled(selectedPage != null && selectedPage != Page.SOUNDBOARDS);
        }
    }
}