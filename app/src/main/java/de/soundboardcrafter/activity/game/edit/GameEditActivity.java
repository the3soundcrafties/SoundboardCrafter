package de.soundboardcrafter.activity.game.edit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;

import java.util.UUID;

import de.soundboardcrafter.activity.common.SingleFragmentActivity;
import de.soundboardcrafter.model.Game;

import static de.soundboardcrafter.activity.common.ActivityConstants.BASE_PACKAGE;

/**
 * Abstract super class for activities for editing a single Game
 */
public class GameEditActivity extends SingleFragmentActivity {
    private static final String EXTRA_GAME_ID = BASE_PACKAGE + ".gameId";

    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext, Game game) {
        Intent intent = new Intent(packageContext, GameEditActivity.class);
        intent.putExtra(EXTRA_GAME_ID, game.getId().toString());
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
    }

    @Override
    @UiThread
    protected Fragment createFragment() {
        UUID gameId = UUID.fromString(getIntent().getStringExtra(EXTRA_GAME_ID));
        return GameEditFragment.newInstance(gameId);
    }

}
