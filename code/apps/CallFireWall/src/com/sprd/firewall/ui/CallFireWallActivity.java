
package com.sprd.firewall.ui;

import com.sprd.firewall.R;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.SystemProperties;
import com.sprd.firewall.util.UniverseThemeUtils;

public class CallFireWallActivity extends Activity {
    private static final String TAG = "CallFireWallActivity";

    private static final int TAB_INDEX_BLACK = 0;
    private static final int TAB_INDEX_CALL_LOG = 1;
    private static final int TAB_INDEX_SMS_LOG = 2;

    private static final int TAB_INDEX_COUNT = 3;

    // SPRD: Add for bug 536682
    private boolean mIsSaveInstanceState = true;

    public interface ViewPagerVisibilityListener {
        public void onVisibilityChanged(boolean visible);
    }

    // SPRD: modify for bug520785
    public class ViewPagerAdapter extends FragmentStatePagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_INDEX_BLACK:
                    return new BlackFragment();
                case TAB_INDEX_CALL_LOG:
                    return new CallLogFragment();
                case TAB_INDEX_SMS_LOG:
                    return new SmsLogFragment();
            }
            throw new IllegalStateException("No fragment at position "
                    + position);
        }

        @Override
        public int getCount() {
            return TAB_INDEX_COUNT;
        }
    }

    private class PageChangeListener implements OnPageChangeListener {
        private int mCurrentPosition = -1;
        private int mNextPosition = 0;

        @Override
        public void onPageScrolled(int positon, float positionOffset,
                int positionoffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            final ActionBar actionBar = getActionBar();
            actionBar.selectTab(actionBar.getTabAt(position));
            /* SPRD: modify for bug 523476 @{ */
            if (mBlackFragment != null && mBlackFragment.getDeleteDone()) {
                resetData(position);
                mBlackFragment.setDeleteDone(false);
            }
            /* @} */
            resetData(mNextPosition);
            mNextPosition = position;
        }

        public void setCurrentPosition(int position) {
            mCurrentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int status) {
            switch (status) {
                case ViewPager.SCROLL_STATE_IDLE:
                    if (mCurrentPosition >= 0) {
                        sendFragmentVisibilityChange(mCurrentPosition, false);
                    }
                    if (mNextPosition >= 0) {
                        sendFragmentVisibilityChange(mNextPosition, true);
                    }
                    invalidateOptionsMenu();

                    mCurrentPosition = mNextPosition;
                    break;
                case ViewPager.SCROLL_STATE_DRAGGING:
                case ViewPager.SCROLL_STATE_SETTLING:
                default:
                    break;
            }

        }
    }

    private ViewPager mViewPager;
    private PageChangeListener mPageChangeListener = new PageChangeListener();
    private BlackFragment mBlackFragment;
    private CallLogFragment mCallLogFragment;
    private SmsLogFragment mSmsFragment;

    private TabListener mTabListener = new TabListener() {

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }
    };

    private final View.OnLayoutChangeListener mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            v.removeOnLayoutChangeListener(this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* SPRD: modify for bug501393 @{*/
        UserManager userManager = (UserManager)
                getBaseContext().getSystemService(Context.USER_SERVICE);
        if (!userManager.isSystemUser()) {
            Toast.makeText(this, R.string.refuse_in_guest,
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        /* @} */
        setContentView(R.layout.callfirewall_activity);

        findViewById(R.id.callfirewall_frame).addOnLayoutChangeListener(mOnLayoutChangeListener);

        Log.d(TAG, "RequestType3=" + findViewById(R.id.pager));
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new ViewPagerAdapter(getFragmentManager()));
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        setupBlack();
        setupCallLog();
        setupSmsLog();

        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {

        final int currentPosition = mViewPager != null ? mViewPager.getCurrentItem() : -1;

        if (fragment instanceof BlackFragment) {
            mBlackFragment = (BlackFragment) fragment;
            if (currentPosition == TAB_INDEX_BLACK) {
                mBlackFragment.onVisibilityChanged(true);
            }
        } else if (fragment instanceof CallLogFragment) {
            mCallLogFragment = (CallLogFragment) fragment;
            if (currentPosition == TAB_INDEX_CALL_LOG) {
                mCallLogFragment.onVisibilityChanged(true);
            }
        } else if (fragment instanceof SmsLogFragment) {
            mSmsFragment = (SmsLogFragment) fragment;
            if (currentPosition == TAB_INDEX_SMS_LOG) {
                mSmsFragment.onVisibilityChanged(true);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        // SPRD: Add for bug 536682
        mIsSaveInstanceState = false;
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        final int currentPosition = mViewPager != null ? mViewPager.getCurrentItem() : -1;

        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (currentPosition == TAB_INDEX_BLACK && mBlackFragment.getDeleterState()) {
                // SPRD: modify for bug 558428
                mBlackFragment.resetTabSwitchState(true);
                return true;
            } else if (currentPosition == TAB_INDEX_CALL_LOG && mCallLogFragment.getDeleteState()) {
                // SPRD: modify for bug 558428
                mCallLogFragment.resetTabSwitchState(true);
                return true;
            } else if (currentPosition == TAB_INDEX_SMS_LOG && mSmsFragment.getDeleteState()) {
                // SPRD: modify for bug 558428
                mSmsFragment.resetTabSwitchState(true);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /* SPRD: Add for bug 513409 @{ */
    @Override
    public void onBackPressed() {
        /* SPRD: Add for bug 536682 @{ */
        if (isResumed() && !mIsSaveInstanceState) {
            super.onBackPressed();
        }
        /* @} */
    }
    /* @} */

    /* SPRD: Add for bug 536682 @{ */
    @Override
    public void onSaveInstanceState(Bundle outstate) {
        mIsSaveInstanceState = true;
        super.onSaveInstanceState(outstate);
    }
    /* @} */


    private void setupBlack() {
        final Tab tab = getActionBar().newTab();
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.buttonbar, null);
        TextView blackText = (TextView) view.findViewById(R.id.tab_text);
        if (blackText != null) {
            blackText.setText(R.string.blackCallsIconLabel);
        }
        tab.setCustomView(view);
        tab.setText(R.string.blackCallsIconLabel);
        tab.setTabListener(mTabListener);
        getActionBar().addTab(tab);
    }

    private void setupCallLog() {
        final Tab tab = getActionBar().newTab();
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.buttonbar, null);
        TextView blackText = (TextView) view.findViewById(R.id.tab_text);
        if (blackText != null) {
            blackText.setText(R.string.blackCallsLogIconLabel);
        }
        tab.setCustomView(view);
        tab.setText(R.string.blackCallsLogIconLabel);
        tab.setTabListener(mTabListener);
        getActionBar().addTab(tab);
    }

    private void setupSmsLog() {
        final Tab tab = getActionBar().newTab();
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.buttonbar, null);
        TextView blackText = (TextView) view.findViewById(R.id.tab_text);
        if (blackText != null) {
            blackText.setText(R.string.SmsCallsLogIconLabel);
        }
        tab.setCustomView(view);
        tab.setText(R.string.SmsCallsLogIconLabel);

        tab.setTabListener(mTabListener);
        getActionBar().addTab(tab);
    }

    private Fragment getFragmentAt(int position) {
        switch (position) {
            case TAB_INDEX_BLACK:
                return mBlackFragment;
            case TAB_INDEX_CALL_LOG:
                return mCallLogFragment;
            case TAB_INDEX_SMS_LOG:
                return mSmsFragment;
            default:
                throw new IllegalStateException("Unknown fragment index: " + position);
        }
    }

    private void sendFragmentVisibilityChange(int position, boolean visibility) {
        final Fragment fragment = getFragmentAt(position);
        if (fragment instanceof ViewPagerVisibilityListener) {
            ((ViewPagerVisibilityListener) fragment).onVisibilityChanged(visibility);
        }
    }

    private void resetData(int position) {
        switch (position) {
            case TAB_INDEX_BLACK:
                if (mBlackFragment != null) {
                    // SPRD: modify for bug 558428
                    mBlackFragment.resetTabSwitchState(false);
                }
                break;
            case TAB_INDEX_CALL_LOG:
                if (mCallLogFragment != null) {
                    // SPRD: modify for bug 558428
                    mCallLogFragment.resetTabSwitchState(false);
                }
                break;
            case TAB_INDEX_SMS_LOG:
                if (mSmsFragment != null) {
                    // SPRD: modify for bug 558428
                    mSmsFragment.resetTabSwitchState(false);
                }
                break;
            default:
                break;
        }
    }
}
