package view.messages;

public enum Error {
    ENTER_MENU_BEFORE_LOGIN ("please login first"),
    BEING_ON_A_MENU ("menu navigation is not possible"),
    TAKEN_USERNAME ("user with username %s already exists\n"),
    TAKEN_NICKNAME ("user with nickname %s already exists\n"),
    INCORRECT_USERNAME ("Username and password didn't match!"),
    INCORRECT_PASSWORD ("Username and password didn't match!"),
    INVALID_CURRENT_PASSWORD ("current password is invalid"),
    SAME_PASSWORD ("please enter a new password"),
    DECK_EXIST("deck with name %s already exists"),
    DECK_NOT_EXIST("deck with name %s does not exist"),
    //INCORRECT_CARD_NAME(""),
    //DECK_IS_FULL(""),
    //EXCESSIVE_NUMBER_ALLOWED (MORE THAN 3 OF ONE CARD),
    //CARD_DOES_NOT_EXIST_IN_DECK,
    CARD_DOES_NOT_EXIST ("there is no card with this name"),
    NOT_ENOUGH_MONEY ("not enough money"),
    PLAYER_DOES_NOT_EXIST ("there is no player with this username"),
    //INACTIVATED_DECK,
    //FORBIDDEN_DECK,
    WRONG_ROUNDS_NUMBER ("number of rounds is not supported"),
    INVALID_SELECTION ("invalid selection"),
    CARD_NOT_FOUND ("no card found in the given position"),
    NO_CARD_SELECTED_YET ("no card is selected yet"),
    CAN_NOT_SUMMON ("you can’t summon this card"),
    ACTION_NOT_ALLOWED ("action not allowed in this phase"),
    MONSTER_ZONE_IS_FULL ("monster card zone is full"),
    ALREADY_SUMMONED_OR_SET ("you already summoned/set on this turn"),
    NOT_ENOUGH_CARDS_TO_TRIBUTE ("there are not enough cards for tribute"),
    WRONG_MONSTER_ADDRESS ("there no monsters one this address"),
    WRONG_MONSTERS_ADDRESSES ("there is no monster on one of these addresses"),
    CAN_NOT_SET ("you can’t set this card"),
    ACTION_CAN_NOT_WORK ("you can’t do this action in this phase"),
    CAN_NOT_CHANGE_POSITION ("you can’t change this card position"),
    CURRENTLY_IN_POSITION ("this card is already in the wanted position"),
    ALREADY_CHANGED_POSITION ("you already changed this card position in this turn"),
    FLIP_SUMMON_NOT_ALLOWED ("you can’t flip summon this card"),
    CAN_NOT_ATTACK ("you can’t attack with this card"),
    ALREADY_ATTACKED ("this card already attacked"),
    NO_CARD_TO_BE_ATTACKED ("there is no card to attack here"),
    ONLY_SPELL_CAN_ACTIVE ("activate effect is only for spell cards."),
    CAN_NOT_ACTIVE_EFFECT ("you can’t activate an effect on this turn"),
    CARD_ALREADY_ACTIVATED ("you have already activated this card"),
    SPELL_ZONE_IS_FULL ("spell card zone is full"),
    PREPARATIONS_IS_NOT_DONE ("preparations of this spell are not done yet"),
    NOT_YOUR_TURN ("it’s not your turn to play this kind of moves"),
    CAN_NOT_RITUAL_SUMMON ("there is no way you could ritual summon a monster"),
    LEVEL_DO_NOT_MATCH ("selected monsters levels don’t match with ritual monster"),
    CAN_NOT_SPECIAL_SUMMON ("there is no way you could special summon a monster"),
    INVISIBLE_CARD ("card is not visible"),
    INVALID_COMMAND ("invalid command");
    private String value;

    Error(String value) {
        setValue (value);
    }

    private void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
