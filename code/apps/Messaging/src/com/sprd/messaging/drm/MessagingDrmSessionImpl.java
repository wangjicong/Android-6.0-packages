/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.sprd.messaging.drm;

import android.content.Context;
import android.content.ContentValues;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Bitmap.Config;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.provider.MediaStore;
import android.net.Uri;
import android.content.Intent;
//import android.os.SystemProperties;
import android.drm.DrmManagerClient;
import android.drm.DrmRights;
import android.drm.DecryptHandle;
import android.database.Cursor;
import android.drm.DrmManagerClient.OnEventListener;
import android.drm.DrmStore.RightsStatus;
import com.android.messaging.R;

import android.os.*;
import android.os.storage.*;
import java.io.*;

import java.io.File;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


import android.provider.MediaStore;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.R.bool;
import android.R.integer;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.drm.DrmStore;
import android.drm.DrmManagerClient;
import android.graphics.Color;


public class MessagingDrmSessionImpl extends MessagingDrmSession {

    private Context mContext;
    private boolean isDrmEnable = false;
    public final static int ERROR = 0;
    public final static int NORMAL_FILETYPE = 0;
    public final static int DRM_SD_FILETYPE = 1;
    public final static int DRM_OTHER_FILETYPE = 2;
    public final static int ABNORMAL_FILETYPE = -1;
    public static final String DRM_DCF_FILE = ".dcf";
    public String mDrmPath;
    public boolean mIsDrm;

    private static final String TAG = "MessagingDrmSessionImpl";

