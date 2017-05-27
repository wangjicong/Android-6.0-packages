
package com.android.providers.telephony.ext.mmsbackup;

import java.io.File;
import android.os.Environment;

public class StorageUtil {
    private static final String TAG = "StorageUitl";
    static boolean MMC_SUPPORT = true;
    private static boolean mIsNAND = false;
    private static final File EXTERNAL_STORAGE_DIRECTORY = getDirectory(
            getMainStoragePathKey(), Environment.getExternalStorageDirectory()
                    .getPath());

    private static final File SECONDRARY_STORAGE_DIRECTORY = getDirectory(
            getInternalStoragePathKey(), "/mnt/internal/");

    public static final String SDCARD_TYPE = "isSdCard";
    public static final String FILE_LIST = "fileList";

    private static String getMainStoragePathKey() {
        // FIXME: Continue highlight at this one on 12b_pxx branch, there is
        // no SECONDARY_STORAGE_TYPE

        try {
            // add a protection to fix if no SECONDARY_STORAGE_TYPE
            if ((null == System.getenv("SECOND_STORAGE_TYPE") || ""
                    .equals(System.getenv("SECOND_STORAGE_TYPE").trim()))
                    && MMC_SUPPORT) {
                BackupLog.log("SlogUI", "No SECOND_STORAGE_TYPE and support emmc");
                return "SECONDARY_STORAGE";
            }
            switch (Integer.parseInt(System.getenv("SECOND_STORAGE_TYPE"))) {
                case 0:
                    mIsNAND = true;
                    return "EXTERNAL_STORAGE";
                case 1:
                    return "EXTERNAL_STORAGE";
                case 2:
                    return "SECONDARY_STORAGE";
                default:
                    BackupLog.log(TAG, "Please check \"SECOND_STORAGE_TYPE\" "
                            + "\'S value after parse to int in System.getenv for framework");
                    if (MMC_SUPPORT) {
                        return "SECONDARY_SOTRAGE";
                    }
                    return "EXTERNAL_STORAGE";
            }
        } catch (Exception parseError) {
            BackupLog.log(TAG, "Parsing SECOND_STORAGE_TYPE crashed.\n" + parseError);
            if (MMC_SUPPORT) {
                return "SECONDARY_SOTRAGE";
            }
            return "EXTERNAL_STORAGE";
        }

    }

    private static String getInternalStoragePathKey() {
        String keyPath = getMainStoragePathKey();
        if (keyPath != null) {
            return keyPath.equals("EXTERNAL_STORAGE") ? "SECONDARY_STORAGE"
                    : "EXTERNAL_STORAGE";
        }
        return "SECONDARY_STORAGE";
    }

    public static String getExternalStorageState() {
        try {
            if (EXTERNAL_STORAGE_DIRECTORY.canRead()) {
                // File doubleCheck = new
                // File(EXTERNAL_STORAGE_DIRECTORY.getPath());
                BackupLog.log(TAG,
                        "Double Check storage is canread ="
                                + String.valueOf(android.os.Environment
                                        .getExternalStorageDirectory()
                                        .canRead()));
                return "mounted";
            }
        } catch (Exception rex) {
            return "removed";
        }
        BackupLog.log(TAG, "SDCard can't read");
        return "removed";
    }

    public static String getInternalStorageState() {
        try {
            if (SECONDRARY_STORAGE_DIRECTORY.canRead()) {
                return "mounted";
            }
        } catch (Exception rex) {
            return "removed";
        }
        return "removed";
    }

    private static File getDirectory(String variableName, String defaultPath) {
        String path = System.getenv(variableName);
        return path == null ? new File(defaultPath) : new File(path);
    }

    public static File getExternalStorage() {
        return EXTERNAL_STORAGE_DIRECTORY;
    }

    public static File getInternalStorage() {
        return SECONDRARY_STORAGE_DIRECTORY;
    }

    public static boolean isNAND() {
        return mIsNAND;
    }
}
