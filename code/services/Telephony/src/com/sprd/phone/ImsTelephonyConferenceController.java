/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.services.telephony;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.content.Context;
import android.net.Uri;
import android.os.Debug;
import android.telecom.Conference;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import com.android.ims.ImsManager;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.GsmConnection;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.phone.PhoneGlobals;

/**
 * Maintains a list of all the known TelephonyConnections connections and controls GSM and
 * default IMS conference call behavior. This functionality is characterized by the support of
 * two top-level calls, in contrast to a CDMA conference call which automatically starts a
 * conference when there are two calls.
 */
final class ImsTelephonyConferenceController {
    private static final int TELEPHONY_CONFERENCE_MAX_SIZE = 5;

    private static final boolean DBG = Debug.isDebug();

    private final Connection.Listener mConnectionListener = new Connection.Listener() {
        @Override
        public void onStateChanged(Connection c, int state) {
            recalculate();
        }

        /** ${inheritDoc} */
        @Override
        public void onDisconnected(Connection c, DisconnectCause disconnectCause) {
            recalculate();
        }

        @Override
        public void onDestroyed(Connection connection) {
            remove(connection);
        }
    };

    /** The known connections. */
    private final List<TelephonyConnection> mTelephonyConnections = new ArrayList<>();

    private final TelephonyConnectionService mConnectionService;

    public ImsTelephonyConferenceController(TelephonyConnectionService connectionService) {
        mConnectionService = connectionService;
    }

    /** The TelephonyConference connection object. */
    private TelephonyConference mTelephonyConference;

    void add(TelephonyConnection connection) {
        mTelephonyConnections.add(connection);
        connection.addConnectionListener(mConnectionListener);
        recalculate();
    }

    void remove(Connection connection) {
        connection.removeConnectionListener(mConnectionListener);
        mTelephonyConnections.remove(connection);

        recalculate();
    }

    private void recalculate() {
        recalculateConference();
        recalculateConferenceable();
    }

    private boolean isFullConference(Conference conference) {
        return conference.getConnections().size() >= TELEPHONY_CONFERENCE_MAX_SIZE;
    }

    private boolean participatesInFullConference(Connection connection) {
        return connection.getConference() != null &&
                isFullConference(connection.getConference());
    }

    /**
     * Calculates the conference-capable state of all GSM connections in this connection service.
     */
    private void recalculateConferenceable() {
        if (DBG) Log.v(this, "recalculateConferenceable : %d", mTelephonyConnections.size());

        List<Connection> activeConnections = new ArrayList<>(mTelephonyConnections.size());
        List<Connection> backgroundConnections = new ArrayList<>(mTelephonyConnections.size());

        // Loop through and collect all calls which are active or holding
        for (Connection connection : mTelephonyConnections) {
            if (DBG) Log.d(this, "recalc - %s %s", connection.getState(), connection);

            if (!participatesInFullConference(connection)) {
                switch (connection.getState()) {
                case Connection.STATE_ACTIVE:
                    activeConnections.add(connection);
                    continue;
                case Connection.STATE_HOLDING:
                    backgroundConnections.add(connection);
                    continue;
                default:
                    break;
                }
            }

            connection.setConferenceableConnections(Collections.<Connection>emptyList());
        }

        Log.v(this, "active: %d, holding: %d",
                activeConnections.size(), backgroundConnections.size());

        // Go through all the active connections and set the background connections as
        // conferenceable.
        for (Connection connection : activeConnections) {
            connection.setConferenceableConnections(backgroundConnections);
        }

        // Go through all the background connections and set the active connections as
        // conferenceable.
        for (Connection connection : backgroundConnections) {
            connection.setConferenceableConnections(activeConnections);
        }

        // Set the conference as conferenceable with all the connections
        if (mTelephonyConference != null && !isFullConference(mTelephonyConference)) {
            List<Connection> nonConferencedConnections =
                    new ArrayList<>(mTelephonyConnections.size());
            for (TelephonyConnection c : mTelephonyConnections) {
                if (c.getConference() == null) {
                    nonConferencedConnections.add(c);
                }
            }
            if (DBG) Log.v(this, "conference conferenceable: %s", nonConferencedConnections);
            mTelephonyConference.setConferenceableConnections(nonConferencedConnections);
        }

        // TODO: Do not allow conferencing of already conferenced connections.
    }

