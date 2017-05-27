package com.sprd.deskclock;

import java.util.Calendar;
import java.util.HashSet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.AdapterView.OnItemClickListener;
import com.android.deskclock.Utils;
import com.android.deskclock.R;
import com.android.deskclock.AlarmClockFragment.AlarmItemAdapter.ItemHolder;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.DaysOfWeek;
import android.util.Log;

public class AlarmRepeatDialog extends DialogFragment {
    private ListView mMultiChoiceListView;
    private RadioGroup mGroup;
    private RadioButton mEveryDay;
    private RadioButton mWorkDay;
    private RadioButton mNever;
    private Context mContext;
    private Alarm mAlarm;
    private static final String TAG = "AlarmRepeatDialog";
    private static final String KEY_REPEATE_ALARM = "repeate_alarm";
    private int[] mDayOrder;
    private int mStartDay;

    public AlarmRepeatDialog() {
    }

    public static AlarmRepeatDialog newInstance(Alarm alarm) {
        AlarmRepeatDialog alarmRepeatDialog = new AlarmRepeatDialog();
        /* SPRD: Bug 530669 java.lang.NullPointerException, read alarm on a null object reference @{ */
        Bundle args = new Bundle();
        Log.d(TAG,"alarm = " + alarm);
        args.putParcelable(KEY_REPEATE_ALARM, alarm);
        alarmRepeatDialog.setArguments(args);
        /* @} */
        return alarmRepeatDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAlarm.daysOfWeek.setBitSet(mAlarm.daysOfWeek
                .getBitSetForTemp());
        mContext.getSharedPreferences("repeat", Context.MODE_PRIVATE)
                .edit()
                .putInt(mAlarm.id + "",
                        mAlarm.daysOfWeek.getBitSet()).commit();
    }

    private final int[] DAY_ORDER = new int[] {
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
    };

    final private void updateRadioGroup(Alarm alarm) {
        switch (mAlarm.daysOfWeek.getBitSetForTemp()) {
            case 127:
                mGroup.check(R.id.every_day);
                break;
            case 31:
                mGroup.check(R.id.working_days);
                break;
            case 0:
                mGroup.check(R.id.never);
                break;
            default:
                mGroup.clearCheck();
                break;
        }
    }

    final private void loadDays(ArrayAdapter<CharSequence> adapter) {
        switch (Utils.getZeroIndexedFirstDayOfWeek(mContext)) {
            case 6:
                adapter.addAll(mContext.getResources().getStringArray(R.array.repeat_days_sat));
                break;
            case 0:
                adapter.addAll(mContext.getResources().getStringArray(R.array.repeat_days_sun));
                break;
            case 1:
                adapter.addAll(mContext.getResources().getStringArray(R.array.repeat_days_mon));
                break;
            default:
                break;
        }
    }

