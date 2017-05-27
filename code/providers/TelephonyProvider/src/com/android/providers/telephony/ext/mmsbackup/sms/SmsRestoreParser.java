
package com.android.providers.telephony.ext.mmsbackup.sms;

import android.content.ContentValues;
import android.provider.Telephony.Sms;
//import android.content.Context;
//import android.util.Log;

import com.android.providers.telephony.ext.mmsbackup.BackupLog;
import com.google.android.mms.pdu.CharacterSets;
//import com.sprd.appbackup.service.Account;
//import org.apache.commons.codec.DecoderException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SmsRestoreParser implements TagStringDef {
    private static final String TAG = "SmsRestoreParser";
    String[] mStreams;
    private boolean mCancel = false;

    public ArrayList<ContentValues> getContentValues(InputStream inputStream) {
        InputStreamReader reader = new InputStreamReader(inputStream);
        bRead = new BufferedReader(reader);
        if (bRead == null) {
            BackupLog.logE(TAG, "Read file error !!!");
            return null;
        }
        ArrayList<ContentValues> vsList = new ArrayList<ContentValues>();
        int restoreCount = 0;
        while (true) {
            if (mCancel) {
                break;
            }
            String line = readLine();
            if (line == null) {
                break;
            }
            if (line.startsWith(_begin_vmsg)) {
                restoreCount++;
                mMessage = new MessageParse();
                mMessage.start();
                ContentValues vs = mMessage.getMessage(restoreCount);
                String tel = (String) vs.get(cADDRESS);
                if(tel != null && !tel.equals("")){
                    vsList.add(vs);
                }
            }
        }
        return vsList;
    }

    BufferedReader bRead = null;

    MessageParse mMessage = null;

    boolean runThread = true;

    public String readLine() {
        String line = null;
        if (bRead != null) {
            try {
                line = bRead.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return line;
    }

    public void close() {
        if (bRead != null) {
            try {
                bRead.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class MessageParse {
        public String mVersion = null;

        public VCard mVcard = null;

        public VBody mVbody = null;

        private boolean flag = true;
        int i = 0;

        public void start() {
            while (flag) {
                parse();
            }
        }

        private void parse() {
            String line = readLine();
            // fix bug 157769 on 20130426 for NullPointerException begin
            if (line == null) {
                flag = false;
                return;
            }
            // fix bug 157769 on 20130426 for NullPointerException end
            if (line.startsWith(_version)) {
                String[] property = line.split(":");// eg: [VERSION,1.1]
                if (property.length < 2) {
                    mVersion = "";
                    return;

                }
                mVersion = property[1];
                if (!mVersion.equalsIgnoreCase(_version1_1)) {
                    flag = false;
                    // stopThread("version error !");
                    BackupLog.logE(TAG, "vMessage version error !!!");
                }
            } else if (line.startsWith(_begin_vcard)) {
                mVcard = new VCard();
                mVcard.start();
            } else if (line.startsWith(_begin_vbody)) {
                mVbody = new VBody();
                mVbody.start();
            } else if (line.startsWith(_end_vmsg)) {
                flag = false;
            } else {
                BackupLog.log(TAG, "Other property in Message not parse !!!");
                BackupLog.log(TAG, "------" + line);

            }
        }

        private ContentValues getMessage(int number) {
            ContentValues values = new ContentValues();
            if (mVbody != null && mVcard != null) {

                int read = getReadValue(mVbody.mStatus);
                int type = getTypeValue(mVbody.mBox);// inbox or sent
                int lock = getLockValue(mVbody.mLocked);
                int phone_id = getPhoneidValue(mVbody.mSimid);
                // get body
                String body = mVbody.mContent.toString();
                // get date
                String date = getDateValue(mVbody.mDate);

                date = date == "" ? String.valueOf(System.currentTimeMillis()) : date;

                // at the same time,decode the body of sms
                body = body.replace(contentShareString, "");
                try {
                    body = QuotedPrintable.decode(body, CharacterSets.DEFAULT_CHARSET_NAME);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                values.put(cADDRESS, mVcard.mTel);
                values.put(cDATE, date);
                values.put(cREAD, read);
                values.put(cBODY, body);
                values.put(cTYPE, String.valueOf(type));
                values.put(cPHONE_ID, String.valueOf(phone_id));
                values.put(cLOCKED, lock);
                values.put(Sms.SEEN, 1);
                if (read == READ) {
                    values.put(cSEEN, READ);
                } else {
                    values.put(cSEEN, UNREAD);
                }
            } else {
                values.clear();
            }
            return values;
        }
    }

    public int getTypeValue(String type) {
        if (_InBox.equals(type)) {
            return INBOX;
        } else if (_Send.equals(type)) {
            return SEND;
        } else if (_Draft.equals(type)) {
            return DRAFT;
        } else if (_OutBox.equals(type)) {
            return OUTBOX;
        } else if (_Failed.equals(type)) {
            return FAILED;
        } else {
            return INBOX;
        }
    }

    private int getLockValue(String lock) {
        if (_Locked.equals(lock)) {
            return LOCKED;
        } else if (_UnLocked.equals(lock)) {
            return UNLOCKED;
        } else {
            BackupLog.logE(TAG, "the values of lock is error !!!");
            return UNLOCKED;
        }
    }

    private int getPhoneidValue(String phoneid) {
        if (_SimId0.equals(phoneid) || _SimId1.equals(phoneid)) {
            return PHONEID0;
        } else if (_SimId2.equals(phoneid)) {
            return PHONEID1;
        } else {
            BackupLog.logE(TAG, "the values of lock is error !!!");
            return PHONEID0;
        }
    }

    private int getReadValue(String read) {
        if (_Read.equals(read)) {
            return READ;
        } else if (_UnRead.equals(read)) {
            return UNREAD;
        } else {
            BackupLog.logE(TAG, "the values of read is error !!!");
            return READ;
        }
    }

    private String getDateValue(String date) {
        Date d = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        try {
            d = sdf.parse(date);
        } catch (ParseException e) {
            BackupLog.logE(TAG, "format date error !!!");
            e.printStackTrace();
            return "";
        }
        return String.valueOf(d.getTime());
    }

    class VCard {
        private static final String TAG = "VCard";

        public String mTel = "";

        private boolean flag = true;

        public void start() {
            while (flag) {
                parse();
            }
        }

        private void parse() {
            String line = readLine();
            if (line != null) {
                if (line.startsWith(_tel)) {
                    String[] property = line.split(":");
                    if (property.length < 2) {
                        mTel = "";
                        return;
                    }
                    mTel = property[1];
                } else if (line.startsWith(_end_vcard)) {
                    flag = false;
                    return;
                } else {
                    BackupLog.log(TAG, "Other property in VCard not parse !!!");
                    BackupLog.log(TAG, "------" + line);
                }
            }
        }

        public String toString() {
            StringBuffer sbf = new StringBuffer();
            sbf.append(_begin_vcard);
            sbf.append("\n");
            sbf.append(_tel);
            sbf.append(this.mTel);
            sbf.append("\n");
            sbf.append(_end_vcard);
            sbf.append("\n");
            return sbf.toString();
        }

    }

    class VBody {
        ArrayList<VCard> list = new ArrayList<VCard>();

        public VCard mVcard = null;

        // private static final String TAG = "VBody";

        public VBody mVbody = null;

        public String mStatus = null;

        public String mBox = null;

        public String mDate = null;

        public String mSimid = null;

        public String mLocked = null;

        public StringBuffer mContent = new StringBuffer();;

        private boolean flag = true;

        public void start() {
            while (flag) {
                parse();
            }
        }

        private void parse() {
            String line = readLine();
            if (line != null) {
                if (line.startsWith(_box)) {
                    String[] property = line.split(":");
                    if (property.length < 2) {
                        mBox = _InBox;
                        return;
                    }
                    mBox = property[1];
                } else if (line.startsWith(_status)) {
                    String[] property = line.split(":");
                    if (property.length < 2) {
                        mStatus = _Read;
                        return;
                    }
                    mStatus = property[1];
                } else if (line.startsWith(_simid)) {
                    String[] property = line.split(":");
                    if (property.length < 2) {
                        mSimid = _SimId0;// default phoneid is 0
                        return;
                    }
                    mSimid = property[1];
                } else if (line.startsWith(_locked)) {
                    String[] property = line.split(":");
                    if (property.length < 2) {
                        mLocked = _UnLocked;
                        return;
                    }
                    mLocked = property[1];
                } else if (line.startsWith(_date)) {
                    int index = line.indexOf(":");
                    mDate = line.substring(index + 1).trim();
                } else if (line.startsWith(_end_vbody)) {
                    flag = false;
                } else if (!line.startsWith("X-")) {
                    if (line.endsWith(ESCAPE_CHAR)) {
                        line = line.substring(0, line.length() - 1);
                        mContent.append(line);
                    } else {
                        mContent.append(line);
                    }
                }
            }
        }

        public String toString() {
            StringBuffer sbf = new StringBuffer();
            if (this.list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    VCard card = list.get(i);
                    sbf.append(card.toString());
                }
            }
            sbf.append(_begin_vbody);
            sbf.append("\n");
            sbf.append(_box);
            sbf.append("\n");
            sbf.append(_status);
            sbf.append("\n");
            sbf.append(_simid);
            sbf.append("\n");
            sbf.append(_locked);
            sbf.append("\n");
            sbf.append(_date);
            sbf.append(this.mDate);
            sbf.append("\n");
            sbf.append("\n");
            sbf.append(this.mContent);
            sbf.append("\n");
            sbf.append(_end_vbody);
            sbf.append("\n");
            return sbf.toString();
        }
    }
}
