package org.example.spotify;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.media.MediaPlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

public class PlayerController {

    @FXML
    private Button playButton;

    @FXML
    private Button pauseButton;

    @FXML
    private Button likeButton;

    @FXML
    private Button skipForwardButton;

    @FXML
    private Button skipBackwardButton;

    @FXML
    private Button premiumButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Button logoutButton;

    @FXML
    private Button cancelPremiumButton;

    private User user;
    private MusicService musicService = new MusicService();
    private UserService userService = new UserService();

    private List<String> playlist = new ArrayList<>();
    private int currentSongIndex = 0;
    private int skipsUsed = 0;
    private Timer skipResetTimer;

    private static final String JAMENDO_API_KEY = "756af173";
    private static final String JAMENDO_API_URL = "https://api.jamendo.com/v3.0/tracks/?client_id=" + JAMENDO_API_KEY + "&format=jsonpretty&limit=10";

    public void setUser(User user) {
        this.user = user;
        initialize();
    }
    private Map<String, Boolean> likedSongs = new HashMap<>();

    @FXML
    private void initialize() {
        if (user == null) {
            statusLabel.setText("Error: User not set. Please log in again.");
            disableControls();
            return;
        }

        fetchSongsFromAPI();

        // Load liked songs from the database
        List<String> likedSongsFromDB = userService.getLikedSongs(user.getUsername());
        for (String songUrl : likedSongsFromDB) {
            likedSongs.put(songUrl, true);
        }

        // Set up button actions
        premiumButton.setOnAction(event -> handlePremium());
        cancelPremiumButton.setOnAction(event -> handleCancelPremium());

        // Enable all buttons when the player starts
        enableControls();

        // Set premium button style and disable it if the user is premium
        if (user.isPremium()) {
            premiumButton.setStyle("-fx-font-weight: bold; -fx-text-fill: gold;");
            premiumButton.setDisable(true); // Disable "Get Premium" button
            cancelPremiumButton.setDisable(false); // Enable "Cancel Premium" button
        } else {
            premiumButton.setStyle("-fx-font-weight: normal; -fx-text-fill: black;");
            premiumButton.setDisable(false); // Enable "Get Premium" button
            cancelPremiumButton.setDisable(true); // Disable "Cancel Premium" button
        }

        if (!playlist.isEmpty()) {
            loadSong(playlist.get(currentSongIndex), false); // Load but don't play automatically
        } else {
            statusLabel.setText("Failed to load songs.");
        }
    }



    private void enableControls() {
        playButton.setDisable(false);
        pauseButton.setDisable(false);
        likeButton.setDisable(false);
        skipForwardButton.setDisable(false);
        skipBackwardButton.setDisable(false);
        premiumButton.setDisable(false);
        logoutButton.setDisable(false);

    }

    private void disableControls() {
        playButton.setDisable(true);
        pauseButton.setDisable(true);
        likeButton.setDisable(true);
        skipForwardButton.setDisable(true);
        skipBackwardButton.setDisable(true);
        premiumButton.setDisable(true);
        logoutButton.setDisable(true);

    }

