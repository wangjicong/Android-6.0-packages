package com.example.smartwakeanimation;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import com.sprd.android.config.OptConfig;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import java.util.List;
import android.content.Context;
import android.content.ActivityNotFoundException;
import com.sprd.android.config.OptConfig;

public class MainActivity extends Activity {
	private ImageView iv;
	private Intent wakeIntent;
	private int resid = 0;
	private int index = 0;
	private final int max = 20;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final WindowManager.LayoutParams params = getWindow().getAttributes();
		params.flags |= (WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.activity_main);
		iv = (ImageView) findViewById(R.id.iv);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Intent intent = getIntent();
		if(intent == null){
			finish();
		}else{
			String actionString = setAnimationResource(intent.getIntExtra("smart_wake_char",KeyEvent.KEYCODE_UNKNOWN));
			if(actionString == null){
				finish();
			}else{
				try {
					doWithAction(actionString);
				} catch (Exception e) {
					e.printStackTrace();
				}				
    			Message msg = new Message();
    			msg.what = 100;
				if(OptConfig.SUNVOV_CUSTOM_C7301_YSF_W20_FWVGA){
				mHandler.sendMessageDelayed(msg, 500);
				}else{
    			mHandler.sendMessageDelayed(msg, 70);
				}
			}
		}
	}

	private void doWithAction(String selString) throws Exception{
		switch(selString){
			case "music":{//music
				wakeIntent = new Intent("android.intent.action.MUSIC_PLAYER");
			}break;

			case "camera":{//camera
				ComponentName cnCamera = new ComponentName("com.android.camera2","com.android.camera.CameraLauncher");
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnCamera);
			}break;

			case "dial":{//dial
				ComponentName cnDial = new ComponentName("com.android.dialer","com.android.dialer.DialtactsActivity");
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnDial);
			}break;

			case "video":{//video
				ComponentName cnVideo ;
				if(isAvilible(this,"com.google.android.videos")){
					cnVideo = new ComponentName("com.google.android.videos",
					    "com.google.android.youtube.videos.EntryPoint");
				}else{
				    cnVideo = new ComponentName("com.android.gallery3d",
					    "com.sprd.gallery3d.app.VideoActivity");
			    }
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnVideo);
			}break;

			case "browser":{//browser
			    ComponentName cnBrowser;
			    
			    if(isAvilible(this,"com.android.chrome")){
			        cnBrowser = new ComponentName("com.android.chrome","com.google.android.apps.chrome.Main");
			    } else {
			        cnBrowser = new ComponentName("com.android.browser","com.android.browser.BrowserActivity");
			    }
				
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnBrowser);
				wakeIntent.putExtra("entry.browser.from.gesture", true);
			}break;

			case "google_play":{//google play
				ComponentName cngplay = new ComponentName("com.android.vending","com.android.vending.AssetBrowserActivity");
				wakeIntent = new Intent();
				wakeIntent.setComponent(cngplay);
			}break;

			case "message":{//message
				ComponentName message = new ComponentName("com.android.messaging","com.android.messaging.ui.conversationlist.ConversationListActivity");
				wakeIntent = new Intent();
				wakeIntent.setComponent(message);
			//	wakeIntent = new Intent(Intent.ACTION_MAIN);
			//	wakeIntent.addCategory(Intent.CATEGORY_DEFAULT);
			//	wakeIntent.setType("vnd.android-dir/mms-sms");
			}break;

			case "calc":{//calculator
				ComponentName cnCalc = new ComponentName("com.android.calculator2","com.android.calculator2.Calculator");
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnCalc);			
			}break;

			case "calendar":{//calendar
				ComponentName cnCalendar;
				if(isAvilible(this,"com.google.android.calendar")){//wangxing add
					cnCalendar = new ComponentName("com.google.android.calendar","com.android.calendar.AllInOneActivity");
				}else{
				    cnCalendar = new ComponentName("com.android.calendar","com.android.calendar.AllInOneActivity");
				}
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnCalendar);			
			}break;

			case "fileexplorer":{//File Explorer
				ComponentName cnFileExplorer = new ComponentName("com.sprd.fileexplorer",
					"com.sprd.fileexplorer.activities.FileExploreActivity");
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnFileExplorer);
			}break;

			case "gallery":{//Gallery
				ComponentName cnGallery;
				if(isAvilible(this,"com.google.android.apps.photos")){
				    cnGallery = new ComponentName("com.google.android.apps.photos","com.google.android.apps.photos.home.HomeActivity");
				}else{
				    cnGallery = new ComponentName("com.android.gallery3d","com.android.gallery3d.app.GalleryActivity");
				}
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnGallery);
			}break;
            
			case "email":{//Email
				ComponentName cnGallery = new ComponentName("com.android.email","com.android.email2.ui.MailActivityEmail");
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnGallery);
			}break;

			case "settings":{//settings
				ComponentName cnGallery = new ComponentName("com.android.settings","com.android.settings.Settings");
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnGallery);
			}break;			
			
			case "bbm":{//bbm
				ComponentName cnGallery = new ComponentName("com.bbm","com.bbm.ui.activities.StartupActivity");
				wakeIntent = new Intent();
				wakeIntent.setComponent(cnGallery);
			}break;			

			case "unlock"://unlock --up150827
			default:break;
		}
	}

	private String setAnimationResource(int keyCode){
		String selString;
		ContentResolver cr = getContentResolver();

		switch (keyCode) {
			case KeyEvent.KEYCODE_F4:// up
				resid = R.drawable.smart_wake_up_01;
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_UP);
			break;
			case KeyEvent.KEYCODE_F5:// down
				resid = R.drawable.smart_wake_down_01;
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_DOWN);
			break;
			case KeyEvent.KEYCODE_F6://C
				resid = R.drawable.smart_wake_c_01;		
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_C);
				break;
			case KeyEvent.KEYCODE_F7://e
				resid = R.drawable.smart_wake_e_01;
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_E);
				break;
			case KeyEvent.KEYCODE_F8://m
				resid = R.drawable.smart_wake_m_01;
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_M);
				break;
			case KeyEvent.KEYCODE_F9://O
				resid = R.drawable.smart_wake_o_01;
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_O);
				break;
			case KeyEvent.KEYCODE_F10://S
				resid = R.drawable.smart_wake_s_01;
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_S);
				break;
			case KeyEvent.KEYCODE_F11://V
				resid = R.drawable.smart_wake_v_01;
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_V);
				break;
			case KeyEvent.KEYCODE_F12://W
				resid = R.drawable.smart_wake_w_01;
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_W);
				break;
			case KeyEvent.KEYCODE_TAB://Z
				resid = R.drawable.smart_wake_z_01;
				selString = Settings.Global.getString(cr, Settings.Global.GESTUREACTION_Z);
				break;
			default:
				selString = null;
				break;
		}

		return selString;
	}

	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			if(msg.what == 100){
				if(index<max){
					iv.setBackgroundResource(resid);
					resid ++;
					index ++;
					Message m = new Message();
					m.what = 100;
					mHandler.sendMessageDelayed(m, 50);
				}else{//Bug35234--up150716
					this.removeMessages(100);
					iv.setBackgroundColor(0xFF000000);//set black
					if(wakeIntent != null){
						wakeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
							| Intent.FLAG_ACTIVITY_CLEAR_TOP);
				        try{
							startActivity(wakeIntent);
				        } catch (ActivityNotFoundException e){
				            e.printStackTrace();
				        }
					}
					Message m = new Message();
					m.what = 101;
					mHandler.sendMessageDelayed(m, 500);
				}
			}else if(msg.what == 101){//Kalyy
				Intent new_intent = new Intent("SmartWake_HideLock");
				sendBroadcast(new_intent);
				Intent intent = new Intent("SmartWake_ShowPassword");    
				sendBroadcast(intent);
				finish();
			}
		}

	}; 

    /*SUN:jicong.wang add for bug47431 start {@*/
    private boolean isAvilible(Context context,String pkg){    
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        for ( int i = 0; i < pinfo.size(); i++ )
        {
            if(pinfo.get(i).packageName.equalsIgnoreCase(pkg))
                return true;
        }
        return false;    
    }
    /*SN:jicong.wang add for bug47431 end @}*/
}
