package de.soundboardcrafter.activity.soundboard.list;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.sound.event.SoundEventListener;
import de.soundboardcrafter.activity.soundboard.edit.SoundboardCreateActivity;
import de.soundboardcrafter.activity.soundboard.edit.SoundboardEditActivity;
import de.soundboardcrafter.activity.soundboard.play.SoundboardPlayActivity;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.dao.TutorialDao;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Shows Soundboards in a list
 */
public class SoundboardListFragment extends Fragment
        implements SoundEventListener {
    private static final String TAG = SoundboardListFragment.class.getName();

    private static final String EXTRA_SOUNDBOARD_ID = "SoundboardId";

    /**
     * Request code used whenever the soundboard playing view
     * is started from this activity
     */
    private static final int SOUNDBOARD_PLAY_REQUEST_CODE = 1;

    private static final int CREATE_SOUNDBOARD_REQUEST_CODE = 25;
    private static final int EDIT_SOUNDBOARD_REQUEST_CODE = 26;

    private @Nullable
    SoundEventListener soundEventListenerActivity;

    private ListView listView;
    private SoundboardListItemAdapter adapter;

    /**
     * Creates a <code>SoundboardListFragment</code>.
     */
    public static SoundboardListFragment createFragment() {
        return new SoundboardListFragment();
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard_list,
                container, false);
        listView = rootView.findViewById(R.id.list_view_soundboard);
        Button addNewSoundboard = rootView.findViewById(R.id.new_soundboard);
        addNewSoundboard.setOnClickListener(e ->
                startActivityForResult(
                        SoundboardCreateActivity.newIntent(
                                getContext()), CREATE_SOUNDBOARD_REQUEST_CODE));
        initSoundboardItemAdapter();
        registerForContextMenu(listView);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            SoundboardWithSounds soundboard = adapter.getItem(position);

            Intent intent = new Intent(getContext(), SoundboardPlayActivity.class);
            intent.putExtra(EXTRA_SOUNDBOARD_ID, soundboard.getId().toString());

            startActivityForResult(intent, SOUNDBOARD_PLAY_REQUEST_CODE);
        });

        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            new SoundboardListFragment.FindSoundboardsTask(requireContext()).execute();
        } // otherwise we will receive an event later

        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof SoundEventListener) {
            soundEventListenerActivity = (SoundEventListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        soundEventListenerActivity = null;
    }

    @Override
    @UiThread
    public void onCreateContextMenu(@Nonnull ContextMenu menu, @Nonnull View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = requireActivity().getMenuInflater();
        inflater.inflate(R.menu.fragment_soundboard_list_context, menu);

        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        SoundboardListItemRow itemRow = (SoundboardListItemRow) adapterContextMenuInfo.targetView;

        menu.setHeaderTitle(itemRow.getSoundboardWithSounds().getName());

        @Nullable final Context context = getContext();
        if (context != null) {
            TutorialDao.getInstance(context).check(TutorialDao.Key.SOUNDBOARD_LIST_CONTEXT_MENU);
        }
    }

    @Override
    @UiThread
    public boolean onContextItemSelected(@Nonnull MenuItem item) {
        if (!getUserVisibleHint()) {
            // The wrong fragment got the event.
            // See https://stackoverflow.com/questions/9753213/wrong-fragment-in-viewpager
            // -receives-oncontextitemselected-call
            return false; // Pass the event to the next fragment
        }
        AdapterView.AdapterContextMenuInfo menuInfo =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        SoundboardListItemRow itemRow = (SoundboardListItemRow) menuInfo.targetView;
        SoundboardWithSounds soundboardWithSounds = itemRow.getSoundboardWithSounds();
        final int id = item.getItemId();
        if (id == R.id.context_menu_edit_soundboard) {
            Intent intent = SoundboardEditActivity
                    .newIntent(getActivity(), soundboardWithSounds.getSoundboard());
            startActivityForResult(intent, EDIT_SOUNDBOARD_REQUEST_CODE);
            return true;
        } else if (id == R.id.context_menu_remove_soundboard) {
            new RemoveSoundboardTask(requireActivity(), soundboardWithSounds).execute();
            adapter.remove(soundboardWithSounds);
            fireSomethingMightHaveChanged();
            return true;
        } else {
            return false;
        }
    }

    private void fireSomethingMightHaveChanged() {
        if (soundEventListenerActivity != null) {
            soundEventListenerActivity.somethingMightHaveChanged();
        }
    }


    @Override
    @UiThread
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case SOUNDBOARD_PLAY_REQUEST_CODE:
                fireSomethingMightHaveChanged();
                break;
            case CREATE_SOUNDBOARD_REQUEST_CODE:
                Log.d(TAG, "created new soundboard " + this);
                new SoundboardListFragment.FindSoundboardsTask(requireContext()).execute();
                break;
            case EDIT_SOUNDBOARD_REQUEST_CODE:
                Log.d(TAG, "update new soundboard " + this);
                new SoundboardListFragment.FindSoundboardsTask(requireContext()).execute();
                break;
        }
    }

    @Override
    public void somethingMightHaveChanged() {
        @Nullable Context context = getContext();
        if (context == null) {
            return;
        }

        new SoundboardListFragment.FindSoundboardsTask(requireContext()).execute();
    }

    @Override
    public void soundChanged(UUID soundId) {
        @Nullable Context context = getContext();
        if (context == null) {
            return;
        }

        new SoundboardListFragment.FindSoundboardsTask(requireContext()).execute();
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
        list.sort(Comparator.comparing(SoundboardWithSounds::getCollationKey));
        adapter.setSoundboards(list);
        if (getUserVisibleHint()) {
            adapter.markAsRightPlaceToShowTutorialHints();
        }
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
    static class RemoveSoundboardTask extends AsyncTask<Integer, Void, Void> {
        private final WeakReference<Context> appContextRef;
        private final UUID soundboardId;

        RemoveSoundboardTask(Context context, SoundboardWithSounds soundboard) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            soundboardId = soundboard.getId();
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
            soundboardDao.remove(soundboardId);
            return null;
        }

    }

    /**
     * A background task, used to retrieve soundboards from the database.
     */
    class FindSoundboardsTask
            extends AsyncTask<Void, Void, ImmutableList<SoundboardWithSounds>> {
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

            ImmutableList<SoundboardWithSounds> res = soundboardDao.findAllWithSounds(null);

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
