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
public class Game implements Serializable {
    @NonNull
    private final UUID id;
    @NonNull
    private String name;
    /**
     * The soundboards the game belonging to
     */
    private ArrayList<Soundboard> soundboards = new ArrayList<>();


    public Game(@NonNull String name) {
        this(UUID.randomUUID(), name);
    }

    public Game(@Nonnull UUID id, String name) {
        this(id, name, new ArrayList<>());
    }

    public Game(@Nonnull UUID id, String name, ArrayList<Soundboard> soundboards) {
        this.id = checkNotNull(id, "id is null");
        this.name = checkNotNull(name, "name is null");
        this.soundboards = checkNotNull(soundboards, "soundboards is null");
        addGameInSoundboards(soundboards);
    }

    private void addGameInSoundboards(ArrayList<Soundboard> soundboards) {
        for (Soundboard soundboard : soundboards) {
            soundboard.addGame(this);
        }
    }

    public void addSoundboard(Soundboard soundboard) {
        Optional<Soundboard> foundSoundboard = soundboards.stream().filter(sd -> sd.getId().equals(soundboard.getId())).findFirst();
        if (!foundSoundboard.isPresent()) {
            soundboards.add(soundboard);
            soundboard.addGame(this);
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

    public ImmutableList<Soundboard> getSoundboards() {
        return ImmutableList.copyOf(soundboards);
    }

    @Override
    public @Nonnull
    String toString() {
        return "Game{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }


}
