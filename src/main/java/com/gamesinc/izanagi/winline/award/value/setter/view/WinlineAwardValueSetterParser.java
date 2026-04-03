package com.gamesinc.izanagi.winline.award.value.setter.view;


import com.gamesinc.feature.core.Feature;
import com.gamesinc.feature.core.FeatureState;
import com.gamesinc.feature.core.model.AutoFeatureParser;
import com.gamesinc.feature.core.model.IFeatureStateParser;
import com.gamesinc.izanagi.winline.award.value.setter.model.WinlineAwardValueSetter;
import com.gamesinc.izanagi.winline.award.value.setter.model.WinlineAwardValueSetterState;
import org.json.JSONException;
import org.json.JSONObject;

@AutoFeatureParser(type = WinlineAwardValueSetter.PROPERTY_SUB_TYPE_LITERAL)
public class WinlineAwardValueSetterParser implements IFeatureStateParser {

    @Override
    public FeatureState parseFeatureState(JSONObject jsonObject, Feature feature) throws JSONException {
        return (WinlineAwardValueSetterState) feature.getFeatureState();
    }

}
