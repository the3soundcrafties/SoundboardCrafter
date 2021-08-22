package de.soundboardcrafter.activity.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import de.soundboardcrafter.R;

public class SettingsActivity extends AppCompatActivity {
    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, SettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_frame, new SettingsFragment())
                .commit();
    }
}
