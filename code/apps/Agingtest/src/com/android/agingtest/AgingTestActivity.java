package com.android.agingtest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import com.sprd.android.config.AgingTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

public class AgingTestActivity extends Activity {
    private static final int REQEUST_MULITIPLE_PERMISSIONS = 1000;
	private static final String[] sPermissionsNeeded = {
		Manifest.permission.CAMERA,
		Manifest.permission.ACCESS_COARSE_LOCATION,
		Manifest.permission.RECORD_AUDIO,
		Manifest.permission.READ_EXTERNAL_STORAGE,
	};
	private ListView mListView;
	private MyAdapter myAdapter;
	private HashMap<Integer, Boolean> mMap = new HashMap();
    private boolean mAllGranted = false;
    private boolean mPermRequesting = false;
    
    private static String TAG = "AgingTestActivity";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.agingtestmain);

		final String[] agingTestList = getResources().getStringArray(R.array.agingtestlist);
		AgingTestUtils.reInit();
		setAllenabled(true);
		myAdapter = new MyAdapter(this,agingTestList);

		mListView = (ListView) findViewById(R.id.lv);
		mListView.setAdapter(myAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> listView, View view, int position, long id){
				switch(position){
					case AgingTestUtils.cameraTest:
					case AgingTestUtils.wifiTest:
					case AgingTestUtils.bluetoothTest:
						if(mListView.isItemChecked(position)){
							mListView.clearChoices();
							mListView.setItemChecked(position,true);
							AgingTestUtils.reInit();
							AgingTestUtils.mAgingTest[position] = true;
							setAllenabled(false);
							mMap.put(position, true);
						}else{
							AgingTestUtils.mAgingTest[position] = false;
							setAllenabled(true);
							myAdapter.notifyDataSetChanged();
						}
						break;
					case AgingTestUtils.speakerTest:
						speakerMicEarpieceExclusive(position,AgingTestUtils.micTest,AgingTestUtils.earpieceTest);
						break;
					case AgingTestUtils.micTest:
						speakerMicEarpieceExclusive(position,AgingTestUtils.speakerTest,AgingTestUtils.earpieceTest);
						break;
					case AgingTestUtils.earpieceTest:
						speakerMicEarpieceExclusive(position,AgingTestUtils.speakerTest,AgingTestUtils.micTest);
						break;
					default:
						if(mListView.isItemChecked(position)){
							AgingTestUtils.mAgingTest[position] = true;
						}else{
							AgingTestUtils.mAgingTest[position] = false;
						}
						break;
				}
			}
		});
		Button button = (Button) findViewById(R.id.start);
		button.setOnClickListener(new Button.OnClickListener(){
			public void onClick(View v) {
				//qiuyaobo,20161121,begin
				if (!mAllGranted && !mPermRequesting) {
					requestPermissions();
				}	
				//}else{
				
				if(mAllGranted){
					Class<?> objectClass;
					if(AgingTestUtils.mAgingTest[AgingTestUtils.cameraTest]){
						objectClass= DoCameraTest.class;
					}else if(AgingTestUtils.mAgingTest[AgingTestUtils.wifiTest]){
						objectClass= DoWifiTest.class;
					}else if(AgingTestUtils.mAgingTest[AgingTestUtils.bluetoothTest]){
						objectClass= DoBluetoothTest.class;
					}else if(mListView.getCheckedItemCount()>0){
						objectClass= CommonTestActivity.class;
					}else{
						return;
					}
					Intent intent = new Intent(AgingTestActivity.this, objectClass);
					startActivity(intent);
				}
				//qiuyaobo,20161121,end
				
			}
		});
	}

	@Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		mPermRequesting = false;
		if(requestCode == REQEUST_MULITIPLE_PERMISSIONS){
			if (grantResults != null && grantResults.length > 0) {
				boolean isGranted = true;
				for (int i = 0; i < grantResults.length; i++) {
					if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
						isGranted = false;
						break;
					}
				}
				if (!isGranted) {
					Toast.makeText(this, R.string.error_permissions,Toast.LENGTH_SHORT).show();
				} else {
					mAllGranted = true;
				}
			}
		}
	}

	public class MyAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private String[] stringType;

		public MyAdapter(Context context, String[] stringType) {
			this.stringType = stringType;
			this.mInflater = LayoutInflater.from(context);
		}

		@Override
		public boolean isEnabled(int position) {
			return mMap.get(position);
		}

		@Override
		public int getCount() {
			return stringType.length;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.list_item_multiple_choice, null);
				holder.checkedTextView = (CheckedTextView) convertView.findViewById(R.id.checkedText);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.checkedTextView.setText(stringType[position]);
			boolean enabled= mMap.get(position);
			holder.checkedTextView.setEnabled(enabled);

			return convertView;
		}
	}

	public class ViewHolder{
		public CheckedTextView checkedTextView;
	}

	private void setAllenabled(boolean enabled) {
		for(int i=0;i<AgingTestUtils.maxTestCnt;i++){
			mMap.put(i, enabled);
		}		
	}

	private void speakerMicEarpieceExclusive(int enableId, int disableId0, int disableId1) {
		if(mListView.isItemChecked(enableId)){
			mListView.setItemChecked(disableId0,false);
			mListView.setItemChecked(disableId1,false);
			mMap.put(disableId0, false);
			mMap.put(disableId1, false);
			AgingTestUtils.mAgingTest[enableId] = true;
		}else{
			AgingTestUtils.mAgingTest[enableId] = false;
			mMap.put(disableId0, true);
			mMap.put(disableId1, true);
			myAdapter.notifyDataSetChanged();
		}	
	}

    private void requestPermissions() {
		List<String> permissionsRequestList = new ArrayList<String>();
		// Check whether the primission need to be requested
		for (int i = 0; i < sPermissionsNeeded.length; i++) {
			if (checkSelfPermission(sPermissionsNeeded[i]) != PackageManager.PERMISSION_GRANTED) {
				permissionsRequestList.add(sPermissionsNeeded[i]);
			}
		}
	
		if (permissionsRequestList.size() > 0) {
			requestPermissions(permissionsRequestList.toArray(new String[permissionsRequestList.size()]),
				REQEUST_MULITIPLE_PERMISSIONS);
			mPermRequesting = true;
		} else {
			mAllGranted = true;
		}
    }
}
