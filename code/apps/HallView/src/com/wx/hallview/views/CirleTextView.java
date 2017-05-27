package com.wx.hallview.views;

/**
 * Created by Administrator on 16-1-21.
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.TextView;

public class CirleTextView extends TextView
{
    public CirleTextView(Context paramContext)
    {
        super(paramContext);
    }

    public CirleTextView(Context paramContext, AttributeSet paramAttributeSet)
    {
        super(paramContext, paramAttributeSet);
    }

    public CirleTextView(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
    {
        super(paramContext, paramAttributeSet, paramInt);
    }

    protected void onDraw(Canvas paramCanvas){
		final String textString = getText().toString();
        paramCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, 3));
        Paint localPaint = new Paint();
        localPaint.setColor(Color.BLACK);
        localPaint.setTextSize(getTextSize());
        
        //qiuyaobo,20170321,begin
        //localPaint.setStyle(Paint.Style.STROKE);
        localPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        //qiuyaobo,20170321,end
        
        Path localPath = new Path();
        int j = (int)(getMeasuredWidth() / 5.0F);
        localPath.arcTo(new RectF(j, j, getMeasuredWidth() - j, getMeasuredHeight() - j), 180.0F, 359.0F);
        int i = textString.length();
        int k = (int)(3.141592653589793D * (getMeasuredWidth() - j) * 2.0D / 26.0D);
        k = (int)(k * 3 - 22 - k * i / 4.5F);
        i = k;
        if (k <= 0)
            i = 0;
        paramCanvas.drawTextOnPath(textString, localPath, i, 0.0F, localPaint);
    }
}
