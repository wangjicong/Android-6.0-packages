/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;

/**
 * TextView that draws a bubble behind the text. We cannot use a LineBackgroundSpan
 * because we want to make the bubble taller than the text and TextView's clip is
 * too aggressive.
 */
public class ClockBubbleTextView extends BubbleTextView{

    private Resources mRes;
    private Context mContext;
    private Bitmap mBackgroundBitmap;
    private IntentFilter mFilter;
    private Bitmap mCicleBitmap;
    private Bitmap mHourBitmap;
    private Bitmap mMinuteBitmap;
    private Bitmap mSecondBitmap;
    private Handler mHandler;
    private Canvas mCanvas ;
    private Bitmap mBitmap;
    private int mBackgroundWidth;
    private int mBackgroundHeight;
    private int mCicleWidth;
    private int mCicleHeight;

    public ClockBubbleTextView(Context context) {
        this(context, null, 0);
    }

    public ClockBubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockBubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mRes = context.getResources();
        init();
    }

    private void init(){
        mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_TIME_TICK);
        mFilter.addAction(Intent.ACTION_TIME_CHANGED);
        mFilter.addAction(Intent.ACTION_DATE_CHANGED);
        mFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        mBackgroundBitmap = BitmapFactory.decodeResource(mRes, R.drawable.ic_dial_plate);
        mCicleBitmap = BitmapFactory.decodeResource(mRes, R.drawable.ic_cicle);
        mHourBitmap = BitmapFactory.decodeResource(mRes, R.drawable.ic_hour_hand);
        mMinuteBitmap = BitmapFactory.decodeResource(mRes, R.drawable.ic_minute_hand);
        mSecondBitmap = BitmapFactory.decodeResource(mRes, R.drawable.ic_second_hand);
        mBackgroundWidth = mBackgroundBitmap.getWidth();
        mBackgroundHeight = mBackgroundBitmap.getHeight();
        mCicleWidth = mCicleBitmap.getWidth();
        mCicleHeight = mCicleBitmap.getHeight();
        mBitmap = Bitmap.createBitmap(mBackgroundWidth, mBackgroundHeight, Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        if (mHandler == null) {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                     super.handleMessage(msg);
                     updateDeskClock();
                     Message msgUp = mHandler.obtainMessage();
                     mHandler.sendMessageDelayed(msgUp, 1000);
                 }
            };
            Message msgUp = mHandler.obtainMessage();
            mHandler.sendMessageDelayed(msgUp, 1000);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mContext.registerReceiver(mBroadcastReceiver, mFilter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDeskClock();
        }
    };

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache,
            boolean setDefaultPadding, boolean promiseStateChanged) {
        super.applyFromShortcutInfo(info, iconCache, setDefaultPadding, promiseStateChanged);
        updateDeskClock();
    }

    public void applyFromApplicationInfo(AppInfo info) {
        super.applyFromApplicationInfo(info);
        updateDeskClock();
    }

    private void updateDeskClock() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        /* SPRD:bug 519737 the location of hour hand was wrong @{ */
        float Seconds = second;
        float Minutes = minute + second / 60.0f;
        float Hour = hour + Minutes / 60.0f;

        float angleSecond = Seconds / 60.0f * 360.0f - 90;
        float angleMinute = Minutes / 60.0f * 360.0f - 90;
        float angleHour = Hour / 12.0f * 360.0f - 90;
        /* @} */
        Drawable d = getCompoundDrawables()[1];
        if (d != null) {
            /*SPRD: bug519731, modify to prevent sawtooth-shaped border @{*/
            if(mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }
            mBitmap = Bitmap.createBitmap(mBackgroundWidth, mBackgroundHeight, Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            /* @} */

            mCanvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
            mCanvas.drawBitmap(mCicleBitmap,getScrollX() + mBackgroundWidth/2 - mCicleWidth/2 ,
                getScrollY() + mBackgroundHeight/2-mCicleHeight/2, null);

            mCanvas.save();
            //SPRD:bug 519737 the location of hour hand was wrong
            mCanvas.rotate(angleHour, mBackgroundWidth/ 2, mBackgroundHeight/2);
            mCanvas.drawBitmap(mHourBitmap,  mBackgroundWidth/ 2- mCicleWidth/2,mBackgroundHeight/2 - mHourBitmap.getHeight()/2, null);
            mCanvas.restore();

            mCanvas.save();
            //SPRD:bug 519737 the location of hour hand was wrong
            mCanvas.rotate(angleMinute, mBackgroundWidth/ 2, mBackgroundHeight/2);
            mCanvas.drawBitmap(mMinuteBitmap, mBackgroundWidth/ 2 - mCicleWidth/2,mBackgroundHeight/2 - mMinuteBitmap.getHeight()/2, null);
            mCanvas.restore();

            mCanvas.save();
            //SPRD:bug 519737 the location of hour hand was wrong
            mCanvas.rotate(angleSecond,mBackgroundWidth/ 2, mBackgroundHeight/2);
            mCanvas.drawBitmap(mSecondBitmap,mBackgroundWidth/ 2- mCicleWidth/2,mBackgroundHeight/2 - mSecondBitmap.getHeight()/2, null);
            mCanvas.restore();
            d = Utilities.createIconDrawable(mBitmap);
            setCompoundDrawables(null , d, null, null);
        }
    }
}
