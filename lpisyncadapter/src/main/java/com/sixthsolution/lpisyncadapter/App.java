package com.sixthsolution.lpisyncadapter;

import android.app.Application;

import com.sixthsolution.lpisyncadapter.util.SSLSocketFactoryCompat;

import net.fortuna.ical4j.util.UidGenerator;

import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;

import at.bitfire.cert4android.CustomCertManager;
import okhttp3.internal.tls.OkHostnameVerifier;
import timber.log.Timber;

/**
 * @author mehdok (mehdok@gmail.com) on 3/29/2017.
 */

public class App extends Application {
    public static SSLSocketFactoryCompat sslSocketFactoryCompat;
    public static HostnameVerifier hostnameVerifier;
    public CustomCertManager certManager;
    public static UidGenerator uidGenerator;

    // comment this if you don't need any logs
    static {
        at.bitfire.dav4android.Constants.log = Logger.getLogger("davdroid.dav4android");
        at.bitfire.cert4android.Constants.log = Logger.getLogger("davdroid.cert4android");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        reinitCertManager();
        uidGenerator = new UidGenerator(null, android.provider.Settings.Secure.getString(getContentResolver(),
                                                                                         android.provider.Settings.Secure.ANDROID_ID));

        Timber.plant(new Timber.DebugTree());
    }

    public void reinitCertManager() {
        if (BuildConfig.customCerts) {
            if (certManager != null) {
                certManager.close();
            }

            certManager = new CustomCertManager(this, true);
            sslSocketFactoryCompat = new SSLSocketFactoryCompat(certManager);
            hostnameVerifier = certManager.hostnameVerifier(OkHostnameVerifier.INSTANCE);
        }
    }
}
