package controller;

import controller.playgame.DuelGameController;
import model.User;
import model.game.Duel;
import view.DuelMenuView;
import view.MenusManager;
import view.messages.Error;
import java.util.Objects;
import java.util.regex.Matcher;

public class DuelMenuController {
    private static final DuelMenuController instance;
    private final DuelMenuView view = DuelMenuView.getInstance ();
    private User loggedInUser;
    private Duel duel;

    static {
        instance = new DuelMenuController ();
    }

    public static DuelMenuController getInstance() {
        return instance;
    }


    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    public void startDuelWithOtherPlayer(Matcher matcher) throws CloneNotSupportedException {
        if (!isPlayerValidToStartDuel(matcher.group("secondPlayerNickName"))) {
            view.showError(Error.PLAYER_DOES_NOT_EXIST);
        } else if (!areRoundsNumberValid(Integer.parseInt(matcher.group("roundNumber")))) {
            view.showError(Error.WRONG_ROUNDS_NUMBER);
        } else if (arePlayersDecksActive(matcher.group("secondPlayerNickName"))) {
            if (arePlayersDecksValid(matcher.group("secondPlayerNickName"))) {
                duel = new Duel(loggedInUser.getUsername(),matcher.group("" +
                        "secondPlayerNickName"),Integer.parseInt(matcher.group("roundNumber")));
                DuelGameController.getInstance().startDuel(duel);
            }
        }
    }

    public void startDuelWithAI(Matcher matcher) {
        if (!areRoundsNumberValid(Integer.parseInt(matcher.group("roundNumber"))))
            view.showError(Error.WRONG_ROUNDS_NUMBER);
    }

    private boolean isPlayerValidToStartDuel(String username) {
        User user = User.getUserByUsername(username);
        return user != null;
    }

    public boolean areRoundsNumberValid(int roundsNumber) {
        return roundsNumber != 3 && roundsNumber != 1;
    }

    public boolean arePlayersDecksActive(String secondPlayerUserName) {
        if (!loggedInUser.getHasActiveDeck()) {
            view.showDynamicError(Error.INACTIVATED_DECK, loggedInUser.getUsername());
            return false;
        }
        User user = Objects.requireNonNull(User.getUserByUsername(secondPlayerUserName));
        if (!user.getHasActiveDeck()) {
            view.showDynamicError(Error.INACTIVATED_DECK, secondPlayerUserName);
            return false;
        }
        return true;
    }

    public boolean arePlayersDecksValid(String secondPlayerUsername) {
        if (!Objects.requireNonNull(User.getActiveDeck(loggedInUser.getUsername())).isValidDeck()) {
            view.showDynamicError(Error.FORBIDDEN_DECK, loggedInUser.getUsername());
            return false;
        } else if (!Objects.requireNonNull(User.getActiveDeck(secondPlayerUsername)).isValidDeck()) {
            view.showDynamicError(Error.FORBIDDEN_DECK, secondPlayerUsername);
            return false;
        }
        return true;
    }

}