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

package com.android.launcher3;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Toast;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

import com.android.gallery3d.common.BitmapCropTask;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.launcher3.base.BaseActivity;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.util.WallpaperUtils;
import com.android.photos.BitmapRegionTileSource;
import com.android.photos.BitmapRegionTileSource.BitmapSource;
import com.android.photos.BitmapRegionTileSource.BitmapSource.InBitmapProvider;
import com.android.photos.views.TiledImageRenderer.TileSource;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
/* xihe, 20160706, lock screen wallpaper, SPCSS00295086 @{ */
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.SystemProperties;
/* @} */

public class WallpaperCropActivity extends BaseActivity implements Handler.Callback {
    private static final String LOGTAG = "Launcher3.CropActivity";

    protected static final String WALLPAPER_WIDTH_KEY = WallpaperUtils.WALLPAPER_WIDTH_KEY;
    protected static final String WALLPAPER_HEIGHT_KEY = WallpaperUtils.WALLPAPER_HEIGHT_KEY;

    // SPRD: 507809 check storage permission
    private final static int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 0;

    /**
     * The maximum bitmap size we allow to be returned through the intent.
     * Intents have a maximum of 1MB in total size. However, the Bitmap seems to
     * have some overhead to hit so that we go way below the limit here to make
     * sure the intent stays below 1MB.We should consider just returning a byte
     * array instead of a Bitmap instance to avoid overhead.
     */
    public static final int MAX_BMAP_IN_INTENT = 750000;
    public static final float WALLPAPER_SCREENS_SPAN = WallpaperUtils.WALLPAPER_SCREENS_SPAN;

    private static final int MSG_LOAD_IMAGE = 1;

    protected CropView mCropView;
    protected View mProgressView;
    protected Uri mUri;
    protected View mSetWallpaperButton;
    //SPRD:bug 514951 Launcher permission dialog popup several times
    private static final String IS_REQUEST_PERMISSIONS = "isrequestpermissions";
    private boolean mIsRequestPermissions = false;

    private HandlerThread mLoaderThread;
    private Handler mLoaderHandler;
    @Thunk LoadRequest mCurrentLoadRequest;
    private byte[] mTempStorageForDecoding = new byte[16 * 1024];
    // A weak-set of reusable bitmaps
    @Thunk Set<Bitmap> mReusableBitmaps =
            Collections.newSetFromMap(new WeakHashMap<Bitmap, Boolean>());
    /* xihe, 20160706, lock screen wallpaper, SPCSS00295086 @{ */
    protected int mTarget;
    protected static final int DEFAULT_TARGET = WallpaperCropActivity.WALLPAPER_TARGET_HOME;
    public static final boolean LOCKSCREEN_WALLPAPER_SUPPORT = SystemProperties.getBoolean("ro.LOCKSCREEN_WALLPAPER", false);

    public static final int WALLPAPER_TARGET_HOME = 1;
    public static final int WALLPAPER_TARGET_LOCKED = 2;
    public static final int WALLPAPER_TARGET_BOTH = 3;
    /* @} */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoaderThread = new HandlerThread("wallpaper_loader");
        mLoaderThread.start();
        mLoaderHandler = new Handler(mLoaderThread.getLooper(), this);

        /* SPRD:bug 514951 Launcher permission dialog popup several times @{ */
        if (savedInstanceState != null) {
            mIsRequestPermissions = savedInstanceState.getBoolean(IS_REQUEST_PERMISSIONS);
        }
        /* @} */

