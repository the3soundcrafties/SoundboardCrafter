package de.soundboardcrafter.activity.soundboard.play;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.Collection;
import java.util.Map;

import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundboardWithSounds;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter for a soundboard item.
 */
class SoundboardItemAdapter extends BaseAdapter {
    private final SoundboardItemRow.MediaPlayerServiceCallback mediaPlayerServiceCallback;
    private final SoundboardWithSounds soundboard;
    private static final String TAG = SoundboardItemAdapter.class.getName();

    SoundboardItemAdapter(@NonNull SoundboardItemRow.MediaPlayerServiceCallback mediaPlayerServiceCallback,
                          @NonNull SoundboardWithSounds soundboard) {
        this.soundboard = checkNotNull(soundboard, "soundboard is null");
        this.mediaPlayerServiceCallback = checkNotNull(mediaPlayerServiceCallback, "mediaPlayerServiceCallback!=null");
    }

    /**
     * Returns the soundboard.
     */
    public SoundboardWithSounds getSoundboard() {
        return soundboard;
    }

    /**
     * If there are already sounds in the soundboard with one of these IDs, replace
     * them with the respective updates.
     * stop sound if not in soundboard anymore
     */
    void updateSounds(Map<Sound, Boolean> sounds) {
        for (Map.Entry<Sound, Boolean> soundEntry : sounds.entrySet()) {
            updateSound(soundEntry.getKey());
            if (!soundEntry.getValue()) {
                mediaPlayerServiceCallback.stopPlaying(soundboard.getSoundboard(), soundEntry.getKey());
            }
        }
    }


    void updateSounds(Collection<Sound> sounds) {
        for (Sound sound : sounds) {
            updateSound(sound);
        }
    }

    /**
     * If there is already a sound in the soundboard with this ID, replace
     * it with the given update.
     */
    private void updateSound(Sound update) {
        for (int i = 0; i < soundboard.getSounds().size(); i++) {
            Sound oldSound = soundboard.getSounds().get(i);
            if (update.getId().equals(oldSound.getId())) {
                mediaPlayerServiceCallback.stopPlaying(
                        soundboard.getSoundboard(), oldSound, false);
                soundboard.setSound(i, update);
                break;
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Removes the sound from the adapter. Should the sound currently be playing,
     * stop it before the sound is removed.
     */
    void remove(int position) {
        Sound sound = soundboard.getSounds().get(position);
        mediaPlayerServiceCallback.stopPlaying(
                soundboard.getSoundboard(), sound, false);

        soundboard.removeSound(position);

        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return soundboard.getSounds().size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Sound getItem(int position) {
        return soundboard.getSounds().get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof SoundboardItemRow)) {
            convertView = new SoundboardItemRow(parent.getContext());
        }

        SoundboardItemRow itemRow = (SoundboardItemRow) convertView;

        Sound sound = soundboard.getSounds().get(position);

        mediaPlayerServiceCallback.setOnPlayingStopped(soundboard.getSoundboard(), sound,
                this::notifyDataSetChanged);

        itemRow.setSound(soundboard.getSoundboard(), sound, mediaPlayerServiceCallback);

        return convertView;
    }


}