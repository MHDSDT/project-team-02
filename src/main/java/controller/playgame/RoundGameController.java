package controller.playgame;

import model.card.Card;
import model.card.Monster;
import model.card.Spell;
import model.card.Trap;
import model.card.informationofcards.*;
import model.card.informationofcards.CardType;
import model.card.informationofcards.MonsterActionType;
import model.card.informationofcards.TrapEffect;
import model.game.DuelPlayer;
import model.game.board.*;
import view.gameview.GameView;
import view.messages.Error;
import view.messages.SuccessMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

public class RoundGameController {
    private int swordsOfRevealingLightRounds = 3;
    private static RoundGameController instance;
    private final GameView view = GameView.getInstance();
    private DuelPlayer firstPlayer;
    private DuelPlayer secondPlayer;
    private Cell selectedCell = null;
    private Zone selectedCellZone = Zone.NONE;
    private int selectedCellAddress;
    private boolean isSummonOrSetOfMonsterUsed = false;
    private Phase currentPhase;
    private List<Card> firstPlayerHand = new ArrayList<>();
    private List<Card> secondPlayerHand = new ArrayList<>();
    private int turn = 1; // 1 : firstPlayer, 2 : secondPlayer
    private DuelGameController duelGameController = DuelGameController.getInstance();
    private List<Integer> usedCellsToAttackNumbers = new ArrayList<>();
    private List<Integer> changedPositionCards = new ArrayList<>();

    static {
        instance = new RoundGameController();
    }

    public static RoundGameController getInstance() {
        if (instance == null) instance = new RoundGameController();
        return instance;
    }

    public void run() {

    }

