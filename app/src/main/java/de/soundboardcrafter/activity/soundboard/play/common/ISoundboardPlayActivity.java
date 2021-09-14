package de.soundboardcrafter.activity.soundboard.play.common;

import java.util.UUID;

public interface ISoundboardPlayActivity {
    void soundChanged(UUID soundId);

    void soundsDeleted();

    void setChangingSoundboardEnabled(boolean changingSoundboardEnabled);
}
