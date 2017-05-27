package com.android.messaging.util;

import java.util.List;

import com.android.messaging.Factory;
import com.android.messaging.smil.data.SmilPartEntity;
import com.android.messaging.smil.view.SmileditPar;
import com.android.messaging.smil.view.TextImage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;




//add for jordan
import com.android.messaging.datamodel.data.DraftMessageData;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.MessagePartData;
//add for jordan

public class GlobleUtil {

    public static boolean isSmilAttament = false;
    public static Uri mSmilAttachmentUri = null;
    public static int FLAG_TYPE_URI = -1;
    public static SmileditPar mTemptSmileditPar = null;
    public static int mViewPosition = -1;
    //add for jordan
    public static DraftMessageData mDraftMessageData  = null;
    public static ConversationMessageData mConvMsgData = null;
    //add for jordan
    public static DraftMessageData mChangedDraftMessageData = null; // for save temp edited DraftmessagePartData
    public static List<SmilPartEntity> smilPartEntities;

    public static MessagePartData mSmilmMessagePartData;

    
    public static final int FDN_TOAST_MSG = 0x00000020;
    
   
    public static Handler mGlobleHandler = new Handler(Factory.get().getApplicationContext().getMainLooper()){

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case FDN_TOAST_MSG:
                Toast.makeText(Factory.get().getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                break;
            }
            super.handleMessage(msg);
        }

    };

    /*Add by SPRD for bug 561492 2016.05.19 Start*/
    public static void saveConId(Context context){
        SharedPreferences sp = context.getSharedPreferences("config", context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString("conId", GlobleUtil.getDraftMessageData()
                .getConversationId());
        editor.commit();
    }

    public static String getConId(Context context){
        SharedPreferences sp = context.getSharedPreferences("config", context.MODE_PRIVATE);
        return sp.getString("conId", "").toString();
    }
    /*Add by SPRD for bug 561492 2016.05.19 End*/

    //add for jordan
    public static void setDraftMessageData(DraftMessageData draftData){
    	mDraftMessageData = draftData;
    }
    
    public static DraftMessageData getDraftMessageData(){
    	return mDraftMessageData;
    }
    
    public static void setConvMessageData(ConversationMessageData convMsgData){
    	mConvMsgData = convMsgData;
    }
    
    public static ConversationMessageData getConvMessageData(){
    	return mConvMsgData;
    }
    //add for jordan
    
    public static void setSmilmDraftMessageData(MessagePartData data){
        mSmilmMessagePartData = data;
    }
    
    public static MessagePartData getSmilDraftMessageData(){
        return mSmilmMessagePartData;
    }
    
    public static void  setEditedDraftMessageDate(DraftMessageData data, List<SmilPartEntity> entities){
        mChangedDraftMessageData = data;
        smilPartEntities = entities;
    }
    
    public static DraftMessageData getEditedDraftMessageDate(){
        return mChangedDraftMessageData;
    }
    
    public static List<SmilPartEntity> getEditedDraftMessageDateEntities(){
        return smilPartEntities;
    }

    //sprd:559019 begin
    public  static boolean isPkgInstalled(Context context, String packageName) {
        if (packageName == null || "".equals(packageName))
             return false;
            android.content.pm.ApplicationInfo info = null;
        try {
            info = context.getPackageManager().getApplicationInfo(packageName, 0);
            return info != null;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
    //sprd:559019 end
}
