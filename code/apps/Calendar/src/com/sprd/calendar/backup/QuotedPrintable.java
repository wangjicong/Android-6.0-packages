/* SPRD: for bug473564, add backup info @{ */
package com.sprd.calendar.backup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import org.apache.commons.codec.DecoderException;

public class QuotedPrintable {
    private static final String TAG = "QuotedPrintable";

    private static final String CHARSET = "UTF-8";

    private static final String CHARSET_ASCII = "US_ASCII";

    private static final byte ESCAPE_CHAR = '=';

    private static final byte[] CHUNK_SEPARATOR = "\r\n".getBytes();

    private static final BitSet PRINTABLE_CHARS = new BitSet(256);

    private static byte TAB = 9;

    private static byte SPACE = 32;

    // Static initializer for printable chars collection
    static {
        // alpha characters
        for (int i = 33; i <= 60; i++) {
            PRINTABLE_CHARS.set(i);
        }
        for (int i = 62; i <= 126; i++) {
            PRINTABLE_CHARS.set(i);
        }
        PRINTABLE_CHARS.set(TAB);
        PRINTABLE_CHARS.set(SPACE);
    }

    public static String encode(String pString, String charset) throws IOException {

        int count = 0;
        if (pString == null) {
            return null;
        }
        byte[] mBytes = pString.getBytes(CHARSET);
        if (mBytes == null) {
            return null;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < mBytes.length; i++) {
            int b = mBytes[i];
            if (b < 0) {
                b = 256 + b;
            }
            count++;
            count = encodeQuotedPrintable(b, buffer, count);
        }
        return new String(buffer.toByteArray(), CHARSET_ASCII);

    }

    public static String decode(String pString, String charset) throws DecoderException,
            UnsupportedEncodingException {
        if (pString == null) {
            return null;
        }
        pString = pString.replaceAll("==", "=");
        byte[] mBytes = pString.getBytes(CHARSET);
        if (mBytes == null) {
            return null;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < mBytes.length; i++) {
            int b = mBytes[i];
            if (b == ESCAPE_CHAR) {
                try {
                    int u = Character.digit((char) mBytes[++i], 16);
                    int l = Character.digit((char) mBytes[++i], 16);
                    if (u == -1 || l == -1) {
                        throw new DecoderException("Invalid quoted-printable encoding");
                    }
                    buffer.write((char) ((u << 4) + l));
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new DecoderException("Invalid quoted-printable encoding");
                }
            } else {
                buffer.write(b);
            }
        }
        return new String(buffer.toByteArray(), charset);

    }

    private static int encodeQuotedPrintable(int b, ByteArrayOutputStream buffer, int count)
            throws IOException {
        if (count == 76) {
            count = 0;
            count++;
            buffer.write(ESCAPE_CHAR);
            buffer.write(CHUNK_SEPARATOR);
        }

        buffer.write(ESCAPE_CHAR);
        char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
        char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));

        count++;

        if (count == 76) {
            count = 0;
            count++;
            buffer.write(ESCAPE_CHAR);
            buffer.write(CHUNK_SEPARATOR);
        }

        buffer.write(hex1);
        count++;

        if (count == 76) {
            count = 0;
            count++;
            buffer.write(ESCAPE_CHAR);
            buffer.write(CHUNK_SEPARATOR);
        }

        buffer.write(hex2);
        return count;
    }

}
/* @} */