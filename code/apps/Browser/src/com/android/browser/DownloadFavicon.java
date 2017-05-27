/**
 * Add for navigation tab
 *@{
 */

package com.android.browser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ParseException;
import android.net.WebAddress;
import android.os.AsyncTask;
import android.util.Log;
import android.database.sqlite.SQLiteException;

public class DownloadFavicon extends AsyncTask<String, Void, Void>{

    private static final String LOGTAG = "SpeedDial";
    private static boolean sIsNetworkUp;
    private Cursor mCursor;
    private String mUserAgent;
    private String mFaviconUrl;
    private ContentResolver mContentResolver;
    //private Context mContext;

    public DownloadFavicon(String userAgent, String url, Context context, ContentResolver content){
        mUserAgent = userAgent;
        mFaviconUrl = url;
        mContentResolver = content;
        //mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {
        Log.d(LOGTAG,"DownloadFavicon begin,mFaviconUrl,"+mFaviconUrl);
        int index = mFaviconUrl.indexOf("/favicon.ico");
        if(index == -1){
            return null;
        }
        String domain = mFaviconUrl.substring(0, index);
        try {
            mCursor = mContentResolver.query(SpeedDial.CONTENT_URI, new String[]{"_id"},
                    "url like ?", new String[]{domain + "%"}, null);
        } catch (SQLiteException e) {
            Log.e(LOGTAG, "DownloadFavicon:", e);
        }
        if (mCursor != null && mCursor.getCount() > 0) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(mFaviconUrl);
                connection = (HttpURLConnection) url.openConnection();
                if (mUserAgent != null) {
                    connection.addRequestProperty("User-Agent", mUserAgent);
                }

                if (connection.getResponseCode() == 200) {
                    InputStream content = connection.getInputStream();
                    Bitmap icon = null;
                    try {
                        icon = BitmapFactory.decodeStream(
                                content, null, null);
                    } finally {
                        try {
                            content.close();
                        } catch (IOException ignored) {
                        }
                    }

                    storeIcon(icon);
                }
            } catch (IOException ignored) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }else{
            Log.d(LOGTAG,"DownloadFavicon begin,db count null,");
        }
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        return null;
    }
    @Override
    protected void onCancelled() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    private void storeIcon(Bitmap icon) {

        if (icon == null || mCursor == null || isCancelled()) {
            Log.d(LOGTAG,"null");
            return;
        }

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, os);
        ContentValues values = new ContentValues();
        values.put(SpeedDial.FAVICON, os.toByteArray());

        if (mCursor.moveToFirst()) {
            do {
                Log.d(LOGTAG,"update:"+mCursor.getInt(0));
                try {
                    int num = mContentResolver.update(SpeedDial.CONTENT_URI,
                            values, "_id = " + mCursor.getInt(0), null);
                    Log.d(LOGTAG,"update num:"+num);
                } catch (SQLiteException e) {
                    Log.e(LOGTAG, "storeIcon:", e);
                }
            } while (mCursor.moveToNext());
        }
    }

    public static String getFaviconUrl(String url) throws ParseException{
        String favicon = null;
        try {
            String port = "";
            WebAddress addr = new WebAddress(url);
            if ((addr.getPort() != 443 && addr.getScheme().equals("https"))
                    || (addr.getPort() != 80 && addr.getScheme().equals("http"))) {
                port = ":" + Integer.toString(addr.getPort());
            }
            String authInfo = "";
            if (addr.getAuthInfo().length() > 0) {
                authInfo = addr.getAuthInfo() + "@";
            }
            favicon = addr.getScheme() + "://" + authInfo + addr.getHost() + port
                    + "/favicon.ico";
            Log.d(LOGTAG, "SaveSpeedDialItemRunnable faviconUrl:" + favicon);
        } catch (ParseException e) {
            Log.d(LOGTAG, "parser favicon url failed:" + url + ", exception:" + e);
            return null;
        }
        return favicon;
    }

    public static void donwnloadFavicon(String userAgent, String url, Context context, ContentResolver resolver){
        try {
            if(!sIsNetworkUp){
                Log.d(LOGTAG,"network is down");
                return;
            }
            String faviconUrl  = getFaviconUrl(url);
            new DownloadFavicon(null, faviconUrl, context, resolver).execute();
        } catch (ParseException e) {
            Log.d(LOGTAG, "donwnloadFavicon url failed:" + url + ", exception:" + e);
        }
    }

    public static void setNetworkUp(boolean up){
        sIsNetworkUp = up;
    }
}
