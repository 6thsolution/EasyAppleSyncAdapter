package com.sixthsolution.lpisyncadapter.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.sixthsolution.lpisyncadapter.GlobalConstant;
import com.sixthsolution.lpisyncadapter.entitiy.BaseCalendarData;
import com.sixthsolution.lpisyncadapter.entitiy.BaseCollectionInfo;
import com.sixthsolution.lpisyncadapter.exceptions.InvalidAccountException;
import com.sixthsolution.lpisyncadapter.resource.LocalCalendar;

import java.util.Map;

import at.bitfire.ical4android.CalendarStorageException;
import okhttp3.HttpUrl;

/**
 * @author mehdok (mehdok@gmail.com) on 4/6/2017.
 */

public abstract class BaseSyncAdapter<T extends BaseCalendarData, V extends BaseCollectionInfo>
        extends AbstractThreadedSyncAdapter {

    protected AccountManager accountManager;
    protected SyncServerHandler serverHandler;

    public BaseSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public BaseSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        // required for dav4android (ServiceLoader)
        Thread.currentThread().setContextClassLoader(getContext().getClassLoader());

        try {
            syncLocalAndRemoteCollections(account, provider);
            syncCalendarsEvents(account, provider, extras);

            // notify any registered caller that sync operation is finished
            getContext().getContentResolver().notifyChange(GlobalConstant.CONTENT_URI, null, false);
        } catch (InvalidAccountException | CalendarStorageException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get local and remote collection list and sync them
     *
     * @param provider
     */
    protected abstract void syncLocalAndRemoteCollections(Account account, ContentProviderClient provider)
            throws CalendarStorageException;

    /**
     * @return list of collections that stored in local storage (Android provider)
     */
    protected abstract LocalCalendar[] getLocalCalendarList(Account account, ContentProviderClient provider)
            throws CalendarStorageException;

    /**
     * @return list of remote collections by querying server
     */
    protected abstract T getRemoteCollectionList(Account account);

    /**
     * Compare local calendars and remote collections to find the difference then add or remove collections
     * from local list
     *
     * @param account
     * @param local
     * @param remote
     * @param provider
     * @throws CalendarStorageException
     */
    protected abstract void updateLocalCalendars(Account account,
                                                 LocalCalendar[] local,
                                                 Map<HttpUrl, V> remote,
                                                 ContentProviderClient provider)
            throws CalendarStorageException;

    /**
     * Get list of collections from local storage and sync their data with server
     *
     * @param provider
     */
    protected abstract void syncCalendarsEvents(Account account, ContentProviderClient provider, Bundle extras)
            throws InvalidAccountException, CalendarStorageException;


}
