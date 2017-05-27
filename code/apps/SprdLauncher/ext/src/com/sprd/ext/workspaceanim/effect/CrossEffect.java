/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.view.View;

public class CrossEffect extends EffectInfo {

    public CrossEffect(int id) {
        super(id);
    }

    @Override
    public void getTransformationMatrix(View view, float offset, int pageWidth, int pageHeight, float distance) {
        float absOffset = Math.abs(offset);
        float mAlpha = 1.0F - absOffset * 0.8f;

        int mViewWidth = pageWidth;
        int mViewHeight = pageHeight;

//        int mViewHalfWidth = mViewWidth >> 1;
        int mViewHalfHeight = mViewHeight >> 1;

        float yRotate = 90.0F * (-offset);

        float xPost = mViewWidth * offset;
        view.setCameraDistance(distance);
        if (offset == -1 || offset == 0 || offset == 1) {
            view.setTranslationX(xPost);
            view.setRotationY(0);
            view.setAlpha(1f);
            view.setPivotY(mViewHalfHeight);
        } else {
            view.setTranslationX(xPost);
            view.setPivotY(mViewHalfHeight);
            view.setRotationY(yRotate);
            view.setAlpha(mAlpha);
        }
    }

}
