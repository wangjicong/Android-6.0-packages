package com.sprd.deskclock.worldclock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import com.android.deskclock.Utils;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CityObj;
import com.android.deskclock.R;
import com.sprd.deskclock.AplicationSupportChange;

public class DeleteCity extends Activity implements OnItemClickListener{
    private static final String TAG = "DeleteCity";
    private TextView mAllSelect;
    private CheckBox mAllSelectCheck;
    private LinearLayout mLinearLayout;
    private ListView mAllCities;
    private DeleteCityAdapter mDeleteCityAdapter;
    private ArrayList<CityObj> mCitiesList;
    protected HashMap<String, CityObj> mCitiesDb = new HashMap<String, CityObj>();
    private LayoutInflater mFactory;
    private MenuItem mMenuDone;
    private HashMap<Integer, Boolean> mSelectedItem = new HashMap<Integer, Boolean>();
    private static int INVALID_SIZE = 0;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        public void run() {
            this.update();
            handler.postDelayed(this, 1000);
        }

        void update() {
            if (mDeleteCityAdapter != null) {
                mDeleteCityAdapter.notifyDataSetChanged();
            }
        }
    };

    private class DeleteCityAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private final Context mContext;

        public DeleteCityAdapter(Context context, LayoutInflater factory) {
            super();
            mContext = context;
            loadData(context);
            loadCitiesDb(context);
            mInflater = factory;
        }

        public void reloadData(Context context) {
            loadData(context);
            notifyDataSetChanged();
        }

        public void loadData(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            mCitiesList = Cities.getCitiesListFromPrefs(prefs);
        }

        public void loadCitiesDb(Context context) {
            mCitiesDb.clear();
            // Read the cities DB so that the names and timezones will be taken from the DB
            // and not from the selected list so that change of locale or changes in the DB will
            // be reflected.
            CityObj[] cities = Utils.loadCitiesDataBase(context);
            if (cities != null) {
                for (int i = 0; i < cities.length; i++) {
                    mCitiesDb.put(cities[i].mCityId, cities[i]);
                }
            }
        }

        public int getCount() {
            return (mCitiesList != null) ? mCitiesList.size() : 0;
        }

        @Override
        public CityObj getItem(int p) {
            if (mCitiesList != null && p >= 0 && p < mCitiesList.size()) {
                return mCitiesList.get(p);
            }
            return null;
        }

        @Override
        public long getItemId(int p) {
            return p;
        }

        @Override
        public boolean isEnabled(int p) {
            return mCitiesList != null && mCitiesList.get(p).mCityId != null;
        }

        private void updateView(ViewHolder holder, CityObj city) {

            boolean timeMode = android.text.format.DateFormat.is24HourFormat(mContext);
            // Home city or city not in DB , use data from the save selected cities list
            CityObj cityInDb = mCitiesDb.get(city.mCityId);
            holder.name.setText(Utils.getCityName(city, cityInDb));

            // Get timezone from cities DB if available
            final Calendar now = Calendar.getInstance();
            String cityTZ = (cityInDb != null) ? cityInDb.mTimeZone : city.mTimeZone;
            now.setTimeZone(TimeZone.getTimeZone(cityTZ));
            if (timeMode) {
                holder.time.setText(DateFormat.format("kk:mm", now));
            } else {
                holder.time.setText(DateFormat.format("h:mm aa", now));
            }
            if(AplicationSupportChange.SUPPORT_WORLD_CLOCK_TIME_ZONE){
                holder.tz.setText(cityTZ);
            } else {
                holder.tz.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                holder.tz.setText(Utils.getGMTHourOffset(
                        TimeZone.getTimeZone(cityTZ), true));
            }
        }

        public HashMap<Integer,Boolean> getSelectedItem() {
            return mSelectedItem;
        }

        public boolean isChecked(int position) {
            return mSelectedItem.containsKey(position);
        }

        public void setChecked(int position, boolean checked) {

            if (checked) {
                mSelectedItem.put(position, true);
            } else {
                mSelectedItem.remove(position);
            }
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {

            ViewHolder holder;
            if (mCitiesList == null || position < 0 || position >= mCitiesList.size()) {
                return null;
            }

            if (view == null) {
                view = mInflater.inflate(R.layout.deletecity_list_item, parent, false);
                holder = new ViewHolder();
                holder.name = (TextView)(view.findViewById(R.id.city_name));
                holder.time = (TextView)(view.findViewById(R.id.city_time));
                holder.tz = (TextView)(view.findViewById(R.id.city_tz));
                holder.check = (CheckBox)(view.findViewById(R.id.check));
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            if (holder.check != null) {
                holder.check.setChecked(isChecked(position));
            }
            CityObj city = mCitiesList.get(position);
            updateView(holder, city);
            return view;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        mFactory = LayoutInflater.from(this);
        updateLayout();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int[] checkedIntArray = savedInstanceState.getIntArray("checkedArray");
        for (int i = 0; i < checkedIntArray.length; i++) {
            mSelectedItem.put(checkedIntArray[i], true);
        }
        checkMenuDoneEnabled();
        if(mAllSelectCheck.isChecked()){
            mAllSelect.setText(R.string.cancel_select);
        }else{
            mAllSelect.setText(R.string.all_select);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Integer[] checkedIntegerArray = mSelectedItem.keySet().toArray(
                new Integer[mSelectedItem.size()]);
        int[] checkedIntArray = new int[mSelectedItem.size()];
        for (int i = 0; i < checkedIntArray.length; i++) {
            checkedIntArray[i] = checkedIntegerArray[i];
        }
        outState.putIntArray("checkedArray", checkedIntArray);
        super.onSaveInstanceState(outState);
    }

    private void updateLayout() {
        setContentView(R.layout.activity_delete_city);
        mDeleteCityAdapter = new DeleteCityAdapter(this, mFactory);
        mLinearLayout = (LinearLayout) findViewById(R.id.delete_city);
        mAllSelect = (TextView) findViewById(R.id.all_select);
        mAllSelect.setText(R.string.all_select);
        mAllSelectCheck = (CheckBox) findViewById(R.id.all_select_check);
        if (mLinearLayout != null && mAllSelectCheck != null) {
            mLinearLayout.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(mAllSelectCheck.isChecked()){
                        checkedAll(false);
                    } else {
                        checkedAll(true);
                    }
                }
            });
            // Make the entire view selected when focused.
            mLinearLayout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                public void onFocusChange(View v, boolean hasFocus) {
                    v.setSelected(hasFocus);
                }
            });
        }

        mAllCities = (ListView) findViewById(R.id.city_list);
        mAllCities.setDivider(null);
        mAllCities.setOnItemClickListener(this);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.delete_city);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    private void checkedAll(boolean checked) {
        mAllSelectCheck.toggle();
        int count = mCitiesList.size();
        if (checked) {
            mAllSelect.setText(R.string.cancel_select);
            for (int i = 0; i < count; i++) {
                mDeleteCityAdapter.setChecked(i, true);
            }
        } else {
            mAllSelect.setText(R.string.all_select);
            for (int i = 0; i < count; i++) {
                mDeleteCityAdapter.setChecked(i, false);
            }
        }
        checkMenuDoneEnabled();
        mDeleteCityAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDeleteCityAdapter != null) {
            mAllCities.setAdapter(mDeleteCityAdapter);
        }
        handler.postDelayed(runnable, 1000);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.world_clock_delete_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mMenuDone = menu.findItem(R.id.menu_alarm_delete_done);
        checkMenuDoneEnabled();
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_alarm_delete_cancel:
                finish();
                return true;

            case R.id.menu_alarm_delete_done:
                int len = mCitiesList.size();
                ArrayList<CityObj> finalList = new ArrayList<CityObj>();
                for (int i = 0; i < len; i++) {
                    if (!mSelectedItem.containsKey(i)) {
                        finalList.add(mCitiesList.get(i));
                    }
                }
                Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this), finalList);
                Intent i = new Intent(Cities.WORLDCLOCK_UPDATE_INTENT);
                sendBroadcast(i);
                finish();
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemClick, position " + position);
        if (mDeleteCityAdapter.isChecked(position)) {
            mDeleteCityAdapter.setChecked(position, false);
        } else {
            mDeleteCityAdapter.getSelectedItem().put(position, true);
            mDeleteCityAdapter.setChecked(position, true);
        }
        mDeleteCityAdapter.notifyDataSetChanged();
        checkIsSelectedAll();
    }

    public void checkIsSelectedAll() {
        boolean listCheckedAll = mDeleteCityAdapter.getSelectedItem().size() == mCitiesList.size();
        if (listCheckedAll) {
            mAllSelectCheck.setChecked(true);
            mAllSelect.setText(R.string.cancel_select);
        } else {
            mAllSelectCheck.setChecked(false);
            mAllSelect.setText(R.string.all_select);
        }
        checkMenuDoneEnabled();
    }

    private void checkMenuDoneEnabled() {
        if (mMenuDone != null) {
            if (isSelectedOne()) {
                mMenuDone.setEnabled(true);
            } else {
                mMenuDone.setEnabled(false);
            }
        }
    }

    private Boolean isSelectedOne() {
        if (INVALID_SIZE == mDeleteCityAdapter.getSelectedItem().size()) {
            return false;
        }
        return true;
    }

    static class ViewHolder {
        TextView name;
        TextView time;
        TextView tz;
        CheckBox check;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setBackgroundColor(Utils.getCurrentHourColor());
    }
}
