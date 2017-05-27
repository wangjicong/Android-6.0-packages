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

public class LauncherClingsVolte implements OnClickListener {
    private static final String VOLTE_MIGRATION_CLING_DISMISSED_KEY = "volte_cling_gel.migration.dismissed";
    private static final String VOLTE_WORKSPACE_CLING_DISMISSED_KEY = "volte_cling_gel.workspace.dismissed";
    private static final String SHARED_PREFERENCES_VOLTE_KEY = "com.android.launcher3.volteprefs";

    private static final String TAG_CROP_TOP_AND_SIDES = "crop_bg_top_and_sides";

    private static final boolean DISABLE_CLINGS = false;

    private static final int SHOW_CLING_DURATION = 250;
    private static final int DISMISS_CLING_DURATION = 200;


    // New Secure Setting in L
    private static final String SKIP_FIRST_USE_HINTS = "skip_first_use_hints";

    private Launcher mLauncher;
    private LayoutInflater mInflater;
    private SharedPreferences mSharedPrefs;


    /** Ctor */
    public LauncherClingsVolte(Launcher launcher) {
        LauncherClingsVolteUtils.getInstance().LauncherClingsVolteUtils(launcher);
    }

    @Override
    public void onClick(View v) {
        LauncherClingsVolteUtils.getInstance().onClick(v);
    }

    /**
     * Shows the migration cling.
     * 
     * This flow is mutually exclusive with showFirstRunCling, and only runs if
     * this Launcher package was not preinstalled and there exists a db to
     * migrate from.
     */
    public void showLongPressCling(boolean showWelcome) {
        LauncherClingsVolteUtils.getInstance().showLongPressCling(showWelcome);
    }

    public boolean shouldShowFirstRunOrMigrationClings() {
        return LauncherClingsVolteUtils.getInstance().shouldShowFirstRunOrMigrationClings();
    }

    public static void synchonouslyMarkFirstRunClingDismissed(Context ctx) {
        LauncherClingsVolteUtils.getInstance().synchonouslyMarkFirstRunClingDismissed(ctx);
    }
}
