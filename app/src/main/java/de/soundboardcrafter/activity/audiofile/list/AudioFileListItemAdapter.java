package de.soundboardcrafter.activity.audiofile.list;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.audiofile.list.AbstractAudioFileListAdapter;
import de.soundboardcrafter.activity.common.audiofile.list.AudioItem;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.AudioModelAndSound;

/**
 * Adapter for the list of audio files (and audio folders).
 */
class AudioFileListItemAdapter extends AbstractAudioFileListAdapter<AbstractAudioFolderEntry> {
    private final AudioItem.Callback callback;

    /**
     * Creates an adapter that's initially empty
     */
    AudioFileListItemAdapter(AudioItem.Callback callback) {
        this.callback = callback;
    }

    @Override
    protected AbstractAudioFolderEntry asAbstractAudioFolderEntry(AbstractAudioFolderEntry entry) {
        return entry;
    }

    @Override
    @UiThread
    public AudioItem getAudioFileRow(AbstractAudioFolderEntry audioModelAndSound,
                                     boolean isPlaying,
                                     @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof AudioItem)) {
            convertView = new AudioItem(parent.getContext());
        }
        AudioItem itemRow = (AudioItem) convertView;

        configureItemRow(itemRow, (AudioModelAndSound) audioModelAndSound, isPlaying);

        return itemRow;
    }

    private void configureItemRow(@NonNull AudioItem itemRow,
                                  AudioModelAndSound audioModelAndSound,
                                  boolean isPlaying) {
        itemRow.setAudioFile(audioModelAndSound, callback);
        itemRow.setImage(isPlaying ? R.drawable.ic_stop : R.drawable.ic_play);
    }
}