    public int getTurn() {
        return turn;
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public void setRoundInfo(DuelPlayer firstPlayer, DuelPlayer secondPlayer) {
        this.firstPlayer = firstPlayer;
        this.secondPlayer = secondPlayer;
        firstPlayer.setLifePoint(8000);
        secondPlayer.setLifePoint(8000);
    }

    private void changeTurn() {
        selectedCell = null;
        turn = (turn == 1) ? 2 : 1;
    }

    public void selectCard(Matcher matcher) {

    }

    private void selectCardInHand(Matcher matcher) {
        int address = Integer.parseInt(matcher.group("address")); //name of group?
        //TODO errors to check
        ArrayList<Card> hand = (ArrayList<Card>) (getTurn() == 1 ? firstPlayerHand : secondPlayerHand);
        selectedCellZone = Zone.HAND;
        selectedCell.setCardInCell(hand.get(address));
        selectedCell.setCellStatus(CellStatus.IN_HAND);
        view.showSuccessMessage(SuccessMessage.CARD_SELECTED);
        //???????????????????????//
    }

    private void selectCardInMonsterZone(Matcher matcher) {
        int address = Integer.parseInt(matcher.group("address")); //name of group?
        //TODO errors to check
        selectedCellZone = Zone.MONSTER_ZONE;
        selectedCell = getCurrentPlayer().getPlayerBoard().getACellOfBoard(selectedCellZone, address);
        selectedCellAddress = address;
        view.showSuccessMessage(SuccessMessage.CARD_SELECTED);
    }

    private void selectCardInSpellZone(Matcher matcher) {
        int address = Integer.parseInt(matcher.group("cardNumber")); //name of group?
        //TODO errors to check
        selectedCellZone = Zone.SPELL_ZONE;
        selectedCell = getCurrentPlayer().getPlayerBoard().getACellOfBoard(selectedCellZone, address);
        view.showSuccessMessage(SuccessMessage.CARD_SELECTED);
    }

    public void deselectCard(int code) {
        if (code == 1) {
            if (selectedCell == null) {
                view.showError(Error.NO_CARD_SELECTED_YET);
                return;
            }
        }
        selectedCell = null;
        selectedCellZone = Zone.NONE;
        view.showSuccessMessage(SuccessMessage.CARD_DESELECTED);
    }

    public void summonMonster() { //TODO might have effect
        if (!isValidSelectionForSummonOrSet()) {
            return;
        }
        if ((!selectedCellZone.equals(Zone.HAND)) || (selectedCell.getCellStatus().equals(CellStatus.EMPTY))) {
            view.showError(Error.CAN_NOT_SUMMON);
            return;
        }
        if (!currentPhase.equals(Phase.BATTLE_PHASE)) {
            view.showError(Error.ACTION_NOT_ALLOWED);
            return;
        }
        Monster monster = ((Monster) selectedCell.getCardInCell());
        if (monster.getMonsterActionType().equals(MonsterActionType.RITUAL)) {
            ritualSummon();
            return;
        }
        if (monster.getLevel() > 4) {
            tributeSummon();
            return;
        }
        normalSummon();
    }

    private boolean isTrapToBeActivatedInSummonSituation() { // TODO haven't added it to flip summon , special summon and ritual summon yet
        for (int i = 1; i <= 5; i++) {
            Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.SPELL_ZONE, i);
            if (cell.getCellStatus().equals(CellStatus.EMPTY)) {
                continue;
            } else if (cell.getCardInCell().getCardType().equals(CardType.SPELL)) {
                continue;
            }
            Trap trap = (Trap) cell.getCardInCell();
            switch (trap.getTrapEffect()) {
                case SOLEMN_WARNING_EFFECT:
                case TORRENTIAL_TRIBUTE_EFFECT:
                    view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL, getOpponentPlayer().getNickname());
                    if (view.yesNoQuestion("do you want to activate your trap and spell?")) {
                        return true;
                    } else {
                        view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL, getCurrentPlayer().getNickname());
                        return false;
                    }
                case TRAP_HOLE_EFFECT:
                    if (isValidSituationForTrapHoleTrapEffect()) {
                        view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL, getOpponentPlayer().getNickname());
                        if (view.yesNoQuestion("do you want to activate your trap and spell?")) {
                            return true;
                        } else {
                            view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL, getCurrentPlayer().getNickname());
                            return false;
                        }
                    }
            }
        }
        return false;
    }

    private boolean isValidSituationForTrapHoleTrapEffect() {
        if (((Monster) selectedCell.getCardInCell()).getAttackPower() > 1000)
            return true;
        return false;
    }


    private boolean isTrapOrSpellInSummonSituationActivated() {//TODO destroy trap or spell after using
        while (true) {
            int address = view.getAddressForTrapOrSpell();
            if (address == -1) {
                return false;
            } else if (address >= 1 && address <= 5) {
                Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.SPELL_ZONE, address);
                if (cell.getCardInCell().getCardType().equals(CardType.SPELL)) {
                    Spell spell = (Spell) cell.getCardInCell();
                    // davood !
                } else {
                    Trap trap = (Trap) cell.getCardInCell();
                    if (isValidActivateTrapEffectInSummonSituationToDo(trap.getTrapEffect())) {
                        view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
                        getCurrentPlayer().getPlayerBoard().removeSpellOrTrapFromBoard(address); //CHECK correctly removed ?
                    }
                    return true;
                }
            } else
                view.showError(Error.INVALID_NUMBER);
        }
    }

    private boolean isValidActivateTrapEffectInSummonSituationToDo(TrapEffect trapEffect) {
        switch (trapEffect) {
            case TORRENTIAL_TRIBUTE_EFFECT:
                torrentialTributeTrapEffect();
                return true;
            case TRAP_HOLE_EFFECT:
                if (isValidSituationForTrapHoleTrapEffect()) {
                    trapHoleTrapEffect();
                    return true;
                }
            case SOLEMN_WARNING_EFFECT:
                solemnWarningTrapEffect();
                return true;
            default:
                view.showError(Error.PREPARATIONS_IS_NOT_DONE);
                return false;
        }
    }

    private void torrentialTributeTrapEffect() {
        for (int i = 1; i <= 5; i++) {
            if (!getCurrentPlayer().getPlayerBoard().getACellOfBoard(Zone.MONSTER_ZONE, i).getCellStatus().equals(CellStatus.EMPTY))
                getCurrentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(i);
            if (!getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.MONSTER_ZONE, i).getCellStatus().equals(CellStatus.EMPTY))
                getOpponentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(i);
        }
    }

    private void trapHoleTrapEffect() {
        if (isValidSituationForTrapHoleTrapEffect()) {
            getOpponentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(selectedCellAddress);
            deselectCard(0);
        }
    }

    private void solemnWarningTrapEffect() {
        getCurrentPlayer().decreaseLP(2000);
        getCurrentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(selectedCellAddress);
        deselectCard(0);
    }

    private void normalSummon() {
        getCurrentPlayer().getPlayerBoard().addMonsterToBoard((Monster) selectedCell.getCardInCell(), CellStatus.OFFENSIVE_OCCUPIED);
        isSummonOrSetOfMonsterUsed = true;
        view.showSuccessMessage(SuccessMessage.SUMMONED_SUCCESSFULLY);
        if (isTrapToBeActivatedInSummonSituation()) {
            if (isTrapOrSpellInSummonSituationActivated()) {
                view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL, getCurrentPlayer().getNickname());
            }
        }
        deselectCard(0);
    }

    private void tributeSummon() {
        if (((Monster) selectedCell.getCardInCell()).getLevel() >= 7) {
            if (didTribute(2)) {
                normalSummon();
            } else view.showError(Error.NOT_ENOUGH_CARDS_TO_TRIBUTE);
        } else if (((Monster) selectedCell.getCardInCell()).getLevel() >= 5) {
            if (didTribute(1)) {
                normalSummon();
            } else view.showError(Error.NOT_ENOUGH_CARDS_TO_TRIBUTE);
        }
        deselectCard(0);
    }

    private boolean didTribute(int number) {
        view.showSuccessMessage(SuccessMessage.TRIBUTE_SUMMON_ENTER_ADDRESS);
        int[] address = new int[number];
        for (int i = 1; i <= number; i++) {
            int tributeAddress = view.getTributeAddress();
            address[i - 1] = tributeAddress;
        }
        for (int i : address) {
            if (i > 5 || i < 1) {
                view.showError(Error.INVALID_SELECTION);
                return false;
            }
            if (getCurrentPlayer().getPlayerBoard().getACellOfBoard(Zone.MONSTER_ZONE, i).getCellStatus().equals(CellStatus.EMPTY)) {
                view.showError(Error.WRONG_MONSTER_ADDRESS);
                return false;
            }
        }
        if (number == 2) {
            if (address[0] == address[1]) {
                view.showError(Error.INVALID_SELECTION);
                return false;
            }
        }
        for (int i : address) {
            getCurrentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(i);
        }
        return true;
    }

    private void ritualSummon() {
        List<Card> currentPlayerHand = getCurrentPlayerHand();
        if (!isRitualCardInHand()) {
            view.showError(Error.CAN_NOT_RITUAL_SUMMON);
        } else if (!sumOfSubsequences("cardName")) {
            view.showError(Error.CAN_NOT_RITUAL_SUMMON);
        } else {
            while (true) {
                Monster monster = (Monster) selectedCell.getCardInCell();
                if (Objects.requireNonNull(monster).getMonsterActionType() == MonsterActionType.RITUAL) break;
                else {
                    view.showError(Error.RITUAL_SUMMON_NOW);
                    view.getSummonOrderForRitual();
                }
            }
            while (true) {
                if (areCardsLevelsEnoughToSummonRitualMonster()) break;
                else view.showError(Error.LEVEL_DOES_NOT_MATCH);
            }
            Matcher matcherOfPosition = view.getPositionForSetRitualMonster();
            setRitualMonster(matcherOfPosition);
        }
        deselectCard(0);
    }

    public void setRitualMonster(Matcher matcherOfPosition) {
        MonsterZone monsterZone = getCurrentPlayer().getPlayerBoard().returnMonsterZone();
        if (matcherOfPosition.group().equals("attack")) {
            for (int i = 1; i <= 5; i++) {
                if (monsterZone.getCellWithAddress(i).getCellStatus() == CellStatus.EMPTY) {
                    Card card = Card.getCardByName(selectedCell.getCardInCell().getName());
                    monsterZone.getCellWithAddress(i).setCardInCell(card);
                    monsterZone.getCellWithAddress(i).setCellStatus(CellStatus.OFFENSIVE_OCCUPIED);
                }
            }
            selectedCell.setCellStatus(CellStatus.OFFENSIVE_OCCUPIED);
        } else if (matcherOfPosition.group().equals("defense")) {
            for (int i = 1; i <= 5; i++) {
                if (monsterZone.getCellWithAddress(i).getCellStatus() == CellStatus.EMPTY) {
                    Card card = Card.getCardByName(selectedCell.getCardInCell().getName());
                    monsterZone.getCellWithAddress(i).setCardInCell(card);
                    monsterZone.getCellWithAddress(i).setCellStatus(CellStatus.DEFENSIVE_OCCUPIED);
                }
            }
            selectedCell.setCellStatus(CellStatus.DEFENSIVE_OCCUPIED);
        }
        view.showSuccessMessage(SuccessMessage.SUMMONED_SUCCESSFULLY);
    }

    public boolean areCardsLevelsEnoughToSummonRitualMonster() {
        Matcher addresses = view.getMonstersAddressesToBringRitual();
        String[] split = addresses.pattern().split("\\s+");
        int sum = 0;
        MonsterZone monsterZone = getCurrentPlayer().getPlayerBoard().returnMonsterZone();
        for (String s : split) {
            Monster monster = (Monster) monsterZone.getCellWithAddress(Integer.parseInt(s)).getCardInCell();
            sum += monster.getLevel();
        }
        Monster monster = (Monster) selectedCell.getCardInCell();
        return sum == monster.getLevel();
    }

    private boolean isRitualCardInHand() {
        List<Card> currentPlayerHand = getCurrentPlayerHand();
        for (Card card : currentPlayerHand) {
            if (card.getCardType() == CardType.MONSTER) {
                if (MonsterActionType.getActionTypeByName(card.getName()) == MonsterActionType.RITUAL) return true;
            }
        }
        return false;
    }

    public boolean sumOfSubsequences(String cardName) {
        MonsterZone monsterZone = getCurrentPlayer().getPlayerBoard().returnMonsterZone();
        int sum = 0;
        int[] levels = new int[5];
        for (int i = 0; i < 5; i++) {
            Monster cardNumberI = (Monster) monsterZone.getCellWithAddress(i).getCardInCell();
            levels[i] = cardNumberI.getLevel();
        }
        ArrayList<Integer> sumOfSubset = new ArrayList<>();
        sumOfSubset = subsetSums(levels, 0, levels.length - 1, 0, sumOfSubset);
        Monster ritualCard = (Monster) Card.getCardByName(cardName);
        for (int i = 0; i < sumOfSubset.size(); i++) {
            if (sumOfSubset.get(i) == Objects.requireNonNull(ritualCard).getLevel()) return true;
        }
        return false;
    }

    public ArrayList subsetSums(int[] arr, int l, int r, int sum, ArrayList<Integer> sumOfSubsets) {
        if (l > r) {
            sumOfSubsets.add(sum);
            return sumOfSubsets;
        }
        subsetSums(arr, l + 1, r, sum + arr[l], sumOfSubsets);
        subsetSums(arr, l + 1, r, sum, sumOfSubsets);
        return sumOfSubsets;
    }


    public void setMonster() {
        if (!isValidSelectionForSummonOrSet()) {
            return;
        }
        if ((!selectedCellZone.equals(Zone.HAND)) || (selectedCell.getCellStatus().equals(CellStatus.EMPTY))) {
            view.showError(Error.CAN_NOT_SET);
            deselectCard(0);
            return;
        }
        if (!(currentPhase.equals(Phase.MAIN_PHASE_1) || currentPhase.equals(Phase.MAIN_PHASE_2))) {
            view.showError(Error.ACTION_NOT_ALLOWED);
            deselectCard(0);
            return;
        }
        Monster monster = (Monster) selectedCell.getCardInCell();
        getCurrentPlayer().getPlayerBoard().addMonsterToBoard(monster, CellStatus.DEFENSIVE_HIDDEN);
        deselectCard(0);
    }

    private boolean isValidSelectionForSummonOrSet() {
        if (selectedCellZone.equals(Zone.NONE)) {
            view.showError(Error.NO_CARD_SELECTED_YET);
            return false;
        } else if (getCurrentPlayer().getPlayerBoard().isMonsterZoneFull()) {
            view.showError(Error.MONSTER_ZONE_IS_FULL);
            return false;
        } else if (isSummonOrSetOfMonsterUsed) {
            view.showError(Error.ALREADY_SUMMONED_OR_SET);
            return false;
        }
        return true;
    }

    public void changeMonsterPosition(Matcher matcher) {
        if (selectedCell == null) {
            view.showError(Error.NO_CARD_SELECTED_YET);
        } else if (!(selectedCellZone == Zone.MONSTER_ZONE)) {
            view.showError(Error.CAN_NOT_CHANGE_POSITION);
        } else if (!(currentPhase == Phase.MAIN_PHASE_1 || currentPhase == Phase.MAIN_PHASE_2)) {
            view.showError(Error.ACTION_CAN_NOT_WORK_IN_THIS_PHASE);
        } else if (!(matcher.group("position").equals("attack") && selectedCell.getCellStatus() == CellStatus.DEFENSIVE_OCCUPIED ||
                matcher.group("position").equals("defense") && selectedCell.getCellStatus() == CellStatus.OFFENSIVE_OCCUPIED)) {
            view.showError(Error.CURRENTLY_IN_POSITION);
        } else if (hasCardChangedPosition()) {
            view.showError(Error.ALREADY_CHANGED_POSITION);
        } else {
            if (matcher.group("position").equals("attack")) selectedCell.setCellStatus(CellStatus.OFFENSIVE_OCCUPIED);
            else if (matcher.group("position").equals("defense"))
                selectedCell.setCellStatus(CellStatus.DEFENSIVE_OCCUPIED);
            view.showSuccessMessage(SuccessMessage.POSITION_CHANGED_SUCCESSFULLY);
            changePositionUsed();
        }
        deselectCard(0);
    }

    public void setSpellOrTrap(Matcher matcher) {
        if (selectedCell == null) {
            view.showError(Error.NO_CARD_SELECTED_YET);
        } else if (!selectedCellZone.equals(Zone.HAND)) {
            view.showError(Error.CAN_NOT_SET);
        } else if (!(selectedCell.getCardInCell().getCardType() == CardType.SPELL &&
                (currentPhase == Phase.MAIN_PHASE_1 || currentPhase == Phase.MAIN_PHASE_2))) {
            view.showError(Error.ACTION_CAN_NOT_WORK_IN_THIS_PHASE);
        } else if (getCurrentPlayer().getPlayerBoard().isSpellZoneFull()) {
            view.showError(Error.SPELL_ZONE_IS_FULL);
        } else { // we can change place of this for ,,, you know...
            SpellZone spellZone = getCurrentPlayer().getPlayerBoard().returnSpellZone();
            for (int i = 1; i <= 5; i++) {
                if (spellZone.getCellWithAddress(i).getCellStatus() == CellStatus.EMPTY) {
                    Card card = Card.getCardByName(matcher.group(1));
                    spellZone.getCellWithAddress(i).setCardInCell(card);
                    spellZone.getCellWithAddress(i).setCellStatus(CellStatus.HIDDEN);
                }
            }
            view.showSuccessMessage(SuccessMessage.SET_SUCCESSFULLY);
        }
        deselectCard(0);
    }

    public List<Card> getCurrentPlayerHand() {
        if (getCurrentPlayer() == firstPlayer) {
            return getFirstPlayerHand();
        } else if (getCurrentPlayer() == secondPlayer) {
            return getSecondPlayerHand();
        }
        return null;
    }

    public void faceUpSpellOrTrap() {

    }

    public void activateEffectOfSpellOrTrap() {
        if (selectedCell == null) {
            view.showError(Error.NO_CARD_SELECTED_YET);
            return;
        }
        if (!selectedCellZone.equals(Zone.SPELL_ZONE) && !selectedCellZone.equals(Zone.HAND)) {
            view.showError(Error.ONLY_SPELL_CAN_ACTIVE);
            return;
        }
        if (!currentPhase.equals(Phase.MAIN_PHASE_1) && !currentPhase.equals(Phase.MAIN_PHASE_2)) {
            view.showError(Error.ACTION_CAN_NOT_WORK_IN_THIS_PHASE);
            return;
        }
        if (selectedCellZone.equals(Zone.SPELL_ZONE)) {
            if (selectedCell.getCellStatus().equals(CellStatus.OCCUPIED)) {
                view.showError(Error.CARD_ALREADY_ACTIVATED);
                return;
            } else if (getCurrentPlayer().getPlayerBoard().isSpellZoneFull()) {
                view.showError(Error.SPELL_ZONE_IS_FULL);
                return;
            }
        }
        if (!isSpellReadyToActivate()) {
            view.showError(Error.PREPARATIONS_IS_NOT_DONE);
            return;
        }
        if (selectedCell.getCardInCell().getCardType() == CardType.SPELL)
            normalSpellActivate();
        else
            normalTrapActivate();
    }

    private void normalSpellActivate() {

    }

    private void normalTrapActivate() {
        switch (((Trap) selectedCell.getCardInCell()).getTrapEffect()) {
            case MIND_CRUSH_EFFECT:
                mindCrushTrapEffect();
                return;
            case CALL_OF_THE_HAUNTED_EFFECT:
                callOfTheHauntedTrapEffect();
                return;
            default:
                view.showError(Error.PREPARATIONS_IS_NOT_DONE);
                return;
        }
    }

    private void mindCrushTrapEffect() {
        boolean happened = false;
        while (true) {
            String cardName = view.askCardName();
            if (Card.getCardByName(cardName) == null) {
                view.showError(Error.WRONG_CARD_NAME);
                continue;
            } else {
                Card card = Card.getCardByName(cardName);
                for (Card handCard : getOpponentHand()) {
                    if (handCard.getName().equals(cardName)) {
                        happened = true;
                        getOpponentHand().remove(handCard);
                    }
                }
                if (!happened) {
                    getCurrentPlayerHand().remove(0);
                }
                view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
                getCurrentPlayer().getPlayerBoard().removeSpellOrTrapFromBoard(selectedCellAddress);
                return;

            }
        }
    }

    private void callOfTheHauntedTrapEffect() {
        boolean containsMonster = false;
        if (getCurrentPlayer().getPlayerBoard().isGraveYardEmpty()) {
            view.showError(Error.PREPARATIONS_IS_NOT_DONE);
            return;
        } else if (!getCurrentPlayer().getPlayerBoard().isGraveYardEmpty()) {
            for (Card graveYardCard : getCurrentPlayer().getPlayerBoard().returnGraveYard().getGraveYardCards()) {
                if (graveYardCard.getCardType().equals(CardType.MONSTER))
                    containsMonster = true;
            }
        } else if (getCurrentPlayer().getPlayerBoard().isMonsterZoneFull()) {
            view.showError(Error.PREPARATIONS_IS_NOT_DONE);
            return;
        }
        if (!containsMonster) { //NOT SURE about error!!!
            view.showError(Error.PREPARATIONS_IS_NOT_DONE);
            return;
        } else view.showGraveYard();
        while (true) {
            int address = view.getNumberOfCardForCallOfTheHaunted();
            if (address < 0 || address > getCurrentPlayer().getPlayerBoard().returnGraveYard().getGraveYardCards().size()) {
                view.showError(Error.INVALID_NUMBER);
                continue;
            } else {
                Card selectedCardToSummon = getCurrentPlayer().getPlayerBoard().getCardInGraveYard(address);
                if (!selectedCardToSummon.getCardType().equals(CardType.MONSTER)) {
                    view.showError(Error.INVALID_SELECTION);
                    continue;
                } else {
                    getCurrentPlayer().getPlayerBoard().addMonsterToBoard((Monster) selectedCardToSummon, CellStatus.OFFENSIVE_OCCUPIED);
                    view.showSuccessMessage(SuccessMessage.SPELL_ACTIVATED); // not spell actually!!!!!!!!!!!;
                    getCurrentPlayer().getPlayerBoard().removeSpellOrTrapFromBoard(selectedCellAddress);
                    return;
                }

            }
        }
    }

    private boolean isSpellReadyToActivate() {
        if (selectedCell.getCardInCell().getCardType().equals(CardType.SPELL)) {

        } else {

        }
        return false;
    }

    public void attackToCard(Matcher matcher) {
        if (!isValidAttack(matcher))
            return;
        int toBeAttackedCardAddress = Integer.parseInt(matcher.group("monsterZoneNumber"));
        CellStatus opponentCellStatus = getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.MONSTER_ZONE, toBeAttackedCardAddress).getCellStatus();
        if (opponentCellStatus.equals(CellStatus.DEFENSIVE_HIDDEN)) {
            attackToDHCard(getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.MONSTER_ZONE, toBeAttackedCardAddress), toBeAttackedCardAddress);
        } else if (opponentCellStatus.equals(CellStatus.DEFENSIVE_OCCUPIED)) {
            attackToDOCard(getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.MONSTER_ZONE, toBeAttackedCardAddress), toBeAttackedCardAddress);
        } else {
            attackToOOCard(getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.MONSTER_ZONE, toBeAttackedCardAddress), toBeAttackedCardAddress);
        }
        attackUsed();
        deselectCard(0);
    }

    private void attackToDHCard(Cell opponentCellToBeAttacked, int toBeAttackedCardAddress) { //TODO might have effect
        Monster opponentCard = (Monster) opponentCellToBeAttacked.getCardInCell();
        view.showSuccessMessageWithAString(SuccessMessage.DH_CARD_BECOMES_DO, opponentCard.getName());
        opponentCellToBeAttacked.changeCellStatus(CellStatus.DEFENSIVE_OCCUPIED);
        attackToDOCard(opponentCellToBeAttacked, toBeAttackedCardAddress);
        //am i sure? i changed it to DO and just used attack to DO card
        DOFlipSummon();
    }

    private void DOFlipSummon() {
        //TODO
    }

    private void attackToDOCard(Cell opponentCellToBeAttacked, int toBeAttackedCardAddress) { //TODO might have effect
        Monster playerCard = (Monster) selectedCell.getCardInCell();
        Monster opponentCard = (Monster) opponentCellToBeAttacked.getCardInCell();
        int damage = playerCard.getAttackPower() - opponentCard.getDefensePower();
        if (damage > 0) {
            view.showSuccessMessage(SuccessMessage.DEFENSIVE_MONSTER_DESTROYED);
            getOpponentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(toBeAttackedCardAddress);
        } else if (damage < 0) {
            view.showSuccessMessageWithAnInteger(SuccessMessage.DAMAGE_TO_CURRENT_PLAYER_AFTER_ATTACK_TI_HIGHER_DEFENSIVE_DO_OR_DH_MONSTER, damage);
            getCurrentPlayer().decreaseLP(-damage);
        } else {
            view.showSuccessMessage(SuccessMessage.NO_CARD_DESTROYED);
        }
    }

    private void attackToOOCard(Cell opponentCellToBeAttacked, int toBeAttackedCardAddress) { // might have effect
        Monster playerCard = (Monster) selectedCell.getCardInCell();
        Monster opponentCard = (Monster) opponentCellToBeAttacked.getCardInCell();
        if (isTrapToBeActivatedInAttackSituation()) {
            if (isTrapOrSpellInAttackSituationActivated()) {
                view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL, getCurrentPlayer().getNickname());
                return;
            }
        }
        view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL, getCurrentPlayer().getNickname());
        int damage = playerCard.getAttackPower() - opponentCard.getAttackPower();
        if (damage > 0) {
            view.showSuccessMessageWithAnInteger(SuccessMessage.OPPONENT_RECEIVE_DAMAGE_AFTER_ATTACK, damage);
            getOpponentPlayer().decreaseLP(damage);
            getOpponentPlayer().getPlayerBoard().addCardToGraveYard(opponentCellToBeAttacked.getCardInCell());
            getOpponentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(toBeAttackedCardAddress);
        } else if (damage < 0) {
            view.showSuccessMessageWithAnInteger(SuccessMessage.CURRENT_PLAYER_RECEIVE_DAMAGE_AFTER_ATTACK, damage);
            getCurrentPlayer().decreaseLP(-damage);
            getCurrentPlayer().getPlayerBoard().addCardToGraveYard(opponentCellToBeAttacked.getCardInCell());
            getCurrentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(selectedCellAddress);
        } else {
            view.showSuccessMessage(SuccessMessage.NO_DAMAGE_TO_ANYONE);
            getOpponentPlayer().getPlayerBoard().addCardToGraveYard(opponentCellToBeAttacked.getCardInCell());
            getCurrentPlayer().getPlayerBoard().addCardToGraveYard(opponentCellToBeAttacked.getCardInCell());
            getOpponentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(toBeAttackedCardAddress);
            getCurrentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(selectedCellAddress);
        }
    }

    private boolean isValidAttack(Matcher matcher) {
        int address = Integer.parseInt(matcher.group("monsterZoneNumber"));
        if (address > 5 || address < 1) {
            view.showError(Error.INVALID_NUMBER);
            return false;
        }
        if (selectedCell == null) {
            view.showError(Error.NO_CARD_SELECTED_YET);
            return false;
        } else if (!selectedCellZone.equals(Zone.MONSTER_ZONE)) {
            view.showError(Error.CAN_NOT_ATTACK);
            return false;
        }
        if (!selectedCell.getCellStatus().equals(CellStatus.OFFENSIVE_OCCUPIED)) { // u sure ?
            view.showError(Error.CAN_NOT_ATTACK);
            return false;
        } else if (!currentPhase.equals(Phase.BATTLE_PHASE)) {
            view.showError(Error.ACTION_NOT_ALLOWED);
            return false;
        }
        if (hasCardUsedItsAttack()) {
            view.showError(Error.ALREADY_ATTACKED);
            return false;
        }
        Cell opponentCell = getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.MONSTER_ZONE, address);
        if (opponentCell.getCellStatus().equals(CellStatus.EMPTY)) {
            view.showError(Error.NO_CARD_TO_BE_ATTACKED);
            return false;
        }
        return true;
    }

    private boolean isTrapToBeActivatedInAttackSituation() {//TODO destroy trap or spell after using
        for (int i = 1; i <= 5; i++) {
            Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.SPELL_ZONE, i);
            if (cell.getCellStatus().equals(CellStatus.EMPTY)) {
                continue;
            } else if (cell.getCardInCell().getCardType().equals(CardType.SPELL)) {
                continue;
            }
            Trap trap = (Trap) cell.getCardInCell();
            switch (trap.getTrapEffect()) {
                case MAGIC_CYLINDER_EFFECT:
                case MIRROR_FORCE_EFFECT:
                case NEGATE_ATTACK_EFFECT:
                    view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL, getOpponentPlayer().getNickname());
                    if (view.yesNoQuestion("do you want to activate your trap and spell?")) {
                        return true;
                    } else {
                        view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL, getCurrentPlayer().getNickname());
                        return false;
                    }
            }
        }
        return false;
    }

    private boolean isTrapOrSpellInAttackSituationActivated() {
        while (true) {
            int address = view.getAddressForTrapOrSpell();
            if (address == -1) {
                return false;
            } else if (address >= 1 && address <= 5) {
                Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoard(Zone.SPELL_ZONE, address);
                if (cell.getCardInCell().getCardType().equals(CardType.SPELL)) {
                    Spell spell = (Spell) cell.getCardInCell();
                    // davood !
                } else {
                    Trap trap = (Trap) cell.getCardInCell();
                    if (activateTrapEffectInAttackSituation(trap.getTrapEffect())) {
                        view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
                        getCurrentPlayer().getPlayerBoard().removeSpellOrTrapFromBoard(address); //CHECK correctly removed ?
                    }
                    return true;
                }
            } else
                view.showError(Error.INVALID_NUMBER);
        }
    }


    private boolean activateTrapEffectInAttackSituation(TrapEffect trapEffect) {
        switch (trapEffect) {
            case MAGIC_CYLINDER_EFFECT:
                TrapMagicCylinderEffect();
                return true;
            case MIRROR_FORCE_EFFECT:
                TrapMirrorForceEffect();
                return true;
            case NEGATE_ATTACK_EFFECT:
                TrapNegateAttackEffect();
                return true;
            default:
                view.showError(Error.PREPARATIONS_IS_NOT_DONE);
                return false;
        }
    }

    private void TrapMagicCylinderEffect() {
        getCurrentPlayer().decreaseLP(((Monster) selectedCell.getCardInCell()).getAttackPower());
        getCurrentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(selectedCellAddress);
    }

    private void TrapMirrorForceEffect() {
        for (int i = 1; i <= 5; i++) {
            if (getCurrentPlayer().getPlayerBoard().getACellOfBoard(Zone.MONSTER_ZONE, i).getCellStatus().equals(CellStatus.OFFENSIVE_OCCUPIED))
                getCurrentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(i);
        }
        view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
    }

    private void TrapNegateAttackEffect() {
        deselectCard(0);
        nextPhase();
    }

    private boolean hasCardUsedItsAttack() {
        for (Integer cell : usedCellsToAttackNumbers) {
            if (cell == selectedCellAddress)
                return true;
        }
        return false;
    }

    private void attackUsed() {
        usedCellsToAttackNumbers.add(selectedCellAddress);
    }

    private boolean hasCardChangedPosition() {
        for (Integer positionCard : changedPositionCards) {
            if (positionCard.equals(selectedCellAddress))
                return true;
        }
        return false;
    }

    private void changePositionUsed() {
        usedCellsToAttackNumbers.add(selectedCellAddress);
    }

    public void drawCardFromDeck() {
        DuelPlayer currentPlayer = getCurrentPlayer();
        Card card;
        if ((card = currentPlayer.getPlayDeck().getMainCards().get(0)) != null) {
            currentPlayer.getPlayDeck().getMainCards().remove(0);
            addCardToFirstPlayerHand(card);
            view.showSuccessMessageWithAString(SuccessMessage.CARD_ADDED_TO_THE_HAND, card.getName());
        } else {
            //TODO a error is needed i guess! because he couldn't draw card ...
            duelGameController.checkGameResult(currentPlayer);// no card so this is loser!
        }
    }

    public void flipSummon() {//TODO might have effect
        if (selectedCell == null) {
            view.showError(Error.NO_CARD_SELECTED_YET);
        } else if (!(selectedCellZone == Zone.MONSTER_ZONE)) {
            view.showError(Error.CAN_NOT_CHANGE_POSITION);
        } else if (!(currentPhase == Phase.MAIN_PHASE_1 || currentPhase == Phase.MAIN_PHASE_2)) {
            view.showError(Error.ACTION_CAN_NOT_WORK_IN_THIS_PHASE);
        } else if (selectedCell.getCellStatus() == CellStatus.DEFENSIVE_HIDDEN) {
            view.showError(Error.FLIP_SUMMON_NOT_ALLOWED);
        } else {
            selectedCell.setCellStatus(CellStatus.OFFENSIVE_OCCUPIED);
            view.showSuccessMessage(SuccessMessage.FLIP_SUMMON_SUCCESSFUL);
        }
    }

    public void directAttack() { // probably no effect ...
        if (selectedCell == null) {
            view.showError(Error.NO_CARD_SELECTED_YET);
            return;
        }
        if (!selectedCellZone.equals(Zone.MONSTER_ZONE)) {
            view.showError(Error.CAN_NOT_ATTACK);
            return;
        }
        if (!selectedCell.getCellStatus().equals(CellStatus.OFFENSIVE_OCCUPIED)) { // u sure ?
            view.showError(Error.CAN_NOT_ATTACK);
            return;
        }
        if (!getCurrentPhase().equals(Phase.BATTLE_PHASE)) {
            view.showError(Error.ACTION_CAN_NOT_WORK_IN_THIS_PHASE);
            return;
        }
        if (!getCurrentPlayer().getPlayerBoard().isMonsterZoneEmpty()) {
            view.showError(Error.CANT_DIRECT_ATTACK);
            return;
        }
        if (hasCardUsedItsAttack()) {
            view.showError(Error.ALREADY_ATTACKED);
            return;
        }
        Monster monster = (Monster) selectedCell.getCardInCell();
        getOpponentPlayer().decreaseLP(monster.getAttackPower());
        view.showSuccessMessageWithAnInteger(SuccessMessage.OPPONENT_RECEIVE_DAMAGE_AFTER_DIRECT_ATTACK, monster.getAttackPower());
    }

    public void nextPhase() {
        if (currentPhase.equals(Phase.DRAW_PHASE)) {
            currentPhase = Phase.STAND_BY_PHASE;
        } else if (currentPhase.equals(Phase.STAND_BY_PHASE)) {
            currentPhase = Phase.MAIN_PHASE_1;
        } else if (currentPhase == Phase.MAIN_PHASE_1) {
            currentPhase = Phase.BATTLE_PHASE;
        } else if (currentPhase == Phase.BATTLE_PHASE) {
            currentPhase = Phase.MAIN_PHASE_2;
        } else if (currentPhase == Phase.MAIN_PHASE_2) {
            currentPhase = Phase.DRAW_PHASE;
            view.showSuccessMessageWithAString(SuccessMessage.PLAYERS_TURN, getCurrentPlayer().getNickname());
            isSummonOrSetOfMonsterUsed = false;
            selectedCell = null;
            selectedCellZone = Zone.NONE;
            usedCellsToAttackNumbers.clear();
            changedPositionCards.clear();
            changeTurn();
        }
    }

    public List<Card> getFirstPlayerHand() {
        return firstPlayerHand;
    }

    public List<Card> getSecondPlayerHand() {
        return secondPlayerHand;
    }

    public void addCardToFirstPlayerHand(Card card) {
        firstPlayerHand.add(card);
    }

    public void addCardToSecondPlayerHand(Card card) {
        secondPlayerHand.add(card);
    }

    private DuelPlayer getCurrentPlayer() {
        if (turn == 1)
            return firstPlayer;
        return secondPlayer;
    }

    private DuelPlayer getOpponentPlayer() {
        if (turn == 1)
            return secondPlayer;
        return firstPlayer;
    }

    public void cancel() {
        selectedCell = null;
        selectedCellZone = Zone.NONE;
    }

    public void monsterReborn() {
        Matcher matcher;
        while (true) {
            matcher = view.monsterReborn();
            if (matcher == null) return;
            else if (Card.getCardByName(matcher.group(2)) == null) view.showError(Error.WRONG_CARD_NAME);
            else break;
        }
        ArrayList<Card> currentPlayer = getCurrentPlayer().getPlayerBoard().returnGraveYard().getGraveYardCards();
        ArrayList<Card> opponentPlayer = getOpponentPlayer().getPlayerBoard().returnGraveYard().getGraveYardCards();
        if (matcher.group(1).equals("opponent") && selectedCellZone == Zone.GRAVEYARD) {
            for (Card card : opponentPlayer) {
                if (card.getName().equals(matcher.group(2))) {
                    ritualSummon();
                }
            }
        } else {
            for (Card card : currentPlayer) {
                if (card.getName().equals(matcher.group(2))) {
                    ritualSummon();
                }
            }
        }
    }

    private void terraForming() {
        //TODO show deck
        String cardName = view.getCardNameForTerraForming();
        while (true) {
            if (cardName.equals("cancel")) return;
            else if (Card.getCardByName(cardName) == null) view.showError(Error.WRONG_CARD_NAME);
            else break;
        }
        List<Card> deckCards = getCurrentPlayer().getPlayDeck().getMainCards();
        while (true) {
            for (Card card : deckCards) {
                if (SpellType.getSpellTypeByTypeName(selectedCell.getCardInCell().getName()) == SpellType.FIELD) {
                    getCurrentPlayerHand().add(selectedCell.getCardInCell());
                    break;
                }
            }
            //TODO shuffle deck
        }
    }

    private void potOfGreed() {
        List<Card> deckCards = getCurrentPlayer().getPlayDeck().getMainCards();
        getSecondPlayerHand().add(deckCards.get(1));
        getSecondPlayerHand().add(deckCards.get(2));
    }

    private void raigeki() {
        MonsterZone monsterZone = getOpponentPlayer().getPlayerBoard().returnMonsterZone();
        int i = 0;
        while (monsterZone.getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY || i >= 5) {
            monsterZone.removeCard(i);
            i++;
        }
    }

    public void changeOfHeart() {
        String cardName = view.getCardNameForChangeOfHeart();
        while (true) {
            if (cardName.equals("cancel")) return;
            else if (Card.getCardByName(cardName) == null) view.showError(Error.WRONG_CARD_NAME);
            else break;
        }
        MonsterZone monsterZone = getOpponentPlayer().getPlayerBoard().returnMonsterZone();
        int i = 0;
        while (monsterZone.getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY || i >= 5) {
            if (monsterZone.getCellWithAddress(i).getCardInCell().getName().equals(cardName)) {
                getOpponentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(i);
                getCurrentPlayer().getPlayerBoard().addMonsterToBoard((Monster) Card.getCardByName(cardName), CellStatus.OFFENSIVE_OCCUPIED);
                break;
            }
            i++;
        }
    }

    private void removeChangeOfHearts(CellStatus cellStatus, String cardName) {
        getCurrentPlayer().getPlayerBoard().addMonsterToBoard((Monster) Card.getCardByName(cardName), cellStatus);
        int i = 0;
        while (getOpponentPlayer().getPlayerBoard().returnMonsterZone().getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY) {
            if (getOpponentPlayer().getPlayerBoard().returnMonsterZone().getCellWithAddress(i).getCardInCell().getName().equals(cardName))
                getOpponentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(i);
            i++;
        }
    }

    private void HarpiesFeatherDuster() {
        int i = 0;
        while (getOpponentPlayer().getPlayerBoard().returnSpellZone().getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY) {
            getOpponentPlayer().getPlayerBoard().returnSpellZone().removeCard(i);
            i++;
        }
    }

    public void swordsOfRevealingLight() {
    }

    public void darkHole() {
        int i = 0;
        while (getOpponentPlayer().getPlayerBoard().returnMonsterZone().getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY || i >= 5) {
            getOpponentPlayer().getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(i);
            i++;
        }
        i = 0;
        while (getCurrentPlayer().getPlayerBoard().returnMonsterZone().getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY || i >= 5) {
            getCurrentPlayer().getPlayerBoard().returnMonsterZone().removeCard(i);
            i++;
        }
    }

    public void spellAbsorption() {
        // call it each time you active an effect
        getCurrentPlayer().increaseLP(500);
    }

    public void twinTwisters() {
        Matcher matcher = view.getCardsNameTwinTwister();
        while (true) {
            if (matcher == null) return;
            else if (Card.getCardByName(matcher.group(1)) == null || Card.getCardByName(matcher.group(2)) == null
                    || Card.getCardByName(matcher.group(3)) == null) view.showError(Error.WRONG_CARD_NAME);
            else break;
        }
        getCurrentPlayerHand().remove(Card.getCardByName(matcher.group(1)));
        getOpponentPlayer().getPlayerBoard().returnSpellZone().removeCard(Integer.parseInt(matcher.group(2)));
        getOpponentPlayer().getPlayerBoard().returnSpellZone().removeCard(Integer.parseInt(matcher.group(3)));
    }

    public void mysticalSpaceTyphoon() {
        int cardPlace = view.mysticalSpaceTyphoon();
        getOpponentPlayer().getPlayerBoard().returnSpellZone().removeCard(cardPlace);
    }

    public void ringOfDefense() {

    }

    private void swordOfDarkDestruction() {

    }

    public int getSwordsOfRevealingLightRounds() {
        return swordsOfRevealingLightRounds;
    }

    public void setSwordsOfRevealingLightRounds(int swordsOfRevealingLightRounds) {
        this.swordsOfRevealingLightRounds = swordsOfRevealingLightRounds;
    }

    private List<Card> getOpponentHand() {
        if (turn == 1) {
            return secondPlayerHand;
        }
        return firstPlayerHand;
    }

}