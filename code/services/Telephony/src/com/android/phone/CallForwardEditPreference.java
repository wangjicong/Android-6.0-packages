package com.android.phone;

import com.android.ims.ImsCallForwardInfo;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;

/* SPRD: function caching edited CF number support. @{ */
import android.telephony.PhoneNumberUtils;
import android.content.SharedPreferences;
import android.widget.EditText;
/* @} */

/* SPRD: function call barring support. @{ */
import com.sprd.phone.settings.callbarring.TimeConsumingPreferenceListener;
/* @} */

public class CallForwardEditPreference extends EditPhoneNumberPreference {
    private static final String LOG_TAG = "CallForwardEditPreference";
    private static final boolean DBG = true;

    private static final String SRC_TAGS[]       = {"{0}"};
    private CharSequence mSummaryOnTemplate;
    /**
     * Remembers which button was clicked by a user. If no button is clicked yet, this should have
     * {@link DialogInterface#BUTTON_NEGATIVE}, meaning "cancel".
     *
     * TODO: consider removing this variable and having getButtonClicked() in
     * EditPhoneNumberPreference instead.
     */
    private int mButtonClicked;
    private int mServiceClass;
    private MyHandler mHandler = new MyHandler();
    int reason;
    private Phone mPhone;
    CallForwardInfo callForwardInfo;
    private TimeConsumingPreferenceListener mTcpListener;
    /* SPRD: add for callforward time @{ */
    ImsCallForwardInfo mImsCallForwardInfo;
    private int mStatus;
    /* @} */
    // SPRD: modify for bug501744
    private static final int VIDEO_CALL_FORWARD = 2;

    /* SPRD: function caching edited CF number support. @{ */
    private Context mContext;
    private SharedPreferences mPrefs;
    private int mPhoneId = 0;
    static final String PREF_PREFIX = "phonecallforward_";
    private EditPhoneNumberPreference.GetDefaultNumberListener mCallForwardListener;
    /* @} */


    public CallForwardEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        /* SPRD: function caching edited CF number support. @{ */
        mContext = context;
        mPrefs = mContext.getSharedPreferences(PREF_PREFIX + mPhoneId, mContext.MODE_WORLD_READABLE);
        /* @} */