    private void fetchSongsFromAPI() {
        try {
            URL url = new URL(JAMENDO_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray tracks = jsonResponse.getJSONArray("results");

            for (int i = 0; i < tracks.length(); i++) {
                JSONObject track = tracks.getJSONObject(i);
                String audioUrl = track.getString("audio");
                playlist.add(audioUrl);
            }

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error fetching songs from API.");
        }
    }

    private void loadSong(String url, boolean autoPlay) {
        if (user == null) {
            statusLabel.setText("Error: User not set. Please log in again.");
            return;
        }

        if (user.isPremium() || musicService.canPlaySong()) {
            musicService.playMusic(url);
            if (autoPlay) {
                musicService.getMediaPlayer().play();
                statusLabel.setText("Playing: Song " + (currentSongIndex + 1));
            } else {
                statusLabel.setText("Loaded: Song " + (currentSongIndex + 1));
            }

            // Update like button style based on the current song's liked status
            if (likedSongs.getOrDefault(url, false)) {
                likeButton.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
            } else {
                likeButton.setStyle("-fx-font-weight: normal; -fx-text-fill: black;");
            }
        } else {
            statusLabel.setText("You have reached the limit of free songs.");
        }
    }

    @FXML
    private void handlePlay() {
        if (user == null) {
            statusLabel.setText("Error: User not set. Please log in again.");
            return;
        }

        if (musicService.getMediaPlayer() != null) {
            musicService.getMediaPlayer().play();
            statusLabel.setText("Playing: Song " + (currentSongIndex + 1));
        }
    }

    @FXML
    private void handlePause() {
        if (user == null) {
            statusLabel.setText("Error: User not set. Please log in again.");
            return;
        }

        musicService.pauseMusic();
        statusLabel.setText("Paused");
    }


    @FXML
    private void handleLike() {
        if (user == null) {
            statusLabel.setText("Error: User not set. Please log in again.");
            return;
        }

        String currentSong = playlist.get(currentSongIndex);
        boolean isLiked = likedSongs.getOrDefault(currentSong, false);

        // Toggle the like status
        if (isLiked) {
            likedSongs.put(currentSong, false);
            userService.removeLikedSong(user.getUsername(), currentSong); // Remove from DB
            likeButton.setStyle("-fx-font-weight: normal; -fx-text-fill: black;");
            statusLabel.setText("Song " + (currentSongIndex + 1) + " unliked.");
        } else {
            likedSongs.put(currentSong, true);
            userService.saveLikedSong(user.getUsername(), currentSong); // Save to DB
            likeButton.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
            statusLabel.setText("Song " + (currentSongIndex + 1) + " liked!");
        }
    }
    @FXML
    private void handleSkipForward() {
        if (user == null) {
            statusLabel.setText("Error: User not set. Please log in again.");
            return;
        }

        if (user.isPremium() || skipsUsed < 4) {
            skipsUsed++;
            skipForward();
            if (!user.isPremium() && skipsUsed >= 4) {
                disableSkipButtons();
                startSkipResetTimer();
            }
        } else {
            statusLabel.setText("You have reached the skip limit for free users.");
        }
    }

    @FXML
    private void handleSkipBackward() {
        if (user == null) {
            statusLabel.setText("Error: User not set. Please log in again.");
            return;
        }

        if (user.isPremium() || skipsUsed < 4) {
            skipsUsed++;
            skipBackward();
            if (!user.isPremium() && skipsUsed >= 4) {
                disableSkipButtons();
                startSkipResetTimer();
            }
        } else {
            statusLabel.setText("You have reached the skip limit for free users.");
        }
    }
    @FXML
    private void handlePremium() {
        if (user == null) {
            statusLabel.setText("Error: User not set. Please log in again.");
            return;
        }

        userService.upgradeToPremium(user.getUsername());
        user.setPremium(true);
        statusLabel.setText("You are now a premium user!");

        // Reset skipsUsed and enable skip buttons
        skipsUsed = 0;
        skipForwardButton.setDisable(false);
        skipBackwardButton.setDisable(false);

        // Update button states
        premiumButton.setStyle("-fx-font-weight: bold; -fx-text-fill: gold;");
        premiumButton.setDisable(true);
        cancelPremiumButton.setDisable(false);
    }


    private void skipForward() {
        if (currentSongIndex < playlist.size() - 1) {
            currentSongIndex++;
        } else {
            currentSongIndex = 0;
        }
        loadSong(playlist.get(currentSongIndex), true);
    }

    private void skipBackward() {
        if (currentSongIndex > 0) {
            currentSongIndex--;
        } else {
            currentSongIndex = playlist.size() - 1;
        }
        loadSong(playlist.get(currentSongIndex), true);
    }

    private void disableSkipButtons() {
        skipForwardButton.setDisable(true);
        skipBackwardButton.setDisable(true);
    }

    private void startSkipResetTimer() {
        if (skipResetTimer != null) {
            skipResetTimer.cancel();
        }

        skipResetTimer = new Timer();
        skipResetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                skipsUsed = 0; // Reset skip counter
                skipForwardButton.setDisable(false); // Re-enable skip buttons
                skipBackwardButton.setDisable(false);
                statusLabel.setText("Skip limit reset. You can now skip songs again.");
            }
        }, 60 * 60 * 1000);
    }

    @FXML
    private void handleLogout() {
        if (user != null) {
            user = null;
            musicService.stopMusic();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error: Unable to load login page.");
        }
    }


    @FXML
    private void handleCancelPremium() {
        if (user == null) {
            statusLabel.setText("Error: User not set. Please log in again.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Premium");
        alert.setHeaderText("Are you sure you want to cancel your premium subscription?");
        alert.setContentText("You will lose access to premium features.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userService.cancelPremium(user.getUsername());
                user.setPremium(false);
                statusLabel.setText("Premium subscription canceled.");

                skipsUsed = 0;
                if (!user.isPremium()) {
                    skipForwardButton.setDisable(true); // Disable skip buttons
                    skipBackwardButton.setDisable(true);
                }

                premiumButton.setStyle("-fx-font-weight: normal; -fx-text-fill: black;");
                premiumButton.setDisable(false); // Enable "Get Premium" button
                cancelPremiumButton.setDisable(true); // Disable "Cancel Premium" button
            }
        });
    }

}