    public MessagingDrmSessionImpl(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean alertDrmError(Intent intent, Uri uri) {
        Log.i(TAG, "alertDrmError: intent " + intent + " and uri " + uri);
        String path = MessagingUriUtil.getPath(mContext, uri);
        if (MessagingDrmHelper.isDrmMimeType(mContext, path, null, isDrmEnable)) {
            boolean wallpaperExtra = intent.hasExtra("applyForWallpaper");
            boolean outExtra = intent.hasExtra(MediaStore.EXTRA_OUTPUT);
            if (wallpaperExtra || outExtra) {
                // Toast.makeText(sContext, R.string.drm_not_be_selected,
                // Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!MessagingDrmHelper.isDrmSDFile(mContext, path, null)) {
                Log.d(TAG, "activity -- isSDfile   ");
                // Toast.makeText(sContext, R.string.drm_not_be_shared,
                // Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isDrmEnabled() {
        checkDrmEnabled();
        return isDrmEnable;
    }

    @Override
    public boolean drmCanHandle(String path, String containingMimeType) {
        checkDrmEnabled();
        return MessagingDrmHelper.isDrmMimeType(mContext, path,
                containingMimeType, isDrmEnable);
    }

    @Override
    public int getDrmObjectType(String path, String containingMimeType) {
        return MessagingDrmHelper.getDrmObjectType(mContext, path,
                containingMimeType);
    }

    @Override
    public int saveDrmObjectRights(String path, String containingMimeType) {
        return MessagingDrmHelper.saveDrmObjectRights(mContext, path,
                containingMimeType);
    }

    private void checkDrmEnabled() {
        // String prop = SystemProperties.get("drm.service.enabled");
        isDrmEnable = true/* prop != null && prop.equals("true") */;
    }

    public String getDrmOrigMimeType(String path, String mimeType) {
        checkDrmEnabled();
        return MessagingDrmHelper.getOriginalMimeType(mContext, path, mimeType,
                isDrmEnable);
    }

    @Override
    public ContentValues getConstraints(String path, int action) {
        return MessagingDrmHelper.getConstraints(mContext, path, action);
    }

    @Override
    public ContentValues getMetadata(String path) {
        return MessagingDrmHelper.getMetadata(mContext, path);
    }

    @Override
    public boolean isCanTranfer(Uri uri, String mimeType) {
        int fileType = getDrmFileType(mContext, uri, mimeType);
        switch (fileType) {
        case DRM_SD_FILETYPE:
            return true;
        case DRM_OTHER_FILETYPE:
            // Toast.makeText(sContext, R.string.drm_not_be_shared,
            // Toast.LENGTH_SHORT).show();
            break;
        case ABNORMAL_FILETYPE:
            // Toast.makeText(sContext, R.string.error_in_shared,
            // Toast.LENGTH_SHORT).show();
            break;
        default:
            break;
        }
        return false;
    }

    private int getDrmFileType(Context context, Uri uri, String mimetype) {
        checkDrmEnabled();
        if (!MessagingDrmHelper.isDrmMimeType(context, null, mimetype,
                isDrmEnable)) {
            return ABNORMAL_FILETYPE;
        }
        String path = MessagingUriUtil.getPath(context, uri);
        Log.i(TAG, "getDrmCanSharedType path " + path);
        if (path != null) {
            if (MessagingDrmHelper.isDrmCanTransfer(context, path, mimetype)) {
                return DRM_SD_FILETYPE;
            } else {
                return DRM_OTHER_FILETYPE;
            }
        }
        return ABNORMAL_FILETYPE;
    }

    public int getDrmIconMimeDrawableId(String path, String containingMimeType) {
        String originType = null;
        checkDrmEnabled();
        Log.d(TAG, "getDrmIconMimeDrawableId");
        originType = MessagingDrmHelper.getOriginalMimeType(mContext, path,
                containingMimeType, isDrmEnable);
        int rightsStates = MessagingDrmHelper.getRightsStatus(mContext, path,
                containingMimeType);
        if (originType == null) {
            return R.drawable.ic_doc_generic_am;
        }
        if (originType.startsWith("image/")) {
            if (rightsStates == RightsStatus.RIGHTS_VALID) {
                return R.drawable.drm_image_unlock;
            } else {
                return R.drawable.drm_image_lock;
            }

        } else if (originType.startsWith("audio/")
                || originType.equalsIgnoreCase("application/ogg")) {
            if (rightsStates == RightsStatus.RIGHTS_VALID) {
                return R.drawable.drm_audio_unlock;
            } else {
                return R.drawable.drm_audio_lock;
            }
        } else if (originType.startsWith("video/")) {
            if (rightsStates == RightsStatus.RIGHTS_VALID) {
                return R.drawable.drm_video_unlock;
            } else {
                return R.drawable.drm_video_lock;
            }
        } else {
            return R.drawable.ic_doc_generic_am;
        }
    }

    public String getDrmFilenameFromPath(String path) {
        String drmName = path;
        if (path != null) {
            int index = path.lastIndexOf("/");
            if (index > 0) {
                drmName = path.substring(index + 1);
            }
        }
        return drmName;
    }

    @Override
    public String getDrmPath(Uri uri) {
        String mDrmPath;
        mDrmPath = MessagingUriUtil.getPath(mContext, uri);
        return mDrmPath;
    }

    @Override
    public boolean isDrmPath(String drmPath) {
        boolean isDrmPath = false;
        if (drmPath != null) {
            checkDrmEnabled();
            isDrmPath = MessagingDrmHelper.isDrmMimeType(mContext, drmPath,
                    null, isDrmEnable);
        }
        return isDrmPath;
    }

    @Override
    public int getIconImage(String path, String containingMimeType) {
        checkDrmEnabled();
        Log.d(TAG, "getIconImage isDrm " + isDrmEnable + " containingMimeType "
                + containingMimeType);
        if (isDrmEnable && containingMimeType != null) {
            return getDrmIconMimeDrawableId(path, containingMimeType);
        }
        return ERROR;
    }

    @Override
    public int processDrmInfo(final String path, final String destPath,
            final String containingMimeType, final OnEventListener listener) {
        return MessagingDrmHelper.processDrmInfo(mContext, path, destPath,
                containingMimeType, listener);
    }

    @Override
    public String generateDestinationfile(String file) {
        return MessagingDrmHelper.generateDestinationfile(file);
    }

    @Override
    public Bitmap decodeDrmBitmap(String path) {
        return MessagingDrmHelper.decodeDrmBitmap(path);
    }

    @Override
    public Bitmap decodeDrmThumbnail(String filePath, Options options,
            int targetSize, int type, DecryptHandle handle) {
        return MessagingDrmHelper.decodeDrmThumbnail(filePath, options,
                targetSize, type, handle);
    }

    @Override
    public InputStream decodeGifStream(String path) {
        return MessagingDrmHelper.decodeGifStream(mContext, path);
    }

    @Override
    public InputStream decodeGifStream(Uri uri) {
        return MessagingDrmHelper.decodeGifStream(mContext, uri);
    }

    @Override
    public byte[] decodeDrmByteArray(String path, DecryptHandle handle) {
        return MessagingDrmHelper.decodeDrmByteArray(mContext, path, handle);
    }

    @Override
    public byte[] decodeDrmByteArray(Uri uri, DecryptHandle handle) {
        return MessagingDrmHelper.decodeDrmByteArray(mContext, uri, handle);
    }

    @Override
    public String getPath(Uri uri) {
        return MessagingUriUtil.getPath(mContext, uri);
    }

    @Override
    public String getFileName(String path) {
        return MessagingDrmHelper.getFileName(mContext, path);
    }

    @Override
    public String getDrmPath() {
        String drmPath = null;
        String status = null;
        String path = null;
        status = Environment.getInternalStoragePathState();// TCard
        path = Environment.getInternalStoragePath().getPath();// TCard
        String externalStatus = Environment.getExternalStoragePathState(); // TCard
        String externalStoragePath = Environment.getExternalStoragePath()
                .getPath();// TCard
        if (externalStatus.equals(Environment.MEDIA_MOUNTED)) {
            drmPath = createDRMdownloadDirectory(externalStoragePath);
            Log.i(TAG, "external path  " + drmPath);
        } else if (status.equals(Environment.MEDIA_MOUNTED)) {
            drmPath = createDRMdownloadDirectory(path);
            Log.i(TAG, "internal path  " + drmPath);
        }
        return drmPath;
    }

    private String createDRMdownloadDirectory(String path) {
        String dir = path + "/Mms/DrmDownload";
        File file = new File(dir);
        Log.i(TAG, "createDRMdownloadDirectory   dir  " + dir);
        if (file.exists()) {
            if (file.isDirectory()) {
                Log.i(TAG,
                        "createDRMdownloadDirectory   file exitst isDirectory  ");
                return (dir + "/");
            } else {
                boolean create = file.mkdirs();

                if (create) {
                    Log.i(TAG,
                            "createDRMdownloadDirectory   file exitst create true ");
                    return (dir + "/");
                } else {
                    Log.i(TAG,
                            "createDRMdownloadDirectory   file exitst return null  ");
                    return null;
                }
            }

        } else {
            boolean sucCreate = file.mkdirs();
            if (sucCreate) {
                Log.i(TAG,
                        "createDRMdownloadDirectory   file not exitst create true  ");
                return (dir + "/");
            } else {
                Log.i(TAG,
                        "createDRMdownloadDirectory   file not  exitst return null  ");
                return null;
            }
        }
    }

    @Override
    public boolean getDrmFileRightsStatus(String path, String containingMimeType) {
        String originType = null;
        checkDrmEnabled();
        Log.d(TAG, "getDrmFileRightsStatus");
        originType = MessagingDrmHelper.getOriginalMimeType(mContext, path,
                containingMimeType, isDrmEnable);
        int rightsStates = MessagingDrmHelper.getRightsStatus(mContext, path,
                containingMimeType);
        Log.d(TAG, "getDrmFileRightsStatus rightsStates " + rightsStates);
        if (rightsStates == RightsStatus.RIGHTS_VALID) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AlertDialog.Builder showProtectInfo(Context context, String filePath, boolean isPicture) {
        Resources res = context.getResources();

        /* Modify by SPRD for Bug:524873  2015.01.21 Start */
        Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        builder.setTitle(res.getString(R.string.drm_consume_title));
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View drmConsumeView = inflater.inflate(R.layout.drm_consume_dialog, null, false);
        TextView drmConsumeSummaryTv = (TextView) drmConsumeView.findViewById(R.id.drm_consume_sumary);
        drmConsumeSummaryTv.setText(res.getString(R.string.drm_consume_hint));
        TextView drmConsumeDetailTv = (TextView) drmConsumeView.findViewById(R.id.drm_consume_detail);
//        String fileMessage = "";
//        TextView detailView = new TextView(context);
//        detailView.setPadding(20, 0, 10, 0);
//        detailView.setTextSize(18);
//        detailView.setHorizontallyScrolling(false);
//        ScrollView scrollView = new ScrollView(context);
//        scrollView.addView(detailView);
        /* Modify by SPRD for Bug:524873  2015.01.21 End */

        ContentValues drmContentValues = MessagingDrmHelper.getConstraints(context, filePath,
                isPicture? DrmStore.Action.DISPLAY:DrmStore.Action.PLAY);
        File file = new File(filePath);
        Long startTime = (Long) drmContentValues
                .getAsLong(DrmStore.ConstraintsColumns.LICENSE_START_TIME);
        Long endTime = (Long) drmContentValues
                .getAsLong(DrmStore.ConstraintsColumns.LICENSE_EXPIRY_TIME);
        byte[] clickTime = drmContentValues
                .getAsByteArray(DrmStore.ConstraintsColumns.EXTENDED_METADATA);

        String expirationTime = (String) drmContentValues
                .get(DrmStore.ConstraintsColumns.LICENSE_AVAILABLE_TIME);
        Object remainObject = drmContentValues
                .get(DrmStore.ConstraintsColumns.REMAINING_REPEAT_COUNT);
        String remainTimes = null;
        if (remainObject != null) {
            remainTimes = remainObject.toString();
        }

        String fileMessage =res.getString(R.string.drm_file_path, file.getParent())
                +"\n"
                +res.getString(R.string.drm_file_name, file.getName())
                + "\n"
                + (isDrmValid(filePath) ? res
                .getString(R.string.drm_rights_validity)
                : res.getString(R.string.drm_rights_invalidity))
                + "\n"
                + (canTransfer(filePath) ? res
                .getString(R.string.drm_rights_status_transfer)
                : res.getString(R.string.drm_rights_status_untransfer))
                + "\n"
                + (res.getString(R.string.drm_start_time, transferDate(startTime, context)))
                + "\n"
                + (res.getString(R.string.drm_end_time, transferDate(endTime, context)))
                + "\n"
                + res.getString(R.string.drm_expiration_time,
                getExpirationTime(expirationTime, clickTime, context))
                + "\n"
                + res.getString(R.string.drm_remain_times,
                getRemainTimesValue(remainTimes, context));

        /* Modify by SPRD for Bug:524873  2015.01.21 Start */
//        detailView.setText(fileMessage);
//        builder.setView(scrollView);
        drmConsumeDetailTv.setText(fileMessage);
        builder.setView(drmConsumeView);
        /* Modify by SPRD for Bug:524873  2015.01.21 End */

        return builder;
    }

    private boolean isDrmValid(String filePath) {
        if (null == filePath || filePath.equals("")){
            return false;
        }
        return (DrmStore.RightsStatus.RIGHTS_VALID == MessagingDrmHelper.getDrmManagerClient().checkRightsStatus(filePath));
    }
    private boolean canTransfer(String filePath) {
        return true;
    }

    private String transferDate(Long time, Context context) {
        if (time == null) {
            return context.getString(R.string.drm_rights_unknown);
        }
        if (time == -1) {
            return context.getString(R.string.drm_unlimited_rights);
        }
        Date date = new Date(time * 1000);
        SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdformat.format(date);
    }

    public Object getExpirationTime(Object object, byte[] clickTime, Context context) {
        if (object == null) {
            return context.getString(R.string.drm_rights_unknown);
        } else if (clickTime == null) {
            return context.getString(R.string.drm_inactive_rights);
        } else if (object.toString().equals(RIGHTS_NO_lIMIT)) {
            return context.getString(R.string.drm_unlimited_rights);
        } else {
            String cTime = new String(clickTime);
            Long time = Long.valueOf(object.toString()) + Long.valueOf(cTime);
            return transferDate(time, context);
        }
    }

    private String getRemainTimesValue(Object object, Context context) {
        if (object == null) {
            return context.getString(R.string.drm_rights_unknown);
        }
        return object.toString().equals(RIGHTS_NO_lIMIT) ?
                context.getString(R.string.drm_unlimited_rights) : object.toString();
    }
    private static final String RIGHTS_NO_lIMIT = "-1";
}
