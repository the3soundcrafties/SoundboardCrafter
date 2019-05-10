package de.soundboardcrafter.activity.audiofile.list;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.sound.edit.audiofile.list.AudiofileListSoundEditActivity;
import de.soundboardcrafter.activity.sound.edit.common.SoundEditFragment;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.model.Sound;

/**
 * Shows Soundboard in a Grid
 */
public class AudioFileListFragment extends Fragment implements AudioFileItemRow.Callback {
    private static final String TAG = AudioFileListFragment.class.getName();

    private static final int REQUEST_EDIT_SOUND = 1;

    private ListView listView;
    private AudioFileListItemAdapter adapter;

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_audiofile_list,
                container, false);
        listView = rootView.findViewById(R.id.listview_audiofile);

        initAudioFileListItemAdapter();
        new FindAudioFileTask(getContext()).execute();

        return rootView;
    }


    @Override
    @UiThread
    public void onEditAudioFile(AudioModelAndSound audioModelAndSound) {
        final Sound sound;
        if (audioModelAndSound.getSound() == null) {
            // Create and save new sound
            sound = new Sound(audioModelAndSound.getAudioModel().getPath(),
                    audioModelAndSound.getAudioModel().getName());
            new AudioFileListFragment.SaveNewSoundTask(getActivity(), sound).execute();
        } else {
            // Use existing sound
            sound = audioModelAndSound.getSound();
        }

        Log.d(TAG, "Editing sound for audio file " +
                audioModelAndSound.getAudioModel().getPath());

        Intent intent = AudiofileListSoundEditActivity.newIntent(getContext(), sound);
        startActivityForResult(intent, REQUEST_EDIT_SOUND);
    }

    @Override
    @UiThread
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_EDIT_SOUND:
                Log.d(TAG, "Editing sound " + this + ": Returned from sound edit fragment with OK");

                final UUID soundId = UUID.fromString(
                        data.getStringExtra(SoundEditFragment.EXTRA_SOUND_ID));

                // The sound name may have been changed.
                new FindAudioFileTask(getContext()).execute();

                break;
        }
    }

    @UiThread
    private void initAudioFileListItemAdapter() {
        adapter = new AudioFileListItemAdapter(this);
        listView.setAdapter(adapter);
        updateUI();
    }

    @UiThread
    private void setAudioFiles(ImmutableList<AudioModelAndSound> audioFilesAndSounds) {
        List<AudioModelAndSound> list = Lists.newArrayList(audioFilesAndSounds);
        list.sort((s1, s2) ->
                s1.getAudioModel().getName().compareTo(s2.getAudioModel().getName()));
        adapter.setAudioFiles(list);
    }


    @UiThread
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * A background task, used to retrieve audio files from the file system
     * and corresponding sounds from the database.
     */
    class FindAudioFileTask extends AsyncTask<Void, Void, ImmutableList<AudioModelAndSound>> {
        private final String TAG = FindAudioFileTask.class.getName();

        private final WeakReference<Context> appContextRef;

        FindAudioFileTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected ImmutableList<AudioModelAndSound> doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            AudioLoader audioLoader = new AudioLoader();

            Log.d(TAG, "Loading audio files from file system...");

            ImmutableList<AudioModel> audioModels = audioLoader.getAllAudioFromDevice(appContext);

            Log.d(TAG, "Audio files loaded.");

            Log.d(TAG, "Loading sounds from database...");

            SoundDao soundDao = SoundDao.getInstance(appContext);
            Map<String, Sound> soundMap = soundDao.findAllByPath();

            Log.d(TAG, "Sounds loaded.");

            ImmutableList.Builder<AudioModelAndSound> res = ImmutableList.builder();
            for (AudioModel audioModel : audioModels) {
                res.add(new AudioModelAndSound(audioModel, soundMap.get(audioModel.getPath())));
            }

            return res.build();
        }

        @Override
        @UiThread
        protected void onPostExecute(ImmutableList<AudioModelAndSound> audioFilesAndSounds) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            setAudioFiles(audioFilesAndSounds);
        }
    }

    /**
     * A background task, used to save the sound
     */
    class SaveNewSoundTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = SaveNewSoundTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final Sound sound;

        SaveNewSoundTask(Context context, Sound sound) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.sound = sound;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Saving sound " + sound);

            // TODO If name already exists, choose another name - like ... "Name1", "Name2"

            SoundDao.getInstance(appContext).insertSound(sound);

            return null;
        }
    }
}
