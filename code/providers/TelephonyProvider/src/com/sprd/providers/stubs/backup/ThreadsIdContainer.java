
package com.sprd.providers.stubs.backup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Telephony.CanonicalAddressesColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.ThreadsColumns;

import com.google.android.mms.pdu.PduHeaders;
//import com.google.android.mms.pdu.SingleDiffApks;

import com.android.providers.telephony.MmsSmsDatabaseHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import android.provider.BaseColumns;
import android.util.Log;

public class ThreadsIdContainer {

    private SQLiteDatabase db;
    private static String TAG = "ThreadsIdContainer";
    private Context mContext;
    static final String TABLE_THREADS = "threads";
    static final String TABLE_PDU = "pdu";
    static final String TABLE_SMS = "sms";
    static final String TABLE_ADRESS = "canonical_addresses";

    private static ThreadsIdContainer sThreadsIdContainer;

    /**
     * @param pp
     */
    private ThreadsIdContainer(Context context) {
        mContext = context;
        db = MmsSmsDatabaseHelper.getInstance(context).getWritableDatabase();
    }

    public static synchronized ThreadsIdContainer getInstance(Context context) {
        if (sThreadsIdContainer == null) {
            sThreadsIdContainer = new ThreadsIdContainer(context);
            //SingleDiffApks.get(context).registerThreadsIdContainer(sThreadsIdContainer);
        }
        return sThreadsIdContainer;
    }

    private long[] getSortedSet(Set<Long> numbers) {
        int size = numbers.size();
        long[] result = new long[size];
        int i = 0;

        for (Long number : numbers) {
            result[i++] = number;
        }

        if (size > 1) {
            Arrays.sort(result);
        }

        return result;
    }

    private String getSpaceSeparatedNumbers(long[] numbers) {
        int size = numbers.length;
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < size; i++) {
            if (i != 0) {
                buffer.append(' ');
            }
            buffer.append(numbers[i]);
        }
        return buffer.toString();
    }

    private Set<Long> getAddressIds(List<String> addresses) {
        Set<Long> result = new HashSet<Long>(addresses.size());
        for (String address : addresses) {
            if (!PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.equals(address)) {
                long id = getSingleAddressId(address);
                if (id != -1L) {
                    result.add(id);
                } else {
                    Log.v(TAG,
                            "getAddressIds: address ID not found for " + address);
                }
            }
        }
        return result;
    }

    private static final String[] ID_PROJECTION = {BaseColumns._ID};

    private long getSingleAddressId(String address) {
        boolean isEmail = Mms.isEmailAddress(address);
        boolean isPhoneNumber = Mms.isPhoneNumber(address);

        String refinedAddress = isEmail ? address.toLowerCase() : address;

        String selection = "address=?";
        String[] selectionArgs;
        long retVal = -1L;

        if (!isPhoneNumber) {
            selectionArgs = new String[]{refinedAddress};
        } else {
            boolean mUseStrictPhoneNumberComparation = mContext.getResources().getBoolean(
                    com.android.internal.R.bool.config_use_strict_phone_number_comparation);
            selection += " OR " + String.format(Locale.ENGLISH, "PHONE_NUMBERS_EQUAL(address, ?, %d)",
                    (mUseStrictPhoneNumberComparation ? 1 : 0));
            selectionArgs = new String[]{refinedAddress, refinedAddress};
        }

        Cursor cursor = null;

        try {
            cursor = db.query(
                    "canonical_addresses", ID_PROJECTION,
                    selection, selectionArgs, null, null, null);
            Log.d(TAG, "getSingleAddressId: cursor count =" + cursor.getCount());
            if (cursor.getCount() == 0) {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(CanonicalAddressesColumns.ADDRESS, refinedAddress);
                retVal = db.insert("canonical_addresses",
                        CanonicalAddressesColumns.ADDRESS, contentValues);
                Log.d(TAG, "getSingleAddressId: retVal =" + retVal);
                return retVal;
            }

            if (cursor.moveToFirst()) {
                retVal = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return retVal;
    }

    synchronized public long getOrCreateThreadId(List<String> recipients, List<String> recipientNames) {
        Cursor cursor = null;
        try {
            cursor = getThreadId(recipients, recipientNames);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    private static final String THREAD_QUERY = "SELECT _id FROM threads " + "WHERE recipient_ids=?";

    private Cursor getThreadId(List<String> recipients, List<String> recipientNames) {
        Set<Long> addressIds = getAddressIds(recipients);
        String recipientIds = "";

        // optimize for size==1, which should be most of the cases
        if (addressIds.size() == 1) {
            for (Long addressId : addressIds) {
                recipientIds = Long.toString(addressId);
            }
        } else {
            recipientIds = getSpaceSeparatedNumbers(getSortedSet(addressIds));
        }

        Log.d(TAG, "getThreadId: recipientIds (selectionArgs) =" +
            /* recipientIds */"xxxxxxx");

        String[] selectionArgs = new String[]{recipientIds};

        Log.d(TAG, "getThreadId: recipientIds:" + recipientIds);
        Cursor cursor = db.rawQuery(THREAD_QUERY, selectionArgs);

        Log.d(TAG, "getThreadId: cursor count =" + cursor.getCount());
        if (cursor.getCount() == 0) {
            cursor.close();
            String addresses = "";
            for (int i = 0; i < recipients.size(); i++) {
                if (i == 0) {
                    addresses = recipients.get(0);
                } else {
                    if (recipients.get(i) != null) {
                        addresses += " " + recipients.get(i).replace(" ", "");
                    }
                }
            }

            String names = "";
            for (int i = 0; i < recipientNames.size(); i++) {
                if (i == 0) {
                    names = recipientNames.get(0);
                } else {
                    names += " " + recipientNames.get(i);
                }
            }
            insertThread(recipientIds, recipients.size(), addresses, names);
            cursor = db.rawQuery(THREAD_QUERY, selectionArgs);
        }

        if (cursor.getCount() > 1) {
            Log.w(TAG, "getThreadId: why is cursorCount=" + cursor.getCount());
        }

        return cursor;
    }

    private long insertThread(String recipientIds, int numberOfRecipients, String addresses,
                              String names) {
        ContentValues values = new ContentValues(4);
        long date = System.currentTimeMillis();
        values.put(ThreadsColumns.DATE, date - date % 1000);
        values.put(ThreadsColumns.RECIPIENT_IDS, recipientIds);
        if (numberOfRecipients > 1) {
            values.put(Threads.TYPE, Threads.BROADCAST_THREAD);
        }
        values.put(ThreadsColumns.MESSAGE_COUNT, 0);
        //values.put(ThreadsColumns.RECIPIENT_ADDRESSES, addresses);
        //values.put(ThreadsColumns.RECIPIENT_NAMES, names);

        long result = db.insert(TABLE_THREADS, null, values);
        Log.d(TAG, "insertThread: created new thread_id " + result +
                " for recipientIds " + recipientIds + " values=" + values);

        mContext.getContentResolver().notifyChange(MmsSms.CONTENT_URI, null);
        return result;
    }

}
