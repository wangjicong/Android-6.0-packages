/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.browser;

import android.content.Context;
import android.util.Log;
import android.os.Environment;
import java.io.File;

public class StorageUtils {

    public static final int STORAGE_PRIMARY_EXTERNAL = 1;
    public static final int STORAGE_PRIMARY_INTERNAL = 2;

    //android original API
    public static File getExternalStorageDirectory(){
        return Environment.getExternalStorageDirectory();
    }

    public static String getExternalStorageState() {
        return Environment.getExternalStorageState();
    }
    //SPRD add API
    public static File getInternalStoragePath(){
        return Environment.getInternalStoragePath();
    }

    public static String getInternalStoragePathState(){
        return Environment.getInternalStoragePathState();
    }

    public static File getExternalStoragePath(){
        return Environment.getExternalStoragePath();
    }

    public static String getExternalStoragePathState(){
        return Environment.getExternalStoragePathState();
    }

    // 1 --- external is primary
    // 2 --- internal is primary
    public static int getStorageType(){
        return Environment.getStorageType();
    }

    public static String getDefaultStoragePath(){
        return getInternalStoragePath() + "/Download";
    }

    public static boolean isExternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(getExternalStoragePathState());
    }

    public static boolean isInternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(getInternalStoragePathState());
    }

    public static boolean checkStoragePathAvailable(String path){
        if (path != null && path.startsWith(getExternalStoragePath().getPath())){
            if(Environment.MEDIA_MOUNTED.equals(getExternalStoragePathState())){
                Log.i("StorageUtils", "checkStoragePathAvailable, is ExternalStorage path");
                return true;
            }
        } else if (path != null && path.startsWith(getInternalStoragePath().getPath())){
            if(Environment.MEDIA_MOUNTED.equals(getInternalStoragePathState())){
                Log.i("StorageUtils", "checkStoragePathAvailable, is InternalStorage path");
                return true;
            }
        }
        Log.i("StorageUtils", "checkStoragePathAvailable, not valid path");
        return false;
    }

/*for storage function not ready
    //SPRD add API
    public static File getInternalStoragePath(){
        return Environment.getExternalStorageDirectory();
    }

    public static String getInternalStoragePathState(){
        return Environment.getExternalStorageState();
    }

    public static File getExternalStoragePath(){
        return Environment.getExternalStorageDirectory();
    }

    public static String getExternalStoragePathState(){
        return Environment.getExternalStorageState();
    }

    // 1 --- external is primary
    // 2 --- internal is primary
    public static int getStorageType(){
        return 2;
    }

    public static String getDefaultStoragePath(){
        return getInternalStoragePath() + "/Download";
    }

    public static boolean isExternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(getExternalStoragePathState());
    }

    public static boolean isInternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(getInternalStoragePathState());
    }

    public static boolean checkStoragePathAvailable(String path){
        if (path != null && path.startsWith(getExternalStoragePath().getPath())){
            if(Environment.MEDIA_MOUNTED.equals(getExternalStoragePathState())){
                return true;
            }
        }else{
            return true;
        }
        return false;
    }
*/
}

