
package com.android.mmsfolderview.parser.data;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import com.android.mmsfolderview.parser.XmlDomParser;
import com.android.mmsfolderview.parser.process.ProcessGroup;

import android.content.Context;
import android.util.Log;

public class TimeQuantumDataBuilder {

    private static final String TAG = "TimeQuantumDataBuilder";
    private static final String R_STRING = "com.android.mmsfolderview.R$string";
    private static final boolean isDebug = true;
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss z");

    public static String[] buildSelectionArgs(TimeQuantumItem item) {

        String[] argskey = item.mArgskey;
        HashMap<String, String> argsValue = item.mArgsValue;
        if (argskey == null || argskey.length == 0 || argskey.length != argsValue.size()) {
            Log.d(XmlDomParser.TAG, TAG + ".return null selectionArgs, item.mDisplayName="
                    + item.mDisplayName);
            return null;
        }
        String[] selectionArgs = new String[argskey.length];
        for (int i = 0; i < argskey.length; i++) {
            selectionArgs[i] = argsValue.get(argskey[i]);
            if (isDebug) {
                Log.d(XmlDomParser.TAG, TAG + ".getSelectionArgs: argskey[" + i + "] = "
                        + argskey[i] + ",  selectionArgs[" + i + "] = " + selectionArgs[i]);
            }
            selectionArgs[i] = ProcessGroup.getInstance().getIParamProcess(argskey[i])
                    .process(null, selectionArgs[i], null);
            if (isDebug) {
                mDateTimeFormat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
                Log.d(XmlDomParser.TAG,
                        TAG
                                + ".getSelectionArgs: process name = "
                                + ProcessGroup.getInstance().getIParamProcess(argskey[i])
                                        .getProcessName() + ", after process selectionArgs[" + i
                                + "] = " + mDateTimeFormat.format(Long.valueOf(selectionArgs[i])));
            }
        }
        return selectionArgs;
    }

    public static String[] getDisplayTexts(Context context, ArrayList<TimeQuantumItem> items) {
        String[] displayTexts = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            int textId = getLocalTextId(items.get(i).mDisplayName);
            displayTexts[i] = context.getResources().getString(textId);
            Log.d(XmlDomParser.TAG, TAG + ".getDisplayTexts: displayTexts[" + i + "] = "
                    + displayTexts[i]);
        }
        return displayTexts;
    }

    private static int getLocalTextId(String name) {
        try {
            final Class<?> clazzR = Class.forName(R_STRING);// inner class
            if (clazzR != null) {
                Field field = clazzR.getField(name);
                return field.getInt(null);// If this field is static, the object
                                          // argument is ignored.
            }
        } catch (final Exception e) {
            Log.e(XmlDomParser.TAG, TAG + ".getLocalText: happen error", e);
        }
        return -1;
    }

}
