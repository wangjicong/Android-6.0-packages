package com.android.incallui;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import java.io.PrintStream;

public class HallCallBackService extends Service
{
  private final IBinder mBinder = new ServiceStub();
  private Handler mHandler = new Handler()
  {
    public void handleMessage(Message paramMessage)
    {
      switch (paramMessage.what)
      {
      
      case 1:
        InCallPresenter.getInstance().answerIncomingCall(HallCallBackService.this, 0);
        return;
      case 2:
        InCallPresenter.getInstance().declineIncomingCall(HallCallBackService.this);
        return;
      case 3:
        InCallPresenter.getInstance().hangUpOngoingCall(HallCallBackService.this);
        return;
      
      default:
        return;
      }
      
    }
  };

  public IBinder onBind(Intent paramIntent)
  {
    return this.mBinder;
  }

  public void onCreate()
  {
    super.onCreate();
    System.out.println("yadong on hall call back service onCreate, thread = " + Thread.currentThread().getName());
  }

  class ServiceStub extends IHallCallBackService.Stub
  {
    public ServiceStub()
    {
    }

    public void answerInCommingCall()
    {
      HallCallBackService.this.mHandler.sendEmptyMessage(1);
    }

    public void declineIncomingCall()
    {
      HallCallBackService.this.mHandler.sendEmptyMessage(2);
    }

    public long getCallElapsedTime()
    {
      return InCallPresenter.getInstance().getCallElapsedTime();
    }

    public String getOutgoingCallNumber()
    {
      return InCallPresenter.getInstance().getOutgoingCallNumber();
    }

    public void hangUpOngoingCall()
    {
      HallCallBackService.this.mHandler.sendEmptyMessage(3);
    }

    public void requestInCallUI()
    {
      InCallPresenter.getInstance().showInCallFromHall();
    }
  }
}