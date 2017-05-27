package com.wx.hallview.views;

/**
 * Created by Administrator on 16-1-21.
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageViewWithUnReadNumber extends ImageView
{
    private int mUnReadNumber = 0;

    public ImageViewWithUnReadNumber(Context paramContext)
    {
        super(paramContext);
    }

    public ImageViewWithUnReadNumber(Context paramContext, AttributeSet paramAttributeSet)
    {
        super(paramContext, paramAttributeSet);
    }

    public ImageViewWithUnReadNumber(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
    {
        super(paramContext, paramAttributeSet, paramInt);
    }

    protected void onDraw(Canvas paramCanvas)
    {
        super.onDraw(paramCanvas);
        if (this.mUnReadNumber == 0) {
            return;
        }
        Paint localPaint;

        paramCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, 3));
        localPaint = new Paint();
        localPaint.setColor(Color.RED);
        paramCanvas.drawCircle(getMeasuredWidth() - 11, 11.0F, 11.0F, localPaint);

        localPaint.setColor(Color.WHITE);
        localPaint.setTextSize(15.0F);
        if(this.mUnReadNumber < 10){
            paramCanvas.drawText(String.valueOf(this.mUnReadNumber), getMeasuredWidth() - 15, 15.0F, localPaint);
        }else if(this.mUnReadNumber > 99){
            paramCanvas.drawText("99", getMeasuredWidth() - 19, 16.0F, localPaint);
        }else{
            paramCanvas.drawText(String.valueOf(this.mUnReadNumber), getMeasuredWidth() - 19, 16.0F, localPaint);
        }

    }

    public void setUnReadNumber(int paramInt)
    {
        this.mUnReadNumber = paramInt;
        postInvalidate();
    }
}