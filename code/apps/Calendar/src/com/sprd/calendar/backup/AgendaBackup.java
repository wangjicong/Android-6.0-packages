/* SPRD: for bug473564, add backup info @{ */
package com.sprd.calendar.backup;

import java.util.ArrayList;
import java.util.List;
import com.sprd.appbackup.service.Account;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Reminders;
import android.util.Log;
import android.text.TextUtils;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;
import java.util.Calendar;
import com.sprd.calendar.vcalendar.VcalendarInfo;
import com.android.calendar.R;
import com.android.calendar.Utils;

public class AgendaBackup {
    private Context mContext;
    private int mCount;
    private boolean mCancel = false;

    public AgendaBackup(Context context) {
        mContext = context;
    }

    static final String[] sTwoCharacterNumbers =
            new String[] {
                    "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"
            };

    private static final String[] EVENT_PROJECTION = new String[] {
            Events._ID, // 0 do not remove; used in DeleteEventHelper
            Events.TITLE, // 1 do not remove; used in DeleteEventHelper
            Events.RRULE, // 2 do not remove; used in DeleteEventHelper
            Events.ALL_DAY, // 3 do not remove; used in DeleteEventHelper
            Events.CALENDAR_ID, // 4 do not remove; used in DeleteEventHelper
            Events.DTSTART, // 5 do not remove; used in DeleteEventHelper
            Events.DTEND,
            Events._SYNC_ID, // 6 do not remove; used in DeleteEventHelper
            Events.EVENT_TIMEZONE, // 7 do not remove; used in DeleteEventHelper
            Events.DESCRIPTION, // 8
            Events.EVENT_LOCATION, // 9
            Events.HAS_ALARM, // 10
            Events.ACCESS_LEVEL, // 11
            Events.CALENDAR_COLOR, // 12
            Events.HAS_ATTENDEE_DATA, // 13
            Events.GUESTS_CAN_MODIFY, // 14
            // TODO Events.GUESTS_CAN_INVITE_OTHERS has not been implemented in
            // calendar provider
            Events.GUESTS_CAN_INVITE_OTHERS, // 15
            Events.ORGANIZER, // 16
            Events.DURATION, // 17
            Events.AVAILABILITY
            // 18
    };

    private static final String[] REMINDERS_PROJECTION = new String[] {
            Reminders._ID, // 0
            Reminders.MINUTES, // 1
    };
    private static final int REMINDERS_INDEX_MINUTES = 1;

    private static final String REMINDERS_WHERE = Reminders.EVENT_ID + "=? AND (" +
            Reminders.METHOD + "=" + Reminders.METHOD_ALERT + " OR " + Reminders.METHOD + "=" +
            Reminders.METHOD_DEFAULT + ")";

    private static final String REMINDERS_SORT = Reminders.MINUTES;

    private static final String[] ATTENDEES_PROJECTION = new String[] {
            Attendees.ATTENDEE_NAME, // 0
            Attendees.ATTENDEE_EMAIL, // 1
            Attendees.ATTENDEE_STATUS, // 2
            Attendees.ATTENDEE_RELATIONSHIP
            // 3
    };
    private static final int ATTENDEES_INDEX_NAME = 0;
    private static final int ATTENDEES_INDEX_EMAIL = 1;
    private static final String ATTENDEES_WHERE = Attendees.EVENT_ID + "=%d AND "
            + Attendees.ATTENDEE_RELATIONSHIP + "<>" + Attendees.RELATIONSHIP_ORGANIZER;
    private static final String ATTENDEES_DELETE_PREFIX = Attendees.EVENT_ID + "=? AND " +
            Attendees.ATTENDEE_EMAIL + " IN (";

    private static final int EVENT_INDEX_ID = 0;
    private static final int EVENT_INDEX_TITLE = 1;
    private static final int EVENT_INDEX_RRULE = 2;
    private static final int EVENT_INDEX_ALL_DAY = 3;
    private static final int EVENT_INDEX_CALENDAR_ID = 4;
    private static final int EVENT_INDEX_SYNC_ID = 6;
    private static final int EVENT_INDEX_EVENT_TIMEZONE = 7;
    private static final int EVENT_INDEX_DESCRIPTION = 8;
    private static final int EVENT_INDEX_EVENT_LOCATION = 9;
    private static final int EVENT_INDEX_HAS_ALARM = 10;
    private static final int EVENT_INDEX_ACCESS_LEVEL = 11;
    private static final int EVENT_INDEX_DTSTART = 12;
    private static final int EVENT_INDEX_HAS_ATTENDEE_DATA = 13;
    private static final int EVENT_INDEX_GUESTS_CAN_MODIFY = 14;
    private static final int EVENT_INDEX_CAN_INVITE_OTHERS = 15;
    private static final int EVENT_INDEX_ORGANIZER = 16;
    private static final int EVENT_INDEX_DURATION = 17;

