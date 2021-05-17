package controller;

import model.User;
import view.ProfileMenuView;
import view.messages.Error;
import view.messages.SuccessMessage;

import java.util.regex.Matcher;

public class ProfileMenuController {
    private User loggedInUser;
    private static ProfileMenuController instance = null;
    private final ProfileMenuView view = ProfileMenuView.getInstance ();

    private ProfileMenuController() {}

    public static ProfileMenuController getInstance() {
        if (instance == null) instance = new ProfileMenuController ();
        return instance;
    }

    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    public boolean isNicknameUsed(String nickname) {
        for (User user : User.getAllUsers ())
            if (user.getNickname ().equals (nickname)) return true;
        return false;
    }

    public boolean isPasswordCorrect(String password) {
        return ProfileMenuController.getInstance ().getLoggedInUser ().getPassword ().equals (password);
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void changeNickname(Matcher matcher) {
        String newNickname = matcher.group ("nickname");
        if (isNicknameUsed (newNickname)) {
            view.showDynamicError (Error.TAKEN_NICKNAME, matcher);
            return;
        }
        ProfileMenuController.getInstance ().getLoggedInUser ().changeNickname (newNickname);
        SuccessMessage.showSuccessMessage (SuccessMessage.NICKNAME_CHANGED);
    }

    public void changePassword(Matcher matcher) {
        String currentPassword = matcher.group ("currentPassword");
        String newPassword = matcher.group ("newPassword");
        if (!isPasswordCorrect (currentPassword)) {
            Error.showError (Error.INVALID_CURRENT_PASSWORD);
            return;
        }
        if (currentPassword.equals (newPassword)) {
            Error.showError (Error.SAME_PASSWORD);
            return;
        }
        ProfileMenuController.getInstance ().getLoggedInUser ().changePassword (newPassword);
        SuccessMessage.showSuccessMessage (SuccessMessage.PASSWORD_CHANGED);
    }
}