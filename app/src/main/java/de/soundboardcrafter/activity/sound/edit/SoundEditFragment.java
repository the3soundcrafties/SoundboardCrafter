package de.soundboardcrafter.activity.sound.edit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.soundboardcrafter.R;
import de.soundboardcrafter.model.Sound;

/**
 * Activity for editing a single sound (name, volume etc.).
 */
public class SoundEditFragment extends Fragment {
    private static final String ARG_SOUND = "sound";

    private Sound sound;

    static SoundEditFragment newInstance(Sound sound) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_SOUND, sound);

        SoundEditFragment fragment = new SoundEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sound = (Sound) getArguments().getSerializable(ARG_SOUND);
    }

    @Override
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sound_edit,
                container, false);

        return rootView;
    }


    // TODO Edit sound

}
