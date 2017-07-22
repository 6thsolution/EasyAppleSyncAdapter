package com.sixthsolution.lpisyncadapter.authenticator;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.sixthsolution.lpisyncadapter.GlobalConstant.AUTHTOKEN_TYPE_FULL_ACCESS;

/**
 * This service bind to {@link ICalAuthenticator}
 *
 * @author mehdok (mehdok@gmail.com) on 3/9/2017.
 */

public class ICalAuthenticatorService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Class clazz = getLoginClass();
        String authType = getAuthenticationType();

        ICalAuthenticator authenticator =
                new ICalAuthenticator(this,
                                      clazz != null ? clazz : LoginActivity.class,
                                      authType != null ? authType : AUTHTOKEN_TYPE_FULL_ACCESS);
        return authenticator.getIBinder();
    }

    /**
     * @return custom login class that passed via meta-data
     */
    private Class getLoginClass() {
        ComponentName service = new ComponentName(this, this.getClass());
        try {
            Bundle data = getPackageManager().getServiceInfo(service, PackageManager.GET_META_DATA).metaData;
            String clazzName = data.getString("login_activity_class");

            if (clazzName != null) {
                Log.e("getLoginClass", "clazzName: " + clazzName);
                return (Class) Class.forName(clazzName);
            } else {
                Log.e("getLoginClass", "clazzName is null");
                return null;
            }
        } catch (PackageManager.NameNotFoundException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return unique authentication type that passed via meta-data
     */
    private String getAuthenticationType() {
        ComponentName service = new ComponentName(this, this.getClass());
        try {
            Bundle data = getPackageManager().getServiceInfo(service, PackageManager.GET_META_DATA).metaData;
            return data.getString("unique_authentication_type");

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
