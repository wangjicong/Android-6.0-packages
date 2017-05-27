
package com.android.mmsfolderview.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import android.R.integer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v4.app.NotificationManagerCompat;

import com.android.mmsfolderview.R;
import com.android.mmsfolderview.data.SortMsgDataCollector;
import com.android.mmsfolderview.data.SortMsgListData;
import com.android.mmsfolderview.data.SortMsgListItemData;
import com.android.mmsfolderview.parser.XmlDomParser;
import com.android.mmsfolderview.parser.data.TimeQuantumDataBuilder;
import com.android.mmsfolderview.parser.process.TimeQuantumProcess;
import com.android.mmsfolderview.ui.SortMsgListFragment;
import com.android.mmsfolderview.ui.SortMsgListFragment.SortMsgListFragmentHost;
import com.android.mmsfolderview.util.MmsUtils;
import com.android.mmsfolderview.util.OsUtil;
import com.android.mmsfolderview.util.IntentUiUtils;
import com.android.mmsfolderview.util.PhoneUtils;
import com.android.mmsfolderview.util.PhoneUtils.PhoneUtilsLMR1;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

//add for bug 532539 begin
import com.android.mmsfolderview.util.VoLteState;

//add for bug 532539 end

public class SortMsgListActivity extends BaseActionBarActivity implements OnNavigationListener,
        MultiSelectActionModeCallback.Listener, SortMsgListFragmentHost {

    public static final String PREFERENCES_NAME = "displaycontrol";

    private static final int SEARCH_MAX_LENGTH = 512;
    private static final String IS_FROM_FOLDER_VIEW = "from_folder_view";
    private static final String DEST_VIEW_MODE = "dest_view_mode";
    private static final String TAG = "SortMsgListActivity";
    private SearchView mSearchView;
    private MenuItem mSearchItem;
    private String[] mSortMsgMenuItems;
    private ArrayAdapter<String> mNavigationAdater;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private int mSortType = SortMsgDataCollector.MSG_UNKNOW;
    private int mLoaderId = SortMsgDataCollector.LOADER_ID_DEFAULT;
    protected SortMsgListFragment mSortMsgListFragment;
    private int mKeyNotification = SortMsgDataCollector.INVAIL_NOTIFICATION_ID;
    private ArrayList<Integer> mMessagesId = new ArrayList<Integer>();

    ActionBar mActionBar;
    boolean isFirstLaunch;

    private boolean mIsFromContextMenu = false;
    // add for bug 532539 begin
    private VoLteState mVoLteState;
    // add for bug 532539 end

    // bug 489223: add for Custom XML SQL Query begin
    private static final String pathFileName = "/etc/com.android.mmsfolderview/sortby_time_quantum.xml";
    // bug 489223: add for Custom XML SQL Query end
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sort_msg_list_activity);
        // bug 489223: add for Custom XML SQL Query begin
        initData();
        // bug 489223: add for Custom XML SQL Query end
        isFirstLaunch = true;
        ensureSharedPrefAvail();
        invalidateActionBar();
        // add for bug 532539 begin
        mVoLteState = new VoLteState(this);
        if(!OsUtil.hasPhonePermission()){
            OsUtil.requestMissingPermission(this);
        }else{
            mVoLteState.registerVoLteNetStateListener();
        }
        // add for bug 532539 end
    }

    // bug 489223: add for Custom XML SQL Query begin
    private void initData() {
        if (!XmlDomParser.xmlParse(pathFileName, null)) {
            Log.e(TAG, "XmlParser parser xml happens error");
        }
        if (getIntent().getBooleanExtra("is_sim_sms", false)) {
            SortDisplayController.getInstance().setGlobalSortType(SortDisplayController.SIM_SMS);
            mSortMsgMenuItems = new String[] {
               getResources().getString(R.string.simsms_title)
            };
        } else {
            if (XmlDomParser.getItemList().size() > 0) {
                SortDisplayController.getInstance().setGlobalSortType(
                        SortDisplayController.TIME_QUANTUM);
                mSortMsgMenuItems = TimeQuantumDataBuilder.getDisplayTexts(this,
                        XmlDomParser.getItemList());
            } else {
                SortDisplayController.getInstance().setGlobalSortType(
                        SortDisplayController.FOLDER_VIEW);
                mSortMsgMenuItems = getResources().getStringArray(R.array.box_sort_menu_items);
            }
        }
    }

    private void refreshNavigationMenu() {
        if (SortDisplayController.getInstance().isTimeQuqntum()) {
            mSortMsgMenuItems = TimeQuantumDataBuilder.getDisplayTexts(this, XmlDomParser.getItemList());
            mNavigationAdater = new ArrayAdapter<String>(mActionBar.getThemedContext(),
                    R.layout.spinner_dropdown_item, mSortMsgMenuItems);
            mActionBar.setListNavigationCallbacks(mNavigationAdater, this);
            if (mActionMode == null) {
                mActionBar.setSelectedNavigationItem(mPreferences.getInt(
                        SortMsgDataCollector.MESSAGE_SORT_TYPE, SortMsgDataCollector.MSG_BOX_INBOX));
            }
        }
    }
    // bug 489223: add for Custom XML SQL Query end
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // fix for bug 527167 begin
        if (mActionMode == null) {
            // bug 489223: add for Custom XML SQL Query begin
            if (mActionBar != null && !isFirstLaunch
                    && SortDisplayController.getInstance().isFolderView()) {
                selectNavigationItem(mActionBar);
            }
            refreshNavigationMenu();
            // bug 489223: add for Custom XML SQL Query end
            isFirstLaunch = false;
            supportInvalidateOptionsMenu();
            if (!SortDisplayController.getInstance().isSimSms()) {//Bug 489223
                startCommonServiceToCancelNotification();
            }
        }
        // fix for bug 527167 end
    }

    // add for bug 532539 begin
    @Override
    protected void onDestroy() {
        mVoLteState.unRegisterVoLteNetStateListener();
        super.onDestroy();
    }

    // add for bug 532539 end
    private void startCommonServiceToCancelNotification() {
        try {
            Intent intent = IntentUiUtils.getFolderViewMessagingCommServiceIntent();
            if (mSortType == SortMsgDataCollector.MSG_BOX_INBOX) {
                intent.putExtra(SortMsgDataCollector.KEY_COMM,
                        SortMsgDataCollector.KEY_SMS_NOTIFICATION_ID);
            } else if (mSortType == SortMsgDataCollector.MSG_BOX_OUTBOX) {
                intent.putExtra(SortMsgDataCollector.KEY_COMM,
                        SortMsgDataCollector.KEY_MSG_SEND_ERROR);
            }
            SortMsgListActivity.this.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "start service error, action="
                    + SortMsgDataCollector.ACTION_FOLDER_VIEW_MESSAGING_COMM, e);
        }
    }

    private void startCommonServiceToCopySimSmsToPhone(int subId, String bobyText,
                                                       long receivedTimestamp, int messageStatus,
                                                       boolean isRead, String address) {
        try {
            Intent intent = IntentUiUtils.getFolderViewMessagingCommServiceIntent();
            intent.putExtra("subId", subId);
            intent.putExtra("bobyText", bobyText);
            intent.putExtra("receivedTimestamp", receivedTimestamp);
            intent.putExtra("messageStatus", messageStatus);
            intent.putExtra("isRead", isRead);
            intent.putExtra("address", address);
            intent.putExtra(SortMsgDataCollector.KEY_COMM,
                    SortMsgDataCollector.KEY_COPY_SIMSMS_TO_PHONE);

            SortMsgListActivity.this.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "start service error, action="
                    + SortMsgDataCollector.ACTION_FOLDER_VIEW_MESSAGING_COMM, e);
        }
    }

    private void startCommonServiceToCopySmsToSim(int messageId, int subId) {
        try {
            Log.d(TAG, "startCommonServiceToCopySmsToSim messageId" + messageId);
            Intent intent = IntentUiUtils.getFolderViewMessagingCommServiceIntent();
            intent.putExtra("message_id", messageId);
            intent.putExtra("subId", subId);
            intent.putExtra(SortMsgDataCollector.KEY_COMM,
                    SortMsgDataCollector.KEY_COPY_SMS_TO_SIM);

            SortMsgListActivity.this.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "start service error, action="
                    + SortMsgDataCollector.ACTION_FOLDER_VIEW_MESSAGING_COMM, e);
        }
    }

    @Override
    public void onAttachFragment(final Fragment fragment) {
        if (fragment instanceof SortMsgListFragment) {
            mSortMsgListFragment = (SortMsgListFragment) fragment;
            mSortMsgListFragment.setHost(this);
        }
    }

    @Override
    protected void updateActionBar(ActionBar actionBar) {
        mActionBar = actionBar;
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(
                R.color.action_bar_background_color)));
        if (mNavigationAdater == null) {
            mNavigationAdater = new ArrayAdapter<String>(actionBar.getThemedContext(),
                    R.layout.spinner_dropdown_item, mSortMsgMenuItems);
        }
        actionBar.setListNavigationCallbacks(mNavigationAdater, this);
        selectNavigationItem(actionBar);
        actionBar.show();
    }

    private void selectNavigationItem(ActionBar actionBar) {
        if (SortDisplayController.getInstance().isSimSms()) {
            return;
        }
        Intent intent = getIntent();
        ensureSharedPrefAvail();
        if (intent != null) {
            mKeyNotification = intent.getIntExtra(SortMsgDataCollector.KEY_NOTIFICATION, -1);
        }
        if (mKeyNotification == SortMsgDataCollector.KEY_SMS_NOTIFICATION_ID) {
            actionBar.setSelectedNavigationItem(SortMsgDataCollector.MSG_BOX_INBOX);
            mSortType = SortMsgDataCollector.MSG_BOX_INBOX;
            mEditor.putInt(SortMsgDataCollector.MESSAGE_SORT_TYPE, mSortType);
            mEditor.commit();
        } else if (mKeyNotification == SortMsgDataCollector.KEY_MSG_SEND_ERROR) {
            actionBar.setSelectedNavigationItem(SortMsgDataCollector.MSG_BOX_OUTBOX);
            mSortType = SortMsgDataCollector.MSG_BOX_OUTBOX;
            mEditor.putInt(SortMsgDataCollector.MESSAGE_SORT_TYPE, mSortType);
            mEditor.commit();
        } else {
            actionBar.setSelectedNavigationItem(mPreferences.getInt(
                    SortMsgDataCollector.MESSAGE_SORT_TYPE, SortMsgDataCollector.MSG_BOX_INBOX));
        }
        intent.putExtra(SortMsgDataCollector.KEY_NOTIFICATION,
                SortMsgDataCollector.INVAIL_NOTIFICATION_ID);
        setIntent(intent);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        ensureSharedPrefAvail();
        switch (position) {
            case 0:
                mSortType = SortMsgDataCollector.MSG_BOX_INBOX;
                break;
            case 1:
                mSortType = SortMsgDataCollector.MSG_BOX_SENT;
                break;
            case 2:
                mSortType = SortMsgDataCollector.MSG_BOX_OUTBOX;
                break;
            case 3:
                mSortType = SortMsgDataCollector.MSG_BOX_DRAFT;
                break;
            // bug 489223: add for Custom XML SQL Query begin
            default:
                Log.d(TAG, "It's not folder view");
                mSortType = position;
            // bug 489223: add for Custom XML SQL Query end
        }
        // setLoaderIdByOrderValue();
        if (SortDisplayController.getInstance().isSimSms()) {
            mEditor.putInt(SortMsgDataCollector.SIM_MESSAGE_SORT_TYPE, mSortType);
        } else {
            mEditor.putInt(SortMsgDataCollector.MESSAGE_SORT_TYPE, mSortType);
        }
        mEditor.commit();
        mSortMsgListFragment.reloadMessage(mLoaderId);
        if (!SortDisplayController.getInstance().isSimSms()) {
            startCommonServiceToCancelNotification();
        }
        return true;
    }

    private void ensureSharedPrefAvail() {
        if (mPreferences == null) {
            mPreferences = SortMsgListActivity.this.getSharedPreferences(PREFERENCES_NAME,
                    SortMsgListActivity.this.MODE_PRIVATE);
        }
        if (mEditor == null) {
            mEditor = mPreferences.edit();
        }
    }

    private boolean isCellBroadcastAppLinkEnabled() {
        try {
            final PackageManager pm = SortMsgListActivity.this.getPackageManager();
            return pm
                    .getApplicationEnabledSetting(IntentUiUtils.CELLBROADCASTRECEIVER_PACKAGE_NAME) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (final IllegalArgumentException ignored) {
            // CMAS app not installed.
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            return true;
        }
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sort_msg_list_menu, menu);
        if (!isCellBroadcastAppLinkEnabled()) {
            menu.findItem(R.id.action_cell_broadcasts).setVisible(false);
        }
        if (!SortDisplayController.isCMCC() || SortDisplayController.getInstance().isSimSms()) {
            menu.removeItem(R.id.message_in_sim);// TODO
        }
        if (SortDisplayController.isCMCC() && SortDisplayController.getInstance().isFolderView()) {
//            menu.removeItem(R.id.folder_view);
            PhoneUtilsLMR1 phoneUtilsLMR1 = new PhoneUtilsLMR1(SortMsgListActivity.this);
            if (!OsUtil.hasPhonePermission()) {
                OsUtil.requestMissingPermission(this);
            } else {
                if (!phoneUtilsLMR1.hasSim()) {
                    menu.findItem(R.id.message_in_sim).setVisible(false);
                }
            }
        }
        initSearchView(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mActionMode != null) {
            return true;
        }
        if (SortDisplayController.getInstance().isSimSms()) {
            menu.removeItem(R.id.action_delete_messages);
            menu.removeItem(R.id.action_sms_merge_forward);
            //menu.removeItem(R.id.folder_view);
        } else {
            if (mSortMsgListFragment.mAdapter.getItemCount() == 0) {
                menu.findItem(R.id.action_delete_messages).setEnabled(false);
            } else {
                menu.findItem(R.id.action_delete_messages).setEnabled(true);
            }
            //Sprd add for sms merge forward begin
            if(mSortType == SortMsgDataCollector.MSG_BOX_INBOX || mSortType == SortMsgDataCollector.MSG_BOX_SENT){
                menu.findItem(R.id.action_sms_merge_forward).setEnabled(true);
            }else{
                menu.findItem(R.id.action_sms_merge_forward).setEnabled(false);
            }
            //Sprd add for sms merge forward end
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void initSearchView(Menu menu) {
        mSearchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        // mSearchItem.setOnActionExpandListener(mOnActionExpandListener);
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String arg0) {
                    Intent intent = new Intent();
                    intent.setClass(SortMsgListActivity.this, SearchActivity.class);
                    intent.putExtra(SearchManager.QUERY, arg0);
                    startActivity(intent);
                    mSearchItem.collapseActionView();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String arg0) {
                    if (arg0 != null && arg0.length() > SEARCH_MAX_LENGTH) {
                        mSearchView.setQuery(arg0.substring(0, SEARCH_MAX_LENGTH - 1), false);
                        Toast.makeText(SortMsgListActivity.this,
                                getString(R.string.search_max_length), Toast.LENGTH_LONG).show();
                    }
                    return false;
                }
            });
        }
        menu.removeItem(R.id.action_search);// TODO
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent = new Intent();
        ComponentName componentName = null;
        switch (id) {
            case R.id.action_compose_new:
                try {
                    intent = IntentUiUtils.getRemoteActivityIntent(
                            IntentUiUtils.MESSAGING_PACKAGE_NAME,
                            IntentUiUtils.MESSAGING_NEW_MESSAGE);
                    startActivityForResult(intent, REQUEST_CODE_NEW_MESSAGE);
                } catch (Exception e) {
                    Log.e(TAG, "onOptionsItemSelected: can't launch this activity: "
                            + IntentUiUtils.MESSAGING_NEW_MESSAGE, e);
                }
                return true;
            case R.id.action_search:
                Toast.makeText(this, "TODO", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_menu_msg_list:
                try {
                    intent = IntentUiUtils.getRemoteActivityIntent(
                            IntentUiUtils.MESSAGING_PACKAGE_NAME,
                            IntentUiUtils.MESSAGING_CONVERSATION_LIST_VIEW);
                    intent.putExtra(IS_FROM_FOLDER_VIEW, true);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "onOptionsItemSelected: can't launch this activity: "
                            + IntentUiUtils.MESSAGING_CONVERSATION_LIST_VIEW, e);
                }
                return true;
            case R.id.action_settings:
                try {
                    intent = IntentUiUtils.getRemoteActivityIntent(
                            IntentUiUtils.MESSAGING_PACKAGE_NAME,
                            IntentUiUtils.MESSAGING_SETTING_ACTIVITY);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "onOptionsItemSelected: can't launch this activity: "
                            + IntentUiUtils.MESSAGING_SETTING_ACTIVITY, e);
                }
                return true;
            case R.id.action_cell_broadcasts:
                try {
                    intent = IntentUiUtils.getRemoteActivityIntent(
                            IntentUiUtils.CELLBROADCASTRECEIVER_PACKAGE_NAME,
                            IntentUiUtils.CELLBROADCASTRECEIVER_CELLBROADCAST_LIST_ACTIVITY);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "onOptionsItemSelected: can't launch this activity: "
                            + IntentUiUtils.CELLBROADCASTRECEIVER_CELLBROADCAST_LIST_ACTIVITY, e);
                }
                return true;
            case R.id.message_in_sim:
                try {
                    intent = IntentUiUtils.getRemoteActivityIntent(
                            IntentUiUtils.MESSAGING_PACKAGE_NAME,
                            IntentUiUtils.MESSAGING_CONVERSATION_LIST_VIEW);
                    intent.putExtra(IS_FROM_FOLDER_VIEW, true);
                    intent.putExtra(DEST_VIEW_MODE, 3);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "onOptionsItemSelected: can't launch this activity: message_in_sim", e);
                }
                return true;
