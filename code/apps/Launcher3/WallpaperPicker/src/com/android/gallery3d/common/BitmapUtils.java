/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.common;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import com.android.gallery3d.exif.ExifInterface;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
public class BitmapUtils {

    private static final String TAG = "BitmapUtils";
    private static int sLimitedSize = -1;

    // Find the min x that 1 / x >= scale
    public static int computeSampleSizeLarger(float scale) {
        int initialSize = (int) Math.floor(1f / scale);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? Utils.prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    /* SPRD: Bug 492417 The wallpaperpicker doesn't support gif or wbmp @{ */
    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static Bitmap resizeBitmapByScale(
            Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth()
                && height == bitmap.getHeight())
            return bitmap;
        Bitmap target;
        /* SPRD: bug 508632 OOM when select a image whose size is 9000*9000 @{ */
        try {
            target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        } catch (OutOfMemoryError e) {
            Log.d(TAG, "BitmapUtils OutOfMemoryError");
            return null;
        }
        /* @} */
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle)
            bitmap.recycle();
        return target;
    }

    /* @} */

    public static int getRotationFromExif(Context context, Uri uri) {
        return BitmapUtils.getRotationFromExifHelper(null, 0, context, uri);
    }

    public static int getRotationFromExif(Resources res, int resId) {
        return BitmapUtils.getRotationFromExifHelper(res, resId, null, null);
    }

    private static int getRotationFromExifHelper(Resources res, int resId, Context context, Uri uri) {
        ExifInterface ei = new ExifInterface();
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            if (uri != null) {
                is = context.getContentResolver().openInputStream(uri);
                bis = new BufferedInputStream(is);
                ei.readExif(bis);
            } else {
                is = res.openRawResource(resId);
                bis = new BufferedInputStream(is);
                ei.readExif(bis);
            }
            Integer ori = ei.getTagIntValue(ExifInterface.TAG_ORIENTATION);
            if (ori != null) {
                return ExifInterface.getRotationForOrientationValue(ori.shortValue());
            }
        } catch (IOException e) {
            Log.w(TAG, "Getting exif data failed", e);
        } catch (NullPointerException e) {
            // Sometimes the ExifInterface has an internal NPE if Exif data isn't valid
            Log.w(TAG, "Getting exif data failed", e);
        } finally {
            Utils.closeSilently(bis);
            Utils.closeSilently(is);
        }
        return 0;
    }

    /* SPRD: bug 508632 OOM when select a image whose size is 9000*9000 @{ */
    public static int getSampleSize(InputStream is) {
        int sampleSize = 1;
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);

        int outWidth = options.outWidth;
        int outHeight = options.outHeight;

        int widthScale = Math.round(outWidth / getLimitedSize());
        int heightScale = Math.round(outHeight / getLimitedSize());

        int destSize = widthScale > heightScale ? widthScale : heightScale;
        sampleSize = destSize > sampleSize ? destSize : sampleSize;

        return sampleSize;
    }

    public static int getLimitedSize() {
        if (sLimitedSize == -1) {
            String limited = SystemProperties.get("dalvik.vm.heapgrowthlimit");
            String number = limited.substring(0, limited.indexOf("m"));
            int limitedSize = Integer.decode(number) * 1024 * 1024;
            // The bitmap should support 32bit, we need to divide 3
            //(Set aside a part of memory,so we need to divide 4).
            // We want to shrink bitmap size to one of eight, so we need to
            // divide 8.
            sLimitedSize = (int) Math.sqrt(limitedSize / 4 / 8);
        }
        return sLimitedSize;
    }
    /* @} */

}
