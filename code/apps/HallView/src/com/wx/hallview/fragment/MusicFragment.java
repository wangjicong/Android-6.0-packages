package com.wx.hallview.fragment;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.music.IMediaPlaybackService;
import com.wx.hallview.views.utils.ColorUtil;
import com.wx.hallview.views.utils.MusicUtil;
import com.wx.hallview.R;

import java.util.List;

public class MusicFragment extends BaseFragmentView {
	private static final String TAG = "MusicFragment";
    private static final String PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    private static final String META_CHANGED = "com.android.music.metachanged";
    private static final String QUEUE_CHANGED = "com.android.music.queuechanged";
    private static final String QUIT_PLAYBACK = "com.android.music.quitplayback";

    private AlbumArtHandler mAlbumArtHandler;
    private Bitmap mArtBitmap;
    private long mArtSongId;
    private TextView mSongNameTextView;
	private TextView mArtistNameTextView;
    private ImageView mArtworkImageView;
    private long mDuration;
	private Button mPrev,mPlay,mNext;
    private int mPrimaryColor;
    private ProgressBar mProgressBar;
    private ObjectAnimator mRotate;
    private IMediaPlaybackService mService;
	
    public MusicFragment(Context context) {
        super(context);
		Log.d(TAG, "MusicFragment!");
    }
    
