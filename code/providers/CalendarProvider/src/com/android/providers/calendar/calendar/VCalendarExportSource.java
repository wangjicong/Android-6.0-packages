/** Create by Spreadst
 *  Add 20141231 of bug385990
 *  */
/*****************************************************************************/
/*                                                            Date : 06/2011 */

/*                            PRESENTATION                                   */

/*          Copyright (c) 2011 Thunder Software Technology Co.,Ltd           */

/*****************************************************************************/

/*                                                                           */

/*    This src/com/android/providers/calendar/VCalendarExportSource.java is  */

/*    company confidential, cannot be reproduced in any form without the     */

/*    written permission of Thunder Software Technology Co.,Ltd              */

/*                                                                           */

/*---------------------------------------------------------------------------*/

/*   Author :  ThunderSoft                                                   */

/*   Role   :  CalendarProvider2                                             */

/*   Reference documents : SWD3_Alcatel_Android_vCal_Req_v0.5                */

/*---------------------------------------------------------------------------*/

/*   Comments :                                                              */

/*   File     : src/com/android/providers/calendar/VCalendarExportSource.java*/

/*   Labels   :                                                              */

/*===========================================================================*/

/* Modifications on Features list / Changes Request / Problems Report        */

/*---------------------------------------------------------------------------*/

/* date    | author           | Key                   | comment              */

/*---------|------------------|-----------------------|----------------------*/

/*06/30/11 | ThunderSoft      | FR133636              |export vcalendar      */

/*---------|------------------|-----------------------|----------------------*/

/*---------|------------------|-----------------------|----------------------*/
/*===========================================================================*/

package com.sprd.providers.calendar;

import java.util.Locale;
import java.util.TimeZone;

import com.android.calendarcommon2.ICalendar.Component;
import com.android.calendarcommon2.ICalendar.Property;
import com.android.calendarcommon2.ICalendar.Parameter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.format.DateFormat;
import android.text.format.Time;
// SPRD: bug234161
import android.util.Log;

/**
 * Calendar content provider. The contract between this provider and
 * applications is defined in {@link android.provider.Calendar}.
 */
public class VCalendarExportSource {

    private static final String TAG = "VCalendarExportSource";

