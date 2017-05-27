package com.wx.hallview.fragment;

import com.wx.hallview.ViewContorller;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.wx.hallview.R;

public class SettingFragment extends BaseFragmentView implements OnItemClickListener{
	
	private View mView;
	private ListView mSettingsList;
	private String[] mSettingArray;
	private SettingsListAdapter mAdapter;   
	
		
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int paramInt, long paramLong) {
		// TODO Auto-generated method stub
		if(paramInt == 0){
			ViewContorller.getInstance(getContext()).moveToFragment("audioprofile");
		}else if(paramInt == 1){
			ViewContorller.getInstance(getContext()).moveToFragment("timeout");
		}
	}

	public SettingFragment (Context context){
		super(context);
		mSettingArray = context.getResources().getStringArray(R.array.setting_array);
		mAdapter = new SettingsListAdapter();
	}

	//Kalyy Bug 49312
	@Override
	protected void onAttach() {
		android.util.Log.d("Kalyy","SettingFragment onAttach");
		if(mAdapter != null){
			mAdapter.notifyDataSetChanged();
		}
	}
	//Kalyy Bug 49312

	@Override
	protected View onCreateView(LayoutInflater paramLayoutInflater,
			ViewGroup paramViewGroup) {
		// TODO Auto-generated method stub
		mView = paramLayoutInflater.inflate(R.layout.setting_view, paramViewGroup, false);
		mSettingsList =(ListView) mView.findViewById(R.id.settings_list);
		mSettingsList.setAdapter(mAdapter);
		mSettingsList.setOnItemClickListener(this);
		return mView;
	}

	
	class SettingsListAdapter extends BaseAdapter {

		private SettingsListAdapter() {
		}

		public int getCount() {
			return mSettingArray.length;
		}

		public Object getItem(int position) {
			return mSettingArray[position];
		}

		public long getItemId(int position) {
			return (long) position;
		}

		

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder  vh = null;
			if(convertView == null){
				vh = new ViewHolder();
				convertView = View.inflate(getContext(), R.layout.settings_item, null);
				vh.setingItemIcon = (ImageView) convertView.findViewById(R.id.setting_list_icon);
				vh.setingItemStr = (TextView) convertView.findViewById(R.id.setting_item_title);
				convertView.setTag(vh);
			}else{
				vh = (ViewHolder) convertView.getTag();
			}
			if(position == 0){
				vh.setingItemIcon.setImageResource(R.drawable.user_profile_icon);
				
			}else if(position == 1){
				vh.setingItemIcon.setImageResource(R.drawable.timeout_icon);
			}
			vh.setingItemStr.setText(mSettingArray[position]);
			return convertView;
		}
		
		class ViewHolder {
			ImageView setingItemIcon;
			TextView setingItemStr;
		}
	}

}
