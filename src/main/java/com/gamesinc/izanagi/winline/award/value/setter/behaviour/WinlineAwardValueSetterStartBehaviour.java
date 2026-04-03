package com.gamesinc.izanagi.winline.award.value.setter.behaviour;


import com.gamesinc.component.slot.award.Award;
import com.gamesinc.component.slot.award.WinlineAward;
import com.gamesinc.component.slot.reel.Symbol;
import com.gamesinc.feature.core.FeatureBehaviour;
import com.gamesinc.feature.core.FeatureState;
import com.gamesinc.izanagi.data.storage.service.DataStorageDescription;
import com.gamesinc.izanagi.data.storage.service.DataStorageUtil;
import com.gamesinc.izanagi.exception.EmptyOrNullFeatureParameterException;
import com.gamesinc.izanagi.matches.util.MatchesUtil;
import com.gamesinc.izanagi.winline.award.value.setter.model.WinlineAwardValueSetter;
import com.gamesinc.izanagi.winline.award.value.setter.model.WinlineAwardValueSetterState;
import com.gamesinc.izanagi.winline.award.value.setter.model.WinlineValueElement;
import com.gamesinc.lzanagi.behaviour.factory.annotation.AutoBehaviour;
import com.gamesinc.lzanagi.common.FeatureUtil;
import com.gamesinc.reel.util.ReelSymbolsUtil;
import com.gamesinc.slot.state.core.GameCycle;
import com.gamesinc.slot.state.core.GameState;
import com.gamesinc.slot.state.core.SlotBet;
import com.gamesinc.slot.state.core.Spin;
import com.gamesinc.slot.state.core.reel.ReelAwardMatch;
import com.gamesinc.slot.state.core.reel.ReelSymbols;
import com.gamesinc.slot.state.core.win.PrizeWin;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This START Behaviour runs only if found EXACT matches based on of feature configuration
 * Calculates value for these EXACT matches by reading additivies and multipliers
 * for value based on the symbol id participated in each winline.
 * <p>
 * If flag #removeFromDSExactMatchesPoints is true in feature in {@link WinlineAwardValueSetter}
 * remove from DS values for used multipliers/additives
 * <p>
 * Flow:
 * 1 read EXACT matches from state
 * 2 per each match read it's point
 * 3 for each point of match read it's value (multiplier / additive).
 * Add each point to the state #symbolPlacesOnReels field
 * 4 calculate total value by multiplying multiplier to the sum of additives
 * 5 make a deep copy of this match with updated award value.
 * Award value is getting from step 4 calculation
 * 6 Update matches of the spin
 * 7 If flag #removeFromDSExactMatchesPoints is true:
 * 7 1 Read from state #pointStoragePlace map
 * 7 2 Per each point in #symbolPlacesOnReels read storage for this point
 * 7 3 Remove from this storage data of this point
 */

@AutoBehaviour(type = WinlineAwardValueSetter.PROPERTY_SUB_TYPE_LITERAL, state = FeatureBehaviour.State.START)
public class WinlineAwardValueSetterStartBehaviour extends FeatureBehaviour<GameState> {
    private static Logger LOGGER = LoggerFactory.getLogger(WinlineAwardValueSetterStartBehaviour.class);
    private static final int EXTRA_WINS_PRESENTATION_TYPE = 0;
    private final ReelSymbolsUtil reelSymbolUtil;
    private final FeatureUtil featureUtil;
    private final MatchesUtil matchesUtil;

    public WinlineAwardValueSetterStartBehaviour() {
        super(State.START);
        reelSymbolUtil = new ReelSymbolsUtil();
        featureUtil = new FeatureUtil();
        matchesUtil = new MatchesUtil();
    }

    @Override
    public String getFeatureType() {
        return WinlineAwardValueSetter.PROPERTY_SUB_TYPE_LITERAL;
    }

