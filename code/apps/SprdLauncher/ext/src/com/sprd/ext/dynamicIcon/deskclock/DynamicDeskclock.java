package com.sprd.ext.dynamicIcon.deskclock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.android.sprdlauncher3.BubbleTextView;
import com.android.sprdlauncher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.dynamicIcon.DynamicIcon;
import com.sprd.ext.dynamicIcon.DynamicIconSettingsFragment;

import java.util.Calendar;

/**
 * Created by SPRD on 10/21/16.
 */
public class DynamicDeskclock extends DynamicIcon {

    private static final String TAG = "DynamicDeskclock";

    private static final String DESKCLOCK_CONFIG_KEY = "ro.launcher.defaultclock";
    private static final boolean IS_SHOW_SECOND = SystemProperties.getBoolean("ro.launcher.dyclock.second", true);

    // Fraction of the length of second hand.
    private static final float SECOND_LENGTH_FACTOR = 0.4f;
    // Fraction of the length of minute hand.
    private static final float MINUTE_LENGTH_FACTOR = 0.32f;
    // Fraction of the length of hour hand.
    private static final float HOUR_LENGTH_FACTOR = 0.23f;

    private Paint mSecondPaint;
    private Paint mMinutePaint;
    private Paint mHourPaint;
    private int mSecondWidth;
    private int mMinuteWidth;
    private int mHourWidth;
    private int mCenterRadius;

    private int mLastHour;
    private int mLastMinute;
    private int mLastSecond;
    // The default time should not be changed, because it should be consistent with that in the deskclock app icon.
    // Unless the default time and the deskclock app icon change together.
    private int[] mDefaultTime = new int[] {03, 07, 00};

    private Handler mSecondsHandler;
    private Drawable mClockBackground;

    HandlerThread mSecondThread;

    private final Runnable mSecondTick =new Runnable() {
        @Override
        public void run() {
            forceUpdateView(true);
            if (mSecondsHandler != null) {
                mSecondsHandler.postAtTime(this, SystemClock.uptimeMillis() / 1000 * 1000 + 1000);
            }
        }
    };

    public DynamicDeskclock(Context context, int type) {
        super(context, type);

        boolean defValue = SystemProperties.getBoolean(DESKCLOCK_CONFIG_KEY,
                mContext.getResources().getBoolean(R.bool.config_show_dynamic_clock));
        mIsChecked = UtilitiesExt.getLauncherSettingsBoolean(mContext,
                DynamicIconSettingsFragment.PRE_DYNAMIC_CLOCK, defValue);
    }

    @Override
    protected void init() {
        Resources res = mContext.getResources();

        mCenterRadius = res.getDimensionPixelSize(R.dimen.dynamic_clock_center_radius);

        mSecondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecondPaint.setColor(res.getColor(R.color.dynamic_clock_second_hand));
        mSecondWidth = res.getDimensionPixelSize(R.dimen.dynamic_clock_second_width);

        mMinutePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMinutePaint.setColor(res.getColor(R.color.dynamic_clock_minute_hand));
        mMinuteWidth = res.getDimensionPixelSize(R.dimen.dynamic_clock_minute_width);

        mHourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHourPaint.setColor(res.getColor(R.color.dynamic_clock_hour_hand));
        mHourWidth = res.getDimensionPixelOffset(R.dimen.dynamic_clock_hour_width);

        mClockBackground = ContextCompat.getDrawable(mContext, R.drawable.ic_dial_plate);

        mSecondThread = new HandlerThread(LogUtils.MODULE_NAME + "sec-thread");
        mSecondThread.start();
    }

    @Override
    protected boolean hasChanged() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        if (mLastSecond != second
                || mLastMinute != minute
                || mLastHour != hour) {
            mLastSecond = second;
            mLastMinute = minute;
            mLastHour = hour;
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected IntentFilter getReceiverFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        return filter;
    }

