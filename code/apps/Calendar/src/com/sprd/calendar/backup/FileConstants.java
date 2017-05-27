/* SPRD: for bug473564, add backup info @{ */
package com.sprd.calendar.backup;

import java.io.File;

import android.os.Environment;
import android.text.TextUtils;
import android.os.SystemProperties;

public class FileConstants {

    public static final String SUFFIX_XML = ".xml";
    public static final String SUFFIX_PDU = ".pdu";
    public static final String SUFFIX_ZIP = ".zip";
    public static final String SUFFIX_VCF = ".vcf";

    public static final boolean IS_NAND = "1"
            .equals(SystemProperties.get("ro.device.support.nand"));
    public static final String EXTERNAL_SDCARD_FILE = Environment.getExternalStoragePath().getPath();
    public static final String EXTERNAL_BACKUP_DIR = EXTERNAL_SDCARD_FILE + File.separator
            + "backup";
    public static final String EXTERNAL_BACKUP_APP = EXTERNAL_SDCARD_FILE + File.separator
            + "backup" + File.separator + "App";
    public static final String EXTERNAL_BACKUP_FILE = EXTERNAL_SDCARD_FILE + File.separator
            + "backup" + File.separator + "Data";
    public static final String INTERNAL_SDCARD_FILE = Environment.getInternalStoragePath().getPath();
    public static final String INTERNAL_BACKUP_DIR = INTERNAL_SDCARD_FILE + File.separator
            + "backup";
    public static final String INTERNAL_BACKUP_APP = INTERNAL_SDCARD_FILE + File.separator
            + "backup" + File.separator + "App";
    public static final String INTERNAL_BACKUP_FILE = INTERNAL_SDCARD_FILE + File.separator
            + "backup" + File.separator + "Data";
    public static final String BACKUP_V1_DIR = EXTERNAL_SDCARD_FILE + File.separator + ".backup";
    public static final String BACKUP_V1_APP = EXTERNAL_SDCARD_FILE + File.separator + ".backup"
            + File.separator + "App";
    public static final String BACKUP_V1_FILE = EXTERNAL_SDCARD_FILE + File.separator + ".backup"
            + File.separator + "Data";

    public static boolean USE_EXTERNAL = true;
    public static boolean IS_CANCEL_ENABLE = true;
    public static final int FLAG_SMS = 0;
    public static final int FLAG_MMS = 1;
    public static boolean SMS_CHECKED = false;
    public static boolean MMS_CHECKED = false;
    public static boolean NONE_SMS = false;
    public static boolean NONE_MMS = false;
    public static boolean SHOW_NOTE = true;


    public static int LENGTH = 43;

    public static String BACKUP_TMP_PATH(String time) {
        String append = TextUtils.isEmpty(time) ? "" : File.separator + time;
        if (IS_NAND) {
            LENGTH = (EXTERNAL_BACKUP_FILE + append).length();
            return EXTERNAL_BACKUP_FILE + append;
        }
        if (USE_EXTERNAL) {
            LENGTH = (EXTERNAL_BACKUP_FILE + append).length();
            return EXTERNAL_BACKUP_FILE + append;
        } else {
            LENGTH = (INTERNAL_BACKUP_FILE + append).length();
            return INTERNAL_BACKUP_FILE + append;
        }
    }

    public static String BACKUP_APP_TMP_PATH() {
        if (IS_NAND) {
            return EXTERNAL_BACKUP_APP;
        }
        if (USE_EXTERNAL) {
            return EXTERNAL_BACKUP_APP;
        } else {
            return INTERNAL_BACKUP_APP;
        }

    }

    public static final String BACKUP_TMP_PATH_V1(String time) {
        String append = TextUtils.isEmpty(time) ? "" : File.separator + time;
        return BACKUP_V1_DIR + append;
    }
}
/* @} */