    @Override
    public boolean execute(JSONObject jsonObject, GameState gameState, FeatureState featureState) {
        featureState.setCurrentEventState(State.COMPLETE);
        GameCycle gameCycle = gameState.getGameCycle();
        SlotBet bet = (SlotBet) gameCycle.getCurrentGameBet();
        Spin spin = bet.getCurrentSpin();
        WinlineAwardValueSetterState state = (WinlineAwardValueSetterState) featureState;
        WinlineAwardValueSetter feature = state.getFeature();

        //0 add point value map and point storage place map for multipliers
        Map<Point, Double> additives = addPointValuesToState(feature.getAdditives(), gameState, state);
        state.setAdditivesPointValueMap(additives);
        Map<Point, Double> multipliers = addPointValuesToState(feature.getMultipliers(), gameState, state);
        state.setMultipliersPointValueMap(multipliers);

        //HARDCODED FROM ACTIVATION BEHAVIOUR
        ReelSymbols currentReelSymbols = gameCycle.getCurrentReelSymbols(bet.getCurrentSpin().getIndex());
        Symbol[][] symbolsInPlay = reelSymbolUtil.getSymboslInPlay(currentReelSymbols, true);
        Map<ReelAwardMatch.MatchResult, List<ReelAwardMatch>> newMatches = featureUtil.prepareNewMatches(feature, symbolsInPlay, bet);
        state.setNewMatches(newMatches);

        // * 1 read EXACT matches from state
        List<ReelAwardMatch> newExactMatches = state.getNewMatches().get(ReelAwardMatch.MatchResult.EXACT);
        newExactMatches.forEach(match -> {
            // * 2 calculate new award value
            double value = calculateTotalAwardValueForMatch(feature.getAdditiveDefaultValue(),
                    feature.getMultiplierDefaultValue(), match, state.getAdditivesPointValueMap(),
                    state.getMultipliersPointValueMap());
            //cast to SlotWinlineAward directly cause feature accepts only this type of award
            WinlineAward award = (WinlineAward) match.getAward();
            Award modifiedValueAward = matchesUtil.awardModifiedCopyWithValue(award, EXTRA_WINS_PRESENTATION_TYPE, value);
            match.setAward(modifiedValueAward);
            match.setFeatureId(feature.getFeatureId());

            // * 3 add match points to the #symbolPlacesOnReels state field
            state.getSymbolPlacesOnReels().addAll(match.getPoints());
        });

        List<ReelAwardMatch> newMissMatches = state.getNewMatches().get(ReelAwardMatch.MatchResult.MISS);
        List<ReelAwardMatch> newNearMatches = state.getNewMatches().get(ReelAwardMatch.MatchResult.NEAR);
        finishMatches(newMissMatches, feature.getFeatureId());
        finishMatches(newNearMatches, feature.getFeatureId());

        //* 4 Add new matches to the spin
        matchesUtil.addExtraMatchesToSpin(spin, feature.getFeatureId(), state.getNewMatches());

        //* 5 Delete used positions of winlines if flag #removeFromDSExactMatchesPoints is true
        if (feature.getRemoveFromDSExactMatchesPoints()) {
            Set<Point> usedPointsOfWinlines = state.getSymbolPlacesOnReels();
            DataStorageUtil.deleteUsedPointsFromStorages(usedPointsOfWinlines, gameState, state.getPointStoragePlace());
        }

        spin.getWins().add(new PrizeWin(0, feature.getFeatureId()));
        LOGGER.debug("start, feature id: " + feature.getFeatureId());
        return true;
    }

