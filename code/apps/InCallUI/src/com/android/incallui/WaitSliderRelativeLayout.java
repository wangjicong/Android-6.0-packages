package com.android.incallui;



import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.telecom.TelecomManager;


public class WaitSliderRelativeLayout extends RelativeLayout {

	private static String TAG = "SliderRelativeLayout";

	private TextView tv_slider_icon = null; 
	private ImageView wait_reject=null,wait_answer=null;
	private Bitmap dragBitmap = null; 
	private Context mContext = null; 
	private InCallActivity mInCallActivity ;

	public WaitSliderRelativeLayout(Context context) {
		super(context);
		mContext = context;
		initDragBitmap();
	}

	public WaitSliderRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs, 0);
		mContext = context;
		initDragBitmap();
	}

	public WaitSliderRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initDragBitmap();
	}

	private void initDragBitmap() {
		if (dragBitmap == null)
			
			dragBitmap = BitmapFactory.decodeResource(mContext.getResources(),
					R.drawable.wait_circle);
	}
	
	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		
		tv_slider_icon = (TextView) findViewById(R.id.slider_icon);
	}
	private int mLastMoveX = 1000; 
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();

		//Log.i("huangjun444","onTouchEvent *********** x="+x+"  y="+y);
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLastMoveX = (int) event.getX();
			//Log.i("huangjun444","onTouchEvent *********** mLastMoveX="+mLastMoveX);
			resetViewState();
			return handleActionDownEvenet(event);
		case MotionEvent.ACTION_MOVE:
			mLastMoveX = x; 
            invalidate(); 	    
			return true;
		case MotionEvent.ACTION_UP:
			
			handleActionUpEvent(event);
			return true;
		}
		return super.onTouchEvent(event);
	}

	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);		

		invalidateDragImg(canvas);
	}

	private void invalidateDragImg(Canvas canvas) {
		int drawXCor = mLastMoveX - dragBitmap.getWidth()/2;
		int drawYCor = tv_slider_icon.getTop();
	
		wait_answer=(ImageView)findViewById(R.id.wait_answer);
		int wait_answerX=wait_answer.getLeft();
		if(drawXCor>wait_answerX&&drawXCor<900){
			
			canvas.drawBitmap(dragBitmap,  wait_answerX  , drawYCor , null);
		}
		else{
			canvas.drawBitmap(dragBitmap,  drawXCor < 25 ? 25 : drawXCor , drawYCor , null);
		}
	}


	private boolean handleActionDownEvenet(MotionEvent event) {
		Rect rect = new Rect();
		tv_slider_icon.getHitRect(rect);
		boolean isHit = rect.contains((int) event.getX(), (int) event.getY());
		//Log.i("huangjun444","handleActionDownEvenet ***********rect="+rect+"  isHit="+isHit);
	
		if(isHit) 
			tv_slider_icon.setVisibility(View.INVISIBLE);
		
		return isHit;
	}

	private static int BACK_DURATION = 20 ;  
   
	private static float VE_HORIZONTAL = 0.7f ; 
	
   
	private void handleActionUpEvent(MotionEvent event){	
		wait_reject=(ImageView)findViewById(R.id.wait_reject);
		wait_answer=(ImageView)findViewById(R.id.wait_answer);
		int wait_rejectX=wait_reject.getRight();
		int wait_answerX=wait_answer.getLeft();
		int x = (int) event.getX() ;	
		//Log.i("huangjun444","handleActionUpEvent *********** x="+x+"  wait_rejectX="+wait_rejectX+"  wait_answerX="+wait_answerX);
		boolean answerSucess= (x - wait_answerX) >= 10 ;//15
		
		boolean rejectSucess=(x- wait_rejectX) <= 10 ;//15

		//Log.i("huangjun444","handleActionUpEvent**********   answerSucess="+answerSucess+"  rejectSucess="+rejectSucess);
		if(answerSucess){
		 //  Toast.makeText(mContext, "answerSucess", 1000).show();
		   resetViewState();	
		  // virbate(); 
		 //answerPhone() ;
		   mInCallActivity.slideToAnswer();
		   wait_answer.setVisibility(View.INVISIBLE);
		}
		else if(rejectSucess){
		//	Toast.makeText(mContext, "rejectSucess", 1000).show();
			   resetViewState();
			   virbate();
			// endPhone();
			   mInCallActivity.slideToReject();
		}
		else {
			resetViewState();
		}
	}

	private void resetViewState(){
		mLastMoveX = 1000 ;
		tv_slider_icon.setVisibility(View.VISIBLE);
		invalidate(); 
	}

	private void virbate(){
		Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(200);
	}

    TelecomManager getTelecommService() {
		//android.util.Log.d("huangjun444", "WaitSliderRelativeLayout.java getTelecommService()   mContext: " + mContext );
        return (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
 
      // return (TelecomManager) this.getSystemService(Context.TELECOM_SERVICE);		
    }

    public  void endPhone() {
                    TelecomManager telecomManager = getTelecommService();
                    //android.util.Log.d("huangjun444", "WaitSliderRelativeLayout.java endphone()   telecomManager: " + telecomManager );
                    if (telecomManager != null) {	
                        telecomManager.endCall();
                    }
    	}


    public  void answerPhone() {
                    TelecomManager telecomManager = getTelecommService();
                    //android.util.Log.d("huangjun444", "WaitSliderRelativeLayout.java answerPhone()   telecomManager: " + telecomManager );
                    if (telecomManager != null) {		
                        if (telecomManager.isRinging()) {
                         //   Log.i(TAG, "interceptKeyBeforeQueueing:"
                         //         + " CALL key-down while ringing: Answer the call!");
                                                   telecomManager.acceptRingingCall();
                            }
                }
         }

    public void setInCallActivity(InCallActivity ic){
        mInCallActivity = ic;
    }
}
