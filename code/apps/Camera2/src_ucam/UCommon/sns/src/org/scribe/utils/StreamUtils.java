/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.utils;

import java.io.*;

import com.ucamera.ucomm.sns.Util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.util.Log;

/**
 * Utils to deal with Streams.
 *
 * @author Pablo Fernandez
 */
public class StreamUtils {
    /**
     * Returns the stream contents as an UTF-8 encoded string
     *
     * @param is input stream
     * @return string contents
     */
    public static String getStreamContents(InputStream is) {
        Preconditions.checkNotNull(is, "Cannot get String from a null object");
        try {
            final char[] buffer = new char[0x10000];
            StringBuilder out = new StringBuilder();
            Reader in = new InputStreamReader(is, "UTF-8");
            int read;
            do {
                read = in.read(buffer, 0, buffer.length);
                if (read > 0) {
                    out.append(buffer, 0, read);
                }
            } while (read >= 0);
            in.close();
            return out.toString();
        } catch (IOException ioe) {
            throw new IllegalStateException("Error while reading response body", ioe);
        }
    }

    public static InputStream createBitmap(Context context, Uri uri, int maxWidth, int maxHeight) {
        InputStream is = null;

        // decode bitmap bounds
        int bitmapWidth = 0;
        int bitmapHeight = 0;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            is = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(is,null,options);
            bitmapWidth = options.outWidth;
            bitmapHeight = options.outHeight;
        } catch (IOException e) {
            Log.e("BitmapUtil", "fail get bounds info",e);
            return null;
        } finally {
            Util.closeSilently(is);
        }

        int sampleSize = 1;
        while (maxWidth < (bitmapWidth / sampleSize) || maxHeight < (bitmapHeight / sampleSize) ) {
            sampleSize <<= 1;
        }

        try {
            Bitmap tmp = null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            is = context.getContentResolver().openInputStream(uri);
            tmp = BitmapFactory.decodeStream(is,null,options);

            float scale = Math.min(1,Math.min((float)maxWidth/options.outWidth, (float)maxHeight/options.outHeight));
            int w = (int)(options.outWidth * scale);
            int h = (int)(options.outHeight*scale);
            Bitmap bitmap = Bitmap.createScaledBitmap(tmp,w,h, false);
            if (bitmap != tmp){
                Util.recyleBitmap(tmp);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(CompressFormat.PNG, 100, bos);
            InputStream ins = new ByteArrayInputStream(bos.toByteArray());
            bos.close();
            Util.recyleBitmap(bitmap);
            return ins;
        } catch (Exception e) {
            Log.e("BitmapUtil","Failt create bitmap",e);
            // some error occure
        }finally {
            Util.closeSilently(is);
        }
        return null;
    }
}
