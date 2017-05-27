package com.sprd.camera.storagepath;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.provider.Settings;
import android.app.Application;

import com.android.camera2.R;
import android.content.Context;
import com.android.camera.app.CameraServices;
import com.android.camera.data.FilmstripItemData;

import android.util.Log;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsManager;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.SystemProperties;//SUN:jicong.wang add for customer storage path name 

/*Sprd added: functions with Storage path, add Image, and so on.
 *
 */

public class StorageUtil {
    private static final String TAG = "StorageUtil";
	/*SUN:jicong.wang add modify for customer storage path name start {@ */
	//private static final String DEFAULT_DIR = "/DCIM/Camera";
    private static final String DEFAULT_DIR = "/DCIM/"+SystemProperties.get("ro.SUN_CAMERA_SAVE_FOLDER_NAME","Camera");
	/*SUN:jicong.wang add modify for customer storage path name end @ } */
    private static StorageUtil mInstance;
    public static final String DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    public static final String DIRECTORY = DCIM + "/Camera";
    //public static final String INTERNALDIR = Environment
    //        .getInternalStoragePath().toString() + DEFAULT_DIR;
    //public static final String EXTERNALDIR = Environment
    //        .getExternalStoragePath().toString() + DEFAULT_DIR;
    private static final int VAL_DEFAULT_ROOT_DIRECTORY_SIZE = 2;
    public static final String KEY_DEFAULT_INTERNAL = "Internal";
    public static final String KEY_DEFAULT_EXTERNAL = "External";
    public static final String JPEG_POSTFIX = ".jpg";
    public static final String GIF_POSTFIX = ".gif";
    /* SPRD:fix bug 494188 No SD card, and can not open Camera and show error message */
    public static String INTERNALDIR = getStoragePathState(KEY_DEFAULT_INTERNAL) ?
            Environment.getInternalStoragePath().toString() + DEFAULT_DIR : null;
    public static String EXTERNALDIR = getStoragePathState(KEY_DEFAULT_EXTERNAL) ?
            Environment.getExternalStoragePath().toString() + DEFAULT_DIR : null;

    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;

    private static final String STORAGE_DEVICE = "storate_device";
    private static final String STORAGE_SDCARD = "storate_sdcard";
    private static final String DEFAULT_STORAGE_LOCATION = "persist.sys.storageLocation";

    private CameraServices mServices;
    private String mStorage;

    public static synchronized StorageUtil getInstance() {
        if (mInstance == null) {
            mInstance = new StorageUtil();
        }
        return mInstance;
    }

    public void initialize(CameraServices service) {
        mServices = service;
    }

