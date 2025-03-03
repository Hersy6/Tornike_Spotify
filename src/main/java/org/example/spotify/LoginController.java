package org.example.spotify;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        User user = userService.authenticate(username, password);
        if (user != null) {
            openPlayerWindow(user);
        } else {
            errorLabel.setText("❌ Invalid username or password");
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (userService.register(username, password)) {
            errorLabel.setText("✅ Registration successful! Please log in.");
        } else {
            errorLabel.setText("❌ Username already exists.");
        }
    }

    private void openPlayerWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/player.fxml"));
            Parent root = loader.load();
            PlayerController playerController = loader.getController();
            playerController.setUser(user);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 400, 300));
            stage.show();

            // Close login window
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
