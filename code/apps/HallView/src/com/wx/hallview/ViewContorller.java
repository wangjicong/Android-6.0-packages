package com.wx.hallview;

/**
 * Created by Administrator on 16-1-20.
 */

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.widget.Button;
import com.wx.hallview.fragment.BaseFragmentView;
import com.wx.hallview.fragment.ChooseClockFragment;
import com.wx.hallview.fragment.ClockFragment;
import com.wx.hallview.fragment.DialerFragment;
import com.wx.hallview.fragment.InCallFragment;
import com.wx.hallview.fragment.LaunchFragment;
import com.wx.hallview.fragment.LaunchFragment.OnItemClick;
import com.wx.hallview.fragment.MmsFragment;
import com.wx.hallview.fragment.SettingFragment;
import com.wx.hallview.fragment.AudioProfileFragment; 
import com.wx.hallview.fragment.TimeoutFragment;
import com.wx.hallview.fragment.MusicFragment;
import com.wx.hallview.fragment.NumberClockFragment;
import com.wx.hallview.fragment.SportClockFragment;
import com.wx.hallview.fragment.WeatherFragment;
import com.wx.hallview.views.RootView;
import com.wx.hallview.views.RootView.AnimationEnd;
import com.wx.hallview.views.utils.DataSave;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sprd.android.config.OptConfig;
//Kalyy
import android.view.Gravity;
import android.graphics.PixelFormat;
import android.view.Display;
import android.graphics.Point;
import android.widget.RelativeLayout;
import android.os.SystemProperties;

