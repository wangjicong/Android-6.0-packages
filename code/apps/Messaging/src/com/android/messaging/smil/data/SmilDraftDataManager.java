
package com.android.messaging.smil.data;

import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.messaging.datamodel.action.InsertNewMessageAction;
import com.android.messaging.datamodel.action.WriteDraftMessageAction;
import com.android.messaging.datamodel.data.DraftMessageData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.smil.data.SmilPartEntity;
import com.android.messaging.smil.view.SmileditPar;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.util.GlobleUtil;
import com.android.messaging.util.TextUtil;

public class SmilDraftDataManager {

    private static final String TAG = SmilDraftDataManager.class.getSimpleName();

    public static void saveSmilDraftData(Context context, DraftMessageData draftMsgData,
            List<SmilPartEntity> smileditPars) {
        if (draftMsgData == null) {
            Log.e("smil-s",
                    "SmilDraftDataManager.saveSmilDraftData: The input DraftMessageData is null, so return.");
            return;
        }
        MessageData message = getMessageData(context, draftMsgData, smileditPars);

        WriteDraftMessageAction.writeDraftMessage(draftMsgData.getConversationId(), message);
    }

    private static MessageData getMessageData(Context context, DraftMessageData draftMsgData,
            List<SmilPartEntity> smileditPars) {
        MessageData message;
        //if (draftMsgData.getIsMms()) {//
        if(smileditPars.size() > 0 || !TextUtils.isEmpty(draftMsgData.getMessageSubject())){
            Log.e("smil-s", "SmilDraftDataManager.getMessageData: it is Mms, save it.");
            message = MessageData.createDraftMmsMessage(draftMsgData.getConversationId(),
                    draftMsgData.getSelfId(), draftMsgData.getMessageText(),
                    draftMsgData.getMessageSubject());
            MessagePartData smilPart = getSmilPart(context, smileditPars);
            Log.e("smil-s", "SmilDraftDataManager.getMessageData: smilPart.mText="+smilPart.getText());
            GlobleUtil.setSmilmDraftMessageData(smilPart);
            message.addPart(smilPart);// add smil part
            for (final MessagePartData attachment : draftMsgData.getAttachments()) {
                message.addPart(attachment);
            }
        } else {
            Log.e("smil-s", "SmilDraftDataManager.getMessageData: it is Sms, save it.");
            message = MessageData.createDraftSmsMessage(draftMsgData.getConversationId(),
                    draftMsgData.getSelfId(), draftMsgData.getMessageText());
        }
        return message;
    }

    /**
     * save draft or send mms must make and add the smil part
     */
    public static MessagePartData getSmilPart(Context context, List<SmilPartEntity> smileditPars) {
        String smilText = MmsUtils.createSmilText(context, smileditPars);
        Log.d("smil-s", "SmilDraftDataManager.getSmilPart:smilText=" + smilText);
        return MessagePartData.createSmilMessagePart(smilText);
    }

    /**
     * send mms with draft data
     * */
    public static void sendMessage(Context context, DraftMessageData draftMsgData,
            List<SmilPartEntity> smileditPars, int subId) {
        if (draftMsgData == null) {
            Log.e("smil-s",
                    "SmilDraftDataManager.sendMessage: The input DraftMessageData is null, so return.");
            return;
        }
        MessageData message = getMessageData(context, draftMsgData, smileditPars);
        InsertNewMessageAction.insertNewMessage(message, subId);
    }
}
