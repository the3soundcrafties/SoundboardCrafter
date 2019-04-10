package de.soundboardcrafter.activity.soundboard.play;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter for a SoundBoardItem. Display a Button with Text and Icon
 */
class SoundboardItemAdapter extends BaseAdapter {
    private final SoundBoardItemRow.MediaPlayerServiceCallback mediaPlayerServiceCallback;
    private Soundboard soundboard;
    private static String TAG = SoundboardItemAdapter.class.getName();

    SoundboardItemAdapter(@Nonnull SoundBoardItemRow.MediaPlayerServiceCallback mediaPlayerServiceCallback, @Nonnull Soundboard soundboard) {
        this.soundboard = checkNotNull(soundboard, "soundboard is null");
        this.mediaPlayerServiceCallback = checkNotNull(mediaPlayerServiceCallback, "mediaPlayerServiceCallback!=null");
    }


    /**
     * Sets the soundboard.
     */
    public void setSoundboard(Soundboard soundboard) {
        this.soundboard = soundboard;
        notifyDataSetChanged();
    }

    /**
     * Returns the soundboard.
     */
    public Soundboard getSoundboard() {
        return soundboard;
    }

    /**
     * If there are already sounds in the soundboard with one of these IDs, replace
     * them with the respective updates.
     */
    void updateSounds(Collection<Sound> updates) {
        for (Sound update : updates) {
            Log.d(TAG, "Edit sound: updateSounds " + update + " this " + this);
            updateSound(update);
        }
    }

    /**
     * If there is already a sound in the soundboard with this ID, replace
     * it with the given update.
     */
    private void updateSound(Sound update) {
        for (int i = 0; i < soundboard.getSounds().size(); i++) {
            final Sound oldSound = soundboard.getSounds().get(i);
            if (update.getId().equals(oldSound.getId())) {
                mediaPlayerServiceCallback.stopPlaying(soundboard, oldSound);
                soundboard.setSound(i, update);

                break;
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Removes all sounds from the adapter. Should a sound currently be playing,
     * stop it before the sound is removed.
     */
    void clear() {
        for (Sound sound : soundboard.getSounds()) {
            mediaPlayerServiceCallback.stopPlaying(soundboard, sound);
        }
        soundboard.clearSounds();
        notifyDataSetChanged();
    }

    /**
     * Removes the sound from the adapter. Should the sound currently be playing,
     * stop it before the sound is removed.
     */
    void remove(int position) {
        Sound sound = soundboard.getSounds().get(position);
        mediaPlayerServiceCallback.stopPlaying(soundboard, sound);

        soundboard.removeSound(position);

        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return soundboard.getSounds().size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Sound getItem(int position) {
        return soundboard.getSounds().get(position);
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        Sound sound = soundboard.getSounds().get(position);
        Log.d(TAG, "getView " + sound.getName() + " " + this);
        AbsListView listView = (AbsListView) parent;
        if (convertView == null) {
            if (listView.getLastVisiblePosition() < position) {
                convertView = new SoundBoardItemRow(parent.getContext());
            } else {
                //Bugfix: The item at the first postion is instanciated two times. But the second one is not visible
                // So the callbacks from the visible one should go to the new Item.
                // The new item has to be created because the view needs the measurements
                SoundBoardItemRow row = (SoundBoardItemRow) getViewByPosition(position, listView);
                convertView = new SoundBoardItemRow(parent.getContext(), row.getInitializeCallback(), row.getStartPlayCallback(), row.getStopPlayCallback());
            }
        }
        // We can now safely cast and set the data
        ((SoundBoardItemRow) convertView).setSound(soundboard, sound, mediaPlayerServiceCallback);
        return convertView;
    }

    /**
     * gets the item view by position. Source https://stackoverflow.com/questions/24811536/android-listview-get-item-view-by-position
     */
    private View getViewByPosition(int pos, AbsListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }
}