package de.soundboardcrafter.activity.mediaplayer;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

public class MediaPlayerSearchId implements Serializable {
    private final UUID soundboardId;
    private final UUID soundId;

    MediaPlayerSearchId(Soundboard soundboard, Sound sound) {
        soundboardId = soundboard.getId();
        soundId = sound.getId();
    }

    MediaPlayerSearchId(UUID soundboardId, UUID soundId) {
        this.soundboardId = soundboardId;
        this.soundId = soundId;
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
