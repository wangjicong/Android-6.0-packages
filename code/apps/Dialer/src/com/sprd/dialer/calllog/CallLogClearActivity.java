package com.sprd.dialer.calllog;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.Manifest.permission;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.CallLog.Calls;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dialer.R;
import com.android.dialer.calllog.CallLogQueryHandler;
import com.android.dialer.util.DialerUtils;
import com.google.common.collect.Lists;
import android.net.Uri;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;

import com.sprd.dialer.CallLogFilterFragment;
import com.sprd.dialer.calllog.CallLogClearDialog;
import com.sprd.dialer.utils.SourceUtils;

import android.app.Dialog;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;

public class CallLogClearActivity extends ListActivity implements CallLogClearColumn {
    private static final String TAG = "CallLogClearActivity";
    // SPRD: add for bug544185
    private static final String KEY_CHECKED_ITEM_ID = "key_checked_item_id";

    private static final Executor sDefaultExecute = Executors.newCachedThreadPool();
    private static final String[] EMPTY_ARRAY = new String[0];
    private static final Uri CONTENT_URI = Uri.parse("content://call_log/calls");

    protected static final int MENU_OK = Menu.FIRST + 1;
    protected static final int MENU_CANCLE = Menu.FIRST;

    //filter call log by call log type (outgoing? incoming? missed?)
    private int mCallType = CallLogQueryHandler.CALL_TYPE_ALL;
    //filter call log by phoneId
    private int mShowType = CallLogFilterFragment.TYPE_ALL;
    private HashMap<Long, Long> mSelectId;
    private CallLogClearAdapter mAdapter;
    private static final int CALL_PHONE_PERMISSION_REQUEST_CODE = 1;

