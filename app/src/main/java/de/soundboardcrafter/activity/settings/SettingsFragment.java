package de.soundboardcrafter.activity.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import de.soundboardcrafter.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // FIXME Use the settings:
        //  https://developer.android.com/guide/topics/ui/settings/use-saved-values
    }
}
