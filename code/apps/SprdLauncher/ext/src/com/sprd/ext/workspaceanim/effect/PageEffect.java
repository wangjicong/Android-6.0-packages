/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.view.View;

public class PageEffect extends EffectInfo {

    public PageEffect(int id) {
        super(id);
    }

    @Override
    public void getTransformationMatrix(View view, float offset, int pageWidth, int pageHeight, float distance) {
        float xpost = pageWidth * offset;
        view.setCameraDistance(distance);
        if (offset > 0 && offset < 1) {
            view.setPivotX(0);
            view.setPivotY(pageHeight >> 1);
            view.setRotationY(offset * -120.f);

        }

        if (offset > -1 && offset < 0) {
            view.setPivotX(view.getMeasuredWidth());
            view.setPivotY(view.getMeasuredHeight() >> 1);
            view.setRotationY(offset * 120.0f);
        }
        view.setAlpha(1f);
        view.setTranslationX(xpost);
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
    }
}
