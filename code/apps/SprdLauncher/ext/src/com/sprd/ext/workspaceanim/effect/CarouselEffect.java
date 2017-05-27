/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.view.View;

public class CarouselEffect extends EffectInfo {

    private boolean flag;

    public CarouselEffect(int id) {
        super(id);
    }

    public CarouselEffect(int id, boolean flag) {
        super(id, flag);
        this.flag = flag;
    }

    @Override
    public void getTransformationMatrix(View view, float offset, int pageWidth, int pageHeight, float distance) {

        float absOffset = Math.abs(offset);
        float mAlpha = 1.0F - absOffset * 0.4f;

        int mViewWidth = pageWidth;
        int mViewHeight = pageHeight;

        int mViewHalfHeight = mViewHeight >> 1;

        view.setCameraDistance(distance);
        float yRotate = 90.0F * (-offset);
        float xPost = mViewWidth * offset;
//        if (overScroll) {
//            xPost = 0f;
//        }
    /* @} */
        view.setTranslationX(xPost);
        view.setPivotY(mViewHalfHeight);
        view.setPivotX(flag ? 0.0f : mViewWidth);
        view.setRotationY(yRotate);
        view.setRotationX(0f);
        view.setRotation(0f);
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(mAlpha);
    }
}
