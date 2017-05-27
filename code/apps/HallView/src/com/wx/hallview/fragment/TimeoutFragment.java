package com.wx.hallview.fragment;

import android.content.Context;
import android.media.AudioManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.wx.hallview.R;
import android.provider.Settings;
import android.content.ContentResolver;
import android.util.Log;

public class TimeoutFragment extends BaseFragmentView implements OnItemClickListener{
	
	private View mView;
	private ListView mTimeoutList;
	private CheckBox[] mCheckBoxArray; 
	private String[] mDisplayArray;
	private String[] mTimeArray;
	private SettingsListAdapter mAdapter;   
	private int mTimeout;
	private int mChecked;
	private ContentResolver mResolver;
	
	
		
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		// TODO Auto-generated method stub
		if(mChecked == position) return;
		mChecked = position;
		int j = mCheckBoxArray.length;
		for(int i = 0;i < j;i++){
		    if(i == mChecked){
		    	if(mCheckBoxArray[i] != null)
		    	    mCheckBoxArray[i].setChecked(true);
		    }else{
		        if(mCheckBoxArray[i] != null &&  mCheckBoxArray[i].isChecked())
		            mCheckBoxArray[i].setChecked(false);
		    }
		}
	
		mTimeout = Integer.parseInt(mTimeArray[mChecked]);
		Log.i("wangxing","position="+position+" mTimeout="+mTimeout+" mChecked="+mChecked);
        Settings.System.putInt(mResolver, "screen_off_timeout", mTimeout);
        
		
	}

	private void initTimeout(){
		mTimeout = Settings.System.getInt(mResolver, "screen_off_timeout", 30000);
		for(int i =0; i < mTimeArray.length; i++){
			if(mTimeout == Integer.parseInt(mTimeArray[i])){
				mChecked = i;
				break;
			}
		}
	}
	
	
	
	public TimeoutFragment (Context context){
		super(context);
		mDisplayArray = context.getResources().getStringArray(R.array.screen_timeout_entries);
	    mTimeArray = context.getResources().getStringArray(R.array.screen_timeout_values);
		mAdapter = new SettingsListAdapter();
		mCheckBoxArray = new CheckBox[mDisplayArray.length];
		mResolver = context.getContentResolver();
		
	}
	
	@Override
	protected void onAttach() {
		initTimeout();
		/*Sleep time setting interface click no response--up170323@{*/
		if(mAdapter != null){
			mAdapter.notifyDataSetChanged();
		}
		/*Sleep time setting interface click no response--up170323@}*/
	}

	@Override
	protected View onCreateView(LayoutInflater paramLayoutInflater,
			ViewGroup paramViewGroup) {
		// TODO Auto-generated method stub
		initTimeout();
		mView = paramLayoutInflater.inflate(R.layout.setting_view, paramViewGroup, false);
		TextView title = (TextView) mView.findViewById(R.id.setting_Title);
		title.setText(R.string.screentimeout);
		mTimeoutList =(ListView) mView.findViewById(R.id.settings_list);
		mTimeoutList.setAdapter(mAdapter);
		mTimeoutList.setOnItemClickListener(this);
		
		return mView;
	}

	
	class SettingsListAdapter extends BaseAdapter {

		private SettingsListAdapter() {
		}

		public int getCount() {
			return mDisplayArray.length;
		}

		public Object getItem(int position) {
			return mDisplayArray[position];
		}

		public long getItemId(int position) {
			return (long) position;
		}

		

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder  vh = null;
		        vh = new ViewHolder();
			convertView = View.inflate(getContext(), R.layout.timeout_item, null);
			vh.title = (TextView) convertView.findViewById(R.id.timeout_list_title);
			vh.cb = (CheckBox) convertView.findViewById(R.id.timeout_list_cb);
			if(mChecked == position){
				vh.cb.setChecked(true);
			}else{
			    vh.cb.setChecked(false);
			}
			mCheckBoxArray[position] = vh.cb;
			vh.title.setText(mDisplayArray[position]);
			return convertView;
		}
		
		class ViewHolder {
			CheckBox cb;
			TextView title;
		}
	}

}
