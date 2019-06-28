package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import java.util.Locale;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;

/**
 * Row in the list of audio files representing one audio file.
 * Allows the file to be played and stopped again.
 */
class AudioFileRow extends RelativeLayout {
    private static final int MINS_PER_SEC = 60;

    public interface Callback {
        void onEditAudioFile(AudioModelAndSound audioModelAndSound);
    }

    @NonNull
    private final ImageView playingImage;
    @NonNull
    private final TextView audioName;
    @Nonnull
    private final TextView audioArtistAndLength;

    private AudioModelAndSound audioModelAndSound;

    AudioFileRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.audiofile_list_file, this, true);
        playingImage = findViewById(R.id.icon);
        audioName = findViewById(R.id.audio_name);
        audioArtistAndLength = findViewById(R.id.audio_artist_and_length);
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    void setAudioFile(AudioModelAndSound audioFileAndSound, Callback callback) {
        audioModelAndSound = audioFileAndSound;
        audioName.setText(formatName());
        audioArtistAndLength.setText(formatArtistAndLength());

        ImageView iconAdd = findViewById(R.id.icon_link_sound_to_soundboards);
        iconAdd.setOnClickListener(l -> callback.onEditAudioFile(audioModelAndSound));
    }

    private String formatName() {
        return abbreviateName(audioModelAndSound.getName());
    }

    private String formatArtistAndLength() {
        return audioModelAndSound.getAudioModel().getArtist()
                + " · " +
                formatLength();
    }

    private String formatLength() {
        return formatMinSecs(audioModelAndSound.getAudioModel().getDurationSecs());
    }

    private static String formatMinSecs(long durationSecs) {
        long mins = durationSecs / MINS_PER_SEC;
        long secs = durationSecs - mins * MINS_PER_SEC;

        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }

    private static String abbreviateName(String name) {
        if (name.length() > 35) {
            return name.substring(0, 35) + "...";
        }

        return name;
    }

    @UiThread
    void setImage(int imageResource) {
        playingImage.setImageResource(imageResource);
    }
}