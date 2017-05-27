
package com.sprd.firewall.ui;

import com.sprd.firewall.db.BlackCallsDb;
import com.sprd.firewall.db.BlackColumns;
import com.sprd.firewall.model.BlackCallEntity;
import com.sprd.firewall.ui.CallFireWallActivity.ViewPagerVisibilityListener;
import com.sprd.firewall.util.BlackEntityUtil;
import com.sprd.firewall.util.DateUtil;
import com.sprd.firewall.util.ProgressUtil;
import com.sprd.firewall.util.ProgressUtil.ProgressType;

import com.sprd.firewall.R;
import android.R.integer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Loader;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class CallLogFragment extends ListFragment implements ViewPagerVisibilityListener,
        OnItemClickListener, ProgressUtil.ProcessTask{
    private static final String TAG = "CallLogFragment";

    private static final int MENU_DELETE = 0;

    private static final int MENU_DETAIL = 1;

    private boolean mDeleteState = false;

    private CheckBox mSelectAll;

    private int count = 0;

    private Bundle mMarkForDelete = new Bundle();

    private boolean mMonitorSelectAll = false;

    private int mCurrentCursorCount = 0;

    private BlackListAdapter mAdapter;

    /* SPRD: add for bug 499959 @{ */
    private AlertDialog.Builder mDeleteAllBuilder;
    private AlertDialog.Builder mDeleteBuilder;
    private AlertDialog.Builder mDetailBuilder;
    private int mDialogType = BlackFragment.NONE_DIALOG;
    private BlackCallEntity mCallEntity;
    /* @} */

    private Context mContext;

    private MenuItem mMenuDelete;
    private MenuItem mMenuDetail;

    private View mSelectAllLinearLayout;
    private View mDivider;
    private TextView mSelectAllTextView;
    private MenuItem mDeleteMenuItem;
    private MenuItem mDeleteConfirmMenuItem;
    private MenuItem mCancelMenuItem;

    ProgressUtil mProgressUtil;

    /* SPRD: add for bug540467 @{ */
    /**
     * Used with LoaderManager.
     */
    private static int LOADER_ID_CALL_LOG = 2;
    private final LoaderManager.LoaderCallbacks<Cursor> mCallLogLoaderListener =
            new CallLogLoaderListener();
    /* @} */

    private ContentObserver mObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            drawList();
        }

    };

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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mContext = this.getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // SPRD: add for bug 499959
        outState.putInt(BlackFragment.DIALOG_TYPE, mDialogType);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View listLayout = inflater.inflate(R.layout.black_log_list, container, false);
        mSelectAllLinearLayout = listLayout.findViewById(R.id.Log_selecte_all_layout);
        mDivider = listLayout.findViewById(R.id.divider);
        mSelectAll = (CheckBox) listLayout.findViewById(R.id.Log_selete_all);
        mSelectAllTextView = (TextView) listLayout.findViewById(R.id.Log_selete_all_text);

        return listLayout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setItemsCanFocus(true);
        getListView().setOnCreateContextMenuListener(this);
        getListView().setOnItemClickListener(this);

        mSelectAll.setOnTouchListener(new OnTouchListener() {

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
                            if (cur != null && cur.moveToFirst()) {
                                do {
                                    mMarkForDelete.putBoolean(cur.getString(cur
                                            .getColumnIndex(BlackColumns.BlockRecorder._ID)), true);
                                    count++;
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
                    count = mMarkForDelete.size();
                    if (mMonitorSelectAll) {
                        mMarkForDelete.clear();
                        drawList();
                        mMonitorSelectAll = false;
                    }
                }
                Cursor cur = getBlackListCursor();
                /* SPRD: add for bug540467 @{ */
                try {
                    if (cur == null || (!cur.moveToFirst())) {
                        setDoneMenu(false);
                    } else {
                        setDoneMenu(isChecked || !(count == cur.getCount()));
                        count = 0;
                    }
                } finally {
                    if (cur != null) {
                        cur.close();
                    }
                }
                /* @} */
            }
        });
        drawList();
        mContext.getContentResolver().registerContentObserver(
                BlackColumns.BlockRecorder.CONTENT_URI, true,
                mObserver);
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
            showDialogByType(savedInstanceState.getInt(
                    BlackFragment.DIALOG_TYPE, BlackFragment.NONE_DIALOG));
        }
        /* @} */
    }

    @Override
    public void onStart() {
        // TODO Auto-generated method stub
        Log.i(TAG, "onStart");
        super.onStart();
        if (mProgressUtil != null && mProgressUtil.ismProgressRun()) {
            mProgressUtil.initProgressDialog(ProgressType.NO_TYPE);
        }
    }

    public void onResume() {
        /* SPRD: add for bug540467 @{ */
        if (getLoaderManager().getLoader(LOADER_ID_CALL_LOG) == null) {
            getLoaderManager().initLoader(LOADER_ID_CALL_LOG, null,
                    mCallLogLoaderListener);
        }
        /* @} */

        drawList();
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mProgressUtil != null && mProgressUtil.ismProgressRun()) {
            mProgressUtil.disMissProgressDailog();
        } else {
            mProgressUtil = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null && mAdapter.getCursor() != null) {
            mAdapter.getCursor().close();
        }
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    private void drawList() {
        /* SPRD: modify for bug540467 @{ */
        if (mAdapter == null) {
            mAdapter = new BlackListAdapter(mContext, null);
            setListAdapter(mAdapter);
        } else if (getActivity() != null
                && getLoaderManager().getLoader(LOADER_ID_CALL_LOG) != null) {
            getLoaderManager().getLoader(LOADER_ID_CALL_LOG).forceLoad();
        }
        /* @} */
        if (mSelectAllLinearLayout != null) {
            if (mDeleteState && !getListAdapter().isEmpty()) {
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

        private BlackCallEntity member = new BlackCallEntity();

        public BlackListAdapter(Context context, Cursor c) {
            super(context, c, 0);
            mInflater = LayoutInflater.from(context); // initialize mInflater
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder;
            holder = new ViewHolder();

            holder.select = (CheckBox) view.findViewById(R.id.log_select);
            holder.select.setFocusable(false);
            holder.select.setFocusableInTouchMode(false);
            holder.select.setClickable(false);

            holder.phone_number = (TextView) view.findViewById(R.id.log_phone_number);
            holder.date_time = (TextView) view.findViewById(R.id.log_date_time);
            holder.name = (TextView) view.findViewById(R.id.log_phone_name);
            if (mDeleteState) {
                holder.select.setVisibility(View.VISIBLE);
            } else {
                holder.select.setVisibility(View.GONE);
            }

            BlackEntityUtil.transform(member, cursor);
            String format_time = DateUtil.formatDate(member.getTime(), context);
            String mId = member.getId() + "";

            if (mMarkForDelete.containsKey(mId)) {
                holder.select.setChecked(true);
            } else {
                holder.select.setChecked(false);
            }
            holder.phone_number.setText(member.getNumber());

            int calltype = member.getType();

            BlackCallsDb bcdb = new BlackCallsDb(context);
            String phoneName = bcdb.FindPhoneNamebyNumber(member.getNumber());
            Log.d(TAG, "FindPhoneNamebyNumber:phoneName=" + phoneName);
            if (phoneName == null || TextUtils.isEmpty(phoneName)) {
                holder.name.setText(member.getNumber());
            } else {
                holder.name.setText(phoneName);
            }

            holder.date_time.setText(format_time);

            view.setTag(holder);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View convertView;
            convertView = mInflater.inflate(R.layout.black_log_list_item, null);
            return convertView;
        }

        class ViewHolder {
            CheckBox select;
            TextView phone_number;
            TextView date_time;
            TextView name;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.call_log_options, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mDeleteMenuItem = menu.findItem(R.id.call_log_menu_delete);
        mDeleteConfirmMenuItem = menu.findItem(R.id.call_log_menu_delete_confirm);
        mCancelMenuItem = menu.findItem(R.id.call_log_menu_cancel);

        setMenuItemState();
    }

    private void setMenuItemState() {
        if (mDeleteMenuItem == null || mDeleteConfirmMenuItem == null || mCancelMenuItem == null) {
            return;
        }
        if (mDeleteState) {
            mDeleteMenuItem.setVisible(false);
            mDeleteConfirmMenuItem.setVisible(true);
            /* SPRD:add newfeature for bug482850@{ */
            if (mMarkForDelete.size() > 0) {
                setDoneMenu(true);
            }
            /* @} */
            mCancelMenuItem.setVisible(true);
        } else {
            if (mDeleteMenuItem != null) {
                mDeleteMenuItem.setVisible(true);
                Cursor cur = getBlackListCursor();
                mDeleteMenuItem.setEnabled(cur != null && cur.getCount() > 0);
                if (cur != null) {
                    cur.close();
                }
            }
            mDeleteConfirmMenuItem.setVisible(false);
            mCancelMenuItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.call_log_menu_delete:
                mDeleteState = true;
                drawList();
                getActivity().invalidateOptionsMenu();
                break;
            case R.id.call_log_menu_delete_confirm:
                // SPRD: add for bug 499959
                showDialogByType(BlackFragment.DELETE_ALL_DIALOG);
                break;
            case R.id.call_log_menu_cancel:
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
        if (type == BlackFragment.NONE_DIALOG) {
            return;
        }
        if (type == BlackFragment.DELETE_ALL_DIALOG) {
            /* SPRD: modify for bug 526611 @{ */
            if (mDeleteAllBuilder == null ) {
                mDeleteAllBuilder = new AlertDialog.Builder(mContext);
            }
            mDeleteAllBuilder.setTitle(R.string.CallLog_confirm_delete_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.CallLog_confirm_delete)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mProgressUtil = new ProgressUtil(mContext,
                                    mMarkForDelete.size(),
                                    ProgressType.CALLLOG_DEL,
                                    CallLogFragment.this);
                            mProgressUtil.setMtitleId(R.string.Delete_CallLog_title);
                            mProgressUtil.execute();
                        }
                    }).setNegativeButton(android.R.string.cancel, null).setCancelable(true)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            mDialogType = BlackFragment.NONE_DIALOG;
                        }
                    });
            /* @} */
            mDeleteAllBuilder.create().show();
        } else if (type == BlackFragment.DELETE_DIALOG) {
            /* SPRD: modify for bug 526611 @{ */
            if (mDeleteBuilder == null) {
                mDeleteBuilder = new AlertDialog.Builder(mContext);
            }
            mDeleteBuilder.setTitle(R.string.CallLog_confirm_delete_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.CallLog_confirm_delete)
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            try {
                                BlackCallsDb bcdb = new BlackCallsDb(mContext);
                                bcdb.DelNewBlackLogsFromId(mCallEntity.getId());
                                drawList();
                            } catch (Exception e) {
                            }
                            dialog.dismiss();
                        }
                    }).setNegativeButton(android.R.string.cancel, null).setCancelable(true)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            mDialogType = BlackFragment.NONE_DIALOG;
                        }
                    });
            /* @} */
            mDeleteBuilder.create().show();
        } else if (type == BlackFragment.DETAIL_DIALOG) {
            /* SPRD: modify for bug 526611 @{ */
            String phoneName = null;
            BlackCallsDb bcdb = new BlackCallsDb(mContext);
            phoneName = bcdb.FindPhoneNamebyNumber(mCallEntity.getNumber());
            Log.d(TAG, "onListItemClick:phoneName=" + phoneName);
            if (phoneName == null || TextUtils.isEmpty(phoneName)) {
                phoneName = mCallEntity.getNumber();
            }
            String phoneNumber = mCallEntity.getNumber();
            String blockTime = DateUtil.formatDate(mCallEntity.getTime(), mContext);
            if (mDetailBuilder == null) {
                mDetailBuilder = new AlertDialog.Builder(mContext);
            }
            mDetailBuilder.setTitle(R.string.blackCallsLogIconLabel)
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
                            mDialogType = BlackFragment.NONE_DIALOG;
                        }
                    });
            /* @} */
            mDetailBuilder.setMessage(
                    getString(R.string.blackCalls_calllog_detail, phoneName, phoneNumber,
                    blockTime));
            mDetailBuilder.create().show();
        }
        mDialogType = type;
    }
    /** @} */

    private void deleteSelected() {

        if (mMarkForDelete.isEmpty()) {
            Toast.makeText(mContext, R.string.blackcalls_delete_not_select, Toast.LENGTH_SHORT)
                    .show();

        } else {
            ContentResolver cr = mContext.getContentResolver();
            String mId;
            Cursor cur = getBlackListCursor();
            try {
                if (cur != null && cur.moveToFirst()) {
                    do {
                        mId = cur.getString(cur.getColumnIndex(BlackColumns.BlockRecorder._ID));
                        if (mMarkForDelete.containsKey(mId)) {
                            cr.delete(BlackColumns.BlockRecorder.CONTENT_URI,
                                    BlackColumns.BlockRecorder._ID + "='" + mId + "'", null);
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long itemId) {
        if (mDeleteState == true) {

            Cursor item = (Cursor) adapterView.getItemAtPosition(position);
            /* SPRD: modify for bug 499959 @{ */
            BlackCallEntity callEntity = new BlackCallEntity();
            BlackEntityUtil.transform(callEntity, item);
            String mId = callEntity.getId() + "";
            /* @} */

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
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (!mDeleteState) {
            menu.setHeaderTitle(R.string.BlaclList_conMenu_title);
            mMenuDelete = menu.add(0, MENU_DELETE, 0, R.string.BlaclList_conMenu_delete);
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
        mCallEntity = new BlackCallEntity();
        if (aItem == mMenuDelete) {
            BlackEntityUtil.transform(mCallEntity, cursor);
            showDialogByType(BlackFragment.DELETE_DIALOG);
            return true;
        } else if (aItem == mMenuDetail) {
            BlackEntityUtil.transform(mCallEntity, cursor);
            showDialogByType(BlackFragment.DETAIL_DIALOG);
            return true;
        } else {
            return false;
        }
        /* @} */
    }

    private Cursor getBlackListCursor() {
        ContentResolver cr = mContext.getContentResolver();
        Cursor cur;
        String[] columns = new String[] {
                BlackColumns.BlockRecorder._ID, BlackColumns.BlockRecorder.MUMBER_VALUE,
                BlackColumns.BlockRecorder.CALL_TYPE, BlackColumns.BlockRecorder.BLOCK_DATE,
                BlackColumns.BlockRecorder.NAME
        };
        cur = cr.query(BlackColumns.BlockRecorder.CONTENT_URI, columns, null, null, null);
        if (cur != null) {
            mCurrentCursorCount = cur.getCount();
        }
        return cur;
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

    public boolean getDeleteState() {
        return mDeleteState;
    }

    public void setDoneMenu(boolean enabled) {
        // SPRD bug431973. mDeleteConfirmMenuItem is not null.
        if (mDeleteConfirmMenuItem == null) {
            return;
        }
        if (enabled) {
            mDeleteConfirmMenuItem.setEnabled(true);
        } else {
            mDeleteConfirmMenuItem.setEnabled(false);
        }
    }

    @Override
    public void doInBack(ProgressType type) {
        // TODO Auto-generated method stub
        if (mProgressUtil == null) {
            Log.i(TAG, "doInBack-mProgressUtil = null");
            return;
        }

        if (type == ProgressType.CALLLOG_DEL) {
            ContentResolver cr = mContext.getContentResolver();
            String mId;
            Cursor cur = getBlackListCursor();

            BlackCallsDb bcdb = new BlackCallsDb(mContext);
            int deleteNums = 0;
            try {
                if (cur != null && cur.moveToFirst()) {
                    do {
                        mId = cur.getString(cur.getColumnIndex(BlackColumns.BlackMumber._ID));
                        if (mMarkForDelete.containsKey(mId)) {

                            cr.delete(BlackColumns.BlockRecorder.CONTENT_URI,
                                    BlackColumns.BlockRecorder._ID + "='" +
                                            mId + "'", null);
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
        /* SPRD: add for bug 499943 @{ */
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
        /* @} */
    }

    /**
     * SPRD: add for bug540467 @{
     */
    private class CallLogLoaderListener implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            String[] columns = new String[] {
                    BlackColumns.BlockRecorder._ID, BlackColumns.BlockRecorder.MUMBER_VALUE,
                    BlackColumns.BlockRecorder.CALL_TYPE, BlackColumns.BlockRecorder.BLOCK_DATE,
                    BlackColumns.BlockRecorder.NAME
            };

            return new CursorLoader(getActivity(), BlackColumns.BlockRecorder.CONTENT_URI, columns,
                    null, null, null);
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
