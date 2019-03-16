package de.soundboardcrafter.activity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.HashMap;

import javax.annotation.Nonnull;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Manage playing Songs.
 */
public class OurSoundPlayer {
   public static HashMap<String, MediaPlayer> currentPlayer = new HashMap<>();

    /**
     * Play a given sound in a mediaplayer
     */
    public static void playSound(Activity activity, Uri pathSoundFile) {
        checkPermission(activity);
        MediaPlayer mediaPlayer = null;
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
            mediaPlayer.setDataSource(activity, pathSoundFile);
            mediaPlayer.prepare();
        } catch (IOException e) {
            // TODO: 16.03.2019 richtiges handling
            e.printStackTrace();
        }
        currentPlayer.put(pathSoundFile.getPath(), mediaPlayer);
        mediaPlayer.start();
    }

    public static boolean isPlaying(@Nonnull Activity activity, @Nonnull Uri path) {
        Preconditions.checkNotNull(activity, "activity is null");
        Preconditions.checkNotNull(path, "path is null");

        MediaPlayer mediaPlayer = currentPlayer.get(path.getPath());
        if(mediaPlayer ==null){
            return false;
        }
        return mediaPlayer.isPlaying();
    }


    public static void stopSound(Activity activity, Uri path) {
        Preconditions.checkNotNull(activity, "activity is null");
        Preconditions.checkNotNull(path, "path is null");

        MediaPlayer mediaPlayer = currentPlayer.get(path.getPath());
        if(mediaPlayer == null) {
            return;
        }
        currentPlayer.remove(path.getPath());
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    public static void checkPermission(Activity activity) {
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
            // TODO: 16.03.2019 wenn berechtigung nicht da ist soll die Anwendung nicht abstürzen Message bringen
        }
    }

}
