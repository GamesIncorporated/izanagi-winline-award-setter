package com.gamesinc.izanagi.winline.award.value.setter.behaviour;


import javax.enterprise.context.Dependent;

import com.gamesinc.feature.core.FeatureBehaviour;
import com.gamesinc.feature.core.FeatureState;
import com.gamesinc.izanagi.winline.award.value.setter.WinlineAwardValueSetter;
import com.gamesinc.lzanagi.behaviour.factory.annotation.AutoBehaviour;
import com.gamesinc.slot.state.core.GameState;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activates feature with no conditions
 */

@AutoBehaviour(type = WinlineAwardValueSetter.PROPERTY_SUB_TYPE_LITERAL, state = FeatureBehaviour.State.ACTIVATION)
@Dependent
public class WinlineAwardValueSetterActivationBehaviour extends FeatureBehaviour<GameState> {
    private static Logger LOGGER = LoggerFactory.getLogger(WinlineAwardValueSetterActivationBehaviour.class);

    public WinlineAwardValueSetterActivationBehaviour() {
        super(State.ACTIVATION);
    }

    @Override
    public String getFeatureType() {
        return WinlineAwardValueSetter.PROPERTY_SUB_TYPE_LITERAL;
    }

    @Override
    public boolean execute(JSONObject jsonObject, GameState gameState, FeatureState featureState) {
        LOGGER.debug("Feature: " + featureState.getFeature().getFeatureId() + " is activated");
        return true;
    }

}
