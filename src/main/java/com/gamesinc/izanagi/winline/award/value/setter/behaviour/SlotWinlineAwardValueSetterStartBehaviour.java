package com.gamesinc.izanagi.winline.award.value.setter.behaviour;


import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This START Behaviour runs only if found EXACT matches based on of feature configuration
 * Calculates value for these EXACT matches by reading additivies and multipliers
 * for value based on the symbol id participated in each winline.
 * <p>
 * If flag #removeFromDSExactMatchesPoints is true in feature in {@link SlotWinlineAwardValueSetter}
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

@Dependent
public class SlotWinlineAwardValueSetterStartBehaviour extends SlotFeatureBehaviour {
    private static Logger logger = LoggerFactory.getLogger(SlotWinlineAwardValueSetterStartBehaviour.class);
    private static final int EXTRA_WINS_PRESENTATION_TYPE = 0;
    private @Inject
    SlotReelSymbolsUtil reelSymbolUtil;

    private @Inject
    SlotReelMatchCollector reelMatchCollector;

    private @Inject
    SlotFeatureUtil slotFeatureUtil;

    public SlotWinlineAwardValueSetterStartBehaviour() {
        super(State.START);
    }

    @Override
    public String getFeatureType() {
        return SlotWinlineAwardValueSetter.PROPERTY_SUB_TYPE_LITERAL;
    }

