package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.audiofile.list.AbstractAudioFileRow;
import de.soundboardcrafter.model.audio.AudioModelAndSound;

/**
 * Row in the list of audio files representing one audio file.
 * Allows the file to be played and stopped again and to be opened in edit mode.
 */
class AudioFileRow extends AbstractAudioFileRow {
    public interface Callback {
        void onEditAudioFile(AudioModelAndSound audioModelAndSound);
    }

    @NonNull
    private final View iconLinkSoundToSoundboards;

    AudioFileRow(Context context) {
        super(context, R.layout.audiofile_list_file);
        iconLinkSoundToSoundboards = findViewById(R.id.icon_link_sound_to_soundboards);
    }

    /**
     * Set the data for the view.
     */
    @UiThread
    void setAudioFile(AudioModelAndSound audioFileAndSound, AudioFileRow.Callback callback) {
        super.setAudioFile(audioFileAndSound);
        iconLinkSoundToSoundboards
                .setOnClickListener(l -> callback.onEditAudioFile(getAudioModelAndSound()));
    }
}
