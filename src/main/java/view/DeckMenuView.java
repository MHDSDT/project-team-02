package view;

import controller.DeckMenuController;
import model.card.Card;
import model.card.Monster;
import model.card.Spell;
import model.card.Trap;
import model.card.informationofcards.CardType;
import view.input.Regex;
import view.messages.Error;
import view.messages.SuccessMessage;

import java.util.Locale;
import java.util.regex.Matcher;

public class DeckMenuView {
    private static DeckMenuView instance = null;
    private static final DeckMenuController controller = DeckMenuController.getInstance();

    private DeckMenuView() {
    }

    public static DeckMenuView getInstance() {
        if (instance == null)
            instance = new DeckMenuView();
        return instance;
    }

    public void run(String command) {
        commandRecognition(command);
    }

    public void commandRecognition(String command) {
        Matcher matcher;
        if ((matcher = Regex.getMatcher(Regex.MENU_ENTER, command)).matches()) {
            if (matcher.group("menuName").toLowerCase(Locale.ROOT).equals("deck"))
                showDynamicError(Error.BEING_ON_CURRENT_MENU, matcher);
            else Error.showError(Error.BEING_ON_A_MENU);
        } else if (Regex.getMatcher(Regex.MENU_EXIT, command).matches()) {
            MenusManager.getInstance().changeMenu(Menu.MAIN_MENU);
            return;
        }
        if ((matcher = Regex.getMatcher(Regex.DECK_CREATE, command)).matches()) {
            controller.createDeck(matcher);
        } else if ((matcher = Regex.getMatcher(Regex.DECK_DELETE, command)).matches()) {
            controller.deleteDeck(matcher);
        } else if ((matcher = Regex.getMatcher(Regex.DECK_SET_ACTIVATE, command)).matches()) {
            controller.activateDeck(matcher);
        } else if ((matcher = Regex.getMatcherFromAllPermutations(Regex.DECK_ADD_CARD_TO_SIDE_DECK, command)) != null) {
            controller.addCardToSideDeck(matcher);
        } else if ((matcher = Regex.getMatcherFromAllPermutations(Regex.DECK_ADD_CARD_TO_MAIN_DECK, command)) != null) {
            controller.addCardToMainDeck(matcher);
        } else if ((matcher = Regex.getMatcherFromAllPermutations(Regex.DECK_REMOVE_CARD_SIDE_DECK, command)) != null) {
            controller.removeCardFromSideDeck(matcher);
        } else if ((matcher = Regex.getMatcherFromAllPermutations(Regex.DECK_REMOVE_CARD_MAIN_DECK, command)) != null) {
            controller.removeCardFromMainDeck(matcher);
        } else if (Regex.getMatcher(Regex.DECK_SHOW_ALL_DECKS, command).matches()) {
            controller.showAllDecks();
        } else if ((matcher = Regex.getMatcherFromAllPermutations(Regex.DECK_SHOW_SIDE_DECK, command)) != null) {
            controller.showDeck(matcher, "Side");
        } else if ((matcher = Regex.getMatcher(Regex.DECK_SHOW_MAIN_DECK, command)).matches()) {
            controller.showDeck(matcher, "Main");
        } else if (Regex.getMatcher(Regex.DECK_SHOW_ALL_CARDS, command).matches()) {
            controller.showAllCards();
        } else if ((matcher = Regex.getMatcher(Regex.CARD_SHOW, command)).matches()) {
            controller.showCard(matcher);
        } else if (Regex.getMatcher(Regex.MENU_SHOW_CURRENT, command).matches()) {
            showCurrentMenu();
        } else if (Regex.getMatcher(Regex.COMMAND_HELP, command).matches()) {
            help();
        } else Error.showError(Error.INVALID_COMMAND);
    }

    public void checkTypeOfCardAndPrintIt(Card card) {
        if (card.getCardType().equals(CardType.MONSTER)) {
            Monster monster = (Monster) card;
            System.out.println(monster);
        } else if (card.getCardType().equals(CardType.SPELL)) {
            Spell spell = (Spell) card;
            System.out.println(spell);
        } else {
            Trap trap = (Trap) card;
            System.out.println(trap);
        }
    }

    public void showDynamicError(Error error, Matcher matcher) {
        if (error.equals(Error.DECK_EXIST)) {
            System.err.printf(Error.DECK_EXIST.getValue(), matcher.group("deckName"));
        } else if (error.equals(Error.DECK_NOT_EXIST)) {
            System.err.printf(Error.DECK_NOT_EXIST.getValue(), matcher.group("deckName"));
        } else if (error.equals(Error.INCORRECT_CARD_NAME)) {
            System.err.printf(Error.INCORRECT_CARD_NAME.getValue(), matcher.group("cardName"));
        } else if (error.equals(Error.EXCESSIVE_NUMBER_IN_DECK)) {
            System.err.printf(Error.EXCESSIVE_NUMBER_IN_DECK.getValue(), matcher.group("cardName"), matcher.group("deckName"));
        } else if (error.equals(Error.CARD_LIMITED_IN_DECK)) {
            System.err.printf(Error.CARD_LIMITED_IN_DECK.getValue(), matcher.group("cardName"));
        } else if (error.equals(Error.CARD_DOES_NOT_EXIST_IN_SIDE_DECK)) {
            System.err.printf(Error.CARD_DOES_NOT_EXIST_IN_SIDE_DECK.getValue(), matcher.group("cardName"));
        } else if (error.equals(Error.CARD_DOES_NOT_EXIST_IN_MAIN_DECK)) {
            System.err.printf(Error.CARD_DOES_NOT_EXIST_IN_MAIN_DECK.getValue(), matcher.group("cardName"));
        } else if (error.equals(Error.BEING_ON_CURRENT_MENU))
            System.err.printf(error.getValue(), Menu.PROFILE_MENU.getValue());
    }

    public void showCurrentMenu() {
        System.out.println("Deck Menu");
    }

    private void help() {
        System.out.println("deck create <deck name>\ndeck create <deck name>\ndeck set-activate <deck name>\n" +
                "deck add-card --card <card name> --deck <deck name>\ndeck add-card --card <card name> --deck <deck name> --" +
                "side\ndeck rm-card --card <card name> --deck <deck name>\ndeck rm-card --card <card name> --deck <deck name> --side\ndeck show --all\ndeck show --deck-name <deck name>\n" +
                "deck show --deck-name <deck name> --side\ndeck show --cards\ncard show <card name>");
    }
}