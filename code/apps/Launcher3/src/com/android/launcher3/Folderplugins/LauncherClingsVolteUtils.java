/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.launcher3.Folderplugins;

import com.android.launcher3.*;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ActivityManager;
import android.app.AddonManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.accessibility.AccessibilityManager;

public class LauncherClingsVolteUtils implements OnClickListener {
    public static final String VOLTE_MIGRATION_CLING_DISMISSED_KEY = "volte_cling_gel.migration.dismissed";
    public static final String VOLTE_WORKSPACE_CLING_DISMISSED_KEY = "volte_cling_gel.workspace.dismissed";
    public static final String SHARED_PREFERENCES_VOLTE_KEY = "com.android.launcher3.volteprefs";

    public static final String TAG_CROP_TOP_AND_SIDES = "crop_bg_top_and_sides";

    public static final boolean DISABLE_CLINGS = false;

    public static final int SHOW_CLING_DURATION = 250;
    public static final int DISMISS_CLING_DURATION = 200;


    // New Secure Setting in L
    public static final String SKIP_FIRST_USE_HINTS = "skip_first_use_hints";

    protected Launcher mLauncher;
    protected LayoutInflater mInflater;
    protected SharedPreferences mSharedPrefs;

    public static LauncherClingsVolteUtils mInstance;


    public static LauncherClingsVolteUtils getInstance() {

        if (mInstance != null) {
            return mInstance;
        }
        mInstance = (LauncherClingsVolteUtils) AddonManager.getDefault().getAddon(
                R.string.launcher_clings_volte_utils_addon, LauncherClingsVolteUtils.class);

        return mInstance;
    }

    /** Ctor */
    public void LauncherClingsVolteUtils(Launcher launcher) {

    }

    public void setLauncher(Launcher launcher) {

    }

    public LauncherClingsVolteUtils() {

    }

    @Override
    public void onClick(View v) {

    }

    /**
     * Shows the migration cling.
     * 
     * This flow is mutually exclusive with showFirstRunCling, and only runs if
     * this Launcher package was not preinstalled and there exists a db to
     * migrate from.
     */
    public void showLongPressCling(boolean showWelcome) {

    }

    /** Hides the specified Cling */
    public void dismissCling(final View cling, final Runnable postAnimationCb, final String flag, int duration) {
        // To catch cases where siblings of top-level views are made invisible,
        // just check whether
        // the cling is directly set to GONE before dismissing it.

    }

    public boolean shouldShowFirstRunOrMigrationClings() {
        return false;
    }

    public static void synchonouslyMarkFirstRunClingDismissed(Context ctx) {

    }
}
