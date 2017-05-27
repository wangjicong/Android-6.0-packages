/** SPRD: Modify for bug473571, add lunar info */
package com.sprd.calendar.lunar;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LunarCalendarConvertUtil {

    public static final boolean SUPPORT_LUNAR = true;
    private final static short[] mLunarCalendarBaseInfo = new short[] { 0x4bd,
            0x4ae, 0xa57, 0x54d, 0xd26, 0xd95, 0x655, 0x56a, 0x9ad, 0x55d,
            0x4ae, 0xa5b, 0xa4d, 0xd25, 0xd25, 0xb54, 0xd6a, 0xada, 0x95b,
            0x497, 0x497, 0xa4b, 0xb4b, 0x6a5, 0x6d4, 0xab5, 0x2b6, 0x957,
            0x52f, 0x497, 0x656, 0xd4a, 0xea5, 0x6e9, 0x5ad, 0x2b6, 0x86e,
            0x92e, 0xc8d, 0xc95, 0xd4a, 0xd8a, 0xb55, 0x56a, 0xa5b, 0x25d,
            0x92d, 0xd2b, 0xa95, 0xb55, 0x6ca, 0xb55, 0x535, 0x4da, 0xa5d,
            0x457, 0x52d, 0xa9a, 0xe95, 0x6aa, 0xaea, 0xab5, 0x4b6, 0xaae,
            0xa57, 0x526, 0xf26, 0xd95, 0x5b5, 0x56a, 0x96d, 0x4dd, 0x4ad,
            0xa4d, 0xd4d, 0xd25, 0xd55, 0xb54, 0xb5a, 0x95a, 0x95b, 0x49b,
            0xa97, 0xa4b, 0xb27, 0x6a5, 0x6d4, 0xaf4, 0xab6, 0x957, 0x4af,
            0x497, 0x64b, 0x74a, 0xea5, 0x6b5, 0x55c, 0xab6, 0x96d, 0x92e,
            0xc96, 0xd95, 0xd4a, 0xda5, 0x755, 0x56a, 0xabb, 0x25d, 0x92d,
            0xcab, 0xa95, 0xb4a, 0xbaa, 0xad5, 0x55d, 0x4ba, 0xa5b, 0x517,
            0x52b, 0xa93, 0x795, 0x6aa, 0xad5, 0x5b5, 0x4b6, 0xa6e, 0xa4e,
            0xd26, 0xea6, 0xd53, 0x5aa, 0x76a, 0x96d, 0x4bd, 0x4ad, 0xa4d,
            0xd0b, 0xd25, 0xd52, 0xdd4, 0xb5a, 0x56d, 0x55b, 0x49b, 0xa57,
            0xa4b, 0xaa5, 0xb25, 0x6d2, 0xada };
    private final static byte[] mLunarCalendarSpecialInfo = new byte[] { 0x08,
            0x00, 0x00, 0x05, 0x00, 0x00, 0x14, 0x00, 0x00, 0x02, 0x00, 0x06,
            0x00, 0x00, 0x15, 0x00, 0x00, 0x02, 0x00, 0x17, 0x00, 0x00, 0x05,
            0x00, 0x00, 0x14, 0x00, 0x00, 0x02, 0x00, 0x06, 0x00, 0x00, 0x05,
            0x00, 0x00, 0x13, 0x00, 0x17, 0x00, 0x00, 0x16, 0x00, 0x00, 0x14,
            0x00, 0x00, 0x02, 0x00, 0x07, 0x00, 0x00, 0x15, 0x00, 0x00, 0x13,
            0x00, 0x08, 0x00, 0x00, 0x06, 0x00, 0x00, 0x04, 0x00, 0x00, 0x03,
            0x00, 0x07, 0x00, 0x00, 0x05, 0x00, 0x00, 0x04, 0x00, 0x08, 0x00,
            0x00, 0x16, 0x00, 0x00, 0x04, 0x00, 0x0a, 0x00, 0x00, 0x06, 0x00,
            0x00, 0x05, 0x00, 0x00, 0x03, 0x00, 0x08, 0x00, 0x00, 0x05, 0x00,
            0x00, 0x04, 0x00, 0x00, 0x02, 0x00, 0x07, 0x00, 0x00, 0x05, 0x00,
            0x00, 0x04, 0x00, 0x09, 0x00, 0x00, 0x16, 0x00, 0x00, 0x04, 0x00,
            0x00, 0x02, 0x00, 0x06, 0x00, 0x00, 0x05, 0x00, 0x00, 0x03, 0x00,
            0x07, 0x00, 0x00, 0x16, 0x00, 0x00, 0x05, 0x00, 0x00, 0x02, 0x00,
            0x07, 0x00, 0x00, 0x15, 0x00, 0x00 };
    private final static long[] mSolarTermInfo = new long[] { 0, 21208, 42467,
            63836, 85337, 107014, 128867, 150921, 173149, 195551, 218072,
            240693, 263343, 285989, 308563, 331033, 353350, 375494, 397447,
            419210, 440795, 462224, 483532, 504758 };
    private final static int[] mAllLunarDays = new int[] {25219,
            25573, 25928, 26312, 26666, 27020, 27404, 27758, 28142, 28496, 28851,
            29235, 29590, 29944, 30328, 30682, 31066, 31420, 31774, 32158, 32513,
            32868, 33252, 33606, 33960, 34343, 34698, 35082, 35436, 35791, 36175,
            36529, 36883, 37267, 37621, 37976, 38360, 38714, 39099, 39453, 39807,
            40191, 40545, 40899, 41283, 41638, 42022, 42376, 42731, 43115, 43469,
            43823, 44207, 44561, 44916, 45300, 45654, 46038, 46392, 46746, 47130,
            47485, 47839, 48223, 48578, 48962, 49316, 49670, 50054, 50408, 50762 };
    private final static int[] mLunarDays = new int[] { 354,
            355, 384, 354, 354, 384, 354, 384, 354, 355, 384,
            355, 354, 384, 354, 384, 354, 354, 384, 355, 355,
            384, 354, 354, 383, 355, 384, 354, 355, 384, 354,
            354, 384, 354, 355, 384, 354, 385, 354, 354, 384,
            354, 354, 384, 355, 384, 354, 355, 384, 354, 354,
            384, 354, 355, 384, 354, 384, 354, 354, 384, 355,
            354, 384, 355, 384, 354, 354, 384, 354, 354, 384,
            355, 355, 384, 354, 384, 354, 354, 384, 354, 355 };
    private final static Calendar mOffDateCalendar;
    private final static long mMilliSecondsForSolarTerm;
    private final static int mBaseYear = 1900;
    private final static int mStartYear = 1969;
    private static int mBeginYear = 1969;
    private final static int mOutBoundYear = 2050;
    private static long mBaseDayTime = 0;
    private final static int mBigMonthDays = 30;
    private final static int mSmallMonthDays = 29;

    static {
        // use Date.getTime to return milliseconds for we don't need timezone info
        // Date(0, 0, 31) represent 1900-1-31, it is the first day of Gengzi year
        // in lunar
        mBaseDayTime = new Date(0, 0, 31).getTime();
        //correct the algorithm of getting solar terms
        mOffDateCalendar = Calendar.getInstance();
        mOffDateCalendar.set(1900, 0, 6, 2, 5, 0);
        mMilliSecondsForSolarTerm = mOffDateCalendar.getTime().getTime();
    }

    /*
     * correct the algorithm of getting solar terms
     */
    public static int getSolarTermDayOfMonth(int year, int n) {
        mOffDateCalendar.setTime(new Date((long) ((31556925974.7 * (year - 1900)
                + mSolarTermInfo[n] * 60000L) + mMilliSecondsForSolarTerm)));
        return mOffDateCalendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int getLunarMonthDays(int lunarYear, int lunarMonth) {
        if (isLunarBigMonth(lunarYear, lunarMonth)) {
            return mBigMonthDays;
        } else {
            return mSmallMonthDays;
        }
    }

    public static boolean isLunarBigMonth(int lunarYear, int lunarMonth) {
        short lunarYearBaseInfo = mLunarCalendarBaseInfo[lunarYear - mBaseYear];
        if ((lunarYearBaseInfo & (0x01000 >>> lunarMonth)) != 0) {
            return true;
        } else {
            return false;
        }
    }

    final public static int getYearDays(int lunarYear) {
        int retSum = 0;
        for (int iLunarMonth = 1; iLunarMonth <= 12; iLunarMonth++) {
            retSum += getLunarMonthDays(lunarYear, iLunarMonth);
        }
        return (retSum + getLeapMonthDays(lunarYear));
    }

    final public static int getLeapMonth(int lunarYear) {
        return mLunarCalendarSpecialInfo[lunarYear - mBaseYear] & 0xf;
    }

    final public static int getLeapMonthDays(int lunarYear) {
        if (getLeapMonth(lunarYear) == 0) {
            return 0;
        } else if ((mLunarCalendarSpecialInfo[lunarYear - mBaseYear] & 0x10) != 0) {
            return mBigMonthDays;
        } else {
            return mSmallMonthDays;
        }
    }

    public static void parseLunarCalendar(int year, int month, int day,
            LunarCalendar lunarCalendar) {
        if (lunarCalendar == null) {
            return;
        }

        int leapLunarMonth = 0;
        Date presentDate = null;
        boolean isLeapMonth = false;
        presentDate = new Date(year - mBaseYear, month, day);
        // we use Math.ceil() here because offsetDayNum some time be truncate
        // this will cause we lost one day
        int offsetDayNum = (int) Math
                .ceil((presentDate.getTime() - mBaseDayTime) * 1.0 / 86400000L);

        mBeginYear = year-1;
        if(mBeginYear<1969){
            mBeginYear = 1969;
        }
        offsetDayNum -= mAllLunarDays[mBeginYear-mStartYear];

        int lunarYear = 0;
        int lunarMonth = 0;
        int lunarDay = 0;

        for (lunarYear = mBeginYear; lunarYear < mOutBoundYear; lunarYear++) {
            int daysOfLunarYear = mLunarDays[lunarYear-mStartYear];
            if (offsetDayNum < daysOfLunarYear) {
                break;
            }
            offsetDayNum -= daysOfLunarYear;
        }
        if (offsetDayNum < 0 || lunarYear == mOutBoundYear) {
            return;
        }

        leapLunarMonth = getLeapMonth(lunarYear);

        for (lunarMonth = 1; lunarMonth <= 12; lunarMonth++) {
            int daysOfLunarMonth = 0;
            if (isLeapMonth) {
                daysOfLunarMonth = getLeapMonthDays(lunarYear);
            } else {
                daysOfLunarMonth = getLunarMonthDays(lunarYear, lunarMonth);
            }

            if (offsetDayNum < daysOfLunarMonth) {
                break;
            } else {
                offsetDayNum -= daysOfLunarMonth;
                if (lunarMonth == leapLunarMonth) {
                    if (!isLeapMonth) {
                        lunarMonth--;
                        isLeapMonth = true;
                    } else {
                        isLeapMonth = false;
                    }
                }
            }
        }

        lunarDay = offsetDayNum + 1;

        lunarCalendar.mLunarYear = lunarYear;
        lunarCalendar.mLunarMonth = lunarMonth;
        lunarCalendar.mLunarDay = lunarDay;
        lunarCalendar.mIsLeapMonth = isLeapMonth;

        lunarCalendar.mSolarYear = year;
        lunarCalendar.mSolarMonth = month;
        lunarCalendar.mSolarDay = day;
    }

    // lunar optimization
    public static void parseLunarCalendarYear(int year, int month, int day,
            LunarCalendar lunarCalendar) {
        if (lunarCalendar == null) {
            return;
        }

        Date presentDate = null;
        presentDate = new Date(year - mBaseYear, month, day);
        // we use Math.ceil() here because offsetDayNum some time be truncate
        // this will cause we lost one day
        int offsetDayNum = (int) Math
                .ceil((presentDate.getTime() - mBaseDayTime) * 1.0 / 86400000L);

        mBeginYear = year - 1;
        if (mBeginYear < 1969) {
            mBeginYear = 1969;
        }
        offsetDayNum -= mAllLunarDays[mBeginYear - mStartYear];

        int lunarYear = 0;

        for (lunarYear = mBeginYear; lunarYear < mOutBoundYear; lunarYear++) {
            int daysOfLunarYear = mLunarDays[lunarYear - mStartYear];
            if (offsetDayNum < daysOfLunarYear) {
                break;
            }
            offsetDayNum -= daysOfLunarYear;
        }
        if (offsetDayNum < 0 || lunarYear == mOutBoundYear) {
            return;
        }

        lunarCalendar.mLunarYear = lunarYear;
    }

    public static boolean isLunarSetting() {
        String language = getLanguageEnv();

        if (language != null
                && (language.trim().equals("zh-CN") || language.trim().equals(
                        "zh-TW"))) {
            return true;
        } else {
            return false;
        }
    }

    private static String getLanguageEnv() {
        Locale l = Locale.getDefault();
        String language = l.getLanguage();
        String country = l.getCountry().toLowerCase();
        if ("zh".equals(language)) {
            if ("cn".equals(country)) {
                language = "zh-CN";
            } else if ("tw".equals(country)) {
                language = "zh-TW";
            }
        } else if ("pt".equals(language)) {
            if ("br".equals(country)) {
                language = "pt-BR";
            } else if ("pt".equals(country)) {
                language = "pt-PT";
            }
        }
        return language;
    }

}