package de.soundboardcrafter.activity.common.audiofile.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.google.common.base.Joiner;

import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.audio.AudioModelAndSound;


/**
 * Abstract superclass for a row in a list of audio files representing one audio file.
 * Allows the file to be played and stopped again.
 */
public abstract class AbstractAudioFileRow extends RelativeLayout {
    private static final int MINUTES_PER_SECOND = 60;
    @NonNull
    protected final ImageView playingImage;
    @NonNull
    protected final TextView audioName;
    @Nonnull
    protected final TextView audioArtistAndLength;
    private AudioModelAndSound audioModelAndSound;

    public AbstractAudioFileRow(Context context, @LayoutRes int layoutResource) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(layoutResource, this, true);

        playingImage = findViewById(R.id.icon);
        audioName = findViewById(R.id.audio_name);
        audioArtistAndLength = findViewById(R.id.audio_artist_and_length);
    }

    private static String formatMinSecs(long durationSecs) {
        long minutes = durationSecs / MINUTES_PER_SECOND;
        long secs = durationSecs - minutes * MINUTES_PER_SECOND;

        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    public void setAudioFile(AudioModelAndSound audioFileAndSound) {
        audioModelAndSound = audioFileAndSound;
        audioName.setText(formatName());
        audioArtistAndLength.setText(formatArtistAndLength());
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
        return AbstractAudioFileRow
                .formatMinSecs(audioModelAndSound.getAudioModel().getDurationSecs());
    }

    @UiThread
    public void setImage(int imageResource) {
        playingImage.setImageResource(imageResource);
    }

    protected AudioModelAndSound getAudioModelAndSound() {
        return audioModelAndSound;
    }
}
