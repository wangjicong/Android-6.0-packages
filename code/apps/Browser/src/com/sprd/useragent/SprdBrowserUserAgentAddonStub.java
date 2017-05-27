/**
 * Add for Browser ua addon
 */

package com.sprd.useragent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;
import com.android.browser.R;;

public class SprdBrowserUserAgentAddonStub {

    private static final String TAG = "SprdBrowserUserAgentAddonStub";
    static SprdBrowserUserAgentAddonStub sInstance;

    public SprdBrowserUserAgentAddonStub(){
    }

    public static SprdBrowserUserAgentAddonStub getInstance(){
        if (sInstance == null) {
            sInstance = (SprdBrowserUserAgentAddonStub) instance(R.string.feature_browser_useragent, SprdBrowserUserAgentAddonStub.class);
            if (sInstance == null ){
                sInstance = new SprdBrowserUserAgentAddonStub();
            }
        }
        Log.d(TAG, "getInstance. sInstance = "+sInstance);
        return sInstance;
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

    public String getUserAgentString() {
        return null;
    }
}
