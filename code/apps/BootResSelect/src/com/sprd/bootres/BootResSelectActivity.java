package com.sprd.bootres;
import android.util.Log;
import android.os.Bundle;
import android.os.SystemProperties;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.Arrays;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import android.app.ActivityManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import com.sprd.android.config.OptConfig;

import java.io.InputStreamReader;
import android.os.FileUtils;
import java.io.IOException;

public class BootResSelectActivity extends Activity {
	public static final String TAG = "BootResSelcetActivity";

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.xml.main);
		ListView lv = (ListView) findViewById(R.id.lv);
		
		int curSelecet = readProductinfo();
		int poweronoff_num = OptConfig.SUN_MULTI_POWERONOFF_NUM;
		if(curSelecet >= poweronoff_num){
			curSelecet = 0;
		}
		final String[] animList = getResources().getStringArray(R.array.animlist);
		String[] listViewList = new String[poweronoff_num];
		listViewList = Arrays.copyOfRange(animList,0,poweronoff_num);
		
		lv.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_single_choice,listViewList));
		lv.setItemChecked(curSelecet, true);
		lv.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3){
            	Log.d(TAG,"use animation " + arg2);
                writeProductinfo((byte)(arg2+'0'));
            }
        });
	}
	
	private void writeProductinfo(byte logo_choice)
	{
		byte BufToWrite[] = new byte[1];

		BufToWrite[0] = logo_choice;

		try {
			File logo_choiceFile = new File("/productinfo/","logochoice.file");
			if(!logo_choiceFile.exists())
			{
				logo_choiceFile.createNewFile();
			}
			FileUtils.setPermissions(logo_choiceFile, 0644, -1, -1); // -rw-r--r--
			
			try {
				FileOutputStream fos = new FileOutputStream(logo_choiceFile, false);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
				fos);
				bufferedOutputStream.write(BufToWrite, 0, BufToWrite.length);
				bufferedOutputStream.flush();
				bufferedOutputStream.close();
			} catch (Exception e) {
				Log.e(TAG,
						"Exception Occured: Trying to write logo_choice.file "
						+ e.toString());
			}
		}
		catch (IOException e) {
            e.printStackTrace();
        }
		catch(Exception e) {
            e.printStackTrace();
        }
	}

	private int readProductinfo()
	{
		char BufToRead[] = new char[1];
		int value = 0;
 		
        try {
        		Log.d("Kalyy","001");
            InputStreamReader inputReader = null;
            File fileName = new File("/productinfo/","logochoice.file");
            if(!fileName.exists()) 
            {
            		Log.d("Kalyy","002");
                    writeProductinfo((byte)'0');
            }
            else
            {			
                try{
                    inputReader = new FileReader(fileName);
                    int numRead = inputReader.read(BufToRead);
                    if( numRead > 0) {
                    	Log.d("Kalyy","003");
            			value =  Integer.parseInt(String.valueOf(BufToRead));
                    }
                } catch (Exception e) {
					Log.e(TAG,
							"Exception Occured: Trying to read logo_choice.file "
							+ e.toString());
				}
                finally {
                    inputReader.close();
                }
            }
        }
		catch (IOException e) {
            e.printStackTrace();
        }
		catch(Exception e) {
            e.printStackTrace();
        }
    Log.d("Kalyy","value="+value);
		return value;
	}
}
