package com.wx.hallview.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.wx.hallview.R;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MyQAnalogClock extends View
{
    int availableHeight = 900;
    int availableWidth = 710;
    BitmapDrawable bmdHour;
    BitmapDrawable bmdMinute;
    BitmapDrawable bmdSecond;
    int centerX;
    int centerY;
    private String day;
    Bitmap mBmpHour;
    Bitmap mBmpMinute;
    Bitmap mBmpSecond;
    int mHeigh;
    private int mHourX;
    private int mHourY;
    private int mMinuteX;
    private int mMinuteY;
    Paint mPaint;
    private int mSecondX;
    private int mSecondY;
    int mTempHeigh;
    int mTempWidth;
    private boolean mTimming = false;
    private long mUseTime = 0L;
    int mWidth;
    Handler tickHandler = new Handler();
    private Runnable tickRunnable = new Runnable()
    {
        public void run()
        {
            Date localDate = new Date();
            MyQAnalogClock.this.week = String.format("%ta", new Object[] { localDate });
            MyQAnalogClock.this.day = String.format("%td", new Object[] { localDate });
			setDayOfWeek();
            MyQAnalogClock.this.postInvalidate();
            if (MyQAnalogClock.this.mUseTime > 1000L)
                MyQAnalogClock.this.mUseTime = 1000L;
            System.out.println("xuehui" + MyQAnalogClock.this.mUseTime);
            MyQAnalogClock.this.tickHandler.postDelayed(MyQAnalogClock.this.tickRunnable, 1000L - MyQAnalogClock.this.mUseTime);
        }
    };
    private String week;
	public int mdayOfWeek;

    public MyQAnalogClock(Context paramContext)
    {
        super(paramContext);
    }

    public MyQAnalogClock(Context paramContext, AttributeSet paramAttributeSet)
    {
        super(paramContext, paramAttributeSet);
        TypedArray typedArray = paramContext.obtainStyledAttributes(paramAttributeSet, R.styleable.ClockLayout);
        int i = typedArray.getResourceId(0, 0);
        int j = typedArray.getResourceId(1, 0);
        int k = typedArray.getResourceId(2, 0);
        this.mHourX = typedArray.getInteger(3, 0);
        this.mHourY = typedArray.getInteger(4, 0);
        this.mMinuteX = typedArray.getInteger(5, 0);
        this.mMinuteY = typedArray.getInteger(6, 0);
        this.mSecondX = typedArray.getInteger(7, 0);
        this.mSecondY = typedArray.getInteger(8, 0);
        this.mBmpHour = BitmapFactory.decodeResource(getResources(), i);
        this.bmdHour = new BitmapDrawable(this.mBmpHour);
        this.mBmpMinute = BitmapFactory.decodeResource(getResources(), j);
        this.bmdMinute = new BitmapDrawable(this.mBmpMinute);
        this.mBmpSecond = BitmapFactory.decodeResource(getResources(), k);
        this.bmdSecond = new BitmapDrawable(this.mBmpSecond);
        this.mPaint = new Paint();
        this.mPaint.setColor(Color.BLUE);
        typedArray.recycle();
    }

    public String getDay()
    {
        return this.day;
    }

    public String getWeek()
    {
        return this.week;
    }

    protected void onDraw(Canvas paramCanvas)
    {
        super.onDraw(paramCanvas);
        long l = System.currentTimeMillis();
        Calendar localCalendar = Calendar.getInstance();
        int i = localCalendar.get(Calendar.HOUR);
        int j = localCalendar.get(Calendar.MINUTE);
        int k = localCalendar.get(Calendar.SECOND);
        float f1 = i;
        float f2 = j / 60.0F;
        float f3 = j;
        float f4 = k;
        i = 0;
        if ((this.availableWidth < this.mWidth) || (this.availableHeight < this.mHeigh))
        {
            i = 1;
            float f5 = Math.min(this.availableWidth / this.mWidth, this.availableHeight / this.mHeigh);
            paramCanvas.save();
            paramCanvas.scale(f5, f5, this.centerX, this.centerY + 100);
        }
        this.mTempWidth = this.bmdHour.getIntrinsicWidth();
        this.mTempHeigh = this.bmdHour.getIntrinsicHeight();
        paramCanvas.save();
        paramCanvas.rotate(f1 * 30.0F + f2 * 30.0F, this.centerX + this.mHourX, this.centerY + this.mHourY);
        this.bmdHour.setBounds(this.centerX - this.mTempWidth / 2, this.centerY - this.mTempHeigh / 2, this.centerX + this.mTempWidth / 2, this.centerY + this.mTempHeigh / 2);
        this.bmdHour.draw(paramCanvas);
        paramCanvas.restore();
        this.mTempWidth = this.bmdMinute.getIntrinsicWidth();
        this.mTempHeigh = this.bmdMinute.getIntrinsicHeight();
        paramCanvas.save();
        paramCanvas.rotate(f3 * 6.0F, this.centerX + this.mMinuteX, this.centerY + this.mMinuteY);
        this.bmdMinute.setBounds(this.centerX - this.mTempWidth / 2, this.centerY - this.mTempHeigh / 2, this.centerX + this.mTempWidth / 2, this.centerY + this.mTempHeigh / 2);
        this.bmdMinute.draw(paramCanvas);
        paramCanvas.restore();
        this.mTempWidth = this.bmdSecond.getIntrinsicWidth();
        this.mTempHeigh = this.bmdSecond.getIntrinsicHeight();
        paramCanvas.rotate(f4 * 6.0F, this.centerX + this.mSecondX, this.centerY + this.mSecondY);
        this.bmdSecond.setBounds(this.centerX - this.mTempWidth / 2, this.centerY - this.mTempHeigh / 2, this.centerX + this.mTempWidth / 2, this.centerY + this.mTempHeigh / 2);
        this.bmdSecond.draw(paramCanvas);
        this.mUseTime = (System.currentTimeMillis() - l);
        if (i != 0)
            paramCanvas.restore();
    }

    protected void onMeasure(int paramInt1, int paramInt2)
    {
        super.onMeasure(paramInt1, paramInt2);
        this.mWidth = getMeasuredWidth();
        this.mHeigh = getMeasuredHeight();
        this.centerX = (this.mWidth / 2);
        this.centerY = (this.mHeigh / 2);
    }

    public void pauseTiming()
    {
        Log.d("MyQAnalogClock", "pauseTiming mTimming=" + this.mTimming);
        if (this.mTimming)
        {
            this.tickHandler.removeCallbacks(this.tickRunnable);
            this.mTimming = false;
        }
    }

    public void run()
    {
        Log.d("MyQAnalogClock", "run mTimming=" + this.mTimming);
        if (!this.mTimming)
        {
            this.mTimming = true;
            Date localDate = new Date();
            this.week = String.format("%ta", new Object[] { localDate });
            this.day = String.format("%td", new Object[] { localDate });
			setDayOfWeek();
            this.tickHandler.post(this.tickRunnable);
        }
    }

    public void startTiming()
    {
        run();
    }

	private void setDayOfWeek(){
		Calendar localCalendar = Calendar.getInstance();
		boolean isFirstMonday = (localCalendar.getFirstDayOfWeek() == Calendar.MONDAY);
		mdayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK)-1;
		if(isFirstMonday){
			mdayOfWeek += 1;
			if(mdayOfWeek == 7){
				mdayOfWeek = 0;
			}
		}
	}
}
