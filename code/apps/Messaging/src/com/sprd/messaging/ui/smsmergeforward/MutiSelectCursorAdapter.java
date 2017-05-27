
package com.sprd.messaging.ui.smsmergeforward;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;

import com.android.messaging.R;

public abstract class MutiSelectCursorAdapter  extends CursorAdapter implements OnClickListener{
    private static final String TAG = "MutiSelectCursorAdapter";
    private boolean mIsSelectMode = false;
    public ListView mListView;
    private View mSelectActionBarView;
    private TextView mSelectedCnt;
    private String mActionName;
    private MenuItem mSelectMenuItem;
    private HashMap<Integer,SmsMessageItem> mSelectSet = new HashMap<Integer,SmsMessageItem>();
    private ActionMode mActionMode;
    private static boolean mIsSelectAll = false;
    private Context mContext;
  

    public MutiSelectCursorAdapter(Context context, Cursor c, boolean autoRequery,
            ListView listView, String actionName) {
        super(context, c, autoRequery);
        mListView = listView;
        mActionName = actionName;
        mContext = context;
    }

    public MutiSelectCursorAdapter(Context context, Cursor c, int flags, ListView listView,
            String actionName) {
        super(context, c, flags);
        mListView = listView;
        mActionName = actionName;
        mContext = context;
    }
    
    public void startSelectMode() {
        Log.d(TAG,"=====sms merge forward=======startSelectMode====");
        mActionMode = mListView.startActionMode(mCallback);
        setSelectMode(true);
    }
    
    public void finishSelectMode(){
        if(mActionMode != null){
            mActionMode.finish();
        }
    }

    private void setSelectMode(boolean isSelectMode) {
        Log.d(TAG,"=====sms merge forward=======setSelectMode====mIsSelectMode: "+mIsSelectMode+"    isSelectMode: "+isSelectMode);
        if (mIsSelectMode != isSelectMode) {
            mIsSelectMode = isSelectMode; 
            notifyDataSetChanged();
        }
    }

    public boolean isSelectMode() {
        return mIsSelectMode;
    }

    private void selectAll() {
        int count = getCount();
        mSelectedCnt.setText(Integer.toString(count));
        for (int i = 0; i < count; i++) {
            SmsMessageItem obj = (SmsMessageItem)getItemKeyByPosition(i);
            if (obj != null) {
                mSelectSet.put(obj.mId,obj);
            }
        }
        notifyDataSetChanged();
        updateSelectTitle();
    }

    private void unSelectAll(){
        mSelectSet.clear();
        mSelectedCnt.setText(Integer.toString(mSelectSet.size()));
        notifyDataSetChanged();
        updateSelectTitle();
    }

    public void updateSelectTitle() {
        if (isSelectMode() && mSelectMenuItem != null) {
            if (mSelectSet.size() > 0 && mSelectSet.size() == getCount()) {
                mSelectMenuItem.setTitle(mContext.getResources()
                        .getString(R.string.menu_select_none));
            } else {
                mSelectMenuItem.setTitle(mContext.getResources()
                        .getString(R.string.muti_select_all));
            }
        }
    }

    public boolean isChecked(Integer itemKey) {
        return itemKey != null && mSelectSet.containsKey(itemKey);
    }

    private Object getItemKeyByPosition(int position) {
        Cursor cursor = (Cursor) getItem(position);
        try {
            return getItemKey(cursor);
        } catch (Exception e) {
            return null;
        }
    }

    private View setCheckedListener(View view) {
        if (view != null && (view instanceof Checkable)) {
            Object obj = getItemKeyByPosition(((Checkable) view).getCheckedPosition());
            ((Checkable) view).setChecked(isSelectMode(), isChecked(((SmsMessageItem)obj).mId));
            if (isSelectMode()) {
                view.setOnClickListener(this);
            }
        }
        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        try {
            view = super.getView(position, convertView, parent);
        } catch (IllegalStateException e) {
            Log.e("MutiselectCursorAdapter", "Cannot get the right data.");
            e.printStackTrace();
        }
        view = setCheckedListener(view);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return setCheckedListener(super.getDropDownView(position, convertView, parent));
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG,"=====sms merge forward=======onClick====");
        updateCheckedState(v);
    }

    public void updateCheckedState(View v){
        Log.d(TAG,"=====sms merge forward=======updateCheckedState====");
        if (isSelectMode() && (v instanceof Checkable)) {
            Object obj = getItemKeyByPosition(((Checkable) v).getCheckedPosition());
            Log.d(TAG,"=====sms merge forward=======updateCheckedState====checked==position: "+((Checkable) v).getCheckedPosition());
            if (obj != null) {
                boolean isChecked = isChecked(((SmsMessageItem)obj).mId);
                Log.d(TAG,"=====sms merge forward=======updateCheckedState====isChecked: "+isChecked);
                if (isChecked) {
                    mSelectSet.remove(((SmsMessageItem)obj).mId);
                } else {
                    mSelectSet.put(((SmsMessageItem)obj).mId,(SmsMessageItem)obj);
                }
                ((Checkable) v).setChecked(isSelectMode(), !isChecked);
                mSelectedCnt.setText(Integer.toString(mSelectSet.size()));
            }
        }
        updateSelectTitle();
    }

    private ActionMode.Callback mCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mContext instanceof Activity) {
                MenuInflater inflater = ((Activity) mContext).getMenuInflater();
                inflater.inflate(R.menu.smsmergeforward_select_menu, menu);
                mSelectMenuItem = menu.findItem(R.id.select_all);
                MenuItem item = menu.findItem(R.id.action_confirm);
                item.setTitle(mActionName); 
                mSelectSet.clear();

                if (mSelectActionBarView == null) {
                    mSelectActionBarView = LayoutInflater.from(mContext)
                            .inflate(R.layout.multi_select_actionbar, null);

                    mSelectedCnt =
                            (TextView) mSelectActionBarView.findViewById(R.id.selected_count);
                    mSelectedCnt.setText(Integer.toString(mSelectSet.size()));
                }
                mode.setCustomView(mSelectActionBarView);
            } else {
                throw new RuntimeException("Context is not an Actitity");
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_confirm:         
                    if (mSelectSet.size() > 0 && mSelectSet.size() == getCount()) {
                        mIsSelectAll = true;
                    } else {
                        mIsSelectAll = false;
                    }
                    confirmAction(mSelectSet);
                    mode.finish();
                    break;
                case R.id.select_all:
                    if (mSelectSet.size() > 0 && mSelectSet.size() == getCount()) {
                        unSelectAll();
                    } else {
                        selectAll();
                    }
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mSelectSet.clear();
            mSelectActionBarView = null;
            mSelectedCnt = null;
            setSelectMode(false);
        }
    };

    public abstract Object getItemKey(Cursor cursor);
    public abstract void confirmAction(HashMap<Integer,SmsMessageItem> set);

    public interface Checkable {
        int getCheckedPosition();

        void setChecked(boolean isSelectMode, boolean isChecked);
    }
  
    public static boolean isSelectAll() {
        return mIsSelectAll;
    }
}
