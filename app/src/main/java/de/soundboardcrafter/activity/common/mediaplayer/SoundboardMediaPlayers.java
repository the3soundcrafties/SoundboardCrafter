package de.soundboardcrafter.activity.common.mediaplayer;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

class SoundboardMediaPlayers {
    /**
     * The players that are <i>actively playing</i>, that is, they are <i>not</i> fading out.
     */
    private final HashMap<MediaPlayerSearchId, SoundboardMediaPlayer> activePlayers =
            new HashMap<>();

    /**
     * The players that are <i>not</i> fading out.
     */
    private final HashMap<MediaPlayerSearchId, SoundboardMediaPlayer> playersFadingOut =
            new HashMap<>();

    /**
     * {@link Handler} object that's attached to the UI thread. Used to post
     * future actions to the players to fade them out.
     */
    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * {@link java.lang.Runnable}  that does a single fade-out step for all players that
     * shall be faded out - and schedules itself for the next step, if necessary.
     */
    private final Fader fader = new Fader();

    @UiThread
    boolean activePlayersEmpty() {
        return activePlayers.isEmpty();
    }

    @UiThread
    int sizeActivePlayers() {
        return activePlayers.size();
    }

    /**
     * Initializes this mediaPlayer. Does not start playing yet.
     *
     * @throws IOException In case of an I/O problem (no audio file at <code>soundPath</code>, e.g.)
     */
    static void initMediaPlayer(Context context,
                                SoundboardMediaPlayer mediaPlayer, String soundName,
                                @NonNull AbstractAudioLocation audioLocation, int volumePercentage,
                                boolean loop)
            throws IOException {
        mediaPlayer.setSoundName(soundName);
        initDataSource(context, mediaPlayer, audioLocation);
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
        SoundboardMediaPlayers.setVolume(
                mediaPlayer,
                SoundboardMediaPlayers.percentageToVolume(volumePercentage));
        mediaPlayer.setLooping(loop);
    }

    private static void initDataSource(Context context, SoundboardMediaPlayer mediaPlayer,
                                       @NonNull AbstractAudioLocation audioLocation)
            throws IOException {
        if (audioLocation instanceof FileSystemFolderAudioLocation) {
            mediaPlayer.setDataSource(
                    ((FileSystemFolderAudioLocation) audioLocation).getInternalPath());
        } else if (audioLocation instanceof AssetFolderAudioLocation) {
            @NonNull String assetPath =
                    ((AssetFolderAudioLocation) audioLocation).getInternalPath();
            try (AssetFileDescriptor fileDescriptor = context.getAssets().openFd(assetPath)) {
                // Javadoc: "It is the caller's responsibility to close the file descriptor. It is
                // safe to do so as soon as this call returns."
                mediaPlayer.setDataSource(
                        fileDescriptor.getFileDescriptor(),
                        fileDescriptor.getStartOffset(),
                        fileDescriptor.getLength());
            }
        } else {
            throw new IllegalStateException("Unexpected audio location type: " +
                    audioLocation.getClass());
        }
    }

    void setOnPlayingStopped(@Nullable Soundboard soundboard, Sound sound,
                             @Nullable SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        MediaPlayerSearchId searchId = new MediaPlayerSearchId(soundboard, sound);

        setOnPlayingStopped(searchId, onPlayingStopped);
    }

    private void setOnPlayingStopped(MediaPlayerSearchId searchId, @Nullable
            SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        SoundboardMediaPlayer activePlayer = activePlayers.get(searchId);
        if (activePlayer != null) {
            activePlayer.setOnPlayingStopped(onPlayingStopped);
        }
        SoundboardMediaPlayer playerFadingOut =
                playersFadingOut.get(searchId);
        if (playerFadingOut != null) {
            playerFadingOut.setOnPlayingStopped(onPlayingStopped);
        }
    }

    /**
     * Return whether this sound is <i>actively playing</i> in this soundboard, e.g.
     * it is playing <i>and not fading out</i>.
     */
    @UiThread
    boolean isActivelyPlaying(@NonNull Soundboard soundboard, @NonNull UUID soundId) {
        SoundboardMediaPlayer mediaPlayer = activePlayers.get(
                new MediaPlayerSearchId(soundboard.getId(), soundId));
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }


