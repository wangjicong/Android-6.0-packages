/** Create by Spreadst */
package com.sprd.settings.timerpower;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;




/**
 * Power ON/OFF application.
 */
public class AlarmClock extends Activity implements OnItemClickListener,Indexable {

    static final String PREFERENCES = "AlarmClock";

    /** This must be false for production.  If true, turns on logging,
        test code, etc. */
    static final boolean DEBUG = true;

    private LayoutInflater mFactory;
    private ListView mAlarmsList;
    private Cursor mCursor;

    private static final String ALARM_TITLE_ON = "on";
    private static final String EMPTY_STRING = "";
    private static final int POWER_ON = 0;
    private void updateIndicatorAndAlarm(boolean enabled, ImageView bar,
            Alarm alarm) {
        Log.v("timerpower AlarmClock ========== >>>>> updateIndicatorAndAlarm "+enabled);
        Alarms.enableAlarm(this, alarm.id, enabled);
        if (enabled) {
            SetAlarm.popAlarmSetToast(this, alarm.hour, alarm.minutes,
                    alarm.daysOfWeek);
        }
    }

    private class AlarmTimeAdapter extends CursorAdapter {
        public AlarmTimeAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View ret = mFactory.inflate(R.layout.alarm_time, parent, false);
            return ret;
        }

        public void bindView(View view, Context context, Cursor cursor) {
            final Alarm alarm = new Alarm(AlarmClock.this,cursor);

            View indicator = view.findViewById(R.id.indicator);

            // Set the initial state of the clock "checkbox"
            final CheckBox clockOnOff =
                    (CheckBox) indicator.findViewById(R.id.clock_onoff);
            clockOnOff.setChecked(alarm.enabled);

            // Clicking outside the "checkbox" should also change the state.
            indicator.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        /* SPRD 520291 */
                        if(alarm != null && !EMPTY_STRING.equals(alarm.label) && ALARM_TITLE_ON.equals(alarm.label)
                               && UserHandle.myUserId() != UserHandle.USER_OWNER){
                            Toast.makeText(mContext,R.string.timer_on_invalid_in_guest,Toast.LENGTH_SHORT).show();
                            return;
                        }
                        /* @} */
                        clockOnOff.toggle();
                        updateIndicatorAndAlarm(clockOnOff.isChecked(),
                                null, alarm);
                    }
            });
            Log.v("timerpower AlarmClock -------------------- >>>>>>>>>>>>>>> "+alarm.label);
            final TextView powerOnOff = (TextView)view.findViewById(R.id.poweronoff);
            if(!alarm.label.equals("") && alarm.label.equals("on"))
            {
                powerOnOff.setText(R.string.power_on);
            }else
            {
                powerOnOff.setText(R.string.power_off);
            }

        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
/*Modify for fix bug 189270 start*/
        ActionBar actionBar =  getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
/*Modify for fix bug 189270 end*/
        mFactory = LayoutInflater.from(this);
        mCursor = Alarms.getAlarmsCursor(getContentResolver());
        Log.v("timerpower AlarmClock ============= mCursor");

        updateLayout();
    }

/*Modify for fix bug 189270 start*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }
/*Modify for fix bug 189270 end*/

    private void updateLayout() {
        setContentView(R.layout.alarm_clock);
        mAlarmsList = (ListView) findViewById(R.id.alarms_list);
        AlarmTimeAdapter adapter = new AlarmTimeAdapter(this, mCursor);
        mAlarmsList.setAdapter(adapter);
        mAlarmsList.setOnItemClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ToastMaster.cancelToast();
        mCursor.close();
    }

    @Override
    public void onItemClick(AdapterView parent, View v, int pos, long id) {
        android.util.Log.d("timerpower AlarmClock", "id = " + id);
        /* SPRD 525041 check the user when tap the item because only owner can set alarm */
        if( pos == POWER_ON && UserHandle.myUserId() != UserHandle.USER_OWNER ){
            Toast.makeText(this,R.string.timer_on_invalid_in_guest,Toast.LENGTH_SHORT).show();
            return;
        }
        /* }@ */
        Intent intent = new Intent(this, SetAlarm.class);
        intent.putExtra(Alarms.ALARM_ID, (int) id);
        startActivity(intent);
    }

    /* SPRD 450052 ,add the search for Timer Switch Machine@{ */
    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new SprdAlarmClockSearchIndexProvider();

    private static class SprdAlarmClockSearchIndexProvider extends BaseSearchIndexProvider {

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            final Resources res = context.getResources();
            final String screenTitle = res.getString(R.string.swtichmachine);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            data.intentAction = "android.intent.action.MAIN";
            data.intentTargetPackage = "com.android.settings";
            data.intentTargetClass = "com.sprd.settings.timerpower.AlarmClock";
            result.add(data);
            return result;
        }
    }
    /* @} */
}
