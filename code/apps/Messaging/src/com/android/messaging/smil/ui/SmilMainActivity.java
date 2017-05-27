package com.android.messaging.smil.ui;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.smil.ui.SmilContactPickFragment;
import com.android.messaging.smil.ui.SmilMainFragment;
import com.android.messaging.smil.ui.SmilContactPickFragment.SmilContactPickHost;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.ui.contact.ContactPickerFragment;
import com.android.messaging.ui.mediapicker.SmilMediaPicker;

import android.support.v7.app.ActionBar;
import android.app.Fragment;
//add for jordan
import android.content.Intent;

import com.android.messaging.ui.contact.ContactPickerFragment.ContactPickerFragmentHost;
import com.android.messaging.ui.conversation.ConversationActivityUiState;
import com.android.messaging.ui.conversation.ConversationActivityUiState.ConversationActivityUiStateHost;
import com.android.messaging.ui.conversationlist.ConversationListActivity;
//add for jordan
import com.android.messaging.util.Assert;
import com.android.messaging.util.GlobleUtil;

import android.widget.FrameLayout;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;

public class SmilMainActivity extends BugleActionBarActivity implements
        ContactPickerFragmentHost, ConversationActivityUiStateHost,
        SmilContactPickHost {
    ActionBar actionBar = null;
    SmilMainFragment mSmilMainFragment;
    SmilContactPickFragment mSmilContactPickFragment;
    public String mConvName = "";
    private String mConversationId;
    private ConversationActivityUiState mUiState;
    private FragmentTransaction mFragmentTransaction = null;
    private FrameLayout mSmilMainFrame = null;
    
    public interface OnbackKeyListener{
        public void onBackPressedSmilMainActivity();
    }
    
    public void setOnbackKeyListener(OnbackKeyListener onbackKeyListener){
        mOnbackKeyListener = onbackKeyListener;
    }
    
    private OnbackKeyListener mOnbackKeyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.smail_main_activity);
        // bug 496495 end
        final Intent intent = getIntent();
        if (intent != null) {
            System.out.println("intent is not null");
            mConvName = intent.getStringExtra("userData");
            System.out.println("convName = [" + mConvName + "");
        }
        if (intent != null) {
            // String userData = intent.getStringExtra("userData");
            // System.out.println("userData = " + userData);
            boolean isDraft = intent.getBooleanExtra("isDraft", false);
            if (isDraft) {
               // bug 550710 start
                /*Modify by SPRD for bug 561492 2016.05.19 Start*/
                if(GlobleUtil.getDraftMessageData()==null){
                       //finish();
                       //return;
                    mConversationId = GlobleUtil.getConId(getApplication());
                }else{
                    // bug 550710 end
                    mConversationId = GlobleUtil.getDraftMessageData()
                            .getConversationId();
                }
                /*Modify by SPRD for bug 561492 2016.05.19 End*/
            } else {
                mConversationId = GlobleUtil.getConvMessageData()
                        .getConversationId();
            }
        }
        // Do our best to restore UI state from saved instance state.
        /*
         * if (savedInstanceState != null) { mUiState =
         * savedInstanceState.getParcelable(SAVED_INSTANCE_STATE_UI_STATE_KEY);
         * } else { if (intent.
         * getBooleanExtra(UIIntents.UI_INTENT_EXTRA_GOTO_CONVERSATION_LIST,
         * false)) { // See the comment in
         * BugleWidgetService.getViewMoreConversationsView() why this // is
         * unfortunately necessary. The Bugle desktop widget can display a list
         * of // conversations. When there are more conversations that can be
         * displayed in // the widget, the last item is a "More conversations"
         * item. The way widgets // are built, the list items can only go to a
         * single fill-in intent which points // to this ConversationActivity.
         * When the user taps on "More conversations", we // really want to go
         * to the ConversationList. This code makes that possible. /*Modify by
         * SPRD for bug526121 20160201 Start* final Intent convListIntent = new
         * Intent(this, ConversationListActivity.class);
         * convListIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
         * startActivity(convListIntent); finish(); /*Modify by SPRD for
         * bug526121 20160201 End* return; } }
         */

        // If saved instance state doesn't offer a clue, get the info from the
        // intent.
        if (mUiState == null) {
            mUiState = new ConversationActivityUiState(mConversationId);
        }
        mUiState.setHost(this);
        // mInstanceStateSaved = false;

        mSmilMainFragment = new SmilMainFragment();
        mSmilContactPickFragment = new SmilContactPickFragment();
        mSmilContactPickFragment.setHost(this);
        initUi();
    }

    private void initUi() {
        System.out.println("jordan,enter initUi()");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar.isShowing()) {
            System.out.println("jordan, actionBar is showing, will hide it");
            actionBar.hide();
        }
        final FragmentManager fragmentManager = getFragmentManager();
        mFragmentTransaction = fragmentManager.beginTransaction();
        mFragmentTransaction.add(R.id.smailmain_fragment_container,
                mSmilMainFragment, SmilMainFragment.FRAGMENT_TAG);
        mFragmentTransaction.add(R.id.smil_contact_pick_fragment_container,
                mSmilContactPickFragment, SmilContactPickFragment.FRAGMENT_TAG);
        Bundle bundle = new Bundle();
        // if mConvName is null, restore it to default value.
        bundle.putString("convName", mConvName == null ? "" : mConvName.trim());
        mSmilContactPickFragment.setArguments(bundle);
        mFragmentTransaction.commit();
        mSmilMainFrame = (FrameLayout) findViewById(R.id.smailmain_fragment_container);

    }

    private FragmentTransaction getFragmentTransaction() {
        return mFragmentTransaction;
    }

    public String getConvName() {
        return mConvName;
    }

    private SmilMainFragment getSmilMainFragment() {
        return (SmilMainFragment) getFragmentManager().findFragmentByTag(
                SmilMainFragment.FRAGMENT_TAG);
    }

    private SmilContactPickFragment getSmilContactPickFragment() {
        return (SmilContactPickFragment) getFragmentManager()
                .findFragmentByTag(SmilContactPickFragment.FRAGMENT_TAG);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(mOnbackKeyListener != null){
            mOnbackKeyListener.onBackPressedSmilMainActivity();
        }
    }

    /*
     * @Override protected void updateActionBar(final ActionBar actionBar) {
     * //add for jordan String userData =
     * getIntent().getStringExtra("userData"); System.out.println("userdata = "
     * + userData); actionBar.setTitle(userData); //add for jordan
     * //actionBar.setTitle(getString(R.string.app_name));
     * actionBar.setDisplayShowTitleEnabled(true);
     * actionBar.setDisplayHomeAsUpEnabled(false);
     * actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
     * actionBar.setBackgroundDrawable(new ColorDrawable(
     * getResources().getColor(R.color.action_bar_background_color)));
     * actionBar.show(); super.updateActionBar(actionBar); }
     */

    @Override
    // From ConversationActivityUiStateListener
    public void onConversationContactPickerUiStateChanged(final int oldState,
            final int newState, final boolean animate) {
        // Assert.isTrue(oldState != newState);
        // updateUiState(animate);
    }

    @Override
    // From ContactPickerFragmentHost
    public void onGetOrCreateNewConversation(final String conversationId) {
        Assert.isTrue(conversationId != null);
        mUiState.onGetOrCreateConversation(conversationId);
    }

    @Override
    // From ContactPickerFragmentHost
    public void onBackButtonPressed() {
        onBackPressed();
    }

    @Override
    // From ContactPickerFragmentHost
    public void onInitiateAddMoreParticipants() {
        System.out.println("jordan,enter onInitiateAddMoreParticipants()");
        if (getSmilMainFragment() != null && !getSmilMainFragment().isHidden()) {
            /*
             * FragmentTransaction transaction =
             * getFragmentManager().beginTransaction();
             * transaction.hide(getSmilMainFragment()); transaction.commit();
             */
            setShowHideContact(true);
            setShowSmileMainContainer(false);
        }
        mUiState.setSmilFlag(true);
        mUiState.onAddMoreParticipants();
        mUiState.setSmilFlag(false);
    }

    @Override
    // From ContactPickerFragmentHost
    public void onParticipantCountChanged(final boolean canAddMoreParticipants) {
        mUiState.onParticipantCountUpdated(canAddMoreParticipants);
    }

    public void setShowSmileMainContainer(boolean show) {
        if (show) {
            mSmilMainFrame.setVisibility(View.VISIBLE);
        } else {
            mSmilMainFrame.setVisibility(View.GONE);
        }
    }

    @Override
    public void updateSmilUi() {
        System.out.println("jordan,enter updateSmilUi()");
        setShowHideContact(false);
        setShowSmileMainContainer(true);
    }

    public void setShowHideContact(boolean show) {
        if (show && getSmilContactPickFragment() != null) {
            getSmilContactPickFragment().setShowHideContact(true);
        } else if (!show && getSmilContactPickFragment() != null) {
            getSmilContactPickFragment().setShowHideContact(false);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        System.out.println("enter dispatchKeyEvent()");
        if (KeyEvent.KEYCODE_ENTER == event.getKeyCode()
                && KeyEvent.ACTION_DOWN == event.getAction()) {
            System.out
                    .println("keyCode == KeyEvent.KEYCODE_ENTER,will show SmileMainFragment");
            updateSmilUi();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

}
