package de.soundboardcrafter.dao;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.UiThread;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Used to read and write the state of the tutorial.
 */
@ParametersAreNonnullByDefault
@UiThread
public class TutorialDao {
    public enum Key {
        SOUNDBOARD_PLAY_START_SOUND,
        SOUNDBOARD_PLAY_MULTIPLE_SOUNDS,
        SOUNDBOARD_PLAY_EDIT_SOUND,
        SOUNDBOARD_PLAY_REMOVE_SOUND,
        AUDIO_FILE_LIST_EDIT,
        SOUNDBOARD_EDIT_LOCAL_AUDIO,
        SOUNDBOARD_LIST_SOUNDBOARD_CONTEXT_MENU,
        SOUNDBOARD_LIST_CUSTOM_SOUNDBOARD_CONTEXT_MENU,
        FAVORITES_LIST_CONTEXT_MENU
    }

    // Will not be included in backup! See backup_descriptor.
    // To test this, use "adb shell bmgr backupnow de.soundboardcrafter" to make a backup of the
    // app's data, then uninstall and reinstall the app.
    private static final String SHARED_PREFERENCES = "Tutorial_Prefs";

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
