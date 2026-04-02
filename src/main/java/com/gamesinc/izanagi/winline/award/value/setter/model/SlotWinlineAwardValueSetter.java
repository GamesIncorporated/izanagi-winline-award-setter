package com.gamesinc.izanagi.winline.award.value.setter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.Set;

/**
 * Set the value for configured winlines by reading the symbol
 * ids positions participated in classic reel wins and their
 * value assigned in data storage.
 * Calculates the total value for match award by summing all additives values
 * (filter by symbol id) and multiplies it to the multiplication values
 *
 */
public class SlotWinlineAwardValueSetter extends SlotReelFeature {

    public static final String PROPERTY_SUB_TYPE_LITERAL = "WINLINE_AWARD_VALUE_SETTER";

    //flag if true - remove from DS points participated in exact matches
    //(of reel wins), while removing use their position as a key
    private final Boolean removeFromDSExactMatchesPoints;

    //Set of winline elements that are additives on reels
    private final Set<SlotWinlineValueElement> additives;

    //Set of winline elements that are multipliers on reels
    private final Set<SlotWinlineValueElement> multipliers;

    //Default value for additive total value if additives are NOT found on reels
    private final Double additiveDefaultValue;

    //Default value for multiplication total value if additives are NOT found on reels
    private final Double multiplierDefaultValue;
    public SlotWinlineAwardValueSetter() {
        removeFromDSExactMatchesPoints = false;
        additives = Collections.emptySet();
        multipliers = Collections.emptySet();
        multiplierDefaultValue = 1d;
        additiveDefaultValue = 0d;
    }

    public SlotWinlineAwardValueSetter(int featureId,
                                       PresentationType presentationType,
                                       ActivationPoint activationPoint,
                                       SlotAwardConfiguration awardConfiguration,
                                       SlotReelBandConfiguration reelBandConfiguration,
                                       SlotWinlineConfiguration winlineConfiguration,
                                       Boolean removeFromDSExactMatchesPoints,
                                       Set<SlotWinlineValueElement> additives,
                                       Set<SlotWinlineValueElement> multipliers,
                                       Double additiveDefaultValue, Double multiplierDefaultValue) {
        super(PROPERTY_SUB_TYPE_LITERAL,
                featureId,
                presentationType,
                activationPoint,
                StateLifeCycle.CONSUMABLE_COMPLETE_UPDATE,
                awardConfiguration, 
                reelBandConfiguration,
                winlineConfiguration);
        this.removeFromDSExactMatchesPoints = removeFromDSExactMatchesPoints;
        this.additives = additives;
        this.multipliers = multipliers;
        this.additiveDefaultValue = additiveDefaultValue;
        this.multiplierDefaultValue = multiplierDefaultValue;
    }

    public Boolean getRemoveFromDSExactMatchesPoints() {
        return removeFromDSExactMatchesPoints;
    }

    public Set<SlotWinlineValueElement> getAdditives() {
        return additives;
    }

    public Set<SlotWinlineValueElement> getMultipliers() {
        return multipliers;
    }

    public Double getAdditiveDefaultValue() {
        return additiveDefaultValue;
    }

    public Double getMultiplierDefaultValue() {
        return multiplierDefaultValue;
    }

    @JsonIgnore
    @Override
    public SlotFeatureState getFeatureState() {
        return new SlotWinlineAwardValueSetterState(this);
    }

}
