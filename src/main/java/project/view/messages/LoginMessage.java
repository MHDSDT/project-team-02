package project.view.messages;

import javafx.scene.control.Alert;

public enum LoginMessage {
    SHORT_USERNAME("Username must be at least 6 characters!", Alert.AlertType.ERROR),
    SHORT_PASSWORD("Password must be at least 8 characters!", Alert.AlertType.ERROR),
    TAKEN_USERNAME("User with this username already exists!", Alert.AlertType.ERROR),
    TAKEN_NICKNAME("User with this nickname already exists!", Alert.AlertType.ERROR),
    NONIDENTICAL_PASSWORDS("Passwords are not the same!", Alert.AlertType.ERROR),
    EMPTY_FIELD ("Fill the fields!", Alert.AlertType.ERROR),
    INCORRECT_USERNAME_PASSWORD("Username and password didn't match!", Alert.AlertType.ERROR),
    SUCCESSFUL_SIGN_UP("Sign-up was successful", Alert.AlertType.INFORMATION),
    SUCCESSFUL_LOGIN("Login was successful", Alert.AlertType.INFORMATION),
    LOGOUT_CONFIRMATION("Are you sure you want to logout?", Alert.AlertType.CONFIRMATION),
    EXIT_CONFIRMATION("Are you sure you want to exit?", Alert.AlertType.CONFIRMATION);
    private final String label;
    private final Alert.AlertType alertType;

    LoginMessage(String label, Alert.AlertType alertType) {
        this.label = label;
        this.alertType = alertType;
    }

    public Alert.AlertType getAlertType() {
        return alertType;
    }

    public String getLabel() {
        return label;
    }
}
