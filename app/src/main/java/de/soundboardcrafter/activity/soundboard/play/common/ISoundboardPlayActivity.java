package de.soundboardcrafter.activity.soundboard.play.common;

import androidx.fragment.app.Fragment;

public interface ISoundboardPlayActivity {
    void addFragment(Fragment fragment);

    void removeFragment(Fragment fragment);

    void soundsChanged();

    void soundsDeleted();

    void setChangingSoundboardEnabled(boolean changingSoundboardEnabled);
}
