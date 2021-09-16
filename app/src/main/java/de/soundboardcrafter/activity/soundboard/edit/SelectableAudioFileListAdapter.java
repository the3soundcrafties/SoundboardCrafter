package de.soundboardcrafter.activity.soundboard.edit;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.google.common.collect.ImmutableList;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.audiofile.list.AbstractAudioFileListAdapter;
import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.AudioModelAndSound;
import de.soundboardcrafter.model.audio.BasicAudioModel;

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

    Iterable<BasicAudioModel> getBasicAudioModelsSelected() {
        final ImmutableList.Builder<BasicAudioModel> res = ImmutableList.builder();
        for (SelectableModel<AbstractAudioFolderEntry> audioFolderEntry :
                copyAudioFolderEntries()) {
            if (audioFolderEntry.isSelected()
                    && audioFolderEntry.getModel() instanceof AudioModelAndSound) {
                res.add(((AudioModelAndSound) audioFolderEntry.getModel()).getAudioModel()
                        .toBasic());
            }
        }

        return res.build();
    }

    ImmutableList<AbstractAudioLocation> getAudioLocations(boolean selected) {
        final ImmutableList.Builder<AbstractAudioLocation> res = ImmutableList.builder();
        for (SelectableModel<AbstractAudioFolderEntry> audioFolderEntry :
                copyAudioFolderEntries()) {
            if (audioFolderEntry.isSelected() == selected
                    && audioFolderEntry.getModel() instanceof AudioModelAndSound) {
                res.add(((AudioModelAndSound) audioFolderEntry.getModel()).getAudioModel()
                        .getAudioLocation());
            }
        }

        return res.build();
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
        // When using convertView there are seldom cases where the user
        // cannot click the checkbox. So we always create a new view.

        convertView = new SoundboardEditSelectableAudioRow(parent.getContext());
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