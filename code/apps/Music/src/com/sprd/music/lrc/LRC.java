
package com.sprd.music.lrc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * LRC is a data structure to restore the lrc file's informations.
 *
 * @author liwd<mailto:liwd@thunderst.com>
 */
public class LRC {

    // Basic informations:
    public String artist;

    public String title;

    public String album;

    public String by;

    public long offset = 0;

    public String key;

    private List<Offset> offsets = new ArrayList<Offset>();

    private List<String> lyrics = new ArrayList<String>();

    private boolean isReady = true;

    private int curLrc = -1; // the current shown lyric index.

    private int lastLrc = -1; // the lyric index shown at last time.

    public LRC() {
    }

    public List<Offset> getOffsets() {
        return offsets;
    }

    public List<String> getLyrics() {
        return lyrics;
    }

    /**
     * Reset the pointers befor restart switch or scroll lyrics.
     */
    public void reset() {
        curLrc = -1;
        lastLrc = -1;
    }

    /**
     * Add an Offset.
     *
     * @param time the offset time
     * @param lrcInd the index of the lyric according to the current offset
     *            time.
     */
    public void addOffset(long time, int lrcInd) {
        if (isReady && lrcInd >= 0 && lrcInd < lyrics.size()) {
            Offset o = new Offset(time, lrcInd);
            int ind = offsets.indexOf(o);
            if (ind >= 0) {
                offsets.remove(ind);
            }
            offsets.add(o);
        }
    }

    /**
     * Add a lyric.
     *
     * @param lyric
     * @return int
     */
    public int addLyric(String lyric) {
        if (isReady) {
            lyrics.add(lyric);
            return lyrics.size() - 1;
        }
        throw new IllegalStateException("LRC is not ready. Did you forget to invoke init() first?");
    }

    /**
     * setBasicInfo
     *
     * @param key
     * @param value
     * @return boolean
     */
    public boolean setBasicInfo(String key, String value) {
        if (key != null) {
            key = key.toLowerCase();
            if (LyricConstants.KEY_AR.equals(key)) {
                artist = value;
            } else if (LyricConstants.KEY_TI.equals(key)) {
                title = value;
            } else if (LyricConstants.KEY_AL.equals(key)) {
                album = value;
            } else if (LyricConstants.KEY_BY.equals(key)) {
                by = value;
            } else if (LyricConstants.KEY_OFFSET.equals(key)) {
                offset = new Long(value);
            } else if (LyricConstants.KEY_KEY.equals(key)) {
                key = value;
            }
            return true;
        }
        return false;
    }

    /**
     * When you have added all the offsets, you should invoke this method.
     */
    public void sortOffsets() {
        if (isReady) {
            Collections.sort(offsets, new Comparator<Offset>() {
                public int compare(Offset one, Offset another) {
                    long t1 = one.time;
                    long t2 = another.time;
                    return t1 == t2 ? 0 : t1 > t2 ? 1 : -1;
                }
            });
            for (Offset o : offsets) {
                o.time -= offset;
            }
        }
    }

