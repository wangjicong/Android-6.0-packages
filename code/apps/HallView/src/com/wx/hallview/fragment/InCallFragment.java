package com.wx.hallview.fragment;

/**
 * Created by Administrator on 16-1-23.
 */
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.view.View;
import com.wx.hallview.views.AnswerRejectConainer;
import android.content.ServiceConnection;
import android.widget.TextView;
import android.os.Handler;
import android.os.IBinder;
import com.wx.hallview.InCallContorller;
import android.os.PowerManager;
import android.widget.ImageView;
import com.android.incallui.IHallCallBackService;
import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.net.Uri;
import com.wx.hallview.views.AnswerRejectConainer;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.util.Log;
import android.os.Message;
import android.text.TextUtils;
import com.wx.hallview.R;
import android.text.format.DateUtils;
import android.content.ComponentName;

public class InCallFragment extends BaseFragmentView implements View.OnClickListener, AnswerRejectConainer.AnswerRejctListener {
    private AnswerRejectConainer mAnswerViewContainer;
    private AsyncQueryHandler mAsyncQueryHandler;
    private int mCallState;
    private TextView mElapsedTimeView;
    private View mEndCallButton;
    private InCallContorller mInCallContorller;
    private PowerManager.WakeLock mInCallWakeLock;
    private TextView mNameTextView;
    private String mPhoneNumber;
    private ImageView mPhotoView;
    private IHallCallBackService mService;
    private int mCallType = InCallContorller.CALL_TYPE_UNKNOWN;
    
    private final int SET_PHOTO_MSG = 3;
    private final int SET_NAME_MSG = 2;
    private final int SET_ELAPSED_TIME_MSG = 4;
    private final int SET_CALL_STATE_MSG = 1;
    private final int SET_INCALL_WAKE_LOCK_MSG = 5;
    
    public boolean needShowBackButton() {
        return false;
    }
    
    public InCallFragment(Context context) {
        super(context);
        PowerManager powerManager = (PowerManager)context.getSystemService("power");
        mInCallWakeLock = powerManager.newWakeLock(0x3000001a, "HallViewInCall");
    }
    
    public void setInCallContorller(InCallContorller contorller) {
        mInCallContorller = contorller;
    }
    
    class ContactAsyncQueryHandler extends AsyncQueryHandler {
        
