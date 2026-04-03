package com.gamesinc.izanagi.winline.award.value.setter.view;

import com.gamesinc.feature.core.FeatureState;
import com.gamesinc.izanagi.winline.award.value.setter.WinlineAwardValueSetter;
import com.gamesinc.izanagi.winline.award.value.setter.WinlineAwardValueSetterState;
import com.gamesinc.lzanagi.common.json.AutoBehaviourView;
import com.gamesinc.lzanagi.common.json.view.IFeatureStateViewGenerator;
import com.gamesinc.lzanagi.common.json.view.PointsView;
import com.gamesinc.lzanagi.common.json.view.ViewGenerator;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


@AutoBehaviourView(type = WinlineAwardValueSetter.PROPERTY_SUB_TYPE_LITERAL)
public class WinlineAwardValueSetterViewGenerator implements IFeatureStateViewGenerator {

    @Override
    public JSONObject toJSON(FeatureState featureState, ViewGenerator.Event event) throws JSONException {
        JSONObject json = IFeatureStateViewGenerator.super.toJSON(featureState, event);
        WinlineAwardValueSetterState state = (WinlineAwardValueSetterState) featureState;
        List<Point> symblolPlaces = new ArrayList<>(state.getSymbolPlacesOnReels());
        json.put("pos", PointsView.pointsToJSON(symblolPlaces));
        return json;
    }

}