    /**
     * @param row -1 means all lyrics, otherwise means the selected one.
     * @param after True means ajust the after lyrics, False means the selected
     *          one.
     * @param advance True means advance, False means Delayed.
     * @param time ajust time
     */
    public void ajust(int row, boolean after, boolean advance, int time) {
        int next = 0;
        if (row >= 0 && row < offsets.size()) {
            if (after) {
                if (advance) {
                    next = row + 1;
                    int len = offsets.size();
                    for (; next < len; next++) {
                        offsets.get(next).time -= time;
                    }
                    int i = 0, j = 0;
                    Offset curOff = null, nextOff = null;
                    for (i = row; i >= 0; i--) {
                        curOff = offsets.get(i);
                        for (j = i + 1; j < len;) {
                            nextOff = offsets.get(j);
                            if (curOff.time > nextOff.time) {
                                offsets.set(j - 1, nextOff);
                                nextOff = offsets.get(++j);
                            } else {
                                break;
                            }
                        }
                        if (j != i + 1) {
                            offsets.set(j - 1, curOff);
                        }
                    }
                } else {
                    next = row + 1;
                    int len = offsets.size();
                    for (; next < len; next++) {
                        offsets.get(next).time += time;
                    }
                }
            } else {
                if (advance) {
                    next = row;
                    Offset curOff = offsets.get(next);
                    Offset nextOff = offsets.get(next - 1);
                    curOff.time -= time;
                    while (nextOff != null && curOff.time < nextOff.time) {
                        offsets.set(next--, nextOff);
                        nextOff = offsets.get(next - 1);
                    }
                    offsets.set(next, curOff);
                } else {
                    next = row;
                    Offset curOff = offsets.get(next);
                    Offset nextOff = offsets.get(next + 1);
                    curOff.time += time;
                    while (nextOff != null && curOff.time > nextOff.time) {
                        offsets.set(next++, nextOff);
                        nextOff = offsets.get(next + 1);
                    }
                    offsets.set(next, curOff);
                }
            }
        } else {
            if (advance) {
                for (Offset o : offsets) {
                    o.time -= time;
                }
            } else {
                for (Offset o : offsets) {
                    o.time += time;
                }
            }
        }
    }

    /**
     * next
     *
     * @param elapsedTime
     * @return object
     */
    public Object[] next(long elapsedTime) {
        if (isReady) {
            int size = offsets.size();
            if (size == 0 || curLrc >= (size - 1)) {
                return null;
            }
            for (++curLrc; curLrc < size && offsets.get(curLrc).time < elapsedTime; curLrc++)
                ;
            long nextTime = -1; // Delayed time for the next lyric.
            int curLyricInd = -1; // The current displayed lyric index.
            if (curLrc >= size) {
                nextTime = -1;
                curLyricInd = size - 1;
                lastLrc = curLyricInd;
            } else {
                Offset o = offsets.get(curLrc);
                if (o.time > elapsedTime) {
                    nextTime = o.time - elapsedTime;
                    --curLrc;
                } else {// o.time == elapsedTime
                    nextTime = curLrc == size - 1 ? -1 : offsets.get(curLrc + 1).time - elapsedTime;
                }
                curLyricInd = curLrc == lastLrc ? -1 : curLrc;
                lastLrc = curLrc;
            }
            return new Object[] {
                    nextTime, curLyricInd == -1 ? null : lyrics.get(curLyricInd)
            };
        }
        throw new IllegalStateException("LRC is not ready. Did you forget to invoke init() first?");
    }

    /**
     * listLyrics
     *
     * @return String
     */
    public String[] listLyrics() {
        if (isReady) {
            int size = offsets.size();
            String[] lrcs = new String[size];
            for (int i = 0; i < size; i++) {
                lrcs[i] = lyrics.get(offsets.get(i).lrcInd);
            }
            return lrcs;
        }
        return null;
    }

    @Override
    public String toString() {
        if (isReady) {
            StringBuilder sb = new StringBuilder();
            sb.append("[artist:").append(artist).append("]\n").append("[title:").append(title)
                    .append("]\n").append("[album:").append(album).append("]\n").append("[by:")
                    .append(by).append("]\n").append("[offset:").append(offset).append("]\n")
                    .append("[key:").append(key).append("]\n");
            for (Offset o : offsets) {
                sb.append("[").append(o.time).append("]").append(lyrics.get(o.lrcInd)).append("\n");
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * A data structure to resotre the offset times and the pointer to the
     * lyrics.
     */
    public static class Offset {
        long time;

        int lrcInd;

        Offset(long t, int l) {
            time = t;
            lrcInd = l;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Offset) {
                Offset another = (Offset) obj;
                if (another.time == time) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = 17;
            int l = (int) (time ^ (time >>> 32));
            result = 37 * result + l;
            return result;
        }
    }

    /**
     * PositionProvider
     *
     * @author lisc
     */
    public interface PositionProvider {

        /**
         * getPosition
         *
         * @return long
         */
        long getPosition();

        /**
         * getDuration
         *
         * @return long
         */
        long getDuration();
    }
}
