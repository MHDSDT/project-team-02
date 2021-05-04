package view.gameview;

import view.messages.Error;
import view.messages.SuccessMessage;

import java.util.regex.Matcher;

public class GameView {
    private static final GameView instance;

    static {
        instance = new GameView();
    }

    public static GameView getInstance() {
        return instance;
    }

    public void run() {
    }

    public void commandRecognition(String command) {

    }

    public void showError(Error error) {

    }

    public void showDynamicError(Error error, Matcher matcher) {

    }

    public void showDynamicErrorWithAString(Error error, String string) {

    }

    public void showSuccessMessage(SuccessMessage message) {

    }

    public void showSuccessMessageWithAString(SuccessMessage message, String string) {

    }

    public void showSuccessMessageWithAnInteger(SuccessMessage message, int number) {

    }

    public void showBoard() {

    }

    public void showPhase() {

    }

    public void showGraveYard() {

    }

    public void showCard() {

    }
    public Matcher getTributeAddress(){
        //here you should get an address and check it if it is for monster address, else you show invalid command and take input again
        Matcher matcher;
        return null;
    }
}
