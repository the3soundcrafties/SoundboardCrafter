package de.soundboardcrafter.activity.sound.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.annotation.Nonnull;

import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import de.soundboardcrafter.R;

/**
 * Shows Soundboard in a Grid
 */
public class SoundListFragment extends Fragment {
    private static final String TAG = SoundListFragment.class.getName();

    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sound_list,
                container, false);

        return rootView;
    }
}
