/* SPRD: for bug473564, add backup info @{ */
package com.sprd.calendar.vcalendar;

public class VcalendarInfo {
    public String eventitle;
    public long starttime;
    public long endtime;
    public boolean allDay;
    public String location;
    public String description;
    public String duration;
    public String rRule;
    public boolean hasAlarm;
    public String AlarmMinute;
    public String timezone;
    public int id;
    public Boolean hasAttendee;
    public int accessLevel;
    public int availablity;
    public String attendeeEmail;
    public String attendeeRelationship;
    public String attendeeStatus;
    public String uid;
    public String organizer;

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("VCalendarsinfo: ");

        builder.append(" eventid: ").append(id + "").

                append(" eventitle: ").append(eventitle + " ").

                append(" starttime: ").append(starttime + " ").

                append(" endtime: ").append(endtime + " ").

                append(" duration:").append(duration + " ").

                append(" allDay:").append(allDay + " ").

                append(" rRule: ").append(rRule).

                append(" location: ").append(location + " ").

                append(" description: ").append(description + " ").

                append(" timezone: ").append(timezone + " ").

                append(" hasAlarm: ").append(hasAlarm + " ").

                append("hasAttendee: ").append(hasAttendee + " ").

                append("AlarmMinute: ").append(AlarmMinute + " ").

                append(" accessLevel: ").append(accessLevel + " ").

                append(" availablity: ").append(availablity + " ").

                append("attendeeEmail: ").append(attendeeEmail + " ").

                append("attendeeRelationship: ").append(attendeeRelationship + " ").

                append("attendeeStatus: ").append(attendeeStatus + " ").

                append("uid: ").append(uid + " ").

                append("organizer: ").append(organizer + " ");

        return builder.toString();

    }
}
/* @} */