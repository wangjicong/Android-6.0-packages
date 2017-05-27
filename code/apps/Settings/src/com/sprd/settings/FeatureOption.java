
package com.sprd.settings;

import android.os.SystemProperties;

public class FeatureOption {
    public static final boolean SPRD_DEFAULT_DISK_WRITE = true;
    public static final boolean SPRD_ROM_SYSTEM_SIZE = true;



//    public static final boolean SPRD_DEFAULT_DISK_WRITE = getValue("ro.sprd_default_disk_write");

    // Important!!!  the SystemProperties key's length must less than 31 , or will have JE
    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
