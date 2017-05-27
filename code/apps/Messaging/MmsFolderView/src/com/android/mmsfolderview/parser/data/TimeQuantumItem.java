
package com.android.mmsfolderview.parser.data;

import java.util.HashMap;
import org.w3c.dom.Element;

import com.android.mmsfolderview.parser.XmlDomParser;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class TimeQuantumItem {

    private static final String TAG = "TimeQuantumItem";
    public static final String FLAG_DATA = "data";
    public static final String FLAG_ITEM = "Item";
    public static final String FLAG_PACKAGENAME = "packagename";
    public static final String FLAG_DISPLAY = "display";
    public static final String FLAG_SQL = "sql";
    public static final String FLAG_URI = "uri";
    public static final String FLAG_CONDITION = "condition";
    public static final String FLAG_WHERE = "where";
    public static final String FLAG_GROUPBY = "groupby";
    public static final String FLAG_HAVING = "having";
    public static final String FLAG_ORDERBY = "orderby";
    public static final String FLAG_PROJECT = "project";
    public static final String FLAG_ARGS = "args";

    private static final boolean isDebug = true;
    public String mPackageName;
    public String mDisplayName;
    public String mCondition;
    public Uri mUri;
    public String mGroupby;
    public String mHaving;
    public String mOrderby;
    public String[] mProjects = null;
    public String[] mArgskey = null;
    public HashMap<String, String> mArgsValue = new HashMap<String, String>();

    public TimeQuantumItem() {
    }

    public void clear() {
        mPackageName = null;
        mDisplayName = null;
        mCondition = null;
        mProjects = null;
        mUri = null;
        mGroupby = null;
        mHaving = null;
        mOrderby = null;
        mArgskey = null;
        mArgsValue.clear();
    }

    public boolean InitFromElement(Element itemElement) {
        try {
            mPackageName = itemElement.getAttributes().getNamedItem(FLAG_PACKAGENAME)
                    .getNodeValue();
            mDisplayName = itemElement.getAttributes().getNamedItem(FLAG_DISPLAY).getNodeValue();

            Element sqlElement = (Element) itemElement.getElementsByTagName(FLAG_SQL).item(0);
            String uri = sqlElement.getElementsByTagName(FLAG_URI).item(0).getFirstChild() == null ? null
                    : sqlElement.getElementsByTagName(FLAG_URI).item(0).getFirstChild()
                            .getNodeValue();
            if (!TextUtils.isEmpty(uri)) {
                mUri = Uri.parse(uri);
            }
            mProjects = getProjects(sqlElement);
            Element conditionElement = (Element) itemElement.getElementsByTagName(FLAG_CONDITION)
                    .item(0);
            mCondition = conditionElement.getFirstChild() == null ? null : conditionElement
                    .getAttributes().getNamedItem(FLAG_WHERE).getNodeValue();
            mArgskey = getArgs(conditionElement);
            setArgsValue(mArgskey, conditionElement);
            mGroupby = sqlElement.getElementsByTagName(FLAG_GROUPBY).item(0).getFirstChild() == null ? null
                    : sqlElement.getElementsByTagName(FLAG_GROUPBY).item(0).getFirstChild()
                            .getNodeValue();
            mHaving = sqlElement.getElementsByTagName(FLAG_HAVING).item(0).getFirstChild() == null ? null
                    : sqlElement.getElementsByTagName(FLAG_HAVING).item(0).getFirstChild()
                            .getNodeValue();
            mOrderby = sqlElement.getElementsByTagName(FLAG_ORDERBY).item(0).getFirstChild() == null ? null
                    : sqlElement.getElementsByTagName(FLAG_ORDERBY).item(0).getFirstChild()
                            .getNodeValue();
        } catch (Exception e) {
            Log.e(XmlDomParser.TAG, TAG + ".InitFromElement: happens error:", e);
            return false;
        }
        if (isDebug) {
            debug();
        }
        return true;
    }

    private void setArgsValue(String[] args, Element compriseElement) {
        if (args != null && args.length != 0) {
            for (int i = 0; i < args.length; i++) {
                String value = compriseElement.getElementsByTagName(args[i]).item(0)
                        .getFirstChild().getNodeValue();
                mArgsValue.put(args[i], value);
            }
        }
    }

    private String[] getProjects(Element compriseElement) {
        String projects = compriseElement.getElementsByTagName(FLAG_PROJECT).item(0)
                .getFirstChild() == null ? null : compriseElement
                .getElementsByTagName(FLAG_PROJECT).item(0).getFirstChild().getNodeValue();
        if (projects != null) {
            return getStringsFromComma(projects);
        }
        return null;
    }

    private String[] getArgs(Element compriseElement) {
        String args = compriseElement.getElementsByTagName(FLAG_ARGS).item(0).getFirstChild() == null ? null
                : compriseElement.getElementsByTagName(FLAG_ARGS).item(0).getFirstChild()
                        .getNodeValue();
        if (args != null) {
            return getStringsFromComma(args);
        }
        return null;
    }

    private String[] getStringsFromComma(String strings) throws NullPointerException {
        String[] sq = strings.split(",");
        for (int i = 0; i < sq.length; i++) {
            sq[i] = sq[i].trim();
        }
        return sq;
    }

    public void debug() {
        Log.d(XmlDomParser.TAG, TAG + ".debug: Item Node Data:===================>");
        Log.d(XmlDomParser.TAG, TAG + ".mPackageName = [" + mPackageName + "]\nmDisplayName = ["
                + mDisplayName + "]\nmCondition = [" + mCondition + "]\nmUri = [" + mUri
                + "]\nmGroupby = [" + mGroupby + "]\nmHaving = [" + mHaving + "]\nmOrderby = ["
                + mOrderby + "]\nmArgsValue = [" + mArgsValue + "]");
        printlnStrings(mArgskey, "mArgskey");
        printlnStrings(mProjects, "mProjects");
        Log.d(XmlDomParser.TAG, TAG + ".debug: <==================================");
    }

    private void printlnStrings(String[] strings, String tag) {
        if (strings != null) {
            for (int i = 0; i < strings.length; i++) {
                Log.d(XmlDomParser.TAG, TAG + ".printlnStrings: " + tag + "[" + i + "] = ["
                        + strings[i] + "]");
            }
        } else {
            Log.d(XmlDomParser.TAG, TAG + ".printlnStrings: " + tag + " = [null]");
        }
    }

}
