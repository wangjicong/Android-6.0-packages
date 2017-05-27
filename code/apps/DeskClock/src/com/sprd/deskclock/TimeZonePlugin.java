package com.sprd.deskclock;

import java.io.File;

import android.app.Activity;
import android.app.AddonManager;
import android.content.Context;
import android.content.Intent;
import com.android.deskclock.R;
import com.android.deskclock.worldclock.CityObj;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParserException;

import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import libcore.icu.ICU;
import libcore.icu.TimeZoneNames;

public class TimeZonePlugin {
    private static final String XMLTAG_TIMEZONE = "timezone";
    private final HashSet<String> mLocalZones = new HashSet<String>();
    private final Date mNow = Calendar.getInstance().getTime();
    private final SimpleDateFormat mZoneNameFormatter = new SimpleDateFormat("zzzz");
    private static TimeZonePlugin sInstance;
    private static final String TAG = "TimeZonePlugin";
    private static Context mAddonContext;

    public static TimeZonePlugin getInstance(Context context) {
        AddonManager addonManager = new AddonManager(context);
        mAddonContext = context;
        sInstance = (TimeZonePlugin) addonManager.getAddon(R.string.timezone_plugin, TimeZonePlugin.class);
        Log.d(TAG, "TimeZonePlugin getInstance: plugin = " + context.getString(R.string.timezone_plugin));
        return sInstance;
    }

    public TimeZonePlugin() {
    }

    public CityObj[] getCityTimeZone(){
        ArrayList<CityObj> citiesList = getZones(mAddonContext);
        CityObj[] citiesArray = new CityObj[citiesList.size()];
        for(int i = 0; i < citiesList.size(); i++) {
            if (i >= citiesArray.length) {
                break;
            }
            citiesArray[i] = citiesList.get(i);
        }
        return citiesArray;
    }

    private String getCityTimeZoneItem(String timezoneid){
        final long date = Calendar.getInstance().getTimeInMillis();
        final TimeZone tz = TimeZone.getTimeZone(timezoneid);
        final int offset = tz.getOffset(date);
        final int p = Math.abs(offset);
        final StringBuilder name = new StringBuilder();
        name.append("GMT");

        if (offset < 0) {
            name.append('-');
        } else {
            name.append('+');
        }

        name.append(p / DateUtils.HOUR_IN_MILLIS);
        name.append(':');

        int min = p / 60000;
        min %= 60;

        if (min < 10) {
            name.append('0');
        }
        name.append(min);
        return name.toString();
    }

    public int getTimezoneOffset (TimeZone tz, long time) {
        return tz.getOffset(time);
    }

    private ArrayList<CityObj> getZones(Context context) {
        ArrayList<CityObj> cities = new ArrayList<CityObj>();
        int len = 0;
        String displayName;
        for (String olsonId : TimeZoneNames.forLocale(Locale.getDefault())) {
            mLocalZones.add(olsonId);
        }
        try {
            XmlResourceParser xrp = context.getResources().getXml(R.xml.timezones);
            while (xrp.next() != XmlResourceParser.START_TAG) {
                continue;
            }
            xrp.next();
            while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                while (xrp.getEventType() != XmlResourceParser.START_TAG && xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
                    xrp.next();
                }
                if (xrp.getName().equals(XMLTAG_TIMEZONE)) {
                    String olsonId = xrp.getAttributeValue(0);
                    final TimeZone tz = TimeZone.getTimeZone(olsonId);
                    if (mLocalZones.contains(olsonId)) {
                        // Within a country, we just use the local name for the time zone.
                        mZoneNameFormatter.setTimeZone(tz);
                        displayName = mZoneNameFormatter.format(mNow);
                    } else {
                        // For other countries' time zones, we use the exemplar location.
                        final String localeName = Locale.getDefault().toString();
                        displayName = TimeZoneNames.getExemplarLocation(localeName, olsonId);
                    }
                    len++;
                    cities.add(new CityObj(displayName.toString(), getCityTimeZoneItem(olsonId.toString()),
                            "C" + len, getCityIndex(displayName.toString())));
                }
                while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                    xrp.next();
                }
                xrp.next();
            }
            xrp.close();
        } catch (XmlPullParserException xppe) {
            Log.d(TAG, "Ill-formatted timezones.xml file");
        } catch (java.io.IOException ioe) {
            Log.d(TAG, "Unable to read timezones.xml file");
        }
        return cities;
    }

    private String getCityIndex(String displayName) {
        final String parseString = displayName;
        final int separatorIndex = parseString.indexOf("=");
        final String index;
        if (parseString.length() <= 1 && separatorIndex >= 0) {
            Log.d(TAG,"Cannot parse city name %s; skipping"+parseString);
            return null;
        }
        if (separatorIndex == 0) {
            // Default to using second character (the first character after the = separator)
            // as the index.
            index = parseString.substring(1, 2);
        } else if (separatorIndex == -1) {
            // Default to using the first character as the index
            index = parseString.substring(0, 1);
        } else {
             index = parseString.substring(0, separatorIndex);
        }
        return index;
    }
}
