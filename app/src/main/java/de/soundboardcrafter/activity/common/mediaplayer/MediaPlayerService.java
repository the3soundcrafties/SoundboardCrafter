package de.soundboardcrafter.activity.common.mediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
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

import java.util.Iterator;
import java.util.UUID;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.soundboard.play.SoundboardPlayActivity;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Android service that allows playing media and also keeps track off
 * most of the media players that are playing sounds in the app.
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
    private static final int REQUEST_CODE_STOP = 1;

    private PendingIntent launchUIPendingIntent;
    private MediaSessionCompat mediaSession;

    /**
     * The interface through which other components can interact with the service
     */
    private final IBinder binder = new Binder();

    /**
     * The media players registered in this service. This does also contain mediaPlayers
     * that are currently fading out!
     */
    private final SoundboardMediaPlayers mediaPlayers = new SoundboardMediaPlayers();

    public MediaPlayerService() {
        Log.d(TAG, "MediaPlayerService is created");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, SoundboardPlayActivity.class);

        // TODO Click on the notification returns to the main activity -
        // even if the user is currently in a different activity.
        // How can we just return TO THE APP?

        // notificationIntent.setFlags( ?!
        // PendingIntent.FLAG?!
        //Intent.FLAG?!

        launchUIPendingIntent =
                PendingIntent.getActivity(this, 0,
                        notificationIntent, 0);

        mediaSession =
                new MediaSessionCompat(this, MEDIA_SESSION_TAG);
        mediaSession.setSessionActivity(launchUIPendingIntent);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                Log.d(TAG, "onPause");
                // As many speakers do not have a stop button, we stop the playing here.
                // We do not offer resuming anyway.
                stopPlaying(true);
                super.onPause();
            }

            @Override
            public void onStop() {
                Log.d(TAG, "onStop");
                stopPlaying(true);
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
        createNotificationChannel();

        handleIntent(intent);

        return Service.START_STICKY;
    }

    private void createNotificationChannel() {
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
                stopPlaying(true);
                break;
        }
    }

    public void setOnPlayingStopped(Soundboard soundboard, Sound sound,
                                    SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        mediaPlayers.setOnPlayingStopped(soundboard, sound, onPlayingStopped);
    }

    /**
     * Sets the volume for this sound.
     */
    public void setVolumePercentage(@NonNull UUID soundId, int volumePercentage) {
        checkNotNull(soundId, "soundId is null");

        mediaPlayers.setVolumePercentage(soundId, volumePercentage);
    }

    /**
     * Sets whether this sound shall be played in a loop.
     */
    public void setLoop(@NonNull UUID soundId, boolean loop) {
        checkNotNull(soundId, "soundId is null");

        mediaPlayers.setLoop(soundId, loop);
    }

    /**
     * Stops all playing sounds in these soundboards
     *
     * @param fadeOut Whether the playing shall be faded out.
     */
    public void stopPlaying(Iterable<Soundboard> soundboards, boolean fadeOut) {
        mediaPlayers.stopPlaying(soundboards, fadeOut);
        updateMediaSessionNotificationAndForegroundService();
    }

    /**
     * Stops this sound when it's played in this soundboard
     *
     * @param fadeOut Whether the playing shall be faded out.
     */
    public void stopPlaying(@Nullable Soundboard soundboard, @NonNull Sound sound,
                            boolean fadeOut) {
        checkNotNull(sound, "sound is null");

        mediaPlayers.stopPlaying(soundboard, sound, fadeOut);
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
     *
     * @see #play(String, String, SoundboardMediaPlayer.OnPlayingStopped)
     */
    public void play(@Nullable Soundboard soundboard, @NonNull Sound sound,
                     @Nullable SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        checkNotNull(sound, "sound is null");

        SoundboardMediaPlayer mediaPlayer = mediaPlayers.get(soundboard, sound);
        if (mediaPlayer == null) {
            mediaPlayer = new SoundboardMediaPlayer();
            mediaPlayer.setOnPlayingStopped(onPlayingStopped);
            initMediaPlayer(sound, mediaPlayer);
            mediaPlayers.putActive(soundboard, sound, mediaPlayer);
        } else {
            // update the callbacks
            mediaPlayer.setOnPlayingStopped(onPlayingStopped);
            mediaPlayer.reset();
            initMediaPlayer(sound, mediaPlayer);
            mediaPlayers.putActive(soundboard, sound, mediaPlayer);
        }

        mediaPlayer.prepareAsync();

        updateMediaSessionNotificationAndForegroundService();
    }

    /**
     * Starts playing from the path - without adding a media player and without
     * necessarily starting a foreground service etc.
     *
     * @see #play(Soundboard, Sound, SoundboardMediaPlayer.OnPlayingStopped)
     */
    public SoundboardMediaPlayer play(@NonNull String name,
                                      @NonNull String path,
                                      @Nullable SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        checkNotNull(path, "path is null");

        SoundboardMediaPlayer mediaPlayer = new SoundboardMediaPlayer();
        mediaPlayer.setOnPlayingStopped(() -> {
            if (onPlayingStopped != null) {
                onPlayingStopped.stop();
            }
        });
        initMediaPlayer(mediaPlayer, name, path, 100, false);

        mediaPlayer.prepareAsync();

        return mediaPlayer;
    }

    /*
    For apps that target Android 5.0 (API level 21) and later, audio apps should use AudioAttributes to describe the type of audio your app is playing. For example, apps that play speech should specify CONTENT_TYPE_SPEECH.
    Apps running Android 8.0 (API level 26) or greater should use the requestAudioFocus() method, which takes an AudioFocusRequest parameter. The AudioFocusRequest contains information about the audio context and capabilities of your app. The system uses this information to manage the gain and loss of audio focus automatically.
    */

    private void updateMediaSessionNotificationAndForegroundService() {
        if (mediaPlayers.activePlayersEmpty()) {
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
                        .setContentIntent(launchUIPendingIntent)
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
                .setActions(0)
                .setState(PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1.0f)
                .build();
    }

    private PlaybackStateCompat createPlaybackStatePlaying() {
        return new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE)
                .setState(PlaybackStateCompat.STATE_PLAYING,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build();
    }

    private PendingIntent createStopPendingIntent() {
        Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        intent.setAction(ACTION_STOP);
        return PendingIntent.getService(getApplicationContext(), REQUEST_CODE_STOP, intent, 0);
    }

    private enum SummaryStyle {
        SHORT, LONG
    }

    private String buildSummary(SummaryStyle style) {
        StringBuilder res = new StringBuilder();
        for (Iterator<SoundboardMediaPlayer> playerIt = mediaPlayers.activePlayersIterator();
             playerIt.hasNext(); ) {
            SoundboardMediaPlayer player = playerIt.next();
            if (res.length() > 0) {
                res.append(", ");
            }

            res.append(player.getSoundName());
            if (style == SummaryStyle.SHORT && res.length() > MAX_NOTIFICATION_LENGTH) {
                int numSoundsPlaying = mediaPlayers.sizeActivePlayers();
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
        initMediaPlayer(mediaPlayer, sound.getName(), sound.getPath(),
                sound.getVolumePercentage(), sound.isLoop());
    }

    /**
     * Initializes this mediaPlayer. Does not start playing yet.
     */
    private void initMediaPlayer(SoundboardMediaPlayer mediaPlayer, String soundName,
                                 String soundPath, int volumePercentage, boolean loop) {
        SoundboardMediaPlayers.initMediaPlayer(mediaPlayer, soundName, soundPath, volumePercentage, loop);

        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnErrorListener((ev, what, extra) -> onError(mediaPlayer, what, extra));
        mediaPlayer.setOnPreparedListener(this::onPrepared);
        mediaPlayer.setOnCompletionListener((mbd) -> onCompletion((SoundboardMediaPlayer) mbd));
    }

    /**
     * Return whether this sound is <i>actively playing</i>, that is,
     * it is playing <i>and not fading out</i>.
     */
    public boolean isActivelyPlaying(@NonNull Sound sound) {
        checkNotNull(sound, "sound is null");

        return mediaPlayers.isActivelyPlaying(sound);
    }

    /**
     * Return whether this sound is <i>actively playing</i> in this soundboard, that is,
     * it is playing <i>and not fading out</i>.
     */
    public boolean isActivelyPlaying(@NonNull Soundboard soundboard, @NonNull Sound sound) {
        checkNotNull(soundboard, "soundboard is null");
        checkNotNull(sound, "sound is null");

        return mediaPlayers.isActivelyPlaying(soundboard, sound.getId());
    }

    /**
     * Called when MediaPlayer is ready
     */
    private void onPrepared(MediaPlayer player) {
        player.start();
    }

    private boolean onError(SoundboardMediaPlayer player, int what, int extra) {
        Log.e(TAG, "Error in media player: what: " + what + " extra: " + extra);

        mediaPlayers.remove(player);
        updateMediaSessionNotificationAndForegroundService();
        return true;
    }

    private void onCompletion(SoundboardMediaPlayer player) {
        mediaPlayers.remove(player);
        updateMediaSessionNotificationAndForegroundService();
    }

    @Override
    public void onDestroy() {
        stopPlaying(false);

        mediaSession.setActive(false);
        mediaSession.release();

        super.onDestroy();
    }

    /**
     * Stops all playing sounds in all soundboards
     *
     * @param fadeOut Whether the playing shall be faded out.
     */
    private void stopPlaying(boolean fadeOut) {
        mediaPlayers.stopPlaying(fadeOut);
        updateMediaSessionNotificationAndForegroundService();
    }
}
