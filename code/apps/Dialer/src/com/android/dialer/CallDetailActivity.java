/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.dialer;

import android.Manifest.permission;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Contacts.Intents.Insert;
import android.provider.ContactsContract.Contacts;
import android.provider.VoicemailContract.Voicemails;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.CallUtil;
import com.android.dialer.calllog.CallDetailHistoryAdapter;
import com.android.dialer.calllog.CallLogAsyncTaskUtil.CallLogAsyncTaskListener;
import com.android.dialer.calllog.CallLogAsyncTaskUtil;
import com.android.dialer.calllog.CallTypeHelper;
import com.android.dialer.calllog.ContactInfo;
import com.android.dialer.calllog.ContactInfoHelper;
import com.android.dialer.calllog.PhoneAccountUtils;
import com.android.dialer.calllog.PhoneNumberDisplayUtil;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.util.PhoneNumberUtil;
import com.android.dialer.util.TelecomUtil;
import com.sprd.phone.common.utils.IpDialingUtils;

import java.util.List;

/**
 * Displays the details of a specific call log entry.
 * <p>
 * This activity can be either started with the URI of a single call log entry, or with the
 * {@link #EXTRA_CALL_LOG_IDS} extra to specify a group of call log entries.
 */
public class CallDetailActivity extends Activity
        implements MenuItem.OnMenuItemClickListener {
    private static final String TAG = "CallDetail";

     /** A long array extra containing ids of call log entries to display. */
    public static final String EXTRA_CALL_LOG_IDS = "EXTRA_CALL_LOG_IDS";
    /** If we are started with a voicemail, we'll find the uri to play with this extra. */
    public static final String EXTRA_VOICEMAIL_URI = "EXTRA_VOICEMAIL_URI";
    /** If the activity was triggered from a notification. */
    public static final String EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION";

    public static final String VOICEMAIL_FRAGMENT_TAG = "voicemail_fragment";
    public boolean isVolte;
    // SPRD: modify for bug533902
    public boolean mVolteServiceEnable = false;
    // SPRD: Add for bug 542979
    private boolean mIsSaveInstanceState = false;
    // SPRD: Add for bug 542979
    private boolean mIsForeground = false;

    private CallLogAsyncTaskListener mCallLogAsyncTaskListener = new CallLogAsyncTaskListener() {
        @Override
        public void onDeleteCall() {
            finish();
        }

        @Override
        public void onDeleteVoicemail() {
            finish();
        }

        @Override
        public void onGetCallDetails(PhoneCallDetails[] details) {
            // SPRD: modify for bug495792
            //if (details == null) {
            if (details == null || details.length == 0) {
                // Somewhere went wrong: we're going to bail out and show error to users.
                Toast.makeText(mContext, R.string.toast_call_detail_error,
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // We know that all calls are from the same number and the same contact, so pick the
            // first.
            PhoneCallDetails firstDetails = details[0];
            mNumber = TextUtils.isEmpty(firstDetails.number) ?
                    null : firstDetails.number.toString();
            final int numberPresentation = firstDetails.numberPresentation;
            final Uri contactUri = firstDetails.contactUri;
            final Uri photoUri = firstDetails.photoUri;
            final PhoneAccountHandle accountHandle = firstDetails.accountHandle;

            // Cache the details about the phone number.
            final boolean canPlaceCallsTo =
                    PhoneNumberUtil.canPlaceCallsTo(mNumber, numberPresentation);
            mIsVoicemailNumber =
                    PhoneNumberUtil.isVoicemailNumber(mContext, accountHandle, mNumber);
            final boolean isSipNumber = PhoneNumberUtil.isSipNumber(mNumber);

            final CharSequence callLocationOrType = getNumberTypeOrLocation(firstDetails);

            final CharSequence displayNumber = firstDetails.displayNumber;
            final String displayNumberStr = mBidiFormatter.unicodeWrap(
                    displayNumber.toString(), TextDirectionHeuristics.LTR);

            if (!TextUtils.isEmpty(firstDetails.name)) {
                mCallerName.setText(firstDetails.name);
                mCallerNumber.setText(callLocationOrType + " " + displayNumberStr);
                // SPRD: Should not show "Save Contact" option.
                mHasSavedInContacts = false;
            } else {
                mCallerName.setText(displayNumberStr);
                if (!TextUtils.isEmpty(callLocationOrType)) {
                    mCallerNumber.setText(callLocationOrType);
                    mCallerNumber.setVisibility(View.VISIBLE);
                } else {
                    mCallerNumber.setVisibility(View.GONE);
                }
                // SPRD: Should show "Save Contact" option.
                mHasSavedInContacts = true;
            }

            mCallButton.setVisibility(canPlaceCallsTo ? View.VISIBLE : View.GONE);

            String accountLabel = PhoneAccountUtils.getAccountLabel(mContext, accountHandle);
            if (!TextUtils.isEmpty(accountLabel)) {
                mAccountLabel.setText(accountLabel);
                mAccountLabel.setVisibility(View.VISIBLE);
            } else {
                mAccountLabel.setVisibility(View.GONE);
            }

            mHasEditNumberBeforeCallOption =
                    canPlaceCallsTo && !isSipNumber && !mIsVoicemailNumber;
            mHasReportMenuOption = mContactInfoHelper.canReportAsInvalid(
                    firstDetails.sourceType, firstDetails.objectId);
            invalidateOptionsMenu();

            ListView historyList = (ListView) findViewById(R.id.history);
            historyList.setAdapter(
                    new CallDetailHistoryAdapter(mContext, mInflater, mCallTypeHelper, details));

            String lookupKey = contactUri == null ? null
                    : ContactInfoHelper.getLookupKeyFromUri(contactUri);

            final boolean isBusiness = mContactInfoHelper.isBusiness(firstDetails.sourceType);

            final int contactType =
                    mIsVoicemailNumber ? ContactPhotoManager.TYPE_VOICEMAIL :
                    isBusiness ? ContactPhotoManager.TYPE_BUSINESS :
                    ContactPhotoManager.TYPE_DEFAULT;

            String nameForDefaultImage;
            if (TextUtils.isEmpty(firstDetails.name)) {
                nameForDefaultImage = firstDetails.displayNumber;
            } else {
                nameForDefaultImage = firstDetails.name.toString();
            }

            // SPRD: This action allows to call the number that places the call.
            if (canPlaceCallsTo) {
                configureCallButton(mIsVoicemailNumber);
            } else {
                disableCallButton();
            }
            loadContactPhotos(
                    contactUri, photoUri, nameForDefaultImage, lookupKey, contactType);
            findViewById(R.id.call_detail).setVisibility(View.VISIBLE);
        }

        /**
         * Determines the location geocode text for a call, or the phone number type
         * (if available).
         *
         * @param details The call details.
         * @return The phone number type or location.
         */
        private CharSequence getNumberTypeOrLocation(PhoneCallDetails details) {
            if (!TextUtils.isEmpty(details.name)) {
                return Phone.getTypeLabel(mResources, details.numberType,
                        details.numberLabel);
            } else {
                return details.geocode;
            }
        }
    };

    private Context mContext;
    private CallTypeHelper mCallTypeHelper;
    private QuickContactBadge mQuickContactBadge;
    private TextView mCallerName;
    private TextView mCallerNumber;
    private TextView mAccountLabel;
    private View mCallButton;
    private ContactInfoHelper mContactInfoHelper;

    protected String mNumber;
    private boolean mIsVoicemailNumber;
    private String mDefaultCountryIso;

    /* package */ LayoutInflater mInflater;
    /* package */ Resources mResources;
    /** Helper to load contact photos. */
    private ContactPhotoManager mContactPhotoManager;

    private Uri mVoicemailUri;
    private BidiFormatter mBidiFormatter = BidiFormatter.getInstance();

    /** Whether we should show "edit number before call" in the options menu. */
    private boolean mHasEditNumberBeforeCallOption;
    private boolean mHasReportMenuOption;
    /** SPRD: Whether we should show "add contact". */
    private boolean mHasSavedInContacts;
    private static final int CALL_PHONE_PERMISSION_REQUEST_CODE = 1;
    // SPRD: add for volte
    ImageView videoCallIcon = null;
    private TelephonyManager telephonyManager;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = this;

        setContentView(R.layout.call_detail);

        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mResources = getResources();

        mCallTypeHelper = new CallTypeHelper(getResources());
        // SPRD: add for volte
        telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        mVoicemailUri = getIntent().getParcelableExtra(EXTRA_VOICEMAIL_URI);

        mQuickContactBadge = (QuickContactBadge) findViewById(R.id.quick_contact_photo);
        mQuickContactBadge.setOverlay(null);
        mQuickContactBadge.setPrioritizedMimeType(Phone.CONTENT_ITEM_TYPE);
        mCallerName = (TextView) findViewById(R.id.caller_name);
        mCallerNumber = (TextView) findViewById(R.id.caller_number);
        mAccountLabel = (TextView) findViewById(R.id.phone_account_label);
        mDefaultCountryIso = GeoUtil.getCurrentCountryIso(this);
        mContactPhotoManager = ContactPhotoManager.getInstance(this);

        mCallButton = (View) findViewById(R.id.call_back_button);
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // SPRD: add for bug534275,534535
                DialerUtils.showCallNotSwitchToast(mContext, mNumber);
                mContext.startActivity(IntentUtil.getCallIntent(mNumber));
            }
        });

        mContactInfoHelper = new ContactInfoHelper(this, GeoUtil.getCurrentCountryIso(this));
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            closeSystemDialogs();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // SPRD: Add for bug542979
        mIsSaveInstanceState = false;
        // SPRD: Add for bug561487
        mIsForeground = true;
        // SPRD: modify for bug 528742 to check phone permission
        getCallDetailsAndListenVolteState();
    }

    /**
     * SPRD: add for volte @{
     */
    @Override
    public void onPause() {
        if (telephonyManager.isVolteCallEnabled()) {
            telephonyManager.listen(mLtePhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        // SPRD: Add for bug561487
        mIsForeground = false;
        super.onPause();
    }
    /** @} */

    /**
     * SPRD: add for volte @{
     */
    private final PhoneStateListener mLtePhoneStateListener = new PhoneStateListener() {
        public void onVoLteServiceStateChanged(VoLteServiceState serviceState) {
            // SPRD: modify for bug533902
            mVolteServiceEnable = (serviceState.getSrvccState() == VoLteServiceState.IMS_REG_STATE_REGISTERED);
            if (videoCallIcon != null && telephonyManager.isVolteCallEnabled()) {
                if (mVolteServiceEnable) {
                    videoCallIcon.setVisibility(View.VISIBLE);
                } else {
                    videoCallIcon.setVisibility(View.GONE);
                }
            }
        }
    };
    /** @} */

    /* SPRD: modify for bug 528742 to check phone permission @{ */
    public void getCallDetailsAndListenVolteState() {
         /* SPRD : modify for bug507963 @{*/
         if (checkSelfPermission(permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
             requestPermissions(new String[] { permission.CALL_PHONE },
                     CALL_PHONE_PERMISSION_REQUEST_CODE);
         } else {
             CallLogAsyncTaskUtil.getCallDetails(this, getCallLogEntryUris(),
                     mCallLogAsyncTaskListener);
             /* SPRD: add for volte @{*/
             if (telephonyManager.isVolteCallEnabled()) {
                 isVolte = true;
                 telephonyManager.listen(
                        mLtePhoneStateListener, PhoneStateListener.LISTEN_VOLTE_STATE);
             } else {
                 isVolte = false;
             }
             /* @} */
        }
        /* @} */
    }
    /* @} */

    /**
    * SPRD: modify for bug507963 @{
    */
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        switch (requestCode) {
        case CALL_PHONE_PERMISSION_REQUEST_CODE:
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CallLogAsyncTaskUtil.getCallDetails(this, getCallLogEntryUris(),
                        mCallLogAsyncTaskListener);
                /* SPRD: modify for bug 528742 to check phone permission @{ */
                if (telephonyManager.isVolteCallEnabled()) {
                    isVolte = true;
                    telephonyManager.listen(
                            mLtePhoneStateListener, PhoneStateListener.LISTEN_VOLTE_STATE);
                } else {
                    isVolte = false;
                }
                /* @} */
            } else {
                finish();
            }
            break;
        default:
            break;
        }
    }
    /** @} */

    private boolean hasVoicemail() {
        return mVoicemailUri != null;
    }

    /**
     * Returns the list of URIs to show.
     * <p>
     * There are two ways the URIs can be provided to the activity: as the data on the intent, or as
     * a list of ids in the call log added as an extra on the URI.
     * <p>
     * If both are available, the data on the intent takes precedence.
     */
    private Uri[] getCallLogEntryUris() {
        final Uri uri = getIntent().getData();
        if (uri != null) {
            // If there is a data on the intent, it takes precedence over the extra.
            return new Uri[]{ uri };
        }
        final long[] ids = getIntent().getLongArrayExtra(EXTRA_CALL_LOG_IDS);
        final int numIds = ids == null ? 0 : ids.length;
        final Uri[] uris = new Uri[numIds];
        for (int index = 0; index < numIds; ++index) {
            uris[index] = ContentUris.withAppendedId(
                    TelecomUtil.getCallLogUri(CallDetailActivity.this), ids[index]);
        }
        return uris;
    }

    /** Load the contact photos and places them in the corresponding views. */
    private void loadContactPhotos(Uri contactUri, Uri photoUri, String displayName,
            String lookupKey, int contactType) {

        final DefaultImageRequest request = new DefaultImageRequest(displayName, lookupKey,
                contactType, true /* isCircular */);

        mQuickContactBadge.assignContactUri(contactUri);
        mQuickContactBadge.setContentDescription(
                mResources.getString(R.string.description_contact_details, displayName));

        mContactPhotoManager.loadDirectoryPhoto(mQuickContactBadge, photoUri,
                false /* darkTheme */, true /* isCircular */, request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.call_details_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // This action deletes all elements in the group from the call log.
        // We don't have this action for voicemails, because you can just use the trash button.
        menu.findItem(R.id.menu_remove_from_call_log)
                .setVisible(!hasVoicemail())
                .setOnMenuItemClickListener(this);
        menu.findItem(R.id.menu_edit_number_before_call)
                .setVisible(mHasEditNumberBeforeCallOption)
                .setOnMenuItemClickListener(this);
        menu.findItem(R.id.menu_trash)
                .setVisible(hasVoicemail())
                .setOnMenuItemClickListener(this);
        menu.findItem(R.id.menu_report)
                .setVisible(mHasReportMenuOption)
                .setOnMenuItemClickListener(this);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_remove_from_call_log:
                final StringBuilder callIds = new StringBuilder();
                for (Uri callUri : getCallLogEntryUris()) {
                    if (callIds.length() != 0) {
                        callIds.append(",");
                    }
                    callIds.append(ContentUris.parseId(callUri));
                }
                CallLogAsyncTaskUtil.deleteCalls(
                        this, callIds.toString(), mCallLogAsyncTaskListener);
                break;
            case R.id.menu_edit_number_before_call:
                startActivity(new Intent(Intent.ACTION_DIAL, CallUtil.getCallUri(mNumber)));
                break;
            case R.id.menu_trash:
                CallLogAsyncTaskUtil.deleteVoicemail(
                        this, mVoicemailUri, mCallLogAsyncTaskListener);
                break;
        }
        return true;
    }

    private void closeSystemDialogs() {
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    // --------------------------------- SPRD --------------------------------------

    /**
     * SPRD: Configures the call button area using the given entry.
     */
    private void configureCallButton(boolean isVoiceMailNumber) {
        View convertView = findViewById(R.id.call_detail_action);
        convertView.setVisibility(View.VISIBLE);

        ImageView ipCallIcon = null;
        ImageView videoCallIcon = null;
        ImageView smsIcon = null;
        ImageView addContactIcon = null;

        ipCallIcon = (ImageView) convertView.findViewById(R.id.ip_call_icon);
        videoCallIcon = (ImageView) convertView.findViewById(R.id.video_call_icon);
        smsIcon = (ImageView) convertView.findViewById(R.id.message_icon);
        addContactIcon = (ImageView) convertView.findViewById(R.id.add_contact_icon);

        /* SPRD: add the ip dial FR && modify for feature switch @{ */
        CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        boolean isIpEnabled = false;
        if (configManager.getConfig() != null) {
            isIpEnabled = configManager.getConfig().getBoolean(
                    CarrierConfigManager.KEY_FEATURE_IP_DIAL_ENABLED_BOOL);
        }
        if (mNumber != null && isIpEnabled) {
            IpDialingUtils ipUtils = new IpDialingUtils(this);
            String voiceNumber = mNumber;
            for (String prefix : IpDialingUtils.EXCLUDE_PREFIX) {
                if (voiceNumber.startsWith(prefix)) {
                    voiceNumber = voiceNumber.substring(prefix.length());
                    break;
                }
            }
            String ipPrefixNum = ipUtils.getIpDialNumber();
            if (!TextUtils.isEmpty(ipPrefixNum)) {
                voiceNumber = ipPrefixNum + voiceNumber;
                final Intent intent = CallUtil.getCallIntent(voiceNumber);
                intent.putExtra(IpDialingUtils.EXTRA_IS_IP_DIAL, true);
                intent.putExtra(IpDialingUtils.EXTRA_IP_PRFIX_NUM, ipPrefixNum);
                ipCallIcon.setTag(intent);
            } else {
                final Intent ipListIntent = new Intent();
                ipListIntent.setAction("android.intent.action.MAIN");
                ipListIntent.addCategory(Intent.CATEGORY_DEVELOPMENT_PREFERENCE);
                ipListIntent.setComponent(new ComponentName("com.android.phone",
                        "com.sprd.phone.settings.ipdial.IpNumberListActivity"));
                ipCallIcon.setTag(ipListIntent);
            }
            ipCallIcon.setVisibility(View.VISIBLE);
            ipCallIcon.setOnClickListener(mActionListener);
        } else {
            ipCallIcon.setVisibility(View.GONE);
        }
        /* @} */

        //video call
        videoCallIcon.setTag(CallUtil.getVideoCallIntent(mNumber, ""));
        videoCallIcon.setVisibility(View.VISIBLE);
        videoCallIcon.setOnClickListener(mActionListener);

        //send sms
        smsIcon.setTag(new Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", mNumber, null)));
        smsIcon.setVisibility(View.VISIBLE);
        smsIcon.setOnClickListener(mActionListener);

        // add contact
        if (mHasSavedInContacts && (!TextUtils.isEmpty(mNumber)) && !mIsVoicemailNumber) {
            Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
            intent.setType(Contacts.CONTENT_ITEM_TYPE);
            intent.putExtra(Insert.PHONE, mNumber);
            addContactIcon.setTag(intent);
            addContactIcon.setVisibility(View.VISIBLE);
            addContactIcon.setOnClickListener(mActionListener);
        } else {
            addContactIcon.setVisibility(View.GONE);
        }

        if (TelephonyManager.isSupportVT()
                // SPRD: add for bug533902
                && (!DialerUtils.is4GPhone() || mVolteServiceEnable)) {
            videoCallIcon.setVisibility(View.VISIBLE);
        } else {
            videoCallIcon.setVisibility(View.GONE);
        }
    }

    /**
     * SPRD: Disables the call button area, e.g., for private numbers.
     */
    private void disableCallButton() {
        findViewById(R.id.call_detail_action).setVisibility(View.GONE);
    }

    private final View.OnClickListener mActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivityForAction(view);
        }
    };

    private void startActivityForAction(View view) {
        final Intent intent = (Intent) view.getTag();
        if (intent != null) {
            DialerUtils.startActivityWithErrorToast(CallDetailActivity.this, intent);
        }
    }

    /* SPRD: add for bug534277 @{ */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                /* SPRD: add for bug542979 & 561487 @{ */
                if (!mIsSaveInstanceState && mIsForeground) {
                    onBackPressed();
                }
                /* @} */
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /* @} */

    /**
     * SPRD: add for bug542979 @{
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mIsSaveInstanceState = true;
        super.onSaveInstanceState(outState);
    }
    /** @} */
}
