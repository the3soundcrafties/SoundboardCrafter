package de.soundboardcrafter.activity.common.audiofile.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.soundboardcrafter.model.audio.AbstractAudioFolderEntry;
import de.soundboardcrafter.model.audio.AudioFolder;

/**
 * Abstract adapter class for a list of audio files (and audio folders)
 *
 * @param <T> Type of the entries in the list
 */
public abstract class AbstractAudioFileListAdapter<T> extends BaseAdapter {
    private final List<T> audioFolderEntries = new ArrayList<>();

    /**
     * Is an audio file currently playing (as a preview)? Then this is
     * its position in the list.
     */
    private Integer positionPlaying;

    /**
     * Creates an adapter that's initially empty
     */
    protected AbstractAudioFileListAdapter() {
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= audioFolderEntries.size()) {
            return 0;
        }

        T entry = audioFolderEntries.get(position);
        final AbstractAudioFolderEntry abstractAudioFolderEntry = asAbstractAudioFolderEntry(entry);
        return (abstractAudioFolderEntry instanceof AudioFolder) ? 0 : 1;
    }

    public void setAudioFolderEntries(Collection<? extends T> audioFolderEntries) {
        this.audioFolderEntries.clear();
        this.audioFolderEntries.addAll(audioFolderEntries);
        positionPlaying = null;

        notifyDataSetChanged();
    }

    public ImmutableList<T> copyAudioFolderEntries() {
        return ImmutableList.copyOf(audioFolderEntries);
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
    public T getItem(int position) {
        return audioFolderEntries.get(position);
    }

    public boolean isPlaying(int position) {
        return positionPlaying != null && positionPlaying == position;
    }

    @Override
    @UiThread
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        T entry = audioFolderEntries.get(position);
        final AbstractAudioFolderEntry abstractAudioFolderEntry = asAbstractAudioFolderEntry(entry);
        if (abstractAudioFolderEntry instanceof AudioFolder) {
            return getAudioSubfolderRow((AudioFolder) abstractAudioFolderEntry, convertView,
                    parent);
        }

        return getAudioFileRow((T) entry, isPlaying(position), convertView,
                parent);
    }

    protected abstract AbstractAudioFolderEntry asAbstractAudioFolderEntry(T entry);

    @NonNull
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

    private void configureItemRow(@NonNull AudioSubfolderRow itemRow, AudioFolder audioFolder) {
        itemRow.setData(audioFolder);
    }

    @UiThread
    public abstract AbstractAudioFileRow getAudioFileRow(T audioModelAndSound, boolean isPlaying,
                                                         @Nullable View convertView,
                                                         ViewGroup parent);

    public void setPositionPlaying(Integer positionPlaying) {
        this.positionPlaying = positionPlaying;
        notifyDataSetChanged();
    }
}