    private long mEventId;
    private static final String TAG = "AgendaBackup";
    private ArrayList<VcalendarInfo> mCalendarInfoList = new ArrayList<VcalendarInfo>();

    public ArrayList<VcalendarInfo> getCalendarInfoList(ArrayList<Account> accountList) {

        Uri mUri = Events.CONTENT_URI;
        Uri attendeeUri = Attendees.CONTENT_URI;
        ContentResolver cr = mContext.getContentResolver();
        StringBuffer whereBuffer = new StringBuffer(Events.DELETED + "='0'");
        if (accountList != null && accountList.size() > 0) {
            whereBuffer.append(" AND (");
            for (int i = 0; i < accountList.size(); i++) {
                if (0 == i) {
                    if (accountList.get(i).getAccountId().equals("0")) {
                        whereBuffer.append(Events.ORGANIZER + "='" + AgendaRestore.LOCAL_CALENDAR + "'");
                    } else {
                        whereBuffer.append(Events.ORGANIZER + "='" + accountList.get(i).getAccountName() + "'");
                    }
                } else {
                    if (accountList.get(i).getAccountId().equals("0")) {
                        whereBuffer.append(" OR " + Events.ORGANIZER + "='"
                                + AgendaRestore.LOCAL_CALENDAR + "'");
                    } else {
                        whereBuffer.append(" OR " + Events.ORGANIZER + "='"
                                + accountList.get(i).getAccountName() + "'");
                    }
                }
            }
            whereBuffer.append(")");
        }

        Log.i(TAG, "getCalendarInfoList() ! where == " +  whereBuffer.toString());

        Cursor cursor = cr.query(mUri, EVENT_PROJECTION, whereBuffer.toString(), null, null);
        if (cursor == null || cursor.getCount() <= 0) {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            return null;
        }

        ArrayList<VcalendarInfo> calendarInfoList = new ArrayList<VcalendarInfo>();
        for (int i = 0; i < cursor.getCount(); i++) {
            if (cursor.moveToNext()) {
                if (mCancel) {
                    if (cursor != null) {
                        cursor.close();
                        cursor = null;
                    }
                    mCancel = false;
                    return null;
                }
                VcalendarInfo Calendarsinfo = new VcalendarInfo();
                String a = Calendarsinfo.toString();
                mEventId = cursor.getInt(EVENT_INDEX_ID);
                Calendarsinfo.id = cursor.getInt(cursor.getColumnIndex(Events._ID));
                Calendarsinfo.eventitle = cursor.getString(cursor.getColumnIndex(Events.TITLE));
                Calendarsinfo.timezone = cursor.getString(cursor
                        .getColumnIndex(Events.EVENT_TIMEZONE));
                Calendarsinfo.allDay = cursor.getInt(cursor.getColumnIndex(Events.ALL_DAY)) != 0;
                Calendarsinfo.starttime = cursor.getLong(cursor.getColumnIndex(Events.DTSTART));
                Calendarsinfo.endtime = cursor.getLong(cursor.getColumnIndex(Events.DTEND));
                Calendarsinfo.description = cursor.getString(cursor
                        .getColumnIndex(Events.DESCRIPTION));
                Calendarsinfo.location = cursor.getString(cursor
                        .getColumnIndex(Events.EVENT_LOCATION));

                Calendarsinfo.rRule = cursor.getString(cursor.getColumnIndex(Events.RRULE));
                Calendarsinfo.duration = cursor.getString(cursor.getColumnIndex(Events.DURATION));
                Calendarsinfo.hasAlarm = cursor.getInt(cursor.getColumnIndex(Events.HAS_ALARM)) != 0;
                Calendarsinfo.uid = ContentUris
                        .withAppendedId(Events.CONTENT_URI, Calendarsinfo.id).toString();
                Calendarsinfo.accessLevel = cursor.getInt(cursor
                        .getColumnIndex(Events.ACCESS_LEVEL));
                Calendarsinfo.availablity = cursor.getInt(cursor
                        .getColumnIndex(Events.AVAILABILITY));
                Calendarsinfo.organizer = cursor.getString(cursor.getColumnIndex(Events.ORGANIZER));
                Cursor attendeeCur = cr.query(attendeeUri, ATTENDEES_PROJECTION, Attendees.EVENT_ID
                        + "=?", new String[] {
                        "" + Calendarsinfo.id
                }, null);
                StringBuilder sbAttendeeEmail = new StringBuilder();
                StringBuilder sbAttendeeStatus = new StringBuilder();
                StringBuilder sbAttendeeRelationship = new StringBuilder();
                while (attendeeCur.moveToNext()) {
                    sbAttendeeEmail.append(attendeeCur.getString(attendeeCur.
                            getColumnIndex(Attendees.ATTENDEE_EMAIL))).append(";");
                    sbAttendeeStatus.append(attendeeCur.getString(attendeeCur.
                            getColumnIndex(Attendees.ATTENDEE_STATUS))).append(";");
                    sbAttendeeRelationship.append(attendeeCur.getString(attendeeCur.
                            getColumnIndex(Attendees.ATTENDEE_RELATIONSHIP))).append(";");
                }
                if (attendeeCur != null) {
                    attendeeCur.close();
                }
                Calendarsinfo.attendeeEmail = sbAttendeeEmail.toString();
                Calendarsinfo.attendeeStatus = sbAttendeeStatus.toString();
                Calendarsinfo.attendeeRelationship = sbAttendeeRelationship.toString();
                if (Calendarsinfo.hasAlarm) {
                    Uri uri = Reminders.CONTENT_URI;
                    Cursor reminderCursor = mContext.getContentResolver().query(uri,
                            REMINDERS_PROJECTION,
                            REMINDERS_WHERE, new String[] {
                                "" + mEventId
                            }, REMINDERS_SORT);
                    StringBuilder sb = new StringBuilder();
                    try {
                        while (reminderCursor.moveToNext()) {
                            sb.append(reminderCursor.getString(REMINDERS_INDEX_MINUTES) + ";");
                        }
                        Calendarsinfo.AlarmMinute = sb.toString();
                    } finally {
                        if (reminderCursor != null) {
                            reminderCursor.close();
                        }
                    }
                }
                Calendarsinfo.hasAttendee = cursor.getInt(cursor
                        .getColumnIndex(Events.HAS_ATTENDEE_DATA)) != 0;

                calendarInfoList.add(Calendarsinfo);
                mCount++;
            }
        }

        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
        if (mCancel) {
            mCancel = false;
            return null;
        }
        return calendarInfoList;

    }

