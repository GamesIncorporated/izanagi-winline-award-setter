package com.gamesinc.izanagi.winline.award.value.setter;


import com.gamesinc.feature.core.Feature;
import com.gamesinc.feature.core.FeatureState;
import com.gamesinc.izanagi.data.storage.service.DataStorageDescription;
import com.gamesinc.slot.state.core.reel.ReelAwardMatch;
import lombok.Getter;
import lombok.Setter;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class WinlineAwardValueSetterState extends FeatureState {

    /**
     * Places(positions) of symbols participated in EXACT matches
     */
    private Set<Point> symbolPlacesOnReels = new HashSet<>();

    /**
     * Position-value map
     *
     * Map, where K - position on reel of additive element, V - value assigned to the position
     */
    private Map<Point, Double> additivesPointValueMap = new HashMap<>();

    /**
     * Position-value map
     *
     * Map, where K - position on reel of multiplier element, V - value assigned to the position
     */
    private Map<Point, Double> multipliersPointValueMap = new HashMap<>();

    /**
     * Storages of points values
     *
     * V - storage in DS
     *
     * This collection can be used to delete from it used values from DS/from reels
     */
    private Set<DataStorageDescription> pointStoragePlace = new HashSet<>();

    /**
     * Matches calculated from the feature
     */
    private Map<ReelAwardMatch.MatchResult, List<ReelAwardMatch>> newMatches;

    public WinlineAwardValueSetterState(Feature feature) {
        super(feature);
    }

    @Override
    public WinlineAwardValueSetter getFeature() {
        return (WinlineAwardValueSetter) super.getFeature();
    }

}