	public View onCreateView(LayoutInflater inflater, ViewGroup container){
		Log.d(TAG, "onCreateView!");
		View view = inflater.inflate(R.layout.music_view, container, false);
        mArtworkImageView = ((ImageView)view.findViewById(R.id.image_album));
		mArtistNameTextView = (TextView) view.findViewById(R.id.text_artist_name);
		mSongNameTextView = (TextView) view.findViewById(R.id.text_name);
		mProgressBar = (ProgressBar)view.findViewById(R.id.progress);
		mProgressBar.setMax(1000);
        mPlay = (Button) view.findViewById(R.id.btn_play_pause);
        mPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"mPlay onClick mPlay mService="+mService);
                if (mService == null){
                    bindMusicService();
                    return;
                }
				doPauseResume();
            }
        });
        mPrev = (Button) view.findViewById(R.id.btn_prev);
        mPrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"mPrev onClick mService="+mService);
                if (mService != null) {
                    try {
                        mService.prev();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mNext = (Button) view.findViewById(R.id.btn_next);
        mNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService != null) {
                    try {
                        mService.next();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
		Log.d(TAG, "onCreateView 02!");
		mAlbumArtHandler = new AlbumArtHandler();
		Log.d(TAG, "onCreateView 03!");
		mRotate = ObjectAnimator.ofFloat(mArtworkImageView, "rotation", 0f, 360f);
        mRotate.setDuration(40000);
        mRotate.setRepeatCount(-1);
        mRotate.setInterpolator(new LinearInterpolator());
        Log.d(TAG, "onCreateView 04!");
        return view;
    }
    
    public void onScreenOff() {
        mRotate.pause();
        mHandler.removeMessages(1);
    }
    
    public void onScreenOn() {
        setProgressBarProcess();
        try {
            boolean playing = mService.isPlaying();
            if(playing) {
                mRotate.resume();
                return;
            }
        } catch(RemoteException e) {
            e.printStackTrace();
        }
    }
    private boolean isRunedRotate = false;
    
    private Bitmap mLastBitmap = null;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            // :( Parsing error. Please contact me.
            switch (msg.what) {
        		case 1:
					setProgressBarProcess();
					break;
				case 2:
					if(mLastBitmap == null || !mLastBitmap.equals((Bitmap)msg.obj)){
						mArtworkImageView.setImageBitmap((Bitmap)msg.obj);
						mLastBitmap = (Bitmap)msg.obj;
						new ColocLoader().execute(new Bitmap[] { (Bitmap)msg.obj });
					}
					break;
				case 5:
	        		int[] result = (int[])msg.obj;
					Log.d(TAG,"mHandler result="+result);
			        if (result != null){
			          mPrimaryColor = result[0];
					}
			        ColorUtil.setBackgroundColor(mPrev, mPrimaryColor);
					ColorUtil.setBackgroundColor(mPlay, mPrimaryColor);
					ColorUtil.setBackgroundColor(mNext, mPrimaryColor);
					mSongNameTextView.setTextColor(mPrimaryColor);
	                mArtistNameTextView.setTextColor(mPrimaryColor);
				  	break;
        		default:  
            		super.handleMessage(msg);
            	break;  
    		}  
        }
    };
	public class AlbumArtHandler extends Handler{
    	private long mAlbumId = -1;

	    public AlbumArtHandler(Looper arg2)
	    {
	      super();
	    }
		public AlbumArtHandler(/*Looper arg2*/)
	    {
	      super();
	    }

	    public void handleMessage(Message paramMessage)
	    {
	      long l1 = ((MusicFragment.AlbumSongIdWrapper)paramMessage.obj).albumid;
	      long l2 = ((MusicFragment.AlbumSongIdWrapper)paramMessage.obj).songid;
	      if ((paramMessage.what == 3) && ((mAlbumId != l1) || (l1 < 0)))
	      {
	        if ((mArtBitmap == null) || (mArtSongId != l2))
	        {
	          mHandler.obtainMessage(2, null);
	          mArtBitmap = MusicUtil.getArtwork(getContext(), l2, l1, false);
	          Log.d(TAG, "get art. mArtSongId = " + mArtSongId + " ,songid = " + l2);
	          mArtSongId = l2;
	        }
	        if (mArtBitmap == null)
	        {
	          mArtBitmap = MusicUtil.getDefaultArtwork(getContext());
	          l1 = -1;
	        }
	        if(mArtBitmap != null)
	        {
	          paramMessage = mHandler.obtainMessage(2,mArtBitmap);
	          mHandler.removeMessages(2);
	          mHandler.sendMessage(paramMessage);
	        }
	        mAlbumId = l1;
	      }
	    }
	}

    public class ColocLoader extends AsyncTask<Bitmap, Void, int[]>{

		public ColocLoader(){
		}  

		@Override 
        protected int[] doInBackground(Bitmap... paramVarArgs) {
        	Log.d(TAG,"doInBackground");
            return ColorUtil.colorFromBitmap(paramVarArgs[0]);
        }

		@Override 
        protected void onPostExecute(int[] result) {
        	Log.d(TAG,"onPostExecute");
            Message message = mHandler.obtainMessage(5);
            message.obj = result;
            message.sendToTarget();
        }
    }
    
    private void setProgressBarProcess() {
        if((mService == null) || (mDuration == 0)) {
            return;
        }
        try {
            long position = mService.position();
            int progress = (int)((1000 * position) / mDuration);
            mProgressBar.setProgress(progress);
            Message message = mHandler.obtainMessage(1);
            mHandler.sendMessageDelayed(message, 1000);
            return;
        } catch(RemoteException e) {
            e.printStackTrace();
            return;
        } catch(IllegalStateException e) {
            e.printStackTrace();
        }
    }
  
    private static class AlbumSongIdWrapper {
        public long albumid;
        public long songid;
        
        AlbumSongIdWrapper(long aid, long sid) {
            albumid = aid;
            songid = sid;
        }
    }

    private static class Worker implements Runnable {
        private Looper mLooper;
        private final Object mLock = new Object();
        
        Worker(String name) {
			Log.d(TAG,"Worker wait 001!");
            Thread thread = new Thread(null, this, name);
            thread.setPriority(1);
            thread.start();
			Log.d(TAG,"Worker wait 002!");
            synchronized(mLock) {
                while(mLooper == null) {
                    try {
						Log.d(TAG,"Worker wait 003!");
                        mLock.wait();
						Log.d(TAG,"Worker wait 004!");
						continue;
                    } catch(InterruptedException localInterruptedException) {
                    	Log.d(TAG,"Worker wait 005!");
                    }
					Log.d(TAG,"Worker wait 006!");
                }
				Log.d(TAG,"Worker wait end!");
            }
        }
        
        public Looper getLooper() {
			Log.d(TAG,"Worker getLooper="+mLooper);
            return mLooper;
        }
        
        public void run() {
            synchronized(mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
				Log.d(TAG,"Worker run mLooper="+mLooper);
                mLock.notifyAll();
				Looper.loop();
				return;
            }
        }
    }

    public boolean bindMusicService(){
        Log.d(TAG,"bindMusicService");
        if(!isMusicServiceRunning()){
            Intent intent = new Intent();  
            intent.setComponent(new ComponentName("com.android.music",
                                "com.android.music.MediaPlaybackService"));
            intent.putExtra("command","next");
            getContext().startService(intent);
        }
		Intent intent = new Intent("com.android.music.MediaPlaybackService");
        intent.setClassName("com.android.music", "com.android.music.MediaPlaybackService");
        return getContext().bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);
    }
    private boolean isMusicServiceRunning() {
        boolean isServiceRuning = false;
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        final int maxServciesNum = 100;
        List<RunningServiceInfo> list = am.getRunningServices(maxServciesNum);
        for (RunningServiceInfo info : list) {
            if (info.service.getClassName().equals("com.android.music.MediaPlaybackService")) {
                isServiceRuning = true;
                break;
            }
        }
		Log.d(TAG,"isMusicServiceRunning isServiceRuning="+isServiceRuning);
        return isServiceRuning;
    }

    ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IMediaPlaybackService.Stub.asInterface(service);
			if(mService != null) {
                try {
                    mService.duration();
                } catch(RemoteException e) {
                    e.printStackTrace();
                }
            }
            updateMusicPanel();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceConnection = null;
            mService = null;
        }
    };

    private void updateMusicPanel() {
        if (mService != null) {
			mAlbumArtHandler.removeMessages(3);
            try {
                CharSequence titleName = mService.getTrackName();
                mSongNameTextView.setText(titleName==null?"":titleName.toString());
				mArtistNameTextView.setText(mService.getArtistName());
                if(mService.isPlaying()){
                    mPlay.setBackgroundResource(R.drawable.stop_selector);
					if (isRunedRotate)
			        {
			          	mRotate.resume();
			        }else{
			        	mRotate.start();
			        	isRunedRotate = true;
					}
                }else{
                    mPlay.setBackgroundResource(R.drawable.play_selector);
                    mHandler.removeMessages(1);
					if(isRunedRotate){
						mRotate.pause();
					} 
				}
				ColorUtil.setBackgroundColor(mPrev, mPrimaryColor);
				ColorUtil.setBackgroundColor(mPlay, mPrimaryColor);
				ColorUtil.setBackgroundColor(mNext, mPrimaryColor);
				mDuration = mService.duration();
				setProgressBarProcess();
				AlbumSongIdWrapper localAlbumSongIdWrapper = new AlbumSongIdWrapper(mService.getAlbumId(), mService.getAudioId());
				Message localMessage = mAlbumArtHandler.obtainMessage(3);
      			localMessage.obj = localAlbumSongIdWrapper;
      			localMessage.sendToTarget();
      			
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            mSongNameTextView.setText(R.string.songnamenull);
			mArtistNameTextView.setText("");
            mPlay.setBackgroundResource(R.drawable.play_selector);
        }
    }
	public void startReceiver() {
        Log.i(TAG, "startReceiver music intent");
        IntentFilter f = new IntentFilter();
        f.addAction(this.PLAYSTATE_CHANGED);
        f.addAction(this.META_CHANGED);
        f.addAction(this.QUIT_PLAYBACK);
        f.addAction(this.QUEUE_CHANGED);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        getContext().registerReceiver(mStatusListener, new IntentFilter(f));
    }
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive mStatusListener: " + action);
            if (action.equals(META_CHANGED)) {
                updateMusicPanel();
            } else if (action.equals(PLAYSTATE_CHANGED)) {
                updateMusicPanel();
            } else if (action.equals(QUIT_PLAYBACK)) {
                updateMusicPanel();
            } else if (action.equals(QUEUE_CHANGED)) {
                updateMusicPanel();
            }
        }
    };
    public boolean needShowBackButton() {
        return false;
    }

    public void onAttach() {
        Log.d(TAG, "on attach called");
        if (isMusicServiceRunning()){
           bindMusicService();
			mRotate.resume();
        }
		startReceiver();
        updateMusicPanel();
		Log.d(TAG, "onAttach!");
    }
    
    public void onDetach() {
        mHandler.removeMessages(2);
        mHandler.removeMessages(1);
        /*bug51243:Have no unbindService --up170320@{*/
		getContext().unbindService(serviceConnection);
		mService = null;
		/*bug51243:Have no unbindService --up170320@}*/
        getContext().unregisterReceiver(mStatusListener);
        mRotate.pause();
    }

	private void doPauseResume(){
		if (mService == null){
			bindMusicService();
		}else{
			try {
				if(mService.isPlaying()){
					mService.pause();
				}else{
					mService.play();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}
