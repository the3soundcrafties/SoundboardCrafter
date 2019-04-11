package de.soundboardcrafter.activity.common.mediaplayer;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.Nullable;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * ID for storing media players by soundboard and sound id.
 */
class MediaPlayerSearchId implements Serializable {
    private final @Nullable
    UUID soundboardId;

    private final UUID soundId;

    MediaPlayerSearchId(@Nullable Soundboard soundboard, Sound sound) {
        this(soundboard != null ? soundboard.getId() : null, sound.getId());
    }

    MediaPlayerSearchId(@Nullable UUID soundboardId, UUID soundId) {
        this.soundboardId = soundboardId;
        this.soundId = soundId;
    }

    @Nullable
    UUID getSoundboardId() {
        return soundboardId;
    }

    UUID getSoundId() {
        return soundId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MediaPlayerSearchId)) {
            return false;
        }
        MediaPlayerSearchId that = (MediaPlayerSearchId) o;
        return Objects.equals(soundboardId, that.soundboardId) &&
                Objects.equals(soundId, that.soundId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(soundboardId, soundId);
    }

}
