package com.android.messaging.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.i18n.phonenumbers.PhoneNumberMatch;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.PhoneNumberUtil.Leniency;
import com.android.messaging.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

public class LinkifyUtil extends Linkify {
    private static String TAG = "LinkifyUtil";
    public static int mark = 0;
    public static final boolean addNewLinksSprd(TextView text, int mask) {
        if (mask == 0) {
            return false;
        }

        CharSequence t = text.getText();
        Log.d(TAG, "addLinksSprd");
        if (t instanceof Spannable) {
            Log.d(TAG, "t instanceof Spannable");
            if (addNewLinksSprd((Spannable) t, mask)) {
                addLinkMovementMethod(text);
                return true;
            }

            return false;
        } else {
            SpannableString s = SpannableString.valueOf(t);

            if (addNewLinksSprd(s, mask)) {
                addLinkMovementMethod(text);
                text.setText(s);

                return true;
            }

            return false;
        }
    }
    private static final void addLinkMovementMethod(TextView t) {
        Log.d(TAG, "addLinkMovementMethod");
        MovementMethod m = t.getMovementMethod();

        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (t.getLinksClickable()) {
                t.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }
    public static final boolean addNewLinksSprd(Spannable text, int mask) {
        if (mask == 0) {
            return false;
        }

        URLSpan[] old = text.getSpans(0, text.length(), URLSpan.class);

        for (int i = old.length - 1; i >= 0; i--) {
            text.removeSpan(old[i]);
        }

        ArrayList<LinkSpec> links = new ArrayList<LinkSpec>();

        if ((mask & WEB_URLS) != 0) {
            gatherLinksSprd(links, text, Patterns.WEB_URL_FOR_TEXTVIEW,
                new String[] { "http://", "https://", "rtsp://" },
                sUrlMatchFilter, null, mask);
        }

        if ((mask & EMAIL_ADDRESSES) != 0) {
            gatherLinksSprd(links, text, Patterns.EMAIL_ADDRESS,
                new String[] { "mailto:" },
                null, null, mask);
        }

        if ((mask & PHONE_NUMBERS) != 0) {
            gatherTelLinks(links, text);
        }

        if ((mask & MAP_ADDRESSES) != 0) {
            gatherMapLinks(links, text);
            
        }

        pruneOverlaps(links);

        if (links.size() == 0) {
            return false;
        }

        for (LinkSpec link: links) {
            applyLink(link.url, link.start, link.end, text);
        }

        return true;
    }
    
    private static final void gatherTelLinks(ArrayList<LinkSpec> links, Spannable s) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Iterable<PhoneNumberMatch> matches = phoneUtil.findNumbers(s.toString(),
                Locale.getDefault().getCountry(), Leniency.POSSIBLE, Long.MAX_VALUE);
        for (PhoneNumberMatch match : matches) {
            LinkSpec spec = new LinkSpec();
            spec.url = "tel:" + PhoneNumberUtils.normalizeNumber(match.rawString());
            spec.start = match.start();
            spec.end = match.end();
            links.add(spec);
            mark = 3;
        }
    }