    @Override
    public boolean execute(JSONObject event, SlotGameState gameState, SlotFeatureState slotFeatureState) {
        slotFeatureState.setCurrentEventState(State.COMPLETE);
        SlotGameCycle gameCycle = gameState.getGameCycle();
        SlotBet bet = gameCycle.getCurrentGameBet();
        SlotSpin spin = bet.getCurrentSpin();
        SlotWinlineAwardValueSetterState state = (SlotWinlineAwardValueSetterState) slotFeatureState;
        SlotWinlineAwardValueSetter feature = state.getFeature();

        //0 add point value map and point storage place map for multipliers
        Map<Point, Double> additives = addPointValuesToState(feature.getAdditives(), gameState, state);
        state.setAdditivesPointValueMap(additives);
        Map<Point, Double> multipliers = addPointValuesToState(feature.getMultipliers(), gameState, state);
        state.setMultipliersPointValueMap(multipliers);

        //HARDCODED FROM ACTIVATION BEHAVIOUR
        SlotReelSymbols currentReelSymbols = gameCycle.getCurrentReelSymbols(bet.getCurrentSpin().getIndex());
        SlotSymbol[][] symbolsInPlay = reelSymbolUtil.getSymboslInPlay(currentReelSymbols, true);
        Map<SlotReelAwardMatch.MatchResult, List<SlotReelAwardMatch>> newMatches = reelSymbolUtil.prepareNewMatches(feature, symbolsInPlay, bet);
        state.setNewMatches(newMatches);

        // * 1 read EXACT matches from state
        List<SlotReelAwardMatch> newExactMatches = state.getNewMatches().get(SlotReelAwardMatch.MatchResult.EXACT);
        newExactMatches.forEach(match->{
            // * 2 calculate new award value
            double value = calculateTotalAwardValueForMatch(feature.getAdditiveDefaultValue(),
                    feature.getMultiplierDefaultValue(), match, state.getAdditivesPointValueMap(),
                    state.getMultipliersPointValueMap());
            //cast to SlotWinlineAward directly cause feature accepts only this type of award
            SlotWinlineAward award = (SlotWinlineAward) match.getAward();
            SlotAward modifiedValueAward = slotFeatureUtil.awardModifiedCopyWithValue(award, EXTRA_WINS_PRESENTATION_TYPE, value);
            match.setAward(modifiedValueAward);
            match.setFeatureId(feature.getFeatureId());

            // * 3 add match points to the #symbolPlacesOnReels state field
            state.getSymbolPlacesOnReels().addAll(match.getPoints());
        });

        List<SlotReelAwardMatch> newMissMatches = state.getNewMatches().get(SlotReelAwardMatch.MatchResult.MISS);
        List<SlotReelAwardMatch> newNearMatches = state.getNewMatches().get(SlotReelAwardMatch.MatchResult.NEAR);
        finishMatches(newMissMatches, feature.getFeatureId());
        finishMatches(newNearMatches, feature.getFeatureId());

        //* 4 Add new matches to the spin
        slotFeatureUtil.addExtraMatchesToSpin(spin, feature.getFeatureId(), state.getNewMatches());

        //* 5 Delete used positions of winlines if flag #removeFromDSExactMatchesPoints is true
        if (feature.getRemoveFromDSExactMatchesPoints()) {
            Set<Point> usedPointsOfWinlines = state.getSymbolPlacesOnReels();
            deleteUsedPointsFromStorages(usedPointsOfWinlines, gameState, state.getPointStoragePlace());
        }

        spin.getWins().add(new SlotPrizeWin(0, feature.getFeatureId()));
        logger.debug("start, feature id: " + feature.getFeatureId());
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
     *
     * Total award value for match A is 9*X = 9X, so this value would be returned as
     * total award value for posted match
     *
     * Set protected access for testing purpose
     * @param additiveDefaultValue default value for additive multiplier (total one), if no additive
     *                             multiplier found on reels
     * @param multiplierDefaultValue default value for multiplication multiplier (total one), if no multiplication
     *      *                      multiplier found on reels
     * @param match target match for what award value  is being calculated
     * @param additivesValues position-multipliers map (for additive values),
     *                       where K - position of symbol on reels,
     *                       V - multiplier assigned to these positions. If this map keys
     *                        participated in matches their assigned values can be used as a part of total
     *                        value for award for winline used for this match
     * @param multipliersValues same position-multipliers map as for addivites,
     *                         but contains data for multiplications
     * @return total award value calculated for posted match
     */
    protected double calculateTotalAwardValueForMatch(Double additiveDefaultValue,
                                                    Double multiplierDefaultValue,
                                                    SlotReelAwardMatch match,
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
            SlotReelAwardMatch match,
            Map<Point, Double> additivesValues) {
        List<Point> points = match.getPoints();
        logger.debug("points of match: " + points);
        Double totalAdditive;
        if (!additivesValues.isEmpty()) {
            totalAdditive = additivesValues.entrySet().stream()
                    .filter(e -> {
                        Point additivePosition = e.getKey();
                        logger.debug("additive position: " + additivePosition);
                        logger.debug("additive value: " + e.getValue());
                        boolean isPointPartOfMatch = points.contains(e.getKey());
                        logger.debug("point is part of match:  " + isPointPartOfMatch);
                        return isPointPartOfMatch;
                    })
                    .map(Map.Entry::getValue).mapToDouble(Double::doubleValue).sum();
        } else totalAdditive = additiveDefaultValue;
        logger.debug("total additive: " + totalAdditive);
        return totalAdditive;
    }

    private double calculateMultiplierTotalValueForMatchPositions(
            Double multiplierDefaultValue,
            SlotReelAwardMatch match,
            Map<Point, Double> multipliersValue) {
        List<Point> points = match.getPoints();
        Double totalMultiplier;
        if (!multipliersValue.isEmpty()) {
            totalMultiplier = multipliersValue.entrySet().stream()
                    .filter(e -> points.contains(e.getKey()))
                    .map(Map.Entry::getValue).mapToDouble(Double::doubleValue).reduce(1.0, (a, b) -> a * b);
        } else totalMultiplier = multiplierDefaultValue;
        logger.debug("total multiplier: " + totalMultiplier);
        return totalMultiplier;
    }

    private Map<Point, Double> addPointValuesToState(Set<SlotWinlineValueElement> elements,
                                                     SlotGameState gameState,
                                                     SlotWinlineAwardValueSetterState state) {
        Map<Point, Double> multipliersValues = new HashMap<>();
        for (SlotWinlineValueElement element : elements) {
            SlotDataStorageDescription storageDescription = element.getStorageDescription();
            multipliersValues.putAll(slotFeatureUtil.preparePointDoubleMapFromDS(storageDescription, gameState));
            state.getPointStoragePlace().add(element.getStorageDescription());
        }
        return multipliersValues;
    }

    private void finishMatches(List<SlotReelAwardMatch> newMissMatches, int featureId) {
        newMissMatches.forEach(m->{
            SlotWinlineAward award = (SlotWinlineAward) m.getAward();
            SlotAward modifiedValueAward = slotFeatureUtil.awardModifiedCopy(award, EXTRA_WINS_PRESENTATION_TYPE, featureId);
            m.setAward(modifiedValueAward);
            m.setFeatureId(featureId);
        });
    }

    private void deleteUsedPointsFromStorages(Set<Point> usedPointsOfWinlines,
                                  SlotGameState gameState,
                                  Collection<SlotDataStorageDescription> storages) {
        Set<String> usedPointsStrFormat = usedPointsOfWinlines.stream()
                .map(PointUtil::castPointToStringArr).collect(Collectors.toSet());

        //Delete from DS by key
        storages.forEach(storage->{
            SlotDataStorageState dsState = slotFeatureUtil.getDataStorageState(gameState, storage.getDataStorageId());
            Map<String, Double> existingDetails = (Map<String, Double>) dsState.getExistingDetails(storage.getPackageId(), storage.getDataId());
            existingDetails.entrySet().removeIf(e->{
                boolean fountInStorage = usedPointsStrFormat.contains(e.getKey());
                if (logger.isDebugEnabled() && fountInStorage){
                    logger.debug("removed points: " + e.getKey() + " from storage: " + storage);
                }
                return fountInStorage;
            });
            dsState.storeData(storage.getPackageId(), storage.getDataId(), existingDetails);
        });
    }

}
