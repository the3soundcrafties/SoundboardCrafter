package de.soundboardcrafter.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A soundboard, that is a keyboard you can play sounds with.
 * The application supports several soundboards.
 * <p></p>
 * <code>Soundboards</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class Soundboard implements Serializable {
    private final @NonNull
    UUID id;
    private @NonNull
    String name;

    public Soundboard(@NonNull String name) {
        this(UUID.randomUUID(), name);

    /**
     * The games the soundboard belonging to
     */
    private final ArrayList<Game> games;

    public Soundboard(UUID uuidSoundboard, String nameSoundboard) {
        this(uuidSoundboard, nameSoundboard, new ArrayList<>(), new ArrayList<>());
    }

    public Soundboard(@NonNull String name, @Nonnull ArrayList<Game> games) {
        this(UUID.randomUUID(), name, games);
    }

    public Soundboard(UUID id, @NonNull String name, @Nonnull ArrayList<Game> games) {
        this.id = checkNotNull(id, "id is null");
        this.name = checkNotNull(name, "name is null");
        this.games = checkNotNull(games, "games is null");
        addSoundboardInGame(games);
    }


    private void addSoundboardInGame(ArrayList<Game> games) {
        for (Game game : games) {
            game.addSoundboard(this);
        }
    }

    void addGame(Game game) {
        Optional<Game> foundGame = games.stream().filter(gm -> gm.getId().equals(game.getId())).findFirst();
        if (!foundGame.isPresent()) {
            games.add(game);
        }
    }


    public @NonNull
    UUID getId() {
        return id;
    }

    public @NonNull
    String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        checkNotNull(name, "name is null");
        this.name = name;
    }


    /**
     * Returns the games in order, unmodifiable.
     */
    public List<Game> getGames() {
        return Collections.unmodifiableList(games);
    }



    @Override
    public @Nonnull
    String toString() {
        return "Soundboard{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
