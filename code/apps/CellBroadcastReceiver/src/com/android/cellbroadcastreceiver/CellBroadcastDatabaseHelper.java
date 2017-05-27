/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.cellbroadcastreceiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Telephony;
import android.telephony.SmsCbCmasInfo;
import android.telephony.SmsCbEtwsInfo;
import android.telephony.SmsCbMessage;
import android.util.Log;

import com.android.internal.telephony.gsm.SmsCbConstants;
import com.sprd.cellbroadcastreceiver.provider.CellbroadcastDefine;
import com.sprd.cellbroadcastreceiver.provider.ChannelTableDefine;
import com.sprd.cellbroadcastreceiver.provider.CreateLangViewDefine;
import com.sprd.cellbroadcastreceiver.provider.LangMapTableDefine;
import com.sprd.cellbroadcastreceiver.provider.LangNameTableDefine;
import com.sprd.cellbroadcastreceiver.provider.MncMccTableDefine;
import com.sprd.cellbroadcastreceiver.provider.CommonSettingTableDefine;
import com.sprd.android.config.OptConfig;

/**
 * Open, create, and upgrade the cell broadcast SQLite database. Previously an inner class of
 * {@code CellBroadcastDatabase}, this is now a top-level class. The column definitions in
 * {@code CellBroadcastDatabase} have been moved to {@link Telephony.CellBroadcasts} in the
 * framework, to simplify access to this database from third-party apps.
 */
