package com.sprd.engineermode.telephony.netinfostatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sprd.engineermode.connectivity.ConnectivityFragment;
import com.sprd.engineermode.debuglog.DebugLogFragment;
import com.sprd.engineermode.hardware.HardWareFragment;
import com.sprd.engineermode.telephony.TelephonyFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ActionBar.Tab;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.sprd.engineermode.debuglog.slogui.SlogAction;
import com.sprd.engineermode.R;
import com.sprd.engineermode.TabFragmentPagerAdapter;

public class NetinfoStatisticsActivity extends FragmentActivity {

    private static final String TAG = "NetinfoStatisticsActivity";
    private ArrayList<Fragment> mFragmentsList;
    private List<String> mTitleList = new ArrayList<String>();
    private ViewPager mViewPager;
    private Context mContext;

    public interface TabState {
        public static int TAB_RESELECT_INDEX = 0;  //Reselect
        public static int TAB_HANDOVER_INDEX = 1; //handOver
        public static int TAB_ATTACH_INDEX = 2; //Attachtime
        public static int TAB_DROP_INDEX = 3; //droptimes
    }

    private int[] mTabTitle = new int[] { R.string.tab_reselect,
            R.string.tab_handover, R.string.tab_attachTime,
            R.string.tab_dropTimes };

    //private int mCurrentTab = TabState.TAB_RESELECT_INDEX;
    public static String CURRENT_TAB = "android.app.engmode.currenttab";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SlogAction.reloadCacheIfInvalid(null);
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_netinfostatistics);  //can reuse ?
        createFragmentAndTab();
        PagerTabStrip netinfotabs = (PagerTabStrip) findViewById(R.id.netinfotabs);
        netinfotabs.setTabIndicatorColorResource(android.R.color.holo_blue_light);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = this.getActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.version_info, menu);
        MenuItem item =menu.findItem(R.id.action_version_info);
        if (item != null) {
            item.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_version_info:
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.version_info))
                .setMessage(getString(R.string.version_info_detail))
                .setPositiveButton(R.string.alertdialog_ok,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }) .create();
                alertDialog.show();
                return true;
            default:
                Log.i(TAG, "default");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = this.getIntent();
        int simindex = intent.getIntExtra("simindex", -1);
        setTitle("SIM" + simindex);
        Log.d(TAG, "onResume simindex=" + simindex);
    }

    private void createFragmentAndTab() {
        mFragmentsList = new ArrayList<Fragment>();
        mViewPager = (ViewPager) findViewById(R.id.netinfopager);
        FragmentManager fragmentManager = getFragmentManager();
        Fragment reselectFragment = new ReselectFragment();
        Fragment handOverFragment = new HandOverFragment();
        Fragment attachTimeFragment = new AttachTimeFragment();
        Fragment dropTimesFragment = new DropTimesFragment();

        mFragmentsList.add(reselectFragment);
        mFragmentsList.add(handOverFragment);
        mFragmentsList.add(attachTimeFragment);
        mFragmentsList.add(dropTimesFragment);
        mViewPager.setAdapter(new TabFragmentPagerAdapter(fragmentManager,
                mFragmentsList, mTabTitle, mContext));
    }
}

