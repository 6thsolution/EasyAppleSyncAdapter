package com.sixthsolution.lpisyncadapter;

import android.net.Uri;

/**
 * @author mehdok (mehdok@gmail.com) on 3/18/2017.
 */

public class GlobalConstant {
    public static final Uri BASE_URL = Uri.parse("https://p01-caldav.icloud.com");

    public final static String ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String AUTH_TYPE = "AUTH_TYPE";
    public final static String IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public final static String ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String PARAM_USER_PASS = "USER_PASS";
    public final static String PARAM_PRINCIPAL = "USER_PRINCIPAL";

    // authentication unique key, this must be different for every app
    public static String AUTHTOKEN_TYPE_FULL_ACCESS = "com.sixthsolution.lpisyncadapter.ical_access";

    // content provider unique authority, this must be unique for every app
    public static String AUTHORITY = "com.android.calendar";
    public static Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/ical");
}
