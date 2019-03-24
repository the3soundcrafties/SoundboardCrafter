package de.soundboardcrafter.activity.sound.edit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.soundboardcrafter.R;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.model.Sound;

/**
 * Activity for editing a single sound (name, volume etc.).
 */
public class SoundEditFragment extends Fragment {
    private static final String ARG_SOUND_ID = "soundId";

    public static final String EXTRA_SOUND_ID = "soundId";

    private Sound sound;

    static SoundEditFragment newInstance(UUID soundId) {
        Bundle args = new Bundle();
        args.putString(ARG_SOUND_ID, soundId.toString());

        SoundEditFragment fragment = new SoundEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final UUID soundId = UUID.fromString(getArguments().getString(ARG_SOUND_ID));

        sound = SoundboardDao.getInstance(getActivity()).findSound(soundId);

        // The result will be the sound id, so that the calling
        // activity can update its GUI for this sound.
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SOUND_ID, soundId.toString());

        getActivity().setResult(
                // There is no cancel button - the result is always OK
                Activity.RESULT_OK,
                // TODO data: sound or soundId?!
                intent);
    }

    @Override
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sound_edit,
                container, false);

        return rootView;
    }

    // TODO Show and edit sound

    @Override
    // Called especially when the user returns to the calling activity.
    public void onPause() {
        super.onPause();

        // TODO Take values from the GUI (here? At some better place)

        sound.setName(sound.getName() + "+");

        SoundboardDao.getInstance(getActivity()).updateSound(sound);
    }
}
