
package com.sprd.contacts.list;

import android.os.Bundle;

import com.android.contacts.R;
import com.android.contacts.common.list.ContactEntryListAdapter;

import java.util.ArrayList;
import java.util.List;

public class AllInOneFavoritesPickerFragment extends AllInOneDataPickerFragment {
    private static final String TAG = AllInOneFavoritesPickerFragment.class.getSimpleName();

    private static final String KEY_DATA_SELECTION = "data_selection";
    private static final String KEY_CASCADING_DATA = "cascadingData";

    private static final String EMIAL_TYPE_DATA = "vnd.android.cursor.item/email_v2";

    private String mDataSelection;
    private String mMimeType;
    private List<String> mCascadingData;

    private int mMimeTypeCnt;

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }
        mCascadingData = savedState.getStringArrayList(KEY_CASCADING_DATA);
        mDataSelection = savedState.getString(KEY_DATA_SELECTION);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(KEY_CASCADING_DATA, new ArrayList<String>(mCascadingData));
        if (mDataSelection != null) {
            outState.putString(KEY_DATA_SELECTION, mDataSelection);
        }
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        AllInOneDataListAdapter adapter = new AllInOneDataListAdapter(getActivity());
        adapter.setDisplayPhotos(true);
        adapter.setCascadingData(mCascadingData);
        if (mDataSelection != null) {
            adapter.setLoaderSelection(mDataSelection);
        }
        return adapter;
    }

    @Override
    protected void prepareEmptyView() {
        super.prepareEmptyView();
        if ((EMIAL_TYPE_DATA.equals(mMimeType)) && (mMimeTypeCnt == 1)) {
            setEmptyText(R.string.noContactwithEmail);
        } else {
            setEmptyText(R.string.listTotalAllContactsZeroStarred);

        }
    }

    public void setCascadingData(List<String> data) {
        super.setCascadingData(data);
        mCascadingData = data;
        mMimeTypeCnt = mCascadingData.size();
        for (String mimeType : mCascadingData) {
            mMimeType = mimeType;
        }
    }

    public void setSelection(String data) {
        mDataSelection = data;
    }
}
