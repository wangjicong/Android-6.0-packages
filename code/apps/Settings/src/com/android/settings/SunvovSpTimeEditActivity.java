package com.android.settings;

import android.app.Activity; 
import android.os.Bundle; 
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.os.SystemProperties;
import android.content.Context;
import android.widget.Toast;

/**
 * Created by Administrator on 2017/2/21 0021.
 */
public class SunvovSpTimeEditActivity extends Activity  implements View.OnClickListener{
    private static final String TAG = SunvovSpTimeEditActivity.class.getName();
    
    private EditText mTimeEdit;
    private Button mCancel;
    private Button mSave;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sunvov_sp_time_edit);
        
        mContext = this;
        mTimeEdit = (EditText)findViewById(R.id.time_edit);
        mCancel = (Button)findViewById(R.id.cancel);
        mSave = (Button)findViewById(R.id.save);

        //ancel.setOnClickListener(this);
        mCancel.setVisibility(View.GONE);
        mSave.setOnClickListener(this);  

        String value = ""+SystemProperties.getInt("persist.sys.sptime",6*60);
        mTimeEdit.setText(value);
        mTimeEdit.setSelection(value.length());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.save:
                String editValue = mTimeEdit.getText().toString();
                Log.d(TAG,"editValue  = "+editValue);
                if (editValue.startsWith("0")){
                    Toast.makeText(mContext,getString(R.string.sunvov_sp_time_input_error),Toast.LENGTH_LONG).show();
                }else if(editValue.length()==0){
                    Toast.makeText(mContext,getString(R.string.sunvov_sp_input_null),Toast.LENGTH_LONG).show();
                }else{
                    SystemProperties.set("persist.sys.sptime",editValue);
                     Toast.makeText(mContext,getString(R.string.sunvov_sp_setting_successed),Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }    
}
