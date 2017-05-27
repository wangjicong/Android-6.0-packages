/*
 * Copyright (C) 2014,2015 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.sns;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class NetLayoutDialog {
    private static final String DATEFORMAT = "yyyy/MM/dd HH:mm";
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private int mResources;
    private Dialog dialog;
    protected View view;
    private String mPhotoUri;
    private String mPhotoDes;

    public NetLayoutDialog(Context context) {
        this(context, null, null);
    }

    public NetLayoutDialog(Context context/*, int resources*/, String photoUri, String photoDes) {
        this.mContext = context;
        this.mResources = R.layout.net_print;
        this.mLayoutInflater = LayoutInflater.from(mContext);
        this.mPhotoUri = photoUri;
        this.mPhotoDes = photoDes;
    }
    public void showDialog() {
        view = mLayoutInflater.inflate(mResources, null);
        dialog = new Dialog(mContext, R.style.NetPrintDialog);
        dialog.setContentView(view);
        dialog.show();

        setViewListener();
    }
    public void dismiss() {
        if(dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    private void setViewListener() {
        TextView tv_shi = (TextView) view.findViewById(R.id.net_print_shi);
        TextView tv_tolot = (TextView) view.findViewById(R.id.net_print_tolot);
        Button btn_back = (Button) view.findViewById(R.id.net_print_cancel);
        tv_shi.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent intent = new Intent("android.intent.action.MAIN");
                    intent.setClassName("jp.n_pri.ap.ShimapriForAp", "jp.n_pri.ap.ShimapriForAp.SplashActivity");
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("aspid", "10000001");
                    mContext.startActivity(intent);
                    dialog.dismiss();
                } catch (ActivityNotFoundException e) {
                    new NetPrintDialog(mContext, NetPrintDialog.SHI).showDialog();
                    dialog.dismiss();
                }
            }
        });

        tv_tolot.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent intent = new Intent("com.tolot.android.action.CREATE_BOOK");
                    intent.putExtra("info", Constant.NET_INFO);
                    String time = new SimpleDateFormat(DATEFORMAT).format(new Date());
                    intent.putExtra("book", replaceStr("CREATETIME", Constant.NET_BOOK, time));
                    if(mPhotoUri != null) {
                        intent.putExtra("page1", replaceStr("PHOTOURI", Constant.NET_PAGE_PHOTO, mPhotoUri));
                    }else {
                        intent.putExtra("page1", Constant.NET_PAGE_BLANK);
                    }
                    if(mPhotoUri != null && mPhotoDes != null) {
                        intent.putExtra("page2", replaceStr("PHOTODESCRIPTION", Constant.NET_PAGE_TEXT, mPhotoDes));
                    }
                    if(mPhotoUri != null) {
                        String content1 = "<itemref idref='page1'/>";
                        if(mPhotoDes != null) {
                            content1 += "<itemref idref='page2'/>";
                        }
                        intent.putExtra("content", replaceStr("PHOTOCONTENT", Constant.NET_CONTENT, content1));
                    } else {
                        String content1 = "<itemref idref='page1'/>";
                        intent.putExtra("content", replaceStr("PHOTOCONTENT", Constant.NET_CONTENT, content1));
                    }
                    mContext.startActivity(intent);
                    dialog.dismiss();
                } catch (ActivityNotFoundException e) {
                    new NetPrintDialog(mContext, NetPrintDialog.TOLOT).showDialog();
                    dialog.dismiss();
                }
            }
        });

        btn_back.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
    private String replaceStr(String replaced, String old, String newStr) {
        Pattern pattern = Pattern.compile(replaced);
        Matcher matcher = pattern.matcher(old);
        return matcher.replaceAll(newStr);
    }

    static class Constant {
        public static final String NET_INFO =
                "<?xml version='1.0' encoding='UTF-8'?>" +
                "<info>" +
                    "<version>1.0</version>" +
                    "<devCode>574985d1e818</devCode>" +
                    "<devName>ソースネクスト株式会社</devName>" +
                    "<appCode>b1e5b27c097e</appCode>" +
                    "<appName>万能カメラ</appName>" +
                "</info>";

        public static final String NET_PAGE_PHOTO =
                "<?xml version='1.0' encoding='utf-8' standalone='no'?>"+
                "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.1//EN' 'http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd'>" +
                "<html>" +
                    "<head>" + "</head>"+
                    "<body class='tolot' data-page-type='page' data-item-lock='false'>" +
                        "<article class='tolot' data-tag-type='item' data-item-type='image'><img src='PHOTOURI' /></article>" +
                    "</body>"+
                "</html>";

        public static final String NET_PAGE_TEXT =
                "<?xml version='1.0' encoding='utf-8' standalone='no'?>"+
                "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.1//EN' 'http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd'>" +
                "<html>" +
                    "<head>" + "</head>" +
                    "<body class='tolot' data-page-type='page' data-item-lock='false'>" +
                        "<article class='tolot' data-tag-type='item' data-item-type='text'>PHOTODESCRIPTION</article>" +
                    "</body>" +
                "</html>";

        public static final String NET_CONTENT =
                "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
                "<package xmlns='http://www.idpf.org/2007/opf' prefix='cc: http://creativecommons.org/ns# rendition: http://www.idpf.org/vocab/rendition/#' unique-identifier='BookId' version='2.0'>" +
                    "<spine toc='ncx'>" +
                        "PHOTOCONTENT" +
                    "</spine>" +
                "</package>";

        public static final String NET_BOOK =
                "<?xml version='1.0' encoding='UTF-8'?>" +
                "<book>" +
                    "<title>タイトル</title>" +
                    "<subTitle>サブタイトル</subTitle>" +
                    "<author>作成者名</author>"+
                    "<description>フォトブックの説明</description>" +
                    "<createDate>CREATETIME</createDate>" +
                    "<themeCode>1025</themeCode>" +
                "</book>";

        public static final String NET_PAGE_BLANK =
                "<?xml version='1.0' encoding='utf-8' standalone='no'?>" +
                "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.1//EN' 'http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd'>" +
                "<html>" +
                    "<head>"+ "</head>" +
                    "<body class='tolot' data-page-type='blankPage' data-item-lock='false'>" +
                    "</body>"+
                "</html>";
    }
}
