package de.soundboardcrafter.activity.common.mediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.soundboard.play.SoundboardPlayActivity;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Android service that keeps track off all the media players that are playing sounds in the app.
 */
@MainThread
public class MediaPlayerService extends Service {
    private static final String TAG = MediaPlayerService.class.getName();

    private static final String NOTIFICATION_CHANNEL_ID = "mediaPlayerNotificationChannel";

    private static final int ONGOING_NOTIFICATION_ID = 1;
    // https://material.io/design/platform-guidance/android-notifications.html#style :
    // "Avoid exceeding the 40-character limit"
    private static final int MAX_NOTIFICATION_LENGTH = 40;
    private static final String MEDIA_SESSION_TAG = "SOUNDBOARD_CRAFTER_MEDIA_SESSION";

    private static final String ACTION_STOP = "action_stop";

    private Intent notificationIntent;
    private PendingIntent pendingIntent;
    private MediaSessionCompat mediaSession;

    /**
     * The interface through which other components can interact with the service
     */
    private final IBinder binder = new Binder();

    private final HashMap<MediaPlayerSearchId, SoundboardMediaPlayer> mediaPlayers = new HashMap<>();

    public MediaPlayerService() {
        Log.d(TAG, "MediaPlayerService is created");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationIntent = new Intent(this, SoundboardPlayActivity.class);

        // TODO Click on the notification returns to the main activity -
        // even if the user is currently in a different activity.
        // How can we just return TO THE APP?

        // notificationIntent.setFlags( ?!
        // PendingIntent.FLAG?!
        //Intent.FLAG?!

        pendingIntent =
                PendingIntent.getActivity(this, 0,
                        notificationIntent, 0);

        mediaSession =
                new MediaSessionCompat(this, MEDIA_SESSION_TAG);
        mediaSession.setSessionActivity(pendingIntent);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                Log.d(TAG, "onMediaButtonEvent");
                return super.onMediaButtonEvent(mediaButtonEvent);
            }

            @Override
            public void onPlay() {
                Log.d(TAG, "onPlay");
                super.onPlay();
            }

            @Override
            public void onPlayFromSearch(String query, Bundle extras) {
                Log.d(TAG, "onPlayFromSearch");
                super.onPlayFromSearch(query, extras);
            }

            @Override
            public void onPrepare() {
                Log.d(TAG, "onPrepare");
                super.onPrepare();
            }

            @Override
            public void onPrepareFromSearch(String query, Bundle extras) {
                Log.d(TAG, "onPrepareFromSearch");
                super.onPrepareFromSearch(query, extras);
            }

