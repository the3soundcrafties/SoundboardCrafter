package de.soundboardcrafter.activity.soundboard.edit;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.audiofile.list.AbstractAudioFileListAdapter;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;

/**
 * Adapter to choose from a list of audio files (and audio folders).
 */
class SelectableAudioFileListAdapter
        extends AbstractAudioFileListAdapter<SelectableModel<AbstractAudioFolderEntry>> {

    /**
     * Creates an adapter that's initially empty
     */
    SelectableAudioFileListAdapter() {
    }

    @Override
    protected AbstractAudioFolderEntry asAbstractAudioFolderEntry(
            SelectableModel<AbstractAudioFolderEntry> entry) {
        return entry.getModel();
    }

    @Override
    @UiThread
    public SoundboardEditSelectableAudioRow getAudioFileRow(
            SelectableModel<AbstractAudioFolderEntry> audioModelAndSound,
            boolean isPlaying,
            @Nullable View convertView,
            ViewGroup parent) {
        if (!(convertView instanceof SoundboardEditSelectableAudioRow)) {
            convertView = new SoundboardEditSelectableAudioRow(parent.getContext());
        }
        SoundboardEditSelectableAudioRow itemRow = (SoundboardEditSelectableAudioRow) convertView;

        configureItemRow(itemRow, audioModelAndSound, isPlaying);

        return itemRow;
    }

    private void configureItemRow(@NonNull SoundboardEditSelectableAudioRow itemRow,
                                  SelectableModel<AbstractAudioFolderEntry> audioModelAndSound,
                                  boolean isPlaying) {
        itemRow.setAudioFile(audioModelAndSound);
        itemRow.setImage(isPlaying ? R.drawable.ic_stop : R.drawable.ic_play);
    }
}