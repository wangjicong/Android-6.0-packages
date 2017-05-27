/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.content.Context;
import android.content.SharedPreferences;

import com.sprd.ext.workspaceanim.WorkspaceEffect;

import java.util.ArrayList;
import java.util.List;

public class EffectFactory {

    private static List<EffectInfo> allEffects = new ArrayList<EffectInfo>();
    public static List<EffectInfo> getAllEffects(){
        return loadEffectsList();
    }
    public static EffectInfo getEffect(int id){

        if(allEffects.isEmpty()){
            loadEffectsList();
        }
        for (int i = 0,count = allEffects.size(); i < count; i++) {
            EffectInfo eInfo = allEffects.get(i);
            if(eInfo.id == id){
                return eInfo;
            }
        }
        return null;
    }
    public static EffectInfo getCurrentEffect(Context context ){
        SharedPreferences mSpaceTypeShared = WorkspaceEffect.getmAnimSharePref(context);
        int id = mSpaceTypeShared.getInt(WorkspaceEffect.KEY_ANIMATION_STYLE, 0);

        for (int i = 0,count = allEffects.size(); i < count; i++) {
            EffectInfo eInfo = allEffects.get(i);
            if(eInfo.id == id){
                return eInfo;
            }
        }
        return null;
    }
    private static List<EffectInfo> loadEffectsList(){
        allEffects.clear();

        NormalEffect normalEffect = new NormalEffect(0);
        allEffects.add(normalEffect);

        CrossEffect crossEffect = new CrossEffect(1);
        allEffects.add(crossEffect);

        PageEffect pageEffect = new PageEffect(2);
        allEffects.add(pageEffect);

        CubeEffect cubeInEffect = new CubeEffect(3, true);
        allEffects.add(cubeInEffect);

        CubeEffect cubeOutEffect = new CubeEffect(4, false);
        allEffects.add(cubeOutEffect);

        CarouselEffect carouselLeftEffect = new CarouselEffect(5, true);
        allEffects.add(carouselLeftEffect);

        CarouselEffect carouselRightEffect = new CarouselEffect(6, false);
        allEffects.add(carouselRightEffect);


//        RotateEffect rotateEffect = new RotateEffect(7);
//        allEffects.add(rotateEffect);

        LayerEffect layerEffect = new LayerEffect(7);
        allEffects.add(layerEffect);

        FadeEffect fadeEffect = new FadeEffect(8);
        allEffects.add(fadeEffect);

        return allEffects;
    }
}
