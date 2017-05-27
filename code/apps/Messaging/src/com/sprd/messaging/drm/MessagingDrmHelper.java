/*
 * Copyright (C) 2011 The Android Open Source Project
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
 *
 */

package com.sprd.messaging.drm;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Bitmap.Config;
import android.database.Cursor;
import android.drm.DrmEvent;
import android.drm.DrmInfo;
import android.drm.DrmInfoRequest;
import android.drm.DrmInfoStatus;
import android.drm.DrmManagerClient;
import android.drm.DrmRights;
import android.drm.DrmStore;
import android.drm.DrmManagerClient.OnEventListener;
import android.net.Uri;
import android.util.Log;
import android.drm.DrmStore;
import android.drm.DecryptHandle;
import android.drm.DrmStore.RightsStatus;
import android.net.Uri;

import java.io.*;
//import java.io.IOException;
//import java.io.InputStream;

import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.FileProvider;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DatabaseHelper.PartColumns;
import com.android.messaging.util.UriUtil;

public class MessagingDrmHelper {
    public static final String MIMETYPE_DRM_MESSAGE = "application/vnd.oma.drm.message";

    /** The extensions of special DRM files */
    public static final String EXTENSION_DRM_MESSAGE = ".dm";

    public static final String EXTENSION_INTERNAL_FWDL = ".fl";

    public static final String DRM_CONTENT_TYPE = "application/vnd.oma.drm.content";
    public static final String DRM_RIGHT_TYPE = "application/vnd.oma.drm.rights+xml";
    public static final String DRM_TRIGGER_TYPE = "application/vnd.oma.drm.message";
    private static String TAG = "MessagingDrmHelper";
    private static DrmManagerClient mClient = new DrmManagerClient(null);

    /**
     * Checks if the Media Type is a DRM Media Type
     * 
     * @param drmManagerClient
     *            A DrmManagerClient
     * @param mimetype
     *            Media Type to check
     * @return True if the Media Type is DRM else false
     */
    public static boolean isDrmMimeType(Context context, String path,
            String mimetype, boolean isDrmEnable) {
        if (!isDrmEnable) {
            return false;
        }
        boolean result = false;
        try {
            if (mimetype != null) {
                String tempPath = null;
                result = mClient.canHandle(tempPath, mimetype);
            } else {
                result = mClient.canHandle(path, mimetype);
            }
            if (result&&(path!=null&&(mClient.getMetadata(path) != null))){
                 result = true;
            }else{
                 result = false;
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG,
                    "DrmManagerClient instance could not be created, context is Illegal.");
        } catch (IllegalStateException e) {
            Log.w(TAG, "DrmManagerClient didn't initialize properly.");
        }
        return result;
    }

    /**
     * Checks if the Media Type needs to be DRM converted
     * 
     * @param mimetype
     *            Media type of the content
     * @return True if convert is needed else false
     */
    public static boolean isDrmConvertNeeded(String mimetype) {
        return MIMETYPE_DRM_MESSAGE.equals(mimetype);
    }

    public static DrmManagerClient getDrmManagerClient(){
        return mClient;
    }
    /**
     * Modifies the file extension for a DRM Forward Lock file NOTE: This
     * function shouldn't be called if the file shouldn't be DRM converted
     */
    public static String modifyDrmFwLockFileExtension(String filename) {
        if (filename != null) {
            int extensionIndex;
            extensionIndex = filename.lastIndexOf(".");
            if (extensionIndex != -1) {
                filename = filename.substring(0, extensionIndex);
            }
            filename = filename.concat(EXTENSION_INTERNAL_FWDL);
        }
        return filename;
    }

