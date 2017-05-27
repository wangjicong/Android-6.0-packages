/** Created by Spreadtrum */
package com.sprd.ext.workspaceanim.effect;

import android.view.View;

public abstract class EffectInfo {
    public final int id;
    public boolean flag;

    public EffectInfo(int id) {
        this.id = id;
    }

    public EffectInfo(int id, boolean flag) {
        this.id = id;
        this.flag = flag;

    }

    public abstract void getTransformationMatrix(View view ,float offset, int pageWidth, int pageHeight,float distance);

}
