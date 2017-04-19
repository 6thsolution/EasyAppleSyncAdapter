/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.sixthsolution.lpisyncadapter.util;

import android.accounts.Account;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.sixthsolution.lpisyncadapter.App;
import com.sixthsolution.lpisyncadapter.BuildConfig;
import com.sixthsolution.lpisyncadapter.exceptions.InvalidAccountException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import at.bitfire.dav4android.BasicDigestAuthHandler;
import at.bitfire.dav4android.UrlUtils;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class HttpClient {
    private static final OkHttpClient client = new OkHttpClient();
    private static final UserAgentInterceptor userAgentInterceptor = new UserAgentInterceptor();

    private static final String userAgent;

    static {
        String date = new SimpleDateFormat("yyyy/MM/dd", Locale.US).format(new Date(BuildConfig.buildTime));
        userAgent = "sixthsolution/" +
                BuildConfig.VERSION_NAME +
                " (" +
                date +
                "; dav4android; okhttp3) Android/" +
                Build.VERSION.RELEASE;
    }

    private HttpClient() {
    }

    public static OkHttpClient create(@NonNull Context context, @NonNull Account account,
                                      @NonNull HttpLoggingInterceptor logging) throws InvalidAccountException {
        OkHttpClient.Builder builder = defaultBuilder(context);

        // use account settings for authentication
        AccountSettings settings = new AccountSettings(context, account);
        builder = addAuthentication(builder, null, settings.username(), settings.password());

        // Set logger
        builder.addInterceptor(logging);

        return builder.build();
    }

    private static OkHttpClient.Builder defaultBuilder(@Nullable Context context) {
        OkHttpClient.Builder builder = client.newBuilder();

        // use MemorizingTrustManager to manage self-signed certificates
        if (context != null) {
            App app = (App) context.getApplicationContext();
            if (App.sslSocketFactoryCompat != null && app.certManager != null) {
                builder.sslSocketFactory(App.sslSocketFactoryCompat, app.certManager);
            }
            if (App.hostnameVerifier != null) {
                builder.hostnameVerifier(App.hostnameVerifier);
            }
        }

        // set timeouts
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(120, TimeUnit.SECONDS);

        // don't allow redirects, because it would break PROPFIND handling
        builder.followRedirects(false);

        // add User-Agent to every request
        builder.addNetworkInterceptor(userAgentInterceptor);

        // add cookie store for non-persistent cookies (some services like Horde use cookies for session tracking)
        builder.cookieJar(new MemoryCookieStore());

        return builder;
    }

    private static OkHttpClient.Builder addAuthentication(@NonNull OkHttpClient.Builder builder,
                                                          @Nullable String host, @NonNull String username,
                                                          @NonNull String password) {
        BasicDigestAuthHandler authHandler =
                new BasicDigestAuthHandler(UrlUtils.hostToDomain(host), username, password);
        return builder
                .addNetworkInterceptor(authHandler)
                .authenticator(authHandler);
    }

    public static OkHttpClient addAuthentication(@NonNull OkHttpClient client, @NonNull String username,
                                                 @NonNull String password) {
        OkHttpClient.Builder builder = client.newBuilder();
        addAuthentication(builder, null, username, password);
        return builder.build();
    }

    public static OkHttpClient addAuthentication(@NonNull OkHttpClient client, @NonNull String host,
                                                 @NonNull String username, @NonNull String password) {
        OkHttpClient.Builder builder = client.newBuilder();
        addAuthentication(builder, host, username, password);
        return builder.build();
    }


    static class UserAgentInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Locale locale = Locale.getDefault();
            Request request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .header("Accept-Language", locale.getLanguage() +
                            "-" +
                            locale.getCountry() +
                            ", " +
                            locale.getLanguage() +
                            ";q=0.7, *;q=0.5")
                    .build();
            return chain.proceed(request);
        }
    }

}