    private String getGlobalStorageLocation(){
        String selectedLocation = SystemProperties.get(DEFAULT_STORAGE_LOCATION, STORAGE_DEVICE);
        if(selectedLocation.equals(STORAGE_SDCARD)){
            if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStoragePathState())){
                SystemProperties.set(DEFAULT_STORAGE_LOCATION, STORAGE_DEVICE);
                return STORAGE_DEVICE;
            }
        }
        return selectedLocation;
    }

    /*private SUN:jicong.wang modify for bug 50746*/public String getCurrentStorage() {
        String globalStorageLocation = getGlobalStorageLocation();
        if(globalStorageLocation.equals(STORAGE_DEVICE)){
            return KEY_DEFAULT_INTERNAL;
        } else if(globalStorageLocation.equals(STORAGE_SDCARD)){
            return KEY_DEFAULT_EXTERNAL;
        } else {
            return null;
        }
	}

    public long getAvailableSpace() {
        String path = null;
        String state = null;
        path = getFileDir();
        Map<String, String> roots = supportedRootDirectory();
        String internal = roots.get(KEY_DEFAULT_INTERNAL);
        String external = roots.get(KEY_DEFAULT_EXTERNAL);
        if (internal == null && external == null) {
            forceUpdateStorageSetting("");
            return UNAVAILABLE;
        //} else if (path != null && external == null && path.equals(EXTERNALDIR)) {
        } else if (path != null && external == null) {
            forceUpdateStorageSetting(KEY_DEFAULT_INTERNAL);
            path = getFileDir();
        } else if (path != null && external != null && !isStorageSetting()) {//bug521124 there is no edited photo
            forceUpdateStorageSetting(KEY_DEFAULT_EXTERNAL);
            path = getFileDir();
        } else if (external != null && internal == null) {
            forceUpdateStorageSetting(KEY_DEFAULT_EXTERNAL);
            path = getFileDir();
        } else if (path != null && internal != null && path.contains(internal)) {
            state = Environment.getInternalStoragePathState();
        } else if (path != null && external != null && path.contains(external)) {
            state = Environment.getExternalStoragePathState();
        }
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        File dir = new File(path);
        dir.mkdirs();

        /*Bug 549528 insert SD card with memory space is insufficient. @{ */
        if (dir.exists()&&(!dir.isDirectory() || !dir.canWrite())) {
            return UNAVAILABLE;
        }
        try {
            StatFs stat = new StatFs(path.replace(DEFAULT_DIR,"")); /*Bug 549528 @} */
            return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong());
        } catch (Exception e) {
            Log.i(TAG, "Fail to access storage", e);
        }
        return UNKNOWN_SIZE;
    }

    /* SPRD:fix bug 494188 No SD card, and can not open Camera and show error message */
    public static boolean getStoragePathState(String storage) {
        Log.i(TAG, "getStoragePathState");
        return KEY_DEFAULT_EXTERNAL.equals(storage) ?
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStoragePathState()) :
                Environment.MEDIA_MOUNTED.equals(Environment.getInternalStoragePathState());
    }

    public static Map<String, String> supportedRootDirectory() {
        Map<String, String> result = null;
        String internal_state = Environment.getInternalStoragePathState();
        String external_state = Environment.getExternalStoragePathState();
        boolean internal_mounted = (Environment.MEDIA_MOUNTED
                .equals(internal_state));
        boolean external_mounted = (Environment.MEDIA_MOUNTED
                .equals(external_state));
        String internal = (internal_mounted ? Environment
                .getInternalStoragePath().getAbsolutePath() : null), external = (external_mounted ? Environment
                .getExternalStoragePath().getAbsolutePath() : null);
        result = new HashMap<String, String>(VAL_DEFAULT_ROOT_DIRECTORY_SIZE);
        result.put(KEY_DEFAULT_INTERNAL, internal);
        result.put(KEY_DEFAULT_EXTERNAL, external);
        return result;
    }

    public String getFileDir() {
        Log.i(TAG, "getFileDir");
        String currentStorage = getCurrentStorage();
        if (KEY_DEFAULT_INTERNAL.equals(currentStorage)) {
            /* SPRD:fix bug 494188 No SD card, and can not open Camera and show error message */
            // return INTERNALDIR;
            return getStoragePathState(KEY_DEFAULT_INTERNAL) ?
                    Environment.getInternalStoragePath().toString() + DEFAULT_DIR : null;
        } else if (KEY_DEFAULT_EXTERNAL.equals(currentStorage)) {
            /* SPRD:fix bug 494188 No SD card, and can not open Camera and show error message */
            // return EXTERNALDIR;
            return getStoragePathState(KEY_DEFAULT_EXTERNAL) ?
                    Environment.getExternalStoragePath().toString() + DEFAULT_DIR :
                    getStoragePathState(KEY_DEFAULT_INTERNAL) ?
                    Environment.getInternalStoragePath().toString() + DEFAULT_DIR : null;
        } else {
            /* SPRD:fix bug 494188 No SD card, and can not open Camera and show error message */
            // return EXTERNALDIR;
            if (getStoragePathState(KEY_DEFAULT_EXTERNAL)) {
                forceUpdateStorageSetting(KEY_DEFAULT_EXTERNAL);
                return Environment.getExternalStoragePath().toString() + DEFAULT_DIR ;
            } else if (getStoragePathState(KEY_DEFAULT_INTERNAL)) {
                forceUpdateStorageSetting(KEY_DEFAULT_INTERNAL);
                return Environment.getInternalStoragePath().toString() + DEFAULT_DIR;
            } else {
                return null;
            }
        }
    }

    public String generateFilePath(String title, String mimeType) {
        String extension = null;
        if (FilmstripItemData.MIME_TYPE_JPEG.equals(mimeType)) {
            extension = JPEG_POSTFIX;
        } else if (FilmstripItemData.MIME_TYPE_GIF.equals(mimeType)) {
            extension = GIF_POSTFIX;
        } else {
            throw new IllegalArgumentException("Invalid mimeType: " + mimeType);
        }
        Log.i(TAG, "For_Test generateFilePath getFileDir() = "
                + getFileDir() + " title = " + title
                + "path = " + (new File(getFileDir(), title + extension)).getAbsolutePath());
        return (new File(getFileDir(), title + extension)).getAbsolutePath();
    }

    public void updateStorage() {
        String storage = getCurrentStorage();
        mStorage = storage;
    }

    public boolean isStorageUpdated() {
        if (mStorage == null) {
            mStorage = getCurrentStorage();
            return true;
        }
        return (!mStorage.equals(getCurrentStorage()));
    }

    public void forceUpdateStorageSetting(String storage) {
        if (mServices != null) {
            SettingsManager settingsManager = mServices.getSettingsManager();
            settingsManager.set(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_STORAGE_PATH, storage);
        }
    }

    public static synchronized final String getImageBucketId(String filePath) {
        return String.valueOf(filePath.toLowerCase().hashCode());
    }

    public static void deleteImage(ContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
        } catch (Throwable th) {
            Log.i(TAG, "Failed to delete image: " + uri);
        }
    }

    /*SPRD:fix bug521124 there is no edited photo*/
    private boolean isStorageSetting(){
        if (mServices != null) {
            SettingsManager settingsManager = mServices.getSettingsManager();
            return settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_STORAGE_PATH);
        }
        return false;
    }

    //SPRD:fix bug537451 pull sd card, edit and puzzle can not work.
    public String getStorageState() {
        String state = Environment.MEDIA_UNMOUNTED;
        /*SPRD: Fix bug 540799 @{ */
        String currentStorage = getCurrentStorage();
        if (currentStorage == null) return state;
        /* @} */
        switch(currentStorage){
            case KEY_DEFAULT_INTERNAL:
                state = Environment.getInternalStoragePathState();
                break;
            case KEY_DEFAULT_EXTERNAL:
                state = Environment.getExternalStoragePathState();
                break;
        }
        return state;
    }
}