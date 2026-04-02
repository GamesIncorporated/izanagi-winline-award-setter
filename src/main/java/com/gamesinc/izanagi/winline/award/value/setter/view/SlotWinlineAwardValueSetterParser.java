package com.gamesinc.izanagi.winline.award.value.setter.view;


import javax.enterprise.context.Dependent;
import org.json.JSONException;
import org.json.JSONObject;

@Dependent
public class SlotWinlineAwardValueSetterParser implements ISlotFeatureStateParser {

    @Override
    public SlotWinlineAwardValueSetterState parseFeatureState(JSONObject featureStateJSON, SlotFeature feature) throws JSONException {
        return (SlotWinlineAwardValueSetterState) feature.getFeatureState();
    }

}
