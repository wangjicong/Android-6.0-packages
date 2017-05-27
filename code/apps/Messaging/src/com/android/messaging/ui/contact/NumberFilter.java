package com.android.messaging.ui.contact;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.util.Log;
import com.sprd.plat.mms.plugin.Impl.BaseUserMessage;
import com.sprd.plat.mms.plugin.Impl.NotifyStatus;
import com.sprd.plat.mms.plugin.Interface.INotify;
import com.android.messaging.util.PhoneUtils;

class NumberFilter extends BaseUserMessage {

    @Override
    public int OnNotify(int nMsg, int nValue, long lValue, Object obj,
            List<Object> lstObj) {
        if (PhoneUtils.isValidSmsMmsDestination(String.valueOf(obj)))
            return NotifyStatus.SUCC;
        int ret =  judgeNumber(String.valueOf(obj)) ? NotifyStatus.SUCC
                : NotifyStatus.FAILURE;
        return ret;
    }

    private boolean judgeNumber(final String number) {
        File file = new File(
                "/data/data/com.android.messaging/files/number_match.txt");
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        ArrayList<String> telRegexs = new ArrayList<String>();
        try {
            InputStream instream = new FileInputStream(file);
            if (instream == null)
                return false;
            InputStreamReader inputreader = new InputStreamReader(instream);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            while ((line = buffreader.readLine()) != null) {
                Log.d("NumberFilter", "=======zhongjihao==1===judgeNumber==line: "
                        + line);
                telRegexs.add(line);
            }
            instream.close();
        } catch (java.io.FileNotFoundException e) {
            Log.e("PhoneUtils", "The File doesn't not exist.");
        } catch (IOException e) {
            Log.e("PhoneUtils", e.getMessage());
        }
        if (telRegexs.size() == 0)
            return false;
        for (String telRegex : telRegexs) {
            Pattern PHONE_PATTERN = Pattern.compile(telRegex);
            Matcher m = PHONE_PATTERN.matcher(number);
            if (m.matches()) {
                return true;
            }
        }
        return false;
    }
}