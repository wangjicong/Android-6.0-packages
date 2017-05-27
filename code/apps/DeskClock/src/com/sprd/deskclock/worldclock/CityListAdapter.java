package com.sprd.deskclock.worldclock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import com.android.deskclock.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.android.deskclock.R;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CityObj;
import com.sprd.deskclock.AplicationSupportChange;

public class CityListAdapter extends BaseAdapter {
    protected ArrayList<CityObj> mCitiesList = new ArrayList<CityObj>();
    private final LayoutInflater mInflater;
    private final Context mContext;
    protected HashMap<String, CityObj> mCitiesDb = new HashMap<String, CityObj>();
    private int mResource;
    private static final String TAG = "CityListAdapter";

    public CityListAdapter(Context context) {
        this(context, R.layout.track_list_item);
    }

    public CityListAdapter(Context context, int resource) {
        super();
        mContext = context;
        loadData(context);
        loadCitiesDb(context);
        mInflater = LayoutInflater.from(context);
        mResource = resource;
    }

    public void reloadData(Context context) {
        loadData(context);
        notifyDataSetChanged();
    }

    public void loadData(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mCitiesList = Cities.getCitiesListFromPrefs(prefs);
    }

    public HashMap<String, CityObj> loadCitiesDb(Context context) {
        mCitiesDb.clear();
        // Read the cities DB so that the names and timezones will be taken from the DB
        // and not from the selected list so that change of locale or changes in the DB will
        // be reflected.
        CityObj[] cities = Utils.loadCitiesDataBase(context);
        if (cities != null) {
            for (int i = 0; i < cities.length; i ++) {
                mCitiesDb.put(cities[i].mCityId, cities [i]);
            }
        }
        return mCitiesDb;
    }

    public void moveItem(int from, int to) {
        synchronized (this) {
            CityObj item = this.getItem(from);
            mCitiesList.remove(from);
            mCitiesList.add(to, item);
        }
        notifyDataSetChanged();
    }

    public void remove(int which) {
        synchronized (this) {
            mCitiesList.remove(which);
        }
        notifyDataSetChanged();
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

    static class ViewHolder {
        TextView name;
        TextView time;
        CheckBox check;
        TextView tz;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        ViewHolder holder;
        if (mCitiesList == null || position < 0 || position >= mCitiesList.size()) {
            return new View(mContext);
        }
        if (view == null) {
            holder = new ViewHolder();
            view = mInflater.inflate(mResource, parent, false);
            holder.name = (TextView)(view.findViewById(R.id.city_name));
            holder.time = (TextView)(view.findViewById(R.id.city_time));
            holder.tz = (TextView)(view.findViewById(R.id.city_tz));
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        CityObj city = mCitiesList.get(position);

        updateView(holder, city);
        return view;

    }

    private void updateView(ViewHolder holder, CityObj city) {

        // true indicates the system time is 24 Hour mode, false indicates the system time is 12 Hour mode
        boolean mTimeMode = android.text.format.DateFormat.is24HourFormat(mContext);

        // Home city or city not in DB , use data from the save selected cities list
        CityObj cityInDb = mCitiesDb.get(city.mCityId);
        holder.name.setText(Utils.getCityName(city, cityInDb));

        // Get timezone from cities DB if available
        final Calendar now = Calendar.getInstance();
        String cityTZ = (cityInDb != null) ? cityInDb.mTimeZone : city.mTimeZone;
        now.setTimeZone(TimeZone.getTimeZone(cityTZ));
        if (mTimeMode) {
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
}
