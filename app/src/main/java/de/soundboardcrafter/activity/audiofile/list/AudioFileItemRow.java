package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import de.soundboardcrafter.R;

/**
 * Tile for a single sound in a soundboard, allows the sound to be played and stopped again.
 */
class AudioFileItemRow extends RelativeLayout {
    @NonNull
    private final TextView audioName;
    @Nonnull
    private final TextView audioArtist;

    private AudioModel audioFile;

    AudioFileItemRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.audiofile_list_item, this, true);
        audioName = findViewById(R.id.audio_name);
        audioArtist = findViewById(R.id.audio_artist);
    }


    /**
     * Set the data for the view.
     */
    @UiThread
    void setAudioFile(AudioModel audioFile) {
        this.audioFile = audioFile;
        String name = audioFile.getName();
        if (name.length() > 35) {
            name = audioFile.getName().substring(0, 35) + "...";
        }
        audioName.setText(name);
        audioArtist.setText(audioFile.getArtist());


        setOnLongClickListener(l -> {
            // Do NOT consume long clicks.
            // Without this, this context menu on the list view won't work
            return false;
        });
    }


}
