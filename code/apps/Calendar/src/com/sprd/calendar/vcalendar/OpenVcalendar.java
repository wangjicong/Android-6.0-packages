/* SPRD: for bug473564, add backup info @{ */
package com.sprd.calendar.vcalendar;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TimeZone;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.android.calendar.event.EditEventActivity;
import com.android.calendar.event.EditEventHelper;
import com.android.calendar.ApiHelper;
import com.android.calendar.CalendarController;
import com.android.calendar.PermissionsActivity;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.Utils;

public class OpenVcalendar extends Activity {

    /**
     * TAG
     */
    private static final String TAG = "OpenVcalendar";

    /**
     * The local name of account
     */
    private static String LOCAL_CALENDAR = "Local Calendar";

    /**
     * import vcs file success
     */
    private static final int IMPORT_SUCCESS = 0;

    /**
     * parse vcs file error
     */
    private static final int PARSE_FAILED = 1;

    /**
     * import vcs file failed
     */
    private static final int IMPORT_FAILED = 2;


    private static final int IMPORT_TYPE_FAILED =3;

    /**
     * participate activity
     */
    private static final int PARTICIPATE = 1;

    /**
     * undermined
     */
    private static final int UNDETERMINED = 4;

    /**
     * nonparticipation
     */
    private static final int NONPARTICIPATION = 2;

    /**
     * dialog to prompt when user open vcs's file
     */
    private AlertDialog mDlg;

    /**
     * display this dialog When load file and insert datebase
     */
    private ProgressDialog waitDialog = null;

    /**
     * The file's address
     */
    private Uri mUri = null;

    /**
     * data sharing
     */
    private ContentResolver mContentResolver = null;

    /**
     * Context
     */
    private Context mContext = null;

