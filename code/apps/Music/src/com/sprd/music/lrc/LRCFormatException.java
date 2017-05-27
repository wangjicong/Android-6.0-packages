
package com.sprd.music.lrc;

/**
 * LRCFormatException
 *
 * @author lisc
 */
public class LRCFormatException extends RuntimeException {

    private static final long serialVersionUID = -5895738790623893622L;

    /**
     * LRCFormatException
     */
    public LRCFormatException() {
        super();
    }

    /**
     * LRCFormatException
     *
     * @param msg
     */
    public LRCFormatException(String msg) {
        super(msg);
    }

    /**
     * LRCFormatException
     *
     * @param msg
     * @param cause
     */
    public LRCFormatException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * LRCFormatException
     *
     * @param cause
     */
    public LRCFormatException(Throwable cause) {
        super(cause);
    }
}
