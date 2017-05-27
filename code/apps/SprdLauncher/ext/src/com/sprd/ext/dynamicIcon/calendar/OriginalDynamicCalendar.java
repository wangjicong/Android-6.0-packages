package com.sprd.ext.dynamicIcon.calendar;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.android.sprdlauncher3.BubbleTextView;
import com.android.sprdlauncher3.R;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.dynamicIcon.DynamicIcon;
import com.sprd.ext.dynamicIcon.DynamicIconSettingsFragment;

import java.util.Calendar;

/**
 * Created by SPRD on 10/21/16.
 */
public class OriginalDynamicCalendar extends DynamicIcon {

    private static final String CALENDAR_CONFIG_KEY = "ro.config.dynamiccalendar";
    // The proportion of the date font occupied in the dynamic calendar icon.
    private static final float DATE_SIZE_FACTOR = 0.5f;

    private String mLastDate = "";
    // The default date should not be changed, because it should be consistent with that in the calendar app icon.
    // Unless the default date and the calendar app icon change together.
    // Month value is 0-based. e.g., 0 for January.
    private int[] mDefaultDate = new int[]{2016, 9, 31};
    private Paint mDatePaint;
    private Drawable mCalendarBackground;

    public OriginalDynamicCalendar(Context context, int type) {
        super(context, type);

        boolean defValue = SystemProperties.getBoolean(CALENDAR_CONFIG_KEY,
                mContext.getResources().getBoolean(R.bool.config_show_dynamic_calendar));
        mIsChecked = UtilitiesExt.getLauncherSettingsBoolean(mContext,
                DynamicIconSettingsFragment.PRE_DYNAMIC_CALENDAR, defValue);
    }

    @Override
    protected void init() {
        Typeface font = Typeface.create("sans-serif", Typeface.NORMAL);

        mDatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDatePaint.setTypeface(font);
        mDatePaint.setTextAlign(Paint.Align.CENTER);
        mDatePaint.setAntiAlias(true);
        mDatePaint.setColor(Color.WHITE);

        mCalendarBackground = ContextCompat.getDrawable(mContext, R.drawable.ic_calendar_plate_original);
    }

    @Override
    protected boolean hasChanged() {
        if (mLastDate.equals(getTodayDate())) {
            return false;
        } else {
            mLastDate = getTodayDate();
            return true;
        }
    }

    @Override
    protected IntentFilter getReceiverFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        return filter;
    }

    public Drawable getStableBackground() {
        return mCalendarBackground;
    }

    public DynamicIconDrawCallback getDynamicIconDrawCallback() {
        return mDrawCallback;
    }

    @Override
    protected void draw(Canvas canvas, View icon, float scale, int[] center) {
        if (canvas == null || center == null || !(icon instanceof BubbleTextView)) {
            return;
        }

        String day;
        if (mIsChecked) {
            day = getTodayDate();
        } else  {
            day = Integer.toString(mDefaultDate[2]);
        }

        int iconSize = ((BubbleTextView) icon).getIconSize();
        float dateSize = iconSize * DATE_SIZE_FACTOR;
        mDatePaint.setTextSize(scale * dateSize);

        Paint.FontMetrics fm = mDatePaint.getFontMetrics();
        float dateBaseline = (float)(center[1] - fm.ascent * 0.5);

        canvas.save();
        canvas.drawText(day, center[0], dateBaseline, mDatePaint);
        canvas.restore();
    }

    private String getTodayDate() {
        Calendar c = Calendar.getInstance();
        int date = c.get(Calendar.DATE);
        return String.valueOf(date);
    }
}
