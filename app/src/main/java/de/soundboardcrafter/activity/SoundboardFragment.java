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

import com.google.common.base.Preconditions;

import de.soundboardcrafter.R;

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
        String[] items = {"hallo","huhu","hihiho","haha"};
        GridView gridView = (GridView)rootView.findViewById(R.id.gridview_soundboard);
        SoundBoardItemAdapter soundBoardItemAdapter = new SoundBoardItemAdapter(this.getContext(), items);
        gridView.setAdapter(soundBoardItemAdapter);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_main, menu);
    }
}
