
package com.sprd.engineermode.slidesettings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sprd.engineermode.R;

public class SettingsAdapter extends BaseAdapter {

    public static final int LAUNCHER_SET_INDEX = 0;
    public static final int LA_DELAY_TIME_INDEX = 1;
    public static final int LA_SLIP_VELOCITY_INDEX = 2;
    public static final int LA_MIN_SNAP_VELOCITY_INDEX = 3;
    public static final int LA_TOUCH_SLOP_INDEX = 4;
    public static final int LA_MIN_FLING_VELOCITY_INDEX = 5;
    public static final int LA_FLING_THRESHOLD_VELOCITY_INDEX = 6;

    public static final int LISTVIEW_SET_INDEX = 7;
    public static final int LV_FRICTION_INDEX = 8;
    public static final int LV_VELOCITY_INDEX = 9;
    public static final int LV_TOUCH_SLOP_INDEX = 10;
    public static final int LV_MIN_VELOCITY_INDEX = 11;

    public static final int ITEM_SIZE = 12;

    private ValueItem[] items;
    private Context mContext;

    public SettingsAdapter(Context context) {
        this.mContext = context;
        items = new ValueItem[ITEM_SIZE];
        for (int i = 0; i < ITEM_SIZE; i++) {
            items[i] = new ValueItem(this, i);
        }
    }

    public void onItemClick(int position) {
        items[position].onItemClick();
    }

    @Override
    public int getCount() {
        return ITEM_SIZE;
    }

    @Override
    public Object getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView setTextView;
        TextView curValueTextView;
        TextView explainTextView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (items[position].isTitle()) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.settings_adpater_title,
                    null);
            TextView tv = (TextView) convertView.findViewById(R.id.settings_title);
            if (position == LAUNCHER_SET_INDEX) {
                tv.setText(R.string.launcher_settings);
            } else {
                tv.setText(R.string.listview_settings);
            }
            return convertView;
        }
        ViewHolder holder = null;
        com.sprd.engineermode.slidesettings.ValueItem.Config config = items[position].getConfig();
        if (convertView == null || convertView.getTag() == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.settings_adpater_value,
                    null);
            holder = new ViewHolder();
            holder.setTextView = (TextView) convertView.findViewById(R.id.set_value_tv);
            holder.curValueTextView = (TextView) convertView.findViewById(R.id.cur_value_tv);
            holder.explainTextView = (TextView) convertView.findViewById(R.id.dexplain_value_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.setTextView.setText(config.setText);
        holder.curValueTextView.setText(items[position].getCurText());
        holder.explainTextView.setText(config.explainText);
        return convertView;
    }

    public Context getContext() {
        return mContext;
    }

}
