package com.wx.hallview;

import com.wx.hallview.fragment.InCallFragment;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.util.Log;

public class InCallContorller {
	
    private ViewContorller mFragmentContorller;
    private InCallFragment mInCallFragment;
    private TelephonyManager mTelephonyManager;
    public static int CALL_TYPE_UNKNOWN = 1;
    public static int CALL_TYPE_OUTGOING = 2;
    public static int CALL_TYPE_INCOMMING = 3;
    private int mPreviousState = 0;
    private int mCurrentCallState = 0;
    private int mCallType = CALL_TYPE_UNKNOWN;
    
    public InCallContorller(Context context, ViewContorller contorller) {
        mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);
        mFragmentContorller = contorller;
    }
    
    public int getCallType() {
        return mCallType;
    }
   
    private void setCallState(int state) {
        if(mInCallFragment != null) {
            mInCallFragment.setCallState(state);
        }
    }

    
    public boolean shouldShowIncall() {
        return (mCurrentCallState != 0);
    }
    
    private void setIsOutgoing(boolean isOutgoing) {
        Log.d("InCallContorller", "set call isOutgoing " + isOutgoing);
        if(mInCallFragment != null) {
            mInCallFragment.setIsOutgoing(isOutgoing);
        }
        if(isOutgoing) {
            mCallType = CALL_TYPE_OUTGOING;
            return;
        }
        mCallType = CALL_TYPE_INCOMMING;
    }

	public void setIncallFragment(InCallFragment paramInCallFragment){
	    mInCallFragment = paramInCallFragment;
	    mInCallFragment.setInCallContorller(this);
	    
	    mInCallFragment.setCallState(mCurrentCallState);//qiuyaobo,20160912
	}

    private void resetCallType() {
        if(mInCallFragment != null) {
            mInCallFragment.resetCallType();
        }
        mCallType = CALL_TYPE_UNKNOWN;
    }
    
    private PhoneStateListener mListener = new PhoneStateListener() {
        
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.d("InCallContorller", "onCallStateChanged state = " + state);
            mCurrentCallState = state;
            if(mPreviousState == state) {
                return;
            }
            setCallState(state);
            switch(state) {
                case 0:
                
                    if("incall".equals(mFragmentContorller.getCurrentFragmentTag())) {
                        mFragmentContorller.moveToPreviousFragment();
                    } 
                    resetCallType(); 
                   
                    break;
               case 1:
                	setIsOutgoing(false);
                    break;
                
               case 2:
                    if(mPreviousState == 0) {
                        setIsOutgoing(true);
                    } else {
                        setIsOutgoing(false);
                    }
                    break;
                
            }
            mPreviousState = state;
        }
    };
}


