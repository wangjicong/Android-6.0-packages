package com.sprd.fileexplorer.util;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
public class IntentUtil {

    /**
     * get intent by filetype
     * @param context    
     * @param fileType fileType

     * @param filePath
     * @return no app can open it, return null
     */
    public static Intent getIntentByFileType(Context context, int fileType, File file) {
        if (fileType == FileType.FILE_TYPE_UNKNOE || file == null) {
            return null;
        }
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        Intent intent = null;
        switch (fileType) {
        case FileType.FILE_TYPE_IMAGE:
            intent = getIntent(file, "image/*");
            intent.putExtra("read-only", false);
            break;
        case FileType.FILE_TYPE_AUDIO:
        case FileType.FILE_TYPE_AUDIO_ACC:
        case FileType.FILE_TYPE_AUDIO_MP3:
        case FileType.FILE_TYPE_AUDIO_OGG:
        // SPRD: Add for bug510980
        case FileType.FILE_TYPE_AUDIO_OGA:
        case FileType.FILE_TYPE_AUDIO_WAV:
        case FileType.FILE_TYPE_AUDIO_WMA:
        case FileType.FILE_TYPE_AUDIO_AMR:
        case FileType.FILE_TYPE_AUDIO_AIFF:
        // SPRD: Modify for bug505013.
        //case FileType.FILE_TYPE_AUDIO_APE:
        case FileType.FILE_TYPE_AUDIO_AV:
        case FileType.FILE_TYPE_AUDIO_CD:
        case FileType.FILE_TYPE_AUDIO_MIDI:
        case FileType.FILE_TYPE_AUDIO_VQF:
        case FileType.FILE_TYPE_AUDIO_AAC:
        case FileType.FILE_TYPE_AUDIO_MID:
        case FileType.FILE_TYPE_AUDIO_M4A:
        case FileType.FILE_TYPE_AUDIO_IMY:
        /* SPRD 437313 @{ */
        case FileType.FILE_TYPE_AUDIO_MP4:
        case FileType.FILE_TYPE_AUDIO_3GPP:
        /* @} */
        // SPRD 456778
        case FileType.FILE_TYPE_AUDIO_3GP:
        // SPRD: Add for bug507035.
        case FileType.FILE_TYPE_AUDIO_3G2:
         // SPRD 498509
        case FileType.FILE_TYPE_AUDIO_OPUS:
        // SPRD 461106
        case FileType.FILE_TYPE_AUDIO_FLAC:
        // SPRD 463007
        case FileType.FILE_TYPE_AUDIO_AWB:
        // SPRD: Add for bug510953.
        case FileType.FILE_TYPE_AUDIO_MKA:
        /* SPRD: Add for bug511015. @{ */
        case FileType.FILE_TYPE_AUDIO_M4B:
        case FileType.FILE_TYPE_AUDIO_M4R:
        /* @} */
            intent = getIntent(file, "audio/*");
            break;
        case FileType.FILE_TYPE_VIDEO:
        case FileType.FILE_TYPE_VIDEO_3GP:
        // SPRD: Add for bug507035.
        case FileType.FILE_TYPE_VIDEO_3G2:
        case FileType.FILE_TYPE_VIDEO_AVI:
        case FileType.FILE_TYPE_VIDEO_FLV:
        case FileType.FILE_TYPE_VIDEO_MP4:
        case FileType.FILE_TYPE_VIDEO_MKV:
        case FileType.FILE_TYPE_VIDEO_RMVB:
        case FileType.FILE_TYPE_VIDEO_MPEG:
        case FileType.FILE_TYPE_VIDEO_ASF:
        case FileType.FILE_TYPE_VIDEO_DIVX:
        case FileType.FILE_TYPE_VIDEO_MPE:
        case FileType.FILE_TYPE_VIDEO_MPG:
        // SPRD: Modify for bug505136.
        case FileType.FILE_TYPE_VIDEO_TS:
        // SPRD: Modify for bug498813.
        //case FileType.FILE_TYPE_VIDEO_RM:
        case FileType.FILE_TYPE_VIDEO_VOB:
        case FileType.FILE_TYPE_VIDEO_WMV:
        // SPRD 459132
        case FileType.FILE_TYPE_VIDEO_M4V:
        case FileType.FILE_TYPE_VIDEO_F4V:
        // SPRD 455914
        case FileType.FILE_TYPE_VIDEO_WEBM:
            intent = getIntent(file, "video/*");
            break;
        case FileType.FILE_TYPE_PACKAGE:
            intent = getIntent(file, "application/vnd.android.package-archive");
            break;
        case FileType.FILE_TYPE_DOC:
            intent = getDocIntent(FileType.getFileType(context).getDocFileType(file), file);
            break;
        case FileType.FILE_TYPE_RECORD:
            break;
        case FileType.FILE_TYPE_VCARD:
            intent = getIntent(file, "text/x-vcard");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            break;
        case FileType.FILE_TYPE_VCALENDER:
            intent = getIntent(file, "text/x-vcalendar");
            break;
        case FileType.FILE_TYPE_WEBTEXT:
            intent = getIntent(file, "text/html");
            break;
        default:
            intent = getDocIntent(fileType, file);
        }
        if(!isIntentAvailable(context, intent)) {
            intent = null;
        }
        return intent;
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {
        if(intent == null) {
            return false;
        }
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return !list.isEmpty();
    }
    
    private static Intent getDocIntent(int fileType, File file) {
        Intent intent = null;
        switch (fileType) {
        case FileType.FILE_TYPE_TEXT:
            intent = getIntent(file, "text/plain");
            break;
        case FileType.FILE_TYPE_WORD:
            intent = getIntent(file, "application/msword");
            break;
        case FileType.FILE_TYPE_EXCEL:
            intent = getIntent(file, "application/vnd.ms-excel");
            break;
        case FileType.FILE_TYPE_PPT:
            intent = getIntent(file, "application/vnd.ms-powerpoint");
            break;
        case FileType.FILE_TYPE_PDF:
            intent = getIntent(file, "application/pdf");
            break;
        }
        return intent;
    }

    public static Intent getIntent(File file, String type) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), type);
        return intent;
    }

}
