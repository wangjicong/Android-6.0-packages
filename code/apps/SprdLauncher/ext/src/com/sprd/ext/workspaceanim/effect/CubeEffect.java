/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.view.View;

public class CubeEffect extends EffectInfo {

    private boolean flag;

    public CubeEffect(int id) {
        super(id);
    }

    public CubeEffect(int id, boolean flag) {
        super(id, flag);
        this.flag = flag;

    }

    @Override
    public void getTransformationMatrix(View view, float offset, int pageWidth, int pageHeight, float distance) {

        float rotation = (flag ? 90.0f : -90.0f) * offset;
        float alpha = 1 - Math.abs(offset) * 0.4f;
        if (flag) {
            view.setCameraDistance(distance);
        }

        float xPost = 0f;
        view.setTranslationX(xPost);
        view.setPivotX(offset < 0 ? 0 : pageWidth);
        view.setPivotY(pageHeight * 0.5f);
        view.setRotationY(rotation);
        view.setAlpha(alpha);
        view.invalidate();

    }

}
