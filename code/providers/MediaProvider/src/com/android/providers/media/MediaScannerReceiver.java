/* //device/content/providers/media/src/com/android/providers/media/MediaScannerReceiver.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class MediaScannerReceiver extends BroadcastReceiver {
    private final static String TAG = "MediaScannerReceiver";
    /* SPRD: add @{ */
    private final String internalStoragePath = Environment.getInternalStoragePath().getPath();//SPRD:Add for double-T card
    //private final String externalStoragePath = Environment.getExternalStoragePath().getPath();//SPRD:Add for double-T card
    /* @} */

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final Uri uri = intent.getData();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Scan both internal and external storage
            scan(context, MediaProvider.INTERNAL_VOLUME, null);
            // SPRD: scan the inner storage
            scan(context, MediaProvider.EXTERNAL_VOLUME, internalStoragePath);

        } else {
            if (uri != null && "file".equals(uri.getScheme())){
                // handle intents related to external storage
                String path = uri.getPath();
                /* SPRD: getExternalStorageDirectory returns "/storage/emulated/0" in original design.
                 * Now in our platform, getExternalStorageDirectory returns different value if SD
                 * card is taken as main card.
                 * move it to global scope.
                 */
                //String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
                String externalStoragePath = Environment.getExternalStoragePath() == null ?
                        Environment.getExternalStorageDirectory().getPath() : Environment
                                .getExternalStoragePath().getPath();// SPRD:Add for double-T card
                String legacyPath = Environment.getLegacyExternalStorageDirectory().getPath();

                try {
                    path = new File(path).getCanonicalPath();
                } catch (IOException e) {
                    Log.e(TAG, "couldn't canonicalize " + path);
                    return;
                }
                if (path.startsWith(legacyPath)) {
                    path = externalStoragePath + path.substring(legacyPath.length());
                }

                Log.d(TAG, "action: " + action + " path: " + path);
                if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                    // SPRD: sometimes ActivityManager do not deliver Intent.ACTION_MEDIA_MOUNTED to
                    // dynamic BroadcastReceiver registered in MediaProvider.java, in which we want to excute
                    // setPathType() to set mPathType, so we do the wanted operation here.
                    MediaProvider.setPathType();

                    // scan whenever any volume is mounted
                    /* SPRD: we scan directory with path @{ */
                    //scan(context, MediaProvider.EXTERNAL_VOLUME);
                    scan(context, MediaProvider.EXTERNAL_VOLUME, path);
                    /* @} */

                    /* SPRD: used by FileExplorer to scan directory @{ */
                    } else if (action.equals("android.intent.action.MEDIA_SCANNER_SCAN_DIR")) {
                        boolean result = new File(path).isDirectory();
                        if (result) {
                            scan(context, MediaProvider.EXTERNAL_VOLUME, path);
                        } else {
                            Log.d(TAG,"invalid directory path: " + path);
                        }
                    /* @} */
                } else if (Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action) &&
                        path != null && (path.startsWith(externalStoragePath + "/")
                                || path.startsWith(internalStoragePath + "/"))) {
                    scanFile(context, path);
                }
            }
        }
    }

    private void scan(Context context, String volume, String path) {
        Log.d(TAG, "scan " + volume + " volume path: " + (null == path
                ? (Environment.getRootDirectory() + "/media") : path));
        Bundle args = new Bundle();
        args.putString("volume", volume);
        // SPRD: Add
        args.putString("path", path);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }    

    private void scanFile(Context context, String path) {
        Log.d(TAG,"scan single file " + path);
        Bundle args = new Bundle();
        args.putString("filepath", path);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }    
}
