/*
 * Copyright (C) 2011,2013 Thundersoft Corporation
 * All rights Reserved
 */

package com.ucamera.ucomm.sns;

import java.math.BigDecimal;

public class ConvertGPS {
    private static final int KEEP_NUM = 4;
    public static String convertToText(double latlon) {
        int degree = (int)Math.floor(Math.abs(latlon));
        double decimalPart = getDecimalPart(latlon);
        double temp = decimalPart * 60;
        int min = (int) Math.floor(temp);
        double second = getDecimalPart(temp) * 60;
        second = getDecimalNum(KEEP_NUM, second);
        int multiple = (int) Math.pow(10, KEEP_NUM);
        int intSecond = (int) (second * multiple);
        return new StringBuffer().append(degree).append("/1,").append(min).append("/1,")
                                 .append(intSecond).append("/").append(multiple).toString();
    }

    public static double convertToDecimal(String latlon) {
        if(latlon != null && latlon != "") {
            String[] strs = latlon.split(",");
            /**
             * FIX BUG: 5827
             * BUG CAUSE: ArrayIndexOutOfBoundsException
             * DATE:2014-02-13
             */
            if(strs.length >= 3) {
                double degree = 0;
                double min = 0;
                double sFront = 0;
                double sBack = 0;
                double second = 0;
                int index = strs[0].indexOf("/");
                if(index != -1) {
                    degree = Double.parseDouble(strs[0].substring(0, index));
                }
                index = strs[1].indexOf("/");
                if(index != -1) {
                    min = Double.parseDouble(strs[1].substring(0, index));
                }
                index = strs[2].indexOf("/");
                if(index != -1) {
                    sFront = Double.parseDouble(strs[2].substring(0, index));
                    sBack = Double.parseDouble(strs[2].substring(index + 1));
                    second = sFront / sBack;
                }
                return getDecimalNum(KEEP_NUM, degree + (min + (second / 60)) / 60);
            }

        }

        return 0;
    }

    public static double getDecimalPart(double latlon) {
        double tmp = latlon;
        int intNum = (int) tmp;
        BigDecimal whole = new BigDecimal(Double.toString(tmp));
        BigDecimal intNum2 = new BigDecimal(Integer.toString(intNum));
        return whole.subtract(intNum2).floatValue();
    }

    private static double getDecimalNum(int keepNum, double decimalNum) {
        BigDecimal b = new BigDecimal(decimalNum);
        return b.setScale(keepNum, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
