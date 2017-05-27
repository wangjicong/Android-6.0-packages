package com.wx.hallview.views;

/**
 * Created by Administrator on 16-1-21.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import java.io.PrintStream;
import com.wx.hallview.R;
import android.util.Log;

public class CircleLayout extends ViewGroup
{
    private int mDegreeDelta;
    private int offset = 0;
    private float radius = 0.0F;

    public CircleLayout(Context paramContext)
    {
        super(paramContext);
    }

    public CircleLayout(Context paramContext, AttributeSet paramAttributeSet)
    {
        super(paramContext, paramAttributeSet);
        TypedArray typedArray  = paramContext.obtainStyledAttributes(paramAttributeSet, R.styleable.CircleLayout);
        this.radius = typedArray.getDimension(0, 0.0F);
        this.offset = typedArray.getInteger(1, 0);
        System.out.println("radius:" + this.radius);
        typedArray.recycle();
    }

    public CircleLayout(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
    {
        super(paramContext, paramAttributeSet, paramInt);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();
        mDegreeDelta = (360 / count);
        int parentLeft = getPaddingLeft();
        int parentRight = (right - left) - getPaddingRight();
        int parentTop = getPaddingTop();
        int parentBottom = (bottom - top) - getPaddingBottom();
        if(count < 1) {
            return;
        }
        System.out.println(Math.cos(0.0));
        for(int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if(child.getVisibility() != View.GONE) {
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                if(count == 1) {
                    int childLeft = parentLeft + (((parentRight - parentLeft) - width) / 2);
                    int childTop = parentTop + (((parentBottom - parentTop) - height) / 2);
                    child.layout(childLeft, childTop, (childLeft + width), (childTop + height));
                    continue;
                }
                int childLeft = (int)((double)((((parentRight - parentLeft) - width) / 2) + parentLeft) - ((double)radius * Math.sin((((double)((mDegreeDelta * i) + offset) * 3.14) / 180.0))));
                int childTop = (int)((double)((((parentBottom - parentTop) - height) / 2) + parentTop) - ((double)radius * Math.cos((((double)((mDegreeDelta * i) + offset) * 3.14) / 180.0))));
                child.layout(childLeft, childTop, (childLeft + width), (childTop + height));
            }
        }
    }

    protected void onMeasure(int paramInt1, int paramInt2)
    {
        int i = View.MeasureSpec.getSize(paramInt1);
        int j = View.MeasureSpec.getSize(paramInt2);
        if (this.radius == 0.0F)
            this.radius = (i / 2 - 40);
        measureChildren(paramInt1, paramInt2);
        setMeasuredDimension(i, j);
    }
}