    public static String exportSource(SQLiteDatabase db, int eventId) {
        String begin = "BEGIN:VCALENDAR\r\n";
        String end = "END:VCALENDAR\r\n";

        StringBuffer vcalendarBuffer = new StringBuffer();
        String sqlEvents = "select * from events where _id = " + eventId + ";";
        Cursor c = null;

        String sqlReminders = "select * from REMINDERS where event_id = "
                + eventId + ";";

        String sqlAttendees = "select * from Attendees where event_id = "
                + eventId + ";";

        Cursor reminderCursor = null;
        Cursor attendeesCursor = null;
        try {
            c = db.rawQuery(sqlEvents, null);
            reminderCursor = db.rawQuery(sqlReminders, null);
            attendeesCursor = db.rawQuery(sqlAttendees, null);
            while (c.moveToNext()) {

                String title = c.getString(c.getColumnIndex(Events.TITLE));
                String des = c.getString(c.getColumnIndex(Events.DESCRIPTION));
                long dtStartmillis = c
                        .getLong(c.getColumnIndex(Events.DTSTART));
                long dtEndmillis = c.getLong(c.getColumnIndex(Events.DTEND));
                int allDay = c.getInt(c.getColumnIndex(Events.ALL_DAY));
                String location = c.getString(c
                        .getColumnIndex(Events.EVENT_LOCATION));
                String status = c.getString(c.getColumnIndex(Events.STATUS));
                String duration = c
                        .getString(c.getColumnIndex(Events.DURATION));
                String timezone = c.getString(c
                        .getColumnIndex(Events.EVENT_TIMEZONE));
                String rrule = c.getString(c.getColumnIndex(Events.RRULE));
                String exrule = c.getString(c.getColumnIndex(Events.EXRULE));
                String rdate = c.getString(c.getColumnIndex(Events.RDATE));
                String exdate = c.getString(c.getColumnIndex(Events.EXDATE));
                int has_alarm = c.getInt(c.getColumnIndex(Events.HAS_ALARM));

                // int transparency = c.getInt(c
                // .getColumnIndex(Events.TRANSPARENCY));
                int availability = c.getInt(c
                        .getColumnIndex(Events.AVAILABILITY));
                // int visibility =
                // c.getInt(c.getColumnIndex(Events.VISIBILITY));
                int access_level = c.getInt(c.getColumnIndex(Events.ACCESS_LEVEL));
                int hasAttendeeData = c.getInt(c
                        .getColumnIndex(Events.HAS_ATTENDEE_DATA));

                if (title == null)
                    title = "";
                if (des == null)
                    des = "";
                if (location == null)
                    location = "";
                if (timezone == null)
                    timezone = "";
                String clazz = "";
                switch (access_level) {
                    case 0:
                        clazz = "DEFAULT";
                        break;
                    case 1:
                        clazz = "CONFIDENTIAL";
                        break;
                    case 2:
                        clazz = "PRIVATE";
                        break;
                    case 3:
                        clazz = "PUBLIC";
                        break;
                    default:
                        clazz = "DEFAULT";
                        break;
                }

                String mName = "VEVENT";

                Component component = new Component(mName, null);

                Property ep = new Property("SUMMARY", title);
                Parameter p = new Parameter("LANGUAGE", getLanguageEnv());

                ep.addParameter(p);
                component.addProperty(ep);

                ep = new Property("DESCRIPTION", des);
                component.addProperty(ep);

                ep = new Property("LOCATION", location);
                component.addProperty(ep);

                java.util.Calendar calStart = java.util.Calendar
                        .getInstance(TimeZone.getTimeZone(Time.TIMEZONE_UTC));
                calStart.setTimeInMillis(dtStartmillis);
                java.util.Calendar newStartCalendar = java.util.Calendar
                        .getInstance();
                newStartCalendar.set(java.util.Calendar.SECOND, 0);
                newStartCalendar.set(java.util.Calendar.MILLISECOND, 0);
                newStartCalendar.set(calStart.get(java.util.Calendar.YEAR),
                        calStart.get(java.util.Calendar.MONTH),
                        calStart.get(java.util.Calendar.DAY_OF_MONTH),
                        calStart.get(java.util.Calendar.HOUR_OF_DAY),
                        calStart.get(java.util.Calendar.MINUTE));
                long startTimezone = newStartCalendar.getTimeInMillis();
                // SPRD bug 458262. Import schedule, schedule time error
                String dtStart = (String) DateFormat.format("yyyyMMdd" + "T"
                        + "HHmmss" + "Z", startTimezone);
                ep = new Property("DTSTART", dtStart);

                Parameter tip = new Parameter("TZID", timezone);
                ep.addParameter(tip);
                component.addProperty(ep);

                /**
                 * SPRD: bug234161 according to RFC2445, we only need pair of
                 * DTSTART/DTEND or DTSTART/DURATION, if dtend read from
                 * database equals 0, that means this event is a repetitive
                 * event, so we don't need to write DTEND field to VCalendar.
                 *
                 * @{
                 */
                if (dtEndmillis != 0) {
                    Log.d(TAG, "repetitive event, no need DTEND field.");
                    java.util.Calendar calEnd = java.util.Calendar
                            .getInstance(TimeZone.getTimeZone(Time.TIMEZONE_UTC));
                    calEnd.setTimeInMillis(dtEndmillis);
                    java.util.Calendar endTimeCalendar = java.util.Calendar
                            .getInstance();
                    endTimeCalendar.set(java.util.Calendar.SECOND, 0);
                    endTimeCalendar.set(java.util.Calendar.MILLISECOND, 0);
                    endTimeCalendar.set(calEnd.get(java.util.Calendar.YEAR),
                            calEnd.get(java.util.Calendar.MONTH),
                            calEnd.get(java.util.Calendar.DAY_OF_MONTH),
                            calEnd.get(java.util.Calendar.HOUR_OF_DAY),
                            calEnd.get(java.util.Calendar.MINUTE));

                    long endTimezone = endTimeCalendar.getTimeInMillis();
                    // SPRD bug 458262. Import schedule, schedule time error
                    String dtEnd = (String) DateFormat.format("yyyyMMdd" + "T"
                            + "HHmmss" + "Z", endTimezone);
                    ep = new Property("DTEND", dtEnd);
                    ep.addParameter(tip);
                    component.addProperty(ep);
                }
                /* @} */

                ep = new Property("STARTMILLIA", String.valueOf(dtStartmillis));
                component.addProperty(ep);

                ep = new Property("ENDMILLIA", String.valueOf(dtEndmillis));
                component.addProperty(ep);

                ep = new Property("X-ALLDAY", String.valueOf(allDay));
                component.addProperty(ep);

                if (status != null) {
                    ep = new Property("STATE", status);
                    component.addProperty(ep);
                }

                if (rrule != null) {
                    ep = new Property("RRULE", rrule);
                    component.addProperty(ep);
                }

                if (exrule != null) {
                    ep = new Property("EXRULE", exrule);
                    component.addProperty(ep);
                }

                if (rdate != null) {
                    ep = new Property("RDATE", rdate);
                    component.addProperty(ep);
                }

                if (exdate != null) {
                    ep = new Property("EXDATE", exdate);
                    component.addProperty(ep);
                }

                if (duration != null) {
                    ep = new Property("DURATION", duration);
                    component.addProperty(ep);
                }

                ep = new Property("HAS_ALARM", String.valueOf(has_alarm));
                component.addProperty(ep);

                ep = new Property("AVAILABILITY", String.valueOf(availability));
                component.addProperty(ep);

                ep = new Property("CLASS", clazz);
                component.addProperty(ep);

                ep = new Property("HAS_ATTENDEE",
                        String.valueOf(hasAttendeeData));
                component.addProperty(ep);

                while (attendeesCursor.moveToNext()) {
                    String attendeeName = attendeesCursor
                            .getString(attendeesCursor
                                    .getColumnIndex(Attendees.ATTENDEE_NAME));
                    String attendeeEmail = attendeesCursor
                            .getString(attendeesCursor
                                    .getColumnIndex(Attendees.ATTENDEE_EMAIL));
                    int attendeeStatus = attendeesCursor.getInt(attendeesCursor
                            .getColumnIndex(Attendees.ATTENDEE_STATUS));
                    int attendeeRelationship = attendeesCursor
                            .getInt(attendeesCursor
                                    .getColumnIndex(Attendees.ATTENDEE_RELATIONSHIP));
                    int attendeeType = attendeesCursor.getInt(attendeesCursor
                            .getColumnIndex(Attendees.ATTENDEE_TYPE));

                    String shipAtt = "";
                    switch (attendeeRelationship) {
                        case 0:
                            shipAtt = "NONE";
                            break;
                        case 1:
                            shipAtt = "ATTENDEE";
                            break;
                        case 2:
                            shipAtt = "ORGANIZER";
                            break;
                        case 3:
                            shipAtt = "PERFORMER";
                            break;
                        case 4:
                            shipAtt = "SPEAKER";
                            break;

                        default:
                            shipAtt = "NONE";
                            break;
                    }

                    String statusAtt = "";
                    switch (attendeeStatus) {
                        case 0:
                            statusAtt = "NONE";
                            break;
                        case 1:
                            statusAtt = "ACCEPTED";
                            break;
                        case 2:
                            statusAtt = "DECLINED";
                            break;
                        case 3:
                            statusAtt = "INVITED";
                            break;
                        case 4:
                            statusAtt = "TENTATIVE";
                            break;

                        default:
                            statusAtt = "NONE";
                            break;
                    }

                    ep = new Property("ATTENDEE", attendeeEmail);
                    Parameter attRole = new Parameter("ROLE", shipAtt);

                    ep.addParameter(attRole);

                    Parameter attstatusPar = new Parameter("STATUE", statusAtt);
                    ep.addParameter(attstatusPar);
                    component.addProperty(ep);
                }

                Component componentAlarm = new Component("VALARM", component);

                while (reminderCursor.moveToNext()) {
                    String minute = reminderCursor.getString(reminderCursor
                            .getColumnIndex(Reminders.MINUTES));
                    String method = reminderCursor.getString(reminderCursor
                            .getColumnIndex(Reminders.METHOD));
                    ep = new Property("REMINDERS", minute);
                    componentAlarm.addProperty(ep);

                    ep = new Property("METHOD", method);
                    componentAlarm.addProperty(ep);
                }
                component.addChild(componentAlarm);

                vcalendarBuffer.append(begin);
                vcalendarBuffer.append(component.toString());
                vcalendarBuffer.append(end);
            }
        } finally {
            if (c != null) {
                c.close();
            }
            if (reminderCursor != null) {
                reminderCursor.close();
            }
            if (attendeesCursor != null) {
                attendeesCursor.close();
            }
        }
        return vcalendarBuffer.toString();
    }

    public static String getLanguageEnv() {
        Locale l = Locale.getDefault();
        String language = l.getLanguage();
        String country = l.getCountry().toLowerCase();
        if ("zh".equals(language)) {
            if ("cn".equals(country)) {
                language = "zh-CN";
            } else if ("tw".equals(country)) {
                language = "zh-TW";
            }
        } else if ("pt".equals(language)) {
            if ("br".equals(country)) {
                language = "pt-BR";
            } else if ("pt".equals(country)) {
                language = "pt-PT";
            }
        }
        return language;
    }
}
