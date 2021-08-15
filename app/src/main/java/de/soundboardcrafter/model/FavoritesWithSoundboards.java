package de.soundboardcrafter.model;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Favorites with their soundboards.
 * <p></p>
 * <code>Favorites</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class FavoritesWithSoundboards implements Serializable {
    @NonNull
    private final Favorites favorites;
    /**
     * The soundboards that make up these favorites.
     */
    private final ArrayList<Soundboard> soundboards;


    public FavoritesWithSoundboards(@NonNull String name) {
        this(new Favorites(name));
    }

    public FavoritesWithSoundboards(@Nonnull Favorites favorites) {
        this(favorites, new ArrayList<>());
    }

    private FavoritesWithSoundboards(@Nonnull Favorites favorites,
                                     @Nonnull ArrayList<Soundboard> soundboards) {
        this.favorites = favorites;
        this.soundboards = checkNotNull(soundboards, "soundboards is null");
    }

    public void addSoundboard(Soundboard soundboard) {
        Optional<Soundboard> foundSoundboard =
                soundboards.stream().filter(sd -> sd.getId().equals(soundboard.getId()))
                        .findFirst();
        if (!foundSoundboard.isPresent()) {
            soundboards.add(soundboard);
        }
    }

    public @Nonnull
    Favorites getFavorites() {
        return favorites;
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
        return "FavoritesWithSoundboards{" +
                "favorites=" + favorites +
                ", soundboards=" + soundboards +
                '}';
    }
}
