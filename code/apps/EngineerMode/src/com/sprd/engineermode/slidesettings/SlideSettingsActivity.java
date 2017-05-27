
package com.sprd.engineermode.slidesettings;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.sprd.engineermode.R;

public class SlideSettingsActivity extends ListActivity {

    private SettingsAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new SettingsAdapter(this);
        setListAdapter(mAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        mAdapter.onItemClick(position);
    }

}
