package com.sixthsolution.lpisyncadapter.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * A simple bind to sync adapter
 *
 * @author mehdok (mehdok@gmail.com) on 3/18/2017.
 */

public class ICalSyncService extends Service {
    private static final Object syncAdapterLock = new Object();
    private static ICalSyncAdapter syncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (syncAdapterLock) {
            if (syncAdapter == null) {
                syncAdapter = new ICalSyncAdapter(getApplicationContext(), true);
            }
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
