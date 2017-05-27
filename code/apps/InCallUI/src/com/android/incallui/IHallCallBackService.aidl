package com.android.incallui;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

interface IHallCallBackService
{
  void answerInCommingCall();

  void declineIncomingCall();

  long getCallElapsedTime();

  String getOutgoingCallNumber();

  void hangUpOngoingCall();

  void requestInCallUI();
}