    /**
     * Returns the media player for this sound in this soundboard.
     * The media player may be <i>actively playing</i> or <i>fading out</i>.
     */
    @UiThread
    SoundboardMediaPlayer get(@Nullable Soundboard soundboard, Sound sound) {
        MediaPlayerSearchId searchId = new MediaPlayerSearchId(soundboard, sound);
        return get(searchId);
    }

    private SoundboardMediaPlayer get(MediaPlayerSearchId searchId) {
        @Nullable SoundboardMediaPlayer activePlayer = activePlayers.get(searchId);
        if (activePlayer != null) {
            return activePlayer;
        }

        return playersFadingOut.get(searchId);
    }

    /**
     * Stops this sound when it's played in this soundboard
     *
     * @param fadeOut Whether the playing shall be faded out.
     */
    @UiThread
    void stopPlaying(@Nullable Soundboard soundboard, @NonNull Sound sound, boolean fadeOut) {
        checkNotNull(sound, "sound is null");

        MediaPlayerSearchId searchId = new MediaPlayerSearchId(soundboard, sound);

        SoundboardMediaPlayer activePlayer = activePlayers.get(searchId);
        if (activePlayer != null) {
            stop(searchId, activePlayer, fadeOut);
        } else if (!fadeOut) {
            SoundboardMediaPlayer playerFadingOut =
                    playersFadingOut.get(searchId);
            if (playerFadingOut != null) {
                stop(searchId, playerFadingOut, false);
            }
        }
    }

    /**
     * Stops this player and removes it (at least after fading out, if desired).
     *
     * @param fadeOut Whether the playing shall be faded out.
     */
    @UiThread
    private void stop(MediaPlayerSearchId searchId, SoundboardMediaPlayer player, boolean fadeOut) {
        if (!fadeOut) {
            player.stop();
            remove(player);
            return;
        }

        fadeOut(searchId, player);
    }

    /**
     * Removes this player from the active players and the players fading out.
     */
    @UiThread
    void remove(SoundboardMediaPlayer mediaPlayer) {
        mediaPlayer.release();
        activePlayers.values().remove(mediaPlayer);
        playersFadingOut.values().remove(mediaPlayer);
    }

