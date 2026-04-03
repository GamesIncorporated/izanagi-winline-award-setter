package com.gamesinc.izanagi.winline.award.value.setter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gamesinc.component.slot.award.AwardConfiguration;
import com.gamesinc.component.slot.feature.ReelFeature;
import com.gamesinc.component.slot.reel.ReelBandConfiguration;
import com.gamesinc.component.slot.winline.WinlineConfiguration;
import com.gamesinc.feature.core.FeatureState;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Set the value for configured winlines by reading the symbol
 * ids positions participated in classic reel wins and their
 * value assigned in data storage.
 * Calculates the total value for match award by summing all additives values
 * (filter by symbol id) and multiplies it to the multiplication values
 *
 */
@NoArgsConstructor(force = true)
@Getter
public class WinlineAwardValueSetter extends ReelFeature {

    public static final String PROPERTY_SUB_TYPE_LITERAL = "WINLINE_AWARD_VALUE_SETTER";

    //flag if true - remove from DS points participated in exact matches
    //(of reel wins), while removing use their position as a key
    private final Boolean removeFromDSExactMatchesPoints;

    //Set of winline elements that are additives on reels
    private final Set<WinlineValueElement> additives;

    //Set of winline elements that are multipliers on reels
    private final Set<WinlineValueElement> multipliers;

    //Default value for additive total value if additives are NOT found on reels
    private final Double additiveDefaultValue;

    //Default value for multiplication total value if additives are NOT found on reels
    private final Double multiplierDefaultValue;


    public WinlineAwardValueSetter(int featureId,
                                   PresentationType presentationType,
                                   ActivationPoint activationPoint,
                                   AwardConfiguration awardConfiguration,
                                   ReelBandConfiguration reelBandConfiguration,
                                   WinlineConfiguration winlineConfiguration,
                                   Boolean removeFromDSExactMatchesPoints,
                                   Set<WinlineValueElement> additives,
                                   Set<WinlineValueElement> multipliers,
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

    @JsonIgnore
    @Override
    public FeatureState getFeatureState() {
        return new WinlineAwardValueSetterState(this);
    }

}
