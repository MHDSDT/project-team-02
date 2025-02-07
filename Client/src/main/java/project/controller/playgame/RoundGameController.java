package project.controller.playgame;

import project.model.Deck;
import project.model.card.Card;
import project.model.card.Monster;
import project.model.card.Spell;
import project.model.card.Trap;
import project.model.card.informationofcards.*;
import project.model.game.DuelPlayer;
import project.model.game.PlayerBoard;
import project.model.game.board.*;
import project.view.gameview.Animation;
import project.view.gameview.GameView;
import project.view.messages.Error;
import project.view.messages.GameViewMessage;
import project.view.messages.SuccessMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static project.model.card.informationofcards.CardType.*;
import static project.view.messages.GameViewMessage.*;

//import project.view.Menu;
//import project.view.MenusManager;

public class RoundGameController {
    private static RoundGameController instance = null;
    private GameView view;
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
    private DuelGameController duelGameController;
    private List<Integer> usedCellsToAttackNumbers = new ArrayList<>();
    private List<Integer> changedPositionCards = new ArrayList<>();
    private Spell fieldZoneSpell = null;
    private ArrayList<Card> fieldEffectedCards = new ArrayList<>();
    private ArrayList<Integer> fieldEffectedCardsAddress = new ArrayList<>();
    private int isFieldActivated = 0; // 0 : no - 1 : firstPlayed activated it- 2 : secondPlayer activated it
    private HashMap<Card, Monster> firstPlayerHashmapForEquipSpells = new HashMap<>();
    private HashMap<Card, Monster> secondPlayerHashmapForEquipSpells = new HashMap<>();
    private boolean isWithAi;
    private boolean isFinishedGame = false;
    private boolean isFinishedRound = false;
    private boolean cantDrawCardBecauseOfTimeSeal = false;
    private int addressOfTimeSealToRemove;
    private boolean drawUsed = false;

    private RoundGameController() {

    }

    public static RoundGameController getInstance() {
        if (instance == null) instance = new RoundGameController();
        return instance;
    }

    public boolean isWithAi() {
        return isWithAi;
    }

    public boolean isFieldActivated() {
        return isFieldActivated != 0;
    }

    public DuelPlayer getFirstPlayer() {
        return firstPlayer;
    }

    public DuelPlayer getSecondPlayer() {
        return secondPlayer;
    }

    public int getTurn() {
        return turn;
    }

    public Cell getSelectedCell() {
        return selectedCell;
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public void setRoundInfo(DuelPlayer firstPlayer, DuelPlayer secondPlayer, DuelGameController duelGameController, boolean isWithAi) {
        drawUsed = false;
        this.firstPlayer = firstPlayer;
        this.secondPlayer = secondPlayer;
        firstPlayer.setLifePoint(8000);
        secondPlayer.setLifePoint(8000);
        this.duelGameController = duelGameController;
        currentPhase = Phase.DRAW_PHASE;
        this.isWithAi = isWithAi;
        isFinishedRound = false;
        isFinishedGame = false;
        duelGameController.setStartHandCards();

    }

    public void changeTurn() {
        selectedCell = null;
        turn = (turn == 1) ? 2 : 1;

    }

    public void selectCardInHand(int selectAddress) {
        if (selectAddress > getCurrentPlayerHand().size()) {
            Error.showError(Error.INVALID_SELECTION);
            return;
        }
        ArrayList<Card> hand = (ArrayList<Card>) (getCurrentPlayerHand());
        selectedCell = new Cell();
        selectedCellZone = Zone.HAND;
        selectedCell.setCardInCell(hand.get(selectAddress - 1));
        selectedCell.setCellStatus(CellStatus.IN_HAND);
        selectedCellAddress = selectAddress;
        if (!getCurrentPlayer().getNickname().equals("ai"))
            //view.showSuccessMessage(SuccessMessage.CARD_SELECTED);
            ;
    }

    public void selectCardInMonsterZone(int address) {
        if (address > 5 || address < 1) {
            Error.showError(Error.INVALID_SELECTION);
            return;
        } else if (getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, address).getCellStatus().equals(CellStatus.EMPTY)) {
            Error.showError(Error.CARD_NOT_FOUND);
            return;
        }
        selectedCellZone = Zone.MONSTER_ZONE;
        selectedCell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(selectedCellZone, address);
        selectedCellAddress = address;
        //view.showSuccessMessage(SuccessMessage.CARD_SELECTED);
    }

