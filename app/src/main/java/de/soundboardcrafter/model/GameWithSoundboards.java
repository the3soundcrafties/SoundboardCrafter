package de.soundboardcrafter.model;

import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;

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
    private ArrayList<Soundboard> soundboards = new ArrayList<>();


    public GameWithSoundboards(@NonNull String name) {
        this(UUID.randomUUID(), name);
    }

    public GameWithSoundboards(@Nonnull UUID id, String name) {
        this(id, name, new ArrayList<>());
    }

    private GameWithSoundboards(@Nonnull UUID id, String name, ArrayList<Soundboard> soundboards) {
        game = new Game(id, name);
        this.soundboards = checkNotNull(soundboards, "soundboards is null");
    }


    public void addSoundboard(Soundboard soundboard) {
        Optional<Soundboard> foundSoundboard = soundboards.stream().filter(sd -> sd.getId().equals(soundboard.getId())).findFirst();
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


    @Override
    public String toString() {
        return "GameWithSoundboards{" +
                "game=" + game +
                ", soundboards=" + soundboards +
                '}';
    }
}
