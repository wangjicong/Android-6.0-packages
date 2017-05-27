/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.view.View;

public class NormalEffect extends EffectInfo {

    private boolean flag;

    public NormalEffect(int id) {
        super(id);
    }

    public NormalEffect(int id, boolean flag) {
        super(id, flag);
        this.flag = flag;

    }

    @Override
    public void getTransformationMatrix(View view, float offset, int pageWidth, int pageHeight, float distance) {

        if (flag) {
            view.setCameraDistance(distance);
        }

        float xPost = 0f;
        view.setAlpha(1f);
        view.setTranslationX(xPost);
        view.setPivotX(offset < 0 ? 0 : pageWidth);
        view.setPivotY(pageHeight * 0.5f);
        view.invalidate();
    }

}
