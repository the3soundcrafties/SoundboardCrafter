package de.soundboardcrafter.activity.soundboard.list;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.activity.soundboard.edit.SoundboardCreateActivity;
import de.soundboardcrafter.activity.soundboard.edit.SoundboardEditActivity;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Shows Soundboards in a list
 */
public class SoundboardListFragment extends Fragment
        implements SoundEventListener {
    private static final String TAG = SoundboardListFragment.class.getName();
    private static final int CREATE_SOUNDBOARD_REQUEST_CODE = 25;
    private static final int EDIT_SOUNDBOARD_REQUEST_CODE = 26;
    private Button addNewSoundboard;
    /**
     * @param editSoundRequestCode request code used whenever a sound edit
     * fragment is started from this activity
     */
    private int editSoundRequestCode;

    private ListView listView;
    private SoundboardListItemAdapter adapter;

    /**
     * Creates a <code>SoundboardListFragment</code>.
     */
    public static SoundboardListFragment createFragment() {
        SoundboardListFragment fragment = new SoundboardListFragment();
        return fragment;
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard_list,
                container, false);
        listView = rootView.findViewById(R.id.listview_soundboard);
        addNewSoundboard = rootView.findViewById(R.id.new_soundboard);
        addNewSoundboard.setOnClickListener(e -> {
            startActivityForResult(SoundboardCreateActivity.newIntent(getContext()), CREATE_SOUNDBOARD_REQUEST_CODE);
        });
        initSoundboardItemAdapter();
        registerForContextMenu(listView);

        new SoundboardListFragment.FindSoundboardsTask(getContext()).execute();

        return rootView;
    }

    @Override
    @UiThread
    public void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.fragment_soundboard_list_context, menu);

        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        SoundboardListItemRow itemRow = (SoundboardListItemRow) adapterContextMenuInfo.targetView;

        menu.setHeaderTitle(itemRow.getSoundboardWithSounds().getName());
    }

    @Override
    @UiThread
    public boolean onContextItemSelected(@Nonnull MenuItem item) {
        if (!getUserVisibleHint()) {
            // The wrong fragment got the event.
            // See https://stackoverflow.com/questions/9753213/wrong-fragment-in-viewpager-receives-oncontextitemselected-call
            return false; // Pass the event to the next fragment
        }
        AdapterView.AdapterContextMenuInfo menuInfo =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        SoundboardListItemRow itemRow = (SoundboardListItemRow) menuInfo.targetView;
        SoundboardWithSounds soundboardWithSounds = itemRow.getSoundboardWithSounds();
        switch (item.getItemId()) {
            case R.id.context_menu_edit_soundboard:
                Intent intent = SoundboardEditActivity.newIntent(getActivity(), soundboardWithSounds.getSoundboard());
                startActivityForResult(intent, EDIT_SOUNDBOARD_REQUEST_CODE);
                return true;
            case R.id.context_menu_remove_soundboard:
                new RemoveSoundboardTask(getActivity(), soundboardWithSounds.getId()).execute();
                adapter.remove(soundboardWithSounds);
                return true;
            default:
                return false;
        }
    }


    @Override
    @UiThread
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case CREATE_SOUNDBOARD_REQUEST_CODE:
                Log.d(TAG, "created new soundboard " + this);
                new SoundboardListFragment.FindSoundboardsTask(getContext()).execute();
                break;
            case EDIT_SOUNDBOARD_REQUEST_CODE:
                Log.d(TAG, "update new soundboard " + this);
                new SoundboardListFragment.FindSoundboardsTask(getContext()).execute();
                break;
        }
    }

    @Override
    public void soundChanged(UUID soundId) {
        // The sound NAME may have been changed.
        new SoundboardListFragment.FindSoundboardsTask(getContext()).execute();
    }

    @UiThread
    private void initSoundboardItemAdapter() {
        adapter = new SoundboardListItemAdapter();
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    private void setSoundboards(ImmutableList<SoundboardWithSounds> soundboards) {
        List<SoundboardWithSounds> list = Lists.newArrayList(soundboards);
        list.sort((s1, s2) -> s1.getName().compareTo(s2.getName()));
        adapter.setSoundboards(list);
    }

    @UiThread
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * A background task, used to remove soundboard with the given indexes from the soundboard
     */
    class RemoveSoundboardTask extends AsyncTask<Integer, Void, Void> {
        private final String TAG = RemoveSoundboardTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private UUID soundboardId;

        RemoveSoundboardTask(Context context, UUID soundboardId) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundboardId = soundboardId;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Integer... indexes) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            SoundboardDao soundboardDao = SoundboardDao.getInstance(appContext);
            soundboardDao.unlinkAllSounds(soundboardId);
            soundboardDao.remove(soundboardId);
            return null;
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
            setSoundboards(soundboards);
        }
    }

}
