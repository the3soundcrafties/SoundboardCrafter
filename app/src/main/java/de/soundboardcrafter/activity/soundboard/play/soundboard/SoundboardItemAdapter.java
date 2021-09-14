package de.soundboardcrafter.activity.soundboard.play.soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;
import java.util.Map;

import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Adapter for a soundboard item.
 */
public class SoundboardItemAdapter
        extends RecyclerView.Adapter<SoundboardItemAdapter.ViewHolder> {
    private final SoundboardItemRow.MediaPlayerServiceCallback mediaPlayerServiceCallback;
    private final SoundboardWithSounds soundboard;

    private ActionListener actionListener;

    @Nullable
    private ViewHolder contextMenuViewHolder;

    private int contextMenuPosition = -1;

    public interface ActionListener {
        @UiThread
        void onItemClick(int position, View v);

        void onCreateContextMenu(int position, ContextMenu menu);
    }

    /**
     * View holder, providing references to the views for one item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(SoundboardItemRow itemView) {
            super(itemView);
        }

        SoundboardItemRow getSoundboardItemRow() {
            return (SoundboardItemRow) itemView;
        }
    }

    SoundboardItemAdapter(
            @NonNull SoundboardItemRow.MediaPlayerServiceCallback mediaPlayerServiceCallback,
            @NonNull SoundboardWithSounds soundboard) {
        this.soundboard = checkNotNull(soundboard, "soundboard is null");
        this.mediaPlayerServiceCallback =
                checkNotNull(mediaPlayerServiceCallback,
                        "mediaPlayerServiceCallback!=null");
    }

    /**
     * Returns the soundboard.
     */
    public SoundboardWithSounds getSoundboard() {
        return soundboard;
    }

    @Override
    public int getItemCount() {
        return soundboard.getSounds().size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    @NonNull
    public SoundboardItemAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        // create a new view
        SoundboardItemRow v = new SoundboardItemRow(parent.getContext());

        return new SoundboardItemAdapter.ViewHolder(v);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.getSoundboardItemRow().setOnClickListener(null);
        holder.getSoundboardItemRow().setOnCreateContextMenuListener(null);

        if (holder == contextMenuViewHolder) {
            contextMenuViewHolder = null;
        }

        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(@NonNull SoundboardItemAdapter.ViewHolder holder, int position) {
        Sound sound = soundboard.getSounds().get(position);

        mediaPlayerServiceCallback.setOnPlayingStopped(soundboard.getSoundboard(), sound,
                this::notifyDataSetChanged);

        holder.getSoundboardItemRow()
                .setSound(soundboard.getSoundboard(), sound, mediaPlayerServiceCallback);

        holder.getSoundboardItemRow().setOnClickListener(
                v -> actionListener.onItemClick(holder.getAdapterPosition(), v)
        );
        holder.getSoundboardItemRow().setOnCreateContextMenuListener(
                (menu, v, menuInfo) -> {
                    contextMenuViewHolder = holder;
                    contextMenuPosition = holder.getAdapterPosition();
                    actionListener.onCreateContextMenu(contextMenuPosition, menu);
                }
        );
    }

    /**
     * If there are already sounds in the soundboard with one of these IDs, replace
     * them with the respective updates.
     * playingStartedOrStopped sound if not in soundboard anymore
     */
    void updateSounds(Map<Sound, Boolean> sounds) {
        for (Map.Entry<Sound, Boolean> soundEntry : sounds.entrySet()) {
            updateSound(soundEntry.getKey());
            if (!soundEntry.getValue()) {
                mediaPlayerServiceCallback
                        .stopPlaying(soundboard.getSoundboard(), soundEntry.getKey(), true);
            }
        }
    }

    void updateSounds(Collection<Sound> sounds) {
        for (Sound sound : sounds) {
            updateSound(sound);
        }
    }

    /**
     * If there is already a sound in the soundboard with this ID, replace
     * it with the given update.
     */
    private void updateSound(Sound update) {
        for (int i = 0; i < soundboard.getSounds().size(); i++) {
            Sound oldSound = soundboard.getSounds().get(i);
            if (update.getId().equals(oldSound.getId())) {
                mediaPlayerServiceCallback.stopPlaying(
                        soundboard.getSoundboard(), oldSound, false);
                soundboard.setSound(i, update);
                break;
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Moves the sound inside the adapter.
     */
    void move(int oldPosition, int newPosition) {
        if (oldPosition == newPosition) {
            return;
        }

        soundboard.moveSound(oldPosition, newPosition);

        notifyItemMoved(oldPosition, newPosition);
        // TODO if it should not work, try notifyDataSetChanged();
    }

    /**
     * Removes the sound from the adapter. Should the sound currently be playing,
     * playingStartedOrStopped it before the sound is removed.
     */
    void remove(int position) {
        Sound sound = soundboard.getSounds().get(position);
        mediaPlayerServiceCallback.stopPlaying(
                soundboard.getSoundboard(), sound, false);

        soundboard.removeSound(position);
        notifyItemRemoved(position);
    }

    @Nullable
    Sound getContextMenuItem() {
        if (contextMenuPosition < 0 || contextMenuPosition >= soundboard.getSounds().size()) {
            return null;
        }

        return soundboard.getSounds().get(contextMenuPosition);
    }

    int getContextMenuPosition() {
        return contextMenuPosition;
    }

    public Sound getItem(int position) {
        return soundboard.getSounds().get(position);
    }

    void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }
}