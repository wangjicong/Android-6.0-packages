package com.wx.hallview.views;

/**
 * Created by Administrator on 16-1-21.
 */
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.wx.hallview.R;

public class RoundProgressBar extends View
{
    private int max;
    OnLoadFinishListener onLoadFinish = null;
    private Paint paint = new Paint();
    private int progress;
    private int roundColor;
    private int roundProgressColor;
    private float roundWidth;
    private int style;
    private int textColor;
    private boolean textIsDisplayable;
    private float textSize;

    public RoundProgressBar(Context paramContext)
    {
        this(paramContext, null);
    }

    public RoundProgressBar(Context paramContext, AttributeSet paramAttributeSet)
    {
        this(paramContext, paramAttributeSet, 0);
    }

    public RoundProgressBar(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
    {
        super(paramContext, paramAttributeSet, paramInt);
        TypedArray typedArray = paramContext.obtainStyledAttributes(paramAttributeSet, R.styleable.RoundProgressBar);
        this.roundColor = typedArray.getColor(0, Color.RED);
        this.roundProgressColor = typedArray.getColor(1, Color.GREEN);
        this.textColor = typedArray.getColor(3,  Color.GREEN);
        this.textSize = typedArray.getDimension(4, 15.0F);
        this.roundWidth = typedArray.getDimension(2, 5.0F);
        this.max = typedArray.getInteger(5, 100);
        this.textIsDisplayable = typedArray.getBoolean(6, true);
        this.style = typedArray.getInt(7, 0);
        typedArray.recycle();
    }

    protected void onDraw(Canvas paramCanvas)
    {
        super.onDraw(paramCanvas);
        int i = getWidth() / 2;
        int j = (int)(i - this.roundWidth / 2.0F);
        this.paint.setColor(this.roundColor);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(this.roundWidth);
        this.paint.setAntiAlias(true);
        paramCanvas.drawCircle(i, i, j, this.paint);
        Log.d("log", i + "");
        this.paint.setStrokeWidth(0.0F);
        this.paint.setColor(this.textColor);
        this.paint.setTextSize(this.textSize);
        this.paint.setTypeface(Typeface.DEFAULT_BOLD);
        float f = this.paint.measureText(progress + "%");
        
        if ((this.textIsDisplayable) && (this.style == 0)){
        	  this.paint.setStyle(Paint.Style.FILL_AND_STROKE);//qiuyaobo,20170321
            paramCanvas.drawText(progress + "%", i - f / 2.0F, i + this.textSize / 2.0F + 22.0F, this.paint);
        }
            
        this.paint.setStrokeWidth(this.roundWidth);
        this.paint.setColor(this.roundProgressColor);
        RectF localRectF = new RectF(i - j, i - j, i + j, i + j);
        if(this.style == 1){
            this.paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paramCanvas.drawArc(localRectF, 0.0F, this.progress * 360 / this.max, true, this.paint);
        }else{
            this.paint.setStyle(Paint.Style.STROKE);
            paramCanvas.drawArc(localRectF, -90.0F, this.progress * 360 / this.max, false, this.paint);
        }
        if ((this.progress == this.max) && (this.onLoadFinish != null))
            this.onLoadFinish.onLoadFinished();
    }

    public synchronized void setProgress(int paramInt)
    {

        if (paramInt < 0){
            try
            {
                throw new IllegalArgumentException("progress not less than 0");
            }
            finally
            {

            }
        }

		int i = paramInt*max/100;
        if(paramInt > this.max){
            i = this.max;
        }
        this.progress = i;
        postInvalidate();
    }

    public static abstract interface OnLoadFinishListener
    {
        public abstract void onLoadFinished();
    }
}
