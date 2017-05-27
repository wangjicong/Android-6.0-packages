package com.android.messaging;

import android.os.Bundle;
import android.support.v7.mms.CarrierConfigValuesLoader;
import android.view.View;

import com.android.messaging.sms.MmsConfig;
import com.android.messaging.sms.SystemProperties;


public abstract class OperatorFactory {
    public static ISetParameter CreateInstance()

    {
      String OperatorName = SystemProperties.get("ro.messaging.operator");
      if("telcel".equals(OperatorName)){
        return  new TelcelImpl();
      }
      else{
        return new DefaultImpl();
      }
    }
    public static void setParamter(Bundle value) {
        CreateInstance().setParamter(value);
    }
    public static void setViewEnabled(View view) {
        CreateInstance().setViewEnabled(view);
    }
}
interface ISetParameter
{
    public void setParamter(Bundle value);
    public void setViewEnabled(View view);
}

class DefaultImpl implements ISetParameter
{
    public void setParamter(Bundle value) {
    }
    public void setViewEnabled(View view) {
    }
}

class TelcelImpl implements ISetParameter
{
    private static final int CONFIG_TELCEL_MAX_MESSAGE_SIZE_DEFAULT = 1024 * 1024;
    public void setParamter(Bundle value)
    {
        value.putInt(CarrierConfigValuesLoader.CONFIG_MAX_MESSAGE_SIZE, CONFIG_TELCEL_MAX_MESSAGE_SIZE_DEFAULT);
    }
    public void setViewEnabled(View view) {
        view.setEnabled(false);
    }
}