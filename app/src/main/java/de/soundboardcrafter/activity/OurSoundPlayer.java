package de.soundboardcrafter.activity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;

import de.soundboardcrafter.R;

public class OurSoundPlayer {
    public static final int S1 = R.raw.trailer2;
    private static SoundPool soundPool;
    private static HashMap<Integer, Integer> soundPoolMap;

    /**
     * Populate the SoundPool
     */
    public static void initSounds(Context context) {
        AudioAttributes attribute = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA).build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attribute).setMaxStreams(2).build();
        soundPoolMap = new HashMap(3);
        soundPoolMap.put(S1, soundPool.load(context, R.raw.trailer2, 1));
    }

    /** Play a given sound in the soundPool */
    public static void playSound(Context context, int soundID) {
        if(soundPool == null || soundPoolMap == null){
            initSounds(context);
        }
        float volume = 0.5f;// whatever in the range = 0.0 to 1.0
        // play sound with same right and left volume, with a priority of 1,
        // zero repeats (i.e play once), and a playback rate of 1f
        soundPool.play(soundPoolMap.get(soundID), volume, volume, 1, 0, 1f);
    }
}
