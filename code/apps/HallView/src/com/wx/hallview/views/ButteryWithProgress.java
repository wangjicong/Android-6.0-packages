package com.wx.hallview.views;

/**
 * Created by Administrator on 16-1-21.
 */
import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.wx.hallview.R;

public class ButteryWithProgress extends RelativeLayout {
    private ButteryView mButteryView;
    private RoundProgressBar mProgressBar;

    public ButteryWithProgress(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mProgressBar = ((RoundProgressBar) findViewById(R.id.progress_bar));
        this.mButteryView = ((ButteryView) findViewById(R.id.buttery_view));
    }

    public void setButterRemain(int paramInt) {
        this.mProgressBar.setProgress(paramInt);
        this.mButteryView.setButterRemain(paramInt);
    }
}
