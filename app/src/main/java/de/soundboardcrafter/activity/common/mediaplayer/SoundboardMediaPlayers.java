package de.soundboardcrafter.activity.common.mediaplayer;

import android.media.AudioAttributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Several media players
 */
class SoundboardMediaPlayers implements Iterable<SoundboardMediaPlayer> {
    private final HashMap<MediaPlayerSearchId, SoundboardMediaPlayer> mediaPlayers = new HashMap<>();

    boolean isEmpty() {
        return mediaPlayers.isEmpty();
    }

    int size() {
        return mediaPlayers.size();
    }

    /**
     * Initializes this mediaPlayer. Does not start playing yet.
     */
    static void initMediaPlayer(SoundboardMediaPlayer mediaPlayer, String soundName,
                                String soundPath, int volumePercentage, boolean loop) {
        mediaPlayer.setSoundName(soundName);
        try {
            mediaPlayer.setDataSource(soundPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
        SoundboardMediaPlayers.setVolume(mediaPlayer, SoundboardMediaPlayers.percentageToVolume(volumePercentage));
        mediaPlayer.setLooping(loop);
    }

    void setOnPlayingStopped(@Nullable Soundboard soundboard, Sound sound,
                             @Nullable SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
        SoundboardMediaPlayer player = mediaPlayers.get(new MediaPlayerSearchId(soundboard, sound));
        if (player != null) {
            player.setOnPlayingStopped(onPlayingStopped);
        }
    }

    boolean isPlaying(@NonNull Soundboard soundboard, @NonNull UUID soundId) {
        SoundboardMediaPlayer mediaPlayer = mediaPlayers.get(
                new MediaPlayerSearchId(soundboard.getId(), soundId));
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    SoundboardMediaPlayer get(@Nullable Soundboard soundboard, Sound sound) {
        MediaPlayerSearchId searchId = new MediaPlayerSearchId(soundboard, sound);
        return mediaPlayers.get(searchId);
    }

    void put(@Nullable Soundboard soundboard, Sound sound, SoundboardMediaPlayer mediaPlayer) {
        MediaPlayerSearchId searchId = new MediaPlayerSearchId(soundboard, sound);
        mediaPlayers.put(searchId, mediaPlayer);
    }

    /**
     * Stops this sound when it's played in this soundboard
     */
    void stopPlaying(@Nullable Soundboard soundboard, @NonNull Sound sound) {
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
        remove(player);
    }

    void remove(SoundboardMediaPlayer mediaPlayer) {
        mediaPlayer.release();
        mediaPlayers.values().remove(mediaPlayer);
    }

    boolean isPlaying(@NonNull Sound sound) {
        checkNotNull(sound, "sound is null");

        return mediaPlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(sound.getId()))
                .map(Map.Entry::getValue)
                .anyMatch(SoundboardMediaPlayer::isPlaying);
    }

    /**
     * Sets the volume for this sound.
     */
    void setVolumePercentage(UUID soundId, int volumePercentage) {
        checkNotNull(soundId, "soundId is null");

        setVolume(soundId, SoundboardMediaPlayers.percentageToVolume(volumePercentage));
    }

    /**
     * Sets the volume for this sound.
     */
    void setVolume(@NonNull UUID soundId, float volume) {
        checkNotNull(soundId, "soundId is null");

        mediaPlayers.entrySet().stream()
                .filter(e -> e.getKey().getSoundId().equals(soundId))
                .map(Map.Entry::getValue)
                .forEach(m -> setVolume(m, volume));
    }

    /**
     * Sets the volume for this <code>mediaPlayer</code>.
     */
    private static void setVolume(@NonNull SoundboardMediaPlayer mediaPlayer, float volume) {
        checkNotNull(mediaPlayer, "mediaPlayer is null");
        mediaPlayer.setVolume(volume, volume);
    }

    private static float percentageToVolume(int volumePercentage) {
        return (float) volumePercentage / 100f;
    }

    /**
     * Stops all playing sounds in these soundboards
     */
    void stopPlaying(Iterable<Soundboard> soundboards) {
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
    }

    /**
     * Stops all playing sounds in all soundboards
     */
    void stopPlaying() {
        for (Iterator<SoundboardMediaPlayer> playerIt = iterator(); playerIt.hasNext(); ) {
            SoundboardMediaPlayer player = playerIt.next();
            player.stop();
            player.release();
            playerIt.remove();
        }

        mediaPlayers.clear();
    }

    @NonNull
    @Override
    public Iterator<SoundboardMediaPlayer> iterator() {
        return mediaPlayers.values().iterator();
    }
}
