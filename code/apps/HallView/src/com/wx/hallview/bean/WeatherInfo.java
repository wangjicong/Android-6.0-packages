package com.wx.hallview.bean;

/**
 * Created by Administrator on 16-1-23.
 */

public class WeatherInfo
{
    private String countyName;
    private String date;
    private String lastRefreshDate;
    private String temp;
    private String text;

    public String getCountyName()
    {
        return this.countyName;
    }

    public String getDate()
    {
        return this.date;
    }

    public String getLastRefreshDate()
    {
        return this.lastRefreshDate;
    }

    public String getTemp()
    {
        return this.temp;
    }

    public String getText()
    {
        return this.text;
    }

    public void setCountyName(String paramString)
    {
        this.countyName = paramString;
    }

    public void setDate(String paramString)
    {
        this.date = paramString;
    }

    public void setLastRefreshDate(String paramString)
    {
        this.lastRefreshDate = paramString;
    }

    public void setTemp(String paramString)
    {
        this.temp = paramString;
    }

    public void setText(String paramString)
    {
        this.text = paramString;
    }

    public String toString()
    {
        return "WeatherInfo [countyName=" + this.countyName + ", temp=" + this.temp + ", text=" + this.text + ", date=" + this.date + ", lastRefreshDate=" + this.lastRefreshDate + "]";
    }
}

