
package com.android.providers.telephony.ext.adapter;

import android.provider.Telephony.Mms;
import com.google.android.mms.pdu.PduHeaders;

public class SqlRawQueryStringAdapter {

     public static String[] MmsProjectsForBackup = new String[] {
                Mms._ID, Mms.DATE, Mms.MESSAGE_BOX, Mms.READ, Mms.MESSAGE_ID, Mms.SUBJECT,
                /* Mms.SUBJECT_CHARSET, */
                Mms.CONTENT_LOCATION, Mms.EXPIRY, Mms.MESSAGE_SIZE, Mms.REPORT_ALLOWED,
                Mms.RESPONSE_STATUS, Mms.STATUS, Mms.RETRIEVE_STATUS, Mms.RETRIEVE_TEXT,
                Mms.RETRIEVE_TEXT_CHARSET, Mms.READ_STATUS, Mms.CONTENT_CLASS, Mms.RESPONSE_TEXT,
                Mms.DELIVERY_TIME, Mms.DELIVERY_REPORT, Mms.PRIORITY, Mms.READ_REPORT, Mms.STATUS,
                /* Mms.Part.TEXT, Mms.Part.CONTENT_TYPE, */Mms.SUBSCRIPTION_ID, Mms.LOCKED
          };

     public static String getSmsBackupRawQuery(String subIds){
          String smsBackupRawQuery = "select address, date, read, body, type, thread_id, locked, sub_id " +
                                                        "from sms where (type=" + String.valueOf(1) + " or type=" + String.valueOf(2) +") "
                                                         + subIds + " order by date asc";
          return smsBackupRawQuery;
     }

     public static String getMmsBackupRawQuery(String phoneIdSelections){
          String mmsBackupRawQuery ="select _id from pdu where ( "
                        + Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_INBOX + " or "
                        + Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_SENT + " ) and ("
                        + Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_SEND_REQ + " or "
                        + Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + ")"
                        + phoneIdSelections;
          return mmsBackupRawQuery;
     }

     public static String getOneMmsBackupRawQuery(long id){
          StringBuffer sBuffer = new StringBuffer();
          sBuffer.append("select ");
          for (int i = 0; i < MmsProjectsForBackup.length; i++) {
              if (i > 0) {
                  sBuffer.append(", ");
              }
              sBuffer.append(MmsProjectsForBackup[i]);
          }
          sBuffer.append(" from pdu where _id=");
          sBuffer.append(id);
          String sql = sBuffer.toString();
          return sql;
     }

     public static String getMmsBackupAttributeRawQuery(long id){
          String str = "select text,ct from part where cid like '<text_%' and mid = "
                                    + id + "--";
          return str;
     }
}