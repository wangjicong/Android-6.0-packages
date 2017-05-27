package com.sprd.deskclock.worldclock;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CityObj;

public class WCReorderActivity extends ListActivity {
    private TouchInterceptor mTrackList;
    private CityListAdapter mAdapter;
    private static final String TAG = "WCReorderActivity";

    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {
            Log.i(TAG, "DropListener from " + from + " to "+ to);
            mAdapter.moveItem(from, to);
        }
    };

    private TouchInterceptor.RemoveListener mRemoveListener = new TouchInterceptor.RemoveListener() {
        public void remove(int which) {
            Log.i(TAG, "RemoveListener which " + which);
            mAdapter.remove(which);
        }
    };

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        public void run() {
            this.update();
            handler.postDelayed(this, 1000);
        }

        void update() {
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reorder_city);

        mTrackList = (TouchInterceptor) getListView();

        mTrackList.setCacheColorHint(0);
        mTrackList.setDivider(null);
        mTrackList.setDropListener(mDropListener);
        mTrackList.setRemoveListener(mRemoveListener);

        mAdapter = new CityListAdapter(this, R.layout.track_list_item);
        setListAdapter(mAdapter);

        handler.postDelayed(runnable, 1000);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.reorder_city);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    public void onPause() {
        super.onPause();
        //after dragging this item ,we should save the mCitiesList to the preference
        Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this), mAdapter.mCitiesList);
        Intent i = new Intent(Cities.WORLDCLOCK_UPDATE_INTENT);
        sendBroadcast(i);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.loadCitiesDb(this);
            mAdapter.reloadData(this);
        }
        getWindow().getDecorView().setBackgroundColor(Utils.getCurrentHourColor());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