/*            case R.id.folder_view:
                try {
                    intent = IntentUiUtils.getRemoteActivityIntent(
                            IntentUiUtils.MESSAGING_PACKAGE_NAME,
                            IntentUiUtils.MESSAGING_CONVERSATION_LIST_VIEW);
                    intent.putExtra(IS_FROM_FOLDER_VIEW, true);
                    intent.putExtra(DEST_VIEW_MODE, 1);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "onOptionsItemSelected: can't launch this activity: folder_view", e);
                }
                return true;*/
            case R.id.action_display_option:
                createSimSelectDialog();
                return true;
            case R.id.action_sortby:
                createOrderTypeDialog();
                return true;
            case R.id.action_delete_messages:
                if (PhoneUtils.isMessagingDefaultSmsApp(this)) {
                    startMultiSelectActionMode();
                } else {
                    final Intent defSmsIntent = PhoneUtils
                            .getChangeDefaultSmsAppIntent(PhoneUtils.MESSAGING_PACKAGE_NAME);
                    startActivityForResult(defSmsIntent, REQUEST_CODE_SET_DEFAULT_SMS_APP);
                }
                return true;
            //Sprd add for sms merge forward begin
        case R.id.action_sms_merge_forward:
            Log.d(TAG, "=======sms merge forward=======Uri: "
                    + SortMsgDataCollector.MESSAGE_LIST_VIEW_URI);
            Intent i = new Intent("com.sprd.messaging.ui.smsmergeforward.SmsMergeForwardActivity");
            i.setClassName("com.android.messaging", "com.sprd.messaging.ui.smsmergeforward.SmsMergeForwardActivity");
            i.putExtra("SMS_MERGE_FORWARD_FROM", "SortMsgListActivity");
            i.putExtra("sms_merge_forward_uri",
                    "content://" + SortMsgDataCollector.AUTHORITY + '/'+"message_list_view_query");
            i.putExtra("sms_merge_forward_project",
                    new String[] { "name",
                            "_id",
                            "received_timestamp",
                            "message_status",
                            "text"});
            String where = null;
            if(mSortType == SortMsgDataCollector.MSG_BOX_INBOX ){
                where = "message_protocol"
                        + "="
                        + SortMsgDataCollector.PROTOCOL_SMS
                        + " AND "
                        + "message_status"
                        + "="
                        + SortMsgDataCollector.BUGLE_STATUS_INCOMING_COMPLETE;
            }else if(mSortType == SortMsgDataCollector.MSG_BOX_SENT){
                where = "message_protocol"
                        + "="
                        + SortMsgDataCollector.PROTOCOL_SMS
                        + " AND "
                        + "("
                        + "message_status"
                        + "="
                        + SortMsgDataCollector.BUGLE_STATUS_OUTGOING_COMPLETE
                        + " OR "
                        + "message_status"
                        + "="
                        + SortMsgDataCollector.BUGLE_STATUS_OUTGOING_DELIVERED
                        + ")";
            }
            i.putExtra(
                    "sms_merge_forward_condition",where);
            i.putExtra("sms_merge_forward_orderby",
                    "received_timestamp desc");
            startActivity(i);
            return true;
           //Sprd add for sms merge forward end
        }
        return super.onOptionsItemSelected(item);
    }

    public int getSubIdShow() {
        int subIdShow;
        if (SortDisplayController.getInstance().isSimSms()) {
            subIdShow = mPreferences.getInt(SortMsgDataCollector.SHOW_SIM_MESSAGE_BY_SUB_ID,
                    SortMsgDataCollector.SHOW_ALL_MESSAGE);
        } else {
            subIdShow = mPreferences.getInt(SortMsgDataCollector.SHOW_MESSAGE_BY_SUB_ID,
                    SortMsgDataCollector.SHOW_ALL_MESSAGE);
        }
        return subIdShow;
    }

    private void createSimSelectDialog() {
        int subIdShow = getSubIdShow();
        int choiceItem = 0;
        boolean isActiveSubId = false;
        final ArrayList<String> simList = new ArrayList<String>();
        simList.add(SortMsgListActivity.this.getString(R.string.folder_display_all));
        PhoneUtilsLMR1 phoneUtilsLMR1 = new PhoneUtilsLMR1(SortMsgListActivity.this);
        if(!OsUtil.hasPhonePermission()){
            OsUtil.requestMissingPermission(this);
        } else {
            final List<SubscriptionInfo> InfoList = phoneUtilsLMR1.getActiveSubscriptionInfoList();
            if (InfoList != null && InfoList.size() != 0) {
                Iterator iterator = InfoList.iterator();
                while (iterator.hasNext()) {
                    SubscriptionInfo subscriptionInfo = (SubscriptionInfo) iterator.next();
                    String simNameText = subscriptionInfo.getDisplayName().toString();
                    String displayName = TextUtils.isEmpty(simNameText) ? SortMsgListActivity.this
                            .getString(R.string.sim_slot_identifier,
                                    subscriptionInfo.getSimSlotIndex() + 1) : simNameText;
                    simList.add(displayName);
                    Log.d("tim_V6_ci", "subscriptionInfo=" + subscriptionInfo);
                    Log.d("tim_V6_ci", "subIdShow=" + subIdShow);
                    if (subscriptionInfo.getSubscriptionId() == subIdShow) {
                        choiceItem = simList.size() - 1;
                        isActiveSubId = true;
                        Log.d("tim_V6_ci", "choiceItem=" + choiceItem);
                    }
                }
            }
            if (!isActiveSubId && subIdShow != 0) {
                choiceItem = 0;// if change the sim, will cause the subid had saved
                                // in Preferences can't match current active subid,
                                // then show the simSelectDialog checked nothing.
                                //but now we need to show it checked all for bug#557268
            }
            ArrayAdapter<String> simAdapter = new ArrayAdapter<String>(SortMsgListActivity.this,
                    R.layout.display_options, simList) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    String simName = getItem(position);
                    TextView tv = (TextView) v;
                    tv.setText(simName);
                    return v;
                }
            };

            DialogInterface.OnClickListener simSelectListener = new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialog, int which) {
                    if (which > 0) {
                        SubscriptionInfo subscriptionInfo = InfoList.get(which - 1);
                        if (SortDisplayController.getInstance().isSimSms()) {
                            mEditor.putInt(SortMsgDataCollector.SHOW_SIM_MESSAGE_BY_SUB_ID,
                                    subscriptionInfo.getSubscriptionId()).commit();
                        } else {
                            mEditor.putInt(SortMsgDataCollector.SHOW_MESSAGE_BY_SUB_ID,
                                    subscriptionInfo.getSubscriptionId()).commit();
                        }
                    } else {
                        if (SortDisplayController.getInstance().isSimSms()) {
                            mEditor.putInt(SortMsgDataCollector.SHOW_SIM_MESSAGE_BY_SUB_ID,
                                    SortMsgDataCollector.SHOW_ALL_MESSAGE).commit();
                        } else {
                            mEditor.putInt(SortMsgDataCollector.SHOW_MESSAGE_BY_SUB_ID,
                                    SortMsgDataCollector.SHOW_ALL_MESSAGE).commit();
                        }
                    }
                    mSortMsgListFragment.reloadMessage(mLoaderId);
                    dialog.dismiss();
                }
            };
            AlertDialog.Builder simSelectDialog = new AlertDialog.Builder(SortMsgListActivity.this);
            simSelectDialog.setTitle(R.string.folder_display_option);
            simSelectDialog.setSingleChoiceItems(simAdapter, choiceItem, simSelectListener);
            simSelectDialog.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            simSelectDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_NEW_MESSAGE:
                // If had sent the message, folderview go to outbox.
                // bug 489223: add for Custom XML SQL Query begin
                if ((resultCode == RESULT_OK)
                        && (mActionMode == null)
                        && (SortDisplayController.getInstance().isFolderView())) {
                    // bug 489223: add for Custom XML SQL Query end
                    mActionBar.setSelectedNavigationItem(SortMsgDataCollector.MSG_BOX_OUTBOX);
                    mSortType = SortMsgDataCollector.MSG_BOX_OUTBOX;
                    mEditor.putInt(SortMsgDataCollector.MESSAGE_SORT_TYPE, mSortType);
                    mEditor.commit();
                    Log.d("tim_V6_sentexit", "onActivityResult:set sort item position:"
                            + (SortMsgDataCollector.MSG_BOX_OUTBOX));
                }
                break;
            case REQUEST_CODE_SET_DEFAULT_SMS_APP:
                if (resultCode == RESULT_OK) {
                    if ((mActionMode == null) && !mIsFromContextMenu) {
                        startMultiSelectActionMode();
                    } else {
                        deleteMessagesFromDatabase(mMessagesId);
                    }
                } else {
                    String needDef = getString(R.string.need_messaging_is_default_app);
                    String string = String.format(needDef, getString(R.string.host_app_name));
                    IntentUiUtils.showToastAtBottom(SortMsgListActivity.this, string);
                }
                break;
            default:
                break;
        }
    }

    private void createOrderTypeDialog() {
        String orderKey = SortMsgDataCollector.getMsgOrderKey(mSortType);
        String orderValue = mPreferences.getString(orderKey, "");
        int checkedItem = getCheckedItemByOrderValue(orderValue);
        final AlertDialog.Builder orderTypeDialog = new AlertDialog.Builder(this);
        orderTypeDialog.setTitle(R.string.folder_sortby);
        orderTypeDialog.setSingleChoiceItems(R.array.sort_type, checkedItem,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        switch (whichButton) {
                            case 0:
                                // mLoaderId =
                                // MessagingDataDef.LOADER_ID_TIME_DESC;
                                mEditor.putString(SortMsgDataCollector.getMsgOrderKey(mSortType),
                                        SortMsgDataCollector.getOrderByReceivedTimeDesc());
                                break;
                            case 1:
                                // mLoaderId =
                                // MessagingDataDef.LOADER_ID_TIME_ASC;
                                mEditor.putString(SortMsgDataCollector.getMsgOrderKey(mSortType),
                                        SortMsgDataCollector.getOrderByReceivedTimeAsc());
                                break;
                            case 2:
                                // mLoaderId =
                                // MessagingDataDef.LOADER_ID_PHONE_NUM_DESC;
                                mEditor.putString(SortMsgDataCollector.getMsgOrderKey(mSortType),
                                        SortMsgDataCollector.getOrderByPhoneNumberDesc());
                                break;
                            case 3:
                                // mLoaderId =
                                // MessagingDataDef.LOADER_ID_PHONE_NUM_ASC;
                                mEditor.putString(SortMsgDataCollector.getMsgOrderKey(mSortType),
                                        SortMsgDataCollector.getOrderByPhoneNumberAsc());
                                break;

                            default:
                                break;
                        }
                        mEditor.commit();
                        Log.d("tim_V6_loader", "createOrderTypeDialog:mLoaderId=" + mLoaderId);
                        mSortMsgListFragment.reloadMessage(mLoaderId);
                        dialog.dismiss();
                    }
                });
        orderTypeDialog.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        orderTypeDialog.show();

    }

    private int getCheckedItemByOrderValue(String orderValue) {
        int checkedItem = 0;
        switch (orderValue.trim().toLowerCase()) {
            case "received_timestamp desc":// MessagingDataDef.ORDER_BY_TIME_DESC:
                checkedItem = 0;
                break;
            case "received_timestamp asc":// MessagingDataDef.ORDER_BY_TIME_ASC:
                checkedItem = 1;
                break;
            case "display_destination desc":// MessagingDataDef.ORDER_BY_PHONE_NUMBER_DESC:
                checkedItem = 2;
                break;
            case "display_destination asc":// MessagingDataDef.ORDER_BY_PHONE_NUMBER_ASC:
                checkedItem = 3;
                break;
            default:
                checkedItem = 0;
                break;
        }
        return checkedItem;
    }

    private int setLoaderIdByOrderValue() {
        String orderKey = SortMsgDataCollector.getMsgOrderKey(mSortType);
        String orderValue = mPreferences.getString(orderKey,
                SortMsgDataCollector.ORDER_BY_TIME_DESC);

        switch (orderValue.trim().toLowerCase()) {
            case "received_timestamp desc":// MessagingDataDef.ORDER_BY_TIME_DESC:
                mLoaderId = SortMsgDataCollector.LOADER_ID_TIME_DESC;
                break;
            case "received_timestamp asc":// MessagingDataDef.ORDER_BY_TIME_ASC:
                mLoaderId = SortMsgDataCollector.LOADER_ID_TIME_ASC;
                break;
            case "participant_normalized_destination desc":// MessagingDataDef.ORDER_BY_PHONE_NUMBER_DESC:
                mLoaderId = SortMsgDataCollector.LOADER_ID_PHONE_NUM_DESC;
                break;
            case "participant_normalized_destination asc":// MessagingDataDef.ORDER_BY_PHONE_NUMBER_ASC:
                mLoaderId = SortMsgDataCollector.LOADER_ID_PHONE_NUM_ASC;
                break;
            default:
                break;
        }
        return mLoaderId;
    }

    protected void startMultiSelectActionMode() {
        mMaxIds = new MaxIDs(getApplicationContext());
        startActionMode(new MultiSelectActionModeCallback(this, mSortMsgListFragment.mAdapter));
    }

    private void holdSelectMessages(final ArrayList<Integer> messagesId) {
        mMessagesId = messagesId;
    }

    private void deleteMessagesFromDatabase(final ArrayList<Integer> messagesId) {
        //modify for bug 559631 begin
        Log.d(TAG, "delete message from db. Adapter count:"+
                mSortMsgListFragment.mAdapter.getItemCount()+" and messagesId.size"+messagesId.size());
        if (mSortMsgListFragment.mAdapter.getItemCount() == messagesId.size() && messagesId.size() != 1) {
            String box_type = String.valueOf(mSortType)+getMaxMessageIds();
            getContentResolver().delete(SortMsgDataCollector.MESSAGE_SPECIFY_DELETE_URI,
                    box_type, integerToString(messagesId));
        } else {
            for (int i : messagesId) {
                Uri uri = ContentUris
                        .withAppendedId(SortMsgDataCollector.MESSAGE_SPECIFY_DELETE_URI, i);
                getContentResolver().delete(uri, null, null);
            }
        }
        //modify for bug 559631 end

        exitMultiSelectState();
        if (mIsFromContextMenu) {
            mMessagesId.clear();
        }
    }

    //modify for bug 559631 begin
    private String[] integerToString(ArrayList<Integer> messagesId){
        String[] messagesIdStr = new String[messagesId.size()];
        for(int i = 0; i < messagesId.size(); i++){
            messagesIdStr[i] = String.valueOf(messagesId.get(i));
        }
        return messagesIdStr;
    }
    //modify for bug 559631 end

    @Override
    protected void confirmDeleteMessage() {
        if (!PhoneUtils.isMessagingDefaultSmsApp(this)) {
            final Intent defSmsIntent = PhoneUtils
                    .getChangeDefaultSmsAppIntent(PhoneUtils.MESSAGING_PACKAGE_NAME);
            startActivityForResult(defSmsIntent, REQUEST_CODE_SET_DEFAULT_SMS_APP);
        } else {
            deleteMessagesFromDatabase(mMessagesId);
        }

    }

    @Override
    public void onActionBarDelete(final ArrayList<Integer> messages) {
        holdSelectMessages(messages);
        createDeleteMessageDialog(R.string.delete_messages_confirmation_dialog_title);
        Log.d("tim_V6_select", "message size=" + messages.size());
    }

    protected void exitMultiSelectState() {
        dismissActionMode();
        mSortMsgListFragment.updateUi();
    }

    @Override
    public Cursor onActionBarSelectAll() {
        return mSortMsgListFragment.mAdapter.getCursor();
    }

    @Override
    public void onActionBarUpdateMessageCount(int cnt) {
        UpdateSelectMessageCount(cnt);
    }

    @Override
    public void onActionBarHome() {
        dismissActionMode();// TODO Auto-generated method stub
    }

    @Override
    public void onMessageClicked(SortMsgListData listData, SortMsgListItemData listItemData,
            boolean isLongClick, SortMsgListItemView listItemView) {
        if (isInMessageListSelectMode()) {
            final MultiSelectActionModeCallback multiSelectActionMode = (MultiSelectActionModeCallback) getActionModeCallback();
            multiSelectActionMode.toggleSelect(listData, listItemData);
            mSortMsgListFragment.updateUi();
        } else {
            Bundle activityOptions = null;
            boolean hasCustomTransitions = false;

            try {
                final Intent intent = IntentUiUtils.getSortMsgDetailActivityIntent(this,
                        listItemData, hasCustomTransitions);

                startActivity(intent, activityOptions);
            } catch (Exception e) {
                Log.e(TAG, "start service error, action="
                        + IntentUiUtils.MESSAGING_DETAILS_ACTIVITY, e);
            }

        }
    }

    @Override
    public void onCreateConversationClick() {
        // TODO Auto-generated method stub

    }

    protected boolean isInMessageListSelectMode() {
        return getActionModeCallback() instanceof MultiSelectActionModeCallback;
    }

    @Override
    public boolean isMessageSelected(int messageId) {
        return isInMessageListSelectMode()
                && ((MultiSelectActionModeCallback) getActionModeCallback()).isSelected(messageId);
    }

    @Override
    public void deleteMessages(ArrayList<Integer> messagesId, boolean isFromContextMenu) {
        mIsFromContextMenu = isFromContextMenu;
        mMessagesId = messagesId;
        createDeleteMessageDialog(R.string.delete_message_confirmation_dialog_title);
    }

    // add for bug 532539 begin
    @Override
    public boolean isEnableVoLte() {
        return (mVoLteState.getIsVoLteProduct() && mVoLteState.getRegisteredState());
    };

    // add for bug 532539 end

    @Override
    public void copySimSmsToPhone(int subId, String bobyText,
                                  long receivedTimestamp, int messageStatus,
                                  boolean isRead, String address) {
        startCommonServiceToCopySimSmsToPhone(subId, bobyText,
                                              receivedTimestamp, messageStatus,
                                              isRead, address);
    }

    @Override
    public void copySmsToSim(int messageId, int subId) {
        startCommonServiceToCopySmsToSim(messageId, subId);
    }

    @Override
    public void deleteSimSms(int indexOnIcc, int subId) {
        startCommonServiceTodeleteSimSms(indexOnIcc, subId);
    }

    private void startCommonServiceTodeleteSimSms(int indexOnIcc, int subId) {
        try {
            Log.d(TAG, "startCommonServiceToCopySimSmsToPhone messageId" + indexOnIcc);
            Intent intent = IntentUiUtils.getFolderViewMessagingCommServiceIntent();
            intent.putExtra("index_on_icc", indexOnIcc);
            intent.putExtra("subId", subId);
            intent.putExtra(SortMsgDataCollector.KEY_COMM,
                    SortMsgDataCollector.KEY_DEL_SIM_SMS);

            SortMsgListActivity.this.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "start service error, action="
                    + SortMsgDataCollector.ACTION_FOLDER_VIEW_MESSAGING_COMM, e);
        }
    }

    @Override
    public boolean isInActionMode() {
        return (mActionMode != null);
    }

    @Override
    public boolean isSwipeAnimatable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSelectionMode() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasWindowFocus() {
        // TODO Auto-generated method stub
        return false;
    }

    //add for bug 559631 begin
    private String getMaxMessageIds(){
        if (getMaxIds() == null) {
            return "";
        }
        return ","+getMaxIds().getMaxMessage()+","+getMaxIds().getMaxMmsID()+","+getMaxIds().getMaxSmsID();
    }

    private MaxIDs mMaxIds;

    @Override
    public MaxIDs getMaxIds(){
        return mMaxIds;
    }

    class MaxIDs{

        public MaxIDs(Context ctx){
            mCtx = ctx;
        }

        public void process(){

            clear();

            getMaxMessageID();

            getMaxSmsID();

            getMaxMmsID();

        }

        //get mms max message id from the telephony
        private String getMaxMmsID(){
            if (!(getMaxMms().length() <=0) || !getMaxMms().trim().isEmpty()) {
                return mnMaxMmsID;
            }
            ContentResolver resolver = mCtx.getContentResolver();
            Cursor cursor = resolver.query(Uri.parse("content://mms"),
                    new String[]{"_id"},
                    null,
                    null,
                    "_id desc");
            try {
                if (cursor == null || cursor.getCount() == 0) {
                    Log.d(TAG, "there is no mms, return.");
                    return "-1";
                }
                cursor.moveToFirst();
                mnMaxMmsID = String.valueOf(cursor.getInt(0));
                Log.d(TAG, "the max mms message id is:"+mnMaxMmsID);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor!= null) {
                    cursor.close();
                    cursor = null;
                }
            }
            return mnMaxMmsID;
        }

        //get sms max message id from the telephony
        private String getMaxSmsID(){
            if (!(getMaxSms().length() <=0) || !getMaxSms().trim().isEmpty()) {
                return mnMaxSmsID;
            }
            ContentResolver resolver = mCtx.getContentResolver();
            Cursor cursor = resolver.query(Uri.parse("content://sms"),
                    new String[]{"_id"},
                    null,
                    null,
                    "_id desc");
            try {
                if (cursor == null || cursor.getCount() == 0) {
                    Log.d(TAG, "there is no sms, return.");
                    return "-1";
                }
                cursor.moveToFirst();
                mnMaxSmsID = String.valueOf(cursor.getInt(0));
                Log.d(TAG, "the max mms message id is:"+mnMaxSmsID);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor!= null) {
                    cursor.close();
                    cursor = null;
                }
            }
            return mnMaxSmsID;
        }

        //get local db's max messageId
        private String getMaxMessageID(){
            String uriOfMaxMessageStr = "";
            // Message;
            ContentResolver resolver = mCtx.getContentResolver();
            Cursor cursor = resolver.query(SortMsgDataCollector.QUERY_ID_IN_MESSAGES_URI,
                    new String[]{"_id", "sms_message_uri"},
                    null,
                    null,
                    "_id desc");
            try {
                if (cursor == null || cursor.getCount() == 0) {
                    Log.d(TAG, "there is no message, return.");
                    return "-1";
                }
                cursor.moveToFirst();
                mnMaxMessageID = String.valueOf(cursor.getInt(0));
                uriOfMaxMessageStr = cursor.getString(1);
                Log.d(TAG, "the max message id in messages_table in bugle.db is:"+ mnMaxMessageID+
                        " and uri:"+uriOfMaxMessageStr);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                        cursor = null;
                    }
            }
            if(uriOfMaxMessageStr == null){
                Log.d(TAG, " delete draft box.");
                return "-1";
            }
            return mnMaxMessageID;
        }

        private void clear(){
            mnMaxMmsID = "";
            mnMaxSmsID = "";
            mnMaxMessageID = "";
        }

        public String getMaxMms(){ return mnMaxMmsID;}
        public String getMaxSms(){ return mnMaxSmsID;}
        public String getMaxMessage(){ return mnMaxMessageID;}

        private String mnMaxMmsID;
        private String mnMaxSmsID;
        private String mnMaxMessageID;
        private Context mCtx;
    }
    //add for bug 559631 end
}
