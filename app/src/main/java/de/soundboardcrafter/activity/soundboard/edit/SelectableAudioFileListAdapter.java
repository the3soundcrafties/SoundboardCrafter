package de.soundboardcrafter.activity.soundboard.edit;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.audiofile.list.AudioSubfolderRow;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.AudioFolder;
import de.soundboardcrafter.model.audio.AudioModelAndSound;

/**
 * Adapter to choose from a list of audio files (and audio folders).
 */
class SelectableAudioFileListAdapter extends BaseAdapter {
    private final List<SelectableModel<AbstractAudioFolderEntry>> audioFolderEntries;

    /**
     * Is an audio file currently playing (as a preview)? Then this is
     * its position in the list.
     */
    private Integer positionPlaying;

    /**
     * Creates an adapter that's initially empty
     */
    SelectableAudioFileListAdapter() {
        audioFolderEntries = new ArrayList<>();
    }

    void setAudioFolderEntries(
            Collection<SelectableModel<AbstractAudioFolderEntry>> audioFolderEntries) {
        this.audioFolderEntries.clear();
        this.audioFolderEntries.addAll(audioFolderEntries);
        positionPlaying = null;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return audioFolderEntries.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public SelectableModel<AbstractAudioFolderEntry> getItem(int position) {
        return audioFolderEntries.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        SelectableModel<AbstractAudioFolderEntry> entry = audioFolderEntries.get(position);
        if (entry.getModel() instanceof AudioModelAndSound) {
            return getAudioFileRow((SelectableModel<AbstractAudioFolderEntry>) entry,
                    isPlaying(position), convertView,
                    parent);
        }

        // Selection of folders is not supported
        return getAudioSubfolderRow((AudioFolder) entry.getModel(), convertView, parent);
    }

    boolean isPlaying(int position) {
        return positionPlaying != null && positionPlaying == position;
    }

    @UiThread
    private SoundboardEditSelectableAudioRow getAudioFileRow(
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

    private void configureItemRow(SoundboardEditSelectableAudioRow itemRow,
                                  SelectableModel<AbstractAudioFolderEntry> audioModelAndSound,
                                  boolean isPlaying) {
        itemRow.setAudioFile(audioModelAndSound);
        itemRow.setImage(isPlaying ? R.drawable.ic_stop : R.drawable.ic_play);
    }

    @UiThread
    private View getAudioSubfolderRow(AudioFolder audioFolder,
                                      @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof AudioSubfolderRow)) {
            convertView = new AudioSubfolderRow(parent.getContext());
        }
        AudioSubfolderRow itemRow = (AudioSubfolderRow) convertView;

        configureItemRow(itemRow, audioFolder);
        return convertView;
    }

    private void configureItemRow(AudioSubfolderRow itemRow, AudioFolder audioFolder) {
        itemRow.setData(audioFolder);
        // FIXME set checkbox invisible
    }

    void setPositionPlaying(Integer positionPlaying) {
        this.positionPlaying = positionPlaying;
        notifyDataSetChanged();
    }
}