    private static final void gatherMapLinks(ArrayList<LinkSpec> links, Spannable s) {
        String string = s.toString();
        String address;
        int base = 0;

        try {
            while ((address = WebView.findAddress(string)) != null) {
                int start = string.indexOf(address);

                if (start < 0) {
                    break;
                }

                LinkSpec spec = new LinkSpec();
                int length = address.length();
                int end = start + length;

                spec.start = base + start;
                spec.end = base + end;
                string = string.substring(end);
                base += end;

                String encodedAddress = null;

                try {
                    encodedAddress = URLEncoder.encode(address,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    continue;
                }

                spec.url = "geo:0,0?q=" + encodedAddress;
                links.add(spec);
                mark = 4;
            }
        } catch (UnsupportedOperationException e) {
            // findAddress may fail with an unsupported exception on platforms without a WebView.
            // In this case, we will not append anything to the links variable: it would have died
            // in WebView.findAddress.
            return;
        }
    }
    
    private static final void gatherLinksSprd(ArrayList<LinkSpec> links,
            Spannable s, Pattern pattern, String[] schemes,
            MatchFilter matchFilter, TransformFilter transformFilter, int mask) {
        Matcher m = pattern.matcher(s);
        int len = s.length();
        int findStart = -1;
        int findEnd = -1;
        int realEnd = 0;
        CharSequence cs;
        while (m.find()) {
            int start = m.start();
            int end = m.end();

            if (matchFilter == null || matchFilter.acceptMatch(s, start, end)) {
                LinkSpec spec = new LinkSpec();
                String url = makeUrl(m.group(0), schemes, m, transformFilter);
                StringBuilder urlBuilder = new StringBuilder(url);
                if ((mask & WEB_URLS) != 0 ) {
                    if (end < len - 1) {
                        cs = s.subSequence(end, len);
                        Matcher matcher= Patterns.FILE_NAME.matcher(cs);
                        while (matcher.find()) {
                            findStart = matcher.start();
                            findEnd = matcher.end();
                            if (findStart == 0 && findEnd < len) {
                                realEnd = end + findEnd;
                                end = realEnd;
                                urlBuilder.append(s.subSequence(findStart, findEnd).toString());
                                if(pattern.equals(Patterns.WEB_URL_FOR_TEXTVIEW))
                                    mark = 1;
                                else
                                    mark = 2;
                                break;
                            }
                        }
                    }
                }
                spec.url = urlBuilder.toString();
                spec.start = start;
                spec.end = end;

                links.add(spec);
            }
        }
    }
    
    private static final String makeUrl(String url, String[] prefixes,
            Matcher m, TransformFilter filter) {
        if (filter != null) {
            url = filter.transformUrl(m, url);
        }

        boolean hasPrefix = false;
        
        for (int i = 0; i < prefixes.length; i++) {
            if (url.regionMatches(true, 0, prefixes[i], 0,
                                  prefixes[i].length())) {
                hasPrefix = true;

                // Fix capitalization if necessary
                if (!url.regionMatches(false, 0, prefixes[i], 0,
                                       prefixes[i].length())) {
                    url = prefixes[i] + url.substring(prefixes[i].length());
                }

                break;
            }
        }

        if (!hasPrefix) {
            url = prefixes[0] + url;
        }

        return url;
    }
    
    private static final void pruneOverlaps(ArrayList<LinkSpec> links) {
        Comparator<LinkSpec>  c = new Comparator<LinkSpec>() {
            public final int compare(LinkSpec a, LinkSpec b) {
                if (a.start < b.start) {
                    return -1;
                }

                if (a.start > b.start) {
                    return 1;
                }

                if (a.end < b.end) {
                    return 1;
                }

                if (a.end > b.end) {
                    return -1;
                }

                return 0;
            }
        };

        Collections.sort(links, c);

        int len = links.size();
        int i = 0;

        while (i < len - 1) {
            LinkSpec a = links.get(i);
            LinkSpec b = links.get(i + 1);
            int remove = -1;

            if ((a.start <= b.start) && (a.end > b.start)) {
                if (b.end <= a.end) {
                    remove = i + 1;
                } else if ((a.end - a.start) > (b.end - b.start)) {
                    remove = i + 1;
                } else if ((a.end - a.start) < (b.end - b.start)) {
                    remove = i;
                }

                if (remove != -1) {
                    links.remove(remove);
                    len--;
                    continue;
                }

            }

            i++;
        }
    }
    
    private static final void applyLink(String url, int start, int end, Spannable text) {
        Log.d(TAG, "applyLink");
        URLSpan span = new MyURLSpan(url, mark);
        text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    
    
}
class MyURLSpan extends URLSpan{   
    private Context mContext;
    private int mark;
    MyURLSpan(String url, int mark) {
        super(url);
        this.mark = mark;
    }
    @Override
    public void onClick(final View widget) {
        // TODO Auto-generated method stub
        mContext =  widget.getContext();
        Log.d("LinkifyUtil", "LinkifyUtil.onClick");
        Log.d("LinkifyUtil", "uri:" + Uri.parse(getURL()));
        if(getURL().startsWith("http") || getURL().startsWith("https") || getURL().startsWith("rtsp")) {
            mark = 1;
        } else if (getURL().startsWith("mailto")) {
            mark = 2;
        } else if (getURL().startsWith("tel")) {
            mark = 3;
        } else if (getURL().startsWith("geo")) {
            mark = 4;
        }
        int resId = 0;
        switch(mark) {
        case 1:
            resId = R.string.browser;
            break;
        case 2:
            resId = R.string.email;
            break;
        case 3:
            resId = R.string.telephone;
            break;
        case 4:
            resId = R.string.map;
            break;
        }
        AlertDialog dialog = new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.is_redirect_to, mContext.getString(resId))).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        }).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                enter(widget);
            }
        }).setCancelable(true).create();
        dialog.show();
    }
    public void enter(View widget) {
        super.onClick(widget);
    }
}
class LinkSpec {
    String url;
    int start;
    int end;
}