    @Override
    public boolean registerReceiver() {
        if (super.registerReceiver()) {
            if (LogUtils.DEBUG_DYNAMIC_ICON) {
                LogUtils.d(TAG, "registerReceiver.");
            }
            if (IS_SHOW_SECOND) {
                startAutoSecondRunnable();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean unRegisterReceiver() {
        if (super.unRegisterReceiver()) {
            if (LogUtils.DEBUG_DYNAMIC_ICON) {
                LogUtils.d(TAG, "unRegisterReceiver.");
            }
            if (IS_SHOW_SECOND) {
                stopAutoSecondRunnable();
            }
            return true;
        }
        return false;
    }

    public Drawable getStableBackground() {
        return mClockBackground;
    }

    public DynamicIconDrawCallback getDynamicIconDrawCallback() {
        return mDrawCallback;
    }

    private void startAutoSecondRunnable() {
        if (IS_SHOW_SECOND) {
            if (mSecondThread == null) {
                LogUtils.e(TAG, "The second thread is null!");
                return;
            }

            if (mSecondsHandler == null) {
                mSecondsHandler = new Handler(mSecondThread.getLooper());
            }
            mSecondsHandler.removeCallbacks(mSecondTick);
            mSecondsHandler.postAtTime(mSecondTick,
                    SystemClock.uptimeMillis() / 1000 * 1000 + 1000);
        }
    }

    private void stopAutoSecondRunnable() {
        if (mSecondsHandler != null) {
            mSecondsHandler.removeCallbacks(mSecondTick);
            mSecondsHandler = null;
        }
    }

    @Override
    protected void draw(Canvas canvas, View icon, float scale, int[] center) {
        if (canvas == null || center == null || !(icon instanceof BubbleTextView)) {
            return;
        }

        int iconSize = ((BubbleTextView) icon).getIconSize();
        float secondLength = iconSize * SECOND_LENGTH_FACTOR;
        float minuteLength = iconSize * MINUTE_LENGTH_FACTOR;
        float hourLength = iconSize * HOUR_LENGTH_FACTOR;

        scale = Math.abs(scale);
        mSecondPaint.setStrokeWidth(mSecondWidth * scale);
        mMinutePaint.setStrokeWidth(mMinuteWidth * scale);
        mHourPaint.setStrokeWidth(mHourWidth * scale);

        Calendar c = Calendar.getInstance();
        int hour = mIsChecked ? c.get(Calendar.HOUR_OF_DAY) % 12 : mDefaultTime[0];
        int minute = mIsChecked ? c.get(Calendar.MINUTE) : mDefaultTime[1];
        int second = mIsChecked ? c.get(Calendar.SECOND) : mDefaultTime[2];

        mLastSecond = second;
        mLastMinute = minute;
        mLastHour = hour;

        float Minutes = minute + second / 60.0f;
        float Hour = hour + Minutes / 60.0f;

        double radianSecond = ((float) second / 60.0f * 360f)/180f * Math.PI;
        double radianMinute = (Minutes / 60.0f * 360f)/180f * Math.PI;
        double radianHour = (Hour / 12.0f * 360f)/180f * Math.PI;

        float secondX = 0f;
        float secondY = 0f;
        if (IS_SHOW_SECOND) {
            secondX = (float) (scale * secondLength * Math.sin(radianSecond));
            secondY = (float) (scale * secondLength * Math.cos(radianSecond));
        }

        float minuteX = (float) (scale * minuteLength * Math.sin(radianMinute));
        float minuteY = (float) (scale * minuteLength * Math.cos(radianMinute));

        float hourX = (float) (scale * hourLength * Math.sin(radianHour));
        float hourY = (float) (scale * hourLength * Math.cos(radianHour));

        canvas.save();
        canvas.drawCircle(center[0], center[1], scale * mCenterRadius, mSecondPaint);
        canvas.drawLine(center[0], center[1], center[0] + hourX, center[1] - hourY, mHourPaint);
        canvas.drawLine(center[0], center[1], center[0] + minuteX, center[1] - minuteY, mMinutePaint);
        if (IS_SHOW_SECOND) {
            canvas.drawLine(center[0], center[1], center[0] + secondX, center[1] - secondY, mSecondPaint);
        }
        canvas.restore();
    }
}