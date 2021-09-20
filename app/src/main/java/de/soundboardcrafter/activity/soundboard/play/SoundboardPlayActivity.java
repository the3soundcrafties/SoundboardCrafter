package de.soundboardcrafter.activity.soundboard.play;

import static com.google.common.base.Strings.isNullOrEmpty;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.main.MainActivity;
import de.soundboardcrafter.activity.soundboard.play.common.ISoundboardPlayActivity;
import de.soundboardcrafter.activity.soundboard.play.playing.PlayingFragment;
import de.soundboardcrafter.activity.soundboard.play.soundboard.SoundboardFragment;
import de.soundboardcrafter.dao.FavoritesDao;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.SoundboardWithSounds;
import de.soundboardcrafter.util.UuidUtil;

/**
 * The most important activity of the app - it shows all the soundboards so that the user
 * can play sounds.
 */
public class SoundboardPlayActivity extends AppCompatActivity
        implements ServiceConnection, ResetAllDialogFragment.OnOkCallback,
        ISoundboardPlayActivity {
    private static final String TAG = SoundboardPlayActivity.class.getName();
    private static final String KEY_TAB_UUID = "tabUuid";
    private static final String KEY_FAVORITES_ID = "favoritesId";
    private static final String SHARED_PREFERENCES =
            SoundboardPlayActivity.class.getName() + "_Prefs";
    private static final String DIALOG_RESET_ALL = "DialogResetAll";
    private ScreenSlidePagerAdapter pagerAdapter;

    private static final String EXTRA_SOUNDBOARD_ID = "SoundboardId";
    public static final String EXTRA_FAVORITES_ID = "FavoritesId";

    /**
     * The UUID used to identiy the "Currently Playing" tab.
     */
    private static final UUID PLAYING_TAB_UUID =
            UUID.fromString("a4a4d6b4-13ec-45ff-b505-6f7840d14c04");

    private MediaPlayerService mediaPlayerService;
    private ViewPager2 pager;
    private TabLayout tabLayout;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;
    private final View.OnTouchListener emptyOnTouchListener = (v, event) -> true;

    /**
     * ID of the chosen favorites - or <code>null</code>, if all soundboards shall be displayed.
     */
    @Nullable
    private UUID favoritesId;

    private boolean stillInitializing = false;

    @Nullable
    private UUID tabUuid;

    private final boolean changingSoundboardEnabled = true;

    @Override
    @UiThread
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MediaPlayerService.Binder b = (MediaPlayerService.Binder) binder;
        mediaPlayerService = b.getService();

        Log.d(TAG, "MediaPlayerService is connected");
    }

    @Override
    @UiThread
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "MediaPlayerService is disconnected");

        // TODO What to do on Service Disconnected?
    }

    @Override
    @UiThread
    public void onBindingDied(ComponentName name) {
        Log.d(TAG, "MediaPlayerService binding has died");

        // TODO What to do on Service Died?
    }

    @Override
    @UiThread
    public void onNullBinding(ComponentName name) {
        Log.d(TAG, "MediaPlayerService has null binding");

        // TODO What to do on Null Binding?
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soundboards);

        Intent intent = new Intent(this, MediaPlayerService.class);
        startService(intent);
        // TODO Necessary?! Also done in onResume()
        bindService();

        pager = findViewById(R.id.viewPager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);

        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                @Nullable UUID tmp = pagerAdapter.getTabUuid(position);
                if (tmp != null && !stillInitializing) {
                    tabUuid = tmp;
                }

                super.onPageSelected(position);
            }
        };
        pager.registerOnPageChangeCallback(pageChangeCallback);

        pager.setAdapter(pagerAdapter);

        tabLayout = findViewById(R.id.tabLayout);
        new TabLayoutMediator(tabLayout, pager,
                (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position))
        ).attach();

        stillInitializing = true;
        if (savedInstanceState != null) {
            tabUuid = getUUID(savedInstanceState, KEY_TAB_UUID);
            favoritesId = getUUID(savedInstanceState, KEY_FAVORITES_ID);
        }
        if (tabUuid == null && getIntent() != null) {
            tabUuid = getUUIDExtra(getIntent(), EXTRA_SOUNDBOARD_ID);
        }
        if (tabUuid == null) {
            tabUuid = getUUIDPreference(KEY_TAB_UUID);
        }
        if (favoritesId == null) {
            if (getIntent() != null) {
                favoritesId = getUUIDExtra(getIntent(), EXTRA_FAVORITES_ID);
            } else {
                favoritesId = getUUIDPreference(KEY_FAVORITES_ID);
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // This is just a dummy result, so that the
        // calling activity can update its GUI. (The user might have removed a
        // sound from a soundboard or the like.)
        Intent resultIntent = new Intent(this, MainActivity.class);
        setResult(// The result is always OK
                Activity.RESULT_OK,
                resultIntent);
    }

    @Override
    public void setChangingSoundboardEnabled(final boolean enabled) {
        pager.setUserInputEnabled(enabled);

        // TODO https://stackoverflow.com/questions/9650265/how-do-disable-paging-by-swiping-with
        //  -finger
        //  -in-viewpager-but-still-be-able-to-s
        /*
        From old DeactivatableViewPager:

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return pagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return pagingEnabled && super.onInterceptTouchEvent(event);
    }

         */


        LinearLayout tabStrip = ((LinearLayout) tabLayout.getChildAt(0));

        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View child = tabStrip.getChildAt(i);
            child.setOnTouchListener(enabled ? null : emptyOnTouchListener);
        }
    }

    @Nullable
    private UUID getUUIDPreference(String key) {
        SharedPreferences pref =
                getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        String idPref = pref.getString(key, null);
        if (idPref == null) {
            return null;
        }
        UUID id = UUID.fromString(idPref);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(KEY_TAB_UUID);
        editor.apply();
        return id;
    }

    @Nullable
    private static UUID getUUIDExtra(Intent intent, String extraKey) {
        String idString = intent.getStringExtra(extraKey);
        return idString != null ? UUID.fromString(idString) : null;
    }

    @Nullable
    private static UUID getUUID(Bundle savedInstanceState, String key) {
        @Nullable String idString = savedInstanceState.getString(key);
        return idString != null ? UUID.fromString(idString) : null;
    }

    private void bindService() {
        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        pagerAdapter.clear(false);
        new FindSoundboardsTask(this, favoritesId).execute();
    }

    @Override
    public void soundsDeleted() {
        pagerAdapter.clear(false);
        new FindSoundboardsTask(this, favoritesId).execute();
    }

    @Override
    @UiThread
    public void doResetAll() {
        Log.i(TAG, "Resetting all data");
        pagerAdapter.clear(true);
        new ResetAllTask(this).execute();
    }

    @Override
    @UiThread
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_soundboard_play, menu);
        if (
            // This button is only visible for the debug build type.
            // We have a separate bools.xml in the debug folder.
                !getResources().getBoolean(R.bool.reset_all) //
                        || favoritesId != null) {
            MenuItem item = menu.findItem(R.id.toolbar_menu_reset_all);
            item.setVisible(false);
            item.setEnabled(false);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.toolbar_menu_reset_all) {
            resetAllOrCancel();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @UiThread
    private void resetAllOrCancel() {
        ResetAllDialogFragment dialog = new ResetAllDialogFragment();
        dialog.show(getSupportFragmentManager(), DIALOG_RESET_ALL);
    }

    /**
     * Adapter for the tabs. Provides
     * <ul>
     *     <li>Currently playing as first tab
     *     ({@link de.soundboardcrafter.activity.soundboard.play.playing.PlayingFragment})</li>
     *     <li>Then a tab for each soundboard ({@link SoundboardFragment})</li>
     * </ul>>
     */
    class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        private final /* static */ long playingTabItemId = UuidUtil.toLong(PLAYING_TAB_UUID);

        private final List<SoundboardWithSounds> soundboardList = new ArrayList<>();

        ScreenSlidePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public int getItemCount() {
            return 1 // "Currently playing" tab
                    + soundboardList.size();
        }

        @Override
        public @NonNull
        Fragment createFragment(int position) {
            if (position == 0) {
                return PlayingFragment.newInstance();
            }

            return SoundboardFragment.newInstance(soundboardList.get(position - 1));
        }

        // https://developer.android.com/training/animation/vp2-migration :
        // "If you are using ViewPager2 to page through a mutable collection, you must also
        // override getItemId()"
        @Override
        public long getItemId(int position) {
            if (position < 0 || position > soundboardList.size()) {
                return RecyclerView.NO_ID;
            }

            if (position == 0) {
                return playingTabItemId;
            }

            final SoundboardWithSounds soundboardWithSounds = soundboardList.get(position - 1);
            return UuidUtil.toLong(soundboardWithSounds.getSoundboard().getId());
        }

        // FragmentStateAdapter: "When overriding, also override containsItem(long)"
        @Override
        public boolean containsItem(long itemId) {
            if (itemId == playingTabItemId) {
                // Currently-Playing tab shall always be contained.
                return true;
            }

            for (SoundboardWithSounds soundboardWithSounds : soundboardList) {
                if (UuidUtil.toLong(soundboardWithSounds.getSoundboard().getId()) == itemId) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Add soundboards to the @link {@link #soundboardList} and refreshes the
         * view
         */
        void addSoundboards(Collection<SoundboardWithSounds> soundboards) {
            soundboardList.addAll(soundboards);
            soundboardList.sort(SoundboardWithSounds.PROVIDED_LAST_THEN_BY_COLLATION_KEY);

            notifySoundsChanged();

            notifyDataSetChanged();

            setChangingSoundboardEnabled(changingSoundboardEnabled);
        }

        CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getResources().getString(R.string.soundboards_currently_playing);
            }

            return soundboardList.get(position - 1).getName();
        }

        /**
         * Returns the tab index for this UUID - or <code>null</code>, if no tab with this UUID
         * exists.
         */
        @Nullable
        Integer getIndex(UUID tabUuid) {
            if (tabUuid.equals(PLAYING_TAB_UUID)) {
                // Currently-Playing tab is always first.
                return 0;
            }

            for (int index = 0; index < soundboardList.size(); index++) {
                if (soundboardList.get(index).getId().equals(tabUuid)) {
                    return index + 1;
                }
            }

            return null;
        }

        /**
         * Returns the UUID at this index - or <code>null</code>, if the index was invalid.
         */
        @Nullable
        UUID getTabUuid(int index) {
            if (index == 0) {
                // Currently-Playing tab is always first.
                return PLAYING_TAB_UUID;
            }

            if (index > soundboardList.size()) {
                return null;
            }

            return soundboardList.get(index - 1).getId();
        }

        /* Old code before migrating to ViewPager2:
        @Override
        public int getItemPosition(@NonNull Object object) {
            // https://medium.com/inloopx/adventures-with-fragmentstatepageradapter-4f56a643f8e0
            return POSITION_NONE;
        }
         */

        void clear(boolean stopPlayingAllSoundboards) {
            // Always keep the "Currently Playing" Tab!

            if (stopPlayingAllSoundboards) {
                stopPlayingAllSoundboards();
            }

            soundboardList.clear();

            notifyDataSetChanged();

            setChangingSoundboardEnabled(changingSoundboardEnabled);
        }

        /**
         * Stops playback for all soundboards.
         */
        private void stopPlayingAllSoundboards() {
            MediaPlayerService service = getService();
            if (service == null) {
                return;
            }
            service.stopPlaying(soundboardList.stream()
                            .map(SoundboardWithSounds::getSoundboard)
                            .collect(Collectors.toList()),
                    false);
        }
    }

    private void notifySoundsChanged() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof SoundboardFragment) {
                ((SoundboardFragment) fragment).notifySoundsChanged();
            }
        }
    }

    private void setToolbarTitle(@Nullable String favoritesName) {
        @Nullable ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) {
            return;
        }

        if (isNullOrEmpty(favoritesName)) {
            supportActionBar.setTitle(R.string.soundboards_default_toolbar);
        } else {
            supportActionBar.setTitle(favoritesName);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    @Override
    @UiThread
    public void onResume() {
        super.onResume();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        bindService();
    }

    private MediaPlayerService getService() {
        if (mediaPlayerService == null) {
            // TODO Necessary?! Also done in onResume()
            bindService();
        }
        return mediaPlayerService;
    }

    @Override
    protected void onDestroy() {
        savePreference(KEY_TAB_UUID, tabUuid);
        savePreference(KEY_FAVORITES_ID, favoritesId);

        if (pageChangeCallback != null) {
            pager.unregisterOnPageChangeCallback(pageChangeCallback);
        }

        super.onDestroy();
    }

    private void savePreference(String key, @Nullable Object value) {
        SharedPreferences pref =
                getApplicationContext().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value != null ? value.toString() : null);
        editor.apply();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        putUUID(outState, KEY_TAB_UUID, pagerAdapter.getTabUuid(pager.getCurrentItem()));
        putUUID(outState, KEY_FAVORITES_ID, favoritesId);
    }

    private void putUUID(@NonNull Bundle outState, String key, UUID id) {
        @Nullable String idString = id != null ? id.toString() : null;
        outState.putString(key, idString);
    }

    /**
     * A background task, used to retrieve soundboards (and name of favorites, if favorites are
     * chosen) from
     * the database.
     */
    class FindSoundboardsTask extends AsyncTask<Void, Void, SoundboardPlayData> {
        private final String TAG = FindSoundboardsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        @Nullable
        private final UUID favoritesId;

        FindSoundboardsTask(Context context, @Nullable UUID favoritesId) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.favoritesId = favoritesId;
        }

        @Override
        @WorkerThread
        protected SoundboardPlayData doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            FavoritesDao favoritesDao = FavoritesDao.getInstance(appContext);
            SoundboardDao soundboardDao = SoundboardDao.getInstance(appContext);

            @Nullable String favoritesName = null;
            if (favoritesId != null) {
                Log.d(TAG, "Loading favorites name");
                favoritesName = favoritesDao.findFavoritesName(favoritesId);
            }

            Log.d(TAG, "Loading soundboards...");

            ImmutableList<SoundboardWithSounds> res = soundboardDao.findAllWithSounds(favoritesId);

            Log.d(TAG, "Soundboards loaded.");

            return new SoundboardPlayData(favoritesName, res);
        }

        @Override
        @UiThread
        protected void onPostExecute(SoundboardPlayData data) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            setToolbarTitle(data.getFavoritesName());

            pagerAdapter.clear(false);
            pagerAdapter.addSoundboards(data.getSoundboards());

            @Nullable Integer index = null;
            if (tabUuid != null) {
                index = pagerAdapter.getIndex(tabUuid);
            }
            stillInitializing = false;

            pager.setCurrentItem(index != null ? index : 0, false);
        }
    }

    /**
     * A background task, used to reset favorites, soundboards and sounds.
     */
    class ResetAllTask extends AsyncTask<Void, Void, ImmutableList<SoundboardWithSounds>> {
        private final String TAG = ResetAllTask.class.getName();

        private final WeakReference<Context> appContextRef;

        ResetAllTask(Context context) {
            super();

            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected ImmutableList<SoundboardWithSounds> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Resetting all data.");

            SoundboardDao soundboardDao = SoundboardDao.getInstance(appContext);
            soundboardDao.clearDatabase();

            Log.d(TAG, "Loading soundboards...");

            final ImmutableList<SoundboardWithSounds> res =
                    // Resetting is only enabled when no favorites are selected
                    soundboardDao.findAllWithSounds();

            Log.d(TAG, "Soundboards loaded.");

            return res;
        }

        @Override
        @UiThread
        protected void onPostExecute(ImmutableList<SoundboardWithSounds> soundboards) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }

            pagerAdapter.addSoundboards(soundboards);

            @Nullable Integer index = null;
            if (tabUuid != null) {
                index = pagerAdapter.getIndex(tabUuid);
            }
            stillInitializing = false;

            pager.setCurrentItem(index != null ? index : 0, false);
        }
    }
}