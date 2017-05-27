package com.sprd.ext.dynamicIcon.calendar;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by SPRD on 10/21/16.
 */
public class DynamicCalendar extends DynamicIcon {

    private static final String CALENDAR_CONFIG_KEY = "ro.launcher.defaultcalendar";
    // The proportion of the date font occupied in the dynamic calendar icon.
    private static final float DATE_SIZE_FACTOR = 0.6f;
    // The proportion of the week font occupied in the dynamic calendar icon.
    private static final float WEEK_SIZE_FACTOR = 0.17f;

    private String mLastDate = "";
    // The default date should not be changed, because it should be consistent with that in the calendar app icon.
    // Unless the default date and the calendar app icon change together.
    // Month value is 0-based. e.g., 0 for January.
    private int[] mDefaultDate = new int[]{2016, 10, 1};
    private Paint mDatePaint;
    private Paint mWeekPaint;
    private Drawable mCalendarBackground;

    public DynamicCalendar(Context context, int type) {
        super(context, type);

        boolean defValue = SystemProperties.getBoolean(CALENDAR_CONFIG_KEY,
                mContext.getResources().getBoolean(R.bool.config_show_dynamic_calendar));
        mIsChecked = UtilitiesExt.getLauncherSettingsBoolean(mContext,
                DynamicIconSettingsFragment.PRE_DYNAMIC_CALENDAR, defValue);
    }

    @Override
    protected void init() {
        Typeface font = Typeface.create("sans-serif-thin", Typeface.NORMAL);

        mDatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDatePaint.setTypeface(font);
        mDatePaint.setTextAlign(Paint.Align.CENTER);
        mDatePaint.setAntiAlias(true);

        mWeekPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWeekPaint.setTextAlign(Paint.Align.CENTER);
        mWeekPaint.setTypeface(font);
        mWeekPaint.setColor(mContext.getResources().getColor(R.color.dynamic_calendar_week));
        mWeekPaint.setAntiAlias(true);

        mCalendarBackground = ContextCompat.getDrawable(mContext, R.drawable.ic_calendar_plate);
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
        String dayOfWeek;
        if (mIsChecked) {
            day = getTodayDate();
            dayOfWeek = getTodayWeek();
        } else  {
            GregorianCalendar date = new GregorianCalendar(mDefaultDate[0], mDefaultDate[1], mDefaultDate[2]);
            SimpleDateFormat format = new SimpleDateFormat("EEEE");
            String weekday = format.format(date.getTime());

            day = Integer.toString(mDefaultDate[2]);
            dayOfWeek = weekday;
        }

        int iconSize = ((BubbleTextView) icon).getIconSize();
        float dateSize = iconSize * DATE_SIZE_FACTOR;
        float weekSize = iconSize * WEEK_SIZE_FACTOR;
        mDatePaint.setTextSize(scale * dateSize);
        mWeekPaint.setTextSize(scale * weekSize);

        Paint.FontMetrics fm = mDatePaint.getFontMetrics();
        float dateHeight = -fm.ascent;
        float dateBaseline = (float)(center[1] - fm.ascent * 0.58);

        canvas.save();
        canvas.drawText(dayOfWeek, center[0], dateBaseline - dateHeight, mWeekPaint);
        canvas.drawText(day, center[0], dateBaseline, mDatePaint);
        canvas.restore();
    }

    private String getTodayDate() {
        Calendar c = Calendar.getInstance();
        int date = c.get(Calendar.DATE);
        return String.valueOf(date);
    }

    private String getTodayWeek() {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("EEEE");
        return format.format(date);
    }
}