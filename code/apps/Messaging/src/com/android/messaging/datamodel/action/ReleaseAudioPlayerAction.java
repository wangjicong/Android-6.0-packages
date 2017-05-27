package com.android.messaging.datamodel.action;

import com.android.messaging.util.AudioPlayerPolicy;

import android.content.Context;
import android.os.Parcelable;
import android.os.Parcel;
import android.util.Log;

public class ReleaseAudioPlayerAction extends Action implements Parcelable {

    private Context mContext;

    public static void releaseAudioPlayer(int player, Context context) {
        final ReleaseAudioPlayerAction action = new ReleaseAudioPlayerAction(
                player, context);
        action.start();
    }

    private static final String KEY_PLAYER_ID = "player_id";

    private ReleaseAudioPlayerAction(int player, Context context) {
        super();
        mContext = context;
        actionParameters.putInt(KEY_PLAYER_ID, player);
    }

    @Override
    protected Object executeAction() {
        int player = actionParameters.getInt(KEY_PLAYER_ID);
        if (player < 0) {
            AudioPlayerPolicy.get(mContext).releaseAllAudioPlayers();
        } else {
            AudioPlayerPolicy.get(mContext).releaseAudioPlayer(player);
        }
        return "ok";
    }

    private ReleaseAudioPlayerAction(final Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<ReleaseAudioPlayerAction> CREATOR = new Parcelable.Creator<ReleaseAudioPlayerAction>() {
        @Override
        public ReleaseAudioPlayerAction createFromParcel(final Parcel in) {
            return new ReleaseAudioPlayerAction(in);
        }

        @Override
        public ReleaseAudioPlayerAction[] newArray(final int size) {
            return new ReleaseAudioPlayerAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }
}
