
package com.sprd.firewall.ui;

import com.sprd.firewall.db.BlackCallsDb;
import com.sprd.firewall.db.BlackColumns;
import com.sprd.firewall.model.BlackNumberEntity;
import com.sprd.firewall.ui.CallFireWallActivity.ViewPagerVisibilityListener;
import com.sprd.firewall.util.BlackEntityUtil;
import com.sprd.firewall.R;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.os.SystemProperties;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.content.ActivityNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import com.sprd.firewall.util.ProgressUtil;
import com.sprd.firewall.util.ProgressUtil.ProgressType;
import android.net.Uri;

public class BlackFragment extends ListFragment implements ViewPagerVisibilityListener,
        OnItemClickListener, ProgressUtil.ProcessTask {
    private static final String TAG = "BlackFragment";

    public static final int ITEM_ADD = Menu.FIRST;

    public static final int ITEM_DELETE = Menu.FIRST + 1;

    public static final int ITEM_CONFIRM_DELETE = Menu.FIRST + 2;

    public static final int ITEM_CANCEL = Menu.FIRST + 3;

    private static final int MENU_DELETE = 0;

    private static final int MENU_EDIT = 1;

    private static final int MENU_DETAIL = 2;

    /* SPRD: add for bug 499959 @{ */
    public static final int NONE_DIALOG = -1;
    public static final int DELETE_ALL_DIALOG = 0;
    public static final int DELETE_DIALOG = 1;
    public static final int DETAIL_DIALOG = 2;
    public static final String DIALOG_TYPE = "dialog_type";
    /* @} */

    private boolean mDeleteState = false;

    private CheckBox mSelectAll;

    private Bundle mMarkForDelete = new Bundle();

    private boolean mMonitorSelectAll = false;

    private boolean mBatchOperation = false;

    private int mCurrentCursorCount = 0;

    private BlackListAdapter mAdapter;

    /* SPRD: add for bug 499959 @{ */
    private AlertDialog.Builder mDeleteAllBuilder;
    private AlertDialog.Builder mDeleteBuilder;
    private AlertDialog.Builder mDetailBuilder;
    private int mDialogType = NONE_DIALOG;
    private BlackNumberEntity mNumberEntity;
    /* @} */

    private Context mContext;

    private static final int SMS_SHIFT = 0;
    private static final int CALL_SHIFT = 1;
    private static final int VT_SHIFT = 2;

    private static final int SMS_SELECT = 1 << SMS_SHIFT;
    private static final int CALL_SELECT = 1 << CALL_SHIFT;
    private static final int VT_SELECT = 1 << VT_SHIFT;

    private static final int REQUEST_CODE_BLACK_EDIT = 1;
    // SPRD:add newfeature for bug423428
    private static final String MULTI_PICK_CONTACTS_ACTION = "com.android.contacts.action.MULTI_TAB_PICK";
    private static final int REQUEST_CODE_PICK = 99;

    private MenuItem mAddMenuItem;
    private MenuItem mMenuDelete;
    private MenuItem mMenuEdit;
    private MenuItem mMenuDetail;

    private boolean mDeleteDone = false;

    private View mSelectAllLinearLayout;
    private View mDivider;
    private TextView mSelectAllTextView;
    private MenuItem mDeleteMenuItem;
    private MenuItem mAddCallMenuItem;
    private MenuItem mAddSmsMenuItem;
    private MenuItem mAddCallWithSmsMenuItem;
    private MenuItem mDeleteConfirmMenuItem;
    private MenuItem mCancelMenuItem;

    /* SPRD: add for bug517572 @{ */
    /**
     * Used with LoaderManager.
     */
    private static int LOADER_ID_BLACK_NUMBER = 1;
    private final LoaderManager.LoaderCallbacks<Cursor> mBlackNumberLoaderListener =
            new BlackNumberLoaderListener();
    /* @} */

    private static final Uri baseUri = BlackColumns.BlackMumber.CONTENT_URI;
    ProgressUtil mProgressUtil;
    private int mBatchAddCount = 0;
    private Intent mData;

    @Override
    public void onVisibilityChanged(boolean visible) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
            /* SPRD: add for bug 558428 @{ */
            mContext = activity;
            drawList();
            /* @} */
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setRetainInstance(true);
        mContext = this.getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        /* SPRD: add for bug 499959 @{ */
        outState.putInt(DIALOG_TYPE, mDialogType);
        /* @} */
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View listLayout = inflater.inflate(R.layout.black_list, container, false);
        mSelectAllLinearLayout = listLayout.findViewById(R.id.selecte_all_layout);
        mDivider = listLayout.findViewById(R.id.divider);
        mSelectAll = (CheckBox) listLayout.findViewById(R.id.selete_all);
        mSelectAllTextView = (TextView) listLayout.findViewById(R.id.selete_all_text);

        return listLayout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        getListView().setItemsCanFocus(true);
        getListView().setOnCreateContextMenuListener(this);
        getListView().setOnItemClickListener(this);
        mSelectAll.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mMonitorSelectAll = true;
                return false;
            }
        });

        mSelectAll.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSelectAllTextView.setText(R.string.blackCalls_all_button_Unselected);
                    if (mMonitorSelectAll) {
                        Cursor cur = getBlackListCursor();
                        try {
                            if (cur.moveToFirst()) {
                                do {
                                    mMarkForDelete.putBoolean(cur.getString(cur
                                            .getColumnIndex(BlackColumns.BlackMumber._ID)), true);
                                } while (cur.moveToNext());
                            }
                        } finally {
                            if (cur != null) {
                                cur.close();
                            }
                        }
                        drawList();
                        mMonitorSelectAll = false;
                    }

                } else {
                    mSelectAllTextView.setText(R.string.blackCalls_all_button_Selected);
                    if (mMonitorSelectAll) {
                        mMarkForDelete.clear();
                        drawList();
                        mMonitorSelectAll = false;
                    }
                }
                setDoneMenu(isChecked);
            }
        });
        drawList();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated");
        // TODO Auto-generated method stub 245
        super.onActivityCreated(savedInstanceState);
        // SPD:add for bug451206
        mContext = this.getActivity();

        /* SPRD: add for bug 499959 @{ */
        if (savedInstanceState != null) {
            showDialogByType(savedInstanceState.getInt(DIALOG_TYPE, NONE_DIALOG));
        }
        /* @} */
    }

    @Override
    public void onStart() {
        // TODO Auto-generated method stub
        Log.i(TAG, "onStart");
        super.onStart();
        if (mProgressUtil != null&&mProgressUtil.ismProgressRun()) {
            mProgressUtil.initProgressDialog(ProgressType.NO_TYPE);
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        /* SPRD: add for bug517572 @{ */
        if (getLoaderManager().getLoader(LOADER_ID_BLACK_NUMBER) == null) {
            getLoaderManager().initLoader(LOADER_ID_BLACK_NUMBER, null,
                    mBlackNumberLoaderListener);

        }
        /* @} */
        /* SPRD: add for bug505761 @{ */
        int markForDeleteCount = mMarkForDelete.size();
        mSelectAll.setChecked(mCurrentCursorCount != 0
                && !refershMarkForDelete()
                && mCurrentCursorCount <= markForDeleteCount);
        /* @} */
        setDoneMenu(mMarkForDelete.size() > 0);
        /* SPRD: add for bug520785 to refresh black list UI if there is no black list @{ */
        if (mCurrentCursorCount == 0) {
            mDeleteState = false;
            Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
        drawList();
        /* @} */
        super.onResume();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
        if (mProgressUtil != null && mProgressUtil.ismProgressRun()) {
            mProgressUtil.disMissProgressDailog();
        }
        else {
            mProgressUtil = null;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        if (mAdapter != null && mAdapter.getCursor() != null) {
            mAdapter.getCursor().close();
        }
    }

    private void drawList() {
        if (mSelectAllLinearLayout != null) {
            if (mDeleteState) {
                mSelectAllLinearLayout.setVisibility(View.VISIBLE);
            } else {
                mSelectAllLinearLayout.setVisibility(View.GONE);
            }
            if (mDivider != null) {
                if (mDeleteState) {
                    mDivider.setVisibility(View.VISIBLE);
                } else {
                    mDivider.setVisibility(View.GONE);
                }
            }
            reSetSelectAllState();
        }

        /* SPRD: add for bug517572 @{ */
        if (mAdapter == null) {
            mAdapter = new BlackListAdapter(mContext, null);
            setListAdapter(mAdapter);
        } else if (getActivity() != null
                && getLoaderManager().getLoader(LOADER_ID_BLACK_NUMBER) != null) {
            getLoaderManager().getLoader(LOADER_ID_BLACK_NUMBER).forceLoad();
        }
        /* @} */
    }

    private void reSetSelectAllState() {
        if (mCurrentCursorCount != 0 && mCurrentCursorCount == mMarkForDelete.size()) {
            mSelectAll.setChecked(true);
        } else {
            mSelectAll.setChecked(false);
        }
    }

    private class BlackListAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        private BlackNumberEntity member = new BlackNumberEntity();

        public BlackListAdapter(Context context, Cursor c) {
            super(context, c);
            mInflater = LayoutInflater.from(context); // initialize mInflater
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder;
            holder = new ViewHolder();

            holder.select = (CheckBox) view.findViewById(R.id.select);
            holder.select.setFocusable(false);
            holder.select.setFocusableInTouchMode(false);
            holder.select.setClickable(false);

            holder.phone_number = (TextView) view.findViewById(R.id.phone_number);
            holder.name = (TextView) view.findViewById(R.id.phone_name);
            holder.block_sms = (ImageView) view.findViewById(R.id.image_block_sms);
            holder.block_call = (ImageView) view.findViewById(R.id.image_block_call);
            holder.divider = (View) view.findViewById(R.id.divider02);

            if (mDeleteState) {
                holder.select.setVisibility(View.VISIBLE);
            } else {
                holder.select.setVisibility(View.GONE);
            }

            BlackEntityUtil.transform(member, cursor);

            String mId = member.getId() + "";
            if (mMarkForDelete.containsKey(mId)) {
                holder.select.setChecked(true);
            } else {
                holder.select.setChecked(false);
            }

            holder.phone_number.setText(member.getNumber());
            if ((member.getName() != null)
                    && (!TextUtils.isEmpty(member.getName()))) {
                holder.name.setText(member.getName());
            } else {
                holder.name.setText(member.getNumber());
            }

            int type = member.getType();

            if ((SMS_SELECT & type) == SMS_SELECT) {
                holder.block_sms.setImageResource(R.drawable.block_type_list_sms_selected);
            } else {
                holder.block_sms.setImageResource(R.drawable.block_type_list_sms_default);
            }
            if ((CALL_SELECT & type) == CALL_SELECT) {
                holder.block_call.setImageResource(R.drawable.block_type_list_call_selected);
            } else {
                holder.block_call.setImageResource(R.drawable.block_type_list_call_default);
            }

            view.setTag(holder);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View convertView;
            convertView = mInflater.inflate(R.layout.black_list_item, null);
            return convertView;
        }

        class ViewHolder {
            CheckBox select;
            TextView phone_number;
            TextView name;
            ImageView block_sms;
            ImageView block_call;
            View divider;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.black_options, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        mAddMenuItem = menu.findItem(R.id.black_menu_add);
        mDeleteMenuItem = menu.findItem(R.id.black_menu_delete);
        /* SPRD:add newfeature for bug423428@{ */
        mAddCallMenuItem = menu.findItem(R.id.add_call_menu);
        mAddSmsMenuItem = menu.findItem(R.id.add_sms_menu);
        mAddCallWithSmsMenuItem = menu.findItem(R.id.add_call_with_sms_menu);
        /* @} */
        mDeleteConfirmMenuItem = menu.findItem(R.id.black_menu_delete_confirm);
        mCancelMenuItem = menu.findItem(R.id.black_menu_cancel);
        if (mAddMenuItem == null) {
            return;
        }

        setMenuItemState();
    }

    private void setMenuItemState() {
        if (mAddMenuItem == null || mDeleteMenuItem == null || mDeleteConfirmMenuItem == null
                || mCancelMenuItem == null) {
            return;
        }
        if (mDeleteState) {
            mAddMenuItem.setVisible(false);
            /* SPRD:add newfuture for bug423428@{ */
            mAddCallMenuItem.setVisible(false);
            mAddSmsMenuItem.setVisible(false);
            mAddCallWithSmsMenuItem.setVisible(false);
            /* @} */
            mDeleteMenuItem.setVisible(false);
            mDeleteConfirmMenuItem.setVisible(true);
            mDeleteConfirmMenuItem.setEnabled(false);
            /* SPRD:add newfeature for bug482850@{ */
            if (mMarkForDelete.size() > 0) {
                setDoneMenu(true);
            }
            /* @} */
            mCancelMenuItem.setVisible(true);
        } else {
            mAddMenuItem.setVisible(true);
            mDeleteMenuItem.setVisible(true);
            mAddCallMenuItem.setVisible(true);
            mAddSmsMenuItem.setVisible(true);
            mAddCallWithSmsMenuItem.setVisible(true);
            mDeleteConfirmMenuItem.setVisible(false);
            mCancelMenuItem.setVisible(false);
            Cursor cur = getBlackListCursor();
            mDeleteMenuItem.setEnabled(cur != null && cur.getCount() > 0);
            if (cur != null) {
                cur.close();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* SPRD:add newfeature for bug423428@{ */
        Intent intentPick = new Intent(MULTI_PICK_CONTACTS_ACTION);
        /* @} */
        /* SPRD:add newfeature for bug459942@{ */
        SharedPreferences sp = mContext.getSharedPreferences("isBatchOperating",
                Context.MODE_PRIVATE);
        mBatchOperation = sp.getBoolean("batchOperation", false);
        /* @} */
        switch (item.getItemId()) {
            case R.id.black_menu_add:
                /** SPRD: The bug for 459942 */
                if (mBatchOperation) {
                    Toast.makeText(mContext, getString(R.string.adding_blacklist_hold_on),
                            Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(this.getActivity(), BlackCallsListAddActivity.class));
                }
                break;
            /* SPRD:add newfeature for bug423428@{ */
            case R.id.add_sms_menu:
                /** SPRD: The bug for 459942 */
                if (mBatchOperation) {
                    Toast.makeText(mContext, getString(R.string.adding_blacklist_hold_on),
                            Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        intentPick.putExtra("blackcall_type", "1");
                        intentPick.putExtra("cascading",
                                new Intent(MULTI_PICK_CONTACTS_ACTION)
                                        .setType(Phone.CONTENT_ITEM_TYPE));
                        startActivityForResult(intentPick, REQUEST_CODE_PICK);
                    } catch (ActivityNotFoundException e) {
                        Log.i(TAG, "Contacts has been stopped.");
                        Toast.makeText(mContext, getString(R.string.Contact_error),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.add_call_menu:
                /** SPRD: The bug for 459942 */
                if (mBatchOperation) {
                    Toast.makeText(mContext, getString(R.string.adding_blacklist_hold_on),
                            Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        intentPick.putExtra("blackcall_type", "2");
                        intentPick.putExtra("cascading",
                                new Intent(MULTI_PICK_CONTACTS_ACTION)
                                        .setType(Phone.CONTENT_ITEM_TYPE));
                        startActivityForResult(intentPick, REQUEST_CODE_PICK);
                    } catch (ActivityNotFoundException e) {
                        Log.i(TAG, "Contacts has been stopped.");
                        Toast.makeText(mContext, getString(R.string.Contact_error),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.add_call_with_sms_menu:
                /** SPRD: The bug for 459942 */
                if (mBatchOperation) {
                    Toast.makeText(mContext, getString(R.string.adding_blacklist_hold_on),
                            Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        intentPick.putExtra("blackcall_type", "3");
                        intentPick.putExtra("cascading",
                                new Intent(MULTI_PICK_CONTACTS_ACTION)
                                        .setType(Phone.CONTENT_ITEM_TYPE));
                        startActivityForResult(intentPick, REQUEST_CODE_PICK);
                    } catch (ActivityNotFoundException e) {
                        Log.i(TAG, "Contacts has been stopped.");
                        Toast.makeText(mContext, getString(R.string.Contact_error),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            /* @} */
            case R.id.black_menu_delete:
                /** SPRD: The bug for 459942 */
                if (mBatchOperation) {
                    Toast.makeText(mContext, getString(R.string.adding_blacklist_hold_on),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Cursor cur = getBlackListCursor();
                    if (cur != null) {
                        if (cur.getCount() > 0) {
                            mDeleteState = true;
                            drawList();
                        }
                        cur.close();
                    }
                    getActivity().invalidateOptionsMenu();
                }
                break;
            case R.id.black_menu_delete_confirm:
                // SPRD: add for bug 499959
                showDialogByType(DELETE_ALL_DIALOG);
                break;
            case R.id.black_menu_cancel:
                mDeleteState = false;
                mMarkForDelete.clear();
                drawList();
                getActivity().invalidateOptionsMenu();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * SPRD: add for bug 499959 @{
     */
    private void showDialogByType(int type){
        if (type == NONE_DIALOG) {
            return;
        }
        if (type == DELETE_ALL_DIALOG) {
            /* SPRD: modify for bug 526611 @{ */
            if (mDeleteAllBuilder == null) {
                mDeleteAllBuilder = new AlertDialog.Builder(mContext);
            }
            mDeleteAllBuilder.setTitle(R.string.confirm_delete_number_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.confirm_delete_number)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mProgressUtil = new ProgressUtil(mContext,
                                    mMarkForDelete.size(),
                                    ProgressType.BLACKLIST_DEL,
                                    BlackFragment.this);
                            mProgressUtil.setMtitleId(R.string.Delete_Blacklist_title);
                            mProgressUtil.execute();
                        }
                    }).setNegativeButton(android.R.string.cancel, null).setCancelable(true)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            mDialogType = NONE_DIALOG;
                        }
                    });
            /* @} */
            mDeleteAllBuilder.create().show();
        } else if (type == DELETE_DIALOG) {
            /* SPRD: modify for bug 526611 @{ */
            if (mDeleteBuilder == null) {
                mDeleteBuilder = new AlertDialog.Builder(mContext);
            }
            mDeleteBuilder.setTitle(R.string.confirm_delete_number_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.confirm_delete_number)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            try {
                                /**
                                 * Delete data from the database
                                 */
                                deleteOneBlack(mNumberEntity.getNumber());
                                updataBlackList();
                                drawList();
                                mDeleteDone = true;
                            } finally {
                                dialog.dismiss();
                            }
                        }
                    }).setNegativeButton(android.R.string.cancel, null).setCancelable(true)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            mDialogType = NONE_DIALOG;
                        }
                    });
            /* @} */
            mDeleteBuilder.create().show();
        } else if (type == DETAIL_DIALOG) {
            String phoneName;
            if (mNumberEntity.getName() == null || TextUtils.isEmpty(mNumberEntity.getName())) {
                phoneName = mNumberEntity.getNumber();
            } else {
                phoneName = mNumberEntity.getName();
            }
            String phoneNumber = mNumberEntity.getNumber();
            /* SPRD: modify for bug 526611 @{ */
            if (mDetailBuilder == null) {
                mDetailBuilder = new AlertDialog.Builder(mContext);
            }
            mDetailBuilder.setTitle(R.string.blackCallsIconLabel)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    mDeleteState = false;
                                }
                            }).setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog,
                                int keyCode, KeyEvent event) {
                            if (KeyEvent.KEYCODE_BACK == keyCode) {
                                dialog.dismiss();
                            }
                            return false;
                        }
                    }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            mDialogType = NONE_DIALOG;
                        }
                    });
            /* @} */
            mDetailBuilder.setMessage(getString(
                    R.string.blackCalls_blacklist_detail, phoneName, phoneNumber));
            mDetailBuilder.create().show();
        }
        mDialogType = type;
    }
    /** @} */

    private void deleteSelected() {
        if (mMarkForDelete.isEmpty()) {
            Toast.makeText(mContext,
                    getString(R.string.blackcalls_delete_phone_tip_error_not_select),
                    Toast.LENGTH_SHORT).show();
        }
        else {
            ContentResolver cr = mContext.getContentResolver();
            String mId;
            Cursor cur = getBlackListCursor();
            try {
                if (cur.moveToFirst()) {
                    do {
                        mId = cur.getString(cur.getColumnIndex(BlackColumns.BlackMumber._ID));
                        if (mMarkForDelete.containsKey(mId)) {
                            String blackNumberForDelete = cur.getString(cur
                                    .getColumnIndex(BlackColumns.BlackMumber.MUMBER_VALUE));
                            cr.delete(BlackColumns.BlackMumber.CONTENT_URI,
                                    BlackColumns.BlackMumber._ID + "='" + mId + "'", null);
                            cr.delete(BlackColumns.BlackMumber.CONTENT_URI,
                                    BlackColumns.BlackMumber.MUMBER_VALUE + "='"
                                            + blackNumberForDelete + "'", null);
                        }
                    } while (cur.moveToNext());
                }
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }
            mDeleteState = false;
        }
    }

    private AlertDialog mAlertDialog = null;

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long itemId) {
        Cursor item = (Cursor) adapterView.getItemAtPosition(position);
        final BlackNumberEntity numberEntity = new BlackNumberEntity();
        BlackEntityUtil.transform(numberEntity, item);

        if (mDeleteState == true) {
            String mId = numberEntity.getId() + "";
            if (mMarkForDelete.containsKey(mId)) {
                mMarkForDelete.remove(mId);
                mSelectAll.setChecked(false);
            } else {
                mMarkForDelete.putBoolean(mId, true);
                if (mMarkForDelete.size() == mCurrentCursorCount) {
                    mSelectAll.setChecked(true);
                }
            }
            if (mMarkForDelete.size() > 0) {
                setDoneMenu(true);
            } else {
                setDoneMenu(false);
            }
            drawList();
        } else {
            Log.d(TAG, "onItemClick else");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (!mDeleteState) {
            menu.setHeaderTitle(R.string.BlaclList_conMenu_title);
            mMenuDelete = menu.add(0, MENU_DELETE, 0, R.string.BlaclList_conMenu_delete);
            mMenuEdit = menu.add(0, MENU_EDIT, 0, R.string.BlaclList_conMenu_edit);
            mMenuDetail = menu.add(0, MENU_DETAIL, 0, R.string.BlackList_conMenu_detail);
        }
    }

    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) aItem.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        if (info == null) {
            return false;
        }
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        /* SPRD: modify for bug 499959 @{ */
        mNumberEntity = new BlackNumberEntity();

        if (aItem == mMenuDelete) {
            BlackEntityUtil.transform(mNumberEntity, cursor);
            showDialogByType(DELETE_DIALOG);
            return true;
        } else if (aItem == mMenuEdit) {
            BlackEntityUtil.transform(mNumberEntity, cursor);
            Log.d(TAG, "Type=" + mNumberEntity.getType());
            Intent intent = new Intent(mContext, BlackCallsListAddActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("Click_BlackCalls_Number", mNumberEntity.getNumber());
            bundle.putInt("Click_BlackCalls_Type", mNumberEntity.getType());
            bundle.putBoolean("Click_BlackCalls_Edit", true);
            bundle.putString("Click_BlackCalls_Name", mNumberEntity.getName());
            intent.putExtras(bundle);
            startActivityForResult(intent, REQUEST_CODE_BLACK_EDIT);
            return true;
        } else if (aItem == mMenuDetail) {
            BlackEntityUtil.transform(mNumberEntity, cursor);
            showDialogByType(DETAIL_DIALOG);
            return true;
        } else {
            return false;
        }
        /* @} */
    }

    private Cursor getBlackListCursor() {
        ContentResolver cr = mContext.getContentResolver();
        Cursor cur;
        cur = cr.query(BlackColumns.BlackMumber.CONTENT_URI, null, BlackColumns.BlackMumber._ID
                + " ) " + " group by " +
                " ( " + BlackColumns.BlackMumber.MUMBER_VALUE, null, null);
        if (cur != null) {
            mCurrentCursorCount = cur.getCount();
        }
        return cur;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent
            data) {
        final int mRequestCode = requestCode;
        final int mResultCode = resultCode;
        mData = data;

        // SPD:modified for bug451206
        if (mData == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_BLACK_EDIT) {
           Bundle bunde = mData.getExtras();
           String add_numberString = bunde.getString("phonenumber");
           int add_type = bunde.getInt("type");
           updataBlackList();
        } else {
            int mAddBlacklistCount = 0;
            HashMap<String, String> contacts;
            if ((mData.getExtras().getString("blackcall_type") != null)
                    && (null != mData.getSerializableExtra("result"))) {
                Log.i(TAG, "onActivityResult add");
                contacts = (HashMap<String, String>)mData.getSerializableExtra("result");
                mProgressUtil = new ProgressUtil(mContext,contacts.size(),ProgressType.BLACKLIST_ADD,this);
                mProgressUtil.setMtitleId(R.string.Add_Blacklist_title);
                mProgressUtil.execute();
            }
        }
    }

    private void BatchOperationAddBlackCalls(ArrayList<ContentProviderOperation> operations) {

        ContentResolver cr = mContext.getContentResolver();
        if (!operations.isEmpty()) {
            try {
                cr.applyBatch(BlackColumns.AUTHORITY, operations);
            } catch (OperationApplicationException e) {
                // TODO: handle exception
                Log.e(TAG, "Version consistency failed");
            } catch (RemoteException e) {
                // TODO: handle exception
                Log.e(TAG, "Add blacklist is failed");
            }
        }
    }

    /* SPRD: modify for bug 558428 @{ */
    public void resetTabSwitchState(boolean needDrawList) {
        mDeleteState = false;
        mMarkForDelete.clear();
        setMenuItemState();
        if (needDrawList) {
            drawList();
        }
    }
    /* @} */

    public boolean getDeleterState() {
        return mDeleteState;
    }

    public boolean getDeleteDone() {
        return mDeleteDone;
    }

    public void setDeleteDone(boolean b) {
        mDeleteDone = b;
    }

    public void setDoneMenu(boolean enabled) {
        if (mDeleteConfirmMenuItem == null) {
            return;
        }
        if (enabled) {
            mDeleteConfirmMenuItem.setEnabled(true);
        } else {
            mDeleteConfirmMenuItem.setEnabled(false);
        }
    }

    public void updataBlackList() {
        Log.i(TAG, "updataBlackList");
        mContext.getContentResolver().notifyChange(baseUri, null);
    }

    private void deleteOneBlack(String number) {
        BlackCallsDb bcdb = new BlackCallsDb(mContext);
        bcdb.DelBlackCalls(number);
    }

    @Override
    public void doInBack(ProgressType type) {
        // TODO Auto-generated method stub

        Log.i(TAG, "doInBack-doInBack type="+type);
        if (mProgressUtil == null) {
            Log.i(TAG, "doInBack-mProgressUtil = null");
            return;
        }

        if (type == ProgressType.BLACKLIST_ADD) {

            HashMap<String, String> contacts;
            contacts = (HashMap<String, String>) mData.getSerializableExtra("result");
            ArrayList<ContentProviderOperation> operations
                    = new ArrayList<ContentProviderOperation>();
            int icount = 0;
            int mAddBlacklistCount = 0;
            Iterator i = contacts.entrySet().iterator();
            ContentResolver cr = mContext.getContentResolver();
            while (i.hasNext()) {

                Map.Entry entry = (Map.Entry) i.next();
                String val;
                if (entry.getValue() == null) {
                    val = entry.getKey().toString();
                } else {
                    val = entry.getValue().toString();
                    mAddBlacklistCount++;
                    mProgressUtil.UpdateProgress(mAddBlacklistCount);
                }

                String key = entry.getKey().toString();
                /* SPRD: modify for bug 501946 @{ */
                cr.delete(BlackColumns.BlackMumber.CONTENT_URI,
                        BlackColumns.BlackMumber.MUMBER_VALUE + "='"
                        + key + "'", null);
                ContentValues values = new ContentValues();
                String normalizedNumber = PhoneNumberUtils.normalizeNumber(key);
                values.put(BlackColumns.BlackMumber.MUMBER_VALUE, key);
                values.put(BlackColumns.BlackMumber.BLOCK_TYPE,
                        Integer.parseInt(mData.getExtras().getString("blackcall_type")));
                values.put(BlackColumns.BlackMumber.NAME, val);
                values.put(BlackColumns.BlackMumber.MIN_MATCH,
                        PhoneNumberUtils.toCallerIDMinMatch(normalizedNumber));
                operations.add(ContentProviderOperation
                        .newInsert(BlackColumns.BlackMumber.CONTENT_URI).withValues(values)
                        .withYieldAllowed(false).build());
                icount++;
                /* @} */
                if (icount >= 20) {
                    BatchOperationAddBlackCalls(operations);
                    operations.clear();
                    icount = 0;
                }
            }
            if (icount > 0) {
                BatchOperationAddBlackCalls(operations);
                operations.clear();
            }
            mData = null;
        }else if (type == ProgressType.BLACKLIST_DEL) {

            ContentResolver cr = mContext.getContentResolver();
            String mId;
            Cursor cur = getBlackListCursor();

            BlackCallsDb bcdb = new BlackCallsDb(mContext);
            int deleteNums = 0;
            try {
                if (cur.moveToFirst()) {
                    do {
                        mId = cur.getString(cur.getColumnIndex(BlackColumns.BlackMumber._ID));
                        if (mMarkForDelete.containsKey(mId)) {
                            String blackNumberForDelete = cur.getString(
                                    cur.getColumnIndex(BlackColumns.BlackMumber.MUMBER_VALUE));
                            cr.delete(BlackColumns.BlackMumber.CONTENT_URI,
                                    BlackColumns.BlackMumber._ID + "='" + mId + "'", null);
                            cr.delete(BlackColumns.BlackMumber.CONTENT_URI,
                                    BlackColumns.BlackMumber.MUMBER_VALUE
                                    + "='" + blackNumberForDelete + "'", null);
                            deleteNums++;
                            mProgressUtil.UpdateProgress(deleteNums);
                        }
                    } while (cur.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "deleteSelected()", e);
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }
            mDeleteState = false;
            mMarkForDelete.clear();
        }
    }

    @Override
    public void doResult() {
        // TODO Auto-generated method stub
        Log.i(TAG, "doResult");

        Activity activity = getActivity();
        if (activity != null) {

            activity.invalidateOptionsMenu();
            drawList();
            updataBlackList();
        }
    }

    /* SPRD: add for bug505761 @{ */
    private boolean refershMarkForDelete() {
        Cursor cur = getBlackListCursor();
        if (cur == null || cur.getCount() == 0) {
            mMarkForDelete.clear();
            Log.i(TAG, "refershMarkForDelete: cur is null");
            return true;
        }

        boolean addFlag = false;
        Bundle tempBundle = new Bundle(mMarkForDelete);

        try {
            if (cur.moveToFirst()) {
                mMarkForDelete.clear();
                do {
                    String strId = cur.getString(cur
                            .getColumnIndex(BlackColumns.BlackMumber._ID));
                    if (tempBundle.containsKey(strId)) {
                        mMarkForDelete.putBoolean(strId, true);
                    } else {
                        addFlag = true;
                    }
                } while (cur.moveToNext());
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }

        return addFlag;
    }
    /* @} */

    /**
     * SPRD: add for bug517572 @{
     */
    private class BlackNumberLoaderListener implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), BlackColumns.BlackMumber.CONTENT_URI, null,
                    BlackColumns.BlackMumber._ID + " ) " + " group by " +
                            " ( " + BlackColumns.BlackMumber.MUMBER_VALUE, null,
                    null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                mCurrentCursorCount = data.getCount();
            }
            mAdapter.changeCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mCurrentCursorCount = 0;
            mAdapter.changeCursor(null);
        }
    }
    /** @} */
}
