package com.sixthsolution.lpisyncadapter.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.sixthsolution.lpisyncadapter.entitiy.CalendarData;
import com.sixthsolution.lpisyncadapter.entitiy.CollectionInfo;
import com.sixthsolution.lpisyncadapter.exceptions.InvalidAccountException;
import com.sixthsolution.lpisyncadapter.resource.LocalCalendar;
import com.sixthsolution.lpisyncadapter.util.HttpClient;

import java.util.Map;

import at.bitfire.ical4android.CalendarStorageException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import static com.sixthsolution.lpisyncadapter.util.Util.getHttpLogger;


/**
 * The syncAdapter, after executing {@link #onPerformSync} it will get user apple id from the authenticator,
 * ask the server for any new data, get the local data via {@link ContentProviderClient}, then compare them and
 * run queries to update server or local storage.
 *
 * @author mehdok (mehdok@gmail.com) on 3/18/2017.
 */

public class ICalSyncAdapter extends BaseSyncAdapter<CalendarData, CollectionInfo> {

    public ICalSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        accountManager = AccountManager.get(context);
        serverHandler = new SyncServerHandlerImpl();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        super.onPerformSync(account, extras, authority, provider, syncResult);
    }

    @Override
    protected void syncLocalAndRemoteCollections(Account account, ContentProviderClient provider)
            throws CalendarStorageException {
        LocalCalendar[] localCalendarData = getLocalCalendarList(account, provider);
        CalendarData remoteCalendarData = getRemoteCollectionList(account);
        updateLocalCalendars(account, localCalendarData, remoteCalendarData.getCollections(), provider);
    }

    @Override
    protected LocalCalendar[] getLocalCalendarList(Account account, ContentProviderClient provider)
            throws CalendarStorageException {
        return (LocalCalendar[]) LocalCalendar.find(account, provider, LocalCalendar.Factory.INSTANCE, null, null);
    }

    @Override
    protected CalendarData getRemoteCollectionList(Account account) {
        return serverHandler.getServerCalendar(getContext(), account);
    }

    @Override
    protected void updateLocalCalendars(Account account,
                                        LocalCalendar[] local,
                                        Map<HttpUrl, CollectionInfo> remote,
                                        ContentProviderClient provider)
            throws CalendarStorageException {
        // delete obsolete local calendar
        for (LocalCalendar calendar : local) {
            String url = calendar.getName();
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (!remote.containsKey(httpUrl)) {
                calendar.delete();
            } else {
                // remote CollectionInfo found for this local collection, update data
                CollectionInfo info = remote.get(httpUrl);
                calendar.update(info, true);
                // we already have a local calendar for this remote collection, don't take into consideration anymore
                remote.remove(httpUrl);
            }
        }

        // create new local calendars
        for (HttpUrl url : remote.keySet()) {
            CollectionInfo info = remote.get(url);
            LocalCalendar.create(account, provider, info);
        }
    }

    @Override
    protected void syncCalendarsEvents(Account account, ContentProviderClient provider, Bundle extras)
            throws InvalidAccountException, CalendarStorageException {
        LocalCalendar[] localCalendar = getLocalCalendarList(account, provider);

        OkHttpClient httpClient = HttpClient.create(getContext(), account, getHttpLogger());
        //for every collection get the data from server
        for (LocalCalendar calendar : localCalendar) {
            SyncManager syncManager = new SyncManager(httpClient, calendar, extras);
            syncManager.performSync();
        }
    }
}
