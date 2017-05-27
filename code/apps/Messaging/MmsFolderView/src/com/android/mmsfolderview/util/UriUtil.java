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

package com.android.mmsfolderview.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import com.google.common.io.ByteStreams;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashSet;

public class UriUtil {
    private static final String SCHEME_SMS = "sms";
    private static final String SCHEME_SMSTO = "smsto";
    private static final String SCHEME_MMS = "mms";
    private static final String SCHEME_MMSTO = "smsto";
    public static final HashSet<String> SMS_MMS_SCHEMES = new HashSet<String>(Arrays.asList(
            SCHEME_SMS, SCHEME_MMS, SCHEME_SMSTO, SCHEME_MMSTO));

    public static final String SCHEME_BUGLE = "bugle";
    public static final HashSet<String> SUPPORTED_SCHEME = new HashSet<String>(Arrays.asList(
            ContentResolver.SCHEME_ANDROID_RESOURCE, ContentResolver.SCHEME_CONTENT,
            ContentResolver.SCHEME_FILE, SCHEME_BUGLE));

    public static final String SCHEME_TEL = "tel:";

    /**
     * Constructs an android.resource:// uri for the given resource id.
     */
    public static Uri getUriForResourceId(final Context context, final int resId) {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName()).appendPath(String.valueOf(resId)).build();
    }

    /**
     * Returns whether the given Uri string is local.
     */
    public static boolean isLocalUri(@NonNull  final Uri uri) {
        if (uri != null) {
            return SUPPORTED_SCHEME.contains(uri.getScheme());
        }
        return false;
    }
}
