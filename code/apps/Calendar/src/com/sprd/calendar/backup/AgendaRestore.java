/* SPRD: for bug473564, add backup info @{ */
package com.sprd.calendar.backup;

import java.util.ArrayList;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Debug;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.CalendarContract.Attendees;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import com.sprd.calendar.vcalendar.VcalendarInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

public class AgendaRestore {

    private static final String TAG = "AgendaRestore";
    private static final boolean DEBUG = true;//Debug.isDebug();
    public static String LOCAL_CALENDAR = "Local Calendar";
    private Context mContext;
    private ContentResolver mResolver;
    private boolean mCancel = false;
    public static final Charset UTF_8 = Charset.forName("UTF-8");
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
            Events.GUESTS_CAN_INVITE_OTHERS, // 15
            Events.ORGANIZER, // 16
            Events.DURATION, // 17
    };

    public long mEventId;

    public AgendaRestore(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
    }

    public byte[] readFileSdcard(InputStream in) {

        byte[] buffer = null;
        try {
            int length = in.available();
            buffer = new byte[length];
            in.read(buffer);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static String fromUtf8(byte[] b) {
        if (b == null) {
            return null;
        }
        final CharBuffer cb = UTF_8.decode(ByteBuffer.wrap(b));
        return new String(cb.array(), 0, cb.length());
    }

    private BlockHash parseIcsContent(byte[] bytes) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(fromUtf8(bytes)));
        String line = reader.readLine();
        if (!("BEGIN:VCALENDAR").equals(line)) {
            throw new IllegalArgumentException();
        }
        return new BlockHash("VCALENDAR", reader);
    }

    private long transitionVCalendarTimeToMillis(String VCalendarTime, TimeZone timezone)
    {
        if (TextUtils.isEmpty(VCalendarTime)) {
            return 0;
        }

        String date = VCalendarTime;

        GregorianCalendar cal = new GregorianCalendar(Integer.parseInt(date.substring(0, 4)),
                Integer.parseInt(date.substring(4, 6)) - 1, Integer.parseInt(date.substring(6, 8)),
                Integer.parseInt(date.substring(9, 11)), Integer.parseInt(date.substring(11, 13)),
                Integer.parseInt(date.substring(13, 15)));

        cal.setTimeZone(timezone);
        return cal.getTimeInMillis();
    }

    public ArrayList<VcalendarInfo> getCalendarInfoListFromInputStream(InputStream in) {

        ArrayList<VcalendarInfo> clendarInfoList = new ArrayList<VcalendarInfo>();

        BlockHash vcalendar;
        try {

            byte[] buffer = readFileSdcard(in);
            vcalendar = parseIcsContent(buffer);
            if (DEBUG) {
                Log.i(TAG, "blocks size:" + vcalendar.blocks.size() + vcalendar.size());
            }
            int size = vcalendar.size();
            for (int idx = 0; idx < size; idx++) {
                if (mCancel) {
                    mCancel = false;
                    return null;
                }
                VcalendarInfo mVCalendarsinfo = new VcalendarInfo();
                String eventid = vcalendar.get(idx, "VEVENTID");
                if (eventid == null || eventid.trim().equals("")) {
                    eventid = "-1";
                }
                mVCalendarsinfo.id = (int) Long.parseLong(eventid);
                /* TZID */
                String timezone = vcalendar.get(idx, "TIMEZONE");
                if (DEBUG) {
                    Log.i(TAG, "TIMEZONE:" + timezone);
                }
                mVCalendarsinfo.timezone = timezone;
                if (DEBUG) {
                    Log.i(TAG, "" + mVCalendarsinfo.timezone);
                }
                /* DTSTART */
                String starttime = vcalendar.get(idx, "TIMESTART");
                mVCalendarsinfo.starttime = transitionVCalendarTimeToMillis(
                        starttime, TimeZone.getTimeZone("GMT" + timezone));
                if (DEBUG) {
                    Log.i(TAG, "DTSTART:" + mVCalendarsinfo.starttime);
                }
                /* DTEND */
                String endtime = vcalendar.get(idx, "TIMEEND");
                if (!TextUtils.isEmpty(endtime)) {
                    mVCalendarsinfo.endtime = transitionVCalendarTimeToMillis(
                            endtime, TimeZone.getTimeZone("GMT" + timezone));
                    if (DEBUG) {
                        Log.i(TAG, "DTEND:" + mVCalendarsinfo.endtime);
                    }
                }
                /* LOCATION */
                String location = vcalendar.get(idx, "LOCATION");
                if (!TextUtils.isEmpty(location)) {
                    if (location.endsWith("=")) {
                        location = location.substring(0, location.length() - 1);
                    }
                    if (DEBUG) {
                        Log.i(TAG, "LOCATION:" + location);
                    }
                    mVCalendarsinfo.location = QuotedPrintable.decode(location, "UTF-8");
                }
                /* SUMMARY */
                String title = vcalendar.get(idx, "SUMMARY");
                if (!TextUtils.isEmpty(title)) {
                    if (title.endsWith("=")) {
                        title = title.substring(0, title.length() - 1);
                    }
                    if (DEBUG) {
                        Log.i(TAG, "SUMMARY:" + title);
                    }
                    mVCalendarsinfo.eventitle = QuotedPrintable.decode(title, "UTF-8");
                }
                /* DESCRIPTION */
                String text = vcalendar.get(idx, "EVENTDESCRIPTION");
                if (!TextUtils.isEmpty(text)) {
                    if (text.endsWith("=")) {
                        text = text.substring(0, text.length() - 1);
                    }
                    if (DEBUG) {
                        Log.i(TAG, "DESCRIPTION:" + text);
                    }
                    mVCalendarsinfo.description = QuotedPrintable.decode(text, "UTF-8");
                }
                /* ATTENDEEEMAIL */
                String attendeeEmail = vcalendar.get(idx, "ATTENDEEEMAIL");
                if (!TextUtils.isEmpty(attendeeEmail)) {
                    if (attendeeEmail.endsWith("=")) {
                        attendeeEmail = attendeeEmail.substring(0,
                                attendeeEmail.length() - 1);
                    }
                    if (DEBUG) {
                        Log.i(TAG, "ATTENDEEEMAIL:" + attendeeEmail);
                    }
                    mVCalendarsinfo.attendeeEmail = QuotedPrintable.decode(attendeeEmail, "UTF-8");
                    if (DEBUG) {
                        Log.i(TAG, "ATTENDEEEMAIL:" + mVCalendarsinfo.attendeeEmail);
                    }
                    /* ATTENDEESTATUS */
                    String attendeeStatus = vcalendar.get(idx, "ATTENDEESTATUS");
                    mVCalendarsinfo.attendeeStatus = attendeeStatus;
                    if (DEBUG) {
                        Log.i(TAG, "ATTENDEESTATUS:" + mVCalendarsinfo.attendeeStatus);
                    }
                    /* ATTENDEERELATIONSHIP */
                    String attendeeRelationship = vcalendar.get(idx,
                            "ATTENDEERELATIONSHIP");
                    mVCalendarsinfo.attendeeRelationship = attendeeRelationship;
                    if (DEBUG) {
                        Log.i(TAG, "ATTENDEERELATIONSHIP:" + mVCalendarsinfo.attendeeRelationship);
                    }
                }
                /* DURATION */
                String duration = vcalendar.get(idx, "DURATION");
                if (!TextUtils.isEmpty(duration)) {
                    if (duration.endsWith("=")) {
                        duration = duration.substring(0, duration.length() - 1);
                    }
                    if (DEBUG) {
                        Log.i(TAG, "DURATION:" + duration);
                    }
                    mVCalendarsinfo.duration = QuotedPrintable.decode(duration, "UTF-8");
                }
                /* RRULE */
                String rrules = vcalendar.get(idx, "RRULE");
                // String BYDAY = rrules;
                mVCalendarsinfo.rRule = rrules;
                if (DEBUG) {
                    Log.i(TAG, "RRULE:" + rrules);
                }

                String allDay = vcalendar.get(idx, "X-ALLDAY");
                if ("1".equals(allDay)) {
                    mVCalendarsinfo.allDay = true;
                } else {
                    mVCalendarsinfo.allDay = false;
                }
                if (DEBUG) {
                    Log.i(TAG, "X-ALLDAY:" + mVCalendarsinfo.allDay);
                }
                /* ACCESSLEVEL */
                String accessLevel = vcalendar.get(idx, "ACCESSLEVEL");
                if (!TextUtils.isEmpty(accessLevel)) {
                    mVCalendarsinfo.accessLevel = Integer.valueOf(accessLevel);
                    if (DEBUG) {
                        Log.i(TAG, "ACCESSLEVEL:" + mVCalendarsinfo.accessLevel);
                    }
                }
                /* AVAILABLITY */
                String availablity = vcalendar.get(idx, "AVAILABLITY");
                if (!TextUtils.isEmpty(availablity)) {
                    mVCalendarsinfo.availablity = Integer.valueOf(availablity);
                    if (DEBUG) {
                        Log.i(TAG, "AVAILABLITY:" + mVCalendarsinfo.availablity);
                    }
                }
                /* ORGANIZER */
                String organizer = vcalendar.get(idx, "ORGANIZER");
                if (!TextUtils.isEmpty(organizer)) {
                    mVCalendarsinfo.organizer = organizer;
                    if (DEBUG) {
                        Log.i(TAG, "ORGANIZER:" + mVCalendarsinfo.organizer);
                    }
                }
                /* ALARM */
                String alarm = vcalendar.get(idx, "TRIGGER");
                if (DEBUG) {
                    Log.i(TAG, "alarm: " + alarm);
                }
                if (!TextUtils.isEmpty(alarm)) {
                    mVCalendarsinfo.hasAlarm = true;
                    StringBuilder sb = new StringBuilder();
                    String[] alarmstr = alarm.trim().split(";");
                    for (String i : alarmstr) {
                        String str = i.substring(3, (i.length() - 1));
                        sb.append(str).append(";");
                    }
                    mVCalendarsinfo.AlarmMinute = sb.toString();
                    if (DEBUG) {
                        Log.i(TAG, "ALARM:" + mVCalendarsinfo.AlarmMinute);
                    }
                    String hasAttendee = vcalendar.get(idx, "HASATTENDEE");
                    if (hasAttendee != null) {
                        if (hasAttendee.equals("1")) {
                            mVCalendarsinfo.hasAttendee = true;
                        } else {
                            mVCalendarsinfo.hasAttendee = false;
                        }
                    }
                }
                clendarInfoList.add(mVCalendarsinfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return clendarInfoList;

    }

    private class UnterminatedBlockException extends IOException {
        private static final long serialVersionUID = 1L;

        UnterminatedBlockException(String name) {
            super(name);
        }
    }

    private class BlockHash {
        String name;

        ArrayList<HashMap<String, String>> hashList = new ArrayList<HashMap<String, String>>();

        ArrayList<BlockHash> blocks = new ArrayList<BlockHash>();

        BlockHash(String _name, BufferedReader reader) throws IOException {
            name = _name;
            String lastField = null;
            String lastValue = null;
            String preline = null;
            HashMap<String, String> hash = new HashMap<String, String>();
            while (true) {
                // Get a line; we're done if it's null
                String line = reader.readLine();
                if (line == null) {
                    throw new UnterminatedBlockException(name);
                }
                if (line.endsWith("=")) {
                    if (preline != null) {
                        line = preline.substring(0, preline.length() - 1) + line;
                    }
                    preline = line;
                    continue;
                } else {
                    if (preline != null && preline.endsWith("=")) {
                        line = preline.substring(0, preline.length() - 1) + line;
                        preline = null;
                    }
                }
                int length = line.length();
                if (length == 0) {
                    // We shouldn't ever see an empty line
                    throw new IllegalArgumentException();
                }
                // A line starting with tab is a continuation
                if (line.charAt(0) == '\t') {
                    // Remember the line and length
                    lastValue = line.substring(1);
                    // Save the concatenation of old and new values
                    hash.put(lastField, hash.get(lastField) + lastValue);
                    continue;
                }
                // Find the field delimiter
                int pos = line.indexOf(':');
                // If not found, or at EOL, this is a bad ics
                if (pos < 0 || pos >= length) {
                    continue;
                }
                // Remember the field, value, and length
                lastField = line.substring(0, pos);
                lastValue = line.substring(pos + 1);

                if (lastField.equals("BEGIN")) {
                    continue;
                } else if (lastField.equals("END")) {

                    if (lastValue.equals("VEVENT")) {
                        hashList.add(hash);
                        hash = new HashMap<String, String>();
                    } else if (lastValue.equals("VCALENDAR")) {
                        break;
                    }
                } else if (lastField.startsWith("TRIGGER")) {
                    if (hash.containsKey("TRIGGER")) {
                        hash.put("TRIGGER", hash.get("TRIGGER") + ";" + lastValue);
                    } else {
                        hash.put("TRIGGER", lastValue);
                    }
                } else {
                    // Save it away and continue
                    if (hash.containsKey(lastField)) {
                        hash.put(lastField, hash.get(lastField) + ";" + lastValue);
                    } else {
                        hash.put(lastField, lastValue);
                    }
                    if (lastField.startsWith("SUMMARY")) {
                        hash.put("SUMMARY", lastValue);
                    } else if (lastField.startsWith("DESCRIPTION;")) {
                        hash.put("EVENTDESCRIPTION", lastValue);
                    } else if (lastField.startsWith("LOCATION")) {
                        hash.put("LOCATION", lastValue);
                    } else if (lastField.startsWith("ATTENDEEEMAIL")) {
                        hash.put("ATTENDEEEMAIL", lastValue);
                    } else if (lastField.startsWith("DTSTART;TZID")) {
                        int index = lastField.indexOf("=");
                        hash.put("TIMEZONE", lastField.substring(index + 1));
                        hash.put("TIMESTART", lastValue);
                    } else if (lastField.startsWith("DTEND;TZID")) {
                        hash.put("TIMEEND", lastValue);
                    } else if (lastField.startsWith("RRULE")) {
                        hash.put("RRULE", lastValue);
                    } else if (lastField.startsWith("DTSTART")) {
                        hash.put("TIMEZONE", "UTC");
                        hash.put("TIMESTART", lastValue);
                    } else if (lastField.startsWith("DTEND")) {
                        hash.put("TIMEEND", lastValue);
                    }
                }
            }
        }

        String get(int i, String field) {
            return hashList.get(i).get(field);
        }

        int size() {
            return hashList.size();
        }

    }

    public void restoreOneEvent(VcalendarInfo minfo) {
        String where = "";
        Uri eventUri = doInsert(minfo);
        mEventId = ContentUris.parseId(eventUri);
        if (minfo.hasAlarm) {
            saveReminders(mEventId, minfo.AlarmMinute);
        }

    }

    public Uri doInsert(VcalendarInfo info) {
        int calendarId = 1;
        String initOrganizer = info.organizer;
        Cursor calendarsCursor = mResolver.query(Calendars.CONTENT_URI, null, null, null, null);
        boolean accountExists = false;
        if (calendarsCursor != null) {
            String owner = null;
            while (calendarsCursor.moveToNext()) {
                owner = calendarsCursor.getString(calendarsCursor
                        .getColumnIndex(Calendars.OWNER_ACCOUNT));
                if (DEBUG) {
                    Log.d(TAG, "1.owner: " + owner);
                }
                if (owner.equals(info.organizer)) {
                    accountExists = true;
                    calendarId = calendarsCursor.getInt(calendarsCursor
                            .getColumnIndex(Calendars._ID));
                    break;
                }
            }
        }
        if (calendarsCursor != null) {
            calendarsCursor.close();
        }
        if (!accountExists) {
            info.organizer = LOCAL_CALENDAR;
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
        Uri insertUri = mResolver.insert(Events.CONTENT_URI, values);
        if (info.attendeeEmail != null) {
            long event_id = ContentUris.parseId(insertUri);
            if (DEBUG) {
                Log.i(TAG, "event_id: " + event_id);
            }
            String[] attendeeEmails = info.attendeeEmail.split(";");
            String[] attendeeStatuses = info.attendeeStatus.split(";");
            String[] attendeeRelationships = info.attendeeRelationship.split(";");
            for (int i = 0; i < attendeeEmails.length; i++) {
                ContentValues attendeeVal = new ContentValues();
                attendeeVal.put(Attendees.EVENT_ID, event_id);
                if (!accountExists && attendeeEmails[i].equalsIgnoreCase(initOrganizer)) {
                    if (DEBUG) {
                        Log.d(TAG, "3.attendeeEmails[i]: " + attendeeEmails[i] + ",initOrganizer: "
                                + initOrganizer);
                    }
                    attendeeVal.put(Attendees.ATTENDEE_EMAIL, LOCAL_CALENDAR);
                } else {
                    attendeeVal.put(Attendees.ATTENDEE_EMAIL, attendeeEmails[i]);
                }
                attendeeVal.put(Attendees.ATTENDEE_STATUS, attendeeStatuses[i]);
                attendeeVal.put(Attendees.ATTENDEE_RELATIONSHIP, attendeeRelationships[i]);
                mResolver.insert(Attendees.CONTENT_URI, attendeeVal);
            }
        }
        return insertUri;
    }

    boolean saveReminders(long eventId, String reminderMinutes) {

        if (TextUtils.isEmpty(reminderMinutes))
            return false;

        ContentValues values = new ContentValues();

        String[] alarmstr = reminderMinutes.trim().split(";");

        for (String i : alarmstr) {
            int minutes = Integer.parseInt(i);
            values.clear();
            values.put(Reminders.MINUTES, minutes);
            values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
            values.put(Reminders.EVENT_ID, eventId);
            mResolver.insert(Reminders.CONTENT_URI, values);
        }
        return true;
    }

    protected int getDefaultCalendarId() {
        Cursor calendarsCursor;
        int defaultCalendarId;
        calendarsCursor = mResolver.query(Calendars.CONTENT_URI, null, null, null, null);

        if (calendarsCursor != null && calendarsCursor.getCount() > 0) {
            calendarsCursor.moveToNext();
            defaultCalendarId = calendarsCursor.getInt(calendarsCursor.getColumnIndex("_id"));
        } else {
            defaultCalendarId = insertDefaultCalendar("sprdcalendar", Time.getCurrentTimezone(),
                    "sprduser@m.google.com");
        }

        if (calendarsCursor != null) {
            calendarsCursor.close();
        }

        return defaultCalendarId;
    }

    private int insertDefaultCalendar(String name, String timezone, String account) {
        ContentValues m = new ContentValues();
        m.put(Calendars.NAME, name);
        m.put(Calendars.CALENDAR_DISPLAY_NAME, name);
        m.put(Calendars.CALENDAR_COLOR, "0");
        m.put(Calendars.CALENDAR_TIME_ZONE, timezone);
        m.put(Calendars.VISIBLE, 1);
        m.put(Calendars.OWNER_ACCOUNT, account);
        m.put(Calendars.ACCOUNT_NAME, account);
        m.put(Calendars.ACCOUNT_TYPE, "com.android.exchange");
        m.put(Calendars.SYNC_EVENTS, 0);
        m.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
        Uri url = mResolver.insert(Calendars.CONTENT_URI, m);
        String id = url.getLastPathSegment();
        if (DEBUG) {
            Log.d(TAG, "insertDefaultCalendar:" + id);
        }
        return Integer.parseInt(id);
    }

    public boolean cancel() {
        mCancel = true;
        return true;
    }
}
/* @} */