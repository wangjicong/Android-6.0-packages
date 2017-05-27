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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.mmsfolderview.data.media.ImageRequest;
import com.android.mmsfolderview.data.media.MediaCacheManager;
import com.android.mmsfolderview.data.media.MemoryCacheManager;
import com.android.mmsfolderview.util.exif.ExifInterface;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int MAX_OOM_COUNT = 1;
    private static final byte[] GIF87_HEADER = "GIF87a".getBytes(Charset.forName("US-ASCII"));
    private static final byte[] GIF89_HEADER = "GIF89a".getBytes(Charset.forName("US-ASCII"));

    // Used for drawBitmapWithCircleOnCanvas.
    // Default color is transparent for both circle background and stroke.
    public static final int DEFAULT_CIRCLE_BACKGROUND_COLOR = 0;
    public static final int DEFAULT_CIRCLE_STROKE_COLOR = 0;

    private static volatile ImageUtils sInstance;

    public static ImageUtils get() {
        if (sInstance == null) {
            synchronized (ImageUtils.class) {
                if (sInstance == null) {
                    sInstance = new ImageUtils();
                }
            }
        }
        return sInstance;
    }

    @VisibleForTesting
    public static void set(final ImageUtils imageUtils) {
        sInstance = imageUtils;
    }

    /**
     * The drawable can be a Nine-Patch. If we directly use the same drawable
     * instance for each drawable of different sizes, then the drawable sizes
     * would interfere with each other. The solution here is to create a new
     * drawable instance for every time with the SAME ConstantState (i.e.
     * sharing the same common state such as the bitmap, so that we don't have
     * to recreate the bitmap resource), and apply the different properties on
     * top (nine-patch size and color tint). TODO: we are creating new drawable
     * instances here, but there are optimizations that can be made. For
     * example, message bubbles shouldn't need the mutate() call and the
     * play/pause buttons shouldn't need to create new drawable from the
     * constant state.
     */
    public static Drawable getTintedDrawable(final Context context, final Drawable drawable,
            final int color) {
        // For some reason occassionally drawables on JB has a null constant
        // state
        final Drawable.ConstantState constantStateDrawable = drawable.getConstantState();
        final Drawable retDrawable = (constantStateDrawable != null) ? constantStateDrawable
                .newDrawable(context.getResources()).mutate() : drawable;
        retDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        return retDrawable;
    }

    /**
     * Returns whether the resource is a GIF image.
     */
    public static boolean isGif(Context context, String contentType, Uri contentUri) {
        if (TextUtils.equals(contentType, ContentType.IMAGE_GIF)) {
            return true;
        }
        if (ContentType.isImageType(contentType)) {
            try {
                ContentResolver contentResolver = context.getContentResolver();
                InputStream inputStream = contentResolver.openInputStream(contentUri);
                return ImageUtils.isGif(inputStream);
            } catch (Exception e) {
                Log.e(TAG, "Could not open GIF input stream", e);
            }
        }
        // Assume anything with a non-image content type is not a GIF
        return false;
    }

    /**
     * @param inputStream The stream to the image file. Closed on completion
     * @return Whether the image stream represents a GIF
     */
    public static boolean isGif(InputStream inputStream) {
        if (inputStream != null) {
            try {
                byte[] gifHeaderBytes = new byte[6];
                int value = inputStream.read(gifHeaderBytes, 0, 6);
                if (value == 6) {
                    return Arrays.equals(gifHeaderBytes, GIF87_HEADER)
                            || Arrays.equals(gifHeaderBytes, GIF89_HEADER);
                }
            } catch (IOException e) {
                return false;
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return false;
    }

    /**
     * @param inputStream The stream to the image file. Closed on completion
     * @return The exif orientation value for the image in the specified stream
     */
    public static int getOrientation(final InputStream inputStream) {
        int orientation = android.media.ExifInterface.ORIENTATION_UNDEFINED;
        if (inputStream != null) {
            try {
                final ExifInterface exifInterface = new ExifInterface();
                exifInterface.readExif(inputStream);
                final Integer orientationValue = exifInterface
                        .getTagIntValue(ExifInterface.TAG_ORIENTATION);
                if (orientationValue != null) {
                    orientation = orientationValue.intValue();
                }
            } catch (IOException e) {
                // If the image if GIF, PNG, or missing exif header, just use
                // the defaults
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "getOrientation error closing input stream", e);
                }
            }
        }
        return orientation;
    }

    /**
     * Based on the input bitmap bounds given by BitmapFactory.Options, compute
     * the required sub-sampling size for loading a scaled down version of the
     * bitmap to the required size
     * 
     * @param options a BitmapFactory.Options instance containing the bounds
     *            info of the bitmap
     * @param reqWidth the desired width of the bitmap. Can be
     *            ImageRequest.UNSPECIFIED_SIZE.
     * @param reqHeight the desired height of the bitmap. Can be
     *            ImageRequest.UNSPECIFIED_SIZE.
     * @return
     */
    public int calculateInSampleSize(final BitmapFactory.Options options, final int reqWidth,
            final int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        final boolean checkHeight = reqHeight != ImageRequest.UNSPECIFIED_SIZE;
        final boolean checkWidth = reqWidth != ImageRequest.UNSPECIFIED_SIZE;
        if ((checkHeight && height > reqHeight) || (checkWidth && width > reqWidth)) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((!checkHeight || (halfHeight / inSampleSize) > reqHeight)
                    && (!checkWidth || (halfWidth / inSampleSize) > reqWidth)) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    
    /**
     * Given the source bitmap and a canvas, draws the bitmap through a circular
     * mask. Only draws a circle with diameter equal to the destination width.
     *
     * @param bitmap The source bitmap to draw.
     * @param canvas The canvas to draw it on.
     * @param source The source bound of the bitmap.
     * @param dest The destination bound on the canvas.
     * @param bitmapPaint Optional Paint object for the bitmap
     * @param fillBackground when set, fill the circle with backgroundColor
     * @param strokeColor draw a border outside the circle with strokeColor
     */
    public static void drawBitmapWithCircleOnCanvas(final Bitmap bitmap, final Canvas canvas,
            final RectF source, final RectF dest, @Nullable Paint bitmapPaint,
            final boolean fillBackground, final int backgroundColor, int strokeColor) {
        // Draw bitmap through shader first.
        final BitmapShader shader = new BitmapShader(bitmap, TileMode.CLAMP, TileMode.CLAMP);
        final Matrix matrix = new Matrix();

        // Fit bitmap to bounds.
        matrix.setRectToRect(source, dest, Matrix.ScaleToFit.CENTER);

        shader.setLocalMatrix(matrix);

        if (bitmapPaint == null) {
            bitmapPaint = new Paint();
        }

        bitmapPaint.setAntiAlias(true);
        if (fillBackground) {
            bitmapPaint.setColor(backgroundColor);
            canvas.drawCircle(dest.centerX(), dest.centerX(), dest.width() / 2f, bitmapPaint);
        }

        bitmapPaint.setShader(shader);
        canvas.drawCircle(dest.centerX(), dest.centerX(), dest.width() / 2f, bitmapPaint);
        bitmapPaint.setShader(null);

        if (strokeColor != 0) {
            final Paint stroke = new Paint();
            stroke.setAntiAlias(true);
            stroke.setColor(strokeColor);
            stroke.setStyle(Paint.Style.STROKE);
            final float strokeWidth = 6f;
            stroke.setStrokeWidth(strokeWidth);
            canvas.drawCircle(dest.centerX(),
                    dest.centerX(),
                    dest.width() / 2f - stroke.getStrokeWidth() / 2f,
                    stroke);
        }
    }
    /**
     * Transforms a bitmap into a byte array.
     *
     * @param quality Value between 0 and 100 that the compressor uses to discern what quality the
     *                resulting bytes should be
     * @param bitmap Bitmap to convert into bytes
     * @return byte array of bitmap
     */
    public static byte[] bitmapToBytes(final Bitmap bitmap, final int quality)
            throws OutOfMemoryError {
        boolean done = false;
        int oomCount = 0;
        byte[] imageBytes = null;
        while (!done) {
            try {
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, os);
                imageBytes = os.toByteArray();
                done = true;
            } catch (final OutOfMemoryError e) {
                Log.d(TAG, "OutOfMemory converting bitmap to bytes.");
                oomCount++;
                if (oomCount <= MAX_OOM_COUNT) {
                    MemoryCacheManager.get().reclaimMemory();
                } else {
                    done = true;
                    Log.d(TAG, "Failed to convert bitmap to bytes. Out of Memory.");
                }
                throw e;
            }
        }
        return imageBytes;
    }
}
