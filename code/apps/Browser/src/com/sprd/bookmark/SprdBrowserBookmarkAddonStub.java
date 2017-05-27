/**
 *Add for Bookmark addon in plugin
 *@
 */
package com.sprd.bookmark;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.net.Uri;
import android.util.Log;
import android.app.AddonManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import com.android.browser.R;;

public class SprdBrowserBookmarkAddonStub {

    private static final String TAG = "SprdBookmarkAddonStub";
    static SprdBrowserBookmarkAddonStub sInstance;

    public SprdBrowserBookmarkAddonStub(){
    }

    public static SprdBrowserBookmarkAddonStub getInstance(){
        if (sInstance == null) {
            sInstance = (SprdBrowserBookmarkAddonStub) instance(R.string.feature_browser_bookmark, SprdBrowserBookmarkAddonStub.class);
            if (sInstance == null ){
                sInstance = new SprdBrowserBookmarkAddonStub();
            }
        }
        Log.d(TAG, "getInstance. sInstance = "+sInstance);
        return sInstance;
    }

    public CharSequence[] getBookmarks() {
        return null;
    }

    public TypedArray getPreloads(){
        return null;
    }

    public byte[] readRaw(int id) throws IOException {
        return null;
    }
    private static Object instance(int addOnNameResId, Class stubClass) {
        Object obj = null;
        try {
            Class clazz = Class.forName("android.app.AddonManager");

            Method defMethod = clazz.getMethod("getDefault");
            Object addonManager = defMethod.invoke(null);

            Method getMethod = clazz.getMethod("getAddon", new Class[] {int.class, Class.class});
            obj = getMethod.invoke(addonManager, addOnNameResId, stubClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