            @Override
            public void onStop() {
                Log.d(TAG, "onStop");
                stopPlaying();
                super.onStop();
            }
        });
        mediaSession.setPlaybackState(createPlaybackStateNotPlaying());
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);

        createNotificationChanel();

        handleIntent(intent);

        return Service.START_STICKY;
    }

    private void createNotificationChanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.media_player_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.media_player_notification_channel_description));
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManagerCompat.from(this)
                    .createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    private void handleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        @Nullable String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case ACTION_STOP:
                stopPlaying();
                break;
        }
    }

    public void setOnPlayingStopped(Soundboard soundboard, Sound sound,
                                    SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            player.setOnPlayingStopped(onPlayingStopped);
        }
    }

    /**
     * Sets the volume for this sound.
     */
    public void setVolumePercentage(UUID soundId, int volumePercentage) {
        checkNotNull(soundId, "soundId is null");

        setVolume(soundId, percentageToVolume(volumePercentage));
    }

    /**
     * Sets the volume for this sound.
     */
    private void setVolume(UUID soundId, float volume) {
        checkNotNull(soundId, "soundId is null");

        mediaPlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(soundId))
                .map(Map.Entry::getValue)
                .forEach(m -> setVolume(m, volume));
    }

    /**
     * Stops all playing sounds in these soundboards
     */
    public void stopPlaying(Iterable<Soundboard> soundboards) {
        for (Soundboard soundboard : soundboards) {
            stopPlaying(soundboard);
        }
    }

    /**
     * Stops all playing sounds in this soundboard
     */
    private void stopPlaying(@NonNull Soundboard soundboard) {
        for (Iterator<Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer>> entryIt =
             mediaPlayers.entrySet().iterator(); entryIt.hasNext(); ) {
            Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer> entry = entryIt.next();
            if (soundboard.getId().equals(entry.getKey().getSoundboardId())) {
                SoundboardMediaPlayer player = entry.getValue();
                player.stop();
                player.release();
                entryIt.remove();
            }
        }

        updateMediaSessionNotificationAndForegroundService();
    }

    /**
     * Stops this sound when it's played in this soundboard
     */
    public void stopPlaying(@Nullable Soundboard soundboard, @NonNull Sound sound) {
        checkNotNull(sound, "sound is null");

        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            stop(player);
        }
    }

    /**
     * Stops this player and removes it.
     */
    private void stop(SoundboardMediaPlayer player) {
        player.stop();
        removeMediaPlayer(player);
    }

    private void removeMediaPlayer(@NonNull SoundboardMediaPlayer mediaPlayer) {
        mediaPlayer.release();
        mediaPlayers.values().remove(mediaPlayer);
        updateMediaSessionNotificationAndForegroundService();
    }

    /**
     * The interface through which other components can interact with the service.
     */
    public class Binder extends android.os.Binder {
        @UiThread
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    /**
     * Adds a media player and starts playing.
     */
    public void play(@Nullable Soundboard soundboard, @NonNull Sound sound,
                     @Nullable SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        checkNotNull(sound, "sound is null");

        MediaPlayerSearchId key = new MediaPlayerSearchId(soundboard, sound);
        SoundboardMediaPlayer mediaPlayer = mediaPlayers.get(key);
        if (mediaPlayer == null) {
            mediaPlayer = new SoundboardMediaPlayer();
            mediaPlayer.setOnPlayingStopped(onPlayingStopped);
            initMediaPlayer(sound, mediaPlayer);
            mediaPlayers.put(key, mediaPlayer);
        } else {
            // update the callbacks
            mediaPlayer.setOnPlayingStopped(onPlayingStopped);
            mediaPlayer.reset();
            initMediaPlayer(sound, mediaPlayer);
        }

        mediaPlayer.prepareAsync();

        updateMediaSessionNotificationAndForegroundService();
    }

    /**
     * Starts playing from the path - without adding a media player and without
     * necessarily starting a foreground service etc.
     */
    public SoundboardMediaPlayer play(@NonNull String name,
                                      @NonNull String path,
                                      @Nullable SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        checkNotNull(path, "path is null");

        SoundboardMediaPlayer mediaPlayer = new SoundboardMediaPlayer();
        mediaPlayer.setOnPlayingStopped(new SoundboardMediaPlayer.OnPlayingStopped() {
            @Override
            public void stop() {
                onPlayingStopped.stop();
                removeMediaPlayer(mediaPlayer);
            }
        });
        initMediaPlayer(mediaPlayer, name, path, 100, false);

        mediaPlayer.prepareAsync();

        return mediaPlayer;
    }

    private void updateMediaSessionNotificationAndForegroundService() {
        if (mediaPlayers.isEmpty()) {
            mediaSession.setPlaybackState(createPlaybackStateNotPlaying());
            mediaSession.setMetadata(null);
            stopForeground(true);
            return;
        }

        String shortSummary = buildSummary(SummaryStyle.SHORT);
        String longSummary = buildSummary(SummaryStyle.LONG);

        PendingIntent stopPendingIntent = createStopPendingIntent();

        mediaSession.setPlaybackState(createPlaybackStatePlaying());
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, longSummary)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, longSummary)
                .build());

        MediaSessionCompat.Token sessionToken = mediaSession.getSessionToken();

        androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                new androidx.media.app.NotificationCompat.MediaStyle()
                        .setCancelButtonIntent(stopPendingIntent)
                        .setMediaSession(sessionToken)
                        .setShowActionsInCompactView(0)
                        .setShowCancelButton(true);

        NotificationCompat.Action stopAction =
                new NotificationCompat.Action(R.drawable.ic_stop_notification,
                        getString(R.string.media_player_notification_stop_action_title),
                        stopPendingIntent);

        Notification notification =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        // .setColor(ContextCompat.getColor(mContext, R.color.notification_bg))
                        .setContentTitle(getString(R.string.media_player_notification_title))
                        .setContentText(shortSummary)
                        .setTicker(shortSummary)
                        // .setDeleteIntent() Deletion impossible for FOREGROUND notification!
                        .setOnlyAlertOnce(true)
                        // TODO Use application icon
                        .setSmallIcon(R.drawable.ic_media_player_notification_icon)
                        .addAction(stopAction)
                        .setStyle(mediaStyle)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        // Do not show time
                        .setShowWhen(false)
                        .build();

        // https://medium.com/androiddevelopers/migrating-mediastyle-notifications-to-support-android-o-29c7edeca9b7

        // Without this, the service will be killed shortly when the
        // user leaves the app and closes the devices
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private PlaybackStateCompat createPlaybackStateNotPlaying() {
        return new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                        PlaybackStateCompat.ACTION_PREPARE |
                        PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
                        | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1.0f)
                .build();
    }

    private PlaybackStateCompat createPlaybackStatePlaying() {
        return new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                        PlaybackStateCompat.ACTION_PREPARE |
                        PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
                        | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(PlaybackStateCompat.STATE_PLAYING,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build();
    }

    private PendingIntent createStopPendingIntent() {
        Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        intent.setAction(ACTION_STOP);
        return PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        // return MediaButtonReceiver.buildMediaButtonPendingIntent(this, ACTION_STOP);
    }

    private enum SummaryStyle {
        SHORT, LONG
    }

    private String buildSummary(SummaryStyle style) {
        StringBuilder res = new StringBuilder();
        for (SoundboardMediaPlayer player : mediaPlayers.values()) {
            if (res.length() > 0) {
                res.append(", ");
            }

            res.append(player.getSoundName());
            if (style == SummaryStyle.SHORT && res.length() > MAX_NOTIFICATION_LENGTH) {
                int numSoundsPlaying = mediaPlayers.size();
                return getResources().getQuantityString(
                        R.plurals.media_player_notification_default_text,
                        numSoundsPlaying, numSoundsPlaying);
            }
        }

        return res.toString();
    }

    /**
     * Initializes this mediaPlayer for this sound. Does not start playing yet.
     */
    private void initMediaPlayer(@NonNull Sound sound, SoundboardMediaPlayer mediaPlayer) {
        String soundName = sound.getName();
        String soundPath = sound.getPath();
        int volumePercentage = sound.getVolumePercentage();
        boolean loop = sound.isLoop();

        initMediaPlayer(mediaPlayer, soundName, soundPath, volumePercentage, loop);
    }

    /**
     * Initializes this mediaPlayer. Does not start playing yet.
     */
    private void initMediaPlayer(SoundboardMediaPlayer mediaPlayer, String soundName,
                                 String soundPath, int volumePercentage, boolean loop) {
        mediaPlayer.setSoundName(soundName);
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnErrorListener((ev, what, extra) -> onError(mediaPlayer, what, extra));
        try {
            mediaPlayer.setDataSource(soundPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
        setVolume(mediaPlayer, percentageToVolume(volumePercentage));
        mediaPlayer.setLooping(loop);
        mediaPlayer.setOnPreparedListener(this::onPrepared);
        mediaPlayer.setOnCompletionListener((mbd) -> onCompletion((SoundboardMediaPlayer) mbd));
    }

    private static float percentageToVolume(int volumePercentage) {
        return (float) volumePercentage / 100f;
    }

    /**
     * Sets the volume for this <code>mediaPlayer</code>.
     */
    private void setVolume(SoundboardMediaPlayer mediaPlayer, float volume) {
        checkNotNull(mediaPlayer, "mediaPlayer is null");
        mediaPlayer.setVolume(volume, volume);
    }

    public boolean isPlaying(@NonNull Sound sound) {
        checkNotNull(sound, "sound is null");

        return mediaPlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(sound.getId()))
                .map(Map.Entry::getValue)
                .anyMatch(SoundboardMediaPlayer::isPlaying);
    }

    public boolean isPlaying(@NonNull Soundboard soundboard, @NonNull Sound sound) {
        checkNotNull(soundboard, "soundboard is null");
        checkNotNull(sound, "sound is null");

        return isPlaying(soundboard, sound.getId());
    }

    private boolean isPlaying(@NonNull Soundboard soundboard, @NonNull UUID soundId) {
        SoundboardMediaPlayer mediaPlayer = mediaPlayers.get(
                new MediaPlayerSearchId(soundboard.getId(), soundId));
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    /**
     * Called when MediaPlayer is ready
     */
    private void onPrepared(MediaPlayer player) {
        player.start();
    }

    private boolean onError(SoundboardMediaPlayer player, int what, int extra) {
        Log.e(TAG, "Error in media player: what: " + what + " extra: " + extra);

        removeMediaPlayer(player);
        return true;
    }

    private void onCompletion(SoundboardMediaPlayer player) {
        removeMediaPlayer(player);
    }

    @Override
    public void onDestroy() {
        stopPlaying();

        mediaSession.setActive(false);
        mediaSession.release();

        super.onDestroy();
    }

    /**
     * Stops all playing sounds in all soundboards
     */
    private void stopPlaying() {
        for (Iterator<SoundboardMediaPlayer> playerIt =
             mediaPlayers.values().iterator(); playerIt.hasNext(); ) {
            SoundboardMediaPlayer player = playerIt.next();
            player.stop();
            player.release();
            playerIt.remove();
        }

        mediaPlayers.clear();

        updateMediaSessionNotificationAndForegroundService();
    }
}
