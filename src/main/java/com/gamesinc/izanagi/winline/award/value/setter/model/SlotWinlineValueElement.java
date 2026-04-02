package com.gamesinc.izanagi.winline.award.value.setter.model;

/**
 * Winline value element, i.e a position on reel that can potentially
 * have the value assigned to it in DS.
 * So for each symbol id defined the place in DS, where
 * the value assigned to this symbol id position stores.
 */
public class SlotWinlineValueElement {
    private final Integer symbolId;
    private final SlotDataStorageDescription storageDescription;

    public SlotWinlineValueElement() {
        symbolId = null;
        storageDescription = null;
    }

    public SlotWinlineValueElement(Integer symbolId,
                                   SlotDataStorageDescription storageDescription) {
        this.symbolId = symbolId;
        this.storageDescription = storageDescription;
    }

    public Integer getSymbolId() {
        return symbolId;
    }

    public SlotDataStorageDescription getStorageDescription() {
        return storageDescription;
    }

}
