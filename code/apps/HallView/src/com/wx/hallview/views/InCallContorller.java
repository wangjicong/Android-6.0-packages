package com.wx.hallview.views;

/**
 * Created by Administrator on 16-1-26.
 */

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.wx.hallview.ViewContorller;
//import com.wx.hallview.fragment.InCallFragment;

public class InCallContorller {
   /* public static int CALL_TYPE_INCOMMING = 3;
    public static int CALL_TYPE_OUTGOING = 2;
    public static int CALL_TYPE_UNKNOWN = 1;
    private int mCallType = CALL_TYPE_UNKNOWN;
    private int mCurrentCallState = 0;
    private ViewContorller mFragmentContorller;
    private InCallFragment mInCallFragment;
    private PhoneStateListener mListener = new PhoneStateListener() {
        public void onCallStateChanged(int paramInt, String paramString) {
            Log.d("InCallContorller", "onCallStateChanged state = " + paramInt);
            mCurrentCallState = paramInt;
            if (InCallContorller.this.mPreviousState == paramInt)
                return;
            InCallContorller.this.setCallState(paramInt);
            switch (paramInt) {

                case 0:
                    if ("incall".equals(InCallContorller.this.mFragmentContorller.getCurrentFragmentTag())) {
                        InCallContorller.this.mFragmentContorller.moveToPreviousFragment();
                    }
                    InCallContorller.this.resetCallType();
                    break;
                case 1:
                    InCallContorller.this.setIsOutgoing(false);
                    break;
                case 2:
                    if (InCallContorller.this.mPreviousState == 0) {
                        InCallContorller.this.setIsOutgoing(true);
                    } else {
                        InCallContorller.this.setIsOutgoing(false);
                    }
                    break;
                default:
                    break;
            }
            InCallContorller.this.mPreviousState = paramInt;

        }

    };
    */private int mPreviousState = 0;
    private TelephonyManager mTelephonyManager;

    public InCallContorller(Context paramContext, ViewContorller paramViewContorller) {
   /*     this.mTelephonyManager = ((TelephonyManager) paramContext.getSystemService("phone"));
        this.mTelephonyManager.listen(this.mListener, 32);
        this.mFragmentContorller = paramViewContorller;
   */ }
/*
    private void resetCallType() {
        if (this.mInCallFragment != null)
            this.mInCallFragment.resetCallType();
        this.mCallType = CALL_TYPE_UNKNOWN;
    }

    private void setCallState(int paramInt) {
        if (this.mInCallFragment != null)
            this.mInCallFragment.setCallState(paramInt);
    }

    private void setIsOutgoing(boolean paramBoolean) {
        Log.d("InCallContorller", "set call isOutgoing " + paramBoolean);
        if (this.mInCallFragment != null)
            this.mInCallFragment.setIsOutgoing(paramBoolean);
        if (paramBoolean) {
            this.mCallType = CALL_TYPE_OUTGOING;
            return;
        }
        this.mCallType = CALL_TYPE_INCOMMING;
    }

    public int getCallType() {
        return this.mCallType;
    }

    public void setIncallFragment(InCallFragment paramInCallFragment) {
        this.mInCallFragment = paramInCallFragment;
        this.mInCallFragment.setInCallContorller(this);
    }
*/
    public boolean shouldShowIncall() {
        //return this.mCurrentCallState != 0;
        return false;
    }
}
