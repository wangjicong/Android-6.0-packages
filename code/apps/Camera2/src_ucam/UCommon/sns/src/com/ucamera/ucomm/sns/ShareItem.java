/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

import static com.ucamera.ucomm.sns.services.ServiceProvider.*;
public enum ShareItem {
    SINA    (R.id.sns_item_sina,    R.drawable.sns_ic_sina,    R.drawable.sns_ic_sina_pressed,      R.string.sns_label_sina,    SERVICE_SINA),
    QZONE   (R.id.sns_item_qzone,   R.drawable.sns_ic_qzone,   R.drawable.sns_ic_qzone_pressed,     R.string.sns_label_qzone,   SERVICE_QZONE),
    RENREN  (R.id.sns_item_renren,  R.drawable.sns_ic_renren,  R.drawable.sns_ic_renren_pressed,    R.string.sns_label_renren,  SERVICE_RENREN),
    KAIXIN  (R.id.sns_item_kaixin,  R.drawable.sns_ic_kaixin,  R.drawable.sns_ic_kaixin_pressed,    R.string.sns_label_kaixin,  SERVICE_KAIXIN),
    TENCENT (R.id.sns_item_tencent, R.drawable.sns_ic_tencent, R.drawable.sns_ic_tencent_pressed,   R.string.sns_label_tencent, SERVICE_TENCENT),
    SOHU    (R.id.sns_item_sohu,    R.drawable.sns_ic_sohu,    R.drawable.sns_ic_sohu_pressed,      R.string.sns_label_sohu,    SERVICE_SOHU),
    FLICKR  (R.id.sns_item_flickr,  R.drawable.sns_ic_flickr,  R.drawable.sns_ic_flickr_pressed,    R.string.sns_label_flickr,  SERVICE_FLICKR),
    FACEBOOK(R.id.sns_item_facebook,R.drawable.sns_ic_facebook,R.drawable.sns_ic_fackbook_pressed,  R.string.sns_label_facebook,SERVICE_FACEBOOK),
    TWITTER (R.id.sns_item_twitter, R.drawable.sns_ic_twitter, R.drawable.sns_ic_twitter_pressed,   R.string.sns_label_twitter, SERVICE_TWITTER),
    TUMBLR  (R.id.sns_item_tumblr,  R.drawable.sns_ic_tumblr,  R.drawable.sns_ic_tumblr_pressed,    R.string.sns_label_tumblr,  SERVICE_TUMBLR),
    MIXI    (R.id.sns_item_mixi,    R.drawable.sns_ic_mixi,    R.drawable.sns_ic_mixi_pressed,      R.string.sns_label_mixi,    SERVICE_MIXI),
    QQVATAR (R.id.sns_item_qqvatar, R.drawable.sns_ic_portrait,R.drawable.sns_ic_ic_portrait_press, R.string.sns_label_qqvatar, SERVICE_QQVATAR);

    private final int mId;
    private final int mIcon;
    private final int mLabel;
    private final int mSelectedIcon;
    private final int mService;

    private ShareItem(int id, int icon, int selectedIcon, int label, int service) {
        mId   = id;
        mIcon = icon;
        mSelectedIcon = selectedIcon;
        mLabel = label;
        mService = service;
    }

    public int getId()            {return mId;}
    public int getIcon()          {return this.mIcon;}
    public int getLabel()         {return this.mLabel;}
    public int getService()       {return this.mService;}
    public int getSelectedIcon() {return this.mSelectedIcon;}

    private static ItemsFilter sFilter;
    public static final void setFilter(ItemsFilter v) {
        sFilter = v;
    }

    public static ShareItem[] sortedValues() {
//        if(Build.SNS_SITE.isCustom()) {
//            if (Build.isSourceNext()) {
//                return new ShareItem[]{FACEBOOK,TWITTER,FLICKR,TUMBLR,MIXI};
//            }else if (Build.isDoov()) {
//                return new ShareItem[]{SINA,QZONE,RENREN,KAIXIN,TENCENT,SOHU};
//            }
//        }
        if ( sFilter != null && sFilter.sortedValues() != null){
            return sFilter.sortedValues();
        }
        return defaultSortedValues();
    }

    public static ShareItem[] sortedAccountValues() {
        if ( sFilter != null && sFilter.sortedValues() != null){
            return sFilter.sortedValues();
        }
        return defaultAccountSortedValues();
    }

    public static ShareItem[] defaultAccountSortedValues() {
        if(ShareActivity.mShowTurkeyShare) {
            return new ShareItem[]{FACEBOOK, TWITTER, FLICKR, TUMBLR};
        }
        if(Util.isChinese()){
            return new ShareItem[]{SINA,QZONE,RENREN,KAIXIN,TENCENT,
                    FACEBOOK,TWITTER,SOHU,FLICKR,TUMBLR,MIXI,QQVATAR};//SOHU,FLICKR,TUMBLR,MIXI
        }
        return new ShareItem[]{FACEBOOK,TWITTER,FLICKR,TUMBLR,
                SINA,QZONE, RENREN,KAIXIN,TENCENT,SOHU,MIXI,QQVATAR};
    }

    public static ShareItem[] defaultSortedValues() {
        if(ShareActivity.mShowTurkeyShare) {
            return new ShareItem[]{FACEBOOK, TWITTER, FLICKR, TUMBLR};
        }
        if(Util.isChinese()){
            return new ShareItem[]{SINA,QZONE,RENREN,KAIXIN,TENCENT,
                    FACEBOOK,TWITTER,SOHU,FLICKR,TUMBLR,MIXI};//SOHU,FLICKR,TUMBLR,MIXI
        }
        return new ShareItem[]{FACEBOOK,TWITTER,FLICKR,TUMBLR,
                SINA,QZONE, RENREN,KAIXIN,TENCENT,SOHU,MIXI};
    }

    public interface ItemsFilter {
        public ShareItem[] sortedValues();
    }
}
