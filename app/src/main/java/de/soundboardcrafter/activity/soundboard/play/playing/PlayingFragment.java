package de.soundboardcrafter.activity.soundboard.play.playing;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.audiofile.list.AudioItem;
import de.soundboardcrafter.activity.common.audioloader.AudioLoader;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.sound.edit.audiofile.list.AudiofileListSoundEditActivity;
import de.soundboardcrafter.activity.sound.edit.common.SoundEditFragment;
import de.soundboardcrafter.activity.soundboard.play.common.ISoundboardPlayActivity;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.audio.AudioModelAndSound;
import de.soundboardcrafter.model.audio.FullAudioModel;

/**
 * Shows all sounds currently playing. Shown first before the
 * {@link de.soundboardcrafter.activity.soundboard.play.soundboard.SoundboardFragment}s.
 */
public class PlayingFragment extends Fragment implements
        ServiceConnection, MediaPlayerService.OnAnyPlayingStartedOrStopped,
        AudioItem.Callback {
    /**
     * Request code used whenever this activity starts a sound edit
     * fragment
     */
    private static final int EDIT_SOUND_REQUEST_CODE = 1;

    private ListView listView;

    private PlayingListItemAdapter adapter;
    private MediaPlayerService mediaPlayerService;

    @Nullable
    private ISoundboardPlayActivity hostingActivity;


    /**
     * Creates a <code>PlayingFragment</code>.
     */
    @NonNull
    public static PlayingFragment newInstance() {
        return new PlayingFragment();
    }

    @Override
    @UiThread
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MediaPlayerService.Binder b = (MediaPlayerService.Binder) binder;
        mediaPlayerService = b.getService();
        // As soon the media player service is connected, the sounds currently playing can
        // be shown

        mediaPlayerService.setOnAnyPlayingStartedOrStopped(this);
        loadSoundsCurrentlyPlaying();
    }

    @Override
    @UiThread
    public void onServiceDisconnected(ComponentName name) {
        adapter.setAudiosPlaying(ImmutableList.of());
    }

    @Override
    @UiThread
    public void onBindingDied(ComponentName name) {
        adapter.setAudiosPlaying(ImmutableList.of());
    }

    @Override
    @UiThread
    public void onNullBinding(ComponentName name) {
        adapter.setAudiosPlaying(ImmutableList.of());
    }

    private void bindService() {
        Intent intent = new Intent(requireActivity(), MediaPlayerService.class);
        requireActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        requireActivity().startService(intent);

        // TODO Necessary?! Also done in onResume()
        bindService();
    }

    @Override
    @UiThread
    public void onPause() {
        super.onPause();

        requireActivity().unbindService(this);
    }

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_playing,
                container, false);

        listView = rootView.findViewById(R.id.list_view_playing);

        initAdapter();

        listView.setOnItemClickListener(
                (parent, view, position, id) -> onClickAudioItem(position));

        return rootView;
    }

    @UiThread
    private void initAdapter() {
        adapter = new PlayingListItemAdapter(this);
        listView.setAdapter(adapter);
        updateUI();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof ISoundboardPlayActivity) {
            hostingActivity = (ISoundboardPlayActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        hostingActivity = null;
    }

    @Override
    public void playingStartedOrStopped() {
        loadSoundsCurrentlyPlaying();
    }

    @UiThread
    private void onClickAudioItem(int position) {
        MediaPlayerService service = getService();
        if (service == null) {
            // Should never happen.
            return;
        }

        AudioModelAndSound audioModelAndSound = adapter.getItem(position);

        @Nullable final Sound sound = audioModelAndSound.getSound();
        if (sound != null) {
            service.stopPlaying(sound, true);
            // This will call playingStartedOrStopped();
        }
    }

    private MediaPlayerService getService() {
        if (mediaPlayerService == null) {
            // TODO Necessary?! Also done in onResume()
            bindService();
        }
        return mediaPlayerService;
    }

    @Override
    @UiThread
    public void onEdit(@NonNull AudioModelAndSound audioModelAndSound) {
        final Sound sound = audioModelAndSound.getSound();

        if (audioModelAndSound.getSound() == null) {
            return;
            // Should never happen.
        }

        Intent intent = AudiofileListSoundEditActivity.newIntent(requireContext(), sound);
        startActivityForResult(intent, EDIT_SOUND_REQUEST_CODE);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    @UiThread
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case EDIT_SOUND_REQUEST_CODE:
                @Nullable String soundIdString =
                        data != null ?
                                data.getStringExtra(SoundEditFragment.EXTRA_SOUND_ID) : null;
                if (soundIdString != null) {
                    final UUID soundId = UUID.fromString(soundIdString);
                    // Sound file has been changed
                    if (hostingActivity != null) {
                        hostingActivity.soundChanged(soundId);
                    }
                    loadSoundsCurrentlyPlaying();
                } else {
                    // Sound file has been deleted
                    if (hostingActivity != null) {
                        hostingActivity.soundsDeleted();
                    }
                }
                break;
        }
    }

    public void loadSoundsCurrentlyPlaying() {
        Collection<UUID> soundIds = mediaPlayerService.getSoundIdsActivelyPlaying();
        new LoadSoundsCurrentlyPlayingTask(this, soundIds).execute();
    }


    @UiThread
    private void updateUI() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    @UiThread
    // Called especially when the edit activity returns.
    public void onResume() {
        super.onResume();

        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        bindService();
    }


    /**
     * A background task, used to retrieve the audio files (and their sounds)
     * that are currently playing.
     */
    static class LoadSoundsCurrentlyPlayingTask extends AsyncTask<Void, Void,
            ImmutableList<AudioModelAndSound>> {
        @NonNull
        private final WeakReference<PlayingFragment> fragmentRef;

        private final ImmutableCollection<UUID> soundIds;

        LoadSoundsCurrentlyPlayingTask(PlayingFragment fragment,
                                       Collection<UUID> soundIds) {
            super();
            this.soundIds = ImmutableList.copyOf(soundIds);
            fragmentRef = new WeakReference<>(fragment);
        }

        @Nullable
        @Override
        @WorkerThread
        protected ImmutableList<AudioModelAndSound> doInBackground(Void... voids) {
            @Nullable PlayingFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                cancel(true);
                return null;
            }

            return loadAudios(fragment.getContext());
        }

        private ImmutableList<AudioModelAndSound> loadAudios(Context context) {
            ArrayList<AudioModelAndSound> res = new ArrayList<>(soundIds.size());
            for (UUID soundId : soundIds) {
                @Nullable final AudioModelAndSound audio = loadAudio(context, soundId);
                if (audio != null) {
                    res.add(audio);
                }
            }

            res.sort(AudioModelAndSound.SortOrder.BY_NAME.getComparator());

            return ImmutableList.copyOf(res);
        }

        @Nullable
        private AudioModelAndSound loadAudio(Context context, UUID soundId) {
            @Nullable
            Sound sound = SoundDao.getInstance(context).find(soundId);
            if (sound == null) {
                return null;
            }

            try {
                @Nullable
                FullAudioModel audioModel =
                        new AudioLoader().getAudio(context, sound.getAudioLocation());
                if (audioModel == null) {
                    // Permission problem? We do not want to deal with this here.
                    return null;
                }

                return new AudioModelAndSound(audioModel, sound);
            } catch (RuntimeException e) {
                // Perhaps in some weird case when the user concurrently removes a permission...
                return null;
            }
        }

        @Override
        @UiThread
        protected void onPostExecute(
                ImmutableList<AudioModelAndSound> audioModelsAndSounds) {
            @Nullable PlayingFragment fragment = fragmentRef.get();
            if (fragment == null || fragment.getContext() == null) {
                // fragment (or context) no longer available, I guess that result
                // will be of no use to anyone
                return;
            }
            fragment.adapter.setAudiosPlaying(audioModelsAndSounds);
        }
    }
}
