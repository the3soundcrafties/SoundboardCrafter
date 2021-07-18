package de.soundboardcrafter.dao;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.UiThread;

import javax.annotation.ParametersAreNonnullByDefault;

import static android.content.Context.MODE_PRIVATE;

/**
 * Used to read and write the state of the tutorial.
 */
@ParametersAreNonnullByDefault
@UiThread
public class TutorialDao {
    public enum Key {
        SOUNDBOARD_PLAY_START_SOUND,
        AUDIO_FILE_LIST_EDIT,
        SOUNDBOARD_PLAY_CONTEXT_MENU,
        SOUNDBOARD_LIST_CONTEXT_MENU,
        GAME_LIST_CONTEXT_MENU
    }

    private static final String SHARED_PREFERENCES =
            TutorialDao.class.getName() + "_Prefs";

    private static TutorialDao instance;
    private final Context appContext;

    public static TutorialDao getInstance(final Context context) {
        if (instance == null) {
            instance = new TutorialDao(context);
        }
        return instance;
    }

    private TutorialDao(Context context) {
        appContext = context.getApplicationContext();
    }

    public boolean isChecked(Key key) {
        return getPrefs().getBoolean(key.name(), false);
    }

    public void check(Key key) {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putBoolean(key.name(), true);
        editor.apply();
    }

    public void uncheckAll() {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.clear();
        editor.apply();
    }

    private SharedPreferences getPrefs() {
        return appContext.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
    }
}
