/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.messaging.ui;

import android.os.Environment;
import android.database.Cursor;
import android.provider.Telephony.Mms.Part;
import android.database.sqlite.SQLiteException;
import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import com.android.messaging.util.LogUtil;
import android.content.Intent;
import com.android.messaging.util.TextUtil;
import com.android.messaging.util.UiUtils;
import com.android.messaging.R;
import com.android.messaging.Factory;
import com.android.messaging.util.ContentType;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.content.ContentUris;
import android.provider.MediaStore;
import com.android.messaging.datamodel.MediaScratchFileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import android.util.Log;
import java.lang.NullPointerException;

public class VdataUtils {

    public static final int NO_SDCARD = -1;
    public static final int SDCARD_READ_ONLY = 0;
    public static final int SDCARD_BUSY = 1;
    public static final int SDCARD_WRITE = 2;
    public static final int SDCARD_NO_SIZE = 3;
    public static final int SDCARD_AVAILABLE = 4;
    private static String TAG = "VdataUtils";

    public static void saveOtherFile(Context context, Uri uri) {
        String dir = getCanSavePath() + "/" + Environment.DIRECTORY_DOWNLOADS
                + "/";
        String path;
        Log.d(TAG, "uri:" + uri);
        if (isMessagingUri(uri)) {
            path = MediaScratchFileProvider.getFileFromUri(uri)
                    .getAbsolutePath();
            Log.d(TAG, "getPath isMessagingMediaScrathUri path is " + path);
        }else{
            path = getUriToPath(context, uri);
        }
        boolean succ = copyToSDCard(context, uri, path, null);
        String msg;
        if (succ) {
            msg = context.getString(R.string.copy_to_sdcard_success, dir);
        } else {
            msg = context.getString(R.string.copy_to_sdcard_fail);
        }
        UiUtils.showToastAtBottom(msg);

    }

    private static File getUniqueDestination(File dir, String src) {
        File file = new File(dir + "/" + src);
        String extension = "";
        String fileName = src;
        if (src.lastIndexOf(".") > 0) {
            extension = src.substring(src.lastIndexOf(".") + 1);
            fileName = src.substring(0, src.lastIndexOf("."));
        }

        for (int i = 2; file.exists(); i++) {
            file = new File(dir + "/" + fileName + "_" + i + "." + extension);
        }
        return file;
    }

    public static long getSdcardAvailableSize() {
        StatFs statfs = null;
        File file = getCanSavePath();
        if (file != null) {
            statfs = new StatFs(file.getPath());
        } else {
            return 0;
        }
        return (long) statfs.getBlockSize()
                * (long) statfs.getAvailableBlocks();
    }

