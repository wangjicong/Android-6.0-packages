package com.sprd.dialer.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.android.dialer.R;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import com.sprd.android.config.OptConfig;
import android.provider.Settings;//Kalyy

public class CallRecordingSettingsFragment extends PreferenceFragment {
	  //qiuyaobo,20160709,begin
    private static final String AUTOMATIC_RECORDING = "automatic_recording_key";
    private CheckBoxPreference mAutomaticRecording;
	  //qiuyaobo,20160709,end
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.call_recording_settings_ex);
        //qiuyaobo,20160709,begin
        mAutomaticRecording = (CheckBoxPreference) findPreference(AUTOMATIC_RECORDING);
        mAutomaticRecording.setChecked(Settings.System.getInt(getActivity().getContentResolver(),Settings.System.CALL_SET_AUTO_RECORD, 0)==1?true:false);
        //qiuyaobo,20160709,end
    }
    
    //qiuyaobo,20160709,begin
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAutomaticRecording) {
			  
        	  if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
		            if(mAutomaticRecording.isChecked()){
		            	   mAutomaticRecording.setChecked(false);
								     new AlertDialog.Builder(this.getActivity()).setTitle("Smart Call Recorder") 					  
								     .setMessage("I agree to abide by the terms and conditions and laws of the land for usage of this feature.")  					  
								     .setPositiveButton("OK",new DialogInterface.OnClickListener() {
								         @Override  						  
								         public void onClick(DialogInterface dialog, int which) {  
								             mAutomaticRecording.setChecked(true);
								             Settings.System.putInt(getActivity().getContentResolver(),Settings.System.CALL_SET_AUTO_RECORD, 1);
								         }  					  
								     }).setNegativeButton("CANCEL",new DialogInterface.OnClickListener() {
								         @Override  					  
								         public void onClick(DialogInterface dialog, int which) { 						  
								             mAutomaticRecording.setChecked(false);
								             Settings.System.putInt(getActivity().getContentResolver(),Settings.System.CALL_SET_AUTO_RECORD, 0);
								         }  						  
								     }).show();              	   
		           }
        	  }else{
        	      Settings.System.putInt(getActivity().getContentResolver(),Settings.System.CALL_SET_AUTO_RECORD, mAutomaticRecording.isChecked()?1:0);
        	  }
            return true;
        }
        return true;
    }  
    //qiuyaobo,20160709,end  
}
