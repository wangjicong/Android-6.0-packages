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

package com.android.messaging.ui.conversationlist;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.telephony.SubscriptionInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.app.ActivityManager;
import android.app.Activity;
import android.util.Log;

import com.android.messaging.R;
import com.android.messaging.sms.MmsConfig;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.DebugUtils;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.Trace;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.FdnUtil;
import com.android.messaging.ui.conversation.ConversationActivity;

// bug 478514: Add for MmsFolderView Feature -- Begin
import android.content.ComponentName;

import com.android.pluginframework.PluginsManager;
import com.android.pluginframework.info.PluginInfo;

import android.content.Intent;
import android.widget.Toast;

import com.android.messaging.ui.UIIntentsImpl;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.BuglePrefsKeys;

import java.util.List;
// bug 478514: Add for MmsFolderView Feature -- End
//bug 495194 : add for search feature begin
import android.net.Uri;
import java.lang.Long;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.widget.Toast;
import android.os.Handler;
import android.os.Message;
import android.graphics.Color;
import android.widget.SearchView;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ComponentName;
import com.android.messaging.ui.SearchActivity;
import android.widget.EditText;
import android.widget.ImageView;
import android.support.v4.view.MenuItemCompat;
import android.widget.SearchView.OnQueryTextListener;
import android.view.inputmethod.InputMethodManager;
import android.text.TextUtils;
import com.android.messaging.sms.MmsConfig;
import android.view.View;
import android.widget.SearchView.SearchAutoComplete;
//bug 495194 : add for search feature end
import com.sprd.android.config.OptConfig;//Kalyy

public class ConversationListActivity extends AbstractConversationListActivity {
    // bug 478514: Add for MmsFolderView Feature -- Begin
    private static final int MENU_FLODER_VIEW = 0x111;
    private static final String IS_FROM_FOLDER_VIEW = "from_folder_view";
	// bug 478514: Add for MmsFolderView Feature -- End
    //bug 496495 begin
    private static ConversationListActivity mLastActivity = null;
    private String TAG = "ConversationListActivity";
    //bug 496495 end
    private int mSubId;//sprd 497178
    // bug 495194 : add for search feature begin
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    // add for bug 551962 begin
    private SearchAutoComplete mSearchEditText;
    // add for bug 551962 end
    public static final int SEARCH_MAX_LENGTH = 512;
    // bug 495194 : add for search feature end

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // bug 495194 : add for search feature begin
        launchThreadFromSearchResult(getIntent());
        // bug 495194 : add for search feature end
        Trace.beginSection("ConversationListActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_list_activity);
        //bug 496495 begin
	 if(ActivityManager.isUserAMonkey()){
            Log.i(TAG, " onCreate() monkey------>> mLastActivity."+mLastActivity);
            if(mLastActivity != null){
                 mLastActivity.finish();
            }
            mLastActivity = this;
        }
        // bug 496495 end

        Trace.endSection();
        // bug 478514: Add for MmsFolderView Feature -- Begin
        if (OsUtil.hasRequiredPermissions()) {
            startFolderViewIfNeeded();
        }
        // bug 478514: Add for MmsFolderView Feature -- End
        invalidateActionBar();
    }
    // bug 495194 : add for search feature begin
    private void launchThreadFromSearchResult(Intent intent) {
        System.out.println("enter ConversationListActivity launchThreadFromSearchResult(),the Component = "
                        + getIntent().getComponent());
        if (intent.hasExtra("come_from_searchActivity")) {
            System.out.println("come from SearchActivity");
            Intent data = intent;
            if (data == null) {
                System.out.println("the data from SearchActivity Intent is null");
            }
            String threadId = Long.toString(data.getLongExtra("thread_id", -1));
            String selectId = Long.toString(data.getLongExtra("select_id", -1));
            String highlight = data.getStringExtra("highlight");
            // add for bug 543691 begin
            String convId = String.valueOf(data.getIntExtra("convId", -1));
            System.out.println("the threadId = " + threadId + ", selectId = "
                    + selectId + ", highlight = " + highlight + "convId = ["
                    + convId + "]");
            UIIntents.get().launchConversationActivity(this, convId, null,
                    null, false);
            // add for bug 543691 end
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        System.out.println("enter ConversationListActivity onNewIntent()");
        launchThreadFromSearchResult(intent);
    }

    // add for bug 543695 begin
    // add for bug 551962 begin
    private SearchAutoComplete getSearchEditText() {
        return mSearchEditText;
    }
    // add for bug 551962 end
    private void clearSearchText() {
        System.out.println("enter clearSearchText()");
        if (getSearchEditText() != null) {
            System.out.println("getSearchEditText()!=null,the SearchEditText = "
                            + getSearchEditText().getText());
            if (!TextUtils.isEmpty(getSearchEditText().getText())) {
                getSearchEditText().setText("");
            }
        }
    }
    // add for bug 543695 end
    // bug 495194 : add for search feature end

    @Override
    protected void updateActionBar(final ActionBar actionBar) {
        actionBar.setTitle(getString(R.string.app_name));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setBackgroundDrawable(new ColorDrawable(
                getResources().getColor(R.color.action_bar_background_color)));
        actionBar.show();
        super.updateActionBar(actionBar);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Invalidate the menu as items that are based on settings may have changed
        // while not in the app (e.g. Talkback enabled/disable affects new conversation
        // button)
        supportInvalidateOptionsMenu();
        // add for bug 543695 begin
        clearSearchText();
        // add for bug 543695 end

        if(!OsUtil.hasRequiredPermissions()){
            OsUtil.requestMissingPermission(this);
        }
        // sprd: 570184 stard
        new FdnContatctsWorkThread().start();
        // sprd: 570184 end
    }

    @Override
    public void onBackPressed() {
        if (isInConversationListSelectMode()) {
            exitMultiSelectState();
        } else {
            super.onBackPressed();
        }
    }
    // bug 495194 : add for search feature begin
    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(ConversationListActivity.this, SearchActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            mSearchItem.collapseActionView();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            System.out.println("onQueryTextChange()");
            /* SPRD: Add for bug#191263. @{ */
            if (newText != null && newText.length() > SEARCH_MAX_LENGTH) {
                mSearchView.setQuery(
                        newText.substring(0, SEARCH_MAX_LENGTH - 1), false);
                Toast.makeText(ConversationListActivity.this,
                        getString(R.string.search_max_length),
                        Toast.LENGTH_LONG).show();
            }
            /* @} */
            // add for bug 551962 begin
            setCloseBtnGone(true);
            // add for bug 551962 end
            return true;
        }
    };

