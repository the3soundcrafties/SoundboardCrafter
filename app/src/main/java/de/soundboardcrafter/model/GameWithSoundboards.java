package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Optional;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A game to which a soundboard might be linked.
 * <p></p>
 * <code>Games</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class GameWithSoundboards implements Serializable {
    @NonNull
    private Game game;
    /**
     * The soundboards the game belonging to
     */
    private ArrayList<Soundboard> soundboards;


    public GameWithSoundboards(@NonNull String name) {
        this(new Game(name));
    }

    public GameWithSoundboards(@Nonnull Game game) {
        this(game, new ArrayList<>());
    }

    private GameWithSoundboards(@Nonnull Game game, @Nonnull ArrayList<Soundboard> soundboards) {
        this.game = game;
        this.soundboards = checkNotNull(soundboards, "soundboards is null");
    }

    public void addSoundboard(Soundboard soundboard) {
        Optional<Soundboard> foundSoundboard =
                soundboards.stream().filter(sd -> sd.getId().equals(soundboard.getId())).findFirst();
        if (!foundSoundboard.isPresent()) {
            soundboards.add(soundboard);
        }
    }

    public @Nonnull
    Game getGame() {
        return game;
    }


    public ImmutableList<Soundboard> getSoundboards() {
        return ImmutableList.copyOf(soundboards);
    }

    public void clearSoundboards() {
        soundboards.clear();
    }

    @Override
    public @NonNull
    String toString() {
        return "GameWithSoundboards{" +
                "game=" + game +
                ", soundboards=" + soundboards +
                '}';
    }
}