    private void setDayOrder() {
        mStartDay = Utils.getZeroIndexedFirstDayOfWeek(mContext);
        mDayOrder = new int[DaysOfWeek.DAYS_IN_A_WEEK];

        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; ++i) {
            mDayOrder[i] = DAY_ORDER[(mStartDay + i) % 7];
        }
    }

    public interface ShowRepeatAlarmDialogListener {
        public void repeatAlarmDialoglistener(Alarm alarm);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /* SPRD: Bug 530669 java.lang.NullPointerException, read alarm on a null object reference @{ */
        Bundle bundle = getArguments();
        mAlarm = bundle.getParcelable(KEY_REPEATE_ALARM);
        Log.d(TAG,"mAlarm = " + mAlarm);
        /* @} */

        // SPRD: Bug 527665 java.lang.NullPointerException
        mContext = getActivity();
        Log.d(TAG,"onCreateDialog mContext = " + mContext);

        mContext.getSharedPreferences("repeat", Context.MODE_PRIVATE).edit()
                .putInt(mAlarm.id + "", mAlarm.daysOfWeek.getBitSet())
                .commit();
        mAlarm.daysOfWeek.setBitSetForTemp(mContext.getSharedPreferences("repeat",
                Context.MODE_PRIVATE).getInt(mAlarm.id + "", 0));
        AlertDialog.Builder mRepeatDialog = new AlertDialog.Builder(mContext);
        mMultiChoiceListView = new ListView(mContext);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View customTitleView = inflater
                .inflate(R.layout.alarm_repeat_dialogcustomtitle, null);
        mGroup = (RadioGroup) customTitleView.findViewById(R.id.repeat_radiogroup);
        mEveryDay = (RadioButton) customTitleView.findViewById(R.id.every_day);
        mWorkDay = (RadioButton) customTitleView.findViewById(R.id.working_days);
        mNever = (RadioButton) customTitleView.findViewById(R.id.never);

        mEveryDay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                for (int i = 0; i < 7; i++) {
                    mMultiChoiceListView.setItemChecked(i, true);
                }
                mAlarm.daysOfWeek.setBitSetForTemp(127);
            }
        });

        mWorkDay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                for (int i = 0; i < 7; i++) {
                    if ((mStartDay + i) % 7 == 0 || (mStartDay + i) % 7 == 6) {
                        mMultiChoiceListView.setItemChecked(i, false);
                    } else {
                        mMultiChoiceListView.setItemChecked(i, true);
                    }
                }
                mAlarm.daysOfWeek.setBitSetForTemp(31);
            }
        });

        mNever.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                for (int i = 0; i < 7; i++) {
                    mAlarm.daysOfWeek.setDaysOfWeekForTemp(false, i);
                    mMultiChoiceListView.setItemChecked(i, false);
                }
                mAlarm.daysOfWeek.setBitSetForTemp(0);
            }
        });

       ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(mContext,
                android.R.layout.simple_list_item_multiple_choice) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                setDayOrder();
                View view = super.getView(position, convertView, parent);
                HashSet<Integer> repeatDays = mAlarm.daysOfWeek.getSetDaysForTemp();
                Integer[] tempRepeatDays = repeatDays.toArray(new Integer[repeatDays.size()]);
                for (int i = 0; i < tempRepeatDays.length; i++) {
                    if (mStartDay == 6) {
                        mMultiChoiceListView.setItemChecked(tempRepeatDays[i] % 7, true);
                    } else if (mStartDay == 0) {
                        mMultiChoiceListView.setItemChecked(tempRepeatDays[i] - 1, true);
                    } else if (mStartDay == 1) {
                        mMultiChoiceListView.setItemChecked((tempRepeatDays[i] + 5) % 7, true);
                    }
                }
                updateRadioGroup(mAlarm);
                return view;
            }
        };

        mMultiChoiceListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean isChecked = mMultiChoiceListView.isItemChecked(position);
                mAlarm.daysOfWeek.setDaysOfWeekForTemp(isChecked, mDayOrder[position]);
                updateRadioGroup(mAlarm);
            }
        });
        loadDays(adapter);
        mMultiChoiceListView.setAdapter(adapter);
        mMultiChoiceListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mRepeatDialog.setCustomTitle(customTitleView);
        mRepeatDialog.setView(mMultiChoiceListView);
        mRepeatDialog.setNegativeButton(
                mContext.getResources().getString(R.string.repeat_dialog_cancel),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        mRepeatDialog.setPositiveButton(
                mContext.getResources().getString(R.string.repeat_dialog_done),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAlarm.daysOfWeek.setBitSet(mAlarm.daysOfWeek
                                .getBitSetForTemp());
                        ShowRepeatAlarmDialogListener listener = (ShowRepeatAlarmDialogListener) getTargetFragment();
                        listener.repeatAlarmDialoglistener(mAlarm);

                        mContext.getSharedPreferences("repeat", Context.MODE_PRIVATE)
                                .edit()
                                .putInt(mAlarm.id + "",
                                        mAlarm.daysOfWeek.getBitSet()).commit();
                    }
                });

        return mRepeatDialog.create();
    }

    public void onDismiss(DialogInterface dialog) {
        try {
            super.onDismiss(dialog);
        } catch (Exception e) {
            Log.w(TAG, "ignore a exception that was found when executed onDismiss, exception is:" + e.getMessage());
        }
    }
}
