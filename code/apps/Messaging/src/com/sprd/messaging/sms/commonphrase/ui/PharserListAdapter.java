
package com.sprd.messaging.sms.commonphrase.ui;

import java.util.ArrayList;

import com.android.messaging.R;
import com.sprd.messaging.sms.commonphrase.model.ItemData;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class PharserListAdapter extends BaseAdapter {
    private static String TAG = "PharserListAdapter";
    private Context mContext;
    private ArrayList<ItemData> mArray = new ArrayList<ItemData>();

    private boolean mItemMultiCheckable;
    private ArrayList<ItemData> mSelectItems = new ArrayList<ItemData>();

    private Context getContext() {
        return mContext;
    }

    private ArrayList<ItemData> getArray() {
        return mArray;
    }

    public PharserListAdapter(Context context, ArrayList<ItemData> array) {
        mContext = context;
        mArray = array;

    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return getArray().size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return getArray().get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        // TODO Auto-generated method stub
        PharserItemView vpharserItem = null;

        vpharserItem = (PharserItemView) LayoutInflater.from(getContext()).inflate(
                R.layout.pharser_item_ex, null);
        vpharserItem.setTag(getArray().get(position));

        Log.d(TAG, " getview ==>>[" + position + "] Value =");
        getArray().get(position).Debug();

        if (getItemMultiCheckable()) {
            vpharserItem.getCheckBox().setVisibility(View.VISIBLE);
            if (SelectedItems().contains(getArray().get(position))) {
                vpharserItem.getCheckBox().setChecked(true);
            } else {
                vpharserItem.getCheckBox().setChecked(false);
            }
        } else {
            vpharserItem.getCheckBox().setVisibility(View.GONE);
        }
        vpharserItem.init();
        return vpharserItem;
    }

    // for multi-delete begin
    public void setItemMultiCheckable(boolean flag) {
        mItemMultiCheckable = flag;
    }

    public boolean getItemMultiCheckable() {
        return mItemMultiCheckable;
    }

    public void addSelectedItem(int position) {
        SelectedItems().add(getArray().get(position));

        /* And by SPRD for Bug:505782 2015.11.30 Start */
        if(mHost != null) {
            mHost.onSelectedItemAdded(position);
        }
        /* And by SPRD for Bug:505782 2015.11.30 End */

        Log.d(TAG, "===addSelectedItem======>addItem");
    }

    public void cancelSelectedItem(int position) {
        SelectedItems().remove(getArray().get(position));

        /* And by SPRD for Bug:505782 2015.11.30 Start */
        if(mHost != null) {
            mHost.onSelectedItemRemoved(position);
        }
        /* And by SPRD for Bug:505782 2015.11.30 End */

        Log.d(TAG, "===cancelSelectedItem======>removeItem");
    }

    public void clearSelectedItem() {
        SelectedItems().clear();

        /* And by SPRD for Bug:505782 2015.11.30 Start */
        if(mHost != null) {
            mHost.onAllSelectedIntemRemoved();
        }
        /* And by SPRD for Bug:505782 2015.11.30 End */
    }

    public ArrayList<ItemData> SelectedItems() {

        return mSelectItems;// for database update cache

    }
    // for multi-delete end

    /* And by SPRD for Bug:505782 2015.11.30 Start */
    private PharserHost mHost;

    public void setHost(PharserHost host) {
        mHost = host;
    }

    public interface PharserHost {
        void onSelectedItemAdded(Integer position);

        void onSelectedItemRemoved(Integer position);

        void onAllSelectedIntemRemoved();
    }
    /* And by SPRD for Bug:505782 2015.11.30 End */
}
