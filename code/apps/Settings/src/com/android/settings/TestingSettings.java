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

package com.android.settings;

import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class TestingSettings extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.testing_settings);
    }

    /* SPRD : Modify for bug492457 @{ */
    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            preferenceScreen.setEnabled(false);
            Toast.makeText(getApplicationContext(),
                    R.string.user_add_profile_item_title,Toast.LENGTH_LONG).show();
        }
        super.setPreferenceScreen(preferenceScreen);
    }
    /* @} */

}
