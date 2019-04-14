package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import de.soundboardcrafter.R;

/**
 * Shows Soundboard in a Grid
 */
public class AudioFileListFragment extends Fragment {
    private static final String TAG = AudioFileListFragment.class.getName();
    private ListView listView;
    private AudioFileListItemAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new FindAudioFileTask(getContext()).execute();
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_audiofile_list,
                container, false);
        listView = rootView.findViewById(R.id.listview_audiofile);

        return rootView;
    }


    @UiThread
    private void initAudioFileListItemAdapter(ImmutableList<AudioModel> audioFiles) {
        List<AudioModel> list = Lists.newArrayList(audioFiles);
        list.sort((s1, s2) -> s1.getName().compareTo(s2.getName()));
        adapter = new AudioFileListItemAdapter(list);
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
    class FindAudioFileTask extends AsyncTask<Void, Void, ImmutableList<AudioModel>> {
        private final String TAG = FindAudioFileTask.class.getName();

        private final WeakReference<Context> appContextRef;

        FindAudioFileTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected ImmutableList<AudioModel> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            AudioLoader audioLoader = new AudioLoader();

            Log.d(TAG, "Loading sounds...");

            ImmutableList<AudioModel> res = audioLoader.getAllAudioFromDevice(appContext);

            Log.d(TAG, "Sounds loaded.");

            return res;
        }

        @Override
        @UiThread
        protected void onPostExecute(ImmutableList<AudioModel> audioFiles) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            initAudioFileListItemAdapter(audioFiles);
        }
    }

}