    /**
     * Calculate total award value for posted match by comparing
     * match positions with positions maps assigned to multipliers value.
     * Total award value is total sum of additive positions of match
     * multiplying to multiplication multipliers. Expected each position of match
     * has the value assigned to it.
     * For example posted match A with next positions: p1: 0:0, p2: 1:0, p3: 2:0
     * In additive map there are next entries: 0:0 - 2, 1:0 - 3, 2:0 - 4, 4:1 -  5
     * Multiplier map is empty.
     * Total additives is sum of all additives for match positions: 2+3+4 =9
     * Cause multiplier map is empty, total multipliers is  default value X : X
     * <p>
     * Total award value for match A is 9*X = 9X, so this value would be returned as
     * total award value for posted match
     * <p>
     * Set protected access for testing purpose
     *
     * @param additiveDefaultValue   default value for additive multiplier (total one), if no additive
     *                               multiplier found on reels
     * @param multiplierDefaultValue default value for multiplication multiplier (total one), if no multiplication
     *                               *                      multiplier found on reels
     * @param match                  target match for what award value  is being calculated
     * @param additivesValues        position-multipliers map (for additive values),
     *                               where K - position of symbol on reels,
     *                               V - multiplier assigned to these positions. If this map keys
     *                               participated in matches their assigned values can be used as a part of total
     *                               value for award for winline used for this match
     * @param multipliersValues      same position-multipliers map as for addivites,
     *                               but contains data for multiplications
     * @return total award value calculated for posted match
     */
    protected double calculateTotalAwardValueForMatch(Double additiveDefaultValue,
                                                      Double multiplierDefaultValue,
                                                      ReelAwardMatch match,
                                                      Map<Point, Double> additivesValues,
                                                      Map<Point, Double> multipliersValues) {
        if (match == null || additivesValues == null || multipliersValues == null) {
            throw new EmptyOrNullFeatureParameterException("Match or/and multipliers/additives map is null!");
        }
        double totalAdditives = calculateAdditiveTotalValueForMatchPositions(additiveDefaultValue, match, additivesValues);
        double totalMultipliers = calculateMultiplierTotalValueForMatchPositions(multiplierDefaultValue, match, multipliersValues);
        double result = totalMultipliers * totalAdditives;
        return result;
    }

    private double calculateAdditiveTotalValueForMatchPositions(
            Double additiveDefaultValue,
            ReelAwardMatch match,
            Map<Point, Double> additivesValues) {
        List<Point> points = match.getPoints();
        LOGGER.debug("points of match: " + points);
        Double totalAdditive;
        if (!additivesValues.isEmpty()) {
            totalAdditive = additivesValues.entrySet().stream()
                    .filter(e -> {
                        Point additivePosition = e.getKey();
                        LOGGER.debug("additive position: " + additivePosition);
                        LOGGER.debug("additive value: " + e.getValue());
                        boolean isPointPartOfMatch = points.contains(e.getKey());
                        LOGGER.debug("point is part of match:  " + isPointPartOfMatch);
                        return isPointPartOfMatch;
                    })
                    .map(Map.Entry::getValue).mapToDouble(Double::doubleValue).sum();
        } else totalAdditive = additiveDefaultValue;
        LOGGER.debug("total additive: " + totalAdditive);
        return totalAdditive;
    }

    private double calculateMultiplierTotalValueForMatchPositions(
            Double multiplierDefaultValue,
            ReelAwardMatch match,
            Map<Point, Double> multipliersValue) {
        List<Point> points = match.getPoints();
        Double totalMultiplier;
        if (!multipliersValue.isEmpty()) {
            totalMultiplier = multipliersValue.entrySet().stream()
                    .filter(e -> points.contains(e.getKey()))
                    .map(Map.Entry::getValue).mapToDouble(Double::doubleValue).reduce(1.0, (a, b) -> a * b);
        } else totalMultiplier = multiplierDefaultValue;
        LOGGER.debug("total multiplier: " + totalMultiplier);
        return totalMultiplier;
    }

    private Map<Point, Double> addPointValuesToState(Set<WinlineValueElement> elements,
                                                     GameState gameState,
                                                     WinlineAwardValueSetterState state) {
        Map<Point, Double> multipliersValues = new HashMap<>();
        for (WinlineValueElement element : elements) {
            DataStorageDescription storageDescription = element.getStorageDescription();
            multipliersValues.putAll(DataStorageUtil.preparePointDoubleMapFromDS(storageDescription, gameState));
            state.getPointStoragePlace().add(element.getStorageDescription());
        }
        return multipliersValues;
    }

    private void finishMatches(List<ReelAwardMatch> newMissMatches, int featureId) {
        newMissMatches.forEach(m -> {
            WinlineAward award = (WinlineAward) m.getAward();
            Award modifiedValueAward = matchesUtil.awardModifiedCopy(award, EXTRA_WINS_PRESENTATION_TYPE, featureId);
            m.setAward(modifiedValueAward);
            m.setFeatureId(featureId);
        });
    }

}
