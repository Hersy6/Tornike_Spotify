package org.example.spotify;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private Connection connection;

    public UserService() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:spotify.db");
            createTable();
            createLikedSongsTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the users table if it doesn't exist.
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "username TEXT PRIMARY KEY," +
                "password TEXT NOT NULL," +
                "isPremium INTEGER NOT NULL)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the liked_songs table if it doesn't exist.
     */
    private void createLikedSongsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS liked_songs (" +
                "username TEXT NOT NULL," +
                "song_url TEXT NOT NULL," +
                "PRIMARY KEY (username, song_url)," +
                "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Authenticate a user.
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getString("username"), rs.getString("password"), rs.getInt("isPremium") == 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Register a new user.
     */
    public boolean register(String username, String password) {
        String sql = "INSERT INTO users(username, password, isPremium) VALUES(?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setInt(3, 0);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Upgrade a user to premium.
     */
    public void upgradeToPremium(String username) {
        String sql = "UPDATE users SET isPremium = 1 WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cancel a user's premium subscription.
     */
    public void cancelPremium(String username) {
        String sql = "UPDATE users SET isPremium = 0 WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save a liked song for a user.
     */
    public void saveLikedSong(String username, String songUrl) {
        String sql = "INSERT INTO liked_songs(username, song_url) VALUES(?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, songUrl);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove a liked song for a user.
     */
    public void removeLikedSong(String username, String songUrl) {
        String sql = "DELETE FROM liked_songs WHERE username = ? AND song_url = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, songUrl);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve all liked songs for a user.
     */
    public List<String> getLikedSongs(String username) {
        List<String> likedSongs = new ArrayList<>();
        String sql = "SELECT song_url FROM liked_songs WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                likedSongs.add(rs.getString("song_url"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return likedSongs;
    }
}