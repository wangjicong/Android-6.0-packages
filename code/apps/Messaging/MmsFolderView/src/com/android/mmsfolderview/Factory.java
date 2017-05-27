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

package com.android.mmsfolderview;

import android.content.Context;

import com.google.common.annotations.VisibleForTesting;

public abstract class Factory {

    // Making this volatile because on the unit tests, setInstance is called from a unit test
    // thread, and then it's read on the UI thread.
    private static volatile Factory sInstance;
    @VisibleForTesting
    protected static boolean sRegistered;
    @VisibleForTesting
    protected static boolean sInitialized;

    public static Factory get() {
        return sInstance;
    }

    protected static void setInstance(final Factory factory) {
        // Not allowed to call this after real application initialization is complete
//        Assert.isTrue(!sRegistered);
//        Assert.isTrue(!sInitialized);
        sInstance = factory;
    }
    public abstract void onRequiredPermissionsAcquired();

    public abstract Context getApplicationContext();

    public abstract void onActivityResume();
}
