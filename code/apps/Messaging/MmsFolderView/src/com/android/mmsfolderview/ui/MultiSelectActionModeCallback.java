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

package com.android.mmsfolderview.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.mmsfolderview.R;
import com.android.mmsfolderview.ui.SortMsgListActivity.MaxIDs;
import com.android.mmsfolderview.data.SortMsgListData;
import com.android.mmsfolderview.data.SortMsgListItemData;
import com.android.mmsfolderview.util.PhoneUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;

public class MultiSelectActionModeCallback implements Callback {

    public interface Listener {
        void onActionBarDelete(ArrayList<Integer> messages);
        Cursor onActionBarSelectAll();
        void onActionBarHome();
        void onActionBarUpdateMessageCount(int cnt);
        //add for bug 559631 begin
        MaxIDs getMaxIds();
        //add for bug 559631 end
    }

    private final ArrayList<Integer> mSelectedMessages;

    private Listener mListener;
    private MenuItem mSelectMenu;
    private MenuItem mDeleteMenu;
     //Cursor must get it at real time, cursor is fickleness when db change
    private Cursor mCursor;
    private SortMsgListAdapter mAdapter;
    private Context mContext;

    public MultiSelectActionModeCallback(final Listener listener, SortMsgListAdapter adapter) {
        mListener = listener;
        mContext = (Context) listener;
        mAdapter = adapter;
        mSelectedMessages = new ArrayList<Integer>();
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.multi_select_menu, menu);
        mSelectMenu = menu.findItem(R.id.action_select);
        mDeleteMenu = menu.findItem(R.id.action_delete);
        updateActionMenuStatus();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_select:
                selectAllOrNone();
                return true;
            case R.id.action_delete:
                mListener.onActionBarDelete(mSelectedMessages);
                return true;
            case android.R.id.home:
                mListener.onActionBarHome();
                return true;
            default:
                return false;
        }
    }

    private void selectAllOrNone() {
        updateCursor();
        if (mCursor != null && !mCursor.isClosed()) {
            if (mCursor.getCount() == mSelectedMessages.size()) {
                mSelectedMessages.clear();
            } else {
                mSelectedMessages.clear();
                for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
                    Integer msgId = mCursor.getInt(SortMsgListItemData.INDEX_ID);
                    mSelectedMessages.add(msgId);
                }
            }
        }
        updateActionMenuStatus();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        Log.d("tim", "onDestroyActionMode-------------");
        mListener = null;
        mSelectedMessages.clear();
    }

    public void toggleSelect(final SortMsgListData listData, final SortMsgListItemData listItemData) {
        final Integer id = listItemData.getMessageId();
        if (mSelectedMessages.contains(id)) {
            mSelectedMessages.remove(id);
        } else {
            mSelectedMessages.add(id);
        }
        updateActionMenuStatus();
    }

    public boolean isSelected(final int selectedId) {
        return mSelectedMessages.contains(selectedId);
    }

    private void updateActionMenuStatus() {
        Log.d("tim_V6_all", "updateActionMenuStatus");
        if (mSelectedMessages.isEmpty()) {
            mDeleteMenu.setEnabled(false);
        } else {
            mDeleteMenu.setEnabled(true);
        }
        updateCursor();
        if (mCursor != null && !mCursor.isClosed()) {
            if (mCursor.getCount() == mSelectedMessages.size()) {
                mSelectMenu.setTitle(mContext.getString(R.string.muti_select_none));
                //add for bug 559631 begin
                mListener.getMaxIds().process();
                //add for bug 559631 end
            } else {
                mSelectMenu.setTitle(mContext.getString(R.string.muti_select_all));
            }
        }
        mListener.onActionBarUpdateMessageCount(mSelectedMessages.size());
    }
    
    private void updateCursor(){
        mCursor = mAdapter.getCursor();
    }
}