    private void recalculateConference() {
        Set<Connection> conferencedConnections = new HashSet<>();
        Set<Connection> activeConnections = new HashSet<>();
        Set<Connection> holdConnections = new HashSet<>();
        int numGsmConnections = 0;

        // SPRD: Fix bug424877
        PhoneAccountHandle phoneAccountHandle = null;

        for (TelephonyConnection connection : mTelephonyConnections) {
            com.android.internal.telephony.Connection radioConnection =
                connection.getOriginalConnection();
            if (radioConnection != null) {
                Call.State state = radioConnection.getState();
                Call call = radioConnection.getCall();
                Log.i(this, "recalculateConference radioConnection: %s", radioConnection);

                if (radioConnection.isMultiparty()) {
                    if(state == Call.State.HOLDING){
                        holdConnections.add(connection);
                    } else {
                        activeConnections.add(connection);
                    }
                    // SPRD: Fix bug424877
                    // We think current "connection" is going to join the conference call, or
                    // has been in the conference call after "connection" is added to
                    // "conferencedConnections", so we get PhoneAccountHandle according
                    // to current "connection".
                    Phone phone = connection.getPhone();
                    phoneAccountHandle = TelecomAccountRegistry.getInstance(
                            PhoneGlobals.getInstance().getApplicationContext())
                            .getPhoneAccountHandle(phone);
                }
            }
        }
        Log.i(this, "recalculateConference active: %s , hold: %s",
                activeConnections.size(), holdConnections.size());
        if(activeConnections.size() >= holdConnections.size()){
            numGsmConnections = activeConnections.size();
            conferencedConnections = activeConnections;
        } else {
            numGsmConnections = holdConnections.size();
            conferencedConnections = holdConnections;
        }

        if (DBG) Log.d(this, "Recalculate conference calls %s %s.",
                mTelephonyConference, conferencedConnections);

        // If this is a GSM conference and the number of connections drops below 2, we will
        // terminate the conference.
        if (numGsmConnections < 2) {
            Log.d(this, "not enough connections to be a conference!");

            // No more connections are conferenced, destroy any existing conference.
            if (mTelephonyConference != null) {
                Log.d(this, "with a conference to destroy!");
                mTelephonyConference.destroy();
                mTelephonyConference = null;
            }
        } else {
            if (mTelephonyConference != null) {
                List<Connection> existingConnections = mTelephonyConference.getConnections();
                // Remove any that no longer exist
                for (Connection connection : existingConnections) {
                    if (connection instanceof TelephonyConnection &&
                            !conferencedConnections.contains(connection)) {
                        mTelephonyConference.removeConnection(connection);
                    }
                }

                // Add any new ones
                for (Connection connection : conferencedConnections) {
                    if (!existingConnections.contains(connection)) {
                        if (DBG) Log.d(this, "Adding a connection to a conference call: %s || %s",
                                mTelephonyConference, connection);
                        mTelephonyConference.addConnection(connection);
                    }
                }
            } else {
                mTelephonyConference = new TelephonyConference(/*null*/phoneAccountHandle);

                long mTempConnectRealTimeMillis = Long.MAX_VALUE;

                for (Connection connection : conferencedConnections) {
                   if (DBG) Log.d(this, "Adding a connection to a conference call: %s || %s",
                            mTelephonyConference, connection);
                    mTelephonyConference.addConnection(connection);

                    mTempConnectRealTimeMillis = Math.min(((TelephonyConnection) connection).getOriginalConnection().getConnectTimeReal(),
                            mTempConnectRealTimeMillis);
                }
                mTelephonyConference.setConnectRealTimeMillis(mTempConnectRealTimeMillis);
                mConnectionService.addConference(mTelephonyConference);
            }

            // Set the conference state to the same state as its child connections.
            Connection conferencedConnection = mTelephonyConference.getPrimaryConnection();
            if (conferencedConnection != null) {
                switch (conferencedConnection.getState()) {
                case Connection.STATE_ACTIVE:
                    mTelephonyConference.setActive();
                    break;
                case Connection.STATE_HOLDING:
                    mTelephonyConference.setOnHold();
                    break;
                case Connection.STATE_DIALING:
                case Connection.STATE_INITIALIZING:
                case Connection.STATE_NEW:
                    mTelephonyConference.setDialing();
                    break;
                default:
                    if (DBG) Log.d(this, "Does not support conference state:"+conferencedConnection.getState());
                    break;
                }
                //SPRD:fix bug for 545067
                if(isHasActiveConnection()){
                    mTelephonyConference.setActive();
                }
            }
        }
    }

    /* SPRD: Traverse all the connection whether has active or not for fix bug 545067 @{ */
    private boolean isHasActiveConnection(){
        if(mTelephonyConference != null){
            List<Connection> connections = mTelephonyConference.getConnections();
            for (Connection connection : connections){
                if(connection.getState()==Connection.STATE_ACTIVE){
                    return true;
                }
            }
        }
        return false;
    }
    /* @} */

}
