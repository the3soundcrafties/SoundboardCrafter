package de.soundboardcrafter.dao;

/**
 * The names of the tables and colummns in the database schema, where the soundboards, sounds etc.
 * are stored.
 */
class DBSchema {
    /**
     * Not to be called
     */
    private DBSchema() {
    }

    /**
     * The games
     */
    static final class GameTable {
        static final String NAME = "game";

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
        }
    }

    /**
     * Which soundboard belongs to which game(s)
     */
    static final class SoundboardGameTable {
        static final String NAME = "soundboard_game";

        static final class Cols {
            static final String SOUNDBOARD_ID = "soundboard_id";
            static final String GAME_ID = "game_id";
        }
    }

    static final class SoundTable {
        static final String NAME = "sound";

        static final class Cols {
            static final String ID = "_id";
            /**
             * Path to the audio file
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
    }

    /**
     * Which sound belong too which soundbar(s)
     */
    static final class SoundboardSoundTable {
        static final String NAME = "soundboard_sound";

        static final class Cols {
            static final String SOUNDBOARD_ID = "soundboard_id";
            static final String SOUND_ID = "sound_id";
            /**
             * Position this sound has on the soundbar
             */
            static final String POS_INDEX = "pos_index";
        }
    }
}
