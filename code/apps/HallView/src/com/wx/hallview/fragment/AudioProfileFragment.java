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
import android.media.AudioManager;
import android.media.RingtoneManager;
import com.wx.hallview.R;
import android.provider.Settings;
import android.content.ContentResolver;
import android.util.Log;

public class AudioProfileFragment extends BaseFragmentView implements OnItemClickListener{
	
	private View mView;
	private ListView mAudioProfileList;
	private String[] mAudioProfileArray;
	private SettingsListAdapter mAdapter;   
	private int mSelectMode;
	private int mChecked;
	private AudioManager am;
	private ContentResolver mResolver;
	
	
		
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		// TODO Auto-generated method stub
		if(mChecked == position) return;

		mChecked = position;
		if(position == 0){
		    am.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
		    mSelectMode = AudioManager.RINGER_MODE_NORMAL;
		}else if(position == 1){
		    am.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
            Settings.System.putInt(mResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0);
            mSelectMode = AudioManager.RINGER_MODE_SILENT;
		}else if(position == 2){
			am.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
            am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
                    AudioManager.VIBRATE_SETTING_ON);
            am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,
                    AudioManager.VIBRATE_SETTING_ON);
            Settings.System.putInt(mResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0);
            mSelectMode = AudioManager.RINGER_MODE_VIBRATE;
		}else if(position == 3){
		    am.setRingerModeInternal(AudioManager.RINGER_MODE_OUTDOOR);
		    mSelectMode = AudioManager.RINGER_MODE_OUTDOOR;
		}
		/*Bug51378:Scene mode interface click on the error--up170323@{*/
		mAdapter.setSelectItem(position);
		mAdapter.notifyDataSetChanged();
		/*Bug51378:Scene mode interface click on the error--up170323@*/
	}

	private void initSelectedRingMode(){
		am = (AudioManager) getContext().getSystemService("audio");
		mSelectMode = am.getRingerModeInternal();
		setCheckedValue();
		Log.i("wangxing","mSelectMode="+mSelectMode);
	}
	
	private void setCheckedValue(){
		switch (mSelectMode){
			case AudioManager.RINGER_MODE_SILENT:
			    mChecked = 1;
			    break;
			case AudioManager.RINGER_MODE_VIBRATE:
			    mChecked = 2;
			    break;
			case AudioManager.RINGER_MODE_NORMAL:
			    mChecked = 0;
			    break;
			case AudioManager.RINGER_MODE_OUTDOOR:
			    mChecked = 3;
			    break;
	     }
	}
	
	public AudioProfileFragment (Context context){
		super(context);
		mAudioProfileArray = context.getResources().getStringArray(R.array.audioprofile_array);
		mAdapter = new SettingsListAdapter();
		mResolver = context.getContentResolver();
		
	}
	
	@Override
	protected void onAttach() {
		initSelectedRingMode();
		/*Scene mode interface click no response--up170323@{*/
		if(mAdapter != null){
			mAdapter.notifyDataSetChanged();
		}
		/*Scene mode interface click no response--up170323@}*/
	}

	@Override
	protected View onCreateView(LayoutInflater paramLayoutInflater,
			ViewGroup paramViewGroup) {
		// TODO Auto-generated method stub
		initSelectedRingMode();
		mView = paramLayoutInflater.inflate(R.layout.setting_view, paramViewGroup, false);
		TextView title = (TextView) mView.findViewById(R.id.setting_Title);
		title.setText(R.string.audioprofile);
		mAudioProfileList =(ListView) mView.findViewById(R.id.settings_list);
		/*Bug51378:Scene mode interface click on the error--up170323@{*/
		mAdapter.setSelectItem(mChecked);
		/*Bug51378:Scene mode interface click on the error--up170323@}*/
		mAudioProfileList.setAdapter(mAdapter);
		mAudioProfileList.setOnItemClickListener(this);
		
		return mView;
	}

	
	class SettingsListAdapter extends BaseAdapter {
		private int selectItem = -1;

		private SettingsListAdapter() {
		}

		public int getCount() {
			return mAudioProfileArray.length;
		}

		public Object getItem(int position) {
			return mAudioProfileArray[position];
		}

		public long getItemId(int position) {
			return (long) position;
		}

 		public void setSelectItem(int selectItem) {  
        	this.selectItem = selectItem;  
    	}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder  vh = null;
			if(convertView == null){
				vh = new ViewHolder();
				convertView = View.inflate(getContext(), R.layout.audioprofile_item, null);
				vh.title = (TextView) convertView.findViewById(R.id.profile_list_title);
				vh.cb = (CheckBox) convertView.findViewById(R.id.profile_list_cb);
				convertView.setTag(vh);
			}else{
				vh = (ViewHolder) convertView.getTag();
			}
			if(selectItem == position){
				vh.cb.setChecked(true);
			}else{
			    vh.cb.setChecked(false);
			}

			vh.title.setText(mAudioProfileArray[position]);
			return convertView;
		}
		
		class ViewHolder {
			CheckBox cb;
			TextView title;
		}
	}

}
