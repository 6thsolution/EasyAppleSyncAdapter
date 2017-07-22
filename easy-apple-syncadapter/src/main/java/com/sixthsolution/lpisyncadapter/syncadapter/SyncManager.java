package com.sixthsolution.lpisyncadapter.syncadapter;

import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;

import com.sixthsolution.lpisyncadapter.download.EventDownloader;
import com.sixthsolution.lpisyncadapter.resource.LocalCalendar;
import com.sixthsolution.lpisyncadapter.resource.LocalCollection;
import com.sixthsolution.lpisyncadapter.resource.LocalEvent;
import com.sixthsolution.lpisyncadapter.resource.LocalResource;
import com.sixthsolution.lpisyncadapter.util.DavResourceFinder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import at.bitfire.dav4android.DavCalendar;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.exception.ConflictException;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.exception.PreconditionFailedException;
import at.bitfire.dav4android.property.GetCTag;
import at.bitfire.dav4android.property.GetETag;
import at.bitfire.ical4android.CalendarStorageException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import timber.log.Timber;

/**
 * @author mehdok (mehdok@gmail.com) on 4/12/2017.
 */

public class SyncManager {
    protected HttpUrl collectionURL;
    protected DavResource davCollection;
    protected LocalCollection localCollection;
    protected OkHttpClient httpClient;
    protected Bundle extras;

    /** remote CTag at the time of {@link #getDownloadList(Map, Map)} */
    protected String remoteCTag = null;

    public SyncManager(OkHttpClient httpClient, LocalCollection localCollection, Bundle extras) {
        this.httpClient = httpClient;
        this.localCollection = localCollection;
        this.extras = extras;
    }

    public void performSync() {
        try {
            prepare();
            queryCapabilities();
            processLocallyDeleted();
            prepareDirty();
            uploadDirty();

            if (checkSyncState()) {
                Map<String, LocalResource> localResources = listLocal();

                LocalCalendar calendar = localCalendar();
                Map<String, DavResource> remoteCollectionEvents =
                        DavResourceFinder.getCollectionEvents(httpClient, calendar.getName(),
                                                              true, true, null, null);

                Set<DavResource> toDownload = getDownloadList(remoteCollectionEvents, localResources);

                DavCalendar davCalendar = new DavCalendar(httpClient, HttpUrl.parse(calendar.getName()));
                EventDownloader.downloadEvents(toDownload, davCalendar, localCalendar(), localResources);

                saveSyncState();
            }

        } catch (DavException | IOException | HttpException | CalendarStorageException e) {
            e.printStackTrace();
        }
    }

    protected boolean prepare() {
        collectionURL = HttpUrl.parse(localCalendar().getName());
        davCollection = new DavCalendar(httpClient, collectionURL);
        return true;
    }

    protected void queryCapabilities() throws DavException, IOException, HttpException {
        davCollection.propfind(0, GetCTag.NAME);
    }

    /**
     * Process locally deleted entries (DELETE them on the server as well).
     * Checks Thread.interrupted() before each request to allow quick sync cancellation.
     */
    protected void processLocallyDeleted() throws CalendarStorageException {
        // Remove locally deleted entries from server (if they have a name, i.e. if they were uploaded before),
        // but only if they don't have changed on the server. Then finally remove them from the local address book.
        LocalResource[] localList = localCollection.getDeleted();
        for (final LocalResource local : localList) {
            if (Thread.interrupted()) {
                return;
            }

            final String fileName = local.getFileName();
            if (!TextUtils.isEmpty(fileName)) {

                final DavResource remote =
                        new DavResource(httpClient, collectionURL.newBuilder().addPathSegment(fileName).build());
                try {
                    remote.delete(local.getETag());
                } catch (IOException | HttpException e) {
                    Timber.w("Couldn't delete " + fileName + " from server; ignoring (may be downloaded again)");
                }
            } else {
                local.delete();
            }
        }
    }

    protected void prepareDirty() throws CalendarStorageException {
        // assign file names and UIDs to new contacts so that we can use the file name as an index
        for (final LocalResource local : localCollection.getWithoutFileName()) {
            local.prepareForUpload();
        }

        localCalendar().processDirtyExceptions();
    }

