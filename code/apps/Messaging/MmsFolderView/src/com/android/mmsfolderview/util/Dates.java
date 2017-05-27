/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.mmsfolderview.util;

import android.content.Context;
import android.text.format.DateUtils;
import android.text.format.Time;

import com.android.mmsfolderview.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Collection of date utilities.
 */
public class Dates {

    public static final int FORCE_24_HOUR = DateUtils.FORMAT_24HOUR;
    public static final int FORCE_12_HOUR = DateUtils.FORMAT_12HOUR;
    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;

    private static Time sThenTime;

    public static CharSequence getMessageTimeString(Context context, final long time) {
        return getTimeString(context, time, false /* abbreviated */, false /* minPeriodToday */);
    }

    private static CharSequence getTimeString(Context context, final long time,
            final boolean abbreviated, final boolean minPeriodToday) {
        int flags;
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            flags = FORCE_24_HOUR;
        } else {
            flags = FORCE_12_HOUR;
        }
        return getTimestamp(context, time, System.currentTimeMillis(), abbreviated, context
                .getResources().getConfiguration().locale, flags, minPeriodToday);
    }

    public static CharSequence getTimestamp(Context context, final long time, final long now,
            final boolean abbreviated, final Locale locale, final int flags,
            final boolean minPeriodToday) {
        final long timeDiff = now - time;

        if (!minPeriodToday && timeDiff < DateUtils.MINUTE_IN_MILLIS) {
            return getLessThanAMinuteOldTimeString(context, abbreviated);
        } else if (!minPeriodToday && timeDiff < DateUtils.HOUR_IN_MILLIS) {
            return getLessThanAnHourOldTimeString(context, timeDiff, flags);
        } else if (getNumberOfDaysPassed(time, now) == 0) {
            return getTodayTimeStamp(context, time, flags);
        } else if (timeDiff < DateUtils.WEEK_IN_MILLIS) {
            return getThisWeekTimestamp(context, time, locale, abbreviated, flags);
        } else if (timeDiff < DateUtils.YEAR_IN_MILLIS) {
            return getThisYearTimestamp(context, time, locale, abbreviated, flags);
        } else {
            return getOlderThanAYearTimestamp(context, time, locale, abbreviated, flags);
        }
    }

    private static CharSequence getLessThanAMinuteOldTimeString(Context context,
            final boolean abbreviated) {
        return context.getResources().getText(
                abbreviated ? R.string.posted_just_now : R.string.posted_now);
    }

    private static CharSequence getLessThanAnHourOldTimeString(Context context,
            final long timeDiff, final int flags) {
        final long count = (timeDiff / MINUTE_IN_MILLIS);
        final String format = context.getResources().getQuantityString(R.plurals.num_minutes_ago,
                (int) count);
        return String.format(format, count);
    }

    private static synchronized long getNumberOfDaysPassed(final long date1, final long date2) {
        if (sThenTime == null) {
            sThenTime = new Time();
        }
        sThenTime.set(date1);
        final int day1 = Time.getJulianDay(date1, sThenTime.gmtoff);
        sThenTime.set(date2);
        final int day2 = Time.getJulianDay(date2, sThenTime.gmtoff);
        return Math.abs(day2 - day1);
    }

    private static CharSequence getTodayTimeStamp(Context context, final long time, final int flags) {
        return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_TIME | flags);
    }

    private static CharSequence getThisWeekTimestamp(Context context, final long time,
            final Locale locale, final boolean abbreviated, final int flags) {
        if (abbreviated) {
            return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_WEEKDAY
                    | DateUtils.FORMAT_ABBREV_WEEKDAY | flags);
        } else {
            if (locale.equals(Locale.US)) {
                return getExplicitFormattedTime(time, flags, "EEE HH:mm", "EEE h:mmaa");
            } else {
                return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_WEEKDAY
                        | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_WEEKDAY | flags);
            }
        }
    }

    private static CharSequence getThisYearTimestamp(Context context, final long time,
            final Locale locale, final boolean abbreviated, final int flags) {
        if (abbreviated) {
            return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE
                    | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_NO_YEAR | flags);
        } else {
            if (locale.equals(Locale.US)) {
                return getExplicitFormattedTime(time, flags, "MMM d, HH:mm", "MMM d, h:mmaa");
            } else {
                return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH
                        | DateUtils.FORMAT_NO_YEAR | flags);
            }
        }
    }

    private static CharSequence getOlderThanAYearTimestamp(Context context, final long time,
            final Locale locale, final boolean abbreviated, final int flags) {
        if (abbreviated) {
            if (locale.equals(Locale.US)) {
                return getExplicitFormattedTime(time, flags, "M/d/yy", "M/d/yy");
            } else {
                return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE);
            }
        } else {
            if (locale.equals(Locale.US)) {
                return getExplicitFormattedTime(time, flags, "M/d/yy, HH:mm", "M/d/yy, h:mmaa");
            } else {
                return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NUMERIC_DATE
                        | DateUtils.FORMAT_SHOW_YEAR | flags);
            }
        }
    }

    private static CharSequence getExplicitFormattedTime(final long time, final int flags,
            final String format24, final String format12) {
        SimpleDateFormat formatter;
        if ((flags & FORCE_24_HOUR) == FORCE_24_HOUR) {
            formatter = new SimpleDateFormat(format24);
        } else {
            formatter = new SimpleDateFormat(format12);
        }
        return formatter.format(new Date(time));
    }
}
