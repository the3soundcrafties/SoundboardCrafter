package de.soundboardcrafter.database;

/**
 * The names of the tables and colummns in the database schema, where the soundboards, sounds etc.
 * are stored.
 */
class SoundboardCrafterDBSchema {
    /**
     * Not to be called
     */
    private SoundboardCrafterDBSchema() {}

    /** The games */
    public static final class GameTable {
        public static final String NAME = "GAME";

        public static final String COL_ID = "ID";
        public static final String COL_NAME = "NAME";
    }

    /** The soundboards */
    public static final class SoundboardTable {
        public static final String NAME = "SOUNDBOARD";

        public static final String COL_ID = "ID";
        public static final String COL_NAME = "NAME";
    }

    /** Which soundboard belongs to which game(s) */
    public static final class SoundboardGameTable {
        public static final String NAME = "SOUNDBOARD_GAME";

        public static final String COL_SOUNDBOARD_ID = "SOUNDBOARD_ID";
        public static final String COL_GAME_ID = "GAME_ID";
    }

    public static final class SoundTable {
        public static final String NAME = "SOUND";

        public static final String COL_SOUND_ID = "ID";
        /** Path to the audio file */
        public static final String COL_PATH = "PATH";
        public static final String COL_NAME = "NAME";
        /** Relative volume of the sound, 1.0 means the original volume */
        public static final String COL_REL_VOLUME = "REL_VOLUME";
        /** Whether the sound shall be played in a loop */
        public static final String COL_LOOP = "LOOP";
    }

    /** Which sound belong too which soundbar(s) */
    public static final class SoundboardSoundTable {
        public static final String NAME = "SOUNDBOARD_SOUND";

        public static final String COL_SOUNDBOARD_ID = "SOUNDBOARD_ID";
        public static final String COL_SOUND_ID = "SOUND_ID";
        /** Position this sound has on the soundbar */
        public static final String COL_INDEX = "INDEX";
    }
}
