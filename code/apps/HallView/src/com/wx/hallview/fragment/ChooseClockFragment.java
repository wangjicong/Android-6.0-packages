package com.wx.hallview.fragment;

/**
 * Created by Administrator on 16-1-23.
 */
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.content.Context;
import com.wx.hallview.views.utils.DataSave;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.wx.hallview.ViewContorller;
import com.wx.hallview.R;

public class ChooseClockFragment extends BaseFragmentView implements View.OnClickListener {
    private static final String[] CLOCK_ITEMS = { "clock", "digit_clock", "roma_clock" };;
    
    //qiuyaobo,remove weather,20160906,begin
    //private static final int[] CLOCK_ITEMS_PREVIEW = {R.drawable.clock1, R.drawable.clock2, R.drawable.clock_style4};
    private static final int[] CLOCK_ITEMS_PREVIEW = {R.drawable.clock1_no_weather, R.drawable.clock2_no_weather, R.drawable.clock_style4_no_weather};
    //qiuyaobo,remove weather,20160906,end
    
    private TextView mClockTitle;
    private Button mLeftButton;
    private Button mRightButton;
    private ViewPager mViewPager;
	private Context mContext;
    
    public ChooseClockFragment(Context context) {
        super(context);
		mContext = context;
    }
    
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        View rootView = inflater.inflate(R.layout.choose_clock_view, container, false);
        mViewPager = (ViewPager)rootView.findViewById(R.id.clock_container);
        mLeftButton = (Button)rootView.findViewById(R.id.btn_left);
        mRightButton = (Button)rootView.findViewById(R.id.btn_right);
        mClockTitle = (TextView)rootView.findViewById(R.id.tv_clock_title);
		mClockTitle.setText(mContext.getString(R.string.clock_style, 1));
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            
            public void onPageSelected(int arg0) {
                mClockTitle.setText(mContext.getString(R.string.clock_style, arg0 + 1));
            }
            
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }
            
            public void onPageScrollStateChanged(int arg0) {
            }
        });
        mViewPager.setCurrentItem(getCurrentTagIndex());
        updateLeftRightBtnVisibility();
        mLeftButton.setOnClickListener(this);
        mRightButton.setOnClickListener(this);
        return rootView;
    }
    
    private int getCurrentTagIndex() {
        String tag = DataSave.getClockStyle(mContext);
        for(int i = 0; i < CLOCK_ITEMS.length; i = i + 1) {
            if(CLOCK_ITEMS[i].equals(tag)) {
                return i;
            }
        }
        return 0;
    }
    private PagerAdapter mPagerAdapter = new PagerAdapter() {
        
        public Object instantiateItem(View container, int position) {
            int imageRes = ChooseClockFragment.CLOCK_ITEMS_PREVIEW[position];
            String tag =  ChooseClockFragment.CLOCK_ITEMS[position];
            ImageView imageView = new ImageView(getContext());
            imageView.setTag(tag);
            imageView.setClickable(true);
            imageView.setImageResource(imageRes);
            imageView.setOnClickListener(ChooseClockFragment.this);
            ((ViewPager)container).addView(imageView);
            return imageView;
        }
        
        public int getCount() {
            return ChooseClockFragment.CLOCK_ITEMS.length;
        }
        
        public boolean isViewFromObject(View view, Object obj) {
            return (view == obj);
        }
        
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }
    };
    
    protected void onAttach() {
        super.onAttach();
    }
    
    protected void onDetach() {
        int currentItem = mViewPager.getCurrentItem();
        String tag = CLOCK_ITEMS[currentItem];
        DataSave.saveClockStyle(getContext(), tag);
    }
    
    private void updateLeftRightBtnVisibility() {
        int currentItem = mViewPager.getCurrentItem();
        if(currentItem == 0) {
            mLeftButton.setVisibility(View.GONE);
            mRightButton.setVisibility(View.VISIBLE);
            return;
        }
        if(currentItem == (CLOCK_ITEMS.length - 1)) {
            mLeftButton.setVisibility(View.VISIBLE);
            mRightButton.setVisibility(View.GONE);
            return;
        }
        mLeftButton.setVisibility(View.VISIBLE);
        mRightButton.setVisibility(View.VISIBLE);
    }
    
    public void onClick(View view) {
        
	    if ((view instanceof ImageView))
	    {
	      Object localObject = view.getTag();
	      if ((localObject instanceof String))
	      {
	        ViewContorller.getInstance(getContext()).moveToFragment((String)localObject);
	        return;
	      }
	    }
	    int i = mViewPager.getCurrentItem();
	    if (view.equals(mLeftButton)) {
	        mViewPager.setCurrentItem(i - 1, true);
	      
	    }else if(view.equals(mRightButton)) {
	        mViewPager.setCurrentItem(i + 1, true);
	    }
	    updateLeftRightBtnVisibility();
    }
}
