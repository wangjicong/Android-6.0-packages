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
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.drm.DrmManagerClient.OnEventListener;
import android.drm.DrmStore.RightsStatus;
import android.drm.DrmRights;
import android.drm.DecryptHandle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Bitmap.Config;
import android.app.AlertDialog;

import java.io.InputStream;

import com.android.messaging.Factory;

public class MessagingDrmSession {

    public final static int ERROR = 0;

    private static final String TAG = "MessagingDrmSession";

    static MessagingDrmSession sSession;
    private Context mContext;

    protected MessagingDrmSession(Context context) {
        mContext = context;
    }

    public static MessagingDrmSession get() {
        if (sSession == null) {
            sSession = new MessagingDrmSessionImpl(Factory.get()
                    .getApplicationContext());
        }
        return sSession;
    }

    public boolean alertDrmError(Intent intent, Uri uri) {
        return true;
    }

    public boolean isDrmEnabled() {
        return false;
    }

    public boolean drmCanHandle(String path, String containingMimeType) {
        return false;
    }

    public int getDrmObjectType(String path, String containingMimeType) {
        return 0;
    }

    public int saveDrmObjectRights(String path, String containingMimeType) {
        return 0;
    }

    public String getDrmOrigMimeType(String path, String containingMimeType) {
        return containingMimeType;
    }

    public String getDrmOrigMimeType(Uri uri, String containingMimeType) {
        return null;
    }

    public ContentValues getConstraints(String path, int action) {
        return null;
    }

    public ContentValues getMetadata(String path) {
        return null;
    }

    public boolean isCanTranfer(Uri uri, String containingMimeType) {
        return true;
    }

    public String getDrmFilenameFromPath(String path) {
        return path;
    }

    public String getDrmPath(Uri uri) {
        return null;
    }

    public boolean isDrmPath(String drmPath) {
        return false;
    }

    public int getIconImage(String path, String containingMimeType) {
        return ERROR;
    }

    public int processDrmInfo(final String path, final String destPath,
            final String containingMimeType, final OnEventListener listener) {
        return 0;
    }

    public String generateDestinationfile(String file) {
        return null;
    }

    public Bitmap decodeDrmBitmap(String path) {
        return null;
    }

    public Bitmap decodeDrmThumbnail(String filePath, Options options,
            int targetSize, int type, DecryptHandle handle) {
        return null;
    }

    public InputStream decodeGifStream(String path) {
        return null;
    }

    public InputStream decodeGifStream(Uri uri) {
        return null;
    }

    public byte[] decodeDrmByteArray(String path, DecryptHandle handle) {
        return null;
    }

    public byte[] decodeDrmByteArray(Uri uri, DecryptHandle handle) {
        return null;
    }

    public String getPath(Uri uri) {
        return null;
    }

    public String getFileName(String path) {
        return null;
    }

    public String getDrmPath() {
        return null;
    }

    public boolean getDrmFileRightsStatus(String path, String containingMimeType) {
        return false;
    }

    public AlertDialog.Builder showProtectInfo(Context context, String filePath, boolean isPicture) {return null;}
}
