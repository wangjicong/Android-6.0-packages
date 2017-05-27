package com.android.providers.telephony;

import android.text.TextUtils;

public class CharEscapeUtil {

    public static String charEscaseEncode(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < text.length(); i++) {
            c = text.charAt(i);
            switch (c) {
            case '/':
                sb.append("//");
                break;
            case '\'':
                sb.append("''");
                break;
            case '[':
                sb.append("/[");
                break;
            case ']':
                sb.append("/]");
                break;
            case '%':
                sb.append("/%");
                break;
            case '&':
                sb.append("/&");
                break;
            case '_':
                sb.append("/_");
                break;
            case '(':
                sb.append("/(");
                break;
            case ')':
                sb.append("/)");
                break;
            default:
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public static String sqliteEscape(String keyWord) {
        keyWord = keyWord.replace("/", "//");
        keyWord = keyWord.replace("'", "''");
        keyWord = keyWord.replace("[", "/[");
        keyWord = keyWord.replace("]", "/]");
        keyWord = keyWord.replace("%", "/%");
        keyWord = keyWord.replace("&", "/&");
        keyWord = keyWord.replace("_", "/_");
        keyWord = keyWord.replace("(", "/(");
        keyWord = keyWord.replace(")", "/)");
        return keyWord;
    }
}
