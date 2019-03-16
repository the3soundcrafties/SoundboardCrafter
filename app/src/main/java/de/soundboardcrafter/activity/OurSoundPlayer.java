package de.soundboardcrafter.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;

import java.io.IOException;
import java.util.HashMap;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.soundboardcrafter.R;

public class OurSoundPlayer {

    /** Play a given sound in the soundPool */
    public static void playSound(Activity activity, Uri pathSoundFile) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
        try {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);

                // PERMISSION_REQUEST_CODE is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

            mediaPlayer.setDataSource(activity, pathSoundFile);
            mediaPlayer.prepare();
        } catch (IOException e) {
            // TODO: 16.03.2019 richtiges handling
            e.printStackTrace();
        }

        mediaPlayer.start();
    }
}
