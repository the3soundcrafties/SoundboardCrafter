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
    public static final class GameTable {
        public static final String NAME = "game";

        public static final class Cols {
            public static final String ID = "_id";
            public static final String NAME = "name";
        }
    }

    /**
     * The soundboards
     */
    public static final class SoundboardTable {
        public static final String NAME = "soundboard";

        public static final class Cols {
            public static final String ID = "_id";
            public static final String NAME = "name";
        }
    }

    /**
     * Which soundboard belongs to which game(s)
     */
    public static final class SoundboardGameTable {
        public static final String NAME = "soundboard_game";

        public static final class Cols {
            public static final String SOUNDBOARD_ID = "soundboard_id";
            public static final String GAME_ID = "game_id";
        }
    }

    public static final class SoundTable {
        public static final String NAME = "sound";

        public static final class Cols {
            public static final String ID = "_id";
            /**
             * Path to the audio file
             */
            public static final String PATH = "path";
            public static final String NAME = "name";
            /**
             * Relative volume of the sound as percentage, 100 means the original volume
             */
            public static final String VOLUME_PERCENTAGE = "volume_percentage";
            /**
             * Whether the sound shall be played in a loop
             */
            public static final String LOOP = "loop";
        }
    }

    /**
     * Which sound belong too which soundbar(s)
     */
    public static final class SoundboardSoundTable {
        public static final String NAME = "soundboard_sound";

        public static final class Cols {
            public static final String SOUNDBOARD_ID = "soundboard_id";
            public static final String SOUND_ID = "sound_id";
            /**
             * Position this sound has on the soundbar
             */
            public static final String INDEX = "index";
        }
    }
}
