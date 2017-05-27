
package com.android.mmsfolderview.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ActionMode.Callback;
import android.widget.CheckedTextView;

import com.android.mmsfolderview.R;
import com.android.mmsfolderview.data.SortMsgDataCollector;
import com.android.mmsfolderview.util.IntentUiUtils;
import com.android.mmsfolderview.util.OsUtil;
import com.android.mmsfolderview.util.PhoneUtils;

public abstract class BaseActionBarActivity extends ActionBarActivity {

    protected CustomActionMode mActionMode;
    private Menu mActionBarMenu;
    public static final int REQUEST_CODE_SET_DEFAULT_SMS_APP = 1;
    public static final int REQUEST_CODE_NEW_MESSAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void updateActionBar(ActionBar actionBar) {
        Log.d("tim-1", "BaseActionBarActivity.-- initActionBar");
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OsUtil.hasRequiredPermissions()){
            OsUtil.requestMissingPermission(this);
        } else {
            startCommonServiceToRefreshParticipants();
        }
    }
    private void startCommonServiceToRefreshParticipants() {
        try {
            Intent intent = IntentUiUtils.getFolderViewMessagingCommServiceIntent();
            intent.putExtra(SortMsgDataCollector.KEY_COMM, SortMsgDataCollector.KEY_PARTICIPANTS_REFRESH);
            BaseActionBarActivity.this.startService(intent);
        } catch (Exception e) {
            Log.e("tim_V6_noti", "start service error, action="
                    + SortMsgDataCollector.ACTION_FOLDER_VIEW_MESSAGING_COMM, e);
        }
    }
    // @Override
    // public boolean onOptionsItemSelected(MenuItem item) {
    // switch (item.getItemId()) {
    // case android.R.id.home:
    // finish();
    // return true;
    // }
    // return false;
    // }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && mActionMode != null) {
            dismissActionMode();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        Log.d("tim", "onCreateOptionsMenu:mActionMode=" + mActionMode);
        mActionBarMenu = menu;
        if (mActionMode != null && mActionMode.getCallback().onCreateActionMode(mActionMode, menu)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        Log.d("tim", "onPrepareOptionsMenu:mActionMode=" + mActionMode);
        mActionBarMenu = menu;
        if (mActionMode != null && mActionMode.getCallback().onPrepareActionMode(mActionMode, menu)) {
            return true;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        if (mActionMode != null
                && mActionMode.getCallback().onActionItemClicked(mActionMode, menuItem)) {
            return true;
        }

        switch (menuItem.getItemId()) {
            case android.R.id.home:
                if (mActionMode != null) {
                    dismissActionMode();
                } else {
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public ActionMode startActionMode(final ActionMode.Callback callback) {
        mActionMode = new CustomActionMode(callback);
        Log.d("tim", "supportInvalidateOptionsMenu-begin");
        supportInvalidateOptionsMenu();
        Log.d("tim", "supportInvalidateOptionsMenu-end");
        invalidateActionBar();
        return mActionMode;
    }

    public void dismissActionMode() {
        Log.d("tim", "dismissActionMode-------------");
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
            invalidateActionBar();
        }
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    protected ActionMode.Callback getActionModeCallback() {
        if (mActionMode == null) {
            return null;
        }

        return mActionMode.getCallback();
    }

    /**
     * Receives and handles action bar invalidation request from sub-components
     * of this activity.
     * <p>
     * Normally actions have sole control over the action bar, but in order to
     * support seamless transitions for components such as the full screen media
     * picker, we have to let it take over the action bar and then restore its
     * state afterwards
     * </p>
     * <p>
     * If a fragment does anything that may change the action bar, it should
     * call this method and then it is this method's responsibility to figure
     * out which component "controls" the action bar and delegate the updating
     * of the action bar to that component
     * </p>
     */
    public final void invalidateActionBar() {
        if (mActionMode != null) {
            mActionMode.updateActionBar(getSupportActionBar());
        } else {
            updateActionBar(getSupportActionBar());
        }
    }

    public void UpdateSelectMessageCount(int cnt) {
        if (mActionMode != null) {
            String string = getString(R.string.have_select_message);
            String haveSelected = String.format(string, cnt);
            getSupportActionBar().setTitle(haveSelected);
        }
    }

    /**
     * Custom ActionMode implementation which allows us to just replace the
     * contents of the main action bar rather than overlay over it
     */
    private class CustomActionMode extends ActionMode {
        private CharSequence mTitle;
        private CharSequence mSubtitle;
        private View mCustomView;
        private final Callback mCallback;

        public CustomActionMode(final Callback callback) {
            mCallback = callback;
        }

        @Override
        public void setTitle(final CharSequence title) {
            mTitle = title;
        }

        @Override
        public void setTitle(final int resId) {
            mTitle = getResources().getString(resId);
        }

        @Override
        public void setSubtitle(final CharSequence subtitle) {
            mSubtitle = subtitle;
        }

        @Override
        public void setSubtitle(final int resId) {
            mSubtitle = getResources().getString(resId);
        }

        @Override
        public void setCustomView(final View view) {
            mCustomView = view;
        }

        @Override
        public void invalidate() {
            Log.d("tim", "invalidate-------------");
            invalidateActionBar();
        }

        @Override
        public void finish() {
            Log.d("tim", "finish-------------");
            mActionMode = null;
            mCallback.onDestroyActionMode(this);
            supportInvalidateOptionsMenu();
            invalidateActionBar();
        }

        @Override
        public Menu getMenu() {
            return mActionBarMenu;
        }

        @Override
        public CharSequence getTitle() {
            return mTitle;
        }

        @Override
        public CharSequence getSubtitle() {
            return mSubtitle;
        }

        @Override
        public View getCustomView() {
            return mCustomView;
        }

        @Override
        public MenuInflater getMenuInflater() {
            return BaseActionBarActivity.this.getMenuInflater();
        }

        public Callback getCallback() {
            return mCallback;
        }

        public void updateActionBar(final ActionBar actionBar) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setTitle("ddddd");
            mActionMode.getCallback().onPrepareActionMode(mActionMode, mActionBarMenu);
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(
                    R.color.delete_action_bar_background_color)));
             actionBar.setHomeAsUpIndicator(R.drawable.ic_cancel_small_light);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.show();
        }
    }

    protected void createDeleteMessageDialog(int titleId) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(titleId)
                .setMessage(R.string.delete_message_confirmation_dialog_text)
                .setPositiveButton(R.string.delete_message_confirmation_button,
                        new OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                confirmDeleteMessage();
                            }
                        }).setNegativeButton(android.R.string.cancel, null);
        builder.create().show();

    }

    abstract protected void confirmDeleteMessage();
}
