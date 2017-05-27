package com.wx.hallview.bean;

/**
 * Created by Administrator on 16-1-23.
 */
public class NotReadMessage
{
    private String mAddress;
    private String mBody;
    private String mDate;
    private String mPerson;
    private String mPhoto_uri;
    private int mRead;
    private int m_id;

    public String getmAddress()
    {
        return this.mAddress;
    }

    public String getmBody()
    {
        return this.mBody;
    }

    public String getmPerson()
    {
        return this.mPerson;
    }

    public String getmPhoto_uri()
    {
        return this.mPhoto_uri;
    }

    public void setmAddress(String paramString)
    {
        this.mAddress = paramString;
    }

    public void setmBody(String paramString)
    {
        this.mBody = paramString;
    }

    public void setmPerson(String paramString)
    {
        this.mPerson = paramString;
    }

    public void setmPhoto_uri(String paramString)
    {
        this.mPhoto_uri = paramString;
    }

    public String toString()
    {
        return "NotReadMessage [mRead=" + this.mRead + ", m_id=" + this.m_id + ", mAddress=" + this.mAddress + ", mPerson=" + this.mPerson + ", mBody=" + this.mBody + ", mDate=" + this.mDate + "]";
    }
}

