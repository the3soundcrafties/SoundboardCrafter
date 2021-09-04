package de.soundboardcrafter.dao;

import androidx.annotation.NonNull;

import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;

/**
 * The names of the tables and columns in the database schema, where the soundboards, sounds etc.
 * are stored.
 */
class DBSchema {
    /**
     * Not to be called
     */
    private DBSchema() {
    }

    /**
     * The games - no longer used
     */
    static final class GamesTable {
        static final String NAME = "games";
    }

    /**
     * The favorites
     */
    static final class FavoritesTable {
        static final String NAME = "favorites";

        static final class Cols {
            static final String ID = "_id";
            static final String NAME = "name";
        }
    }

    /**
     * The soundboards
     */
    static final class SoundboardTable {
        static final String NAME = "soundboard";

        static final class Cols {
            static final String ID = "_id";
            static final String NAME = "name";
            /**
             * Whether the soundboard has been built automatically from provided sounds.
             * A <i>provided</i> soundboard cannot be deleted.
             */
            static final String PROVIDED = "provided";
        }
    }

    /**
     * Which soundboard belongs to which game? - No longer used.
     */
    static final class SoundboardGamesTable {
        static final String NAME = "soundboard_games";
    }

    /**
     * Which soundboard is part of which favorites?
     */
    static final class SoundboardFavoritesTable {
        static final String NAME = "soundboard_favorites";

        static final class Cols {
            static final String SOUNDBOARD_ID = "soundboard_id";
            static final String FAVORITES_ID = "favorites_id";
        }
    }

    static final class SoundTable {
        static final String NAME = "sound";

        enum LocationType {
            /**
             * Located in the file system
             */
            FILE,
            /**
             * Located in the app as an asset
             */
            ASSET
        }

        static final class Cols {
            static final String ID = "_id";

            /**
             * Path to the audio file (in the device's file system or the assets folder)
             */
            static final String LOCATION_TYPE = "location_type";

            /**
             * Path to the audio file (in the device's file system or the assets folder)
             */
            static final String PATH = "path";
            static final String NAME = "name";
            /**
             * Relative volume of the sound as percentage, 100 means the original volume
             */
            static final String VOLUME_PERCENTAGE = "volume_percentage";
            /**
             * Whether the sound shall be played in a loop
             */
            static final String LOOP = "loop";
        }

        /**
         * Converts a {@link LocationType} and a <code>path</code> to an
         * {@link AbstractAudioLocation}.
         */
        static AbstractAudioLocation toAudioLocation(
                @NonNull SoundTable.LocationType locationType, @NonNull String path) {
            switch (locationType) {
                case FILE:
                    return new FileSystemFolderAudioLocation(path);
                case ASSET:
                    return new AssetFolderAudioLocation(path);
                default:
                    throw new IllegalStateException(
                            "Unexpected audio location type: " + locationType);
            }
        }
    }

    /**
     * Which sound belong too which soundboard(s)
     */
    static final class SoundboardSoundTable {
        static final String NAME = "soundboard_sound";

        static final class Cols {
            static final String SOUNDBOARD_ID = "soundboard_id";
            static final String SOUND_ID = "sound_id";
            /**
             * Position this sound has on the soundboard
             */
            static final String POS_INDEX = "pos_index";
        }
    }
}
