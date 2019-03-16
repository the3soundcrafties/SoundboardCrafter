package de.soundboardcrafter.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.common.collect.Lists;

import java.nio.file.Paths;
import java.util.List;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

/**
 * Shows Soundboard in a Grid
 */
public class SoundboardFragment extends Fragment {

    public SoundboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard, container, false);
        Sound livinOnAPrayer = new Sound("/storage/emulated/0/soundboard crafter test songs/Bon Jovi-Livin On A Prayer.mp3",
                "Livin On A Prayer", 0.5f, true);
        Sound stayAnotherDay = new Sound("/storage/emulated/0/soundboard crafter test songs/Stay Another Day.mp3",
                "Stay Another Day", 0.5f, true);
        Sound trailer2 = new Sound("/storage/emulated/0/soundboard crafter test songs/trailer2.wav",
                "Trailer2", 0.9f, false);
        Soundboard board = new Soundboard("my new Soundboard", Lists.newArrayList(livinOnAPrayer, stayAnotherDay, trailer2));
        GridView gridView = (GridView)rootView.findViewById(R.id.gridview_soundboard);
        SoundboardItemAdapter soundBoardItemAdapter = new SoundboardItemAdapter(this.getActivity(), board);
        gridView.setAdapter(soundBoardItemAdapter);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_main, menu);
    }
}