    public void selectCardInSpellZone(int matcher) {
        if (matcher > 5 || matcher < 1) {
            Error.showError(Error.INVALID_SELECTION);
            return;
        } else if (getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, matcher).getCellStatus().equals(CellStatus.EMPTY)) {
            Error.showError(Error.CARD_NOT_FOUND);
            return;
        }
        selectedCellZone = Zone.SPELL_ZONE;
        selectedCell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(selectedCellZone, matcher);
        selectedCellAddress = matcher;
        // view.showSuccessMessage(SuccessMessage.CARD_SELECTED);
    }

    public void deselectCard(int code) {
        if (code == 1) {
            if (selectedCell == null) {
                Error.showError(Error.NO_CARD_SELECTED_YET);
                return;
            }
            // view.showSuccessMessage(SuccessMessage.CARD_DESELECTED);
        }
        selectedCell = null;
        selectedCellZone = Zone.NONE;
        selectedCellAddress = 0;
    }

    private void torrentialTributeTrapEffect() {
        for (int i = 1; i <= 5; i++) {
            if (!getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i).getCellStatus().equals(CellStatus.EMPTY))
                addCardToGraveYard(Zone.MONSTER_ZONE, i, getCurrentPlayer());
            if (!getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i).getCellStatus().equals(CellStatus.EMPTY))
                addCardToGraveYard(Zone.MONSTER_ZONE, i, getOpponentPlayer());
        }
    }

    private void trapHoleTrapEffect(int addedCardAddress) {

        addCardToGraveYard(Zone.MONSTER_ZONE, addedCardAddress, getCurrentPlayer());
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

    public List<Card> getOpponentPlayerHand() {
        if (getCurrentPlayer() == firstPlayer) {
            return getSecondPlayerHand();
        } else if (getCurrentPlayer() == secondPlayer) {
            return getFirstPlayerHand();
        }
        return null;
    }

    private void trapMagicCylinderEffect() {
        getCurrentPlayer().decreaseLP(((Monster) selectedCell.getCardInCell()).getAttackPower());
        addCardToGraveYard(Zone.MONSTER_ZONE, selectedCellAddress, getCurrentPlayer());
        deselectCard(0);
        GameResult result = duelGameController.checkGameResult(getOpponentPlayer(), getCurrentPlayer(), GameResultToCheck.NO_LP);
        if (result == GameResult.GAME_FINISHED) {
            finishGame(getOpponentPlayer());
        } else if (result == GameResult.ROUND_FINISHED) {
            finishRound(getOpponentPlayer());
        }
    }

    private void trapMirrorForceEffect() {
        for (int i = 1; i <= 5; i++) {
            if (getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i).getCellStatus().equals(CellStatus.OFFENSIVE_OCCUPIED))
                addCardToGraveYard(Zone.MONSTER_ZONE, i, getCurrentPlayer());
        }
        //  view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
        deselectCard(0);
    }

    private void trapNegateAttackEffect() {
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
        changedPositionCards.add(selectedCellAddress);
    }

    public void drawCardFromDeck() {
        if (currentPhase != Phase.DRAW_PHASE || drawUsed)
            return;
        if (cantDrawCardBecauseOfTimeSeal) {
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showPopUpMessageForEffect("You can't draw card this turn because of time seal effect!", TRAP);
            addCardToGraveYard(Zone.SPELL_ZONE, addressOfTimeSealToRemove, getOpponentPlayer());
            cantDrawCardBecauseOfTimeSeal = false;
            drawUsed = true;
            nextPhase();
        }
        if (drawUsed)
            return;
        DuelPlayer currentPlayer = getCurrentPlayer();
        Card card;
        if (currentPlayer.getPlayDeck().getMainCards().size() != 0) {
            card = currentPlayer.getPlayDeck().getMainCards().get(currentPlayer.getPlayDeck().getMainCards().size() - 1);
            if (turn == 1) {
                System.out.println("draw " + card.getName());
                addCardToFirstPlayerHand(card);
            } else {
                System.out.println("draw " + card.getName());
                addCardToSecondPlayerHand(card);
            }
            currentPlayer.getPlayDeck().getMainCards().remove(currentPlayer.getPlayDeck().getMainCards().size() - 1);
            drawUsed = true;
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.DRAW_CARD, card.getName(), 0, 0, 0, true);
            //view.drawCardFromDeckAnimation(card.getName(), true);
        } else {
            GameResult result = duelGameController.checkGameResult(currentPlayer, getOpponentPlayer(), GameResultToCheck.NO_CARDS_TO_DRAW);// no card so this is loser!
            if (result == GameResult.GAME_FINISHED) {
                finishGame(currentPlayer);
            } else if (result == GameResult.ROUND_FINISHED) {
                finishRound(currentPlayer);
            }
        }

    }

    public GameViewMessage directAttack() { // probably no effect ...
        if (selectedCell == null) {
            Error.showError(Error.NO_CARD_SELECTED_YET);
            return NO_CARD_SELECTED;
        }
        if (!selectedCellZone.equals(Zone.MONSTER_ZONE)) {
            Error.showError(Error.CAN_NOT_ATTACK);
            return CAN_NOT_ATTACK_WITH_THIS_CARD;
        }
        if (!selectedCell.getCellStatus().equals(CellStatus.OFFENSIVE_OCCUPIED)) { // u sure ?
            Error.showError(Error.CAN_NOT_ATTACK);
            return CAN_NOT_ATTACK_WITH_THIS_CARD;
        }
        if (hasCardUsedItsAttack()) {
            Error.showError(Error.ALREADY_ATTACKED);
            return ATTACK_USED_BEFORE;
        }
        Monster monster = (Monster) selectedCell.getCardInCell();
        getOpponentPlayer().decreaseLP(monster.getAttackPower());
        if (!getCurrentPlayer().getNickname().equals("ai"))
            view.playAnimation(Animation.DIRECT_ATTACK, selectedCell.getCardInCell().getName(), selectedCellAddress, 0, 0, true);
        attackUsed();
        GameResult result = duelGameController.checkGameResult(getCurrentPlayer(), getOpponentPlayer(), GameResultToCheck.NO_LP);

        if (result == GameResult.GAME_FINISHED) {
            finishGame(getCurrentPlayer());
        } else if (result == GameResult.ROUND_FINISHED) {
            finishRound(getCurrentPlayer());
        }
        return SUCCESS;
    }

    public GameViewMessage nextPhase() {
        if (currentPhase.equals(Phase.DRAW_PHASE)) {
            if (drawUsed) {
                currentPhase = Phase.STAND_BY_PHASE;
                view.phaseLabel.setText(Phase.STAND_BY_PHASE.toString());
            } else return MUST_DRAW_CARD;
            System.out.println(currentPhase);
        } else if (currentPhase.equals(Phase.STAND_BY_PHASE)) {
            currentPhase = Phase.MAIN_PHASE_1;
            System.out.println(currentPhase);
        } else if (currentPhase == Phase.MAIN_PHASE_1) {
            currentPhase = Phase.BATTLE_PHASE;
            System.out.println(currentPhase);
        } else if (currentPhase == Phase.BATTLE_PHASE) {
            currentPhase = Phase.MAIN_PHASE_2;
            System.out.println(currentPhase);
        } else if (currentPhase == Phase.MAIN_PHASE_2) {
            drawUsed = false;
            currentPhase = Phase.DRAW_PHASE;
            //  view.showSuccessMessageWithAString(SuccessMessage.PLAYERS_TURN, getOpponentPlayer().getNickname());
            isSummonOrSetOfMonsterUsed = false;
            selectedCell = null;
            selectedCellZone = Zone.NONE;
            usedCellsToAttackNumbers.clear();
            changedPositionCards.clear();
            changeTurn();
            view.changeTurn();
            if (isWithAi) {
                if (getCurrentPlayer().getNickname().equals("ai"))
                    aiTurn();
            }

        }
        return SUCCESS;
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

    public DuelPlayer getCurrentPlayer() {
        if (turn == 1)
            return firstPlayer;
        return secondPlayer;
    }

    public DuelPlayer getOpponentPlayer() {
        if (turn == 1)
            return secondPlayer;
        return firstPlayer;
    }

    public void cancel() {
        selectedCell = null;
        selectedCellZone = Zone.NONE;
    }

    public GameViewMessage monsterRebornSpell() {
        ArrayList<Card> graveYard = null;
        if (getCurrentPlayer().getPlayerBoard().isGraveYardEmpty() && getOpponentPlayer().getPlayerBoard().isGraveYardEmpty() || getCurrentPlayer().getPlayerBoard().isMonsterZoneFull()) {
            return PREPARATIONS_IS_NOT_DONE;
        }
        ArrayList<Card> opponentGraveyard = getOpponentPlayer().getPlayerBoard().returnGraveYard().getGraveYardCards();
        ArrayList<Card> opponentMonstersInGraveyard = (ArrayList<Card>) opponentGraveyard.stream().filter(card -> card.getCardType() == MONSTER).collect(Collectors.toList());
        ArrayList<Card> currentGraveYard = getCurrentPlayer().getPlayerBoard().returnGraveYard().getGraveYardCards();
        ArrayList<Card> currentMonstersInGraveyard = (ArrayList<Card>) currentGraveYard.stream().filter(card -> card.getCardType() == MONSTER).collect(Collectors.toList());
        int choice;
        if (currentMonstersInGraveyard.size() == 0 && opponentMonstersInGraveyard.size() == 0)
            return PREPARATIONS_IS_NOT_DONE;
        int addressOfAdd;
        if (selectedCellZone == Zone.HAND) {
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
            addressOfAdd = getCurrentPlayer().getPlayerBoard().addSpellOrTrapToBoard(selectedCell.getCardInCell(), CellStatus.OCCUPIED);
            view.showActivateEffectOfSpellFromHand(addressOfAdd, selectedCellAddress, selectedCell.getCardInCell().getName());
        } else {
            getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, selectedCellAddress).setCellStatus(CellStatus.OCCUPIED);
            addressOfAdd = selectedCellAddress;
            view.showActivateEffectOfSpellInZone();
        }

        if (currentMonstersInGraveyard.size() == 0)
            choice = view.twoChoiceQuestions("to summon card from, which you choose :", "", "opponent graveyard : " + opponentGraveyard.size());
        else if (opponentMonstersInGraveyard.size() == 0)
            choice = view.twoChoiceQuestions("to summon card from, which you choose :", "your graveyard : " + currentGraveYard.size(), "");
        else
            choice = view.twoChoiceQuestions("to summon card from, which you choose :", "your graveyard : " + currentGraveYard.size(), "opponent graveyard : " + opponentGraveyard.size());
        ArrayList<Card> monsters = null;
        if (choice == 2) {
            graveYard = opponentGraveyard;
            monsters = opponentMonstersInGraveyard;
        } else if (choice == 1) {
            graveYard = currentGraveYard;
            monsters = currentMonstersInGraveyard;
        }
        Card card;
        int address = view.chooseCardInGraveYard(monsters, graveYard);
        card = graveYard.get(address - 1);
        int summonChoice = view.twoChoiceQuestions("choose what to do:", "summon", "set");
        if (summonChoice == 1) {
            specialSummon(card, CellStatus.OFFENSIVE_OCCUPIED, Zone.GRAVEYARD, address,graveYard);
        } else {
            specialSummon(card, CellStatus.DEFENSIVE_HIDDEN, Zone.GRAVEYARD, address,graveYard);
        }
        graveYard.remove(address - 1);
        deselectCard(0);
        addCardToGraveYard(Zone.SPELL_ZONE, addressOfAdd, getCurrentPlayer());
        return SUCCESS;
    }

    private GameViewMessage terraFormingSpell() {
        Deck deck = getCurrentPlayer().getPlayDeck();
        ArrayList<Card> cards = deck.getMainCards();
        boolean flagOfExistence = false;
        for (Card card : cards) {
            if (card.getCardType() == CardType.SPELL) {
                if (((Spell) card).getSpellType() == SpellType.FIELD) {
                    flagOfExistence = true;
                }
            }
        }
        if (!flagOfExistence) {
            return PREPARATIONS_IS_NOT_DONE;
        }
        int addressOfAdd;
        if (selectedCellZone == Zone.HAND) {
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
            addressOfAdd = getCurrentPlayer().getPlayerBoard().addSpellOrTrapToBoard(selectedCell.getCardInCell(), CellStatus.OCCUPIED);
            view.showActivateEffectOfSpellFromHand(addressOfAdd, selectedCellAddress, selectedCell.getCardInCell().getName());
            System.out.println("showed!");
        } else {
            getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, selectedCellAddress).setCellStatus(CellStatus.OCCUPIED);
            addressOfAdd = selectedCellAddress;
            view.showActivateEffectOfSpellInZone();
        }
        view.showBoard();

        int address = view.chooseCardInDeck(deck);
        if (((Spell) deck.getMainCards().get(address - 1)).getSpellType().equals(SpellType.FIELD)) {
            addCardToFirstPlayerHand(deck.getMainCards().get(address - 1));
            deck.getMainCards().remove(address - 1);
        } else view.showError(Error.INVALID_SELECTION);

        deck.shuffleDeck();
        addCardToGraveYard(Zone.SPELL_ZONE, addressOfAdd, getCurrentPlayer());
        System.out.println("TerraForming effect");
        return SUCCESS;
    }

    private GameViewMessage potOfGreedSpell() {
        List<Card> deckCards = getCurrentPlayer().getPlayDeck().getMainCards();
        int size = deckCards.size();
        if (size < 2) {
            view.showError(Error.PREPARATIONS_IS_NOT_DONE);
            return PREPARATIONS_IS_NOT_DONE;
        }
        int addressOfAdd;
        if (selectedCellZone == Zone.HAND) {
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
            addressOfAdd = getCurrentPlayer().getPlayerBoard().addSpellOrTrapToBoard(selectedCell.getCardInCell(), CellStatus.OCCUPIED);
            view.showActivateEffectOfSpellFromHand(addressOfAdd, selectedCellAddress, selectedCell.getCardInCell().getName());

        } else {
            getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, selectedCellAddress).setCellStatus(CellStatus.OCCUPIED);
            addressOfAdd = selectedCellAddress;
            view.showActivateEffectOfSpellInZone();
        }
        if (!getCurrentPlayer().getNickname().equals("ai"))
            view.showPopUpMessageForEffect("Pot of Greed Effect", SPELL);
        view.showBoard();
        getCurrentPlayerHand().add(deckCards.get(size - 1));
        deckCards.remove(size - 1);
        size = deckCards.size();
        getCurrentPlayerHand().add(deckCards.get(size - 1));
        deckCards.remove(size - 1);
        view.updateDeckLabels();
        addCardToGraveYard(Zone.SPELL_ZONE, addressOfAdd, getCurrentPlayer());
        System.out.println("pot of greed effect");
        deselectCard(0);

        return SUCCESS;
    }

    private GameViewMessage raigekiSpell() {
        MonsterZone monsterZone = getOpponentPlayer().getPlayerBoard().returnMonsterZone();

        int addressOfAdd;
        if (selectedCellZone == Zone.HAND) {
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
            addressOfAdd = getCurrentPlayer().getPlayerBoard().addSpellOrTrapToBoard(selectedCell.getCardInCell(), CellStatus.OCCUPIED);
            view.showActivateEffectOfSpellFromHand(addressOfAdd, selectedCellAddress, selectedCell.getCardInCell().getName());
        } else {
            getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, selectedCellAddress).setCellStatus(CellStatus.OCCUPIED);
            addressOfAdd = selectedCellAddress;
            view.showActivateEffectOfSpellInZone();
        }
        view.showBoard();
        int i = 1;
        int counter = 1;
        while (counter <= 5) {
            if (monsterZone.getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY)
                addCardToGraveYard(Zone.MONSTER_ZONE, i, getOpponentPlayer());
            i++;
            counter++;
        }
        if (!getCurrentPlayer().getNickname().equals("ai"))
            view.showPopUpMessageForEffect("Raigeki Spell effect!", SPELL);
        addCardToGraveYard(Zone.SPELL_ZONE, addressOfAdd, getCurrentPlayer());
        deselectCard(0);

        return SUCCESS;
    }

    private GameViewMessage harpiesFeatherDusterSpell() {
        int i = 1;
        int addressOfAdd;
        if (selectedCellZone == Zone.HAND) {
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
            addressOfAdd = getCurrentPlayer().getPlayerBoard().addSpellOrTrapToBoard(selectedCell.getCardInCell(), CellStatus.OCCUPIED);
            view.showActivateEffectOfSpellFromHand(addressOfAdd, selectedCellAddress, selectedCell.getCardInCell().getName());
        } else {
            getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, selectedCellAddress).setCellStatus(CellStatus.OCCUPIED);
            addressOfAdd = selectedCellAddress;
            view.showActivateEffectOfSpellInZone();
        }
        view.showBoard();
        while (getOpponentPlayer().getPlayerBoard().returnSpellZone().getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY) {
            addCardToGraveYard(Zone.SPELL_ZONE, i, getOpponentPlayer());
            i++;
        }
        if (isFieldActivated == 2 && turn == 1) {
            reversePreviousFieldZoneSpellEffectAndRemoveIt();
        } else if (isFieldActivated == 1 && turn == 2) {
            reversePreviousFieldZoneSpellEffectAndRemoveIt();
        }
        addCardToGraveYard(Zone.SPELL_ZONE, addressOfAdd, getCurrentPlayer());
        if (!getCurrentPlayer().getNickname().equals("ai"))
            view.showPopUpMessageForEffect("Harpie’s Feather Duster Effect", SPELL);
        deselectCard(0);

        return SUCCESS;
    }

    public GameViewMessage darkHoleSpell() {
        int i = 1;
        int addressOfAdd;
        if (selectedCellZone == Zone.HAND) {
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
            addressOfAdd = getCurrentPlayer().getPlayerBoard().addSpellOrTrapToBoard(selectedCell.getCardInCell(), CellStatus.OCCUPIED);
            view.showActivateEffectOfSpellFromHand(addressOfAdd, selectedCellAddress, selectedCell.getCardInCell().getName());

        } else {
            getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, selectedCellAddress).setCellStatus(CellStatus.OCCUPIED);
            addressOfAdd = selectedCellAddress;
            view.showActivateEffectOfSpellInZone();
        }
        view.showBoard();
        while (getOpponentPlayer().getPlayerBoard().returnMonsterZone().getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY || i >= 5) {
            addCardToGraveYard(Zone.MONSTER_ZONE, i, getOpponentPlayer());
            i++;
        }
        i = 1;
        while (getCurrentPlayer().getPlayerBoard().returnMonsterZone().getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY || i >= 5) {
            addCardToGraveYard(Zone.MONSTER_ZONE, i, getCurrentPlayer());
            i++;
        }
        addCardToGraveYard(Zone.SPELL_ZONE, addressOfAdd, getCurrentPlayer());
        if (!getCurrentPlayer().getNickname().equals("ai"))
            view.showPopUpMessageForEffect("Dark Hole Spell Effect", SPELL);
        deselectCard(0);

        return SUCCESS;
    }

    private GameViewMessage swordOfDarkDestructionSpell() {
        if (getCurrentPlayer().getPlayerBoard().isMonsterZoneEmpty() || !isFiendOrSpellCasterOnMap()) {
            view.showError(Error.PREPARATIONS_IS_NOT_DONE);
            return PREPARATIONS_IS_NOT_DONE;
        }
        int addressOfAdd;
        if (selectedCellZone == Zone.HAND) {
            addressOfAdd = getCurrentPlayer().getPlayerBoard().addSpellOrTrapToBoard(selectedCell.getCardInCell(), CellStatus.OCCUPIED);
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
            view.showActivateEffectOfSpellFromHand(addressOfAdd, selectedCellAddress, selectedCell.getCardInCell().getName());
        } else {
            getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, selectedCellAddress).setCellStatus(CellStatus.OCCUPIED);
            view.showActivateEffectOfSpellInZone();
        }
        Card spellCard = selectedCell.getCardInCell();
        Monster monsterCard;
        int address;

        address = view.swordOfDarkDestruction();
        Cell cell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, address);
        monsterCard = (Monster) cell.getCardInCell();
        monsterCard.changeAttackPower(400);
        monsterCard.changeDefensePower(-200);
        if (turn == 1) {
            firstPlayerHashmapForEquipSpells.put(spellCard, monsterCard);
        } else secondPlayerHashmapForEquipSpells.put(spellCard, monsterCard);
        return NONE;
    }

    private boolean isFiendOrSpellCasterOnMap() {
        for (int i = 1; i < 5; i++) {
            Cell cell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i);
            if (cell.getCellStatus() != CellStatus.EMPTY) {
                if (((Monster) cell.getCardInCell()).getMonsterType() == MonsterType.FIEND || ((Monster) cell.getCardInCell()).getMonsterType() == MonsterType.SPELLCASTER)
                    return true;
            }
        }
        return false;
    }

    public void removeSwordOfDarkDestruction(Card card) {
        Monster monster = (Monster) card;
        monster.changeAttackPower(-400);
        monster.changeDefensePower(+200);
    }

    public GameViewMessage blackPendantSpell() {
        if (getCurrentPlayer().getPlayerBoard().isMonsterZoneEmpty()) {
            view.showError(Error.PREPARATIONS_IS_NOT_DONE);
            return PREPARATIONS_IS_NOT_DONE;
        }
        int addressOfAdd;
        if (selectedCellZone == Zone.HAND) {
            addressOfAdd = getCurrentPlayer().getPlayerBoard().addSpellOrTrapToBoard(selectedCell.getCardInCell(), CellStatus.OCCUPIED);
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
            view.showActivateEffectOfSpellFromHand(addressOfAdd, selectedCellAddress, selectedCell.getCardInCell().getName());
        } else {
            getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, selectedCellAddress).setCellStatus(CellStatus.OCCUPIED);
            view.showActivateEffectOfSpellInZone();
        }
        Card spellCard = selectedCell.getCardInCell();
        Monster monsterCard;
        int address;
        address = view.blackPendant();
        Cell cell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, address);
        monsterCard = (Monster) cell.getCardInCell();
        monsterCard.changeAttackPower(500);
        if (turn == 1) {
            firstPlayerHashmapForEquipSpells.put(spellCard, monsterCard);
        } else secondPlayerHashmapForEquipSpells.put(spellCard, monsterCard);
        return SUCCESS;
    }


    public void removeBlackPendant(Card card) {
        Monster monster = (Monster) card;
        monster.setAttackPower(monster.getAttackPower() - 500);
    }

    public void surrender() {
        GameResult result;
        DuelPlayer winner;
        if (getCurrentPlayer() == firstPlayer) {
            result = duelGameController.checkGameResult(secondPlayer, firstPlayer, GameResultToCheck.SURRENDER);
            winner = secondPlayer;
        } else {
            result = duelGameController.checkGameResult(firstPlayer, secondPlayer, GameResultToCheck.SURRENDER);
            winner = firstPlayer;
        }
        if (result == GameResult.GAME_FINISHED) {
            finishGame(winner);
        } else if (result == GameResult.ROUND_FINISHED) {
            finishRound(winner);
        }
    }

    public void specialSummon(Card card, CellStatus cellStatus, Zone fromZone, int addressInFromZone,ArrayList<Card> fromZoneCards) {
        MonsterZone monsterZone = getCurrentPlayer().getPlayerBoard().returnMonsterZone();
        int addressOfAdd = monsterZone.addCard((Monster) card, cellStatus);
        view.showBoard();
        if (fromZone == Zone.HAND) {
            fromZoneCards.remove(addressInFromZone-1);
            if (cellStatus == CellStatus.OFFENSIVE_OCCUPIED)
                view.playAnimation(Animation.SUMMON_MONSTER, card.getName(), addressOfAdd, addressInFromZone, 0, true);
            else
                view.playAnimation(Animation.SET_MONSTER, card.getName(), addressOfAdd, addressInFromZone, 0, true);
        } else if (fromZone == Zone.GRAVEYARD) {
            fromZoneCards.remove(addressInFromZone-1);
            view.reloadCurrentAndOpponentMonsterZone();
        }
        checkNewCardToBeBeUnderEffectOfFieldCard((Monster) card);
        if (isCurrentPlayerTrapToBeActivatedInSummonSituation()) {
            if (isTrapOfCurrentPlayerInSummonSituationActivated()) {
                return;
            }
        }
        if (isOpponentTrapToBeActivatedInSummonSituation(addressOfAdd)) {
            if (isOpponentTrapInSummonSituationActivated(addressOfAdd)) {
                view.endOfMiddleGameTurnChange();
                return;
            }
        }
    }

    private List<Card> getOpponentHand() {
        if (turn == 1) {
            return secondPlayerHand;
        }
        return firstPlayerHand;
    }

    private void addCardToGraveYard(Zone fromZone, int address, DuelPlayer player) {
        if (fromZone.equals(Zone.MONSTER_ZONE)) {
            Cell cell = player.getPlayerBoard().getACellOfBoardWithAddress(fromZone, address);
            if (cell.getCellStatus().equals(CellStatus.EMPTY))
                return;
            String cardname = cell.getCardInCell().getName();
            checkDeathOfUnderFieldEffectCard((Monster) cell.getCardInCell());
            MonsterZone monsterZone = player.getPlayerBoard().returnMonsterZone();
            boolean isCurrent = (player.getNickname().equals(getCurrentPlayer().getNickname()));
            if (player == firstPlayer) {
                removeEquipSpell(address, monsterZone, firstPlayerHashmapForEquipSpells, firstPlayer);
            } else {
                removeEquipSpell(address, monsterZone, secondPlayerHashmapForEquipSpells, secondPlayer);
            }
            if (((Monster) cell.getCardInCell()).getMonsterEffect() == MonsterEffect.BEAST_KING_BARBAROS_EFFECT) {
                Monster monster = ((Monster) cell.getCardInCell());
                monster.setAttackPower(3000);
            }
            player.getPlayerBoard().removeMonsterFromBoardAndAddToGraveYard(address);
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.MONSTER_ZONE_TO_GRAVEYARD, cardname, address, 0, 0, isCurrent);
            //view.showBoard();
        } else if (fromZone.equals(Zone.FIELD_ZONE)) {
            player.getPlayerBoard().removeFieldSpell();
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSpellToGraveYard();
            view.showBoard();
        } else if (fromZone.equals(Zone.SPELL_ZONE)) {
            Cell cell = player.getPlayerBoard().getACellOfBoardWithAddress(fromZone, address);

            if (cell.getCellStatus().equals(CellStatus.EMPTY)) {
                System.out.println("reached" + " empty");
                return;
            }
            String cardName = cell.getCardInCell().getName();
            player.getPlayerBoard().removeSpellOrTrapFromBoard(address);
            boolean isCurr = player == getCurrentPlayer();
            System.out.println("doesnt reach here!");
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.SPELL_TO_GRAVEYARD, cardName, address, 0, 0, isCurr);
            view.showBoard();
        } else if (fromZone.equals(Zone.HAND)) {
            if (player.getNickname().equals(getCurrentPlayer().getNickname())) {
                Card card = getCardInHand(address - 1);
                String cardName = card.getName();
                player.getPlayerBoard().addCardToGraveYardDirectly(card);
                getCurrentPlayerHand().remove(address - 1);
                if (!getCurrentPlayer().getNickname().equals("ai"))
                    view.playAnimation(Animation.HAND_TO_GRAVEYARD, cardName, address, address, 0, true);
                // view.showAddToGraveYardFromHandAnimation(address, true, caradName);
                view.showBoard();
            } else {
                Card card = getCardInOpponentHand(address - 1);
                if (card == null)
                    return;
                String cardName = card.getName();
                player.getPlayerBoard().addCardToGraveYardDirectly(card);
                getOpponentHand().remove(address - 1);
                if (!getCurrentPlayer().getNickname().equals("ai"))
                    view.playAnimation(Animation.HAND_TO_GRAVEYARD, cardName, address, address, 0, false);

                //view.showAddToGraveYardFromHandAnimation(address, false, cardName);
                view.showBoard();
            }
        }
    }

    private void removeEquipSpell(int address, MonsterZone monsterZone, HashMap<Card, Monster> map, DuelPlayer
            player) {
        for (Card card : map.keySet()) {
            if (monsterZone.getCellWithAddress(address).getCardInCell() == map.get(card)) {
                SpellZone spellZone = player.getPlayerBoard().returnSpellZone();
                int i = 1;
                while (i <= 5) {
                    Spell spell;
                    if (spellZone.getCellWithAddress(i).getCellStatus() != CellStatus.EMPTY) {
                        if ((spell = (Spell) spellZone.getCellWithAddress(i).getCardInCell()) == card) {
                            if (spell.getSpellEffect() == SpellEffect.BLACK_PENDANT_EFFECT) {
                                removeBlackPendant(map.get(card));
                            } else {
                                removeSwordOfDarkDestruction(map.get(card));
                            }

                            player.getPlayerBoard().removeSpellOrTrapFromBoard(i);
                            view.reloadCurrentSpellZone();
                            view.reloadOpponentSpellZone();
                        }
                    }
                    i++;
                }
            }
        }
    }

    private Card getCardInHand(int address) {
        Card card;
        if (address > getCurrentPlayerHand().size())
            return null;
        if ((card = getCurrentPlayerHand().get(address)) == null) {
            return null;
        } else return card;
    }

    private Card getCardInOpponentHand(int address) {
        Card card;
        if (address > getOpponentPlayerHand().size())
            return null;
        if ((card = getCurrentPlayerHand().get(address)) == null) {
            return null;
        } else return card;
    }

    //MONSTER RELATED CODES :
    public synchronized GameViewMessage summonMonster() {
        GameViewMessage message;

        if ((message = isValidSelectionForSummonOrSet()) != SUCCESS) {
            return message;
        }
        if ((message = isValidSummon()) != GameViewMessage.SUCCESS) {
            return message;
        }
        Monster monster = ((Monster) selectedCell.getCardInCell());
        System.out.println(monster);
        if (monster.getMonsterEffect().equals(MonsterEffect.GATE_GUARDIAN_EFFECT)) {
            return gateGuardianEffect(CellStatus.OFFENSIVE_OCCUPIED);
        } else if (monster.getMonsterEffect().equals(MonsterEffect.BEAST_KING_BARBAROS_EFFECT)) {
            if (beastKingBarbosEffect(CellStatus.OFFENSIVE_OCCUPIED))
                return SUCCESS;
        } else if (monster.getMonsterEffect().equals(MonsterEffect.THE_TRICKY_EFFECT)) {
            if (theTrickyEffect(CellStatus.OFFENSIVE_OCCUPIED)) {
                return SUCCESS;
            }
        }
        // check haven't summoned more than once
        if (!isSummonOrSetUsed()) {
            System.out.println(selectedCell.getCardInCell() + "cant summon cause used!");
            return GameViewMessage.USED_SUMMON_OR_SET;
        }
        if (monster.getMonsterActionType().equals(MonsterActionType.RITUAL)) {
            view.showError(Error.CAN_NOT_RITUAL_SUMMON);
            return GameViewMessage.MUST_ACTIVE_RITUAL_SPELL_TO_RITUAL_SUMMON;
        }
        if (monster.getLevel() > 4 && monster.getLevel() <= 10) {
            message = tributeSummon();
            return message;
        }
        return normalSummon();
    }

    private GameViewMessage isValidSummon() {

        if (!currentPhase.equals(Phase.MAIN_PHASE_1) && !currentPhase.equals(Phase.MAIN_PHASE_2)) {
            Error.showError(Error.ACTION_NOT_ALLOWED);
            return GameViewMessage.ACTION_NOT_ALLOWED;
        }
        return GameViewMessage.SUCCESS;
    }

    private boolean isSummonOrSetUsed() {
        if (isSummonOrSetOfMonsterUsed) {
            Error.showError(Error.ALREADY_SUMMONED_OR_SET);
            return false;
        }
        return true;
    }

    private GameViewMessage gateGuardianEffect(CellStatus status) {
        if (view.yesNoQuestion("do you want to tribute for GateGuardian Special Summon?")) {
            if (didTribute(3, getCurrentPlayer())) {
                ArrayList<Card> hand = (ArrayList<Card>) getCurrentPlayerHand();
                specialSummon(selectedCell.getCardInCell(), status, Zone.HAND, selectedCellAddress,(ArrayList<Card>) getCurrentPlayerHand());
            } else return NOT_ENOUGH_CARD_TO_TRIBUTE;
        }
        return SUCCESS;
    }

    private boolean beastKingBarbosEffect(CellStatus status) {
        int howToSummon = view.howToSummonBeastKingBarbos();//1-normal tribute, 2-without tribute, 3-with 3 tributes\n"
        if (howToSummon == -1) {
            return true;
        }
        if (howToSummon == 1) {
            return false;
        } else if (howToSummon == 2) {
            ((Monster) selectedCell.getCardInCell()).changeAttackPower(-1900);
            if (status.equals(CellStatus.OFFENSIVE_OCCUPIED)) {
                summon();
            } else set();
            return true;
        } else {
            if (didTribute(3, getCurrentPlayer())) {
                beastKingBarbosSpecialSummonEffect(status);
                return true;
            }
        }
        return false;
    }

    private void beastKingBarbosSpecialSummonEffect(CellStatus status) {
        for (int i = 1; i <= 5; i++) {
            addCardToGraveYard(Zone.MONSTER_ZONE, i, getOpponentPlayer());
        }
        for (int i = 1; i <= 5; i++) {
            addCardToGraveYard(Zone.SPELL_ZONE, i, getOpponentPlayer());
        }
        if (fieldZoneSpell != null) {
            reversePreviousFieldZoneSpellEffectAndRemoveIt();
        }
        specialSummon(selectedCell.getCardInCell(), status, Zone.HAND, selectedCellAddress,(ArrayList<Card>) getCurrentPlayerHand());
        deselectCard(0);
    }

    private boolean theTrickyEffect(CellStatus status) {
        if (getCurrentPlayerHand().size() == 1)
            return false;
        if (view.yesNoQuestion("do you want to special summon/set it with a tribute in hand ?")) {
            int address = view.chooseCardInHand(selectedCellAddress);
            addCardToGraveYard(Zone.HAND, address, getCurrentPlayer());
            ArrayList<Card> hand = (ArrayList<Card>) getCurrentPlayerHand();
            for (int i = 1; i <= hand.size(); i++) {
                if (hand.get(i - 1) == selectedCell.getCardInCell()) {
                    selectedCellAddress = i;
                    break;
                }
            }
            specialSummon(selectedCell.getCardInCell(), status, Zone.HAND, selectedCellAddress,(ArrayList<Card>) getCurrentPlayerHand());
            deselectCard(0);
            return true;
        }
        return false;
    }

    private boolean isCurrentPlayerTrapToBeActivatedInSummonSituation() {
        PlayerBoard board = getCurrentPlayer().getPlayerBoard();
        for (int i = 1; i <= 5; i++) {
            Cell cell = board.getACellOfBoardWithAddress(Zone.SPELL_ZONE, i);
            if (cell.getCellStatus().equals(CellStatus.EMPTY)) {
                continue;
            } else if (cell.getCardInCell().getCardType().equals(CardType.SPELL)) {
                continue;
            }
            Trap trap = (Trap) cell.getCardInCell();
            if (trap.getTrapEffect().equals(TrapEffect.TORRENTIAL_TRIBUTE_EFFECT)) {
                return view.yesNoQuestion("do you want to activate your trap and spell?");
            }
        }
        return false;
    }

    private boolean isOpponentTrapToBeActivatedInSummonSituation(int addressOfSummonedCard) {
        PlayerBoard board = getOpponentPlayer().getPlayerBoard();
        for (int i = 1; i <= 5; i++) {
            Cell cell = board.getACellOfBoardWithAddress(Zone.SPELL_ZONE, i);
            if (cell.getCellStatus().equals(CellStatus.EMPTY)) {
                continue;
            } else if (cell.getCardInCell().getCardType().equals(CardType.SPELL)) {
                continue;
            }
            Trap trap = (Trap) cell.getCardInCell();
            if (trap.getTrapEffect() == null)
                continue;
            switch (trap.getTrapEffect()) {
                case TORRENTIAL_TRIBUTE_EFFECT:
                    view.middleGameTurnChange();
                    if (view.yesNoQuestion("do you want to activate your trap and spell?")) {
                        return true;
                    } else {
                        view.endOfMiddleGameTurnChange();
                        return false;
                    }
                case TRAP_HOLE_EFFECT:
                    if (isValidSituationForTrapHoleTrapEffect(getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, addressOfSummonedCard))) {
                        view.middleGameTurnChange();
                        if (view.yesNoQuestion("do you want to activate your trap and spell?")) {
                            return true;
                        } else {
                            view.endOfMiddleGameTurnChange();
                            return false;
                        }
                    }
            }
        }
        return false;
    }

    private boolean isValidSituationForTrapHoleTrapEffect(Cell cell) {
        Monster monster = (Monster) cell.getCardInCell();
        return monster.getAttackPower() > 1000;
    }

    private boolean isOpponentTrapInSummonSituationActivated(int addressOfNewSummonedCard) {
        int address = view.getAddressForTrapOrSpell(getOpponentPlayer().getPlayerBoard().returnSpellZone(), "Summon opponent");
        if (address == -1) {
            return false;
        } else {
            Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, address);
            if (checkTrapCellToBeActivatedForOpponentInSummonSituation(address, cell, addressOfNewSummonedCard)) {
                if (!getCurrentPlayer().getNickname().equals("ai"))
                    view.showPopUpMessageForEffect("Trap effect!", TRAP);
                return true;
            }
        }
        return false;
    }

    private boolean checkTrapCellToBeActivatedForOpponentInSummonSituation(int address, Cell
            selectedCellByPlayerToActivate, int addressOfNewSummonedCard) {
        if (selectedCellByPlayerToActivate.getCardInCell().getCardType().equals(CardType.SPELL)) {
            Error.showError(Error.ACTION_NOT_ALLOWED); //right error ?
        } else {
            Trap trap = (Trap) selectedCellByPlayerToActivate.getCardInCell();
            if (isValidActivateTrapEffectInSummonSituationForOpponentToDo(trap.getTrapEffect(), selectedCellByPlayerToActivate, addressOfNewSummonedCard)) {
                addCardToGraveYard(Zone.SPELL_ZONE, address, getOpponentPlayer()); //CHECK correctly removed ?
                return true;
            }
        }
        return false;
    }

    private boolean isTrapOfCurrentPlayerInSummonSituationActivated() {

        int address = view.getAddressForTrapOrSpell(getCurrentPlayer().getPlayerBoard().returnSpellZone(), "Summon current");
        if (address == -1) {
            return false;
        } else {
            Cell cell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, address);
            if (checkTrapCellToBeActivatedForCurrentPlayerInSummonSituation(address, cell)) {
                if (!getCurrentPlayer().getNickname().equals("ai"))
                    view.showPopUpMessageForEffect("Trap effect!", TRAP);
                return true;
            }

        }
        return false;
    }

    private boolean checkTrapCellToBeActivatedForCurrentPlayerInSummonSituation(int address, Cell cell) {
        if (cell.getCardInCell().getCardType().equals(CardType.SPELL)) {
            Error.showError(Error.ACTION_NOT_ALLOWED); //right error ?
        } else {
            Trap trap = (Trap) cell.getCardInCell();
            if (isValidActivateTrapEffectInSummonSituationForCurentPlayerToDo(trap.getTrapEffect(), cell)) {

                addCardToGraveYard(Zone.SPELL_ZONE, address, getCurrentPlayer()); //CHECK correctly removed ?
                return true;
            }
        }
        return false;
    }

    private boolean isValidActivateTrapEffectInSummonSituationForOpponentToDo(TrapEffect trapEffect, Cell cell,
                                                                              int addressOfNewSummonedCard) {
        switch (trapEffect) {
            case TORRENTIAL_TRIBUTE_EFFECT:
                cell.setCellStatus(CellStatus.OCCUPIED);
                view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
                view.showBoard();
                torrentialTributeTrapEffect();
                return true;
            case TRAP_HOLE_EFFECT:
                cell.setCellStatus(CellStatus.OCCUPIED);
                view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
                view.showBoard();
                trapHoleTrapEffect(addressOfNewSummonedCard);
                return true;
            default:
                Error.showError(Error.PREPARATIONS_IS_NOT_DONE);
                return false;
        }
    }

    private boolean isValidActivateTrapEffectInSummonSituationForCurentPlayerToDo(TrapEffect trapEffect, Cell cell) {
        if (trapEffect == TrapEffect.TORRENTIAL_TRIBUTE_EFFECT) {
            cell.setCellStatus(CellStatus.OCCUPIED);
            view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
            view.showBoard();
            torrentialTributeTrapEffect();
            return true;
        }
        Error.showError(Error.PREPARATIONS_IS_NOT_DONE);
        return false;
    }

    private GameViewMessage normalSummon() {
        GameViewMessage message;
        isSummonOrSetOfMonsterUsed = true;
        message = summon();
        deselectCard(0);
        return message;
    }

    private GameViewMessage summon() {
        String cardName = selectedCell.getCardInCell().getName();
        int addressOfAdd = getCurrentPlayer().getPlayerBoard().addMonsterToBoard((Monster) selectedCell.getCardInCell(), CellStatus.OFFENSIVE_OCCUPIED);
        //view.showSuccessMessage(SuccessMessage.SUMMONED_SUCCESSFULLY);
        System.out.println(getCurrentPlayer());
        getCurrentPlayerHand().remove(selectedCellAddress - 1);
        if (!getCurrentPlayer().getNickname().equals("ai"))
            view.playAnimation(Animation.SUMMON_MONSTER, cardName, addressOfAdd, selectedCellAddress, 0, true);
        view.showBoard();
        if (isCurrentPlayerTrapToBeActivatedInSummonSituation()) {
            if (isTrapOfCurrentPlayerInSummonSituationActivated()) {
                return SUCCESS;
            }
        }
        if (isOpponentTrapToBeActivatedInSummonSituation(addressOfAdd)) {
            if (isOpponentTrapInSummonSituationActivated(addressOfAdd)) {
                view.endOfMiddleGameTurnChange();
                return SUCCESS;
            }
            view.endOfMiddleGameTurnChange();
        }
        checkNewCardToBeBeUnderEffectOfFieldCard((Monster) selectedCell.getCardInCell());
        return SUCCESS;
    }

    private GameViewMessage tributeSummon() {
        if (((Monster) selectedCell.getCardInCell()).getLevel() >= 7) {
            if (view.yesNoQuestion("You must tribute summon, are you sure ?")) {
                if (didTribute(2, getCurrentPlayer())) {
                    return normalSummon();
                }
            } else return NONE;
        } else if (((Monster) selectedCell.getCardInCell()).getLevel() >= 5) {
            if (view.yesNoQuestion("You must tribute summon, are you sure ?")) {
                if (didTribute(1, getCurrentPlayer())) {
                    return normalSummon();
                }
            } else return NONE;
        }
        return GameViewMessage.NEED_MORE_TRIBUTE;
    }

    private boolean didTribute(int number, DuelPlayer player) {
        if (!isThereEnoughMonsterToTribute(number, player)) {
            Error.showError(Error.NOT_ENOUGH_CARDS_TO_TRIBUTE);
            return false;
        }

        ArrayList<Integer> tributeAddress = view.getTributeAddress(number);
        for (Integer integer : tributeAddress) {
            addCardToGraveYard(Zone.MONSTER_ZONE, integer, player);
        }
        return true;
    }

    private boolean isThereEnoughMonsterToTribute(int number, DuelPlayer player) {
        int counter = 0;
        for (int i = 1; i <= 5; i++) {
            if (!player.getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i).getCellStatus().equals(CellStatus.EMPTY)) {
                counter++;
            }
            if (i == 5) {
                if (counter >= number) {
                    break;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private GameViewMessage activateRitualSpell() {
        int ritualMonsterAddressInHand;
        Monster monster;
        if (!isRitualCardInHand()) {
            return GameViewMessage.CAN_NOT_RITUAL_SUMMON;
        }
        if (selectedCellZone == Zone.HAND)
            ritualMonsterAddressInHand = view.getRitualMonsterAddress(selectedCellAddress);
        else
            ritualMonsterAddressInHand = view.getRitualMonsterAddress(-2);
        if (ritualMonsterAddressInHand == -1) {
            return NONE;
        }
        monster = (Monster) getCurrentPlayerHand().get(ritualMonsterAddressInHand - 1);
        System.out.println("ritual address : " + ritualMonsterAddressInHand + " monster : " + monster);
        if (!sumOfSubsequences(monster)) {
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showPopUpMessageForEffect("Can't ritual summon this card", MONSTER);
            return NONE;
        } else {
            if (areCardsLevelsEnoughToSummonRitualMonster(monster)) {
                int addAddressOfSpell;
                if (selectedCellZone == Zone.HAND) {
                    addAddressOfSpell = getCurrentPlayer().getPlayerBoard().addSpellOrTrapToBoard(selectedCell.getCardInCell(), CellStatus.OCCUPIED);
                    getCurrentPlayerHand().remove(selectedCellAddress - 1);
                    view.showActivateEffectOfSpellFromHand(addAddressOfSpell, selectedCellAddress, selectedCell.getCardInCell().getName());
                } else {
                    getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, selectedCellAddress).setCellStatus(CellStatus.OCCUPIED);
                    addAddressOfSpell = selectedCellAddress;
                    view.showActivateEffectOfSpellInZone();
                }
                view.showBoard();
                int choice = view.twoChoiceQuestions("Choose monster position :", "Summon", "Set");
                CellStatus status = choice == 1 ? CellStatus.OFFENSIVE_OCCUPIED : CellStatus.DEFENSIVE_HIDDEN;
                int addressOfAdd = getCurrentPlayer().getPlayerBoard().addMonsterToBoard(monster, status);
                System.out.println(getOpponentPlayer());
                for (int i = 0; i < getCurrentPlayerHand().size(); i++) {
                    if (getCurrentPlayerHand().get(i) == monster) {
                        ritualMonsterAddressInHand = i + 1;
                        break;
                    }
                }
                getCurrentPlayerHand().remove(ritualMonsterAddressInHand - 1);
                if (status == CellStatus.OFFENSIVE_OCCUPIED)
                    view.playAnimation(Animation.SUMMON_MONSTER, monster.getName(), addressOfAdd, ritualMonsterAddressInHand, 0, true);
                else
                    view.playAnimation(Animation.SET_MONSTER, monster.getName(), addressOfAdd, ritualMonsterAddressInHand, 0, true);
                view.showBoard();
                addCardToGraveYard(Zone.SPELL_ZONE, addAddressOfSpell, getCurrentPlayer());
                view.showBoard();
                if (isCurrentPlayerTrapToBeActivatedInSummonSituation()) {
                    if (isTrapOfCurrentPlayerInSummonSituationActivated()) {
                        return SUCCESS;
                    }
                }
                if (isOpponentTrapToBeActivatedInSummonSituation(addressOfAdd)) {
                    if (isOpponentTrapInSummonSituationActivated(addressOfAdd)) {
                        view.endOfMiddleGameTurnChange();
                        return SUCCESS;
                    }
                }
            }
            return SUCCESS;
        }

    }

    public boolean areCardsLevelsEnoughToSummonRitualMonster(Monster ritualMonster) {
        ArrayList<Integer> addresses = view.getMonstersAddressesToBringRitual(ritualMonster.getLevel());
        for (Integer address : addresses) {
            addCardToGraveYard(Zone.MONSTER_ZONE, address, getCurrentPlayer());
        }
        return true;
    }


    private boolean isRitualCardInHand() {
        List<Card> currentPlayerHand = getCurrentPlayerHand();
        for (Card card : currentPlayerHand) {
            if (card.getCardType() == CardType.MONSTER) {
                if (((Monster) card).getMonsterActionType() == MonsterActionType.RITUAL) return true;
            }
        }
        return false;
    }

    public boolean sumOfSubsequences(Monster monster) {
        MonsterZone monsterZone = getCurrentPlayer().getPlayerBoard().returnMonsterZone();
        int[] levels = new int[5];
        for (int i = 1; i <= 5; i++) {
            Cell cell = monsterZone.getCellWithAddress(i);
            if (cell.getCellStatus() != CellStatus.EMPTY) {
                Monster cardNumberI = (Monster) cell.getCardInCell();
                levels[i] = cardNumberI.getLevel();
            }
        }
        ArrayList<Integer> sumOfSubset = new ArrayList<>();
        sumOfSubset = subsetSums(levels, 0, levels.length - 1, 0, sumOfSubset);
        for (Integer level : sumOfSubset) {
            if (level == Objects.requireNonNull(monster).getLevel()) return true;
        }
        return false;
    }

    public ArrayList<Integer> subsetSums(int[] arr, int l, int r, int sum, ArrayList<Integer> sumOfSubsets) {
        if (l > r) {
            sumOfSubsets.add(sum);
            return sumOfSubsets;
        }
        subsetSums(arr, l + 1, r, sum + arr[l], sumOfSubsets);
        subsetSums(arr, l + 1, r, sum, sumOfSubsets);
        return sumOfSubsets;
    }

    public GameViewMessage setCard() {
        if (selectedCellZone == Zone.NONE || selectedCell == null) {

            return SELECT_CARD;
        }
        if (selectedCellZone != Zone.HAND)
            return GameViewMessage.NONE;
        if (selectedCell.getCardInCell().getCardType() == MONSTER) {
            return setMonster();
        } else {
            return setSpellOrTrap();
        }
    }

    public GameViewMessage setMonster() {
        GameViewMessage message;
        if ((message = isValidSelectionForSummonOrSet()) != SUCCESS) {
            return message;
        }
        if (!(currentPhase.equals(Phase.MAIN_PHASE_1) || currentPhase.equals(Phase.MAIN_PHASE_2))) {
            message = ACTION_NOT_ALLOWED;
            return message;
        }
        //check special Set
        Monster monster = ((Monster) selectedCell.getCardInCell());
        if (monster.getMonsterEffect().equals(MonsterEffect.GATE_GUARDIAN_EFFECT)) {
            message = gateGuardianEffect(CellStatus.DEFENSIVE_HIDDEN);
            return message;
        } else if (monster.getMonsterEffect().equals(MonsterEffect.BEAST_KING_BARBAROS_EFFECT)) {
            if (beastKingBarbosEffect(CellStatus.DEFENSIVE_HIDDEN))
                return SUCCESS;
        } else if (monster.getMonsterEffect().equals(MonsterEffect.THE_TRICKY_EFFECT)) {
            if (theTrickyEffect(CellStatus.DEFENSIVE_HIDDEN)) {
                return SUCCESS;
            }
        }
        if (!isSummonOrSetUsed()) {
            return USED_SUMMON_OR_SET;
        }
//       TODO if (monster.getMonsterActionType().equals(MonsterActionType.RITUAL)) {
//            view.showError(Error.CAN_NOT_RITUAL_SUMMON);
//            return;
//        }
        if (monster.getLevel() >= 5 && monster.getLevel() <= 10) {
            return tributeSet();

        }
        return normalSet();
    }

    private boolean isTorrentialTributeTrapToActivateInSet(DuelPlayer player) {
        for (int i = 1; i <= 5; i++) {
            Cell cell = player.getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, i);
            if (cell.getCellStatus().equals(CellStatus.EMPTY))
                continue;
            if (cell.getCardInCell().getCardType().equals(CardType.SPELL))
                continue;
            if (cell.getCardInCell().getCardType().equals(TRAP)) {
                if (((Trap) cell.getCardInCell()).getTrapEffect().equals(TrapEffect.TORRENTIAL_TRIBUTE_EFFECT)) {
                    return true;
                }
            }
        }
        return false;
    }

    private GameViewMessage normalSet() {
        isSummonOrSetOfMonsterUsed = true;
        set();
        deselectCard(0);
        return SUCCESS;
    }

    private void set() {
        String cardName = selectedCell.getCardInCell().getName();
        int addressOfAddToZone = getCurrentPlayer().getPlayerBoard().addMonsterToBoard((Monster) selectedCell.getCardInCell(), CellStatus.DEFENSIVE_HIDDEN);
        getCurrentPlayerHand().remove(selectedCellAddress - 1);
        //view.showSetMonsterTransition(addressOfAddToZone, selectedCellAddress, cardName);
        view.playAnimation(Animation.SET_MONSTER, cardName, addressOfAddToZone, selectedCellAddress, 0, true);

        //TRAPS :
        if (isTorrentialTributeTrapToActivateInSet(getCurrentPlayer())) {
            if (isTrapOfCurrentPlayerInSummonSituationActivated()) {
                if (isTrapOfCurrentPlayerInSummonSituationActivated()) {
                    deselectCard(0);
                    return;
                }
            }
        } else if (isTorrentialTributeTrapToActivateInSet(getOpponentPlayer())) {
            view.middleGameTurnChange();
            if (view.yesNoQuestion("do you want to activate trap")) {
                while (true) {
                    Trap trap;
                    int address = view.getAddressForTrapOrSpell(getOpponentPlayer().getPlayerBoard().returnSpellZone(), "Summon opponent");
                    if (address == -1) {
                        cancel();
                        view.showSuccessMessageWithAString(SuccessMessage.SHOW_TURN_WHEN_OPPONENT_WANTS_ACTIVE_TRAP_OR_SPELL_OR_MONSTER, getCurrentPlayer().getNickname());
                        return;
                    } else if (address > 5 || address < 1) {
                        view.showError(Error.INVALID_NUMBER);
                    } else if (getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, address).getCardInCell().getCardType().equals(TRAP)) {
                        trap = (Trap) getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, address).getCardInCell();
                        if (trap.getTrapEffect().equals(TrapEffect.TORRENTIAL_TRIBUTE_EFFECT)) {
                            torrentialTributeTrapEffect();
                            addCardToGraveYard(Zone.SPELL_ZONE, address, getOpponentPlayer());
                            return;
                        }
                    } else view.showError(Error.INVALID_SELECTION);
                }
            }
            view.endOfMiddleGameTurnChange();
        }
        checkNewCardToBeBeUnderEffectOfFieldCard((Monster) selectedCell.getCardInCell());
    }

    private GameViewMessage tributeSet() {
        if (((Monster) selectedCell.getCardInCell()).getLevel() >= 7) {
            if (didTribute(2, getCurrentPlayer())) {
                return normalSet();
            }
        } else if (((Monster) selectedCell.getCardInCell()).getLevel() >= 5) {
            if (didTribute(1, getCurrentPlayer())) {
                return normalSet();
            }
        }
        return NEED_MORE_TRIBUTE;

    }

    private GameViewMessage isValidSelectionForSummonOrSet() {
//        if (selectedCellZone.equals(Zone.NONE)) {
//            Error.showError(Error.NO_CARD_SELECTED_YET);
//            return false;
//        } else
        if (getCurrentPlayer().getPlayerBoard().isMonsterZoneFull()) {
            Error.showError(Error.MONSTER_ZONE_IS_FULL);
            return FULL_MONSTER_ZONE;
        }
        return GameViewMessage.SUCCESS;
    }

    public GameViewMessage changeMonsterPosition() {
        if (selectedCell == null) {
            return NO_CARD_SELECTED;
        } else if (!(selectedCellZone == Zone.MONSTER_ZONE)) {
            return CANT_CHANGE_POSITION_OF_THIS_CARD;
        } else if (hasCardChangedPosition()) {
            return USED_CHANGE_POSITION;
        } else {
            String position;
            if (selectedCell.getCellStatus() == CellStatus.DEFENSIVE_OCCUPIED)
                position = "Attack Position";
            else if (selectedCell.getCellStatus() == CellStatus.OFFENSIVE_OCCUPIED)
                position = "Defense Position";
            else if (selectedCell.getCellStatus() == CellStatus.DEFENSIVE_HIDDEN)
                position = "Flip Summon";
            else position = "";
            if (view.askNewPosition(position)) {
                switch (position) {
                    case "Attack Position":
                        selectedCell.setCellStatus(CellStatus.OFFENSIVE_OCCUPIED);
                        break;
                    case "Defense Position":
                        selectedCell.setCellStatus(CellStatus.DEFENSIVE_OCCUPIED);
                        break;
                    case "Flip Summon":
                        flipSummon();
                        break;
                    default:
                        break;
                }
                changePositionUsed();
                view.reloadCurrentAndOpponentMonsterZone();
                view.showBoard();
                deselectCard(0);
                return SUCCESS;
            } else return NONE;
        }

    }

    public void attackToCard(int attackedAddress) {
        CellStatus opponentCellStatus = getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, attackedAddress).getCellStatus();
        if (opponentCellStatus.equals(CellStatus.DEFENSIVE_HIDDEN)) {
            attackToDHCard(getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, attackedAddress), attackedAddress);
        } else if (opponentCellStatus.equals(CellStatus.DEFENSIVE_OCCUPIED)) {
            attackToDOCard(getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, attackedAddress), attackedAddress);
        } else {
            attackToOOCard(getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, attackedAddress), attackedAddress);
        }
        attackUsed();
        deselectCard(0);
    }

    private void attackToDHCard(Cell opponentCellToBeAttacked, int toBeAttackedCardAddress) {
        GameResult result = null;
        DuelPlayer probableWinner = null;
        Monster opponentCard = (Monster) opponentCellToBeAttacked.getCardInCell();
        view.showSuccessMessageWithAString(SuccessMessage.DH_CARD_BECOMES_DO, opponentCard.getName());
        opponentCellToBeAttacked.changeCellStatus(CellStatus.DEFENSIVE_OCCUPIED);

        if (isTrapToBeActivatedInAttackSituation()) {
            if (isTrapOrSpellInAttackSituationActivated()) {
                view.endOfMiddleGameTurnChange();
                return;
            }
            view.endOfMiddleGameTurnChange();
        }
        Monster playerCard = (Monster) selectedCell.getCardInCell();
        int damage = playerCard.getAttackPower() - opponentCard.getAttackPower();
        if (damage > 0) {
            checkForManEaterBugAttacked();
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSuccessMessage(SuccessMessage.DEFENSIVE_MONSTER_DESTROYED);
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.NORMAL_ATTACK, "", selectedCellAddress, 0, toBeAttackedCardAddress, true);
            addCardToGraveYard(Zone.MONSTER_ZONE, toBeAttackedCardAddress, getOpponentPlayer());
            checkForYomiShipOrExploderDragonEffect(toBeAttackedCardAddress, opponentCard);
        } else if (damage < 0) {
            checkForManEaterBugAttacked();
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSuccessMessageWithAnInteger(SuccessMessage.CURRENT_PLAYER_RECEIVE_DAMAGE_AFTER_ATTACK, damage);
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSuccessMessageWithAnInteger(SuccessMessage.DAMAGE_TO_CURRENT_PLAYER_AFTER_ATTACK_TI_HIGHER_DEFENSIVE_DO_OR_DH_MONSTER, damage);
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.NORMAL_ATTACK, "", selectedCellAddress, 0, toBeAttackedCardAddress, true);

            getCurrentPlayer().decreaseLP(-damage);
            result = duelGameController.checkGameResult(getOpponentPlayer(), getCurrentPlayer(), GameResultToCheck.NO_LP);
            probableWinner = getOpponentPlayer();
        } else {
            checkForManEaterBugAttacked();
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSuccessMessage(SuccessMessage.NO_CARD_DESTROYED);
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.NORMAL_ATTACK, "", selectedCellAddress, 0, toBeAttackedCardAddress, true);

        }
        System.out.println("reach the end");
        if (result == GameResult.GAME_FINISHED) {
            finishGame(probableWinner);
        } else if (result == GameResult.ROUND_FINISHED) {
            finishRound(probableWinner);
        }
    }

    private void checkForManEaterBugAttacked() {
        PlayerBoard board = getOpponentPlayer().getPlayerBoard();
        boolean flag = false;
        for (int i = 1; i <= 5; i++) {
            Cell cell = board.getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i);
            if (cell.getCellStatus() == CellStatus.EMPTY) {
                continue;
            }
            Monster monster = ((Monster) cell.getCardInCell());
            if (monster.getMonsterEffect() == MonsterEffect.MAN_EATER_BUG_EFFECT) {
                flag = true;
            }
        }
        if (!flag) {
            return;
        }
        view.middleGameTurnChange();
        if (view.yesNoQuestion("do you want to activate man eater bug effect?")) {

            int address = view.askAddressForManEaterBug(getCurrentPlayer().getPlayerBoard().returnMonsterZone());
            addCardToGraveYard(Zone.MONSTER_ZONE, address, getCurrentPlayer());
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showPopUpMessageForEffect("Man eater bug effect : destroy " + address + "card of you!", MONSTER);


        }
        view.endOfMiddleGameTurnChange();
    }

    private void attackToDOCard(Cell opponentCellToBeAttacked, int toBeAttackedCardAddress) {
        GameResult result = null;
        DuelPlayer probableWinner = null;
        Monster playerCard = (Monster) selectedCell.getCardInCell();
        Monster opponentCard = (Monster) opponentCellToBeAttacked.getCardInCell();
        int damage = playerCard.getAttackPower() - opponentCard.getDefensePower();
        if (isTrapToBeActivatedInAttackSituation()) {
            if (isTrapOrSpellInAttackSituationActivated()) {
                view.endOfMiddleGameTurnChange();
                return;
            }
            view.endOfMiddleGameTurnChange();
        }
        if (damage > 0) {
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSuccessMessage(SuccessMessage.DEFENSIVE_MONSTER_DESTROYED);
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.NORMAL_ATTACK, "", selectedCellAddress, 0, toBeAttackedCardAddress, true);
            addCardToGraveYard(Zone.MONSTER_ZONE, toBeAttackedCardAddress, getOpponentPlayer());
            checkForYomiShipOrExploderDragonEffect(toBeAttackedCardAddress, opponentCard);
        } else if (damage < 0) {
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSuccessMessageWithAnInteger(SuccessMessage.DAMAGE_TO_CURRENT_PLAYER_AFTER_ATTACK_TI_HIGHER_DEFENSIVE_DO_OR_DH_MONSTER, damage);
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.NORMAL_ATTACK, "", selectedCellAddress, 0, toBeAttackedCardAddress, true);
            getCurrentPlayer().decreaseLP(-damage);
            result = duelGameController.checkGameResult(getOpponentPlayer(), getCurrentPlayer(), GameResultToCheck.NO_LP);
            probableWinner = getOpponentPlayer();
        } else {
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSuccessMessage(SuccessMessage.NO_CARD_DESTROYED);

            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.NORMAL_ATTACK, "", selectedCellAddress, 0, toBeAttackedCardAddress, true);

        }
        if (result == GameResult.GAME_FINISHED) {
            finishGame(probableWinner);
        } else if (result == GameResult.ROUND_FINISHED) {
            finishRound(probableWinner);
        }
    }

    private void attackToOOCard(Cell opponentCellToBeAttacked, int toBeAttackedCardAddress) { // might have effect
        GameResult result = null;
        DuelPlayer probableWinner = null;
        Monster playerCard = (Monster) selectedCell.getCardInCell();
        Monster opponentCard = (Monster) opponentCellToBeAttacked.getCardInCell();
        if (isTrapToBeActivatedInAttackSituation()) {
            if (isTrapOrSpellInAttackSituationActivated()) {
                view.endOfMiddleGameTurnChange();
                return;
            }
            view.endOfMiddleGameTurnChange();
        }
        int damage = playerCard.getAttackPower() - opponentCard.getAttackPower();
        if (damage > 0) {
            if (!checkForYomiShipOrExploderDragonEffect(toBeAttackedCardAddress, opponentCard)) {
                if (!getCurrentPlayer().getNickname().equals("ai"))
                    view.showSuccessMessageWithAnInteger(SuccessMessage.OPPONENT_RECEIVE_DAMAGE_AFTER_ATTACK, damage);
                if (!getCurrentPlayer().getNickname().equals("ai"))
                    view.playAnimation(Animation.NORMAL_ATTACK, "", selectedCellAddress, 0, toBeAttackedCardAddress, true);
                addCardToGraveYard(Zone.MONSTER_ZONE, toBeAttackedCardAddress, getOpponentPlayer());
                getOpponentPlayer().decreaseLP(damage);
                result = duelGameController.checkGameResult(getCurrentPlayer(), getOpponentPlayer(), GameResultToCheck.NO_LP);
                probableWinner = getCurrentPlayer();
            } else {
                System.out.println("exploder effect");
            }
        } else if (damage < 0) {
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSuccessMessageWithAnInteger(SuccessMessage.CURRENT_PLAYER_RECEIVE_DAMAGE_AFTER_ATTACK, damage);
            getCurrentPlayer().decreaseLP(-damage);
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.NORMAL_ATTACK, "", selectedCellAddress, 0, toBeAttackedCardAddress, true);
            addCardToGraveYard(Zone.MONSTER_ZONE, selectedCellAddress, getCurrentPlayer());
            result = duelGameController.checkGameResult(getOpponentPlayer(), getCurrentPlayer(), GameResultToCheck.NO_LP);
            probableWinner = getOpponentPlayer();
        } else {
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showSuccessMessage(SuccessMessage.NO_DAMAGE_TO_ANYONE);
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.playAnimation(Animation.NORMAL_ATTACK, "", selectedCellAddress, 0, toBeAttackedCardAddress, true);
            addCardToGraveYard(Zone.MONSTER_ZONE, selectedCellAddress, getCurrentPlayer());
            addCardToGraveYard(Zone.MONSTER_ZONE, toBeAttackedCardAddress, getOpponentPlayer());
        }
        if (result == GameResult.GAME_FINISHED) {
            finishGame(probableWinner);
        } else if (result == GameResult.ROUND_FINISHED) {
            finishRound(probableWinner);
        }
    }

    private boolean checkForYomiShipOrExploderDragonEffect(int toBeAttackedCardAddress, Monster opponentCard) {
        if (opponentCard.getMonsterEffect().equals(MonsterEffect.YOMI_SHIP_EFFECT)) {//yomi ship effect
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showPopUpMessageForEffect("Yomi Ship effect", MONSTER);
            addCardToGraveYard(Zone.MONSTER_ZONE, selectedCellAddress, getCurrentPlayer());
        } else if (opponentCard.getMonsterEffect().equals(MonsterEffect.EXPLODER_DRAGON_EFFECT)) {//exploder dragon effect
            addCardToGraveYard(Zone.MONSTER_ZONE, selectedCellAddress, getCurrentPlayer());
            addCardToGraveYard(Zone.MONSTER_ZONE, toBeAttackedCardAddress, getOpponentPlayer());
            if (!getCurrentPlayer().getNickname().equals("ai"))
                view.showPopUpMessageForEffect("Exploder dragon effect", MONSTER);
            return true;//to stop damage decreasing
        }
        addCardToGraveYard(Zone.MONSTER_ZONE, toBeAttackedCardAddress, getOpponentPlayer());
        return false;
    }

    private GameViewMessage isValidAttack() {
        if (!selectedCell.getCellStatus().equals(CellStatus.OFFENSIVE_OCCUPIED)) { // u sure ?
            Error.showError(Error.CAN_NOT_ATTACK);
            return CAN_NOT_ATTACK_WITH_THIS_CARD;
        }
        if (hasCardUsedItsAttack()) {
            Error.showError(Error.ALREADY_ATTACKED);
            return ATTACK_USED_BEFORE;
        }
        return SUCCESS;
    }

    private boolean isTrapToBeActivatedInAttackSituation() {
        for (int i = 1; i <= 5; i++) {
            Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, i);
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
                    view.middleGameTurnChange();
                    if (view.yesNoQuestion("do you want to activate your trap and spell?")) {
                        return true;
                    } else {
                        view.endOfMiddleGameTurnChange();
                        return false;
                    }
            }
        }
        return false;
    }

    private boolean isTrapOrSpellInAttackSituationActivated() {
        int address = view.getAddressForTrapOrSpell(getOpponentPlayer().getPlayerBoard().returnSpellZone(), "Attack");
        if (address == -1) {
            return false;
        } else {
            Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.SPELL_ZONE, address);
            if (cell.getCardInCell().getCardType().equals(TRAP)) {
                Trap trap = (Trap) cell.getCardInCell();
                if (activateTrapEffectInAttackSituation(trap.getTrapEffect(), cell)) {
                    addCardToGraveYard(Zone.SPELL_ZONE, address, getOpponentPlayer()); //CHECK correctly removed ?
                }
                if (!getCurrentPlayer().getNickname().equals("ai"))
                    view.showPopUpMessageForEffect("Trap effect!", TRAP);
                return true;
            }
        }
        return false;
    }

    public boolean isValidTrapToActivateInAttackSituationByOpponent(Card card) {
        if (card.getCardType() != TRAP)
            return false;
        switch (((Trap) card).getTrapEffect()) {
            case MAGIC_CYLINDER_EFFECT:
            case NEGATE_ATTACK_EFFECT:
            case MIRROR_FORCE_EFFECT:
                return true;
            default:
                Error.showError(Error.PREPARATIONS_IS_NOT_DONE);
                return false;
        }
    }

    private boolean activateTrapEffectInAttackSituation(TrapEffect trapEffect, Cell cell) {
        switch (trapEffect) {
            case MAGIC_CYLINDER_EFFECT:
                cell.setCellStatus(CellStatus.OCCUPIED);
                view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
                view.showBoard();
                trapMagicCylinderEffect();
                return true;
            case MIRROR_FORCE_EFFECT:
                cell.setCellStatus(CellStatus.OCCUPIED);
                view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
                view.showBoard();
                trapMirrorForceEffect();
                return true;
            case NEGATE_ATTACK_EFFECT:
                cell.setCellStatus(CellStatus.OCCUPIED);
                view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
                view.showBoard();
                trapNegateAttackEffect();
                return true;
            default:
                Error.showError(Error.PREPARATIONS_IS_NOT_DONE);
                return false;
        }
    }

    public void flipSummon() {
        if (!((Monster) selectedCell.getCardInCell()).getMonsterEffect().equals(MonsterEffect.MAN_EATER_BUG_EFFECT)) {
            selectedCell.setCellStatus(CellStatus.OFFENSIVE_OCCUPIED);
            view.showSuccessMessage(SuccessMessage.FLIP_SUMMON_SUCCESSFUL);
            view.showBoard();

        } else {
            manEaterBugMonsterEffectAndFlipSummon(getCurrentPlayer(), getOpponentPlayer());
        }
        if (isCurrentPlayerTrapToBeActivatedInSummonSituation()) {
            if (isTrapOfCurrentPlayerInSummonSituationActivated()) {
                return;
            }
        }
        if (isOpponentTrapToBeActivatedInSummonSituation(selectedCellAddress)) {
            view.middleGameTurnChange();
            isOpponentTrapInSummonSituationActivated(selectedCellAddress);
            view.endOfMiddleGameTurnChange();
        }
        deselectCard(0);

    }

    private void manEaterBugMonsterEffectAndFlipSummon(DuelPlayer current, DuelPlayer opponent) {
        if (view.yesNoQuestion("do you want to activate man eater bug effect? yes or no")) {

            int address = view.askAddressForManEaterBug(getOpponentPlayer().getPlayerBoard().returnMonsterZone());
            if (address >= 1 && address <= 5) {
                if (opponent.getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, address).getCellStatus().equals(CellStatus.EMPTY)) {
                    Error.showError(Error.INVALID_SELECTION);
                } else {
                    addCardToGraveYard(Zone.MONSTER_ZONE, address, opponent);
                    System.out.println("Man eater bug effect : destroy " + address + "card of opponent!");
                    if (!getCurrentPlayer().getNickname().equals("ai"))
                        view.showPopUpMessageForEffect("Man eater bug effect!", MONSTER);
                }
            } else Error.showError(Error.INVALID_NUMBER);
        }
        selectedCell.setCellStatus(CellStatus.OFFENSIVE_OCCUPIED);
        view.showSuccessMessage(SuccessMessage.FLIP_SUMMON_SUCCESSFUL);
        view.showBoard();
        deselectCard(0);

    }

    // SPELL RELATED
    public GameViewMessage setSpellOrTrap() {
        if (!selectedCellZone.equals(Zone.HAND)) {
            return CAN_NOT_SET;
        } else if (getCurrentPlayer().getPlayerBoard().isSpellZoneFull()) {
            return FULL_SPELL_ZONE;
        } else if (selectedCell.getCardInCell().getCardType().equals(SPELL)) {
            if (((Spell) selectedCell.getCardInCell()).getSpellType().equals(SpellType.FIELD)) {
                return CANT_SET_FIELD_CARD;
            } else {
                SpellZone spellZone = getCurrentPlayer().getPlayerBoard().returnSpellZone();
                int addressOfAdd = spellZone.addCard(selectedCell.getCardInCell(), CellStatus.HIDDEN);
                getCurrentPlayerHand().remove(selectedCellAddress - 1);
                view.playAnimation(Animation.SET_SPELL, selectedCell.getCardInCell().getName(), addressOfAdd, selectedCellAddress, 0, true);
                view.showSuccessMessage(SuccessMessage.SET_SUCCESSFULLY);
                view.showBoard();
            }
            deselectCard(0);
        } else {
            SpellZone spellZone = getCurrentPlayer().getPlayerBoard().returnSpellZone();
            int addressOfAdd = spellZone.addCard(selectedCell.getCardInCell(), CellStatus.HIDDEN);
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
            view.playAnimation(Animation.SET_SPELL, selectedCell.getCardInCell().getName(), addressOfAdd, selectedCellAddress, 0, true);
            view.showSuccessMessage(SuccessMessage.SET_SUCCESSFULLY);
            view.showBoard();
            deselectCard(0);
        }
        return NONE;

    }

    private void setFieldCard() {
        getCurrentPlayer().getPlayerBoard().setFieldSpell((Spell) selectedCell.getCardInCell());
        view.showSuccessMessage(SuccessMessage.SET_SUCCESSFULLY);
        getCurrentPlayerHand().remove(selectedCellAddress - 1);
        view.showBoard();
        deselectCard(0);
    }

    public GameViewMessage activateEffectOfSpellOrTrap() {

        if (selectedCell == null) {

            return NO_CARD_SELECTED;
        }
//        if (!selectedCellZone.equals(Zone.SPELL_ZONE) && !selectedCellZone.equals(Zone.HAND) && selectedCellZone.equals(Zone.FIELD_ZONE)) {
//            Error.showError(Error.ONLY_SPELL_CAN_ACTIVE);
//            return;
//        }
        if (!currentPhase.equals(Phase.MAIN_PHASE_1) && !currentPhase.equals(Phase.MAIN_PHASE_2)) {
            return ACTION_NOT_ALLOWED;
        }
        if (selectedCellZone.equals(Zone.SPELL_ZONE)) {
            if (selectedCell.getCellStatus().equals(CellStatus.OCCUPIED)) {
                return CARD_ALREADY_ACTIVATED;
            }
        }
        if (selectedCellZone.equals(Zone.HAND)) {
            if (getCurrentPlayer().getPlayerBoard().isSpellZoneFull()) {
                return FULL_SPELL_ZONE;
            }
        }
        if (selectedCell.getCardInCell().getCardType() == CardType.SPELL) {
            Spell spell = (Spell) selectedCell.getCardInCell();
            if (spell.getSpellType() == SpellType.RITUAL) {
                return activateRitualSpell();
            }
            if (!((Spell) selectedCell.getCardInCell()).getSpellType().equals(SpellType.FIELD)) {
                return normalSpellActivate(((Spell) selectedCell.getCardInCell()).getSpellEffect());
            } else return fieldZoneSpellActivate();
        } else if (selectedCell.getCardInCell().getCardType() == TRAP) {
            if (selectedCellZone == Zone.SPELL_ZONE) {
                return normalTrapActivate(((Trap) selectedCell.getCardInCell()).getTrapEffect());
            } else {
                return CAN_NOT_ACTIVE_TRAP_IN_HAND;
            }
        }
        return NONE;
    }

    private GameViewMessage normalSpellActivate(SpellEffect spellEffect) {
        switch (spellEffect) {
            case MONSTER_REBORN_EFFECT:
                return monsterRebornSpell();

            case TERRAFORMING_EFFECT:
                return terraFormingSpell();

            case POT_OF_GREED_EFFECT:
                return potOfGreedSpell();
            case RAIGEKI_EFFECT:
                return raigekiSpell();

            case HARPIES_FEATHER_DUSTER_EFFECT:
                return harpiesFeatherDusterSpell();

            case DARK_HOLE_EFFECT:
                return darkHoleSpell();

            case SWORD_OF_DARK_DESTRUCTION_EFFECT:
                return swordOfDarkDestructionSpell();

            case BLACK_PENDANT_EFFECT:
                return blackPendantSpell();

            default:
                return PREPARATIONS_IS_NOT_DONE;
        }
    }

    private GameViewMessage normalTrapActivate(TrapEffect trapEffect) {
        switch (trapEffect) {
            case CALL_OF_THE_HAUNTED_EFFECT:
                return callOfTheHauntedTrap();

            case TIME_SEAL_EFFECT:
                return timeSealTrap();
            default:
                return PREPARATIONS_IS_NOT_DONE;
        }
    }

    private GameViewMessage callOfTheHauntedTrap() {
        if (getCurrentPlayer().getPlayerBoard().isGraveYardEmpty() || getCurrentPlayer().getPlayerBoard().isMonsterZoneFull()) {
            view.showError(Error.PREPARATIONS_IS_NOT_DONE);
            return PREPARATIONS_IS_NOT_DONE;
        }
        ArrayList<Card> currentGraveYard = getCurrentPlayer().getPlayerBoard().returnGraveYard().getGraveYardCards();
        ArrayList<Card> currentMonstersInGraveyard = (ArrayList<Card>) currentGraveYard.stream().filter(card -> card.getCardType() == MONSTER).collect(Collectors.toList());
        if (currentMonstersInGraveyard.size() == 0)
            return PREPARATIONS_IS_NOT_DONE;
        GraveYard graveYard = getCurrentPlayer().getPlayerBoard().returnGraveYard();
        ArrayList<Card> cards = graveYard.getGraveYardCards();
        view.showCurrentGraveYard(false);
        view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
        selectedCell.setCellStatus(CellStatus.OCCUPIED);
        view.showActivateEffectOfSpellInZone();
        view.showBoard();
        Card card;
        int address = view.chooseCardInGraveYard(currentMonstersInGraveyard, cards);
        card = graveYard.getGraveYardCards().get(address - 1);
        specialSummon(card, CellStatus.OFFENSIVE_OCCUPIED, Zone.GRAVEYARD, address,graveYard.getGraveYardCards());
        addCardToGraveYard(Zone.SPELL_ZONE, selectedCellAddress, getCurrentPlayer());
        return NONE;
    }

    private GameViewMessage timeSealTrap() {
        cantDrawCardBecauseOfTimeSeal = true;
        selectedCell.setCellStatus(CellStatus.OCCUPIED);
        view.showSuccessMessage(SuccessMessage.TRAP_ACTIVATED);
        view.showBoard();
        view.showActivateEffectOfSpellInZone();
        addressOfTimeSealToRemove = selectedCellAddress;
        return NONE;
    }

    private GameViewMessage fieldZoneSpellActivate() {

        if (fieldZoneSpell == null) {
            if (turn == 1) {
                isFieldActivated = 1;
                firstPlayer.getPlayerBoard().faceUpActiveFieldSpell((Spell) selectedCell.getCardInCell());
            } else {
                isFieldActivated = 2;
                secondPlayer.getPlayerBoard().faceUpActiveFieldSpell((Spell) selectedCell.getCardInCell());
            }
        } else {
            reversePreviousFieldZoneSpellEffectAndRemoveIt();
            isFieldActivated = getTurn();
            if (isFieldActivated == 1) {
                firstPlayer.getPlayerBoard().faceUpActiveFieldSpell((Spell) selectedCell.getCardInCell());
            } else {
                secondPlayer.getPlayerBoard().faceUpActiveFieldSpell((Spell) selectedCell.getCardInCell());
            }
        }
        view.showBoard();
        if (selectedCellZone == Zone.HAND) {
            getCurrentPlayerHand().remove(selectedCellAddress - 1);
        }
        fieldZoneSpell = (Spell) selectedCell.getCardInCell();

        findAndActivateFieldCard();
        view.playAnimation(Animation.FIELD_SPELL, fieldZoneSpell.getName(), 0, selectedCellAddress, 0, true);
        deselectCard(0);
        return NONE;
    }

    private void reversePreviousFieldZoneSpellEffectAndRemoveIt() {
        switch (fieldZoneSpell.getSpellEffect()) {
            case YAMI_EFFECT:
                yamiFieldEffectReverse();
                break;
            case FOREST_EFFECT:
                forestFieldEffectReverse();
                break;
            case CLOSED_FOREST_EFFECT:
                closedForestFieldEffectReverse();
                break;
            case UMIIRUKA_EFFECT:
                umirukaEffectReverse();
        }
        fieldZoneSpell = null;
        addCardToGraveYard(Zone.FIELD_ZONE, 0, isFieldActivated == 1 ? firstPlayer : secondPlayer);
        isFieldActivated = 0;
    }

    private void yamiFieldEffectReverse() {
        for (Card card : fieldEffectedCards) {
            if (((Monster) card).getMonsterType().equals(MonsterType.FIEND) || ((Monster) card).getMonsterType().equals(MonsterType.SPELLCASTER)) {
                ((Monster) card).changeAttackPower(-200);
                ((Monster) card).changeDefensePower(-200);
            } else {
                ((Monster) card).changeAttackPower(+200);
                ((Monster) card).changeDefensePower(+200);
            }
        }
        fieldEffectedCards.clear();
    }

    private void forestFieldEffectReverse() {
        for (Card card : fieldEffectedCards) {
            if (((Monster) card).getMonsterType().equals(MonsterType.INSECT) || ((Monster) card).getMonsterType().equals(MonsterType.BEAST) || ((Monster) card).getMonsterType().equals(MonsterType.BEAST_WARRIOR)) {
                ((Monster) card).changeAttackPower(-200);
                ((Monster) card).changeDefensePower(-200);
            }
        }
        fieldEffectedCards.clear();
    }

    private void closedForestFieldEffectReverse() {
        for (Card card : fieldEffectedCards) {
            if (((Monster) card).getMonsterType().equals(MonsterType.BEAST_WARRIOR)) {
                ((Monster) card).changeAttackPower(-100);
            }
        }
        fieldEffectedCards.clear();
    }

    private void umirukaEffectReverse() {
        for (Card card : fieldEffectedCards) {
            if (((Monster) card).getMonsterType().equals(MonsterType.AQUA)) {
                ((Monster) card).changeAttackPower(-500);
                ((Monster) card).changeDefensePower(400);
            }
        }
        fieldEffectedCards.clear();
    }

    private void findAndActivateFieldCard() {
        switch (fieldZoneSpell.getSpellEffect()) {
            case YAMI_EFFECT:
                yamiFieldEffect();
                break;
            case FOREST_EFFECT:
                forestFieldEffect();
                break;
            case CLOSED_FOREST_EFFECT:
                closedForestFieldEffect();
                break;
            case UMIIRUKA_EFFECT:
                umiirukaFieldEffect();
                break;
        }
    }

    private void yamiFieldEffect() {
        addYamiFieldCardsToBeEffected();
        for (Card card : fieldEffectedCards) {
            if (((Monster) card).getMonsterType().equals(MonsterType.FIEND) || ((Monster) card).getMonsterType().equals(MonsterType.SPELLCASTER)) {
                ((Monster) card).changeAttackPower(200);
                ((Monster) card).changeDefensePower(200);
            } else if (((Monster) card).getMonsterType().equals(MonsterType.FAIRY)) {
                ((Monster) card).changeDefensePower(-200);
                ((Monster) card).changeAttackPower(-200);
            }
        }
    }

    private void addYamiFieldCardsToBeEffected() {
        for (int i = 1; i <= 5; i++) {
            Cell cell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i);
            if (cell.getCellStatus() != CellStatus.EMPTY) {
                if (((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.FIEND) || ((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.SPELLCASTER) && !fieldEffectedCardsAddress.contains(10 + i))
                    fieldEffectedCardsAddress.add(10 + i);
            }
        }
        for (int i = 1; i <= 5; i++) {
            Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i);
            if (cell.getCellStatus() != CellStatus.EMPTY) {
                if (((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.FIEND) || ((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.SPELLCASTER) && !fieldEffectedCardsAddress.contains(20 + i))
                    fieldEffectedCardsAddress.add(20 + i);
            }
        }
        addFoundCardsToBeEffectedByFieldCardToArrayList();
    }

    private void addFoundCardsToBeEffectedByFieldCardToArrayList() {
        for (Integer address : fieldEffectedCardsAddress) {
            if (address > 20) {
                if (!getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, address - 20).getCellStatus().equals(CellStatus.EMPTY))
                    fieldEffectedCards.add(getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, address - 20).getCardInCell());
            } else {
                if (!getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, address - 10).getCellStatus().equals(CellStatus.EMPTY))
                    fieldEffectedCards.add(getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, address - 10).getCardInCell());
            }
        }
    }

    private void forestFieldEffect() {
        addForestFieldCardsToBeEffected();
        for (Card card : fieldEffectedCards) {
            if (((Monster) card).getMonsterType().equals(MonsterType.INSECT) || ((Monster) card).getMonsterType().equals(MonsterType.BEAST) || ((Monster) card).getMonsterType().equals(MonsterType.BEAST_WARRIOR)) {
                ((Monster) card).changeAttackPower(200);
                ((Monster) card).changeDefensePower(200);
            }
        }
    }

    private void addForestFieldCardsToBeEffected() {
        for (int i = 1; i <= 5; i++) {
            Cell cell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i);
            if (cell.getCellStatus() != CellStatus.EMPTY) {
                if (((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.INSECT) ||
                        ((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.BEAST) ||
                        ((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.BEAST_WARRIOR) && !fieldEffectedCardsAddress.contains(10 + i))
                    fieldEffectedCardsAddress.add(10 + i);
            }
        }
        for (int i = 1; i <= 5; i++) {
            Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i);
            if (cell.getCellStatus() != CellStatus.EMPTY) {
                if (((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.FIEND) ||
                        ((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.BEAST) ||
                        ((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.BEAST_WARRIOR) && !fieldEffectedCardsAddress.contains(20 + i))
                    fieldEffectedCardsAddress.add(20 + i);
            }
        }
        addFoundCardsToBeEffectedByFieldCardToArrayList();
    }

    private void closedForestFieldEffect() {
        addClosedForestFieldCardsToBeEffected();
        for (Card card : fieldEffectedCards) {
            if (((Monster) card).getMonsterType().equals(MonsterType.BEAST)) {
                ((Monster) card).changeAttackPower(100);
            }
        }
    }

    private void addClosedForestFieldCardsToBeEffected() {
        for (int i = 1; i <= 5; i++) {
            Cell cell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i);
            if (cell.getCellStatus() != CellStatus.EMPTY) {
                if (((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.BEAST) && !fieldEffectedCardsAddress.contains(10 + i))
                    fieldEffectedCardsAddress.add(10 + i);
            }
        }
        addFoundCardsToBeEffectedByFieldCardToArrayList();
    }

    private void umiirukaFieldEffect() {
        addUmiirukaFieldCardsToBeEffected();
        for (Card card : fieldEffectedCards) {
            if (((Monster) card).getMonsterType().equals(MonsterType.AQUA)) {
                ((Monster) card).changeAttackPower(500);
                ((Monster) card).changeDefensePower(-400);
            }
        }
    }

    private void addUmiirukaFieldCardsToBeEffected() {
        for (int i = 1; i <= 5; i++) {
            Cell cell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i);
            if (cell.getCellStatus() != CellStatus.EMPTY) {
                if (((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.AQUA) &&
                        !fieldEffectedCardsAddress.contains(10 + i))
                    fieldEffectedCardsAddress.add(10 + i);
            }
        }
        for (int i = 1; i <= 5; i++) {
            Cell cell = getOpponentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, i);
            if (cell.getCellStatus() != CellStatus.EMPTY) {
                if (((Monster) cell.getCardInCell()).getMonsterType().equals(MonsterType.AQUA) &&
                        !fieldEffectedCardsAddress.contains(10 + i))
                    fieldEffectedCardsAddress.add(20 + i);
            }
        }
        addFoundCardsToBeEffectedByFieldCardToArrayList();
    }

    private void checkDeathOfUnderFieldEffectCard(Monster monster) {
        if (fieldZoneSpell == null)
            return;
        switch (fieldZoneSpell.getSpellEffect()) {
            case YAMI_EFFECT:
                reverseYamiFieldEffectOnOneCard(monster);
                break;
            case FOREST_EFFECT:
                reverseForestFieldEffectOnOneCard(monster);
                break;
            case CLOSED_FOREST_EFFECT:
                reverseClosedForestFieldEffectOnOneCard(monster);
                break;
            case UMIIRUKA_EFFECT:
                reverseUmiirukaFieldEffectOnOneCard(monster);
                break;
        }
    }

    private void checkNewCardToBeBeUnderEffectOfFieldCard(Monster monster) {
        if (fieldZoneSpell == null)
            return;
        switch (fieldZoneSpell.getSpellEffect()) {
            case YAMI_EFFECT:
                yamiFieldEffectOnOneCard(monster);
                break;
            case FOREST_EFFECT:
                forestFieldEffectOnOneCard(monster);
                break;
            case CLOSED_FOREST_EFFECT:
                closedForestFieldEffectOnOneCard(monster);
                break;
            case UMIIRUKA_EFFECT:
                umiirukaFieldEffectOnOneCard(monster);
                break;
        }
    }

    private void yamiFieldEffectOnOneCard(Monster monster) {
        if (monster.getMonsterType().equals(MonsterType.FIEND) || monster.getMonsterType().equals(MonsterType.BEAST)
                || monster.getMonsterType().equals(MonsterType.SPELLCASTER)) {
            monster.changeAttackPower(200);
            monster.changeDefensePower(200);
            fieldEffectedCards.add(monster);
        } else if (monster.getMonsterType().equals(MonsterType.FAIRY)) {
            monster.changeDefensePower(-200);
            monster.changeAttackPower(-200);
            fieldEffectedCards.add(monster);
        }
    }

    private void forestFieldEffectOnOneCard(Monster monster) {
        if (monster.getMonsterType().equals(MonsterType.INSECT) ||
                monster.getMonsterType().equals(MonsterType.BEAST) ||
                monster.getMonsterType().equals(MonsterType.BEAST_WARRIOR)) {
            monster.changeAttackPower(200);
            monster.changeDefensePower(200);
            fieldEffectedCards.add(monster);
        }
    }

    private void closedForestFieldEffectOnOneCard(Monster monster) {
        if (monster.getMonsterType().equals(MonsterType.BEAST)) {
            monster.changeAttackPower(100);
            fieldEffectedCards.add(monster);
        }
    }

    private void umiirukaFieldEffectOnOneCard(Monster monster) {
        if (monster.getMonsterType().equals(MonsterType.AQUA)) {
            monster.changeAttackPower(500);
            monster.changeDefensePower(-400);
            fieldEffectedCards.add(monster);
        }
    }

    private void reverseYamiFieldEffectOnOneCard(Monster monster) {
        if (monster.getMonsterType().equals(MonsterType.INSECT) || monster.getMonsterType().equals(MonsterType.BEAST)
                || monster.getMonsterType().equals(MonsterType.BEAST_WARRIOR)) {
            monster.changeAttackPower(-200);
            monster.changeDefensePower(-200);
            fieldEffectedCards.remove(monster);
        } else if (monster.getMonsterType().equals(MonsterType.FAIRY)) {
            monster.changeDefensePower(200);
            monster.changeAttackPower(200);
            fieldEffectedCards.remove(monster);
        }
    }

    private void reverseForestFieldEffectOnOneCard(Monster monster) {
        if (monster.getMonsterType().equals(MonsterType.INSECT) ||
                monster.getMonsterType().equals(MonsterType.BEAST) ||
                monster.getMonsterType().equals(MonsterType.BEAST_WARRIOR)) {
            monster.changeAttackPower(-200);
            monster.changeDefensePower(-200);
            fieldEffectedCards.remove(monster);
        }
    }

    private void reverseClosedForestFieldEffectOnOneCard(Monster monster) {
        if (monster.getMonsterType().equals(MonsterType.BEAST)) {
            monster.changeAttackPower(100);
            fieldEffectedCards.remove(monster);
        }
    }

    private void reverseUmiirukaFieldEffectOnOneCard(Monster monster) {
        if (monster.getMonsterType().equals(MonsterType.AQUA)) {
            monster.changeAttackPower(-500);
            monster.changeDefensePower(400);
            fieldEffectedCards.remove(monster);
        }
    }

    public void aiTurn() {
        drawCardFromDeck();
        if (getCurrentPlayer().getPlayerBoard().isMonsterZoneEmpty()) {
            selectCardInHand(1);
            currentPhase = Phase.MAIN_PHASE_1;
            summonMonster();
        }
        selectedCell = getCurrentPlayer().getPlayerBoard().getACellOfBoardWithAddress(Zone.MONSTER_ZONE, 1);
        selectedCellAddress = 1;
        selectedCellZone = Zone.MONSTER_ZONE;
        currentPhase = Phase.BATTLE_PHASE;
        if (selectedCell.getCellStatus() != CellStatus.EMPTY) {
            if (!getOpponentPlayer().getPlayerBoard().isMonsterZoneEmpty()) {
                MonsterZone opponentZone = getOpponentPlayer().getPlayerBoard().returnMonsterZone();
                for (int i = 1; i <= 5; i++) {
                    Cell cell = opponentZone.getCellWithAddress(i);
                    if (cell.getCellStatus() != CellStatus.EMPTY) {
                        attackToCard(i);
                        break;
                    }
                }
            } else {
                directAttack();
            }
        }

        currentPhase = Phase.MAIN_PHASE_2;
        nextPhase();
    }

    public void setWinnerCheat(String winnerNickName) {
        DuelPlayer winner;
        DuelPlayer loser;
        if (firstPlayer.getNickname().equals(winnerNickName)) {
            winner = firstPlayer;
            loser = secondPlayer;
        } else {
            winner = secondPlayer;
            loser = firstPlayer;
        }
        if (duelGameController.getDuel().getNumberOfRounds() == 3)
            duelGameController.updateScoreAndCoinForThreeRounds(winner, loser, 2);
        else duelGameController.updateScoreAndCoinForOneRound(winner, loser);
        finishGame(winner);
    }

    private void finishGame(DuelPlayer winner) {
        isFinishedGame = true;
        clear();
    }

    private void finishRound(DuelPlayer winner) {
        duelGameController.getDuel().finishRound();
        isFinishedRound = true;

        view.showSuccessMessageWithAString(SuccessMessage.ROUND_FINISHED, winner.getNickname());
        DuelPlayer player1 = DuelGameController.getInstance().getDuel().getPlayer1();
        DuelPlayer player2 = DuelGameController.getInstance().getDuel().getPlayer2();
        if (player1.getNickname().equals("ai"))
            BetweenRoundController.getInstance().setPlayer1(player2, true);
        else if (player2.getNickname().equals("ai"))
            BetweenRoundController.getInstance().setPlayer1(player1, true);
        else {
            BetweenRoundController.getInstance().setPlayer1(player1, false);
            BetweenRoundController.getInstance().setPlayer2(player2, false);
        }
        if (duelGameController.getDuel().getNumberOfRounds() == 3) {
            RoundGameController.getInstance().getView().showFinishRoundOfMatchPopUpMessageAndCloseGameView(winner.getNickname());
        }


        DuelGameController.getInstance().setSpecifier(winner.getNickname());
        clear();
    }

    public void clear() {
        selectedCell = null;
        selectedCellZone = Zone.NONE;
        drawUsed = false;
        isSummonOrSetOfMonsterUsed = false;
        currentPhase = Phase.DRAW_PHASE;
        firstPlayerHand = new ArrayList<>();
        secondPlayerHand = new ArrayList<>();
        turn = 1; // 1 : firstPlayer, 2 : secondPlayer
        usedCellsToAttackNumbers = new ArrayList<>();
        changedPositionCards = new ArrayList<>();
        fieldZoneSpell = null;
        fieldEffectedCards = new ArrayList<>();
        fieldEffectedCardsAddress = new ArrayList<>();
        isFieldActivated = 0; // 0 : no - 1 : firstPlayed activated it- 2 : secondPlayer activated it
        firstPlayerHashmapForEquipSpells = new HashMap<>();
        secondPlayerHashmapForEquipSpells = new HashMap<>();
    }

    public boolean isFinishedRound() {
        return isFinishedRound;
    }

    public boolean isFinishedGame() {
        return isFinishedGame;
    }

    public GameViewMessage summonOrActivate() {
        if (selectedCellZone == Zone.HAND || selectedCellZone == Zone.SPELL_ZONE || selectedCellZone == Zone.FIELD_ZONE) {
            if (selectedCell.getCardInCell().getCardType() == MONSTER)
                return summonMonster();
            else
                return activateEffectOfSpellOrTrap();
        }
        return null;
    }

    public GameViewMessage attack() {
        System.out.println(selectedCell + "  " + selectedCellZone + "  " + selectedCellAddress);
        GameViewMessage message;
        if (selectedCell == null)
            return NO_CARD_SELECTED;
        if (selectedCell.getCellStatus() == CellStatus.EMPTY)
            return NONE;
        if (selectedCellZone != Zone.MONSTER_ZONE)
            return CAN_NOT_ATTACK_WITH_THIS_CARD;
        if (getOpponentPlayer().getPlayerBoard().isMonsterZoneEmpty()) {
            message = directAttack();
        } else {

            if ((message = isValidAttack()) == SUCCESS) {
                attackToCard();
                message = CHOOSE_CARD_TO_ATTACK;
            } else return message;
        }
        return message;
    }

    private void attackToCard() {
        view.askAttackAddress();

    }

    public GameView getView() {
        return view;
    }

    public void setView(GameView view) {
        this.view = view;
    }

    public boolean isValidTrapToActivateInSummonByCurrent(Card cardInCell) {
        if (cardInCell.getCardType() == SPELL) {
            return false;
        }
        return ((Trap) cardInCell).getTrapEffect() == TrapEffect.TORRENTIAL_TRIBUTE_EFFECT;
    }

    public boolean isValidTrapToActivateInSummonByOpponent(Card cardInCell) {
        if (cardInCell.getCardType() == SPELL) {
            return false;
        }
        TrapEffect trapEffect = ((Trap) cardInCell).getTrapEffect();
        return trapEffect == TrapEffect.TORRENTIAL_TRIBUTE_EFFECT || trapEffect == TrapEffect.TRAP_HOLE_EFFECT;
    }
}