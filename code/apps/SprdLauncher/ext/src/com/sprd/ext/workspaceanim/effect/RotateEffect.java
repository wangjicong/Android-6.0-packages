/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.view.View;

public class RotateEffect extends EffectInfo{

    public RotateEffect(int id) {
        super(id);
    }

    @Override
    public void getTransformationMatrix(View view ,float offset, int pageWidth, int pageHeight,float distance){
        int mViewWidth = pageWidth;
        int mViewHeight = pageHeight;


        float xPost = 0f;

        float offsetDegree = -offset * 20.0F;
        view.setTranslationX(0f);
        view.setPivotY(mViewHeight);
        view.setPivotX(mViewWidth / 2.0f);
        view.setRotation(offsetDegree);
        view.setTranslationX(xPost);
        view.setRotationX(0f);
        view.setRotationY(0f);
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(1f);


    }
}