public class ViewContorller
        implements View.OnClickListener, LaunchFragment.OnItemClick, RootView.AnimationEnd
{
    public static final Map<String, FragmentItem> ADDED_FRAGMENT = new LinkedHashMap();
    public static ViewContorller sViewContorller;
    private Button mBackButton;
    private Context mContext;
    private String mCurrentFragmentTag = null;
    private float mDownX = 0.0F;
    private float mDownY = 0.0F;
    public String mFistPage = "clock";
    private int mHallViewWidth;
    private InCallContorller mInCallContorller;
    private LayoutInflater mLayoutInflater;
    private String mPreviousTag = null;
    private RootView mRootView = null;
    private RelativeLayout mButtomView = null;//Kalyy
    private boolean mRootViewShowing = false;
    private Button mExit;

    static
    {
        ADDED_FRAGMENT.put("launch", new FragmentItem(-1));
        ADDED_FRAGMENT.put("incall", new FragmentItem(-1));
        ADDED_FRAGMENT.put("clock", new FragmentItem(-1));
        ADDED_FRAGMENT.put("digit_clock", new FragmentItem(-1));
        ADDED_FRAGMENT.put("roma_clock", new FragmentItem(-1));
        ADDED_FRAGMENT.put("audioprofile", new FragmentItem(-1));
        ADDED_FRAGMENT.put("timeout", new FragmentItem(-1));
        ADDED_FRAGMENT.put("dialer", new FragmentItem(R.drawable.launch_dialer));
        ADDED_FRAGMENT.put("mms", new FragmentItem(R.drawable.launch_mms));
        
        ADDED_FRAGMENT.put("music", new FragmentItem(R.drawable.launch_music));
        
        //qiuyaobo,remove weather,20160901,begin
        //ADDED_FRAGMENT.put("weather", new FragmentItem(R.drawable.launch_weather));
        //qiuyaobo,remove weather,20160901,end

        ADDED_FRAGMENT.put("choose_clock_style", new FragmentItem(R.drawable.launch_clock));

        ADDED_FRAGMENT.put("settings", new FragmentItem(R.drawable.launch_settings));
    }

    private ViewContorller(Context paramContext)
    {
        this.mContext = paramContext;
        this.mLayoutInflater = ((LayoutInflater)paramContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        this.mRootView = ((RootView)this.mLayoutInflater.inflate(R.layout.hall_view, null));
        this.mButtomView = ((RelativeLayout)this.mLayoutInflater.inflate(R.layout.buttom_view, null));//Kalyy
        this.mBackButton = (Button)this.mRootView.findViewById(R.id.back);
        this.mBackButton.setOnClickListener(this);
        this.mExit = (Button)this.mRootView.findViewById(R.id.exit);
        mExit.setOnClickListener(this);
        this.mHallViewWidth = this.mContext.getResources().getDimensionPixelOffset(R.dimen.hall_view_width);
        this.mInCallContorller = new InCallContorller(this.mContext, this);
        
        //qiuyaobo£¬20170316£¬begin
        if (OptConfig.SUN_CUSTOM_C7367_HWD_FWVGA_R2 || OptConfig.SUN_CUSTOM_C7367_HWD_FWVGA_R3 || OptConfig.SUN_CUSTOM_C7367_HWD_FWVGA_R8 || (OptConfig.SUN_CUSTOM_C7367_QHD_R9&&!OptConfig.SUN_SUBCUSTOM_C7367_HWD_QHD_R9_SINGTECH))
        {
            this.mExit.setVisibility(View.VISIBLE);
        }else{
            this.mExit.setVisibility(View.GONE);
        }   
        //qiuyaobo£¬20170316£¬end     
    }

    private void addRootViewToWindow()
    {
		/*SUN:jicong.wang add for exit button not translation {@*/
		mExit.setText(mContext.getResources().getString(R.string.exit));
		/*SUN:jicong.wang add for exit button not translation @}*/
				
        if (!this.mRootViewShowing){
            try {
                WindowManagerGlobal.getWindowManagerService().lockNow(null);
            }catch (RemoteException localRemoteException)
            {
                localRemoteException.printStackTrace();
            }
        
            WindowManager mWm = ((WindowManager)this.mContext.getSystemService(Context.WINDOW_SERVICE));//Kalyy
            WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            	ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.TYPE_SYSTEM_ERROR,
            	LayoutParams.FLAG_DISMISS_KEYGUARD|LayoutParams.FLAG_FULLSCREEN, -3);
            localLayoutParams.flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            	| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
			/*Bug47368:Leather interface is always vertical--up161010@{*/
			localLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			/*Bug47368:Leather interface is always vertical--up161010@}*/
            mWm.addView(this.mRootView, localLayoutParams);
            if(SystemProperties.get("qemu.hw.mainkeys").equals("0")){//Kalyy
                int buttom_height = 0;  
                int statusbar_height = 0;
                try {  
                    Class<?> clazz = Class.forName("com.android.internal.R$dimen");  
                    Object object = clazz.newInstance();  
                    int height = Integer.parseInt(clazz.getField("navigation_bar_height").get(object).toString());
                    buttom_height = mContext.getResources().getDimensionPixelSize(height);  
                    height = Integer.parseInt(clazz.getField("status_bar_height").get(object).toString());
                    statusbar_height =  mContext.getResources().getDimensionPixelSize(height);  
                } catch (Exception e) {  
                    e.printStackTrace();  
                }
                WindowManager.LayoutParams mLp = new WindowManager.LayoutParams();
                mLp.format = PixelFormat.RGBA_8888;
                mLp.type = WindowManager.LayoutParams.TYPE_DRAG;
                mLp.gravity = Gravity.LEFT | Gravity.TOP;
                mLp.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                Display display = mWm.getDefaultDisplay();
                Point p = new Point();
                display.getRealSize(p);
                int lcd_width;
                int lcd_height;
                if(p.y>p.x){
                    lcd_width = p.x;
                    lcd_height = p.y;
                }else{
                    lcd_width = p.y;
                    lcd_height = p.x;
                }
                mLp.x = 0;
                mLp.y = lcd_height - buttom_height - statusbar_height;
                mLp.width = lcd_width;
                mLp.height = buttom_height + statusbar_height;
                mLp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                mWm.addView(this.mButtomView, mLp);
            }
            this.mRootViewShowing = true;

          /* if(OptConfig.SUNVOV_S7350_HWD_V2_LM_HD_HOTWAV){ // sunvov hj 20161009
                int flag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (this.mRootView != null) {
                    this.mRootView.setSystemUiVisibility(flag);
                }
            } */
        }
    }

    private void changeBackButtonVisibility(boolean paramBoolean)
    {
        if (paramBoolean)
        {
            this.mBackButton.setVisibility(View.VISIBLE);

        }else{
            this.mBackButton.setVisibility(View.GONE);
        }
    }

    private BaseFragmentView getCurrentFragment()
    {
        String str = getCurrentFragmentTag();
        return getFragmentFromTag(this.mContext, str);
    }

    public static ViewContorller getInstance(Context paramContext)
    {
        if (sViewContorller == null) {
            sViewContorller = new ViewContorller(paramContext);
        }
        return sViewContorller;
    }

    private void showFragment(String paramString, boolean paramBoolean1, boolean paramBoolean2)
    {
    	 
        Log.d("FragmentContorller", "show fragment tag = " + paramString + " force = " + paramBoolean1 + " current tag = " + this.mCurrentFragmentTag);
        if ((TextUtils.isEmpty(paramString)) || ((paramString.equals(this.mCurrentFragmentTag)) && (!paramBoolean1)))
        {
            BaseFragmentView fragmentView  = getFragmentFromTag(this.mContext, paramString);
            if ((fragmentView != null) && (!fragmentView.attached())) {
                fragmentView.performAttach();
            }
        }else{
            BaseFragmentView localBaseFragmentView;
            if ((!TextUtils.isEmpty(this.mCurrentFragmentTag)) && (!this.mCurrentFragmentTag.equals(paramString))) {
                this.mPreviousTag = this.mCurrentFragmentTag;
            }

            this.mCurrentFragmentTag = paramString;
            localBaseFragmentView = getFragmentFromTag(this.mContext, paramString);
            View localView = localBaseFragmentView.getRootView(this.mLayoutInflater, this.mRootView.getContainer());
            this.mRootView.moveToFragmentView(localView, paramBoolean2, this);
            if ("incall".equals(paramString)) {
                mInCallContorller.setIncallFragment((InCallFragment)localBaseFragmentView);

            }
           
            
            BaseFragmentView fragmentView = getFragmentFromTag(this.mContext, this.mPreviousTag);
            if ((fragmentView != null) && (fragmentView.attached())) {
                fragmentView.performDetach();
            }
            if (!localBaseFragmentView.attached())
                localBaseFragmentView.performAttach();
        }
    }

    public String getCurrentFragmentTag()
    {
        return this.mCurrentFragmentTag;
    }

    public BaseFragmentView getFragmentFromTag(Context paramContext, String paramString)
    {
        if (TextUtils.isEmpty(paramString))
        {
            return null;
        }
        String str = paramString;
        if (ADDED_FRAGMENT.get(paramString) == null) {
            str = "clock";
        }

        BaseFragmentView localBaseFragmentView = ((FragmentItem)ADDED_FRAGMENT.get(str)).object;
        if (localBaseFragmentView == null) {
            if ("launch".equals(str)){
                localBaseFragmentView = new LaunchFragment(paramContext, this);
            }else if ("dialer".equals(str)){
                localBaseFragmentView = new DialerFragment(paramContext);
            }else if ("mms".equals(str)){
                localBaseFragmentView = new MmsFragment(paramContext);
            }else if ("music".equals(str)){
                localBaseFragmentView = new MusicFragment(paramContext);
            }else if ("clock".equals(str)){
                localBaseFragmentView = new ClockFragment(paramContext);
            }else if ("incall".equals(str)){
                localBaseFragmentView = new InCallFragment(paramContext);
            }else if ("weather".equals(str)){
                localBaseFragmentView = new WeatherFragment(paramContext);
            }else if ("choose_clock_style".equals(str)){
                localBaseFragmentView = new ChooseClockFragment(paramContext);
            }else if ("digit_clock".equals(str)){
                localBaseFragmentView = new NumberClockFragment(paramContext);
            }else if ("roma_clock".equals(str)){
                localBaseFragmentView = new SportClockFragment(paramContext);
            }else if ("settings".equals(str)){
                localBaseFragmentView = new SettingFragment(paramContext);
            }else if ("audioprofile".equals(str)){
            	localBaseFragmentView = new AudioProfileFragment(paramContext);
            }else if ("timeout".equals(str)){
            	localBaseFragmentView = new TimeoutFragment(paramContext);
            }

            ((FragmentItem)ADDED_FRAGMENT.get(str)).object = localBaseFragmentView;
        }

        return localBaseFragmentView;
    }


    public void handleTouchEvent(MotionEvent paramMotionEvent)
    {
        int i = paramMotionEvent.getAction();
        if (this.mCurrentFragmentTag.equals("incall")){
            return;
        }
        float f1;
        float f2;
        switch (i){
            case 0:
                this.mDownX = paramMotionEvent.getX();
                mDownY = paramMotionEvent.getY();
                break;
            case 1:
                f1 = paramMotionEvent.getX();
                f2 = this.mDownX - f1;
                Log.d("FragmentContorller", "Upx = " + f1 + " diffX = " + f2);
                if(Math.abs(f2) > this.mHallViewWidth / 3 && (mDownY < mHallViewWidth + 20)){
                    if ("launch".equals(this.mCurrentFragmentTag)){
                        if (this.mDownX - f1 > 0.0F){
                            moveToFirstPage(true);
                        }else{
                            moveToFirstPage(false);
                        }
                    }else{
                        if (this.mDownX - f1 > 0.0F){
                            showLaunchFragment(true);
                        }else{
                            showLaunchFragment(false);
                        }
                    }
                }
                break;
            default:
        }
    }

    public void reload(){
        ADDED_FRAGMENT.clear();
        ADDED_FRAGMENT.put("launch", new FragmentItem(-1));
        ADDED_FRAGMENT.put("incall", new FragmentItem(-1));
        ADDED_FRAGMENT.put("clock", new FragmentItem(-1));
        ADDED_FRAGMENT.put("digit_clock", new FragmentItem(-1));
        ADDED_FRAGMENT.put("roma_clock", new FragmentItem(-1));
        ADDED_FRAGMENT.put("audioprofile", new FragmentItem(-1));
        ADDED_FRAGMENT.put("timeout", new FragmentItem(-1));
        ADDED_FRAGMENT.put("dialer", new FragmentItem(R.drawable.launch_dialer));
        ADDED_FRAGMENT.put("mms", new FragmentItem(R.drawable.launch_mms));
        ADDED_FRAGMENT.put("music", new FragmentItem(R.drawable.launch_music));
        ADDED_FRAGMENT.put("choose_clock_style", new FragmentItem(R.drawable.launch_clock));
        ADDED_FRAGMENT.put("settings", new FragmentItem(R.drawable.launch_settings));      
    }
    
    public void hideRootView()
    {
        if (this.mRootViewShowing)
        {
            Object localObject = (WindowManager)this.mContext.getSystemService(Context.WINDOW_SERVICE);
            if (this.mRootView.isAttachedToWindow()) {
                ((WindowManager)localObject).removeView(this.mRootView);
                if(SystemProperties.get("qemu.hw.mainkeys").equals("0")){//Kalyy
                    ((WindowManager)localObject).removeView(this.mButtomView);
                }
            }
            this.mRootViewShowing = false;
            localObject = (FragmentItem)ADDED_FRAGMENT.get(getCurrentFragmentTag());
            if ((localObject != null) && (((FragmentItem)localObject).object != null))
            {
                ((FragmentItem)localObject).object.onHide();
                ((FragmentItem)localObject).object.performDetach();
            }
        }
    }

    public void launchRootView()
    {
        addRootViewToWindow();
        if ((this.mInCallContorller != null) && this.mInCallContorller.shouldShowIncall()) {
            moveToFragment("incall");
        }else {
            moveToFirstPage(false);
        }
        FragmentItem localFragmentItem = (FragmentItem)ADDED_FRAGMENT.get(getCurrentFragmentTag());
        if ((localFragmentItem != null) && (localFragmentItem.object != null)) {
            localFragmentItem.object.onShow();
        }
        return;
    }


    public void moveToFirstPage(boolean paramBoolean)
    {
        this.mFistPage = DataSave.getClockStyle(this.mContext);
        moveToFragment(this.mFistPage, paramBoolean);
    }

    public void moveToFragment(String paramString)
    {
        moveToFragment(paramString, false);
    }

    public void moveToFragment(String paramString, boolean paramBoolean)
    {
        showFragment(paramString, false, paramBoolean);
    }

    public void moveToPreviousFragment()
    {
        Log.d("FragmentContorller", "moveToPreviousFragment mPreviousTag=" + this.mPreviousTag);
        if (this.mPreviousTag != null) {
            moveToFragment(this.mPreviousTag);
        }else{
            moveToFirstPage(false);
        }
    }

    public void onClick(View paramView)
    {
        if (paramView.getId() == R.id.exit) {
            hideRootView();
            return;
        }

        BaseFragmentView view = ((FragmentItem)ADDED_FRAGMENT.get(getCurrentFragmentTag())).object;
        if(getCurrentFragmentTag().equals("audioprofile") || getCurrentFragmentTag().equals("timeout")){
        	moveToFragment("settings");
        }else if(!(view instanceof BaseFragmentView) || !view.handleBackPress()){
            showLaunchFragment(false);
        }
    }

    public void onItemClick(String paramString)
    {
        if (("dialer".equals(paramString)) && (this.mInCallContorller != null) && (this.mInCallContorller.shouldShowIncall()))
        {
            moveToFragment("incall");
            return;
        }
        moveToFragment(paramString);
    }

    public void onScreenOff()
    {
        BaseFragmentView localBaseFragmentView = getCurrentFragment();
        if (localBaseFragmentView != null) {
            localBaseFragmentView.onScreenOff();
        }
    }

    public void onScreenOn()
    {
        BaseFragmentView localBaseFragmentView = getCurrentFragment();
        if (localBaseFragmentView != null) {
            localBaseFragmentView.onScreenOn();
        }
    }

    public void showInCallView(boolean paramBoolean)
    {
        if (this.mInCallContorller.shouldShowIncall())
        {
            addRootViewToWindow();
            moveToFragment("incall");
            ((InCallFragment)((FragmentItem)ADDED_FRAGMENT.get("incall")).object).setIsOutgoing(paramBoolean);
        }
    }

    public void showLaunchFragment(boolean paramBoolean)
    {
        moveToFragment("launch", paramBoolean);
    }

    public void showOrGoneButton()
    {
        System.out.println("xuehui");
        BaseFragmentView localBaseFragmentView = getFragmentFromTag(this.mContext, getCurrentFragmentTag());
        if (((localBaseFragmentView instanceof BaseFragmentView)) && (localBaseFragmentView.needShowBackButton()))
        {
            System.out.println("xuehui:true");
            changeBackButtonVisibility(true);
            return;
        }
        changeBackButtonVisibility(false);
        System.out.println("xuehui:false");
    }

    public static class FragmentItem
    {
        public int iconRes;
        public BaseFragmentView object;

        public FragmentItem(int paramInt)
        {
            this.iconRes = paramInt;
        }
    }
}

