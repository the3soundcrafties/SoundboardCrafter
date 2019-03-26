package de.soundboardcrafter.activity.soundboard.play;

import androidx.fragment.app.Fragment;
import de.soundboardcrafter.activity.common.SingleFragmentActivity;

/**
 * The main activity, showing the soundboards.
 */
public class MainActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new SoundboardFragment();
    }
}
