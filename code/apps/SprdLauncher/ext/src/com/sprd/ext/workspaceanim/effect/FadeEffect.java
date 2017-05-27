/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.view.View;

public class FadeEffect extends EffectInfo{

    public FadeEffect(int id) {
        super(id);
    }

    @Override
    public void getTransformationMatrix(View view ,float offset, int pageWidth, int pageHeight,float distance){
        float absOffset = Math.abs(offset);
        float mAlpha = 1.0F - absOffset;

        int mViewWidth = pageWidth;
        int mViewHeight = pageHeight;

        int mViewHalfWidth = mViewWidth >> 1;
        int mViewHalfHeight = mViewHeight >> 1;

        float xPost = 0f;

        view.setTranslationX(xPost);
        view.setPivotY(mViewHalfHeight);
        view.setPivotX(mViewHalfWidth);
        view.setRotationX(0f);
        view.setRotation(0f);
        view.setRotationY(0f);
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);

        view.setAlpha(mAlpha);
    }

}