    /**
     * Returns whether this sound is <i>actively playing</i>, that is,
     * it is playing <i>and not fading out</i>.
     */
    @UiThread
    boolean isActivelyPlaying(@NonNull Sound sound) {
        checkNotNull(sound, "sound is null");

        return activePlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(sound.getId()))
                .map(Map.Entry::getValue)
                .anyMatch(SoundboardMediaPlayer::isPlaying);
    }

    /**
     * Returns the IDs of the sounds that are   <i>actively playing</i>:
     * Playing <i>and not fading out</i>.
     */
    Collection<UUID> getSoundIdsActivelyPlaying() {
        return activePlayers.entrySet().stream()
                .filter(e -> e.getValue().isPlaying())
                .map(e -> e.getKey().getSoundId())
                .collect(ImmutableSet.toImmutableSet());
    }

    /**
     * Return whether in this soundboard there is more than one sound <i>actively playing</i>.
     * To be <i>actively playing</i> means, a sound is playing <i>and not fading out</i>.
     */
     boolean isActivelyPlayingMultipleSounds(@NonNull Soundboard soundboard) {
         checkNotNull(soundboard, "soundboard is null");

        return activePlayers.entrySet().stream()
                .filter(e -> e.getValue().isPlaying())
                .filter(e -> soundboard.getId().equals(e.getKey().getSoundboardId()))
                .count() > 1;
    }

    /**
     * Return whether in this soundboard there is currently some sound <i>actively playing</i>.
     * To be <i>actively playing</i> means, the sound is playing <i>and not fading out</i>.
     */
    boolean isActivelyPlaying(@NonNull Soundboard soundboard) {
        checkNotNull(soundboard, "soundboard is null");

        return activePlayers.entrySet().stream()
                .filter(e -> e.getValue().isPlaying())
                .anyMatch(e -> soundboard.getId().equals(e.getKey().getSoundboardId()));
    }

    /**
     * Sets the volume for this sound.
     */
    @UiThread
    void setVolumePercentage(UUID soundId, int volumePercentage) {
        checkNotNull(soundId, "soundId is null");

        setVolume(soundId, SoundboardMediaPlayers.percentageToVolume(volumePercentage));
    }

    /**
     * Sets the volume for this sound.
     */
    @UiThread
    private void setVolume(@NonNull UUID soundId, float volume) {
        checkNotNull(soundId, "soundId is null");

        activePlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(soundId))
                .map(Map.Entry::getValue)
                .forEach(m -> setVolume(m, volume));

        playersFadingOut.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(soundId))
                .map(Map.Entry::getValue)
                .forEach(m -> setVolume(m, volume));
    }

    /**
     * Sets the volume for this <code>mediaPlayer</code>.
     */
    @UiThread
    private static void setVolume(@NonNull SoundboardMediaPlayer mediaPlayer, float volume) {
        checkNotNull(mediaPlayer, "mediaPlayer is null");
        mediaPlayer.setVolume(volume);
    }

    @UiThread
    private static float percentageToVolume(int volumePercentage) {
        return (float) volumePercentage / 100f;
    }

    /**
     * Sets whether this sound shall be played in a loop.
     */
    @UiThread
    void setLoop(UUID soundId, boolean loop) {
        checkNotNull(soundId, "soundId is null");

        activePlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(soundId))
                .map(Map.Entry::getValue)
                .forEach(m -> setLoop(m, loop));

        playersFadingOut.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(soundId))
                .map(Map.Entry::getValue)
                .forEach(m -> setLoop(m, loop));
    }

    /**
     * Sets whether this <code>mediaPlayer</code> shall play in a loop.
     */
    @UiThread
    private static void setLoop(@NonNull SoundboardMediaPlayer mediaPlayer, boolean loop) {
        checkNotNull(mediaPlayer, "mediaPlayer is null");
        mediaPlayer.setLooping(loop);
    }

    /**
     * Stops all playing sounds in these soundboards
     *
     * @param fadeOut Whether the playing shall be faded out.
     */
    @UiThread
    void stopPlaying(Iterable<Soundboard> soundboards, boolean fadeOut) {
        for (Soundboard soundboard : soundboards) {
            stopPlaying(soundboard, fadeOut);
        }
    }

    /**
     * Stops all playing sounds in this soundboard
     *
     * @param fadeOut Whether the playing shall be faded out.
     */
    @UiThread
    private void stopPlaying(@NonNull Soundboard soundboard, boolean fadeOut) {
        stopPlaying(searchId -> soundboard.getId().equals(searchId.getSoundboardId()), fadeOut);
    }

    /**
     * Stops this sound (which might be played from any soundboard);
     *
     * @param fadeOut Whether the playing shall be faded out.
     */
    @UiThread
    public void stopPlaying(@NonNull Sound sound, boolean fadeOut) {
        stopPlaying(searchId -> sound.getId().equals(searchId.getSoundId()), fadeOut);
    }

    private void stopPlaying(Predicate<MediaPlayerSearchId> filter, boolean fadeOut) {
        for (Iterator<Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer>> entryIt =
             activePlayers.entrySet().iterator(); entryIt.hasNext(); ) {
            Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer> entry = entryIt.next();
            MediaPlayerSearchId searchId = entry.getKey();

            if (filter.test(searchId)) {
                SoundboardMediaPlayer player = entry.getValue();
                if (!fadeOut) {
                    player.stop();
                    player.release();
                } else {
                    player.playingLogicallyStopped();
                    startFaderIfNotRunning();
                    playersFadingOut.put(searchId, player);
                }
                entryIt.remove();
            }
        }

        if (!fadeOut) {
            for (Iterator<Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer>> entryIt =
                 playersFadingOut.entrySet().iterator(); entryIt.hasNext(); ) {
                Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer> entry = entryIt.next();
                if (filter.test(entry.getKey())) {
                    SoundboardMediaPlayer player = entry.getValue();
                    player.stop();
                    player.release();
                    entryIt.remove();
                }
            }
        }
    }

    /**
     * Stops all playing sounds in all soundboards
     *
     * @param fadeOut Whether the playing shall be faded out.
     */
    @UiThread
    void stopPlaying(boolean fadeOut) {
        for (Map.Entry<MediaPlayerSearchId, SoundboardMediaPlayer> entry :
                activePlayers.entrySet()) {
            MediaPlayerSearchId searchId = entry.getKey();
            SoundboardMediaPlayer player = entry.getValue();

            if (!fadeOut) {
                player.stop();
                player.release();
            } else {
                player.playingLogicallyStopped();
                startFaderIfNotRunning();
                playersFadingOut.put(searchId, player);
            }
        }

        activePlayers.clear();

        if (!fadeOut) {
            for (Iterator<SoundboardMediaPlayer> playerIt = playersFadingOutIterator();
                 playerIt.hasNext(); ) {
                SoundboardMediaPlayer player = playerIt.next();
                player.stop();
                player.release();
            }

            playersFadingOut.clear();
        }
    }

    private void fadeOut(MediaPlayerSearchId searchId, SoundboardMediaPlayer player) {
        player.playingLogicallyStopped();
        startFaderIfNotRunning();
        putFadingOut(searchId, player);
    }

    /**
     * If there is currently no {@link Fader} running, start one.
     */
    private void startFaderIfNotRunning() {
        if (playersFadingOut.isEmpty()) {
            // Currently, there is no Fader running - start one!
            uiThreadHandler.postDelayed(fader, 50);
        }
    }

    @NonNull
    @UiThread
    Iterator<SoundboardMediaPlayer> activePlayersIterator() {
        return activePlayers.values().iterator();
    }

    @NonNull
    @UiThread
    private Iterator<SoundboardMediaPlayer> playersFadingOutIterator() {
        return playersFadingOut.values().iterator();
    }

    /**
     * Puts this player into the active players map - also removing it from the
     * players currently fading out (if contained).
     */
    @UiThread
    void putActive(@Nullable Soundboard soundboard, Sound sound,
                   SoundboardMediaPlayer mediaPlayer) {
        MediaPlayerSearchId searchId = new MediaPlayerSearchId(soundboard, sound);

        putActive(searchId, mediaPlayer);
    }

    /**
     * Puts this player into the active players map - also removing it from the
     * players currently fading out (if contained).
     */
    @UiThread
    private void putActive(MediaPlayerSearchId searchId, SoundboardMediaPlayer mediaPlayer) {
        playersFadingOut.remove(searchId);
        activePlayers.put(searchId, mediaPlayer);
    }

    /**
     * Puts this player into the map of players fading out - also removing it from the
     * active players (if contained).
     */
    @UiThread
    private void putFadingOut(MediaPlayerSearchId searchId, SoundboardMediaPlayer mediaPlayer) {
        activePlayers.remove(searchId);
        playersFadingOut.put(searchId, mediaPlayer);
    }

    /**
     * {@link java.lang.Runnable} that does a single fade-out step for all players that
     * shall be faded out - and schedules itself for the next step, if necessary.
     */
    private class Fader implements Runnable {
        @Override
        @UiThread
        public void run() {
            fadeOut();

            if (!playersFadingOut.isEmpty()) {
                uiThreadHandler.postDelayed(fader, 40);
            }
        }

        @UiThread
        private void fadeOut() {
            for (Iterator<SoundboardMediaPlayer> playerIt = playersFadingOut.values().iterator();
                 playerIt.hasNext(); ) {
                SoundboardMediaPlayer player = playerIt.next();
                float oldVolume = player.getVolume();

                float newVolume = oldVolume / 1.116f;

                if (newVolume < 0.001) {
                    player.stop();
                    player.release();
                    playerIt.remove();
                } else {
                    setVolume(player, newVolume);
                }
            }
        }
    }
}
