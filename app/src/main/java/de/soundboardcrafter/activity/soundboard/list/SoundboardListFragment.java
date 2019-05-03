package de.soundboardcrafter.activity.soundboard.list;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Shows Soundboard in a Grid
 */
public class SoundboardListFragment extends Fragment {
    private ListView listView;
    private SoundboardListItemAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new SoundboardListFragment.FindSoundboardsTask(getContext()).execute();
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard_list,
                container, false);
        listView = rootView.findViewById(R.id.listview_soundboard);

        return rootView;
    }


    @UiThread
    private void initSoundboardItemAdapter(ImmutableList<SoundboardWithSounds> soundboards) {
        List<SoundboardWithSounds> list = Lists.newArrayList(soundboards);
        list.sort((s1, s2) -> s1.getName().compareTo(s2.getName()));
        adapter = new SoundboardListItemAdapter(list);
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }


    /**
     * A background task, used to retrieve soundboards from the database.
     */
    class FindSoundboardsTask extends AsyncTask<Void, Void, ImmutableList<SoundboardWithSounds>> {
        private final String TAG = SoundboardListFragment.FindSoundboardsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        FindSoundboardsTask(Context context) {
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

            SoundboardDao soundboardDao = SoundboardDao.getInstance(appContext);

            Log.d(TAG, "Loading soundboards...");

            ImmutableList<SoundboardWithSounds> res = soundboardDao.findAllWithSounds();

            if (res.isEmpty()) {
                Log.d(TAG, "No soundboards found.");
                Log.d(TAG, "Insert some dummy data...");

                soundboardDao.insertDummyData();

                Log.d(TAG, "...and load the soundboards again");
                res = soundboardDao.findAllWithSounds();
            }

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
            initSoundboardItemAdapter(soundboards);
        }
    }

}
