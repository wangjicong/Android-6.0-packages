/**
 * Copyright (c) 2009, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mmsfolderview.ui;

import android.app.ListActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.android.mmsfolderview.R;

/***
 * Presents a List of search results. Each item in the list represents a thread
 * which matches. The item contains the contact (or phone number) as the "title"
 * and a snippet of what matches, below. The snippet is taken from the most
 * recent part of the conversation that has a match. Each match within the
 * visible portion of the snippet is highlighted.
 */

public class SearchActivity extends BaseActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        invalidateActionBar();
    }

    @Override
    protected void confirmDeleteMessage() {
        // TODO Auto-generated method stub
        
    }

}
