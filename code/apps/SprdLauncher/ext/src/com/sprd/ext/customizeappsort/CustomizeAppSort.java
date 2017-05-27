package com.sprd.ext.customizeappsort;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.Xml;

import com.android.sprdlauncher3.AppInfo;
import com.android.sprdlauncher3.R;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LogUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by SPRD on 2017/2/20.
 */
public class CustomizeAppSort {
    private static final String TAG = "CustomizeAppSort";

    private static final String XML_ITEM_TAG = "App";

    private static HashMap<Integer, Pair<String, String>> sCustomizePositions;
    private static Context sContext;
    private static CustomizeAppSort INSTANCE;

    private boolean mHasCustomizeAppData = false;

    /**
     * private constructor here, It is a singleton class.
     */
    private CustomizeAppSort() {
        if (!FeatureOption.SPRD_CUSTOMIZEAPPSORT_SUPPORT) {
            return;
        }
        if (sContext == null) {
            LogUtils.e(TAG, "CustomizeAppSort inited before app context set");
        }
    }

    public static void setContext(Context context) {
        if (sContext != null) {
            LogUtils.w(TAG, "setContext called twice! old=" + sContext + " new=" + context);
        }
        sContext = context.getApplicationContext();
    }

    public static CustomizeAppSort getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomizeAppSort();
        }
        return INSTANCE;
    }

    public final boolean hasCustomizeAppsData() {
        return mHasCustomizeAppData;
    }

    public final void loadCustomizeAppsData() {
        if (sContext == null) {
            return;
        }

        if (sCustomizePositions == null) {
            sCustomizePositions = loadCustomizeAppPositions(sContext);
            mHasCustomizeAppData = !sCustomizePositions.isEmpty();
            LogUtils.d(TAG, "CustomizeAppSort loadCustomizeAppsData:" + mHasCustomizeAppData );
        }
    }

    public final void sortApps(List<AppInfo> apps) {
        if (sContext == null) {
            return;
        }

        if (sCustomizePositions == null || sCustomizePositions.size() == 0) {
            LogUtils.d(TAG, "sortApps, no customize apps data.");
            return;
        }

        ArrayList<ComponentName> sortedCNs = new ArrayList<>();
        HashMap<ComponentName, AppInfo> maps = new HashMap<>();

        for (AppInfo app : apps) {
            sortedCNs.add(app.componentName);
            maps.put(app.componentName, app);
        }

        onSortApps(sortedCNs);

        // refresh mApps
        apps.clear();
        for (ComponentName cn : sortedCNs) {
            apps.add(maps.get(cn));
        }
    }

    private void onSortApps(ArrayList<ComponentName> componentNames) {
        LogUtils.d(TAG, "onSortApps customize implementation.");
        TreeMap<Integer, ComponentName> sortedMaps = new TreeMap<>();

        // find the customize component in componentNames
        Pair<String, String> pair;
        Map.Entry<Integer, Pair<String, String>> entry;

        for (ComponentName cn : componentNames) {
            for (Map.Entry<Integer, Pair<String, String>> integerPairEntry : sCustomizePositions.entrySet()) {
                entry = integerPairEntry;
                pair = entry.getValue();

                if (pair.first.equals( cn.getPackageName() )) {
                    if (pair.second == null || pair.second.equals( cn.getClassName() )) {
                        sortedMaps.put( entry.getKey(), cn );
                        break;
                    }
                }
            }
        }

        // remove the found component
        Iterator<Map.Entry<Integer, ComponentName>> it = sortedMaps.entrySet().iterator();
        while (it.hasNext()) {
            componentNames.remove(it.next().getValue());
        }

        // insert at the customize position
        Map.Entry<Integer, ComponentName> ent;
        it = sortedMaps.entrySet().iterator();
        while (it.hasNext()) {
            ent = it.next();
            if (ent.getKey() > componentNames.size()) {
                // append to last position
                componentNames.add(ent.getValue());
            } else {
                // insert at specific position
                componentNames.add(ent.getKey(), ent.getValue());
            }
        }
    }

    /**
     * Get customize app's position. The result is a map, the key indicate the
     * customize position, and the value is a pair of package name and class
     * name.
     * @param context cTx
     * @return customize app map
     */
    private HashMap<Integer, Pair<String, String>> loadCustomizeAppPositions(Context context) {
        HashMap<Integer, Pair<String, String>> customizePositions = new HashMap<>();
        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.customize_app_positions);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (XML_ITEM_TAG.equals(tagName)) {
                        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SprdAppInfo);
                        String pkgName = a.getString(R.styleable.SprdAppInfo_pkgName);
                        String clsName = a.getString(R.styleable.SprdAppInfo_clsName);
                        int position = a.getInteger(R.styleable.SprdAppInfo_position, 0);

                        // package name must not be null or empty
                        if (pkgName != null && pkgName.length() != 0) {
                            customizePositions.put(position, new Pair<>( pkgName, clsName ));
                        }
                        a.recycle();
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException | RuntimeException e) {
            LogUtils.w(TAG, "parse xml failed", e);
        }
        return customizePositions;
    }

}
