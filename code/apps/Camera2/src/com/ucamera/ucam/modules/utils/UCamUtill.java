/**
 *
 */
package com.ucamera.ucam.modules.utils;

import com.android.camera.util.ApiHelper;
import android.content.pm.PackageManager;
import android.Manifest;
import android.app.Activity;

import android.content.Context;

/**
 * @author sprd
 *
 */
public class UCamUtill {

    private final static String TARGET_TS_UCAM_MAKEUP_BEAUTY_ENABL = "persist.sys.ucam.beauty";
    private final static String TARGET_TS_UCAM_MAKEUP_PUZZLE_ENABLE = "persist.sys.ucam.puzzle";
    private final static String TARGET_TS_UCAM_MAKEUP_PEDIT_ENABLE = "persist.sys.ucam.edit";
    private final static String TARGET_TS_TIMESTAMP_ENABL = "persist.sys.cam.timestamp";
    private final static String TARGET_TS_GIF_NENABL = "persist.sys.cam.gif";

    private static boolean isUcamBeautyEnable;
    private static boolean isUcamPuzzleEnable;
    private static boolean isUcamEditEnable;
    private static boolean isTimeStampEnable;
    private static boolean isGifEnable;


    public static void initialize(Context context) {
        isUcamBeautyEnable = android.os.SystemProperties.getBoolean(
                TARGET_TS_UCAM_MAKEUP_BEAUTY_ENABL, true);
        isUcamPuzzleEnable = android.os.SystemProperties.getBoolean(
                TARGET_TS_UCAM_MAKEUP_PUZZLE_ENABLE, true);
        isUcamEditEnable = android.os.SystemProperties.getBoolean(
                TARGET_TS_UCAM_MAKEUP_PEDIT_ENABLE, true);
        isTimeStampEnable = android.os.SystemProperties.getBoolean(
                TARGET_TS_TIMESTAMP_ENABL, true);
        isGifEnable = android.os.SystemProperties.getBoolean(
                TARGET_TS_GIF_NENABL, true);
    }

    public static boolean isUcamBeautyEnable() {
        return isUcamBeautyEnable;
    }

    public static boolean isUcamPuzzleEnable() {
        return isUcamPuzzleEnable;
    }

    public static boolean isUcamEditEnable() {
        return isUcamEditEnable;
    }

    /* SPRD:fix bug514045 Some Activity about UCamera lacks method of checkpermission@{ */
    public static boolean checkPermissions(Activity mActivity) {
        if (!ApiHelper.isMOrHigher()) {
            return true;
        }

        if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                mActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }
    /* }@ */

    public static boolean isTimeStampEnable() {
        return isTimeStampEnable;
    }
    public static boolean isGifEnable() {
        return isGifEnable;
    }
}
