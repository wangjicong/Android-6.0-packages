/*
 * Copyright (C) 2011,2012 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.sns;

import android.os.Build;

public abstract class Models {

    private static final String sModelName;
    static {
        sModelName = Build.MODEL.replace('-', '_').replace(' ', '_');
    }

    private Models() {
    }

    public static String getModel() {
        return sModelName;
    }

    // ////////////////////////////////////////////////////////////////////////////////////
    // DETAIL MODELs
    // ////////////////////////////////////////////////////////////////////////////////////
    public static final String SN_IS11CA = "IS11CA";
    public static final String SN_IS11N = "IS11N";
    public static final String SN_N06C  = "N06C";
    public static final String SN_N_03E = "N_03E";
    public static final String SN_N_04D = "N_04D";
    public static final String SN_N_04C = "N_04C";
    public static final String SN_N_06C = "N_06C";
    public static final String SN_N_07D = "N_07D";
    public static final String Oppo_X907             = "X907";

    public static final String Sony_IS12S            = "IS12S";
    public static final String SN_SHL21              = "SHL21";
    public static final String SN_ISW16SH            = "ISW16SH";
    public static final String SN_P_04D              = "P_04D";
    public static final String SN_INFOBAR_A01        = "INFOBAR_A01";
    public static final String SN_IS11SH             = "IS11SH";
    public static final String Samsung_GT_I9000      = "GT_I9000";
    public static final String Samsung_SC_02B        = "SC_02B"; //GT-I9000
    public static final String SN_SH_01D             = "SH_01D";
    public static final String SN_SH_02E             = "SH_02E";
    public static final String SN_F_07D              = "F_07D";
    public static final String SN_SH_09D             = "SH_09D";
    public static final String SN_SH_12C             = "SH_12C";
    public static final String SN_SO_01C             = "SO_01C";
    public static final String Inhon_MA86            = "Inhon_MA86";
    public static final String Inhon_Xhuriken_EVDO   = "INHON_Xhuriken_EVDO";
}
