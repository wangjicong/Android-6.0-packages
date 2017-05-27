package com.sprd.messaging.util;

import android.util.Log;
import com.android.messaging.util.LogUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by lxg on 15-12-18.
 */
public class Utils {
    /* Add by SPRD for Bug:504724 2015.12.18 Start */
    /**
     *  Get supper method via reflection, mainly for private method.
     * @param methodName The method name who will be invoked.
     * @param depth Search depth.
     * @param supperClass Supper class.
     * @param argsType The type of args in the method.
     * @return The method if find in super class, null otherwise.
     */
    public static Method getSuperMethod(String methodName, int depth, Class<?> supperClass, Class<?>... argsType) {
        if (methodName == null || methodName.length() == 0
                || supperClass == null || depth <= 0) {
            Log.d(LogUtil.BUGLE_TAG, "Not find out mehtod '" + methodName + "' in supper class");
            return null;
        }
        Method m;
        try {
            m = supperClass.getDeclaredMethod(methodName, argsType);
        } catch (NoSuchMethodException e) {
            try {
                m = supperClass.getMethod(methodName, argsType);
            } catch (NoSuchMethodException e1) {
                return getSuperMethod(methodName, --depth, supperClass.getSuperclass(), argsType);
            }
        }
        return m;
    }

    /**
     *  Invoke method vid reflection, same as caller.m(args1, args2...)
     * @param caller The method caller.
     * @param method The method will be called.
     * @param argsValue Parameters need be sent to the method.
     */
    public static void invoke(final Object caller, Method method, Object... argsValue) {
        boolean invokeSucc = false;
        if (method != null) {
            method.setAccessible(true);
            try {
                method.invoke(caller, argsValue);
                invokeSucc = true;
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        if (!invokeSucc) {
            Log.d(LogUtil.BUGLE_TAG, "invoke method " + method.getName() + "fail.");
        }
    }
    /* Add by SPRD for Bug:504724 2015.12.18 End */

}
