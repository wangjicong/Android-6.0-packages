
package com.android.mmsfolderview.data;

import com.android.mmsfolderview.parser.XmlDomParser;
import com.android.mmsfolderview.parser.data.TimeQuantumDataBuilder;
import com.android.mmsfolderview.parser.data.TimeQuantumItem;
import com.android.mmsfolderview.parser.process.TimeQuantumProcess;
import com.android.mmsfolderview.ui.SortDisplayController;
import com.android.mmsfolderview.ui.SortMsgListActivity;
import com.android.mmsfolderview.ui.SortMsgListFragment;
import com.android.mmsfolderview.util.MmsUtils;
import com.android.mmsfolderview.util.OsUtil;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class SortMsgListData implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "SortMsgListData";
    private final SortMsgListDataListener mListener;
    private final Context mContext;

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private int sortType;
    private int subIdShow;

    public int getSortType() {
        int sortType;
        if (SortDisplayController.getInstance().isSimSms()) {
            sortType = mPreferences.getInt(SortMsgDataCollector.SIM_MESSAGE_SORT_TYPE,
                    SortMsgDataCollector.MSG_UNKNOW);
        } else {
            sortType = mPreferences.getInt(SortMsgDataCollector.MESSAGE_SORT_TYPE,
                    SortMsgDataCollector.MSG_UNKNOW);
        }
        return sortType;
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
        /** Add for 557268 begin */
        if (Integer.MAX_VALUE == MmsUtils.tanslateSubIdToPhoneId(mContext, subIdShow)) {
            subIdShow = 0;
        }
        /** Add for 557268 end */
        return subIdShow;
    }

    public interface SortMsgListDataListener {
        public void onSortMsgListCursorUpdated(SortMsgListData data, Cursor cursor);
    }

    public SortMsgListData(final Context context, final SortMsgListDataListener listener) {
        mListener = listener;
        mContext = context;
    }

    public void initLoader(final LoaderManager loaderManager, int loaderId) {
        loaderManager.initLoader(loaderId, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        ensureSharedPrefAvail();
        Loader<Cursor> loader = null;
        String where = null;
        String order = null;
        // bug 489223: add for Custom XML SQL Query begin
        Uri uri = null;
        String[] projections = SortMsgDataCollector.getMessageListViewProjection();
        String[] selectionArgs = null;
        // bug 489223: add for Custom XML SQL Query end

        final int sortType = getSortType();
        final int subIdShow = getSubIdShow();
        if ((sortType != SortMsgDataCollector.MSG_UNKNOW)) {
            where = SortMsgDataCollector.getSortTypeQueryWhere(sortType);
            // bug 489223: add for Custom XML SQL Query begin
            uri = SortMsgDataCollector.MESSAGE_LIST_VIEW_URI;
            if (SortDisplayController.getInstance().isTimeQuqntum()) {
                TimeQuantumItem item = XmlDomParser.getItemList().get(sortType);
                if (item.mUri != null) {
                    uri = item.mUri;
                }
                if (item.mProjects != null && item.mProjects.length != 0) {
                    projections = item.mProjects;
                }

                selectionArgs = TimeQuantumDataBuilder.buildSelectionArgs(item);
            }
            if (SortDisplayController.getInstance().isSimSms()) {
                uri = SortMsgDataCollector.SIM_MESSAGE_LIST_VIEW_URI;
                SortDisplayController.getInstance().setItf(new SimPrepare());
                Uri simSmsUri = Uri.parse("content://sms/icc_load");
                if (!OsUtil.hasSmsPermission()) {
                    if (mContext instanceof SortMsgListActivity) {
                        OsUtil.requestMissingPermission((SortMsgListActivity) mContext);
                    }
                } else {
                    if (SortDisplayController.getInstance().initEvn(mContext, simSmsUri)) {
                        Log.d(TAG, "initEnv is true");
                        //mContext.getContentResolver().delete(SortMsgDataCollector.CLEAR_SIM_MESSAGES_URI, null, null);
                    }
                }
                //   Bug_app providers implements ,
                // Notes  Delete all simmessage , init from telephony.Provider ,  query View;
            }

            if (((sortType != SortMsgDataCollector.MSG_BOX_DRAFT) || !SortDisplayController
                    .getInstance().isFolderView())
                    // bug 489223: add for Custom XML SQL Query end
                    && (subIdShow != SortMsgDataCollector.SHOW_ALL_MESSAGE)) {
                if (TextUtils.isEmpty(where)) {
                    where = "sub_id=" + subIdShow;
                } else {
                    where = where + " AND (" + "sub_id=" + subIdShow + ")";
                }
            }
            if (SortDisplayController.getInstance().isSimSms()) {
                order = mPreferences.getString(SortMsgDataCollector.getMsgOrderKey(sortType), "");
            }
            order = mPreferences.getString(SortMsgDataCollector.getMsgOrderKey(sortType), "");
            Log.d(TAG, "onCreateLoader where = " + where + ", loaderid = " + arg0 + "  projections = " + projections);
            // bug 489223: add for Custom XML SQL Query begin
            loader = new CursorLoader(mContext, uri, projections, where, selectionArgs, order);
            // bug 489223: add for Custom XML SQL Query end
        } else {
            Log.w(TAG, "The MESSAGE_SORT_TYPE data in Bundle is not valid");
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
        if (arg1 != null) {
            Log.d(TAG, "onLoadFinished");
            mListener.onSortMsgListCursorUpdated(this, arg1);
        } else {
            Log.w(TAG, "The Loader or Cursor is null");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        Log.d(TAG, "onLoaderReset");
    }

    public void restartLoader(final LoaderManager loaderManager, int loaderId) {
        loaderManager.restartLoader(loaderId, null, this);
    }

    private void ensureSharedPrefAvail() {
        if (mPreferences == null) {
            mPreferences = mContext.getSharedPreferences(SortMsgListActivity.PREFERENCES_NAME,
                    mContext.MODE_PRIVATE);
        }
        if (mEditor == null) {
            mEditor = mPreferences.edit();
        }
    }

}


class SimPrepare implements SortDisplayController.IPrepare {

    public SimPrepare() {

    }

    public boolean initEnv(Context ctx, Object obj) {
        if (ctx == null) {
            return false;
        }

        // telephony providers Database implements
//        Uri uri = Uri.parse("content://sms/icc_load");
        Uri uri = (Uri)obj;
        Cursor cursor = ctx.getContentResolver().query(uri, new String[]{"_id"}, null, null, null);
        if (cursor == null) {
            return false;
        } else {
            cursor.close();
            cursor = null;
            return true;
        }

    }
}