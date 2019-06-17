package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;

/**
 * Row in the list of audio files representing one subfolder.
 */
class AudioSubfolderRow extends RelativeLayout {
    @NonNull
    private final TextView path;
    @Nonnull
    private final TextView numAudioFiles;
    @Nullable
    private AudioFolder audioSubfolder;

    AudioSubfolderRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.audiofile_list_subfolder, this, true);
        path = findViewById(R.id.path);
        numAudioFiles = findViewById(R.id.num_audiofiles);
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    void setData(AudioFolder audioSubfolder) {
        this.audioSubfolder = audioSubfolder;
        path.setText(audioSubfolder.getPath());
        numAudioFiles.setText(formatNumSounds());
    }

    @Nullable
    String getPath() {
        if (audioSubfolder == null) {
            return null;
        }

        return audioSubfolder.getPath();
    }

    private String formatNumSounds() {
        return getResources().getQuantityString(R.plurals.subfolder_num_sounds,
                audioSubfolder.getNumAudioFiles(), audioSubfolder.getNumAudioFiles());
    }
}
