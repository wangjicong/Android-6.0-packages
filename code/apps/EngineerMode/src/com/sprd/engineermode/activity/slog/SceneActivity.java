package com.sprd.engineermode.activity.slog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.sprd.engineermode.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import android.os.Handler;
import com.sprd.engineermode.core.SlogCore;
import com.sprd.engineermode.debuglog.slogui.CustomSettingActivity;
import com.sprd.engineermode.debuglog.slogui.SlogUISettings;
import android.graphics.Color;


/**
 * Created by SPREADTRUM\zhengxu.zhang on 9/6/15.
 */
public class SceneActivity extends Activity implements View.OnClickListener{

    private ArrayList<Button> btnScene = new ArrayList<Button>();
    private TextView title;
    private static final String TAG = "SLOG_SCENE";




    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_slog_scene);

        btnScene.add((Button) findViewById(R.id.btn_slog_normal_scene));
        btnScene.add((Button)findViewById(R.id.btn_slog_data_scene));
        btnScene.add((Button)findViewById(R.id.btn_slog_voice_scene));
        btnScene.add((Button)findViewById(R.id.btn_slog_modem_scene));
        btnScene.add((Button)findViewById(R.id.btn_slog_wcn_scene));
        btnScene.add((Button)findViewById(R.id.btn_slog_sim_scene));
        btnScene.add((Button)findViewById(R.id.btn_slog_custom_scene));

        for(Button btn:btnScene){
            btn.setOnClickListener(this);
            btn.setBackgroundColor(Color.GRAY);
        }

    }

    @Override
    public void onResume(){
        super.onResume();
        if(SlogInfo.self().slog_tmp!= SlogInfo.SceneStatus.close)
            return;
        switch (SlogInfo.self().getSceneStatus()){
            case normal:
                btnScene.get(0).setBackgroundColor(Color.GREEN);
                break;
            case data:
                btnScene.get(1).setBackgroundColor(Color.GREEN);
                break;
            case voice:
                btnScene.get(2).setBackgroundColor(Color.GREEN);
                break;
            case modem:
                btnScene.get(3).setBackgroundColor(Color.GREEN);
                break;
            case wcn:
                btnScene.get(4).setBackgroundColor(Color.GREEN);
                break;
            case sim:
                btnScene.get(5).setBackgroundColor(Color.GREEN);
                break;
            case customer:
                for(Button btn:btnScene){
                    btn.setBackgroundColor(Color.GRAY);
                }
            	btnScene.get(6).setBackgroundColor(Color.GREEN);
            	break;
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_slog_normal_scene:
                if(SlogInfo.self().getSceneStatus()== SlogInfo.SceneStatus.normal){
                    SlogInfo.self().closeScene();
                    SlogInfo.self().setSceneStatus(SlogInfo.SceneStatus.close);
                    for(Button btn:btnScene){
                        btn.setBackgroundColor(Color.GRAY);
                    }
                    return;
                }
                Log.d(TAG,"open normalLog 00");

                SlogInfo.self().openNormalScene();

                for(Button btn:btnScene){
                    btn.setBackgroundColor(Color.GRAY);
                }
                btnScene.get(0).setBackgroundColor(Color.GREEN);
                break;
            case R.id.btn_slog_data_scene:
                if(SlogInfo.self().getSceneStatus()== SlogInfo.SceneStatus.data){
                    SlogInfo.self().closeScene();
                    SlogInfo.self().setSceneStatus(SlogInfo.SceneStatus.close);
                    for(Button btn:btnScene){
                        btn.setBackgroundColor(Color.GRAY);
                    }
                    return;
                }
                Log.d(TAG,"open dataLog 00");
                SlogInfo.self().openDataScene();
                for(Button btn:btnScene){
                    btn.setBackgroundColor(Color.GRAY);
                }
                btnScene.get(1).setBackgroundColor(Color.GREEN);

                break;
            case R.id.btn_slog_voice_scene:
                if(SlogInfo.self().getSceneStatus()== SlogInfo.SceneStatus.voice){
                    SlogInfo.self().closeScene();
                    SlogInfo.self().setSceneStatus(SlogInfo.SceneStatus.close);
                    for(Button btn:btnScene){
                        btn.setBackgroundColor(Color.GRAY);
                    }
                    return;
                }
                Log.d(TAG,"open voiceLog 00");
                SlogInfo.self().openVoiceScene();
                for(Button btn:btnScene){
                    btn.setBackgroundColor(Color.GRAY);
                }
                btnScene.get(2).setBackgroundColor(Color.GREEN);

                break;
            case R.id.btn_slog_modem_scene:
                if(SlogInfo.self().getSceneStatus()== SlogInfo.SceneStatus.modem){
                    SlogInfo.self().closeScene();
                    SlogInfo.self().setSceneStatus(SlogInfo.SceneStatus.close);
                    for(Button btn:btnScene){
                        btn.setBackgroundColor(Color.GRAY);
                    }
                    return;
                }
                Log.d(TAG,"open modemLog 00");
                SlogInfo.self().openModemScene();
                for(Button btn:btnScene){
                    btn.setBackgroundColor(Color.GRAY);
                }
                btnScene.get(3).setBackgroundColor(Color.GREEN);

                break;
            case R.id.btn_slog_wcn_scene:
                if(SlogInfo.self().getSceneStatus()== SlogInfo.SceneStatus.wcn){
                    SlogInfo.self().closeScene();
                    SlogInfo.self().setSceneStatus(SlogInfo.SceneStatus.close);
                    for(Button btn:btnScene){
                        btn.setBackgroundColor(Color.GRAY);
                    }
                    return;
                }
                Log.d(TAG,"open wcnLog 00");
                SlogInfo.self().openWcnScene();
                for(Button btn:btnScene){
                    btn.setBackgroundColor(Color.GRAY);
                }
                btnScene.get(4).setBackgroundColor(Color.GREEN);
                break;
            case R.id.btn_slog_sim_scene:
                if(SlogInfo.self().getSceneStatus()== SlogInfo.SceneStatus.sim){
                    SlogInfo.self().closeScene();
                    SlogInfo.self().setSceneStatus(SlogInfo.SceneStatus.close);
                    for(Button btn:btnScene){
                        btn.setBackgroundColor(Color.GRAY);
                    }
                    return;
                }
                Log.d(TAG,"open simLog 00");
                SlogInfo.self().openSimScene();
                for(Button btn:btnScene){
                    btn.setBackgroundColor(Color.GRAY);
                }
                btnScene.get(5).setBackgroundColor(Color.GREEN);
                break;

            case R.id.btn_slog_custom_scene:
                Log.d(TAG,"slog_custom_scene");
                //Intent iCustomer = new Intent(this,SlogUISettings.class);
                //startActivity(iCustomer);
                Intent iOpenUserDefined = new Intent(this,UserDefinedActivity.class);
                startActivity(iOpenUserDefined);
                break;
        }
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){

                case 0:

                    break;
                case 1:

                    break;

            }
        }

    };





}