    CallLogClearDialog mClearDialog = new CallLogClearDialog();
    Dialog mDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_log_clear_activity_ex);

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);

        mCallType = getIntent().getIntExtra(SourceUtils.CALL_LOG_TYPE_EXTRA, CallLogQueryHandler.CALL_TYPE_ALL);
        mShowType = CallLogFilterFragment.getCallLogShowType(this);
        mAdapter = new CallLogClearAdapter(this, R.layout.clear_call_log_list_item_ex, null);
        /* SPRD: modify for bug544185 @{ */
        if (savedInstanceState != null) {
            mSelectId =
                    (HashMap<Long, Long>) savedInstanceState.getSerializable(KEY_CHECKED_ITEM_ID);
        } else {
            mSelectId = new HashMap<Long, Long>();
        }
        /* @} */
        setListAdapter(mAdapter);
        getListView().setItemsCanFocus(true);
        /* SPRD:add for bug512213 @{*/
        if (checkSelfPermission(permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { permission.CALL_PHONE },
                    CALL_PHONE_PERMISSION_REQUEST_CODE);
        } else {
            AsyncQueryThread thread = new AsyncQueryThread(
                    getApplicationContext(), mShowType, mCallType);
            thread.executeOnExecutor(sDefaultExecute);
        }
        /* @} */

        mQueryHandler = new QueryHandler();
        getContentResolver().registerContentObserver(CONTENT_URI, true, mCallLogObserver);
    }

    /**
     * SPRD: modify for bug512213 @{
     */
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        switch (requestCode) {
        case CALL_PHONE_PERMISSION_REQUEST_CODE:
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AsyncQueryThread thread = new AsyncQueryThread(
                        getApplicationContext(), mShowType, mCallType);
                thread.executeOnExecutor(sDefaultExecute);
            } else {
                finish();
            }
            break;
        default:
            break;
        }
    }
    /** @} */

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.changeCursor(null);
            mAdapter = null;
        }
        if (mClearDialog != null) {
            mClearDialog = null;
        }
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        getContentResolver().unregisterContentObserver(mCallLogObserver);
    }

    private QueryHandler mQueryHandler = null;
    private final int DISMISS = 1;

    ContentObserver mCallLogObserver = new ContentObserver(mQueryHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mQueryHandler.removeMessages(DISMISS);
            mQueryHandler.sendEmptyMessage(DISMISS);
        }
    };

    private class QueryHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISMISS:
                    if (mDialog != null) {
                        mDialog.dismiss();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.call_log_clear_options_ex, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem delete_all = menu.findItem(R.id.delete_all);
        final MenuItem delete_select = menu.findItem(R.id.delete_selected);
        final MenuItem selectAll = menu.findItem(R.id.select_all);
        final MenuItem unselectAll = menu.findItem(R.id.unselect_all);
        int listCount = getListAdapter() == null ? 0 : getListAdapter().getCount();
        int selectCount = mSelectId.size();

        Log.d(TAG, "listCount = " + listCount + ", selectCount = " + selectCount);

        delete_all.setVisible(false);
        delete_select.setVisible(selectCount > 0 && listCount > 0);
        selectAll.setVisible(selectCount < listCount && listCount > 0);
        unselectAll.setVisible(selectCount == listCount && listCount > 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case MENU_OK:
                int listCount = getListAdapter() == null ? 0 : getListAdapter().getCount();
                int selectCount = mSelectId.size();
                if (selectCount == listCount && listCount > 0) {
                    Runnable runAll = getClearAllRunnable(getApplicationContext());
                    mDialog = mClearDialog.show(this, runAll, true);
                } else {
                    Runnable runSelect = getClearSelectCallLog(getApplicationContext());
                    mDialog = mClearDialog.show(this, runSelect, false);
                }
                break;
            case MENU_CANCLE:
                finish();
                break;
            case R.id.delete_all:
                int listTotal = getListAdapter() == null ? 0 : getListAdapter().getCount();
                if (mSelectId.size() == 0) {
                    Toast.makeText(getApplicationContext(), R.string.do_not_select,Toast.LENGTH_SHORT).show();
                } else if (mSelectId.size() == listTotal){
                    Runnable runAll = getClearAllRunnable(getApplicationContext());
                    mDialog = mClearDialog.show(this, runAll, true);
                } else {
                    Runnable runselect = getClearSelectCallLog(getApplicationContext());
                    mDialog = mClearDialog.show(this, runselect, true);
                }
                return true;
            case R.id.delete_selected:
                Runnable runSelect = getClearSelectCallLog(getApplicationContext());
                mDialog = mClearDialog.show(this, runSelect, false);
                return true;
            case R.id.select_all:
                AsyncThread selectThread = new AsyncThread();
                selectThread.execute(true);
                return true;
            case R.id.unselect_all:
                AsyncThread unSelectThread = new AsyncThread();
                unSelectThread.execute(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        CheckBox box = (CheckBox) v.findViewById(R.id.call_icon);
        box.setChecked(!box.isChecked());
        boolean checked = box.isChecked();
        Log.d(TAG, "position = " + position + ", id = " + id + ", checked = " + checked);
        if (checked) {
            mSelectId.put(id, id);
        } else {
            mSelectId.remove(id);
        }
    }

    private final class CallLogClearAdapter extends ResourceCursorAdapter {
        public CallLogClearAdapter(Context context, int layout, Cursor cursor) {
            super(context, layout, cursor,FLAG_REGISTER_CONTENT_OBSERVER);
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            ImageView iconView = (ImageView) view.findViewById(R.id.call_type_icon);
            TextView line1View = (TextView) view.findViewById(R.id.line1);
            TextView numberView = (TextView) view.findViewById(R.id.number);
            TextView dateView = (TextView) view.findViewById(R.id.date);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.call_icon);
            checkBox.setFocusable(false);
            checkBox.setClickable(false);

            final long id = c.getLong(ID_COLUMN_INDEX);
            long date = c.getLong(DATE_COLUMN_INDEX);

            String number = c.getString(NUMBER_COLUMN_INDEX);
            String formattedNumber = number;
            String name = c.getString(CALLER_NAME_COLUMN_INDEX);

            if (PhoneNumberUtils.isVoiceMailNumber(context, number)) {
                line1View.setText(R.string.voicemail);
                numberView.setText(formattedNumber);
            } else {
                if (!TextUtils.isEmpty(name)) {
                    line1View.setText(name);
                    numberView.setText(formattedNumber);
                } else {
                    if (!TextUtils.isEmpty(number)) {
                        line1View.setText(number);
                    } else {
                        line1View.setText(R.string.unknown);
                    }

                    numberView.setText("");
                }
            }

            if (iconView != null) {
                int type = c.getInt(CALL_TYPE_COLUMN_INDEX);
                Drawable drawable = getResources().getDrawable(
                        SourceUtils.getDrawableFromCallType(type));
                iconView.setImageDrawable(drawable);
            }

            CharSequence  dateText;
            if(date <= System.currentTimeMillis()) {
                dateText =
                        DateUtils.getRelativeTimeSpanString(date,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.FORMAT_NUMERIC_DATE);
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                dateText = dateFormat.format(date);
            }

            dateView.setText(dateText);

            if (mSelectId.containsKey(id)) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
            }
        }

        @Override
        protected void onContentChanged() {
            super.onContentChanged();
            AsyncQueryThread thread = new AsyncQueryThread(
                    getApplicationContext(),mShowType, mCallType);
            thread.executeOnExecutor(sDefaultExecute);
        }
    }

    private class AsyncQueryThread extends AsyncTask<Void, Void, Cursor> {
        private int aCallType = CallLogQueryHandler.CALL_TYPE_ALL;
        private Context aContext;
        private int aShowType =CallLogFilterFragment.TYPE_ALL;

        public AsyncQueryThread(Context context, int showType) {
            this(context, showType, CallLogQueryHandler.CALL_TYPE_ALL);
        }

        public AsyncQueryThread(Context context, int showType, int callType) {
            aContext = context;
            aShowType = showType;
            aCallType = callType;
        }
        @Override
        protected Cursor doInBackground(Void... params) {
            StringBuffer where = new StringBuffer();
            List<String> args = Lists.newArrayList();
            if (aShowType > CallLogFilterFragment.TYPE_ALL) {
                // Translate slot ID to account ID
                // SPRD: add for bug510314
                SubscriptionInfo subscriptionInfo = DialerUtils
                        .getActiveSubscriptionInfo(aContext, mShowType, false);

                if (subscriptionInfo != null) {
                    String subscription_id = subscriptionInfo.getIccId();
                    where.append(String.format("(%s = ?)", Calls.PHONE_ACCOUNT_ID));
                    args.add(String.valueOf(subscription_id));
                }
            }
            if (aCallType > CallLogQueryHandler.CALL_TYPE_ALL) {
                if (where.length() > 0) {
                    where.append(" AND ");
                }
                where.append(String.format("(%s = ?)", Calls.TYPE));
                args.add(Integer.toString(aCallType));
            }
            /* SPRD: add for bug495439 @{ */
            UserManager userManager = (UserManager) aContext.getSystemService(Context.USER_SERVICE);
            if (!userManager.isSystemUser()) {
                if (where.length() > 0) {
                    where.append(" AND ");
                }
                where.append(String.format("(%s = ?)", Calls.USERS_ID));
                args.add(Integer.toString(Calls.getUserId()));
            }
            /* @} */
            ContentResolver cr = aContext.getContentResolver();
            final String selection = where.length() > 0 ? where.toString() : null;
            final String[] selectionArgs = args.toArray(EMPTY_ARRAY);
            Cursor c = cr.query(Calls.CONTENT_URI, CALL_LOG_PROJECTION, selection, selectionArgs,
                    "_id desc");
            return c;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            if (mAdapter != null) {
                mAdapter.changeCursor(result);
                invalidateOptionsMenu();
                if (mAdapter.isEmpty()) {
                    finish();
                }
            } else {
                result.close();
            }
        }
    };

    private class AsyncThread extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            if (mAdapter != null) {
                int count = mAdapter.getCount();
                for (int i = 0; i < count; i++) {
                    try {
                        if (mAdapter.getCursor() != null && !mAdapter.getCursor().isClosed()) {
                            long id = mAdapter.getItemId(i);
                            if (params[0] == true) {
                                mSelectId.put(id, id);
                            } else {
                                mSelectId.remove(id);
                            }
                        }
                    } catch (CursorIndexOutOfBoundsException ex){
                        Log.e(TAG,"CursorIndexOutOfBoundsException "+ex);
                        break;
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    private Runnable getClearAllRunnable(final Context context) {
        Runnable run = new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();
                StringBuffer where = new StringBuffer();
                List<String> args = Lists.newArrayList();
                if (mCallType > CallLogQueryHandler.CALL_TYPE_ALL) {
                    where.append(String.format("(%s = ?)", Calls.TYPE));
                    args.add(Integer.toString(mCallType));
                }
                String deleteWhere = where.length() > 0 ? where.toString() : null;
                String[] selectionArgs = deleteWhere == null ? null : args.toArray(EMPTY_ARRAY);
                cr.delete(Calls.CONTENT_URI, deleteWhere, selectionArgs);
                mSelectId.clear(); //should clear the map.
            }
        };
        return run;
    }

    private Runnable getClearSelectCallLog(final Context context) {
        Runnable run = new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();
                StringBuffer where = new StringBuffer();
                where.append("calls._id in (");
                Set<Entry<Long, Long>> set = mSelectId.entrySet();
                Iterator<Entry<Long, Long>> iterator = set.iterator();
                boolean first = true;
                while (iterator.hasNext()) {
                    if (!first) {
                        where.append(",");
                    }
                    first = false;
                    Entry<Long, Long> entry = iterator.next();
                    long id = entry.getKey().longValue();
                    where.append(Long.toString(id));
                }
                where.append(")");
                cr.delete(Calls.CONTENT_URI, where.toString(), null);
                mSelectId.clear(); //should clear the map.
            }
        };
        return run;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        invalidateOptionsMenu();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.recentCalls_deleteAll);
        }
    }

    /* SPRD: add for bug544185 @{ */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_CHECKED_ITEM_ID, mSelectId);
    }
    /* @} */
}
