package de.soundboardcrafter.activity.soundboard.play;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
class SoundboardItemAdapter extends BaseAdapter {
    private final SoundBoardItemRow.MediaPlayerServiceCallback mediaPlayerServiceCallback;
    private Soundboard soundboard;

    SoundboardItemAdapter(@Nonnull SoundBoardItemRow.MediaPlayerServiceCallback mediaPlayerServiceCallback, @Nonnull Soundboard soundboard) {
        this.soundboard = checkNotNull(soundboard, "soundboard is null");
        this.mediaPlayerServiceCallback = checkNotNull(mediaPlayerServiceCallback, "mediaPlayerServiceCallback!=null");
    }


    /**
     * Sets the soundboard.
     */
    public void setSoundboard(Soundboard soundboard) {
        this.soundboard = soundboard;
        notifyDataSetChanged();
    }

    /**
     * Returns the soundboard.
     */
    public Soundboard getSoundboard() {
        return soundboard;
    }

    /**
     * If there are already sounds in the soundboard with one of these IDs, replace
     * them with the respective updates.
     */
    void updateSounds(Collection<Sound> updates) {
        for (Sound update : updates) {
            updateSound(update);
        }
    }

    /**
     * If there is already a sound in the soundboard with this ID, replace
     * it with the given update.
     */
    private void updateSound(Sound update) {
        for (int i = 0; i < soundboard.getSounds().size(); i++) {
            final Sound oldSound = soundboard.getSounds().get(i);
            if (update.getId().equals(oldSound.getId())) {
                if (mediaPlayerServiceCallback.shouldBePlaying(soundboard, oldSound)) {
                    mediaPlayerServiceCallback.stopPlaying(soundboard, oldSound);
                }
                soundboard.setSound(i, update);

                break;
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Removes all sounds from the adapter. Should a sound currently be playing,
     * stop it before the sound is removed.
     */
    void clear() {
        for (Sound sound : soundboard.getSounds()) {
            if (mediaPlayerServiceCallback.shouldBePlaying(soundboard, sound)) {
                mediaPlayerServiceCallback.stopPlaying(soundboard, sound);
            }
        }
        soundboard.clearSounds();
        notifyDataSetChanged();
    }

    /**
     * Removes the sound from the adapter. Should the sound currently be playing,
     * stop it before the sound is removed.
     */
    void remove(int position) {
        Sound sound = soundboard.getSounds().get(position);
        if (mediaPlayerServiceCallback.shouldBePlaying(soundboard, sound)) {
            mediaPlayerServiceCallback.stopPlaying(soundboard, sound);
        }

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
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        Sound sound = soundboard.getSounds().get(position);
        if (convertView == null) {
            convertView = new SoundBoardItemRow(parent.getContext());
        }
        // We can now safely cast and set the data
        ((SoundBoardItemRow) convertView).setSound(soundboard, sound, mediaPlayerServiceCallback);
        return convertView;
    }
}