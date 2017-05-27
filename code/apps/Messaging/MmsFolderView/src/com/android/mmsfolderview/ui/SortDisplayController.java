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

package com.android.mmsfolderview.ui;

import android.content.Context;
import android.util.Log;

import com.android.mmsfolderview.util.SystemProperties;

public class SortDisplayController {
    private static SortDisplayController mInstance = null;

    private int mSortType;
    public final static int FOLDER_VIEW = 1;
    public final static int TIME_QUANTUM = 2;
    public final static int SIM_SMS = 3;

    private static final String TAG = "SortDisplayController";

    private SortDisplayController() {
    }

    public static synchronized SortDisplayController getInstance() {
        if (mInstance == null) {
            mInstance = new SortDisplayController();
        }
        return mInstance;
    }

    public void setGlobalSortType(int type) {
        mSortType = type;
        Log.d(TAG, "Global Sort = " + mSortType);
    }

    public int getGlobalSortType() {
        return mSortType;
    }

    public boolean isFolderView() {
        return (mSortType == FOLDER_VIEW);
    }



    public boolean isTimeQuqntum() {
        return (mSortType == TIME_QUANTUM);
    }


    public boolean isSimSms() {
        return (mSortType == SIM_SMS);
    }

    public static boolean isCMCC() {
        return "cmcc".equalsIgnoreCase(SystemProperties.get("ro.operator")) ||
               "spec3".equalsIgnoreCase(SystemProperties.get("ro.operator.version"));
    }

    // prepare process  when Activity init from sim ,then set cbf; else set cbf = null;
    public boolean initEvn(Context ctx, Object obj) {
        if (getItf() != null) {
            return getItf().initEnv(ctx, obj);
        } else {
            return true;
        }
    }

    public void setItf(IPrepare ins){
        mIns = ins;
    }

    protected  IPrepare getItf(){
        return mIns;
    }
    private IPrepare  mIns = null;
    public interface IPrepare{
        boolean initEnv(Context ctx, Object obj);
        // telephony database  is reloader from sim;
    }
}