    /**
     * Gets the original mime type of DRM protected content.
     * 
     * @param context
     *            The context
     * @param path
     *            Path to the file
     * @param containingMime
     *            The current mime type of of the file i.e. the containing mime
     *            type
     * @return The original mime type of the file if DRM protected else the
     *         currentMime
     */
    public static String getOriginalMimeType(Context context, String path,
            String containingMimeType, boolean isDrmEnable) {
        String result = containingMimeType;
        Log.d(TAG, "getDrmOrigMimeType " + path + " containingMimeType "
                + containingMimeType);
        try {
            if (isDrmEnable && mClient.canHandle(path, null)) {
                if (path != null && !path.endsWith(".dcf")) {
                    Log.i(TAG,
                            "getOriginalMimeType  path is not end with dcf path   "
                                    + path);
                    return containingMimeType;
                }
                result = mClient.getOriginalMimeType(path);
                Log.i(TAG, "getOriginalMimeType -- type  " + result
                        + "   path   " + path);
            } else {
                Log.i(TAG, "getOriginalMimeType  Client can not handle  "
                        + path);
            }
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "getOriginalMimeType " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "getOriginalMimeType " + ex.toString());
        }
        return result;
    }

    public static int getDrmObjectType(Context context, String path,
            String mimeType) {
        int result = 0;
        try {
            result = mClient.getDrmObjectType(path, mimeType);
            Log.i(TAG, "getDrmObjectType:result " + result + " path " + path
                    + " mimeType " + mimeType);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "getDrmObjectType " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "getDrmObjectType " + ex.toString());
        }
        return result;
    }

    public static int saveDrmObjectRights(Context context, String path,
            String mimeType) {
        int result = -1;
        try {
            DrmRights drmRights = new DrmRights(new File(path), mimeType);
            result = mClient.saveRights(drmRights, null, null);
            Log.i(TAG, "saveDrmObjectRights:result " + result + " path " + path
                    + " mimeType " + mimeType);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "saveDrmObjectRights " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "saveDrmObjectRights " + ex.toString());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public static String generateDestinationfile(String file) {
        String destinationfile = null;
        if (file != null) {
            int dotIndex = file.lastIndexOf(".");
            if (dotIndex > 0) {
                destinationfile = file.substring(0, dotIndex) + ".dcf";
            }
        }
        if (destinationfile == null) {
            destinationfile = file + ".dcf";
            Log.w(TAG, "processDrmInfo destinationfile is  " + destinationfile);
        }
        return destinationfile;
    }

    public static int processDrmInfo(final Context context, final String path,
            final String destPath, final String containingMimeType,
            final OnEventListener listener) {
        int result = -1;
        try {
            mClient.setOnEventListener(listener);
            String destinationfile = null;
            if (destPath == null) {
                destinationfile = generateDestinationfile(path);
            } else {
                destinationfile = destPath;
            }
            DrmInfoRequest reqest = new DrmInfoRequest(
                    DrmInfoRequest.TYPE_REGISTRATION_INFO, containingMimeType);
            reqest.put("file_in", path);
            reqest.put("file_out", destinationfile);
            DrmInfo info = mClient.acquireDrmInfo(reqest);
            result = mClient.processDrmInfo(info);
            Log.i(TAG, "processDrmInfo:result " + result + " destinationFile "
                    + destinationfile);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "processDrmInfo " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "processDrmInfo " + ex.toString());
        }
        return result;
        /*
         * mClient.setOnEventListener(new OnEventListener() {
         * 
         * @Override public void onEvent(DrmManagerClient client, DrmEvent
         * event) {
         * 
         * DrmInfoStatus status = (DrmInfoStatus) event
         * .getAttribute(DrmEvent.DRM_INFO_STATUS_OBJECT); if (status.statusCode
         * == status.STATUS_OK) { if (path != null && new File(path).exists()) {
         * boolean delete = new File(path).delete(); Log.i(TAG,
         * "processDrmInfo status.STATUS_OK delete file  " + path); } Log.i(TAG,
         * "processDrmInfo status_ok path " + path + "  mimeType  " + mimeType);
         * String desfile = null; if (path != null) { int index =
         * path.lastIndexOf("/"); if (index > 0) { desfile =
         * path.substring(index, path.length()) + ".dcf"; } }
         * 
         * Log.w(TAG, "processDrmInfo listener desfile is  " + desfile);
         * 
         * ContentValues values = new ContentValues();
         * 
         * Uri newUri = FileProvider .buildFileUriAfterDrmInfoProcessed(
         * mProcessDrmInfoUri, desfile); values.put(PartColumns.CONTENT_URI,
         * UriUtil.stringFromUri(newUri));
         * BugleDatabaseOperations.updatePartRowIfExists(DataModel
         * .get().getDatabase(), partId, values); } else { Log.v(TAG,
         * "processDrmInfo  onEvent  status.statusCode == " +
         * status.statusCode); } } });
         */
    }

    public static int getRightsStatus(Context context, String path,
            String containingMimeType) {
        int result = 0;
        try {
            result = mClient.checkRightsStatus(path);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG,
                    "getRightsStatus IllegalArgumentException " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "getRightsStatus IllegalStateException " + ex.toString());
        }
        return result;
    }

    public static ContentValues getConstraints(Context context, String path,
            int action) {
        ContentValues cv = null;
        try {
            cv = mClient.getConstraints(path, action);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG,
                    "getRightsStatus IllegalArgumentException " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "getRightsStatus IllegalStateException " + ex.toString());
        }
        return cv;
    }

    public static ContentValues getMetadata(Context context, String path) {
        ContentValues cv = null;
        try {
            cv = mClient.getMetadata(path);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG,
                    "getRightsStatus IllegalArgumentException " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "getRightsStatus IllegalStateException " + ex.toString());
        }
        return cv;
    }

    public static boolean isDrmFLFile(Context context, String path,
            String mimeType) {
        String flType = null;
        try {
            ContentValues values = mClient.getMetadata(path);
            if (values != null) {
                flType = values.getAsString("extended_data");
                Log.w(TAG, "isDrmFile:" + flType);
            }
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "isDrmFLFile " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "isDrmFLFile " + ex.toString());
        }
        if (flType != null && flType.equals("fl")) {
            return true;
        }
        return false;
    }

    public static boolean isDrmSDFile(Context context, String path,
            String mimeType) {
        String sdType = null;
        try {
            ContentValues values = mClient.getMetadata(path);
            Log.d(TAG, "values:" + values);
            if (values != null) {
                sdType = values.getAsString("extended_data");
                Log.w(TAG, "isDrmSDFile:" + sdType);
            }
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "isDrmSDFile " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "isDrmSDFile " + ex.toString());
        }

        if (sdType != null && sdType.equals("sd")) {
            return true;
        }
        return false;
    }

    public static boolean isDrmCDFile(Context context, String path,
            String mimeType) {
        String cdType = null;
        try {
            ContentValues values = mClient.getMetadata(path);
            Log.d(TAG, "values:" + values);
            if (values != null) {
                cdType = values.getAsString("extended_data");
                Log.w(TAG, "isDrmCDFile:" + cdType);
            }
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "isDrmCDFile " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "isDrmCDFile " + ex.toString());
        }

        if (cdType != null && cdType.equals("cd")) {
            return true;
        }
        return false;
    }

    public static String renewDrmRightsDownload(Context context, String path,
            String mimeType) {
        String url = null;
        try {
            ContentValues values = mClient.getMetadata(path);
            Log.i(TAG, "getMetadata contentvalues  " + values);
            if (values != null) {
                Log.w(TAG, "renew rights:" + values);
                String httpUrl = values.getAsString("rights_issuer");
                url = httpUrl;
            }
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "renewDrmRightsDownload " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "renewDrmRightsDownload " + ex.toString());
        }
        Log.w(TAG, "renewDrmRightsDownload: url " + url);
        return url;
    }

    public static boolean isDrmCanTransfer(Context context, String path,
            String mimeType) {
        int status;
        try {
            status = mClient.checkRightsStatus(path, DrmStore.Action.TRANSFER);
            if (status == RightsStatus.RIGHTS_VALID) {
                return true;
            }
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "isDrmSDFile IllegalArgumentException " + ex.toString());
        } catch (IllegalStateException ex) {
            Log.w(TAG, "isDrmSDFile IllegalStateException " + ex.toString());
        }

        return false;
    }

    public static Bitmap decodeDrmBitmap(String path) {
        DecryptHandle handle = null;
        if (path == null) {
            return null;
        }
        Options options = new Options();
        options.inJustDecodeBounds = true;
        try {
            handle = mClient.openDecryptSession(path);
            return BitmapFactory.decodeDrmStream(mClient, handle, options);
        } finally {
            if (handle != null) {
                mClient.closeDecryptSession(handle);
            }
        }
    }

    public static Bitmap decodeDrmThumbnail(String filePath, Options options,
            int targetSize, int type, DecryptHandle temphandle) {
        Log.d(TAG, "decodeDrmThumbnail, filePath = " + filePath);
        DrmManagerClient client = mClient;
        if (options == null)
            options = new Options();
        options.inJustDecodeBounds = true;
        DecryptHandle handle = client.openDecryptSession(filePath);
        if (handle != null) {
            Log.d(TAG, "drm{" + filePath + "} Just Bounds start");
            BitmapFactory.decodeDrmStream(client, handle, options);
            Log.d(TAG, "drm{" + filePath + "} Just Bounds end");
            client.closeDecryptSession(handle);
        } else {
            BitmapFactory.decodeFile(filePath, options);
        }

        int w = options.outWidth;
        int h = options.outHeight;
        /*
         * if (type == MediaItem.TYPE_MICROTHUMBNAIL) { // We center-crop the
         * original image as it's micro thumbnail. In this case, // we want to
         * make sure the shorter side >= "targetSize". float scale = (float)
         * targetSize / Math.min(w, h); options.inSampleSize =
         * BitmapUtils.computeSampleSizeLarger(scale);
         * 
         * // For an extremely wide image, e.g. 300x30000, we may got OOM when
         * decoding // it for TYPE_MICROTHUMBNAIL. So we add a max number of
         * pixels limit here. final int MAX_PIXEL_COUNT = 640000; // 400 x 1600
         * if ((w / options.inSampleSize) * (h / options.inSampleSize) >
         * MAX_PIXEL_COUNT) { options.inSampleSize =
         * BitmapUtils.computeSampleSize( FloatMath.sqrt((float) MAX_PIXEL_COUNT
         * / (w * h))); } } else { // For screen nail, we only want to keep the
         * longer side >= targetSize. float scale = (float) targetSize /
         * Math.max(w, h); options.inSampleSize =
         * BitmapUtils.computeSampleSizeLarger(scale); }
         */
        options.inJustDecodeBounds = false;
        handle = client.openDecryptSession(filePath);
        Bitmap result = null;
        if (handle != null) {
            Log.d(TAG, "drm{" + filePath + "} decode bitmap start");
            //mClient.setPlaybackStatus(handle, DrmStore.Playback.START);
            result = BitmapFactory.decodeDrmStream(client, handle, options);
            //mClient.setPlaybackStatus(handle, DrmStore.Playback.STOP);
            Log.d(TAG, "drm{" + filePath + "} decode bitmap end");
            //client.closeDecryptSession(handle);
        } else {
            BitmapFactory.decodeFile(filePath, options);
        }

        if (result == null) {
            Log.w(TAG, "Drm bitmap decode result is null!");
            return null;
        }

        // We need to resize down if the decoder does not support inSampleSize
        // (For example, GIF images)
        /*
         * float scale = (float) targetSize / (type ==
         * MediaItem.TYPE_MICROTHUMBNAIL ? Math.min(result.getWidth(),
         * result.getHeight()) : Math.max(result.getWidth(),
         * result.getHeight()));
         */

        /*
         * if (scale <= 0.5) result = BitmapUtils.resizeBitmapByScale(result,
         * scale, true); if (result == null || result.getConfig() != null)
         * return result;
         */
        Bitmap newBitmap = result.copy(Config.ARGB_8888, false);
        result.recycle();
        return newBitmap;
    }

    public static String getFileName(Context context, String path) {
        String fileName = null;
        if (path != null) {
            int nameStart = path.lastIndexOf("/");
            int dotIndex = path.lastIndexOf(".");
            if (nameStart > 0 && dotIndex > nameStart) {
                fileName = path.substring(nameStart + 1, dotIndex) + ".dcf";
            }
        }
        return fileName;
    }

    public static InputStream decodeGifStream(Context context, String path) {
        if (path == null) {
            Log.d(TAG, "decodeGifStream path is null");
            return null;
        }
        return GifDecoder.get(mClient).readDrmInputStream(path);
    }

    public static InputStream decodeGifStream(Context context, Uri uri) {
        if (uri == null) {
            Log.d(TAG, "decodeGifStream uri is null");
            return null;
        }

        String path = MessagingUriUtil.getPath(context, uri);
        return GifDecoder.get(mClient).readDrmInputStream(path);
    }

    public static byte[] decodeDrmByteArray(Context context, String dataPath, DecryptHandle tempdecryptHandle) {
        if (dataPath == null) {
            Log.d(TAG, "decodeDrmByteArray path is null");
            return null;
        }
        int fileSize = 0;
        FileInputStream fis = null;
        Log.d(TAG, " decodeDrmByteArray path is " + dataPath);
        DecryptHandle decryptHandle = mClient.openDecryptSession(dataPath);
        if (decryptHandle == null){
            //try once
            Log.d(TAG, " decodeDrmByteArray decryptHandle is null,  try once " + dataPath);
            decryptHandle = mClient.openDecryptSession(dataPath);
        }
        try {
            File file = new File(dataPath);
            if (file.exists()) {
                fis = new FileInputStream(file);
                fileSize = fis.available();
            }
        } catch (Exception e) {
            Log.d(TAG, "decodeDrmByteArray.file error");
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                Log.d(TAG, "decodeDrmByteArray.file close error");
                e.printStackTrace();
            }
        }
        byte[] ret = null;
	    if (decryptHandle!=null) {
            //mClient.setPlaybackStatus(decryptHandle, DrmStore.Playback.START);
            ret = mClient.pread(decryptHandle, fileSize, 0);
            //mClient.setPlaybackStatus(decryptHandle, DrmStore.Playback.STOP);
        }
        Log.d(TAG, "Drm decodeDrmByteArray pread ret = " + ret);
        // is = new ByteArrayInputStream(ret);
        if (decryptHandle != null) {
            mClient.closeDecryptSession(decryptHandle);
        }
        return ret;
    }

    public static byte[] decodeDrmByteArray(Context context, Uri uri, DecryptHandle decryptHandle) {
        if (uri == null) {
            Log.d(TAG, "decodeDrmByteArray uri is null");
            return null;
        }

        String path = MessagingUriUtil.getPath(context, uri);
        return decodeDrmByteArray(context, path, decryptHandle);
    }

    public static DecryptHandle  setDrmPlayStatusStart(String filePath) {
        Log.d(TAG, "setDrmPlayStatus---start");
        DecryptHandle decryptHandle = mClient.openDecryptSession(filePath);
        if (decryptHandle != null) {
            mClient.setPlaybackStatus(decryptHandle, DrmStore.Playback.START);
            Log.d(TAG, "setDrmPlayStatus filepath = {" + filePath + "}");
        } else {
            Log.e(TAG, "mDecryptHandle open fail :" + filePath);
        }
        return decryptHandle;
    }
    public static boolean  setDrmPlayStatusStop(DecryptHandle decryptHandle) {
        Log.d(TAG, "setDrmPlayStatus---stop : "+decryptHandle);
        if (decryptHandle != null) {
            mClient.setPlaybackStatus(decryptHandle, DrmStore.Playback.STOP);
            mClient.closeDecryptSession(decryptHandle);
        } else {
            Log.e(TAG, "mDecryptHandle  args is null ");
        }
        return true;
    }
}
