package com.wx.hallview.views;

/**
 * Created by Administrator on 16-1-20.
 */
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import com.wx.hallview.R;

public class ButteryView extends RelativeLayout
{
    private int mButteryRemain = 0;
    private View mButteryView = null;

    public ButteryView(Context paramContext, AttributeSet paramAttributeSet)
    {
        super(paramContext, paramAttributeSet);
    }

    protected void onFinishInflate()
    {
        super.onFinishInflate();
        this.mButteryView = getChildAt(0);
    }

    public void setButterRemain(int paramInt)
    {
        this.mButteryRemain = paramInt;
        ViewGroup.LayoutParams localLayoutParams = this.mButteryView.getLayoutParams();
        localLayoutParams.height = (int)((getContext().getResources().getDimensionPixelOffset(R.dimen.buttery_height) - 20) * (this.mButteryRemain / 100.0F));
        this.mButteryView.setLayoutParams(localLayoutParams);
        postInvalidate();
    }
}
