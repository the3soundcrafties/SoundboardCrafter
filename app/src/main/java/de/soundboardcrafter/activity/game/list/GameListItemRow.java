package de.soundboardcrafter.activity.game.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import de.soundboardcrafter.R;
import de.soundboardcrafter.model.Game;

/**
 * Tile for a single game
 */
class GameListItemRow extends RelativeLayout {
    private static final String EXTRA_GAME_ID = "GameId";
    @NonNull
    private final TextView gameName;
    @Nonnull
    private final TextView soundboardCount;

    private Game game;

    GameListItemRow(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the view into this object
        inflater.inflate(R.layout.game_list_item, this, true);
        gameName = findViewById(R.id.game_name);
        soundboardCount = findViewById(R.id.soundboard_count);
    }


    /**
     * Set the data for the view.
     */
    @UiThread
    void setGame(Game game) {
        this.game = game;
        gameName.setText(this.game.getName());
        soundboardCount.setText(getSoundboardCountText());

        setOnLongClickListener(l -> {
            // Do NOT consume long clicks.
            // Without this, this context menu on the list view won't work
            return false;
        });
    }

    private String getSoundboardCountText() {
        int count = game.getSoundboards().size();
        if (count == 1) {
            return count + " Soundboard";
        }
        return count + " Soundboards";

    }

}
