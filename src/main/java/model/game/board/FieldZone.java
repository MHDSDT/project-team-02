package model.game.board;

import model.card.Card;

public class FieldZone {

    private Cell fieldCell;

    public void addCard(Card card) {
        fieldCell.setCardInCell(card);
    }

    public void setCellStatus(CellStatus status) {
        fieldCell.setCellStatus(status);
    }

    public Card getCard() {
        return fieldCell.getCardInCell();
    }

    public CellStatus getCellStatus() {
        return fieldCell.getCellStatus();
    }
    public void setCard(Card card){
        fieldCell.setCardInCell(card);
    }

    public Cell getFieldCell() {
        return fieldCell;
    }
}