    @Override
    public boolean onSearchRequested() {
        System.out.println("enter onSearchRequested()");
        if (mSearchItem != null) {
            mSearchItem.expandActionView();
        }
        return true;
    }
    // bug 495194 : add for search feature end

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            return true;
        }
        getMenuInflater().inflate(R.menu.conversation_list_fragment_menu, menu);
        final MenuItem item = menu.findItem(R.id.action_debug_options);
        if (item != null) {
            final boolean enableDebugItems = DebugUtils.isDebugEnabled();
            item.setVisible(enableDebugItems).setEnabled(enableDebugItems);
        }
       //sprd bug497178 begin
        MenuItem menuItem = menu.findItem(R.id.action_wireless_alerts);
        if (menuItem != null && isCellBroadcastAppLinkEnabled()) {
            menuItem.setVisible(true);
        }else{
            menuItem.setVisible(false);
        }
        if(OptConfig.SUN_C7359_C5S_FWVGA_FPT || OptConfig.SUN_CUSTOM_C7356_XT_HVGA_SUPPORT){//Kalyy
            menuItem.setVisible(false);
        }
       //sprd bug497178 end
        // bug 551962  : add for search feature begin
        initSearchView(menu);
        // bug 551962 : add for search feature end
        return true;
    }
    // bug 551962 : add for search feature begin
    public void initSearchView(Menu menu){
        // bug 495194 : add for search feature begin
        System.out.println("enter onCreateOptionsMenu(), will set search menu properties");
        mSearchItem = menu.findItem(R.id.menu_search_item);
        //add for switch of Search feature
        if (!MmsConfig.getIsCMCC()) {
            mSearchItem.setVisible(false);
        }
        //add for switch of Search feature
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        // mSearchView.setSearchTextSize(16);
        /*Delete  by SPRD for Bug:550566 Start */
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setIconified(false);
        /*Delete  by SPRD for Bug:550566 End */
        mSearchView.clearFocus();
        /* SPRD:change the background of searchView. @{ */
        mSearchView.setBackgroundResource(R.drawable.search_bg);
        /* @} */
        /* [Add] by SPRD for Bug:[437363] 2015.09.10 Start */
        int searchSrcTextId = getResources().getIdentifier(
                "android:id/search_src_text", null, null);
        // add for bug 543695 begin
        mSearchEditText = (SearchAutoComplete) (mSearchView.findViewById(searchSrcTextId));
        mSearchEditText.setPadding(0, 20, 0, 0);
        mSearchEditText.setTextColor(Color.BLACK);
        mSearchEditText.setHintTextColor(Color.GRAY);
        mSearchEditText.setTextSize(18);
        setCloseBtnGone(true);
        // add for bug 543695 end
        ImageView search_button = (ImageView) mSearchView
                .findViewById(android.support.v7.appcompat.R.id.search_button);
        mSearchView.setSubmitButtonEnabled(false);
        /* [Add] by SPRD for Bug:[437363] 2015.09.10 End */

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        if (searchManager != null) {
            System.out.println("searchManager != null");
            SearchableInfo info = searchManager.getSearchableInfo(this
                    .getComponentName());
            mSearchView.setSearchableInfo(info);
        }
        // bug 495194 : add for search feature end
    }

    public void setCloseBtnGone(boolean bool){
        int closeBtnId = getResources().getIdentifier(
                "android:id/search_close_btn", null, null);
        ImageView mCloseButton = null;
        if (mSearchView!=null) {
            mCloseButton = (ImageView) mSearchView
                    .findViewById(closeBtnId);
        }
        if (mCloseButton != null && bool) {
            System.out.println("mCloseButton!=null");
            //mCloseButton.setBackgroundResource(R.drawable.ic_cancel_small_dark);
            mCloseButton.setVisibility(View.GONE);
        }else if(mCloseButton != null && !bool){
            mCloseButton.setVisibility(View.VISIBLE);
        }
    }
    // bug 551962 : add for search feature end
    // bug 478514: Add for MmsFolderView Feature -- Begin
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        if (mActionMode != null) {
            return true;
        }
        try {
         // bug 540833:-- Begin
           // PluginsManager.initPluginsManager();
         // bug 540833:-- End
            PluginInfo pluginInfo = PluginsManager
                    .getTargetPluginInfo(UIIntentsImpl.MMS_FLODER_VIEW_PACKAGE);
            if (pluginInfo != null) {
                menu.removeItem(MENU_FLODER_VIEW);
                menu.removeItem(R.id.action_sim_sms);
                // bug 540833:-- Begin
                //  menu.add(Menu.NONE, MENU_FLODER_VIEW, Menu.FIRST,
                     //   pluginInfo.getPluginLable());
                menu.add(Menu.NONE, MENU_FLODER_VIEW, Menu.FIRST,
                        pluginInfo.getPluginLable(ConversationListActivity.this));
                // bug 540833:-- End
                if (MmsConfig.getIsCMCC()) {
                    menu.removeItem(R.id.action_sim_sms);
                    menu.add(Menu.NONE, R.id.action_sim_sms, Menu.NONE,
                            R.string.action_sim_sms);
                    if(!OsUtil.hasPhonePermission()){
                        OsUtil.requestMissingPermission(this);
                    } else {
                        if (!PhoneUtils.getDefault().hasSim()) {
                            menu.findItem(R.id.action_sim_sms).setVisible(false);
                        }
                    }
                }
            } else {
                menu.removeItem(MENU_FLODER_VIEW);
                menu.removeItem(R.id.action_sim_sms);
            }
        } catch (Exception e) {
            Log.e(TAG, "Add FolderView Menu Error", e);
        }
        return true;
    }
    // bug 478514: Add for MmsFolderView Feature -- End
    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch(menuItem.getItemId()) {
        // bug 495194 : add for search feature begin
        case R.id.menu_search_item:
            mSearchView.setFocusable(true);
            mSearchView.setFocusableInTouchMode(true);
            mSearchView.requestFocus();
            // add for bug 543695 begin
            clearSearchText();
            // add for bug 543695 end
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            boolean isOpen = imm.isActive();
            if (!isOpen) {
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
            System.out.println("click menu_search_item menu");
            return true;
            // bug 495194 : add for search feature end
            case R.id.action_start_new_conversation:
                onActionBarStartNewConversation();
                return true;
            case R.id.action_settings:
                onActionBarSettings();
                return true;
            case R.id.action_debug_options:
                onActionBarDebug();
                return true;
            case R.id.action_show_archived:
                onActionBarArchived();
                return true;
            //sprd bug497178 begin
            case R.id.action_wireless_alerts:
                 try {
                       startActivity(UIIntents.get().getWirelessAlertsIntent());
                  } catch (final ActivityNotFoundException e) {
                      // Handle so we shouldn't crash if the wireless alerts
                      // implementation is broken.
                      LogUtil.e(LogUtil.BUGLE_TAG,
                                   "Failed to launch wireless alerts activity", e);
                   }
                return true;
            //sprd bug497178 end
            case R.id.action_show_blocked_contacts:
                onActionBarBlockedParticipants();
                return true;
            // bug 478514: Add for MmsFolderView Feature -- Begin
            case MENU_FLODER_VIEW:
                try {
                    BuglePrefs.getApplicationPrefs().putInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE, BuglePrefsKeys.FOLDERVIEW_STATE);
                    Intent intent = getStartFolderViewIntent();
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(ConversationListActivity.this, R.string.no_mms_plugin,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onOptionsItemSelected: start floder view error.", e);
                }
                return true;
             // bug 478514: Add for MmsFolderView Feature -- End

            // bug 478514: Add for SimMessage Feature -- Begin
            case R.id.action_sim_sms:
                try {
                    BuglePrefs.getApplicationPrefs().putInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE, BuglePrefsKeys.SIMSMS_STATE);
                    Intent intent = getStartFolderViewIntent();
                    intent.putExtra("is_sim_sms", true);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(ConversationListActivity.this, R.string.no_mms_plugin,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onOptionsItemSelected: start floder view error.", e);
                }
                return true;
            // bug 478514: Add for SimMessage Feature -- End
        }
        return super.onOptionsItemSelected(menuItem);
    }
    // bug 478514: Add for MmsFolderView Feature -- Begin
    private void startFolderViewIfNeeded() {
        Intent intent = getIntent();
        Log.d("tim_V6_start", TAG+" startFolderViewIfNeeded");
        if (intent != null) {
            if (intent.getBooleanExtra(IS_FROM_FOLDER_VIEW, false)) {
                if (intent.getIntExtra(BuglePrefsKeys.DEST_VIEW_MODE, 0) == BuglePrefsKeys.FOLDERVIEW_STATE) {
                    BuglePrefs.getApplicationPrefs().putInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE, BuglePrefsKeys.FOLDERVIEW_STATE);
                } else if (intent.getIntExtra(BuglePrefsKeys.DEST_VIEW_MODE, 0) == BuglePrefsKeys.SIMSMS_STATE) {
                    BuglePrefs.getApplicationPrefs().putInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE, BuglePrefsKeys.SIMSMS_STATE);
                } else {
                    BuglePrefs.getApplicationPrefs().putInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE, BuglePrefsKeys.CONVERSATION_STATE);
                }
            }
        }
        int folderViewMode = BuglePrefs.getApplicationPrefs().getInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE,
                0);
        if (folderViewMode != BuglePrefsKeys.CONVERSATION_STATE) {
            try {
                Intent remoteIntent = getStartFolderViewIntent();
                if (folderViewMode == BuglePrefsKeys.SIMSMS_STATE) {
                    remoteIntent.putExtra("is_sim_sms", true);
                    BuglePrefs.getApplicationPrefs().putInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE, BuglePrefsKeys.SIMSMS_STATE);
                } else {
                    BuglePrefs.getApplicationPrefs().putInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE, BuglePrefsKeys.FOLDERVIEW_STATE);
                }
                startActivity(remoteIntent);
                finish();
            } catch (Exception e) {
                Log.e(TAG, "onCreate: start floder view error.", e);
            }
        }
    }
    private Intent getStartFolderViewIntent() {
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName(
                UIIntentsImpl.MMS_FLODER_VIEW_PACKAGE,
                UIIntentsImpl.MMS_FLODER_VIEW_MAIN_ACTIVITY);
        intent.setComponent(componentName);
        return intent;
    }
    // bug 478514: Add for MmsFolderView Feature -- End

    @Override
    public void onActionBarHome() {
        exitMultiSelectState();
    }

    public void onActionBarStartNewConversation() {
        UIIntents.get().launchCreateNewConversationActivity(this, null);
    }

    public void onActionBarSettings() {
        UIIntents.get().launchSettingsActivity(this);
    }

    public void onActionBarBlockedParticipants() {
        UIIntents.get().launchBlockedParticipantsActivity(this);
    }

    public void onActionBarArchived() {
        UIIntents.get().launchArchivedConversationsActivity(this);
    }

    //sprd bug497178 begin
    private boolean isCellBroadcastAppLinkEnabled() {
        if (!MmsConfig.get(mSubId).getShowCellBroadcast()) {
            return false;
        }
        try {
            final PackageManager pm = ConversationListActivity.this.getPackageManager();
            return pm.getApplicationEnabledSetting(UIIntents.CMAS_COMPONENT)
                    != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (final IllegalArgumentException ignored) {
            // CMAS app not installed.
        }
        return false;
    }
    //sprd bug497178 end

    @Override
    public boolean isSwipeAnimatable() {
        return !isInConversationListSelectMode();
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        final ConversationListFragment conversationListFragment =
                (ConversationListFragment) getFragmentManager().findFragmentById(
                        R.id.conversation_list_fragment);
        // When the screen is turned on, the last used activity gets resumed, but it gets
        // window focus only after the lock screen is unlocked.
        if (hasFocus && conversationListFragment != null) {
            conversationListFragment.setScrolledToNewestConversationIfNeeded();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    // sprd: 570184 stard
    private class FdnContatctsWorkThread extends Thread {

        public FdnContatctsWorkThread() {
        }

        @Override
        public void run() {
            FdnUtil fdnUtil = FdnUtil.getFdnUtilInstance();
            fdnUtil.initCacheMap();
            super.run();
        }
    }
    // sprd: 570184 end
}
