package de.soundboardcrafter.activity.soundboard.play;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;
import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Soundboard;

/**
 * The main activity, showing the soundboards.
 */
public class MainActivity extends AppCompatActivity
        implements ServiceConnection, ResetAllDialogFragment.OnOkCallback {
    private static final String TAG = MainActivity.class.getName();

    private static final String DIALOG_RESET_ALL = "DialogResetAll";
    private static final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1024;
    private ScreenSlidePagerAdapter pagerAdapter;
    private static final int REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 0;

    private MediaPlayerService mediaPlayerService;

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
        // TODO What to do on Service Disconnected?
    }

    @Override
    @UiThread
    public void onBindingDied(ComponentName name) {
        // TODO What to do on Service Died?
    }

    @Override
    @UiThread
    public void onNullBinding(ComponentName name) {
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


        ViewPager pager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(pager);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
    }

    private void bindService() {
        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            return;
        }
        pagerAdapter.clear(false);
        new FindSoundboardsTask(this).execute();
    }

    @Override
    @UiThread
    public void doResetAll() {
        Log.i(TAG, "Resetting sound data");
        pagerAdapter.clear(true);
        new ResetAllTask(this).execute();
    }

    @Override
    @UiThread
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_menu_reset_all:
                resetAllOrCancel();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @UiThread
    private void resetAllOrCancel() {
        ResetAllDialogFragment dialog = new ResetAllDialogFragment();
        dialog.show(getSupportFragmentManager(), DIALOG_RESET_ALL);
    }

    class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private final List<Soundboard> soundboardList = new ArrayList<>();

        ScreenSlidePagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @Override
        public @NonNull
        Fragment getItem(int position) {
            return SoundboardFragment.createTab(soundboardList.get(position));
        }

        void addSoundboard(Soundboard soundboard) {
            soundboardList.add(soundboard);
            notifyDataSetChanged();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return soundboardList.get(position).getName();
        }

        @Override
        public int getCount() {
            return soundboardList.size();
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            // https://medium.com/inloopx/adventures-with-fragmentstatepageradapter-4f56a643f8e0
            return POSITION_NONE;
        }

        void clear(boolean stopPlayingAllSoundboards) {
            if (stopPlayingAllSoundboards) {
                stopPlayingAllSoundboards();
            }

            soundboardList.clear();
            notifyDataSetChanged();
        }

        private void stopPlayingAllSoundboards() {
            MediaPlayerService service = getService();
            if (service == null) {
                return;
            }
            service.stopPlaying(soundboardList);
        }
    }

    @Override
    @UiThread
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE || requestCode == REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // User denied. Stop the app.
                finishAndRemoveTask();
            }
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
        bindService();
    }

    private MediaPlayerService getService() {
        if (mediaPlayerService == null) {
            // TODO Necessary?! Also done in onResume()
            bindService();
        }
        return mediaPlayerService;
    }

    /**
     * A background task, used to retrieve soundboards from the database.
     */
    class FindSoundboardsTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
        private final String TAG = FindSoundboardsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        FindSoundboardsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected ImmutableList<Soundboard> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            SoundboardDao soundboardDao = SoundboardDao.getInstance(appContext);

            Log.d(TAG, "Loading soundboards...");

            ImmutableList<Soundboard> res = soundboardDao.findAll();

            if (res.isEmpty()) {
                Log.d(TAG, "No soundboards found.");
                Log.d(TAG, "Insert some dummy data...");

                soundboardDao.insertDummyData();

                Log.d(TAG, "...and load the soundboards again");
                res = soundboardDao.findAll();
            }

            Log.d(TAG, "Soundboards loaded.");

            return res;
        }

        @Override
        @UiThread
        protected void onPostExecute(ImmutableList<Soundboard> soundboards) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            for (Soundboard soundboard : soundboards) {
                pagerAdapter.addSoundboard(soundboard);
            }

        }
    }

    /**
     * A background task, used to reset the soundboards and retrieve them from the database.
     */
    class ResetAllTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
        private final String TAG = ResetAllTask.class.getName();

        private final WeakReference<Context> appContextRef;

        ResetAllTask(Context context) {
            super();

            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected ImmutableList<Soundboard> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Resetting soundboards.");

            SoundboardDao soundboardDao = SoundboardDao.getInstance(appContext);
            soundboardDao.clearDatabase();
            soundboardDao.insertDummyData();

            Log.d(TAG, "Loading soundboards...");

            final ImmutableList<Soundboard> res = soundboardDao.findAll();

            Log.d(TAG, "Soundboards loaded.");

            return res;
        }

        @Override
        @UiThread
        protected void onPostExecute(ImmutableList<Soundboard> soundboards) {

            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }

            for (Soundboard soundboard : soundboards) {
                pagerAdapter.addSoundboard(soundboard);
            }
        }

    }
}