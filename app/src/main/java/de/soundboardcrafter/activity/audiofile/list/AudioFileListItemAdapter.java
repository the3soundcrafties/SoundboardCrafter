package de.soundboardcrafter.activity.audiofile.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.AudioFolder;
import de.soundboardcrafter.model.audio.AudioModelAndSound;

/**
 * Adapter for the list of audio files (and audio folders).
 */
class AudioFileListItemAdapter extends BaseAdapter {
    private final AudioFileRow.Callback callback;
    private final List<AbstractAudioFolderEntry> audioFolderEntries;

    /**
     * Is an audio file currently playing (as a preview)? Then this is
     * its position in the list.
     */
    private Integer positionPlaying;

    /**
     * Creates an adapter that's initially empty
     */
    AudioFileListItemAdapter(AudioFileRow.Callback callback) {
        audioFolderEntries = new ArrayList<>();
        this.callback = callback;
    }

    void setAudioFolderEntries(Collection<? extends AbstractAudioFolderEntry> audioFolderEntries) {
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
    public AbstractAudioFolderEntry getItem(int position) {
        return audioFolderEntries.get(position);
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        AbstractAudioFolderEntry entry = audioFolderEntries.get(position);
        if (entry instanceof AudioModelAndSound) {
            return getAudioFileRow((AudioModelAndSound) entry, isPlaying(position), convertView,
                    parent);
        }

        return getAudioSubfolderRow((AudioFolder) entry, convertView, parent);
    }

    boolean isPlaying(int position) {
        return positionPlaying != null && positionPlaying == position;
    }

    @UiThread
    private AudioFileRow getAudioFileRow(AudioModelAndSound audioModelAndSound, boolean isPlaying,
                                         @Nullable View convertView, ViewGroup parent) {
        if (!(convertView instanceof AudioFileRow)) {
            convertView = new AudioFileRow(parent.getContext());
        }
        AudioFileRow itemRow = (AudioFileRow) convertView;

        configureItemRow(itemRow, audioModelAndSound, isPlaying);

        return itemRow;
    }

    private void configureItemRow(AudioFileRow itemRow, AudioModelAndSound audioModelAndSound,
                                  boolean isPlaying) {
        itemRow.setAudioFile(audioModelAndSound, callback);
        itemRow.setImage(isPlaying ?
                R.drawable.ic_stop : R.drawable.ic_play);
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
    }

    void setPositionPlaying(Integer positionPlaying) {
        this.positionPlaying = positionPlaying;
        notifyDataSetChanged();
    }
}