        if (!enableRotation()) {
            setRequestedOrientation(Configuration.ORIENTATION_PORTRAIT);
        }
        // SPRD: 514951 Launcher permission dialog popup several times
        checkPermission();
    }

    protected void init() {
        setContentView(R.layout.wallpaper_cropper);

        mCropView = (CropView) findViewById(R.id.cropView);
        mProgressView = findViewById(R.id.loading);

        Intent cropIntent = getIntent();
        final Uri imageUri = cropIntent.getData();

        if (imageUri == null) {
            Log.e(LOGTAG, "No URI passed in intent, exiting WallpaperCropActivity");
            finish();
            return;
        }

        // Action bar
        // Show the custom action bar view
        final ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.actionbar_set_wallpaper);
        actionBar.getCustomView().setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean finishActivityWhenDone = true;
                        /* xihe, 20160706, lock screen wallpaper, SPCSS00295086 @{ */
                        if (LOCKSCREEN_WALLPAPER_SUPPORT) {
                            Dialog dialog = new AlertDialog.Builder(WallpaperCropActivity.this).setTitle(R.string.wallpaper_instructions)
                                .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO Auto-generated method stub
                                        int[] target = getResources().getIntArray(R.array.wallpaper_settings_values);
                                        cropImageAndSetWallpaper(imageUri, null, true, target[mTarget]);
                                    }
                                })
                                .setSingleChoiceItems(getResources().getStringArray(R.array.wallpaper_settings_title), 0,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // TODO Auto-generated method stub
                                            mTarget = which;
                                        }
                                })
                                .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO Auto-generated method stub
                                        if (mSetWallpaperButton != null) {
                                            mSetWallpaperButton.setEnabled(true);
                                        }
                                    }
                                }).create();
                            dialog.show();
                        } else {
                            cropImageAndSetWallpaper(imageUri, null, finishActivityWhenDone, WALLPAPER_TARGET_HOME);
                        }
                        /* @} */
                    }
                });
        mSetWallpaperButton = findViewById(R.id.set_wallpaper_button);

        // Load image in background
        final BitmapRegionTileSource.UriBitmapSource bitmapSource =
                new BitmapRegionTileSource.UriBitmapSource(getContext(), imageUri);
        mSetWallpaperButton.setEnabled(false);
        Runnable onLoad = new Runnable() {
            public void run() {
                if (bitmapSource.getLoadingState() != BitmapSource.State.LOADED) {
                    Toast.makeText(getContext(), R.string.wallpaper_load_fail,
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    mSetWallpaperButton.setEnabled(true);
                }
            }
        };
        setCropViewTileSource(bitmapSource, true, false, null, onLoad);
    }

    @Override
    public void onDestroy() {
        if (mCropView != null) {
            mCropView.destroy();
        }
        if (mLoaderThread != null) {
            mLoaderThread.quit();
        }
        super.onDestroy();

    }

    /**
     * This is called on {@link #mLoaderThread}
     */
    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_LOAD_IMAGE) {
            final LoadRequest req = (LoadRequest) msg.obj;
            try {
                req.src.loadInBackground(new InBitmapProvider() {

                    @Override
                    public Bitmap forPixelCount(int count) {
                        Bitmap bitmapToReuse = null;
                        // Find the smallest bitmap that satisfies the pixel count limit
                        synchronized (mReusableBitmaps) {
                            int currentBitmapSize = Integer.MAX_VALUE;
                            for (Bitmap b : mReusableBitmaps) {
                                int bitmapSize = b.getWidth() * b.getHeight();
                                if ((bitmapSize >= count) && (bitmapSize < currentBitmapSize)) {
                                    bitmapToReuse = b;
                                    currentBitmapSize = bitmapSize;
                                }
                            }

                            if (bitmapToReuse != null) {
                                mReusableBitmaps.remove(bitmapToReuse);
                            }
                        }
                        return bitmapToReuse;
                    }
                });
            } catch (SecurityException securityException) {
                if (isActivityDestroyed()) {
                    // Temporarily granted permissions are revoked when the activity
                    // finishes, potentially resulting in a SecurityException here.
                    // Even though {@link #isDestroyed} might also return true in different
                    // situations where the configuration changes, we are fine with
                    // catching these cases here as well.
                    return true;
                } else {
                    // otherwise it had a different cause and we throw it further
                    /* Sprd: bug 539786,Launcher crash @{ */
                    //throw securityException;
                    Log.e(LOGTAG, securityException.toString());
                    if (isResumed()) {
                        finish();
                    }
                    /* @} */
                }
            }

            req.result = new BitmapRegionTileSource(getContext(), req.src, mTempStorageForDecoding);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (req == mCurrentLoadRequest) {
                        onLoadRequestComplete(req,
                                req.src.getLoadingState() == BitmapSource.State.LOADED);
                    } else {
                        addReusableBitmap(req.result);
                    }
                }
            });
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected boolean isActivityDestroyed() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                && isDestroyed();
    }

    @Thunk void addReusableBitmap(TileSource src) {
        synchronized (mReusableBitmaps) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                    && src instanceof BitmapRegionTileSource) {
                Bitmap preview = ((BitmapRegionTileSource) src).getBitmap();
                if (preview != null && preview.isMutable()) {
                    mReusableBitmaps.add(preview);
                }
            }
        }
    }

    protected void onLoadRequestComplete(LoadRequest req, boolean success) {
        mCurrentLoadRequest = null;
        if (success) {
            TileSource oldSrc = mCropView.getTileSource();
            mCropView.setTileSource(req.result, null);
            mCropView.setTouchEnabled(req.touchEnabled);
            if (req.moveToLeft) {
                mCropView.moveToLeft();
            }
            if (req.scaleProvider != null) {
                mCropView.setScale(req.scaleProvider.getScale(req.result));
            }

            // Free last image
            if (oldSrc != null) {
                // Call yield instead of recycle, as we only want to free GL resource.
                // We can still reuse the bitmap for decoding any other image.
                /* SPRD: Bug 492417 The wallpaperpicker doesn't support gif or wbmp @{ */
                if (oldSrc.getPreview() != null) {
                    oldSrc.getPreview().yield();
                }
                /* @} */
            }
            addReusableBitmap(oldSrc);
        }
        if (req.postExecute != null) {
            req.postExecute.run();
        }
        mProgressView.setVisibility(View.GONE);
    }

    public final void setCropViewTileSource(BitmapSource bitmapSource, boolean touchEnabled,
            boolean moveToLeft, CropViewScaleProvider scaleProvider, Runnable postExecute) {
        final LoadRequest req = new LoadRequest();
        req.moveToLeft = moveToLeft;
        req.src = bitmapSource;
        req.touchEnabled = touchEnabled;
        req.postExecute = postExecute;
        req.scaleProvider = scaleProvider;
        mCurrentLoadRequest = req;

        // Remove any pending requests
        mLoaderHandler.removeMessages(MSG_LOAD_IMAGE);
        Message.obtain(mLoaderHandler, MSG_LOAD_IMAGE, req).sendToTarget();

        // We don't want to show the spinner every time we load an image, because that would be
        // annoying; instead, only start showing the spinner if loading the image has taken
        // longer than 1 sec (ie 1000 ms)
        mProgressView.postDelayed(new Runnable() {
            public void run() {
                if (mCurrentLoadRequest == req) {
                    mProgressView.setVisibility(View.VISIBLE);
                }
            }
        }, 1000);
    }


    public boolean enableRotation() {
        return getResources().getBoolean(R.bool.allow_rotation);
    }

    protected void setWallpaper(Uri uri, final boolean finishActivityWhenDone, int target) { // xihe, 20160706, lock screen wallpaper, SPCSS00295086
        int rotation = BitmapUtils.getRotationFromExif(getContext(), uri);
        BitmapCropTask cropTask = new BitmapCropTask(
                getContext(), uri, null, rotation, 0, 0, true, false, null, target);
       /*  SPRD: add for bug513026 add the selection .when set wallpaper in homeScreen,
        *  if the user deletes the wallpaper in settings,the picture will not found,and crash.
        *  so add check if the picture exists @{
        */
        if (cropTask.getImageBounds() == null) {
            Toast.makeText(this, R.string.picture_not_exist, Toast.LENGTH_SHORT).show();
            Runnable onEndCrop = new Runnable() {
                public void run() {
                    updateWallpaperDimensions(0, 0);
                    if (finishActivityWhenDone) {
                        setResult(Activity.RESULT_OK);
                        finish();
                    }
                }
            };
            cropTask.setOnEndRunnable(onEndCrop);
            cropTask.setNoCrop(true);
            cropTask.execute();
            return;
        }
        /* @} */
        final Point bounds = cropTask.getImageBounds();
        Runnable onEndCrop = new Runnable() {
            public void run() {
                updateWallpaperDimensions(bounds.x, bounds.y);
                if (finishActivityWhenDone) {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }
        };
        cropTask.setOnEndRunnable(onEndCrop);
        cropTask.setNoCrop(true);
        cropTask.execute();
    }

    protected void cropImageAndSetWallpaper(
            Resources res, int resId, final boolean finishActivityWhenDone, int target) { // xihe, 20160706, lock screen wallpaper, SPCSS00295086
        // crop this image and scale it down to the default wallpaper size for
        // this device
        int rotation = BitmapUtils.getRotationFromExif(res, resId);
        Point inSize = mCropView.getSourceDimensions();
        Point outSize = WallpaperUtils.getDefaultWallpaperSize(getResources(),
                getWindowManager());
        RectF crop = Utils.getMaxCropRect(
                inSize.x, inSize.y, outSize.x, outSize.y, false);
        Runnable onEndCrop = new Runnable() {
            public void run() {
                // Passing 0, 0 will cause launcher to revert to using the
                // default wallpaper size
                updateWallpaperDimensions(0, 0);
                if (finishActivityWhenDone) {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }
        };
        BitmapCropTask cropTask = new BitmapCropTask(getContext(), res, resId,
                crop, rotation, outSize.x, outSize.y, true, false, onEndCrop, target); // xihe, 20160706, lock screen wallpaper, SPCSS00295086
        cropTask.execute();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void cropImageAndSetWallpaper(Uri uri,
            BitmapCropTask.OnBitmapCroppedHandler onBitmapCroppedHandler, final boolean finishActivityWhenDone, int target) { // xihe, 20160706, lock screen wallpaper, SPCSS00295086
        boolean centerCrop = getResources().getBoolean(R.bool.center_crop);
        // Get the crop
        boolean ltr = mCropView.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;

        Display d = getWindowManager().getDefaultDisplay();

        Point displaySize = new Point();
        d.getSize(displaySize);
        boolean isPortrait = displaySize.x < displaySize.y;

        Point defaultWallpaperSize = WallpaperUtils.getDefaultWallpaperSize(getResources(),
                getWindowManager());
        // Get the crop
        RectF cropRect = mCropView.getCrop();

        Point inSize = mCropView.getSourceDimensions();

        int cropRotation = mCropView.getImageRotation();
        float cropScale = mCropView.getWidth() / (float) cropRect.width();


        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(cropRotation);
        float[] rotatedInSize = new float[] { inSize.x, inSize.y };
        rotateMatrix.mapPoints(rotatedInSize);
        rotatedInSize[0] = Math.abs(rotatedInSize[0]);
        rotatedInSize[1] = Math.abs(rotatedInSize[1]);


        // due to rounding errors in the cropview renderer the edges can be slightly offset
        // therefore we ensure that the boundaries are sanely defined
        cropRect.left = Math.max(0, cropRect.left);
        cropRect.right = Math.min(rotatedInSize[0], cropRect.right);
        cropRect.top = Math.max(0, cropRect.top);
        cropRect.bottom = Math.min(rotatedInSize[1], cropRect.bottom);

        // ADJUST CROP WIDTH
        // Extend the crop all the way to the right, for parallax
        // (or all the way to the left, in RTL)
        float extraSpace;
        if (centerCrop) {
            extraSpace = 2f * Math.min(rotatedInSize[0] - cropRect.right, cropRect.left);
        } else {
            extraSpace = ltr ? rotatedInSize[0] - cropRect.right : cropRect.left;
        }
        // Cap the amount of extra width
        float maxExtraSpace = defaultWallpaperSize.x / cropScale - cropRect.width();
        extraSpace = Math.min(extraSpace, maxExtraSpace);

        if (centerCrop) {
            cropRect.left -= extraSpace / 2f;
            cropRect.right += extraSpace / 2f;
        } else {
            if (ltr) {
                cropRect.right += extraSpace;
            } else {
                cropRect.left -= extraSpace;
            }
        }

        // ADJUST CROP HEIGHT
        if (isPortrait) {
            cropRect.bottom = cropRect.top + defaultWallpaperSize.y / cropScale;
        } else { // LANDSCAPE
            float extraPortraitHeight =
                    defaultWallpaperSize.y / cropScale - cropRect.height();
            float expandHeight =
                    Math.min(Math.min(rotatedInSize[1] - cropRect.bottom, cropRect.top),
                            extraPortraitHeight / 2);
            cropRect.top -= expandHeight;
            cropRect.bottom += expandHeight;
        }
        final int outWidth = (int) Math.round(cropRect.width() * cropScale);
        final int outHeight = (int) Math.round(cropRect.height() * cropScale);

        Runnable onEndCrop = new Runnable() {
            public void run() {
                updateWallpaperDimensions(outWidth, outHeight);
                if (finishActivityWhenDone) {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }
        };
        BitmapCropTask cropTask = new BitmapCropTask(getContext(), uri,
                cropRect, cropRotation, outWidth, outHeight, true, false, onEndCrop, target); // xihe, 20160706, lock screen wallpaper, SPCSS00295086
        if (onBitmapCroppedHandler != null) {
            cropTask.setOnBitmapCropped(onBitmapCroppedHandler);
        }
        cropTask.execute();
    }

    protected void updateWallpaperDimensions(int width, int height) {
        String spKey = LauncherFiles.WALLPAPER_CROP_PREFERENCES_KEY;
        SharedPreferences sp = getContext().getSharedPreferences(spKey, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sp.edit();
        if (width != 0 && height != 0) {
            editor.putInt(WALLPAPER_WIDTH_KEY, width);
            editor.putInt(WALLPAPER_HEIGHT_KEY, height);
        } else {
            editor.remove(WALLPAPER_WIDTH_KEY);
            editor.remove(WALLPAPER_HEIGHT_KEY);
        }
        editor.commit();
        WallpaperUtils.suggestWallpaperDimension(getResources(),
                sp, getWindowManager(), WallpaperManager.getInstance(getContext()), true);
    }

    static class LoadRequest {
        BitmapSource src;
        boolean touchEnabled;
        boolean moveToLeft;
        Runnable postExecute;
        CropViewScaleProvider scaleProvider;

        TileSource result;
    }

    interface CropViewScaleProvider {
        float getScale(TileSource src);
    }

    /* SPRD:bug 514951 Launcher permission dialog popup several times @{ */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        outState.putBoolean(IS_REQUEST_PERMISSIONS, mIsRequestPermissions);
        super.onSaveInstanceState(outState);
    }
    /* @} */
    /* SPRD: 507809 check storage permission @{ */
    private void checkPermission() {
        if (checkSelfPermission(READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            init();
        } else {
            /* SPRD:bug 514951 Launcher permission dialog popup several times @{ */
            if (!mIsRequestPermissions) {
                requestPermissions(new String[] {READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                mIsRequestPermissions = true;
            }
            /* @} */
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        boolean isPermitted = true;
        switch (requestCode) {
        case PERMISSION_REQUEST_READ_EXTERNAL_STORAGE:
            if (grantResults.length > 0) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    isPermitted = false;
                    Toast.makeText(getApplicationContext(), R.string.error_permissions, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                }
                if (isPermitted)
                    init();
            } else {
                Toast.makeText(getApplicationContext(), R.string.error_permissions, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        default:
            break;
        }
    }
    /* @} */
}
