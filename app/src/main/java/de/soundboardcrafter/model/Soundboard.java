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
 * <code>Games</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class Soundboard implements Serializable {
    private final @NonNull
    UUID id;
    private @NonNull
    String name;
    /**
     * The sounds on their position on the <code>Soundboard</code>.
     * Sounds can be shared between soundboards, and there are no empty
     * positions.
     */
    private final ArrayList<Sound> sounds;
    /**
     * The games the soundboard belonging to
     */
    private final ArrayList<Game> games;

    public Soundboard(UUID uuidSoundboard, String nameSoundboard) {
        this(uuidSoundboard, nameSoundboard, new ArrayList<>(), new ArrayList<>());
    }

    public Soundboard(@NonNull String name, @NonNull ArrayList<Sound> sounds, @Nonnull ArrayList<Game> games) {
        this(UUID.randomUUID(), name, sounds, games);
    }

    public Soundboard(UUID id, @NonNull String name, @NonNull ArrayList<Sound> sounds, ArrayList<Game> games) {
        this.id = checkNotNull(id, "id is null");
        this.name = checkNotNull(name, "name is null");
        this.sounds = checkNotNull(sounds, "sound is null");
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
     * Returns the sounds in order, unmodifiable.
     */
    public List<Sound> getSounds() {
        return Collections.unmodifiableList(sounds);
    }

    /**
     * Returns the games in order, unmodifiable.
     */
    public List<Game> getGames() {
        return Collections.unmodifiableList(games);
    }

    /**
     * Replaces the sound with this <code>index</code> with the new
     * <code>sound</code>.
     */
    public void setSound(int index, Sound sound) {
        sounds.set(index, sound);
    }

    /**
     * Removes this sound from the soundboard.
     */
    public void removeSound(int index) {
        sounds.remove(index);
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