public class CellBroadcastDatabaseHelper extends SQLiteOpenHelper implements
		ChannelTableDefine, CellbroadcastDefine, LangMapTableDefine,
		LangNameTableDefine, MncMccTableDefine, CreateLangViewDefine,
		CommonSettingTableDefine {

    private static final String TAG = "CellBroadcastDatabaseHelper";

    static final String DATABASE_NAME = "cell_broadcasts.db";
    static final String TABLE_NAME = "broadcasts";

    /** Temporary table for upgrading the database version. */
    static final String TEMP_TABLE_NAME = "old_broadcasts";
    private Context mContext;

    /**
     * Database version 1: initial version
     * Database version 2-9: (reserved for OEM database customization)
     * Database version 10: adds ETWS and CMAS columns and CDMA support
     * Database version 11: adds delivery time index
     */
    static final int DATABASE_VERSION = 14;

    public CellBroadcastDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BROADCAST);
        createDeliveryTimeIndex(db);

        db.execSQL(CREATE_CHANNEL_TABLE);
        Log.d(TAG, "create database table lang_map:"+ LANG_MAP_TABLE_NAME);
        db.execSQL(CREATE_LANG_MAP_TABLE);
        db.execSQL(CREATE_MNC_MCC_TABLE);
        db.execSQL(CREATE_LANG_NAME_TABLE);
        db.execSQL(CREATE_LANG_VIEW);
        db.execSQL(CREATE_COMMOMSETTING_TABLE);
        
        /* wangxing 20160628 add for channel 50 and 60 start @{ */
		if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
			for(int j = 0; j < 100; j++){
	            ContentValues values_channel50 = new ContentValues(4);
	            values_channel50.put(ChannelTableDefine.CHANNEL_ID, "50");
	            values_channel50.put(ChannelTableDefine.CHANNEL_NAME, "INDIA");
	            values_channel50.put(ChannelTableDefine.ENABLE, 1);
	            values_channel50.put(ChannelTableDefine.SUB_ID, j);
	            db.insert(ChannelTableDefine.TABLE_NAME, null, values_channel50);
	
	            ContentValues values_channel60 = new ContentValues(4);
	            values_channel60.put(ChannelTableDefine.CHANNEL_ID, "60");
	            values_channel60.put(ChannelTableDefine.CHANNEL_NAME, "INDIA");
	            values_channel60.put(ChannelTableDefine.ENABLE, 1);
	            values_channel60.put(ChannelTableDefine.SUB_ID, j);
	            db.insert(ChannelTableDefine.TABLE_NAME, null, values_channel60);
            }
		}
		/* wangxing 20160628 add for channel 50 and 60 end @} */

        importLangInfoTablesFromXml(db,R.xml.mnc_mcc);
        importLangInfoTablesFromXml(db,R.xml.lang_name);
        importLangInfoTablesFromXml(db,R.xml.lang_map);
    }

    private void createDeliveryTimeIndex(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS deliveryTimeIndex ON " + CellbroadcastDefine.TABLE_NAME
                + " (" + Telephony.CellBroadcasts.DELIVERY_TIME + ");");
    }

    /** Columns to copy on database upgrade. */
    private static final String[] COLUMNS_V1 = {
            Telephony.CellBroadcasts.GEOGRAPHICAL_SCOPE,
            Telephony.CellBroadcasts.SERIAL_NUMBER,
            Telephony.CellBroadcasts.V1_MESSAGE_CODE,
            Telephony.CellBroadcasts.V1_MESSAGE_IDENTIFIER,
            Telephony.CellBroadcasts.LANGUAGE_CODE,
            Telephony.CellBroadcasts.MESSAGE_BODY,
            Telephony.CellBroadcasts.DELIVERY_TIME,
            Telephony.CellBroadcasts.MESSAGE_READ,
    };

    private static final int COLUMN_V1_GEOGRAPHICAL_SCOPE   = 0;
    private static final int COLUMN_V1_SERIAL_NUMBER        = 1;
    private static final int COLUMN_V1_MESSAGE_CODE         = 2;
    private static final int COLUMN_V1_MESSAGE_IDENTIFIER   = 3;
    private static final int COLUMN_V1_LANGUAGE_CODE        = 4;
    private static final int COLUMN_V1_MESSAGE_BODY         = 5;
    private static final int COLUMN_V1_DELIVERY_TIME        = 6;
    private static final int COLUMN_V1_MESSAGE_READ         = 7;

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == newVersion) {
            return;
        }
        // always log database upgrade
        log("Upgrading DB from version " + oldVersion + " to " + newVersion);

        // Upgrade from V1 to V10
        if (oldVersion == 1) {
            db.beginTransaction();
            try {
                // Step 1: rename original table
                db.execSQL("DROP TABLE IF EXISTS " + TEMP_TABLE_NAME);
                db.execSQL("ALTER TABLE " + CellbroadcastDefine.TABLE_NAME + " RENAME TO " + TEMP_TABLE_NAME);

                // Step 2: create new table and indices
                onCreate(db);

                // Step 3: copy each message into the new table
                Cursor cursor = db.query(TEMP_TABLE_NAME, COLUMNS_V1, null, null, null, null,
                        null);
                try {
                    while (cursor.moveToNext()) {
                        upgradeMessageV1ToV2(db, cursor);
                    }
                } finally {
                    cursor.close();
                }

                // Step 4: drop the original table and commit transaction
                db.execSQL("DROP TABLE " + TEMP_TABLE_NAME);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            oldVersion = 10;    // skip to version 10
        }

        // Note to OEMs: if you have customized the database schema since V1, you will need to
        // add your own code here to convert from your version to version 10.
        if (oldVersion < 10) {
            throw new SQLiteException("CellBroadcastDatabase doesn't know how to upgrade "
                    + " DB version " + oldVersion + " (customized by OEM?)");
        }

        if (oldVersion == 10) {
            createDeliveryTimeIndex(db);
            oldVersion++;
        }

        if (oldVersion < 13) {
            Log.d(TAG, "enter onUpgrade(), before onSprdUpgrade()");
            upgradeToLollipop(db, oldVersion, newVersion);
            upgradeToMarshmallow(db);
        }

        if (oldVersion == 13) {
            upgradeToMarshmallow(db);
        }
    }

    private void upgradeToLollipop(SQLiteDatabase db, int oldVersion, int newVersion){

        if (oldVersion == newVersion) {
            Log.e(TAG, "onSprdUpgrade(), the oldVersion equals to newVersion,doNothing");
            return;
        }
        Log.d(TAG,"enter onSprdUpgrade(),update DB from version " + oldVersion + " to " + newVersion);
        //make the updating databases operations
        //add column of table
        String BROADCASTS_ADD_COLUMN_SUB_ID = "ALTER TABLE " + CellbroadcastDefine.TABLE_NAME
                + " ADD sub_id INTEGER DEFAULT -1";
        String CHANNEL_ADD_COLUMN_SUB_ID = "ALTER TABLE " + ChannelTableDefine.TABLE_NAME
                + " ADD sub_id INTEGER DEFAULT -1";
        String COMMON_SETTING_ADD_COLUMN_SUB_ID = "ALTER TABLE " + "common_setting"
                + " ADD sub_id INTEGER DEFAULT -1";
        String oldColumn = "phone_id";
        String newColumn = "sub_id";

        if (checkIfTableExist(db, TABLE_NAME) && checkIfTableExist(db, "channel")) {
            Log.d(TAG, "all table is exist.");
            db.execSQL(BROADCASTS_ADD_COLUMN_SUB_ID);
            db.execSQL(CHANNEL_ADD_COLUMN_SUB_ID);

            // update the old_column' data to new_column' data
            onSyncData(db, TABLE_NAME, oldColumn, newColumn);
            onSyncData(db, "channel", oldColumn, newColumn);
        }
        if (checkIfTableExist(db, "common_setting")) {
            db.execSQL(COMMON_SETTING_ADD_COLUMN_SUB_ID);
            onSyncData(db,"common_setting",oldColumn,newColumn);
        }

    }

    private void onSyncData(SQLiteDatabase db,  String tableName, String oldColumn, String newColumn){
     // update the data
        String updateSql = "update "+ tableName + " set " + newColumn + " = " + oldColumn + " + 1;";
        Log.e(TAG, "enter onSprdSyncData(),the updateSql = " + updateSql);
        db.execSQL(updateSql);
    }

    private void upgradeToMarshmallow(SQLiteDatabase db){
        Log.d(TAG, "enter onUpgrade(), upgrade to Marshmallow.");
        String CHANNEL_ADD_COLUMN_SAVE = "ALTER TABLE " + ChannelTableDefine.TABLE_NAME
                + " ADD save INTEGER DEFAULT 1";
        String CHANNEL_ADD_COLUMN_MCC = "ALTER TABLE " + ChannelTableDefine.TABLE_NAME
                + " ADD mcc INTEGER";
        String CHANNEL_ADD_COLUMN_MNC = "ALTER TABLE " + ChannelTableDefine.TABLE_NAME
                + " ADD mnc INTEGER";
        String CHANNEL_ADD_COLUMN_VIBRATE = "ALTER TABLE " + ChannelTableDefine.TABLE_NAME
                + " ADD vibrate INTEGER";
        String CHANNEL_ADD_COLUMN_URI = "ALTER TABLE " + ChannelTableDefine.TABLE_NAME
                + " ADD sound_uri TEXT";
        String CHANNEL_ADD_NOTIFY = "ALTER TABLE " + ChannelTableDefine.TABLE_NAME
                + " ADD notification INTEGER DEFAULT 1";

        String COMMSETING_ADD_RINGTONEURI = "ALTER TABLE " + CommonSettingTableDefine.TABLE_NAME
                + " ADD ring_url TEXT";
        // DEFAULT content://settings/system/notification_sound
        if (checkIfTableExist(db, "channel")) {
            db.execSQL(CHANNEL_ADD_COLUMN_SAVE);
            db.execSQL(CHANNEL_ADD_COLUMN_MCC);
            db.execSQL(CHANNEL_ADD_COLUMN_MNC);
            db.execSQL(CHANNEL_ADD_COLUMN_VIBRATE);
            db.execSQL(CHANNEL_ADD_COLUMN_URI);
            db.execSQL(CHANNEL_ADD_NOTIFY);
        }
        if (checkIfTableExist(db, "common_setting")) {
            db.execSQL(COMMSETING_ADD_RINGTONEURI);
        }

        db.execSQL(CREATE_LANG_MAP_TABLE);
        db.execSQL(CREATE_MNC_MCC_TABLE);
        db.execSQL(CREATE_LANG_NAME_TABLE);
        db.execSQL(CREATE_LANG_VIEW);
        importLangInfoTablesFromXml(db,R.xml.mnc_mcc);
        importLangInfoTablesFromXml(db,R.xml.lang_name);
        importLangInfoTablesFromXml(db,R.xml.lang_map);
    }

    private boolean checkIfTableExist(SQLiteDatabase db, String tableName) {
        boolean result = false;
        if (tableName == null) {
            return false;
        }
        Cursor cursor = null;
        try {
            String sql = "select * from "+ tableName.trim();
            cursor = db.rawQuery(sql, null);
            Log.d(TAG, "check table:"+tableName+" by use SQL:"+ sql+" and the cursor is:"
                        +(cursor==null?"null":"not null"));
            if (cursor != null) {
                Log.d(TAG, "table:"+tableName+" exist.");
                result = true;
            }
        } catch (Exception e) {
            result = false;
            Log.e(TAG, "query failed."+tableName+" is not exist.");
        }
        return result;
    }

    /**
     * Upgrades a single broadcast message from version 1 to version 2.
     */
    private static void upgradeMessageV1ToV2(SQLiteDatabase db, Cursor cursor) {
        int geographicalScope = cursor.getInt(COLUMN_V1_GEOGRAPHICAL_SCOPE);
        int updateNumber = cursor.getInt(COLUMN_V1_SERIAL_NUMBER);
        int messageCode = cursor.getInt(COLUMN_V1_MESSAGE_CODE);
        int messageId = cursor.getInt(COLUMN_V1_MESSAGE_IDENTIFIER);
        String languageCode = cursor.getString(COLUMN_V1_LANGUAGE_CODE);
        String messageBody = cursor.getString(COLUMN_V1_MESSAGE_BODY);
        long deliveryTime = cursor.getLong(COLUMN_V1_DELIVERY_TIME);
        boolean isRead = (cursor.getInt(COLUMN_V1_MESSAGE_READ) != 0);

        int serialNumber = ((geographicalScope & 0x03) << 14)
                | ((messageCode & 0x3ff) << 4) | (updateNumber & 0x0f);

        ContentValues cv = new ContentValues(16);
        cv.put(Telephony.CellBroadcasts.GEOGRAPHICAL_SCOPE, geographicalScope);
        cv.put(Telephony.CellBroadcasts.SERIAL_NUMBER, serialNumber);
        cv.put(Telephony.CellBroadcasts.SERVICE_CATEGORY, messageId);
        cv.put(Telephony.CellBroadcasts.LANGUAGE_CODE, languageCode);
        cv.put(Telephony.CellBroadcasts.MESSAGE_BODY, messageBody);
        cv.put(Telephony.CellBroadcasts.DELIVERY_TIME, deliveryTime);
        cv.put(Telephony.CellBroadcasts.MESSAGE_READ, isRead);
        cv.put(Telephony.CellBroadcasts.MESSAGE_FORMAT, SmsCbMessage.MESSAGE_FORMAT_3GPP);

        int etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_UNKNOWN;
        int cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_UNKNOWN;
        int cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_UNKNOWN;
        int cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_UNKNOWN;
        int cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_UNKNOWN;
        switch (messageId) {
            case SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_WARNING:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE;
                break;

            case SmsCbConstants.MESSAGE_ID_ETWS_TSUNAMI_WARNING:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_TSUNAMI;
                break;

            case SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_AND_TSUNAMI_WARNING:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE_AND_TSUNAMI;
                break;

            case SmsCbConstants.MESSAGE_ID_ETWS_TEST_MESSAGE:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_TEST_MESSAGE;
                break;

            case SmsCbConstants.MESSAGE_ID_ETWS_OTHER_EMERGENCY_TYPE:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_OTHER_EMERGENCY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_PRESIDENTIAL_LEVEL:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_PRESIDENTIAL_LEVEL_ALERT;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_OBSERVED:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_EXTREME_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_EXTREME;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_LIKELY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_EXTREME_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_EXTREME;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_OBSERVED:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_EXTREME;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_EXPECTED;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_LIKELY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_EXTREME;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_EXPECTED;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_OBSERVED:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_SEVERE;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_LIKELY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_SEVERE;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_OBSERVED:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_SEVERE;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_EXPECTED;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_LIKELY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_SEVERE;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_EXPECTED;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_CHILD_ABDUCTION_EMERGENCY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_CHILD_ABDUCTION_EMERGENCY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_REQUIRED_MONTHLY_TEST:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_REQUIRED_MONTHLY_TEST;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXERCISE:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_CMAS_EXERCISE;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_OPERATOR_DEFINED_USE:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_OPERATOR_DEFINED_USE;
                break;
        }

        if (etwsWarningType != SmsCbEtwsInfo.ETWS_WARNING_TYPE_UNKNOWN
                || cmasMessageClass != SmsCbCmasInfo.CMAS_CLASS_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.MESSAGE_PRIORITY,
                    SmsCbMessage.MESSAGE_PRIORITY_EMERGENCY);
        } else {
            cv.put(Telephony.CellBroadcasts.MESSAGE_PRIORITY,
                    SmsCbMessage.MESSAGE_PRIORITY_NORMAL);
        }

        if (etwsWarningType != SmsCbEtwsInfo.ETWS_WARNING_TYPE_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.ETWS_WARNING_TYPE, etwsWarningType);
        }

        if (cmasMessageClass != SmsCbCmasInfo.CMAS_CLASS_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.CMAS_MESSAGE_CLASS, cmasMessageClass);
        }

        if (cmasSeverity != SmsCbCmasInfo.CMAS_SEVERITY_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.CMAS_SEVERITY, cmasSeverity);
        }

        if (cmasUrgency != SmsCbCmasInfo.CMAS_URGENCY_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.CMAS_URGENCY, cmasUrgency);
        }

        if (cmasCertainty != SmsCbCmasInfo.CMAS_CERTAINTY_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.CMAS_CERTAINTY, cmasCertainty);
        }

        db.insert(CellbroadcastDefine.TABLE_NAME, null, cv);
    }

    public void importLangInfoTablesFromXml(SQLiteDatabase db, int resourceId) {

        Log.d(TAG, "CREATE_MNC_MCC_TABLE start  resourceId = "
                + resourceId + "\n");
        Resources res = mContext.getResources();
        XmlResourceParser xmlp = res.getXml(resourceId);
        Log.d(TAG, "CREATE_MNC_MCC_TABLE enter  resourceId = "
                + resourceId + "\n");
        try {
            db.beginTransaction();
            while (xmlp.getEventType() != XmlResourceParser.END_DOCUMENT) {
                if (xmlp.getEventType() == XmlResourceParser.START_TAG) {
                    String tagName = xmlp.getName();
                    Log.d(TAG, "tagName = " + tagName);
                    if (tagName.endsWith("mncmcc")) {
                        Log.d(TAG, "tagName.endsWith(mncmcc)");
                        int mnc = Integer.parseInt(xmlp.getAttributeValue(null,
                                "mnc"));
                        int mcc = Integer.parseInt(xmlp.getAttributeValue(null,
                                "mcc"));
                        int mnc_mcc_id = Integer.parseInt(xmlp.getAttributeValue(null,
                                "mncmcc_id"));
                        Log.d(TAG, "out: mnc = " + mnc
                                + " \n  mcc = " + mcc + "");
                        ContentValues value = new ContentValues();

                        value.put(MncMccTableDefine.MNC, mnc);
                        value.put(MncMccTableDefine.MCC, mcc);
                        value.put(MncMccTableDefine.MNCMCC_ID, mnc_mcc_id);
                        db.insert(MncMccTableDefine.MNC_MCC_TABLE_NAME, "", value);

                    } else if (tagName.endsWith("langname")) {
                        Log.d(TAG, "tagName.endsWith(langname)");
                        //String description = xmlp.nextText();
                        String description = xmlp.getAttributeValue(null, "description");
                        int lang_name_id = Integer.parseInt(xmlp.getAttributeValue(null,
                                "_id"));

                        Log.d(TAG, "out : description = " + description + "");
                        ContentValues value = new ContentValues();

                        value.put(LangNameTableDefine.DESCRIPTION, description);
                        value.put(LangNameTableDefine.LANG_NAME_ID, lang_name_id);
                        db.insert(LangNameTableDefine.LANG_NAME_TABLE_NAME, "",
                                value);

                    } else if (tagName.endsWith("langmap")) {
                        Log.d(TAG, "tagName.endsWith(langmap)");

                        int lang_id = Integer.parseInt(xmlp.getAttributeValue(
                                null, "lang_id"));
                        int mnc_mcc_id = Integer.parseInt(xmlp
                                .getAttributeValue(null, "mnc_mcc_id"));
                        int show = Integer.parseInt(xmlp.getAttributeValue(
                                null, "show"));
                        int enable = Integer.parseInt(xmlp.getAttributeValue(
                                null, "enable"));
                        Log.d(TAG, "out : lang_id = " + lang_id
                                + " \n  mnc_mcc_id = " + mnc_mcc_id + "");
                        ContentValues value = new ContentValues();

                        value.put(LangMapTableDefine.LANG_ID, lang_id);
                        value.put(LangMapTableDefine.MNC_MCC_ID, mnc_mcc_id);
                        value.put(LangMapTableDefine.SHOW, show);
                        value.put(LangMapTableDefine.ENABLE, enable);
                        value.put(LangMapTableDefine.SUBID, -1);
                        db.insert(LangMapTableDefine.LANG_MAP_TABLE_NAME, "",
                                value);

                    }
                }
                xmlp.next();
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
