/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services;

import android.util.Log;
import android.util.SparseArray;

import com.ucamera.ucomm.sns.services.impl.FacebookService;
import com.ucamera.ucomm.sns.services.impl.FlickrService;
import com.ucamera.ucomm.sns.services.impl.KaixinService;
import com.ucamera.ucomm.sns.services.impl.MixiService;
import com.ucamera.ucomm.sns.services.impl.QQVatarService;
import com.ucamera.ucomm.sns.services.impl.QZoneService;
import com.ucamera.ucomm.sns.services.impl.RenrenService;
import com.ucamera.ucomm.sns.services.impl.SinaService;
import com.ucamera.ucomm.sns.services.impl.SohuService;
import com.ucamera.ucomm.sns.services.impl.TencentService;
import com.ucamera.ucomm.sns.services.impl.TumblrService;
import com.ucamera.ucomm.sns.services.impl.TwitterService;

/**
 * NOTE: This class is not thread safe, please use carefully!
 */
public class ServiceProvider {

    private static final String TAG = "ServiceProvider";

    public static final int SERVICE_FACEBOOK    = 0x01;
    public static final int SERVICE_FLICKR      = 0x02;
    public static final int SERVICE_KAIXIN      = 0x03;
    public static final int SERVICE_RENREN      = 0x04;
    public static final int SERVICE_SINA        = 0x05;
    public static final int SERVICE_SOHU        = 0x06;
    public static final int SERVICE_TENCENT     = 0x07;
    public static final int SERVICE_QZONE       = 0x08;
    public static final int SERVICE_TWITTER     = 0x09;
    public static final int SERVICE_MIXI        = 0x0A;
    public static final int SERVICE_TUMBLR      = 0x0B;
    public static final int SERVICE_QQVATAR      = 0x0C;

    public static final int MIN_USER_SERVICE_ID = 0x100;
    private static ServiceProvider sInstance;

    public static ServiceProvider getProvider() {
        if (sInstance == null) {
            synchronized (ServiceProvider.class) {
                sInstance = new ServiceProvider();
            }
        }
        return sInstance;
    }

    private SparseArray<Class<? extends ShareService>> mServiceRegistry;
    // maybe should use weak refs.
    private SparseArray<ShareService> mServices;

    private ServiceProvider() {
        mServices = new SparseArray<ShareService>();
        mServiceRegistry = new SparseArray<Class<? extends ShareService>>(8);
        mServiceRegistry.put(SERVICE_FACEBOOK,  FacebookService.class);
        mServiceRegistry.put(SERVICE_FLICKR,    FlickrService.class);
        mServiceRegistry.put(SERVICE_KAIXIN,    KaixinService.class);
        mServiceRegistry.put(SERVICE_RENREN,    RenrenService.class);
        mServiceRegistry.put(SERVICE_SINA,      SinaService.class);
        mServiceRegistry.put(SERVICE_SOHU,      SohuService.class);
        mServiceRegistry.put(SERVICE_QZONE,     QZoneService.class);
        mServiceRegistry.put(SERVICE_TENCENT,   TencentService.class);
        mServiceRegistry.put(SERVICE_TWITTER,   TwitterService.class);
        mServiceRegistry.put(SERVICE_MIXI,      MixiService.class);
        mServiceRegistry.put(SERVICE_TUMBLR,    TumblrService.class);
        mServiceRegistry.put(SERVICE_QQVATAR,   QQVatarService.class);
    }

    public final void register(int id, Class<? extends ShareService> serviceClass) {
        checkIdExistence(id);
        mServiceRegistry.put(id, serviceClass);
    }

    private void checkIdExistence(int id) {
        final Class<? extends ShareService> service = mServiceRegistry.get(id);
        if (service != null) {
            StringBuilder error = new StringBuilder()
                    .append("Service id ").append(id)
                    .append(" already occupied by ").append(service.getName());
            throw new ServiceRegisterException(error.toString());
        }
    }

    public ShareService getService(int serviceId) {
        ShareService service = mServices.get(serviceId);

        if (service != null) {
            return service;
        }

        Class<? extends ShareService> claz = mServiceRegistry.get(serviceId);
        if (claz != null) {
            try {
                service = claz.newInstance();
                mServices.put(serviceId, service);
            } catch (Exception e) {
                StringBuilder error = new StringBuilder()
                        .append("service id ").append(serviceId)
                        .append(" definded as ").append(claz.getName()).append(".")
                        .append("But can not constucted. Remove it!");
                Log.e(TAG, error.toString());
                mServices.delete(serviceId);
            }
        }

        if (service != null) {
            return service;
        }

        StringBuilder error = new StringBuilder()
                .append("Service for id ").append(serviceId)
                .append(" is not found!");
        throw new ServiceNotFoundException(error.toString());
    }
}
