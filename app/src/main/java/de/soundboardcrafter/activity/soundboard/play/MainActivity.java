package de.soundboardcrafter.activity.soundboard.play;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;
import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import androidx.annotation.NonNull;
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
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Soundboard;

/**
 * The main activity, showing the soundboards.
 */
public class MainActivity extends AppCompatActivity implements ResetAllDialogFragment.OnOkCallback {
    private static final String TAG = MainActivity.class.getName();

    private static final String DIALOG_RESET_ALL = "DialogResetAll";
    private static final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1024;
    private ViewPager pager;
    private ScreenSlidePagerAdapter pagerAdapter;
    private TabLayout tabLayout;
    private static final int REQUEST_PERMISSIONS_READ_EXTERNAL_STORAGE = 0;
    private ArrayList<Soundboard> soundboardList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soundboards);
        pager = (ViewPager) findViewById(R.id.viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(pager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            return;
        }
        pagerAdapter.clear();
        new FindSoundboardsTask(this).execute();
    }

    @Override
    @UiThread
    public void doResetAll() {
        Log.i(TAG, "Resetting sound data");
        pagerAdapter.clear();
        new ResetAllTask(this).execute();
    }

    @Override
    @UiThread
    public boolean onCreateOptionsMenu(@Nonnull Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@Nonnull MenuItem item) {
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
        public Fragment getItem(int position) {
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

        void clear() {
            soundboardList.clear();
            notifyDataSetChanged();
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

    /**
     * A background task, used to retrieve soundboards from the database.
     */
    public class FindSoundboardsTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
        private final String TAG = FindSoundboardsTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final SoundboardDao soundboardDao;

        FindSoundboardsTask(Context context) {
            super();
            soundboardDao = SoundboardDao.getInstance(context);
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
    public class ResetAllTask extends AsyncTask<Void, Void, ImmutableList<Soundboard>> {
        private final String TAG = ResetAllTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final SoundboardDao soundboardDao;

        ResetAllTask(Context context) {
            super();
            soundboardDao = SoundboardDao.getInstance(context);
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