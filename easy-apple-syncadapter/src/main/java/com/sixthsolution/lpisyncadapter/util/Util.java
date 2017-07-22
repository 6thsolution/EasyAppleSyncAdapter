package com.sixthsolution.lpisyncadapter.util;


import com.sixthsolution.lpisyncadapter.BuildConfig;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * @author mehdok (mehdok@gmail.com) on 4/5/2017.
 */

public class Util {
    public static HttpLoggingInterceptor getHttpLogger() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        return logging;
    }
}