    public String combineKeyValue(String name, String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return name + ":" + value + "\r" + "\n";
    }

    private String appendStringOfKeyValue(String originStr, String appendStr) {
        if (originStr == null) {
            originStr = "";
        }
        if (TextUtils.isEmpty(appendStr)) {
            return originStr;
        }
        originStr += appendStr;

        return originStr;
    }

    public String[] getCalendarWriteDataString(ArrayList<Account> accountList) {
        mCalendarInfoList = getCalendarInfoList(accountList);

        if (mCalendarInfoList == null
                || mCalendarInfoList.size() == 0) {
            return null;
        }
        String[] dataString = new String[mCalendarInfoList.size()];
        for (int i = 0; i < mCalendarInfoList.size(); i++) {
            if (mCancel) {
                mCancel = false;
                return null;
            }
            VcalendarInfo info = mCalendarInfoList.get(i);
            /* BEGIN:VTIMEZONE */
            dataString[i] = "";
            if (i == 0) {
                // Create our iCalendar writer and start generating tags
                /* BEGIN:VCALENDAR */
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("BEGIN", "VCALENDAR"));
                /* PRODID */
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("PRODID", "-//Pekall Corporation//Android//EN"));
                /* VERSION */
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("VERSION", "2.0"));
            }
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("BEGIN", "VTIMEZONE"));
            /* TZID */
            if (info.timezone == null) {
                info.timezone = TimeZone.getDefault().toString();
            }
            String timeZone = info.timezone;
            dataString[i] = appendStringOfKeyValue(dataString[i], combineKeyValue("TZID", timeZone));

            /* BEGIN:STANDARD */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("BEGIN", "STANDARD"));
            /* DTSTART */
            String starttime = transitionMillisToVCalendarTime(info.starttime,
                    TimeZone.getTimeZone(info.timezone), !info.allDay);
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("DTSTART", starttime));
            /* TZOFFSETFROM */
            SimpleDateFormat sdf = new SimpleDateFormat("Z");
            sdf.setTimeZone(TimeZone.getTimeZone(info.timezone));
            String tzOffset = sdf.format(new Date());
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("TZOFFSETFROM", tzOffset));
            /* TZOFFSETTO */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("TZOFFSETTO", tzOffset));
            /* END:STANDARD */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("END", "STANDARD"));

            /* BEGIN:DAYLIGHT */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("BEGIN", "DAYLIGHT"));
            /* DTSTART */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("DTSTART", starttime));
            /* TZOFFSETFROM */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("TZOFFSETFROM", tzOffset));
            /* TZOFFSETTO */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("TZOFFSETTO", tzOffset));
            /* END:DAYLIGHT */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("END", "DAYLIGHT"));
            /* END:VTIMEZONE */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("END", "VTIMEZONE"));

            /* BEGIN:VEVENT/VTODO */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("BEGIN", "VEVENT"));
            /* DTSTART;TZID= */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("DTSTART;TZID=" + info.timezone, starttime));
            /* DTEND;TZID= */
            if (info.endtime != 0) {
                String endtime = transitionMillisToVCalendarTime(info.endtime,
                        TimeZone.getTimeZone(info.timezone), !info.allDay);
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("DTEND;TZID=" + info.timezone, endtime));
            }
            /* DTSTAMP */
            SimpleDateFormat stampFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
            String stamp = stampFormat.format(new Date());
            stamp = stamp.replaceAll("-", "T");
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("DTSTAMP", "" + stamp + "Z"));
            /* UID */
            dataString[i] = appendStringOfKeyValue(dataString[i], combineKeyValue("UID", info.uid));
            /* LOCATION */
            String location = null;
            try {
                location = QuotedPrintable.encode(info.location, "UTF-8");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("LOCATION;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8",
                            location));
            /* SUMMARY: */
            String title = info.eventitle;
            if (TextUtils.isEmpty(title)) {
                title = "SprdVcalendar";
            }
            try {
                title = QuotedPrintable.encode(title, "UTF-8");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("SUMMARY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8", title));
            /* DESCRIPTION */
            String description = null;
            try {
                description = QuotedPrintable.encode(info.description, "UTF-8");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("DESCRIPTION;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8",
                            description));
            /* RRULE: */
            String rrule = info.rRule;
            if (!TextUtils.isEmpty(rrule)) {
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("RRULE", rrule));
            }
            /* DURATION */
            String duration = info.duration;
            if (!TextUtils.isEmpty(duration)) {
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("DURATION", duration));
            }
            /* ATTENDEEEMAIL */
            String attendeeEmail = null;
            try {
                attendeeEmail = QuotedPrintable.encode(info.attendeeEmail, "UTF-8");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("ATTENDEEEMAIL;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8",
                            attendeeEmail));
            /* ATTENDEESTATUS */
            String attendeeStatus = info.attendeeStatus;
            if (!TextUtils.isEmpty(attendeeStatus)) {
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("ATTENDEESTATUS", attendeeStatus));
            }
            /* ATTENDEERELATIONSHIP */
            String attendeeRelationship = info.attendeeRelationship;
            if (!TextUtils.isEmpty(attendeeRelationship)) {
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("ATTENDEERELATIONSHIP", attendeeRelationship));
            }
            /* HASATTENDEE */
            if (info.hasAttendee) {
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("HASATTENDEE",
                                String.valueOf(info.hasAttendee ? 1 : 0)));
            }
            /* X-ALLDAY */
            dataString[i] = appendStringOfKeyValue(dataString[i],
                    combineKeyValue("X-ALLDAY", String.valueOf(info.allDay ? 1 : 0)));
            /* ACCESSLEVEL */
            String accessLevel = String.valueOf(info.accessLevel);
            if (!TextUtils.isEmpty(accessLevel)) {
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("ACCESSLEVEL", accessLevel));
            }
            /* AVAILABLITY */
            String availablity = String.valueOf(info.availablity);
            if (!TextUtils.isEmpty(availablity)) {
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("AVAILABLITY", availablity));
            }
            /* ORGANIZER */
            String organizer = String.valueOf(info.organizer);
            if (!TextUtils.isEmpty(organizer)) {
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("ORGANIZER", organizer));
            }

            /* Alarm */
            if (info.hasAlarm) {
                String[] minutes = info.AlarmMinute.trim().split(";");
                for (int j = 0; j < minutes.length; j++) {
                    String minute = minutes[j];
                    if (minute != null && (!"".equals(minute))) {
                        dataString[i] = appendStringOfKeyValue(dataString[i],
                                combineKeyValue("BEGIN", "VALARM"));
                        dataString[i] = appendStringOfKeyValue(dataString[i],
                                combineKeyValue("ACTION", "DISPLAY"));
                        dataString[i] = appendStringOfKeyValue(dataString[i],
                                combineKeyValue("TRIGGER;RELATED=\"START\"", "-PT"
                                        + minutes[j] + "M"));
                        dataString[i] = appendStringOfKeyValue(dataString[i],
                                combineKeyValue("DESCRIPTION", "REMINDER"));
                        dataString[i] = appendStringOfKeyValue(dataString[i],
                                combineKeyValue("END", "VALARM"));
                    }
                }
            }

            /* END:VEVENT */
            dataString[i] = appendStringOfKeyValue(dataString[i], combineKeyValue("END", "VEVENT"));
            if (i == mCalendarInfoList.size() - 1) {
                /* END:VCALENDAR */
                dataString[i] = appendStringOfKeyValue(dataString[i],
                        combineKeyValue("END", "VCALENDAR"));
            }
        }

        if (mCancel) {
            mCancel = false;
            return null;
        }
        return dataString;
    }

    String transitionMillisToVCalendarTime(long millis, TimeZone tz, boolean dst) {
        StringBuilder sb = new StringBuilder();

        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(millis);
        int zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET);
        int dstOffset = cal.get(java.util.Calendar.DST_OFFSET);
        cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset));

        sb.append(cal.get(Calendar.YEAR));
        sb.append(formatTwo(cal.get(Calendar.MONTH) + 1));
        sb.append(formatTwo(cal.get(Calendar.DAY_OF_MONTH)));
        sb.append('T');
        sb.append(formatTwo(cal.get(Calendar.HOUR_OF_DAY)));
        sb.append(formatTwo(cal.get(Calendar.MINUTE)));
        sb.append(formatTwo(cal.get(Calendar.SECOND)));
        sb.append('Z');

        return sb.toString();
    }

    public String formatTwo(int num) {
        if (num <= 12) {
            return sTwoCharacterNumbers[num];
        } else {
            return Integer.toString(num);
        }
    }

    public int getSuccessCount() {
        return mCount;
    }

    public boolean cancel() {
        mCancel = true;
        return true;
    }

    public boolean isEnabled(String accountName) {

        ContentResolver cr = mContext.getContentResolver();
        String where = null;

        if (accountName == null) {
            where = Events.DELETED + "='0'";
        } else {
            where = Events.ORGANIZER + "='" + accountName + "' AND " + Events.DELETED + "='0'" ;
        }

        Cursor cursor = cr.query(Events.CONTENT_URI, EVENT_PROJECTION, where, null, null);

        if (cursor != null) {
            int count = cursor.getCount();
            cursor.close();
            cursor = null;
            if (count > 0) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    public List<Account> getAccounts() {
        ArrayList<Account> accountList = new ArrayList<Account>();
        ContentResolver cr = mContext.getContentResolver();
        String where = Calendars.CALENDAR_ACCESS_LEVEL + ">="
                + Calendars.CAL_ACCESS_CONTRIBUTOR + " AND " + Calendars.VISIBLE + "=1";
        Cursor cursor = cr.query(Calendars.CONTENT_URI, null, where, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String accountName = cursor
                        .getString(cursor.getColumnIndex(Calendars.ACCOUNT_NAME));
                String accountType = cursor
                        .getString(cursor.getColumnIndex(Calendars.ACCOUNT_TYPE));
                Account account = new Account();
                if (AgendaRestore.LOCAL_CALENDAR.equals(accountName)) {
                    account.setAccountId("0");
                    account.setAccountName(mContext.getResources()
                            .getString(R.string.local_account));
                    account.setAccountType(accountType);
                    Log.i(TAG, "Add Local Account ! id = " + account.getAccountId()
                            + "; name = " + account.getAccountName()
                            + "; type = " + account.getAccountType());
                } else {
                    account.setAccountId(accountList.size() + 1 + "");
                    account.setAccountName(accountName);
                    account.setAccountType(accountType);
                    Log.i(TAG, "Account Which is not local ! id =" + account.getAccountId()
                            + "; name = " + account.getAccountName()
                            + "; type = " + account.getAccountType());
                }
                accountList.add(account);
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }

        return accountList;
    }
}
/* @} */