    /**
     * Event id
     */
    private long mEventId;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case IMPORT_SUCCESS:
                    (Toast.makeText(mContext, R.string.import_ok, Toast.LENGTH_LONG)).show();
                    break;
                case PARSE_FAILED:
                    (Toast.makeText(mContext, R.string.parse_fail, Toast.LENGTH_LONG)).show();
                    break;
                case IMPORT_TYPE_FAILED:
                    (Toast.makeText(mContext, R.string.import_type_error, Toast.LENGTH_LONG)).show();
                    break;
                case IMPORT_FAILED:
                    if (mIsExistAccount) {
                        (Toast.makeText(mContext, R.string.import_error, Toast.LENGTH_LONG)).show();
                    } else {
                        Toast.makeText(mContext, R.string.no_exist_accounts, Toast.LENGTH_LONG).show();
                    }
                    break;
                default:
                    break;
            }
            try {
                waitDialog.dismiss();
            } catch (Exception e) {
                Log.d(TAG, "--------------Calendar OpenVcalendar.java close the waitDialog error!");
            }

            finish();
        }

    };

    /**
     * operation datebase thread
     */
    private Thread thread = new Thread(new Runnable() {

        @Override
        public void run() {
            loadFileAndInsertDatabase();
        }
    });

    private boolean mIsImporting = false;
    private boolean mIsExistAccount = false;
    private boolean mimportTypeError= false;

    //SPRD: bug498391, request runtime permissions
    private int mNeedRequestPermissions = 0;
    private String mUriStr = null;
    public static final String ACTIVITY_FLAG = "Activity_Flag";
    SharedPreferences mPreferences = null;
    SharedPreferences.Editor mEditor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        this.mUri = uri;
        this.mContentResolver = getContentResolver();
        mContext = this;

        /* SPRD: bug498391, request runtime permissions @{ */
        mPreferences = null;
        mEditor = null;
        mPreferences = getSharedPreferences("vcalendar", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        mNeedRequestPermissions = Utils.checkPermissions(this);
        if (0 != mNeedRequestPermissions) {
            Intent intent = new Intent(this, PermissionsActivity.class);
            intent.putExtra(ACTIVITY_FLAG, 2);
            startActivity(intent);
            if (null != uri) {
                mEditor.putString("vcalendarUri", uri.toString());
                mEditor.commit();
            }
            finish();
        }
        mUriStr = mPreferences.getString("vcalendarUri", null);
        if (null == mUri && null != mUriStr) {
            mUri = Uri.parse(mUriStr);
        }
        /* @} */

        waitDialog = new ProgressDialog(this);
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.setTitle(null);
        waitDialog.setIcon(null);
        waitDialog.setMessage(getResources().getString(R.string.import_waiting));
        waitDialog.setCancelable(false);

        mDlg = new AlertDialog.Builder(this)
                .setTitle(R.string.type_vcalendar)
                .setMessage(R.string.import_vcalendar)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // close the activity
                        finish();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!mIsImporting) {
                            mIsImporting = true;
                            waitDialog.show();
                            thread.start();
                        }
                    }
                }).show();
        mDlg.setCancelable(false);
    }

    /**
     * Insert the date to datebase after load and parse the vcs file.
     * Later display EditEventActivity
     */
    private void loadFileAndInsertDatabase() {
        Vcalendar vcalendar = new Vcalendar(mContext);
        ArrayList<Uri> uriList = new ArrayList<Uri>();
        ArrayList<VcalendarInfo> vcalendarInfoList = vcalendar.VcalendarImport(mContentResolver, mUri);
        if (vcalendarInfoList == null) {
            handler.sendEmptyMessage(PARSE_FAILED);
        } else {
            try {
                for (VcalendarInfo cInfo : vcalendarInfoList) {
                    Uri eventUri = restoreOneEvent(cInfo);
                    if (eventUri != null) {
                        uriList.add(eventUri);
                    }
                }
                int count = uriList.size();
                if (count != 0) {
                  Message message = new Message();
                  message.what = IMPORT_SUCCESS;
                  handler.sendMessage(message);
                } else if (mimportTypeError) {
                    handler.sendEmptyMessage(IMPORT_TYPE_FAILED);
                } else {
                    handler.sendEmptyMessage(IMPORT_FAILED);
                }
            } catch (Exception e) {
                Log.i(TAG, "MODE_RESTORE --- Exception ! tObserver.onResult(-1)! ");
            }
        }
    }

    private Uri restoreOneEvent(VcalendarInfo minfo){
        Uri eventUri  = doInsert(minfo);
        if (eventUri != null) {
            mEventId = ContentUris.parseId(eventUri);
            if (minfo.hasAlarm) {
                saveReminders(mEventId, minfo.AlarmMinute);
            }
        }
        return eventUri;
    }
    /**
     * Jump to EditEventActivity
     *
     * @param info eventInfo
     */
    private void startEditActivity(VcalendarInfo info, long eventId) {
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, info.starttime);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, info.endtime);
        intent.setClass(mContext, EditEventActivity.class);
        intent.putExtra(CalendarController.EVENT_EDIT_ON_LAUNCH, true);
        mEventId = eventId;
        mContext.startActivity(intent);
    }

    /**
     * Import the event activity datas to database
     *
     * @param info
     * @return
     */
    public Uri doInsert(VcalendarInfo info){
        int calendarId = 1;
        Cursor calendarsCursor = mContentResolver.query(Calendars.CONTENT_URI, null, null, null, null);
        boolean accountExists = false;

        if (calendarsCursor != null && calendarsCursor.getCount() > 0) {
            mIsExistAccount = true;
            String owner = null;
            calendarsCursor.moveToFirst();
            while (calendarsCursor.moveToNext()) {
                owner = calendarsCursor.getString(calendarsCursor.getColumnIndex(Calendars.OWNER_ACCOUNT));
                if (owner != null && owner.equals(info.organizer)) {
                    accountExists = true;
                    calendarId = calendarsCursor.getInt(calendarsCursor.getColumnIndex(Calendars._ID));
                    break;
                }
            }
        } else {
            mIsExistAccount = false;
            if (calendarsCursor != null) {
                calendarsCursor.close();
                calendarsCursor = null;
            }
            return null;
        }
        if (calendarsCursor != null) {
            calendarsCursor.close();
            calendarsCursor = null;
        }
        if (!accountExists) {
            info.organizer = LOCAL_CALENDAR;
        }

        if (info.organizer == LOCAL_CALENDAR && info.attendeeEmail != null && !"".equals(info.attendeeEmail)) {
            Log.d(TAG, "import calendar type error");
            mimportTypeError = true;
            return null;
        }

        ContentValues values = new ContentValues();

        values.put(Events.TITLE, info.eventitle);
        values.put(Events.CALENDAR_ID, calendarId);
        values.put(Events.DTSTART, info.starttime);
        if (info.endtime == 0) {
            values.put(Events.DURATION, info.duration);
        } else {
            values.put(Events.DTEND, info.endtime);
        }
        if (info.rRule != null) {
            if (info.rRule.startsWith("FREQ")) {
                values.put(Events.RRULE, info.rRule);
            }
        }
        values.put(Events.DESCRIPTION, info.description);
        values.put(Events.EVENT_TIMEZONE, info.timezone);
        values.put(Events.EVENT_LOCATION, info.location);
        values.put(Events.ALL_DAY, info.allDay ? 1 : 0);
        values.put(Events.HAS_ALARM, info.hasAlarm ? 1 : 0);
        values.put(Events.ACCESS_LEVEL, info.accessLevel);
        values.put(Events.AVAILABILITY, info.availablity);
        values.put(Events.ORGANIZER, info.organizer);
        if (info.hasAttendee == null) {
            info.hasAttendee = true;
        }
        values.put(Events.HAS_ATTENDEE_DATA, info.hasAttendee ? 1 : 0);
        Uri insertUri = mContentResolver.insert(Events.CONTENT_URI, values);
        if (info.attendeeEmail != null && !"".equals(info.attendeeEmail)) {
            try {
                long event_id = ContentUris.parseId(insertUri);
                String[] attendeeEmails = info.attendeeEmail.split(";");
                String[] attendeeStatuses = info.attendeeStatus.split(";");
                for (int i=0;i<attendeeEmails.length;i++) {
                    ContentValues attendeeVal = new ContentValues();
                    attendeeVal.put(Attendees.EVENT_ID, event_id);
                    if (!accountExists && attendeeEmails[i].equalsIgnoreCase(info.organizer)) {
                        attendeeVal.put(Attendees.ATTENDEE_EMAIL, LOCAL_CALENDAR);
                    } else {
                        attendeeVal.put(Attendees.ATTENDEE_EMAIL, attendeeEmails[i]);
                    }
                    int status = UNDETERMINED;
                    if (i < attendeeStatuses.length) {
                        if (attendeeStatuses[i].equalsIgnoreCase("ACCEPTED")) {
                            status = PARTICIPATE;
                        } else if (attendeeStatuses[i].equalsIgnoreCase("NONE")) {
                            status = UNDETERMINED;
                        } else if (attendeeStatuses[i].equalsIgnoreCase("DECLINED")){
                            status = NONPARTICIPATION;
                        }
                    }
                    attendeeVal.put(Attendees.ATTENDEE_STATUS, status);
                    mContentResolver.insert(Attendees.CONTENT_URI, attendeeVal);
                }
            } catch (Exception e) {
                Log.d(TAG, "-----------------------------------insert attendeeEail fail!");
                Log.d(TAG, "save the attedeeEmail occur error!");
            }
        }
        return insertUri;
    }

    /**
     * save event Reminders
     *
     * @param eventId eventId
     * @param reminderMinutes reminderMinutes time
     * @return success or fail
     */
    private boolean saveReminders(long eventId,String reminderMinutes) {
        if (TextUtils.isEmpty(reminderMinutes)) {
            return false ;
        }
        try {
            ContentValues values = new ContentValues();
            String[] alarmstr  = reminderMinutes.trim().split(";");
            for (String i:alarmstr) {
                int minutes = Integer.parseInt(i);
                values.clear();
                values.put(Reminders.MINUTES, minutes);
                values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
                values.put(Reminders.EVENT_ID, eventId);
                mContentResolver.insert(Reminders.CONTENT_URI,values);
            }
        } catch (Exception e) {
            Log.d(TAG, "---------------insert reminder error!");
            Log.d(TAG, "OpenVcalendar.saveRminder() occure error");
            return false;
        }
        return true;
    }
}