        public ContactAsyncQueryHandler(InCallFragment p1, ContentResolver resolver) {
            super(resolver);
        }
        
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if(cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndex("display_name"));
                String photoUri = cursor.getString(cursor.getColumnIndex("photo_uri"));
                setNameText(name);
                setPhoto(photoUri);
            }
        }
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.incall_view, container, false);
        mAnswerViewContainer = (AnswerRejectConainer)view.findViewById(R.id.answer_view);
        mAnswerViewContainer.setAnswerRejctListener(this);
        mEndCallButton = view.findViewById(R.id.bottom_endcall_button);
        mNameTextView = (TextView)view.findViewById(R.id.text_name);
        mPhotoView = (ImageView)view.findViewById(R.id.user_head);
        mElapsedTimeView = (TextView)view.findViewById(R.id.elapsed_time);
        mEndCallButton.setOnClickListener(this);
        mAsyncQueryHandler = new InCallFragment.ContactAsyncQueryHandler(this, getContext().getContentResolver());
        return view;
    }
    
    private void startQueryCallerInfo(String number) {
        Uri contactRef = ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI.buildUpon().appendPath(number).build();
        mAsyncQueryHandler.startQuery(0, null, contactRef, null, null, null, null);
    }
    
    public void setPhoneNumber(String phoneNumber) {
        mPhoneNumber = phoneNumber;
        mNameTextView.setText(mPhoneNumber);
        startQueryCallerInfo(phoneNumber);
    }
    
    public void resetCallType() {
        mCallType = InCallContorller.CALL_TYPE_UNKNOWN;
    }
    
    public void setIsOutgoing(boolean outgoing) {
        if(outgoing) {
            mCallType = InCallContorller.CALL_TYPE_OUTGOING;
        } else {
            mCallType = InCallContorller.CALL_TYPE_INCOMMING;
        }
        initType();
    }
    
    public void setPhoto(String photoUri) {
        if(!TextUtils.isEmpty(photoUri)) {
            Message message = mHandler.obtainMessage(SET_PHOTO_MSG);
            message.obj = Uri.parse(photoUri);
            message.sendToTarget();
        }
    }
    
    public void setNameText(String text) {
        Message message = mHandler.obtainMessage(SET_NAME_MSG);
        message.obj = text;
        message.sendToTarget();
    }
    
    public void setCallState(int callState) {
        mCallState = callState;
        mHandler.sendEmptyMessage(SET_CALL_STATE_MSG);
        mHandler.removeMessages(SET_ELAPSED_TIME_MSG);
        if(callState == 2) {
            mHandler.sendEmptyMessage(SET_ELAPSED_TIME_MSG);
            return;
        }
        mElapsedTimeView.setText("");
        mElapsedTimeView.setVisibility(View.GONE);
    }
    private Handler mHandler = new Handler() {
        
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch(what) {
                case SET_CALL_STATE_MSG:
                {
                	initType();
                    return;
                }
                case SET_NAME_MSG:
                {
                    mNameTextView.setText((String)msg.obj);
                    return;
                }
                case SET_PHOTO_MSG:
                {
                    Uri uri =(Uri)msg.obj;
                    if(uri == null) {
                        mPhotoView.setImageResource(R.drawable.head_photo);
                        return;
                    }
                    mPhotoView.setImageURI((Uri)msg.obj);
                    return;
                }
                case SET_ELAPSED_TIME_MSG:
                {
                    if(mService != null) {
                        long time = -1;
                        try {
                            time = mService.getCallElapsedTime();
                        } catch(RemoteException localRemoteException1) {
                        }
                        System.out.println("yadong time = " + time);
                        if((time  == -1) || (time == 0)) {
                            if(mCallType == InCallContorller.CALL_TYPE_OUTGOING) {
                                if(mElapsedTimeView.getVisibility() != View.VISIBLE) {
                                    mElapsedTimeView.setVisibility(View.VISIBLE);
                                }
                                mElapsedTimeView.setText(R.string.wating);
                            } else if(mElapsedTimeView.getVisibility() != View.INVISIBLE) {
                                mElapsedTimeView.setVisibility(View.INVISIBLE);
                            }
                        } else if(mElapsedTimeView.getVisibility() != View.VISIBLE) {
                            mElapsedTimeView.setVisibility(View.VISIBLE);
                        } else {
                            long duration = System.currentTimeMillis() - time;
                            String callTimeElapsed = DateUtils.formatElapsedTime(duration / 1000);
                            mElapsedTimeView.setText(callTimeElapsed);
                        }
                        mHandler.sendEmptyMessageDelayed(SET_ELAPSED_TIME_MSG, 990);
                        return;
                    }
                    break;
                }
                case SET_INCALL_WAKE_LOCK_MSG:
                {
                    if(mInCallWakeLock.isHeld()) {
                        mInCallWakeLock.release();
                        break;
                    }
                }
                
            }
        }
    };
    
    private void initType() {
        if(mCallType == InCallContorller.CALL_TYPE_UNKNOWN) {
            mAnswerViewContainer.setVisibility(View.GONE);
            mEndCallButton.setVisibility(View.GONE);
            return;
        }
        if((mCallType == InCallContorller.CALL_TYPE_OUTGOING) || (mCallState == 2)) {
            mAnswerViewContainer.setVisibility(View.GONE);
            mEndCallButton.setVisibility(View.VISIBLE);
            //mNameTextView.setVisibility(View.VISIBLE);//qiuyaobo,20160317, for bug51018
            return;
        }
        mEndCallButton.setVisibility(View.GONE);
        mAnswerViewContainer.setVisibility(View.VISIBLE);
       // mNameTextView.setVisibility(View.GONE);//qiuyaobo,20160317, for bug51018
    }
    
    private void bindInCallCallBackService() {
        Intent intent = new Intent();
        intent.setClassName("com.android.dialer", "com.android.incallui.HallCallBackService");
        getContext().bindService(intent, mCallBackConnection, Context.BIND_AUTO_CREATE);
    }
    
    public void onHide() {
    }
    
    public void onAttach() {
        bindInCallCallBackService();
        if(mInCallContorller != null) {
            mCallType = mInCallContorller.getCallType();
            initType();
        }
        acquire();
    }
    
    private void acquire() {
        mInCallWakeLock.acquire();
        mHandler.sendEmptyMessageDelayed(SET_INCALL_WAKE_LOCK_MSG, 10000);
    }
    
    public void onDetach() {
        Log.d("InCallFragment", "on hide called!");
        if((mService != null) && (mCallState != 0)) {
            try {
                Log.d("InCallFragment", "call state is not idel, request to launch system Incall");
                mService.requestInCallUI();
            } catch(RemoteException exception) {
                exception.printStackTrace();
            }
        }
        getContext().unbindService(mCallBackConnection);
        mHandler.removeMessages(SET_ELAPSED_TIME_MSG);
        if(mInCallWakeLock.isHeld()) {
            mInCallWakeLock.release();
            mHandler.removeMessages(SET_INCALL_WAKE_LOCK_MSG);
        }
    }
    private ServiceConnection mCallBackConnection = new ServiceConnection() {
        
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
        
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IHallCallBackService.Stub.asInterface(service);
            String number = null;
            if(mService != null) {
                try {
                    number = mService.getOutgoingCallNumber();
                    if(mCallState == 2) {
                        mHandler.sendEmptyMessage(SET_ELAPSED_TIME_MSG);
                    }
                } catch(RemoteException e) {
                    e.printStackTrace();
                }
                setPhoneNumber(number);
            }
        }
    };
    
    public void doAction(AnswerRejectConainer.AnswerRejctListener.Action action) {
        if(action == AnswerRejectConainer.AnswerRejctListener.Action.Answer) {
            if(mService != null) {
                try {
                    mService.answerInCommingCall();
                } catch(RemoteException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }else if(action == AnswerRejectConainer.AnswerRejctListener.Action.Rejcet) {
            if(mService != null) {
               try {
                   mService.declineIncomingCall();
               }catch(RemoteException e) {
                e.printStackTrace();
                throw new RuntimeException();
              }
               
            } 
        }
    }
    
    public void onClick(View v) {
        if(mService != null) {
            try {
                mService.hangUpOngoingCall();
            } catch(RemoteException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }
    }
}