    /**
     * Uploads dirty records to the server, using a PUT request for each record.
     * Checks Thread.interrupted() before each request to allow quick sync cancellation.
     */
    protected void uploadDirty() throws IOException, HttpException, CalendarStorageException {
        // upload dirty contacts
        for (final LocalResource local : localCollection.getDirty()) {
            if (Thread.interrupted()) {
                return;
            }

            final String fileName = local.getFileName();
            final DavResource remote =
                    new DavResource(httpClient, collectionURL.newBuilder().addPathSegment(fileName).build());

            // generate entity to upload (VCard, iCal, whatever)
            RequestBody body = prepareUpload(local);

            try {
                if (local.getETag() == null) {
                    remote.put(body, null, true);
                } else {
                    remote.put(body, local.getETag(), false);
                }
            } catch (ConflictException | PreconditionFailedException e) {
                // we can't interact with the user to resolve the conflict, so we treat 409 like 412
                Timber.w("Resource has been modified on the server before upload, ignoring", e);
            }

            String eTag = null;
            GetETag newETag = (GetETag) remote.properties.get(GetETag.NAME);
            if (newETag != null) {
                eTag = newETag.eTag;
            } else {
                Timber.d("Didn't receive new ETag after uploading, setting to null");
            }

            local.clearDirty(eTag);
        }
    }

    protected RequestBody prepareUpload(LocalResource resource) throws IOException, CalendarStorageException {
        LocalEvent local = (LocalEvent) resource;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        local.getEvent().write(os);

        return RequestBody.create(
                DavCalendar.MIME_ICALENDAR,
                os.toByteArray()
        );
    }

    protected boolean checkSyncState() throws CalendarStorageException {
        // check CTag (ignore on manual sync)
        GetCTag getCTag = (GetCTag) davCollection.properties.get(GetCTag.NAME);
        if (getCTag != null) {
            remoteCTag = getCTag.cTag;
        }

        String localCTag = null;
        if (extras.containsKey(ContentResolver.SYNC_EXTRAS_MANUAL)) {
            Timber.d("Manual sync, ignoring CTag");
        } else {
            localCTag = localCollection.getCTag();
        }

        if (remoteCTag != null && remoteCTag.equals(localCTag)) {
            return false;
        } else {
            return true;
        }
    }

    protected Map<String, LocalResource> listLocal() throws CalendarStorageException {
        // fetch list of local contacts and build hash table to index file name
        LocalResource[] localList = localCollection.getAll();
        Map<String, LocalResource> localResources = new HashMap<>(localList.length);
        for (LocalResource resource : localList) {
            localResources.put(resource.getFileName(), resource);
        }

        return localResources;
    }

    protected Set<DavResource> getDownloadList(Map<String, DavResource> remoteResources,
                                               Map<String, LocalResource> localResources)
            throws DavException, CalendarStorageException {
        Set<DavResource> toDownload = new HashSet<>();

        for (String localName : localResources.keySet()) {
            final DavResource remote = remoteResources.get(localName);

            if (remote == null) {
                final LocalResource local = localResources.get(localName);
                local.delete();
            } else {
                // contact is still on server, check whether it has been updated remotely
                GetETag getETag = (GetETag) remote.properties.get(GetETag.NAME);
                if (getETag == null || getETag.eTag == null) {
                    throw new DavException("Server didn't provide ETag");
                }
                String localETag = localResources.get(localName).getETag(),
                        remoteETag = getETag.eTag;
                if (!remoteETag.equals(localETag)) {
                    toDownload.add(remote);
                }

                // remote entry has been seen, remove from list
                remoteResources.remove(localName);
            }
        }

        // add all unseen (= remotely added) remote contacts
        if (!remoteResources.isEmpty()) {
            toDownload.addAll(remoteResources.values());
        }

        return toDownload;
    }

    protected void saveSyncState() throws CalendarStorageException {
        /* Save sync state (CTag). It doesn't matter if it has changed during the sync process
           (for instance, because another client has uploaded changes), because this will simply
           cause all remote entries to be listed at the next sync. */
        localCollection.setCTag(remoteCTag);
    }

    private LocalCalendar localCalendar() {
        return ((LocalCalendar) localCollection);
    }

}
