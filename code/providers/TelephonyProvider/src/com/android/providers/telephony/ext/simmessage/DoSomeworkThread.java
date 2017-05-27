package com.android.providers.telephony.ext.simmessage;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.List;

/**
 * Created by apuser on 16-3-25.
 */
public class DoSomeworkThread extends HandlerThread implements Handler.Callback{

    public static final int READ_MSG_FROM_ICC = 0;
    public static final int Delete_MSG_FROM_ICC = 1;
    public static final int INSERT_MSG_TO_ICC = 2;
    private  Context mContext;
    private static Handler sMainHandler;
    private static Handler sSubHandler;
    private ICCMessageManager mins;

    private ICCMessageManager getIccManager(){ return mins;}
    public DoSomeworkThread(String name) {
        super(name);
    }

    public DoSomeworkThread(Context context, Handler mainHandler, String name, ICCMessageManager ins) {
        this(name);
        this.mContext = context;
        this.sMainHandler = mainHandler;
        mins = ins;
    }

    public static Handler initialDoSomeworkThread(Context context, Handler mainHandler,ICCMessageManager ins) {
        DoSomeworkThread mIccReadDelayThread = new DoSomeworkThread(context, mainHandler,
                "IccReadDelayThread", ins);
        mIccReadDelayThread.start();
        sSubHandler = new Handler(mIccReadDelayThread.getLooper(), mIccReadDelayThread);
        return sSubHandler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d("ICCMessageManager", "enter handleMessage msg.what = " + msg.what);
        try {
            switch (msg.what) {
                case READ_MSG_FROM_ICC:
                    String fromSimLoaded = (String) msg.obj;
                    if ("true".equals(fromSimLoaded)) {
                        Log.d("ICCMessageManager", "fromSimLoaded");
                        Status.GetInstance().SetFirstInit(true);
                    }
                    Status.GetInstance().CheckCanLoader();
                    break;

                case Delete_MSG_FROM_ICC: {
               /* List<ICCMessageManager.DeleteParameter> list = ICCMessageManager.getInstance().getList();
                for( int index = 0; index < list.size(); ++ index){
                    ICCMessageManager.DeleteParameter ins = list.remove(0);
                    ICCMessageManager.getInstance().DeleteIcc(ins.getMessageID(), ins.getSubID(), ins.getWhere(), ins.getArgs());
                }*/
                    Object obj = msg.obj;
                    if (obj instanceof ICCMessageManager.DeleteParameter) {
                        Log.d("ICCMessageManager", " obj instanceof ICCMessageManager.DeleteParameter)");
                        ICCMessageManager.DeleteParameter ins = (ICCMessageManager.DeleteParameter) obj;
                        this.getIccManager().DeleteIcc(ins.getMessageID(), ins.getSubID(), ins.getWhere(), ins.getArgs());

                    } else {
                        Log.d("ICCMessageManager", " DoSomeworkThread obj = " + obj.getClass());
                    }
                }
                break;

                case INSERT_MSG_TO_ICC:
                    Object obj = msg.obj;
                    if (obj instanceof ICCMessageManager.DeleteParameter) {
                        Log.d("ICCMessageManager", " obj instanceof ICCMessageManager.DeleteParameter)");
                        ICCMessageManager.DeleteParameter ins = (ICCMessageManager.DeleteParameter) obj;
                        getIccManager().copyMessageToIcc(ins.getBody(), ins.getAddress(), Long.valueOf(ins.getDateTime()),
                                Integer.valueOf(ins.getSubID()).intValue(), String.valueOf(ins.getMessageID()));
                        ICCMessageManager.getInstance().syncIndexFromICC(Integer.valueOf(ins.getSubID()).intValue(), String.valueOf(ins.getMessageID()));
                        // reload sim messaging;
//                        Status.GetInstance().SetFirstInit(true);
//                        Status.GetInstance().CheckCanLoader();

                    } else {
                        Log.d("ICCMessageManager", " DoSomeworkThread obj = " + obj.getClass());
                    }
                    break;
                default:

            }
            //  quitSafely();
        }catch(Exception e){
            e.printStackTrace();
        }
      //  quitSafely();
        return true;
    }
}
