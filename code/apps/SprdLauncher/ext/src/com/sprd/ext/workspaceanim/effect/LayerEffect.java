/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.view.View;

public class LayerEffect extends EffectInfo{

    public LayerEffect(int id) {
        super(id);
    }

    @Override
    public void getTransformationMatrix(View view ,float offset, int pageWidth, int pageHeight,float distance){
        float absOffset = Math.abs(offset);
        float mAlpha = 1.0F - absOffset;

        int mViewWidth = pageWidth;
        int mViewHeight = pageHeight;

        int mViewHalfWidth = mViewWidth >>  1;
        int mViewHalfHeight = mViewHeight >> 1;

        float level = 0.4F * (1.0F - offset);
        float scale = 0.6F + level;
        float xPost = 0.4F * offset* mViewWidth * 3.0F;
        float yPost = 0.4F * offset* mViewHeight * 0.5F;

        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setTranslationX(xPost);
        view.setTranslationY(0);
        view.setPivotY(mViewHalfHeight);
        view.setPivotX(mViewHalfWidth);
        view.setRotationY(0f);
        view.setRotationX(0f);
        view.setRotation(0f);
        view.setAlpha(mAlpha);
    }
}