        mSummaryOnTemplate = this.getSummaryOn();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.CallForwardEditPreference, 0, R.style.EditPhoneNumberPreference);
        mServiceClass = a.getInt(R.styleable.CallForwardEditPreference_serviceClass,
                CommandsInterface.SERVICE_CLASS_VOICE);
        reason = a.getInt(R.styleable.CallForwardEditPreference_reason,
                CommandsInterface.CF_REASON_UNCONDITIONAL);
        a.recycle();

        if (DBG) Log.d(LOG_TAG, "mServiceClass=" + mServiceClass + ", reason=" + reason);
    }

    public CallForwardEditPreference(Context context) {
        this(context, null);
    }

    void init(TimeConsumingPreferenceListener listener, boolean skipReading, Phone phone) {
        mPhone = phone;
        mTcpListener = listener;
        /* SPRD: function caching edited CF number support. @{ */
        mPhoneId = mPhone.getPhoneId();
        mCallForwardListener = new EditPhoneNumberPreference.GetDefaultNumberListener() {
            public String onGetDefaultNumber(EditPhoneNumberPreference preference) {
                /* SPRD: modify for bug 529388 to distinguish voideo number and voice number @{ */
                String number;
                if (GsmUmtsCallForwardOptions.checkVideoCallServiceClass(mServiceClass)) {
                    number = mPrefs.getString(PREF_PREFIX + "Video_" + mPhoneId + "_" + reason, "");
                } else {
                    number = mPrefs.getString(PREF_PREFIX + mPhoneId + "_" + reason, "");
                }
                /* @} */
                return number;
            }
        };
        /* @} */

        if (!skipReading) {
            /* SPRD: add for callforward time @{ */
            if (ImsManager.isVolteEnabledByPlatform(getContext())
                    && (reason == CommandsInterface.CF_REASON_UNCONDITIONAL
                                || mServiceClass == VIDEO_CALL_FORWARD)) {
                if (reason == CommandsInterface.CF_REASON_UNCONDITIONAL
                        && mServiceClass != VIDEO_CALL_FORWARD) {
                    int serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
                    mPhone.getCallForwardingOption(reason, serviceClass, null,
                            mHandler.obtainMessage(MyHandler.MESSAGE_GET_CFV,
                                    // unused in this case
                                    CommandsInterface.CF_ACTION_DISABLE,
                                    MyHandler.MESSAGE_GET_CFV, null));
                } else {
                    mPhone.getCallForwardingOption(reason, mServiceClass, null,
                            mHandler.obtainMessage(MyHandler.MESSAGE_GET_CFV,
                                    // unused in this case
                                    CommandsInterface.CF_ACTION_DISABLE,
                                    MyHandler.MESSAGE_GET_CFV, null));
                }
            } else {
            /* @} */
                 mPhone.getCallForwardingOption(reason,
                         mHandler.obtainMessage(MyHandler.MESSAGE_GET_CF,
                                 // unused in this case
                                 CommandsInterface.CF_ACTION_DISABLE,
                                 MyHandler.MESSAGE_GET_CF, null));
            }
            if (mTcpListener != null) {
                mTcpListener.onStarted(this, true);
            }
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        // default the button clicked to be the cancel button.
        mButtonClicked = DialogInterface.BUTTON_NEGATIVE;
        super.onBindDialogView(view);
        /* SPRD: function caching edited CF number support. @{ */
        EditText editText = getEditText();
        if (editText != null) {
            // see if there is a means to get a default number, set it accordingly.
            if (mCallForwardListener != null) {
                String defaultNumber = mCallForwardListener.onGetDefaultNumber(this);
                if (defaultNumber != null) {
                    editText.setText(defaultNumber);
                }
            }
        }
        /* @} */
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        mButtonClicked = which;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (DBG) Log.d(LOG_TAG, "mButtonClicked=" + mButtonClicked
                + ", positiveResult=" + positiveResult);
        // Ignore this event if the user clicked the cancel button, or if the dialog is dismissed
        // without any button being pressed (back button press or click event outside the dialog).
        if (this.mButtonClicked != DialogInterface.BUTTON_NEGATIVE) {
            int action = (isToggled() || (mButtonClicked == DialogInterface.BUTTON_POSITIVE)) ?
                    CommandsInterface.CF_ACTION_REGISTRATION :
                    CommandsInterface.CF_ACTION_DISABLE;
            int time = (reason != CommandsInterface.CF_REASON_NO_REPLY) ? 0 : 20;
            final String number = /** SPRD: Handle P&W in callforward number.
                                    * @orig getPhoneNumber();*/ getPhoneNumberWithPW();

            if (DBG) Log.d(LOG_TAG, "callForwardInfo=" + callForwardInfo);

            if (action == CommandsInterface.CF_ACTION_REGISTRATION
                    && callForwardInfo != null
                    && callForwardInfo.status == 1
                    && number.equals(callForwardInfo.number)) {
                // no change, do nothing
                if (DBG) Log.d(LOG_TAG, "no change, do nothing");
            } else {
                // set to network
                if (DBG) Log.d(LOG_TAG, "reason=" + reason + ", action=" + action
                        + ", number=" + number);

                // Display no forwarding number while we're waiting for
                // confirmation
                setSummaryOn("");

                // the interface of Phone.setCallForwardingOption has error:
                // should be action, reason...
                /* SPRD: add for callforward time @{ */
                if (ImsManager.isVolteEnabledByPlatform(getContext())) {
                    mPhone.setCallForwardingOption(action,
                            reason,
                            mServiceClass,
                            number,
                            time,
                            null,
                            mHandler.obtainMessage(MyHandler.MESSAGE_SET_CF,
                                    action,
                                    MyHandler.MESSAGE_SET_CF));
                } else {
                /* @} */
                    mPhone.setCallForwardingOption(action,
                            reason,
                            number,
                            time,
                            mHandler.obtainMessage(MyHandler.MESSAGE_SET_CF,
                                    action,
                                    MyHandler.MESSAGE_SET_CF));
                }

                if (mTcpListener != null) {
                    mTcpListener.onStarted(this, false);
                }
            }
        }
    }

    void handleCallForwardResult(CallForwardInfo cf) {
        callForwardInfo = cf;
        if (DBG) Log.d(LOG_TAG, "handleGetCFResponse done, callForwardInfo=" + callForwardInfo);

        setToggled(callForwardInfo.status == 1);
        setPhoneNumber(callForwardInfo.number);

        /* SPRD: function caching edited CF number support. @{ */
        String numberToCache = callForwardInfo.number;
        if (!TextUtils.isEmpty(callForwardInfo.number)) {
            numberToCache = PhoneNumberUtils.stripSeparators(callForwardInfo.number);
            saveStringPrefs(PREF_PREFIX + mPhoneId + "_" + reason, numberToCache);
        }
        /* @} */
    }

    void handleCallForwardVResult(ImsCallForwardInfo cf) {
        mImsCallForwardInfo = cf;
        if (DBG) Log.d(LOG_TAG, "handleGetCFVResponse done, mImsCallForwardInfo=" + mImsCallForwardInfo);

        /*SPRD:add newfuture for bug423432@{*/
        mStatus = mImsCallForwardInfo.mStatus;
        if (mImsCallForwardInfo.mRuleset != null) {
            if ((mImsCallForwardInfo.mServiceClass & CommandsInterface.SERVICE_CLASS_VOICE) != 0) {
                setToggled(true);
                setPhoneNumber(mImsCallForwardInfo.mNumber);
                mTcpListener.onEnableStatus(CallForwardEditPreference.this, 0);
            }
        } else {
            if ((mImsCallForwardInfo.mServiceClass & CommandsInterface.SERVICE_CLASS_VOICE) != 0) {
                setToggled(mImsCallForwardInfo.mStatus == 1);
                setPhoneNumber(mImsCallForwardInfo.mNumber);
                if (mImsCallForwardInfo.mCondition == 0) {
                    /* SPRD: add for bug531765 @{ */
                    mTcpListener.onUpdateTwinsPref((mImsCallForwardInfo.mStatus == 1),
                            mImsCallForwardInfo.mServiceClass,
                            reason, mImsCallForwardInfo.mNumber, null);
                }
            }

            if (GsmUmtsCallForwardOptions
                    .checkVideoCallServiceClass(mImsCallForwardInfo.mServiceClass)) {
                /* @} */
                mTcpListener.onUpdateTwinsPref((mImsCallForwardInfo.mStatus == 1),
                        mImsCallForwardInfo.mServiceClass,
                        reason, mImsCallForwardInfo.mNumber, null);
            }
        }

        if (!TextUtils.isEmpty(mImsCallForwardInfo.mNumber)) {
            if (GsmUmtsCallForwardOptions.checkVideoCallServiceClass(mImsCallForwardInfo.mServiceClass)) {
                if (PhoneNumberUtils.isUriNumber(mImsCallForwardInfo.mNumber)) {
                    saveStringPrefs(PREF_PREFIX + "Video_" + mPhoneId + "_" + reason,
                            mImsCallForwardInfo.mNumber);
                } else {
                    saveStringPrefs(PREF_PREFIX + "Video_" + mPhoneId + "_" + reason,
                            PhoneNumberUtils.stripSeparators(mImsCallForwardInfo.mNumber));
                }
            } else {
                if (PhoneNumberUtils.isUriNumber(mImsCallForwardInfo.mNumber)) {
                    saveStringPrefs(PREF_PREFIX + mPhoneId + "_" + reason, mImsCallForwardInfo.mNumber);
                } else {
                    saveStringPrefs(PREF_PREFIX + mPhoneId + "_" + reason,
                            PhoneNumberUtils.stripSeparators(mImsCallForwardInfo.mNumber));
                }
            }
        }
    }

    public void updateSummaryText() {
        if (isToggled()) {
            CharSequence summaryOn;
            final String number = getRawPhoneNumber();
            if (number != null && number.length() > 0) {
                // Wrap the number to preserve presentation in RTL languages.
                String wrappedNumber = BidiFormatter.getInstance().unicodeWrap(
                        number, TextDirectionHeuristics.LTR);
                String values[] = { wrappedNumber };
                summaryOn = TextUtils.replace(mSummaryOnTemplate, SRC_TAGS, values);
            } else {
                summaryOn = getContext().getString(R.string.sum_cfu_enabled_no_number);
            }

            setSummaryOn(summaryOn);
        }
    }

    // Message protocol:
    // what: get vs. set
    // arg1: action -- register vs. disable
    // arg2: get vs. set for the preceding request
    private class MyHandler extends Handler {
        static final int MESSAGE_GET_CF = 0;
        static final int MESSAGE_SET_CF = 1;
        static final int MESSAGE_GET_CFV = 2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_CF:
                    handleGetCFResponse(msg);
                    break;
                case MESSAGE_GET_CFV:
                    handleGetCFVResponse(msg);
                    break;
                case MESSAGE_SET_CF:
                    handleSetCFResponse(msg);
                    break;
            }
        }

        private void handleGetCFResponse(Message msg) {
            if (DBG) Log.d(LOG_TAG, "handleGetCFResponse: done");

            mTcpListener.onFinished(CallForwardEditPreference.this, msg.arg2 != MESSAGE_SET_CF);

            AsyncResult ar = (AsyncResult) msg.obj;

            callForwardInfo = null;
            if (ar.exception != null) {
                if (DBG) Log.d(LOG_TAG, "handleGetCFResponse: ar.exception=" + ar.exception);
                if (ar.exception instanceof CommandException) {
                    mTcpListener.onException(CallForwardEditPreference.this,
                            (CommandException) ar.exception);
                } else {
                    // Most likely an ImsException and we can't handle it the same way as
                    // a CommandException. The best we can do is to handle the exception
                    // the same way as mTcpListener.onException() does when it is not of type
                    // FDN_CHECK_FAILURE.
                    mTcpListener.onError(CallForwardEditPreference.this, EXCEPTION_ERROR);
                }
            } else {
                if (ar.userObj instanceof Throwable) {
                    mTcpListener.onError(CallForwardEditPreference.this, RESPONSE_ERROR);
                }
                CallForwardInfo cfInfoArray[] = (CallForwardInfo[]) ar.result;
                if (cfInfoArray.length == 0) {
                    if (DBG) Log.d(LOG_TAG, "handleGetCFResponse: cfInfoArray.length==0");
                    setEnabled(false);
                    mTcpListener.onError(CallForwardEditPreference.this, RESPONSE_ERROR);
                } else {
                    for (int i = 0, length = cfInfoArray.length; i < length; i++) {
                        if (DBG) Log.d(LOG_TAG, "handleGetCFResponse, cfInfoArray[" + i + "]="
                                + cfInfoArray[i]);
                        if (GsmUmtsCallForwardOptions
                                .checkServiceClassSupport(cfInfoArray[i].serviceClass)
                                && (cfInfoArray[i].serviceClass
                                            & CommandsInterface.SERVICE_CLASS_VOICE) != 0) {
                            // corresponding class
                            CallForwardInfo info = cfInfoArray[i];
                            handleCallForwardResult(info);

                            // Show an alert if we got a success response but
                            // with unexpected values.
                            // Currently only handle the fail-to-disable case
                            // since we haven't observed fail-to-enable.
                            if (msg.arg2 == MESSAGE_SET_CF &&
                                    msg.arg1 == CommandsInterface.CF_ACTION_DISABLE &&
                                    info.status == 1) {
                                CharSequence s;
                                switch (reason) {
                                    case CommandsInterface.CF_REASON_BUSY:
                                        s = getContext().getText(R.string.disable_cfb_forbidden);
                                        break;
                                    case CommandsInterface.CF_REASON_NO_REPLY:
                                        s = getContext().getText(R.string.disable_cfnry_forbidden);
                                        break;
                                    default: // not reachable
                                        s = getContext().getText(R.string.disable_cfnrc_forbidden);
                                }
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setNeutralButton(R.string.close_dialog, null);
                                builder.setTitle(getContext().getText(R.string.error_updating_title));
                                builder.setMessage(s);
                                builder.setCancelable(true);
                                builder.create().show();
                            }
                        }
                    }
                }
            }

            // Now whether or not we got a new number, reset our enabled
            // summary text since it may have been replaced by an empty
            // placeholder.
            updateSummaryText();
        }

        private void handleGetCFVResponse(Message msg) {
            if (DBG) Log.d(LOG_TAG, "handleGetCFVResponse: done");

            mTcpListener.onFinished(CallForwardEditPreference.this, msg.arg2 != MESSAGE_SET_CF);

            AsyncResult ar = (AsyncResult) msg.obj;

            mImsCallForwardInfo = null;
            if (ar.exception != null) {
                if (DBG) Log.d(LOG_TAG, "handleGetCFVResponse: ar.exception=" + ar.exception);
                if (ar.exception instanceof CommandException) {
                    mTcpListener.onException(CallForwardEditPreference.this,
                            (CommandException) ar.exception);
                } else {
                    // Most likely an ImsException and we can't handle it the same way as
                    // a CommandException. The best we can do is to handle the exception
                    // the same way as mTcpListener.onException() does when it is not of type
                    // FDN_CHECK_FAILURE.
                    mTcpListener.onError(CallForwardEditPreference.this, EXCEPTION_ERROR);
                }
            } else {
                if (ar.userObj instanceof Throwable) {
                    mTcpListener.onError(CallForwardEditPreference.this, RESPONSE_ERROR);
                }
                /* SPRD: add for bug495303 @{ */
                if (ar.result instanceof CallForwardInfo[]) {
                    if (DBG) Log.d(LOG_TAG,"handleGetCFVResponse: Ims Service is not started." +
                            "  ar.result =" + ar.result);
                    handleGetCFResponse(msg);
                    return;
                }
                /* @} */
                ImsCallForwardInfo cfInfoArray[] = (ImsCallForwardInfo[]) ar.result;
                if (cfInfoArray.length == 0) {
                    if (DBG) Log.d(LOG_TAG, "handleGetCFVResponse: cfInfoArray.length==0");
                    setEnabled(false);
                    mTcpListener.onError(CallForwardEditPreference.this, RESPONSE_ERROR);
                } else {
                    for (int i = 0, length = cfInfoArray.length; i < length; i++) {
                        if (DBG) Log.d(LOG_TAG, "handleGetCFVResponse, cfInfoArray[" + i + "]="
                                + cfInfoArray[i]);
                        if (GsmUmtsCallForwardOptions
                                .checkServiceClassSupport(cfInfoArray[i].mServiceClass)) {
                            // corresponding class
                            ImsCallForwardInfo info = cfInfoArray[i];
                            handleCallForwardVResult(info);

                            // Show an alert if we got a success response but
                            // with unexpected values.
                            // Currently only handle the fail-to-disable case
                            // since we haven't observed fail-to-enable.
                            if (msg.arg2 == MESSAGE_SET_CF &&
                                    msg.arg1 == CommandsInterface.CF_ACTION_DISABLE &&
                                    info.mStatus == 1) {
                                CharSequence s;
                                switch (reason) {
                                    case CommandsInterface.CF_REASON_BUSY:
                                        s = getContext().getText(R.string.disable_cfb_forbidden);
                                        break;
                                    case CommandsInterface.CF_REASON_NO_REPLY:
                                        s = getContext().getText(R.string.disable_cfnry_forbidden);
                                        break;
                                    default: // not reachable
                                        s = getContext().getText(R.string.disable_cfnrc_forbidden);
                                }
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setNeutralButton(R.string.close_dialog, null);
                                builder.setTitle(getContext().getText(R.string.error_updating_title));
                                builder.setMessage(s);
                                builder.setCancelable(true);
                                builder.create().show();
                            }
                        }
                    }
                }
            }

            // Now whether or not we got a new number, reset our enabled
            // summary text since it may have been replaced by an empty
            // placeholder.
            updateSummaryText();
        }

        private void handleSetCFResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                if (DBG) Log.d(LOG_TAG, "handleSetCFResponse: ar.exception=" + ar.exception);
                // setEnabled(false);
            }
            if (DBG) Log.d(LOG_TAG, "handleSetCFResponse: re get");
            /* SPRD: add for callforward time @{ */
            if (ImsManager.isVolteEnabledByPlatform(getContext())) {
                if (DBG)
                    Log.d(LOG_TAG, "mServiceClass ： " + mServiceClass);
                /* SPRD: add for bug531765 @{ */
                mPhone.getCallForwardingOption(reason, mServiceClass, null,
                        obtainMessage(MESSAGE_GET_CFV, msg.arg1, MESSAGE_SET_CF, ar.exception));
                /* @} */
            } else {
                if (DBG) Log.d(LOG_TAG, "else");
                mPhone.getCallForwardingOption(reason,
                        obtainMessage(MESSAGE_GET_CF, msg.arg1, MESSAGE_SET_CF, ar.exception));
            }
            /* @} */
        }
    }

    /* SPRD: function caching edited CF number support. @{ */
    void saveStringPrefs(String key, String value) {
        Log.w(LOG_TAG, "saveStringPrefs(" + key + ", " + value + ")");
        try {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(key, value);
            editor.apply();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception happen.");
        }
    }
    /* @} */

    public int getServiceClass() {
        return mServiceClass;
    }

    public int getReason() {
        return reason;
    }

    public int getStatus() {
        return mStatus;
    }
}
