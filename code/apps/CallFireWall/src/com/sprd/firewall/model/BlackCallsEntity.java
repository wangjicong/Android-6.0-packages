
package com.sprd.firewall.model;

public class BlackCallsEntity {

    private Integer BlackCallsID;

    private String BlackCallsNumber;

    private String BlackCallsType;

    private String BlackRecoderDate;

    // private String Link;

    public BlackCallsEntity()
    {

    }

    public BlackCallsEntity(Integer blackCallsID, String blackCallsNumber,
            String blackCallsType, String blackRecoderDate) {
        super();
        BlackCallsID = blackCallsID;
        BlackCallsNumber = blackCallsNumber;
        BlackCallsType = blackCallsType;
        BlackRecoderDate = blackRecoderDate;
    }

    public String getBlackRecoderDate() {
        return BlackRecoderDate;
    }

    public void setBlackRecoderDate(String blackRecoderDate) {
        BlackRecoderDate = blackRecoderDate;
    }

    public Integer getBlackCallsID() {
        return BlackCallsID;
    }

    public void setBlackCallsID(Integer blackCallsID) {
        BlackCallsID = blackCallsID;
    }

    public String getBlackCallsNumber() {
        return BlackCallsNumber;
    }

    public void setBlackCallsNumber(String blackCallsNumber) {
        BlackCallsNumber = blackCallsNumber;
    }

    public String getBlackCallsType() {
        return BlackCallsType;
    }

    public void setBlackCallsType(String blackCallsType) {
        BlackCallsType = blackCallsType;
    }

}
