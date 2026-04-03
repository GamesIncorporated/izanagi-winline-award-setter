package com.gamesinc.izanagi.winline.award.value.setter.model;


import com.gamesinc.izanagi.data.storage.service.DataStorageDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Winline value element, i.e a position on reel that can potentially
 * have the value assigned to it in DS.
 * So for each symbol id defined the place in DS, where
 * the value assigned to this symbol id position stores.
 */
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
public class WinlineValueElement {
    private final Integer symbolId;
    private final DataStorageDescription storageDescription;

}
