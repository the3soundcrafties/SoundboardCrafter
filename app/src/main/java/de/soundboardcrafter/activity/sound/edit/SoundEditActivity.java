package de.soundboardcrafter.activity.sound.edit;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import de.soundboardcrafter.R;

/**
 * Activity for editing a single sound (name, volume etc.).
 */
public class SoundEditActivity extends AppCompatActivity {
    // TODO Edit sound

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_edit);
    }
}