    public static int getSdcardStatus() {
        String in_state = Environment.getInternalStoragePathState();
        String ex_state = Environment.getExternalStoragePathState();
        if (!ex_state.equals(Environment.MEDIA_MOUNTED)
                && !in_state.equals(Environment.MEDIA_MOUNTED)) {
            return NO_SDCARD;
        }
        if (ex_state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)
                && in_state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            return SDCARD_READ_ONLY;
        }
        if (ex_state.equals(Environment.MEDIA_SHARED)
                && in_state.equals(Environment.MEDIA_SHARED)) {
            return SDCARD_BUSY;
        }
        return SDCARD_WRITE;
    }

    public static boolean checkSdcardIsAvaliable(int size) {
        int sdcardstatus = getSdcardStatus();
        int status;
        if (sdcardstatus == SDCARD_WRITE) {
            if (size < getSdcardAvailableSize()) {
                status = SDCARD_AVAILABLE;
            } else {
                status = SDCARD_NO_SIZE;
            }
        } else {
            status = sdcardstatus;
        }
        if (status == SDCARD_AVAILABLE) {
            return true;
        }
        return false;
    }

    private static boolean copyToSDCard(final Context context, Uri uri,
            String path, String dir) {
        boolean result = true;
        if (checkSdcardIsAvaliable(getAttachmentSize(context, uri))) {
            result = copyMedia(context, uri, path, dir);
        } else {
            result = false;
        }
        if (result) {
            File file = getCanSavePath();
            if (file == null) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public static File getCanSavePath() {
        String in_state = Environment.getInternalStoragePathState();
        String ex_state = Environment.getExternalStoragePathState();
        File dir = null;
        if (Environment.MEDIA_MOUNTED.equals(ex_state)) {
            dir = Environment.getExternalStoragePath();
        } else if (Environment.MEDIA_MOUNTED.equals(in_state)) {
            dir = Environment.getInternalStoragePath();
        }
        return dir;
    }

    public static String getUriToPath(final Context context, final Uri uri) {
        String path = null;
        Cursor cursor = null;
        final String[] part_projection = new String[] { Part._ID, Part.CHARSET,
                Part.CONTENT_DISPOSITION, Part.CONTENT_ID,
                Part.CONTENT_LOCATION, Part.CONTENT_TYPE, Part.FILENAME,
                Part.NAME, Part._DATA, Part.TEXT };
        try {
            cursor = context.getContentResolver().query(uri, part_projection,
                    null, null, null);
            if (null == cursor || 0 == cursor.getCount()
                    || !cursor.moveToFirst()) {
                throw new IllegalArgumentException(
                        "Given Uri could not be found" + " in media store");
            }
            final int pathIndex = cursor.getColumnIndexOrThrow(Part._DATA);
            path = cursor.getString(pathIndex);
        } catch (final SQLiteException e) {
            throw new IllegalArgumentException(
                    "Given Uri is not formatted in a way "
                            + "so that it can be found in media store.");
        } finally {
            if (null != cursor) {
                cursor.close();
            }

        }
        return path;
    }

    public static boolean copyMedia(Context context, Uri uri, String path,
            String dir) {
        File sdcard = getCanSavePath();
        if (sdcard == null) {
            return false;
        }
        File fdir = sdcard;
        if (dir == null) {
            fdir = new File(sdcard, Environment.DIRECTORY_DOWNLOADS);
        } else {
            fdir = new File(sdcard, dir);
        }
        if (!fdir.isDirectory() || !fdir.exists()) {
            fdir.mkdir();
        }
        String mSrc = path;

        if (mSrc.indexOf('/') != -1) {
            mSrc = mSrc.substring(mSrc.lastIndexOf('/') + 1);
        }
        if (mSrc.indexOf(':') != -1) {
            mSrc = mSrc.substring(mSrc.lastIndexOf(':') + 1);
        }
        /*
         * if ((mSrc.lastIndexOf('.')) <= 0) { String extension =
         * MimeTypeMap.getSingleton().getExtensionFromMimeType(
         * mediaModel.getContentType()); if (!TextUtils.isEmpty(extension)) { if
         * (mSrc.lastIndexOf('.') == 0) { mSrc = mSrc + extension; } else { mSrc
         * = mSrc + '.' + extension; } } }
         */
        int index = mSrc.lastIndexOf('.');
        if (index > 0) {
            mSrc = mSrc.substring(0, index).replaceAll(
                    "[\\p{Punct}\\p{Space}]+", "_")
                    + mSrc.substring(index);
        }
        File file = getUniqueDestination(fdir, mSrc);
        FileOutputStream fops;
        InputStream ips;
        try {
            ips = context.getContentResolver().openInputStream(uri);
            fops = new FileOutputStream(file);
            byte[] data = new byte[1024];
            try {
                int tempSize = 0;
                while ((tempSize = ips.read(data)) != -1) {
                    fops.write(data, 0, tempSize);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (ips != null) {
                    try {
                        ips.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fops != null) {
                    try {
                        fops.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // Notify other applications listening to scanner events
                // that a media file has been added to the sd card
                context.sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                                .fromFile(file)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static int getAttachmentSize(final Context context, final Uri uri) {
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            try {
                return is == null ? 0 : is.available();
            } catch (final IOException e) {
                LogUtil.e(LogUtil.BUGLE_TAG, "couldn't stream: " + uri, e);
            }
        } catch (final FileNotFoundException e) {
            LogUtil.e(LogUtil.BUGLE_TAG, "couldn't open: " + uri, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    LogUtil.e(LogUtil.BUGLE_TAG, "couldn't close: " + uri, e);
                }
            }
        }
        return 0;
    }

    /* Modify by SPRD for Bug:527552 Start */
    public static String getFileType(final Uri uri,String filetype){
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        final Context context = Factory.get().getApplicationContext();
        String extension =filetype;
        String path=null;
        if (isExternalStorageDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];
            Log.d(TAG, "getPath isExternalStorageDocument type = " + type
                    + "  docId = " + docId);
            if ("primary".equalsIgnoreCase(type)) {
                path = Environment.getInternalStoragePath() + "/";
            } else {
                path = Environment.getExternalStoragePath() + "/";
            }
            // TODO handle non-primary volumes
            if (split.length > 1) {
                path = path + split[1];
            }
        }else if (isDownloadsDocument(uri)) {
            final String id = DocumentsContract.getDocumentId(uri);
            final Uri contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    Long.valueOf(id));
            Log.d(TAG, " getPath isDownloadsDocument contentUri = "
                    + contentUri);
            path = getDataColumn(context, contentUri, null, null);
        }else if (isMediaDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];
            Log.d(TAG, "getPath isMediaDocument type = " + type);
            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }
            final String selection = "_id=?";
            final String[] selectionArgs = new String[] { split[1] };
            path = getDataColumn(context, contentUri, selection, selectionArgs);
        }else if ("content".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, "getPath isContentScheme ");
            path = getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, "getPath isFileSheme");
            path =  uri.getPath();
        }
        try {
            if (path.lastIndexOf(".") > 0) {
                extension = path.substring(path.lastIndexOf(".") + 1);
            }
        }catch(final NullPointerException e){
            Log.e(TAG, "Couldn't find file:"+e);
        }
        LogUtil.d(TAG," extension:"+extension+" path:"+path);
        return extension;
    }

    public static String getAudioType(String type){
        String audioMimeType = "";
        if(!TextUtils.isEmpty(type)) {
            type = type.toLowerCase();
            // Handle special type.
            if (type.equals("imelody")) {
                audioMimeType = ContentType.AUDIO_IMELODY;
            } else if (type.equals("mid")) {
                audioMimeType = ContentType.AUDIO_MID;
            } else if (type.equals("mp3")) {
                audioMimeType = ContentType.AUDIO_MP3;
            } else if (type.equals("mpeg3")) {
                audioMimeType = ContentType.AUDIO_MPEG3;
            } else if (type.equals("mpeg")) {
                audioMimeType = ContentType.AUDIO_MPEG;
            } else if (type.equals("mp4")) {
                audioMimeType = ContentType.AUDIO_MP4;
            } else if (type.equals("mp4-latm")) {
                audioMimeType = ContentType.AUDIO_MP4_LATM;
            } else if (type.equals("x-mid")) {
                audioMimeType = ContentType.AUDIO_X_MID;
            } else if (type.equals("x-midi")) {
                audioMimeType = ContentType.AUDIO_X_MIDI;
            } else if (type.equals("x-mp3")) {
                audioMimeType = ContentType.AUDIO_X_MP3;
            } else if (type.equals("x-mpeg3")) {
                audioMimeType = ContentType.AUDIO_X_MPEG3;
            } else if (type.equals("x-mpeg")) {
                audioMimeType = ContentType.AUDIO_X_MPEG;
            } else if (type.equals("x-mpg")) {
                audioMimeType = ContentType.AUDIO_X_MPG;
            } else if (type.equals("3gpp")) {
                // video or audio's file name can be ended with "3gpp".
                audioMimeType = ContentType.AUDIO_3GPP;
            } else if (type.equals("x-wav")) {
                audioMimeType = ContentType.AUDIO_X_WAV;
            }
            // Handle common type: support by libcore/luni/src/main/java/libcore/net/MimeUtils.java
            else {
                MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                audioMimeType = mimeTypeMap.getMimeTypeFromExtension(type);
            }
        }
        LogUtil.d(LogUtil.BUGLE_TAG,  "lxg getAudioType type=" + type + ", audioMimeType=" +  audioMimeType);
        if (TextUtils.isEmpty(audioMimeType)) {
            // Assume type is not null or empty.
            // default value.
            audioMimeType = ContentType.AUDIO_OGG;
        }
        return audioMimeType;
    }

    public static String getVideoType(String type){
        String videoMimeType = "";
        if (!TextUtils.isEmpty(type)) {
            type = type.toLowerCase();
            // Handle special type.
            if (type.equals("3gp")) {
                videoMimeType = ContentType.VIDEO_3GP;
            } else if (type.equals("3gpp")) {
                // video or audio's file name can be ended with "3gpp".
                videoMimeType = ContentType.VIDEO_3GPP;
            } else if (type.equals("h263")) {
                videoMimeType = ContentType.VIDEO_H263;
            } else if (type.equals("mpeg")) {
                videoMimeType = ContentType.VIDEO_MPEG;
            }
            // Handle common type: support by libcore/luni/src/main/java/libcore/net/MimeUtils.java
            else {
                MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                videoMimeType = mimeTypeMap.getMimeTypeFromExtension(type);
            }
        }
        LogUtil.d(LogUtil.BUGLE_TAG,  "lxg getVideoType type=" + type + ", videoMimeType=" +  videoMimeType);
        if (TextUtils.isEmpty(videoMimeType)) {
            // Assume type is not null or empty.
            // default value.
            videoMimeType = ContentType.VIDEO_MP4;
        }
        return videoMimeType;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }
    public static boolean isMessagingUri(Uri uri) {
        return uri.getAuthority().startsWith("com.android.messaging");
    }

    private static String getDataColumn(Context context, Uri uri,
            String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    /* Modify by SPRD for Bug:527552 end */

}
