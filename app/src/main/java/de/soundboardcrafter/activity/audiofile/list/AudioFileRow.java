package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.google.common.base.Joiner;

import java.util.Locale;
import java.util.UUID;

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
    @NonNull
    private final View iconLinkSoundToSoundboards;
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
        iconLinkSoundToSoundboards = findViewById(R.id.icon_link_sound_to_soundboards);
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
        iconLinkSoundToSoundboards
                .setOnClickListener(l -> callback.onEditAudioFile(audioModelAndSound));
    }

    private String formatName() {
        return audioModelAndSound.getName();
    }

    private String formatArtistAndLength() {
        return Joiner.on(" · ").skipNulls().join(
                audioModelAndSound.getAudioModel().getArtist(),
                formatLength());
    }

    @Nullable
    public UUID getSoundId() {
        return audioModelAndSound.getSoundId();
    }

    private String formatLength() {
        return formatMinSecs(audioModelAndSound.getAudioModel().getDurationSecs());
    }

    private static String formatMinSecs(long durationSecs) {
        long mins = durationSecs / MINS_PER_SEC;
        long secs = durationSecs - mins * MINS_PER_SEC;

        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }

    @UiThread
    void setImage(int imageResource) {
        playingImage.setImageResource(imageResource);
    }

    @NonNull
    View getIconLinkSoundToSoundboards() {
        return iconLinkSoundToSoundboards;
    }
}
