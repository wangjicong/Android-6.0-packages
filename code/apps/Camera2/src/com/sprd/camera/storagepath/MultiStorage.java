package com.sprd.camera.storagepath;

import java.util.ArrayList;
import java.util.List;
import android.os.Environment;
import android.util.Log;

/*
 * Sprd added: For Multi Storage External and Internal
 * Note: This File Just contains functions with devices,
 *  such as sd card, internal card. But no functions with
 *  Storage path, add Image, and so on.
 * Take Attention on Note when you Add Functions!
 */
public class MultiStorage {
    private static final String TAG = "MultiStorage";

    public static final int VAL_DEFAULT_ROOT_DIRECTORY_SIZE = 2;
    public static final String KEY_DEFAULT_INTERNAL = "Internal";
    public static final String KEY_DEFAULT_EXTERNAL = "External";

    /**
     * get current supported storage list
     *
     * @return current supported storage list:max len = 2:internal/external,min
     *         len = 1:internal
     */
    public static List<String> getSupportedStorage() {
        List<String> supportedStorage = new ArrayList<String>(
                VAL_DEFAULT_ROOT_DIRECTORY_SIZE);
        String internal_state = Environment.getInternalStoragePathState();
        String external_state = Environment.getExternalStoragePathState();
        boolean internal_mounted = (Environment.MEDIA_MOUNTED
                .equals(internal_state));
        boolean external_mounted = (Environment.MEDIA_MOUNTED
                .equals(external_state));

        if (internal_mounted) {
            Log.d(TAG, "internal storage found");
            supportedStorage.add(KEY_DEFAULT_INTERNAL);
        }
        if (external_mounted) {
            Log.d(TAG, "external storage found");
            supportedStorage.add(KEY_DEFAULT_EXTERNAL);
        }
        return supportedStorage;
    }
}