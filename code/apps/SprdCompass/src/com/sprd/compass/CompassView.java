package com.sprd.compass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class CompassView extends View {
    private float mDirection;

    Bitmap mCompass;
    Bitmap mCompassArrow;

    Bitmap[] mCompassArray = new Bitmap[3];
    Bitmap[] mCompassArrowArray = new Bitmap[3];

    public CompassView(Context context) {
        this(context, null, 0);
    }

    public CompassView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompassView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDirection = 0.0f;

        mCompassArray[0] = BitmapFactory.decodeResource(getResources(), R.drawable.cp01_compass).copy(
                Bitmap.Config.ARGB_8888, true);
        mCompassArray[1] = BitmapFactory.decodeResource(getResources(), R.drawable.cp02_compass).copy(
                Bitmap.Config.ARGB_8888, true);
        mCompassArray[2] = BitmapFactory.decodeResource(getResources(), R.drawable.cp03_compass).copy(
                Bitmap.Config.ARGB_8888, true);

        mCompassArrowArray[0] = BitmapFactory.decodeResource(getResources(), R.drawable.arrow01_compass).copy(
                Bitmap.Config.ARGB_8888, true);
        mCompassArrowArray[1] = BitmapFactory.decodeResource(getResources(), R.drawable.arrow02_compass).copy(
                Bitmap.Config.ARGB_8888, true);
        mCompassArrowArray[2] = BitmapFactory.decodeResource(getResources(), R.drawable.arrow03_compass).copy(
                Bitmap.Config.ARGB_8888, true);

        mCompass = mCompassArray[0];
        mCompassArrow = mCompassArrowArray[0];
    }

    public void setCompassStype(int i) {
        if (i > 3 || i < 1) {
            mCompass = mCompassArray[0];
            mCompassArrow = mCompassArrowArray[0];
        }

        mCompass = mCompassArray[i - 1];
        mCompassArrow = mCompassArrowArray[i - 1];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int pivotX = this.getWidth() / 2;
        int pivotY = this.getHeight() / 2;


        canvas.save();
        canvas.translate(0, (mCompass.getHeight() - this.getHeight()) / 2);
        canvas.rotate(mDirection, pivotX, pivotY);

        int left = pivotX - mCompass.getWidth() / 2;
        int top = pivotY - mCompass.getHeight() / 2;
        canvas.drawBitmap(mCompass, left, top, null);

        left = pivotX - mCompassArrow.getWidth() / 2;
        top = pivotY - mCompassArrow.getHeight() / 2;
        canvas.drawBitmap(mCompassArrow, left, top, null);
        canvas.restore();
    }

    public void updateDirection(float direction) {
        mDirection = direction;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);

        setMeasuredDimension(mCompass.getWidth(), mCompass.getHeight());
    }

}
