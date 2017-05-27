package com.android.launcher3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// SPRD: bug395717 2015-01-19 Feature to support device not with addon.
public class SprdAddonUtils {

    public static Object instance(int addOnNameResId, Class stubClass) {
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
