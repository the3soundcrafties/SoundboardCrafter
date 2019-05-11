package de.soundboardcrafter.activity.sound.event;

import java.util.UUID;

public interface SoundEventListener {
    void soundChanged(UUID soundId);
}
