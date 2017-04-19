package com.sixthsolution.lpisyncadapter.resource;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;

import com.sixthsolution.lpisyncadapter.App;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.Event;
import lombok.Cleanup;

/**
 * @author mehdok (mehdok@gmail.com) on 4/8/2017.
 */

public class LocalEvent extends AndroidEvent implements LocalResource {
    static final String COLUMN_ETAG = CalendarContract.Events.SYNC_DATA1,
            COLUMN_UID = Build.VERSION.SDK_INT >= 17 ? CalendarContract.Events.UID_2445 :
                    CalendarContract.Events.SYNC_DATA2,
            COLUMN_SEQUENCE = CalendarContract.Events.SYNC_DATA3;

    protected String fileName;
    protected String eTag;

    public boolean weAreOrganizer = true;

    public LocalEvent(@NonNull AndroidCalendar calendar, Event event, String fileName, String eTag) {
        super(calendar, event);
        this.fileName = fileName;
        this.eTag = eTag;
    }

    protected LocalEvent(@NonNull AndroidCalendar calendar, long id, ContentValues baseInfo) {
        super(calendar, id, baseInfo);
        if (baseInfo != null) {
            fileName = baseInfo.getAsString(CalendarContract.Events._SYNC_ID);
            eTag = baseInfo.getAsString(COLUMN_ETAG);
        }
    }

    @Override
    protected void populateEvent(ContentValues values) {
        super.populateEvent(values);
        fileName = values.getAsString(CalendarContract.Events._SYNC_ID);
        eTag = values.getAsString(COLUMN_ETAG);
        event.uid = values.getAsString(COLUMN_UID);

        event.sequence = values.getAsInteger(COLUMN_SEQUENCE);
        if (Build.VERSION.SDK_INT >= 17) {
            weAreOrganizer = values.getAsInteger(CalendarContract.Events.IS_ORGANIZER) != 0;
        } else {
            String organizer = values.getAsString(CalendarContract.Events.ORGANIZER);
            weAreOrganizer = organizer == null || organizer.equals(calendar.account.name);
        }
    }

    @Override
    protected void buildEvent(Event recurrence, ContentProviderOperation.Builder builder) {
        super.buildEvent(recurrence, builder);

        boolean buildException = recurrence != null;
        Event eventToBuild = buildException ? recurrence : event;

        builder.withValue(COLUMN_UID, event.uid)
                .withValue(COLUMN_SEQUENCE, eventToBuild.sequence)
                .withValue(CalendarContract.Events.DIRTY, 0)
                .withValue(CalendarContract.Events.DELETED, 0);

        if (buildException) {
            builder.withValue(CalendarContract.Events.ORIGINAL_SYNC_ID, fileName);
        } else {
            builder.withValue(CalendarContract.Events._SYNC_ID, fileName)
                    .withValue(COLUMN_ETAG, eTag);
        }
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    @Override
    public int delete() throws CalendarStorageException {
        return super.delete();
    }

    public void prepareForUpload() throws CalendarStorageException {
        try {
            String uid = null;
            @Cleanup Cursor
                    c = calendar.provider.query(eventSyncURI(), new String[] {COLUMN_UID}, null, null, null);
            if (c.moveToNext()) {
                uid = c.getString(0);
            }
            if (uid == null) {
                uid = App.uidGenerator.generateUid().getValue();
            }

            final String newFileName = uid + ".ics";

            ContentValues values = new ContentValues(2);
            values.put(CalendarContract.Events._SYNC_ID, newFileName);
            values.put(COLUMN_UID, uid);
            calendar.provider.update(eventSyncURI(), values, null, null);

            fileName = newFileName;
            if (event != null) {
                event.uid = uid;
            }

        } catch (RemoteException e) {
            throw new CalendarStorageException("Couldn't update UID", e);
        }
    }

    @Override
    public void clearDirty(String eTag) throws CalendarStorageException {
        try {
            ContentValues values = new ContentValues(2);
            values.put(CalendarContract.Events.DIRTY, 0);
            values.put(COLUMN_ETAG, eTag);
            if (event != null) {
                values.put(COLUMN_SEQUENCE, event.sequence);
            }
            calendar.provider.update(eventSyncURI(), values, null, null);

            this.eTag = eTag;
        } catch (RemoteException e) {
            throw new CalendarStorageException("Couldn't update UID", e);
        }
    }

    static class Factory implements AndroidEventFactory {
        static final Factory INSTANCE = new Factory();

        @Override
        public AndroidEvent newInstance(AndroidCalendar calendar, long id, ContentValues baseInfo) {
            return new LocalEvent(calendar, id, baseInfo);
        }

        @Override
        public AndroidEvent newInstance(AndroidCalendar calendar, Event event) {
            return new LocalEvent(calendar, event, null, null);
        }

        @Override
        public AndroidEvent[] newArray(int size) {
            return new LocalEvent[size];
        }
    }
}
