package com.gamesinc.izanagi.winline.award.value.setter.behaviour;


import javax.enterprise.context.Dependent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Activates feature with no conditions
 */
@Dependent
public class SlotWinlineAwardValueSetterActivationBehaviour extends SlotFeatureBehaviour {
    private static Logger logger = LoggerFactory.getLogger(SlotWinlineAwardValueSetterActivationBehaviour.class);

    public SlotWinlineAwardValueSetterActivationBehaviour() {
        super(State.ACTIVATION);
    }

    @Override
    public String getFeatureType() {
        return SlotWinlineAwardValueSetter.PROPERTY_SUB_TYPE_LITERAL;
    }


    @Override
    public boolean execute(JSONObject event, SlotGameState gameState, SlotFeatureState slotFeatureState) {
        slotFeatureState.setCurrentEventState(State.ACTIVATION);
        SlotWinlineAwardValueSetterState state = (SlotWinlineAwardValueSetterState) slotFeatureState;
        SlotWinlineAwardValueSetter feature = state.getFeature();
        logger.debug("activated, feature id: " + feature.getFeatureId());
        return true;
    }

}
