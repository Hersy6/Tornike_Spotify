package org.example.spotify;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MusicService {
    private MediaPlayer mediaPlayer;
    private int songsPlayed = 0;

    public void playMusic(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.stop(); // Stop the current song before playing a new one
        }
        Media media = new Media(url);
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.play();
        songsPlayed++;
    }

    public void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public boolean canPlaySong() {
        return songsPlayed < 4; // Free users can only play 4 songs per hour
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void resetSongsPlayed() {
        songsPlayed = 0; // Reset the song counter (e.g., after an hour)
    }
}