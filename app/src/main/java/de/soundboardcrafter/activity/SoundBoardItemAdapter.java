package de.soundboardcrafter.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import javax.annotation.Nullable;

import de.soundboardcrafter.R;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
public class SoundBoardItemAdapter extends BaseAdapter {

    private final Context context;
    private final String[] soundboardItems;

    public SoundBoardItemAdapter(Context context, String[] soundboardItems) {
        this.context = context;
        this.soundboardItems = soundboardItems;
    }

    @Override
    public int getCount() {
        return soundboardItems.length;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        String name = soundboardItems[position];

        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.linearlayout_soundboarditem, null);
        }

        TextView nameTextView = (TextView) convertView.findViewById(R.id.name);
        nameTextView.setText(name);
        return convertView;
    }
}
