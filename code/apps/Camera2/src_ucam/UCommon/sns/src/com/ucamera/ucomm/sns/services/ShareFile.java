/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */

package com.ucamera.ucomm.sns.services;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore.Images.Media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.scribe.utils.StreamUtils;

import com.ucamera.ucomm.sns.integration.ShareUtils;

public class ShareFile {
    private static final String DEFUALT_NAME = "ucam-upload";
    private Activity             mContext;
    private Uri                 mDataUri;
    private String              mName        = DEFUALT_NAME;
    private String              mFilePath;
    private String              mMimeType;

    public ShareFile(Activity context, Uri data) {
        mContext = context;
        mDataUri = data;
        initImageInfo();
    }

    public InputStream open() throws FileNotFoundException {
        int width = getSize();
        String model = Build.MODEL.replace('-', '_').replace(' ', '_');
        if("sp7710ga".equals(model) || "LA_M1".equals(model)) {
            return StreamUtils.createBitmap(mContext, mDataUri, width, width);
        }else
            return mContext.getContentResolver().openInputStream(mDataUri);
    }
    private int getSize()
    {
        final ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);

            double availMem = (memInfo.availMem - memInfo.threshold) * 0.8;
            double maxSize = (availMem/4);
            int x=1024;
            return (int)Math.min(Math.sqrt(maxSize), x);
    }
    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setMimeType(String type) {
        this.mMimeType = type;
    }

    public String getMimeType() {
        return mMimeType;
    }

    private void initImageInfo() {
        if (ContentResolver.SCHEME_FILE.equals(mDataUri.getScheme())) {
            initImageInfoFromFile();
        } else if (ContentResolver.SCHEME_CONTENT.equals(mDataUri.getScheme())) {
            initImageInfoFromDB();
        }
    }

    private void initImageInfoFromFile() {
        mFilePath = mDataUri.getPath();
        int index = mFilePath.lastIndexOf(File.separatorChar);
        mName = mFilePath.substring(index);
        if (mName.endsWith(".jpg")) {
            mMimeType = "image/jpeg";
        } else if (mName.endsWith(".gif")) {
            mMimeType = "image/gif";
        } else if (mName.endsWith(".png")) {
            mMimeType = "image/png";
        } else if (mName.endsWith(".bmp")) {
            mMimeType = "image/bmp";
        } else {
            mMimeType = "image/" + mName.substring(mName.lastIndexOf("."));
        }
    }

    private void initImageInfoFromDB() {
        final String[] IMAGE_PROJECTION = new String[] {
                Media.DATA, Media.DISPLAY_NAME, Media.MIME_TYPE
        };
        Cursor cr = mContext.getContentResolver().query(mDataUri, IMAGE_PROJECTION, null, null,
                null);
        if (cr != null) {
            if (cr.moveToNext()) {
                mName = cr.getString(cr.getColumnIndex(Media.DISPLAY_NAME));
                mFilePath = cr.getString(cr.getColumnIndex(Media.DATA));
                mMimeType = cr.getString(cr.getColumnIndex(Media.MIME_TYPE));
            }
            cr.close();
        }
    }
}
