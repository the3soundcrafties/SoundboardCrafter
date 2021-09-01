package de.soundboardcrafter.activity.common.audiofile.list;

import static com.google.common.base.Preconditions.checkState;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.audio.AudioFolder;

/**
 * Row in the list of audio files representing one subfolder.
 */
public class AudioSubfolderRow extends RelativeLayout {
    @NonNull
    private final TextView path;
    @Nonnull
    private final TextView numAudioFiles;
    @Nullable
    private AudioFolder audioSubfolder;

    public AudioSubfolderRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.audiofile_subfolder, this, true);
        path = findViewById(R.id.path);
        numAudioFiles = findViewById(R.id.num_audiofiles);
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    public void setData(AudioFolder audioSubfolder) {
        this.audioSubfolder = audioSubfolder;
        path.setText(audioSubfolder.getName());
        numAudioFiles.setText(formatNumSounds());
    }

    @Nullable
    public String getPath() {
        if (audioSubfolder == null) {
            return null;
        }

        return audioSubfolder.getPath();
    }

    private String formatNumSounds() {
        checkState(audioSubfolder != null, "audioSubfolder (still) null");

        return getResources().getQuantityString(R.plurals.subfolder_num_sounds,
                audioSubfolder.getNumAudioFiles(), audioSubfolder.getNumAudioFiles());
    }
}
