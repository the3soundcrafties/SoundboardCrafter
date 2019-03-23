package de.soundboardcrafter.activity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
class SoundboardItemAdapter extends BaseAdapter {
    private Soundboard soundboard;
    private final MediaPlayerManagerService mediaPlayerManagerService;

    SoundboardItemAdapter(@Nonnull MediaPlayerManagerService mediaPlayerManagerService, @Nonnull Soundboard soundboard) {
        this.soundboard = checkNotNull(soundboard, "soundboard is null");
        this.mediaPlayerManagerService = checkNotNull(mediaPlayerManagerService, "mediaPlayerManagerService!=null");
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
     * Removes all sounds from the adapter. Should a sound currently be playing,
     * stop it before the sound is removed.
     */
    void clear() {
        for (Sound sound : soundboard.getSounds()) {
            if (mediaPlayerManagerService.shouldBePlaying(soundboard, sound)) {
                mediaPlayerManagerService.stopSound(soundboard, sound);
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

        if (mediaPlayerManagerService.shouldBePlaying(soundboard, sound)) {
            mediaPlayerManagerService.stopSound(soundboard, sound);
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
        ((SoundBoardItemRow) convertView).setSound(soundboard, sound, mediaPlayerManagerService);


        return convertView;
    }
}
