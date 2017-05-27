package com.wx.hallview.fragment;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.wx.hallview.bean.WeatherInfo;
import com.wx.hallview.services.WeatherService;
import com.wx.hallview.services.WeatherService.MyBinder;
import com.wx.hallview.services.WeatherService.WeatherLoadListener;
import com.wx.hallview.views.utils.DataSave;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.wx.hallview.R;

public class WeatherFragment extends BaseFragmentView
  implements View.OnClickListener, WeatherService.WeatherLoadListener
{
  private ObjectAnimator mAnimator;
  private TextView mComments;
  private TextView mCountyName;
  private TextView mDate;
  private boolean mIsNeedUpdate = false;
  private boolean mIsOnAttach = true;
  private ImageView mLocate;
  private View mLocationSetContainer;
  private ImageView mRefresh;
  private ProgressBar mRefreshProgressBar;
  private WeatherService mService;
  private ServiceConnection mServiceConnection;
  private TextView mTemp;
  private View mTempInfoContainer;
  private Button mTryAgain;
  private ImageView mWeatherBg;
  private ImageView mWeatherIcon;

  public WeatherFragment(Context paramContext)
  {
    super(paramContext);
		mIsNeedUpdate = false;
		mIsOnAttach = true;
  }

  private void bindService()
  {
    Intent localIntent = new Intent();
    localIntent.setClassName("com.wx.hallview", "com.wx.hallview.services.WeatherService");
    getContext().bindService(localIntent, mServiceConnection, 1);
  }

  private void changeUI(boolean flag)
  {
		final View closeView;
		final View openView;
		if (flag){
			openView = mTempInfoContainer;
			closeView = mLocationSetContainer;
		} else{
			closeView = mTempInfoContainer;
			openView = mLocationSetContainer;
		}
		
		if (openView.getVisibility() == View.VISIBLE || closeView.getVisibility() == View.GONE)
		{
			Log.d("WeatherFragment", "weather:needn't changeUI");
		} else{
			openView.setVisibility(View.VISIBLE);
			closeView.setVisibility(View.VISIBLE);
			AnimatorSet animatorSet = createAnimator(openView, closeView);
            animatorSet.addListener(new Animator.AnimatorListener() {
	          
				public void onAnimationStart(Animator animator) {
				}
				
				public void onAnimationRepeat(Animator animator) {
				}
				
				public void onAnimationEnd(Animator animator) {
				  openView.setVisibility(View.VISIBLE);
				  closeView.setVisibility(View.GONE);
				}
				
				public void onAnimationCancel(Animator animator) {
				}
			});
            animatorSet.start();
		}
  }

  private AnimatorSet createAnimator(View view, View view1)
  {
		ObjectAnimator objectanimator = ObjectAnimator.ofFloat(view, "Alpha", new float[] {0.0F, 0.01F, 1.0F});
		ObjectAnimator objectanimator1 = ObjectAnimator.ofFloat(view1, "Alpha", new float[] {1.0F, 0.01F, 0.0F});
		AnimatorSet animatorset = new AnimatorSet();
		animatorset.setDuration(500L);
		animatorset.playTogether(new Animator[] {
			objectanimator1, 
			objectanimator, 
			ObjectAnimator.ofFloat(view1, "rotationY", new float[] {0.0F, 180F}), 
			ObjectAnimator.ofFloat(view, "rotationY", new float[] {-180F, 0.0F})
		});
		return animatorset;
  }

  private void displayWeatherInfo(WeatherInfo paramWeatherInfo)
  {
    try
    {
      Log.d("WeatherFragment", "weather:displayWeatherInfo()");
      Log.d("WeatherFragment", "weather:" + paramWeatherInfo.toString());
      String str1 = paramWeatherInfo.getCountyName();
      if (TextUtils.isEmpty(str1))
      {
        mLocate.setVisibility(View.GONE);
        mRefresh.setVisibility(View.GONE);
        mRefreshProgressBar.setVisibility(View.VISIBLE);
        mIsNeedUpdate = true;
      }
      else
      {
        mRefresh.setVisibility(View.VISIBLE);
        mLocate.setVisibility(View.VISIBLE);
        mRefreshProgressBar.setVisibility(View.GONE);
        mTemp.setTextSize(50.0F);
      }
      mCountyName.setText(str1);
      mTemp.setText(paramWeatherInfo.getTemp());
      String str2 = paramWeatherInfo.getDate();
      if (!TextUtils.isEmpty(str2))
      {
        Date localDate = new SimpleDateFormat("E, dd MMM yyyy hh:mm a", Locale.US).parse(str2);
        str2 = new SimpleDateFormat("yyyy.MM.dd  E", Locale.getDefault()).format(localDate);
      }
      mDate.setText(str2);
      setWeatherTypeImg(paramWeatherInfo.getText());
    }
    catch (ParseException localParseException)
    {
      localParseException.printStackTrace();
    }
  }

  private void initView(View paramView)
  {
    mTemp = ((TextView)paramView.findViewById(R.id.tv_temp));
    mDate = ((TextView)paramView.findViewById(R.id.tv_date));
    mCountyName = ((TextView)paramView.findViewById(R.id.tv_countyName));
    mWeatherBg = ((ImageView)paramView.findViewById(R.id.iv_weatherBg));
    mWeatherIcon = ((ImageView)paramView.findViewById(R.id.iv_weatherIcon));
    mLocate = ((ImageView)paramView.findViewById(R.id.iv_locate));
    mRefresh = ((ImageView)paramView.findViewById(R.id.iv_refresh));
    mTryAgain = ((Button)paramView.findViewById(R.id.btn_try_again));
    mTempInfoContainer = paramView.findViewById(R.id.container_temp_info);
    mLocationSetContainer = paramView.findViewById(R.id.location_setting_container);
    mComments = ((TextView)paramView.findViewById(R.id.tv_comments));
    mRefreshProgressBar = ((ProgressBar)paramView.findViewById(R.id.pg_refresh));
  }

  private void setWeatherTypeImg(String text)
  {
      if(text.contains("cloud")) {
          mWeatherBg.setImageResource(text.contains("night") ? R.drawable.cloudy_night_bg : R.drawable.cloudy_bg);
          mWeatherIcon.setImageResource(text.contains("night") ? R.drawable.cloudy_night_icon : R.drawable.cloudy_icon);
      }
      else if((text.contains("sun")) || (text.contains("clear"))) {
          mWeatherBg.setImageResource(text.contains("night") ? R.drawable.sunny_night_bg : R.drawable.sunny_bg);
          mWeatherIcon.setImageResource(text.contains("night") ? R.drawable.sunny_night_icon : R.drawable.sunny_icon);
      }
      else if((text.contains("snow")) || (text.contains("flurries"))) {
          mWeatherBg.setImageResource(R.drawable.snow_bg);
          mWeatherIcon.setImageResource(R.drawable.snow_icon);
      }
      else if((text.contains("rain")) || (text.contains("shower"))) {
          mWeatherBg.setImageResource(R.drawable.rain_bg);
          mWeatherIcon.setImageResource(R.drawable.rain_icon);
      }
      else {
		      mWeatherBg.setImageResource(R.drawable.unknown_bg);
		      mWeatherIcon.setImageResource(R.drawable.unknown_icon);
      }
  }

  public void onAttach()
  {
    displayWeatherInfo(DataSave.getWeatherInfo(getContext()));
    bindService();
  }

  public void onClick(View view)
  {
      switch(view.getId()) {
          case R.id.iv_refresh:
          {
              Log.d("WeatherFragment", "weather:mRefresh");
              mIsNeedUpdate = true;
              bindService();
              mIsOnAttach = false;
              break;
          }
          case R.id.btn_try_again:
          {
              Log.d("WeatherFragment", "weather:btn_try_again");
              changeUI(true);
              mIsNeedUpdate = true;
              bindService();
              mIsOnAttach = false;
              break;
          }
      }
  }

  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup)
  {
    View localView = View.inflate(getContext(), R.layout.weather_view, null);
    initView(localView);
    mRefresh.setOnClickListener(this);
    mTryAgain.setOnClickListener(this);
    mServiceConnection = new ServiceConnection()
    {
      public void onServiceConnected(ComponentName paramComponentName, IBinder paramIBinder)
      {
        Log.d("WeatherFragment", "weather:service connected");
				mService = ((WeatherService.MyBinder)paramIBinder).getService();
        mRefresh.setClickable(false);
        mAnimator.start();
        if (mIsNeedUpdate)
        {
          mService.getWeatherInfo(WeatherFragment.this, mIsNeedUpdate);
          mIsNeedUpdate = false;
        }
        else
        {
          mService.getWeatherInfo(WeatherFragment.this);
        }
      }

      public void onServiceDisconnected(ComponentName paramComponentName)
      {
      	mService = null;
      }
    };
    ImageView localImageView = this.mRefresh;
    float[] arrayOfFloat = new float[2];
    arrayOfFloat[0] = 0.0F;
    arrayOfFloat[1] = 360.0F;
    this.mAnimator = ObjectAnimator.ofFloat(localImageView, "rotation", arrayOfFloat);
    this.mAnimator.setRepeatCount(-1);
    this.mAnimator.setRepeatMode(1);
    this.mAnimator.setInterpolator(new LinearInterpolator());
    this.mAnimator.setDuration(500L);
    return localView;
  }

  public void onDetach()
  {
    Log.d("WeatherFragment", "weather:onDetach");
    changeUI(true);
  }

  public void onWeatherInfoChanged(int result, WeatherInfo info)
  {
      Log.d("WeatherFragment", "weather:onWeatherInfoChanged result = " + result + " info = " + info);
      mRefresh.setClickable(true);
      mAnimator.end();
      getContext().unbindService(mServiceConnection);
      if(result == 0) {
          if(mLocate.getVisibility() == View.GONE) {
              mLocate.setVisibility(View.VISIBLE);
          }
          changeUI(true);
          displayWeatherInfo(info);
          return;
      }
      if(result == 1) {
          mComments.setText(R.string.network_is_not_good);
          mTryAgain.setVisibility(View.VISIBLE);
      } else if((result == 3) || (result == 2)) {
          mComments.setText(R.string.network_not_open);
          mTryAgain.setVisibility(View.GONE);
      } else if(result == 4) {
          mComments.setText(R.string.network_is_not_available);
          mTryAgain.setVisibility(View.GONE);
      } else if(result == 5) {
          mComments.setText(R.string.open_network_gps_comments);
          mTryAgain.setVisibility(View.GONE);
      }
      WeatherInfo weatherInfo = DataSave.getWeatherInfo(getContext());
      if((!mIsOnAttach) || (TextUtils.isEmpty(weatherInfo.getCountyName()))) {
          mIsOnAttach = true;
          changeUI(false);
      }
  }
}