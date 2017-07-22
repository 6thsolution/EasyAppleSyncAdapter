package com.sixthsolution.lpisyncadapter.resource;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.sixthsolution.lpisyncadapter.entitiy.CollectionInfo;

import net.fortuna.ical4j.model.component.VTimeZone;

import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidCalendarFactory;
import at.bitfire.ical4android.BatchOperation;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.DateUtils;
import lombok.Cleanup;

/**
 * @author mehdok (mehdok@gmail.com) on 4/8/2017.
 */

public class LocalCalendar extends AndroidCalendar implements LocalCollection {

    public static final String COLUMN_CTAG = CalendarContract.Calendars.CAL_SYNC1;

    static String[] BASE_INFO_COLUMNS = new String[] {
            CalendarContract.Events._ID,
            CalendarContract.Events._SYNC_ID,
            LocalEvent.COLUMN_ETAG
    };

    protected LocalCalendar(Account account, ContentProviderClient provider, long id) {
        super(account, provider, LocalEvent.Factory.INSTANCE, id);
    }

    public static Uri create(@NonNull Account account, @NonNull ContentProviderClient provider, @NonNull
            CollectionInfo info) throws
            CalendarStorageException {
        ContentValues values = valuesFromCollectionInfo(info, true);

        // ACCOUNT_NAME and ACCOUNT_TYPE are required (see docs)! If it's missing, other apps will crash.
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, account.name);
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, account.type);
        values.put(CalendarContract.Calendars.OWNER_ACCOUNT, account.name);

        // flag as visible & synchronizable at creation, might be changed by user at any time
        values.put(CalendarContract.Calendars.VISIBLE, 0);
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);

        return create(account, provider, values);
    }

    public void update(CollectionInfo info, boolean updateColor) throws CalendarStorageException {
        update(valuesFromCollectionInfo(info, updateColor));
    }

    private static ContentValues valuesFromCollectionInfo(CollectionInfo info, boolean withColor) {
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.NAME, info.url);
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, info.getDisplayName());

        if (withColor) {
            values.put(CalendarContract.Calendars.CALENDAR_COLOR, info.getColor());
        }

        if (info.readOnly) {
            values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                       CalendarContract.Calendars.CAL_ACCESS_READ);
        } else {
            values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                       CalendarContract.Calendars.CAL_ACCESS_OWNER);
            values.put(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE, 1);
            values.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 1);
        }

        if (!TextUtils.isEmpty(info.timeZone)) {
            VTimeZone timeZone = DateUtils.parseVTimeZone(info.timeZone);
            if (timeZone != null && timeZone.getTimeZoneId() != null) {
                values.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE,
                           DateUtils.findAndroidTimezoneID(timeZone.getTimeZoneId().getValue()));
            }
        }
        values.put(CalendarContract.Calendars.ALLOWED_REMINDERS, CalendarContract.Reminders.METHOD_ALERT);
        values.put(CalendarContract.Calendars.ALLOWED_AVAILABILITY, StringUtils.join(
                new int[] {CalendarContract.Reminders.AVAILABILITY_TENTATIVE,
                        CalendarContract.Reminders.AVAILABILITY_FREE,
                        CalendarContract.Reminders.AVAILABILITY_BUSY},
                ","));
        values.put(CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES, StringUtils.join(
                new int[] {CalendarContract.Attendees.TYPE_OPTIONAL,
                        CalendarContract.Attendees.TYPE_REQUIRED,
                        CalendarContract.Attendees.TYPE_RESOURCE},
                ", "));
        return values;
    }

    @Override
    public LocalResource[] getDeleted() throws CalendarStorageException {
        return (LocalEvent[]) queryEvents(
                CalendarContract.Events.DELETED + "!=0 AND " + CalendarContract.Events.ORIGINAL_ID + " IS NULL",
                null);
    }

    @Override
    public LocalResource[] getWithoutFileName() throws CalendarStorageException {
        return (LocalEvent[]) queryEvents(
                CalendarContract.Events._SYNC_ID +
                        " IS NULL AND " +
                        CalendarContract.Events.ORIGINAL_ID +
                        " IS NULL", null);
    }

    @Override
    public LocalResource[] getDirty() throws CalendarStorageException, FileNotFoundException {
        List<LocalResource> dirty = new LinkedList<>();

        // get dirty events which are required to have an increased SEQUENCE value
        for (LocalEvent event : (LocalEvent[]) queryEvents(
                CalendarContract.Events.DIRTY + "!=0 AND " + CalendarContract.Events.ORIGINAL_ID + " IS NULL",
                null)) {
            if (event.getEvent().sequence ==
                    null)      // sequence has not been assigned yet (i.e. this event was just locally created)
            {
                event.getEvent().sequence = 0;
            } else if (event.weAreOrganizer) {
                event.getEvent().sequence++;
            }
            dirty.add(event);
        }

        return dirty.toArray(new LocalResource[dirty.size()]);
    }

    @Override
    public LocalResource[] getAll() throws CalendarStorageException {
        return (LocalEvent[]) queryEvents(CalendarContract.Events.ORIGINAL_ID + " IS NULL", null);
    }

    @Override
    public String getCTag() throws CalendarStorageException {
        try {
            @Cleanup Cursor
                    cursor = provider.query(calendarSyncURI(), new String[] {COLUMN_CTAG}, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                return cursor.getString(0);
            }
        } catch (RemoteException e) {
            throw new CalendarStorageException("Couldn't read local (last known) CTag", e);
        }
        return null;
    }

    @Override
    public void setCTag(String cTag) throws CalendarStorageException {
        try {
            ContentValues values = new ContentValues(1);
            values.put(COLUMN_CTAG, cTag);
            provider.update(calendarSyncURI(), values, null, null);
        } catch (RemoteException e) {
            throw new CalendarStorageException("Couldn't write local (last known) CTag", e);
        }
    }

    public void processDirtyExceptions() throws CalendarStorageException {
        // process deleted exceptions
        try {
            @Cleanup Cursor cursor = provider.query(
                    syncAdapterURI(CalendarContract.Events.CONTENT_URI),
                    new String[] {CalendarContract.Events._ID, CalendarContract.Events.ORIGINAL_ID, LocalEvent.COLUMN_SEQUENCE},
                    CalendarContract.Events.DELETED +
                            "!=0 AND " +
                            CalendarContract.Events.ORIGINAL_ID +
                            " IS NOT NULL", null,
                    null);
            while (cursor != null && cursor.moveToNext()) {
                long id = cursor.getLong(0),             // can't be null (by definition)
                        originalID = cursor.getLong(1);     // can't be null (by query)

                // get original event's SEQUENCE
                @Cleanup Cursor cursor2 = provider.query(
                        syncAdapterURI(
                                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, originalID)),
                        new String[] {LocalEvent.COLUMN_SEQUENCE},
                        null, null, null);
                int originalSequence = (cursor2 == null || cursor2.isNull(0)) ? 0 : cursor2.getInt(0);

                BatchOperation batch = new BatchOperation(provider);
                // re-schedule original event and set it to DIRTY
                batch.enqueue(new BatchOperation.Operation(
                        ContentProviderOperation.newUpdate(syncAdapterURI(
                                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, originalID)))
                                .withValue(LocalEvent.COLUMN_SEQUENCE, originalSequence + 1)
                                .withValue(CalendarContract.Events.DIRTY, 1)
                ));
                // remove exception
                batch.enqueue(new BatchOperation.Operation(
                        ContentProviderOperation.newDelete(syncAdapterURI(
                                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)))
                ));
                batch.commit();
            }
        } catch (RemoteException e) {
            throw new CalendarStorageException("Couldn't process locally modified exception", e);
        }

        // process dirty exceptions
        try {
            @Cleanup Cursor cursor = provider.query(
                    syncAdapterURI(CalendarContract.Events.CONTENT_URI),
                    new String[] {CalendarContract.Events._ID, CalendarContract.Events.ORIGINAL_ID, LocalEvent.COLUMN_SEQUENCE},
                    CalendarContract.Events.DIRTY +
                            "!=0 AND " +
                            CalendarContract.Events.ORIGINAL_ID +
                            " IS NOT NULL", null, null);
            while (cursor != null && cursor.moveToNext()) {
                long id = cursor.getLong(0),             // can't be null (by definition)
                        originalID = cursor.getLong(1);     // can't be null (by query)
                int sequence = cursor.isNull(2) ? 0 : cursor.getInt(2);

                BatchOperation batch = new BatchOperation(provider);
                // original event to DIRTY
                batch.enqueue(new BatchOperation.Operation(
                        ContentProviderOperation.newUpdate(syncAdapterURI(
                                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, originalID)))
                                .withValue(CalendarContract.Events.DIRTY, 1)
                ));
                // increase SEQUENCE and set DIRTY to 0
                batch.enqueue(new BatchOperation.Operation(
                        ContentProviderOperation.newUpdate(
                                syncAdapterURI(
                                        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)))
                                .withValue(LocalEvent.COLUMN_SEQUENCE, sequence + 1)
                                .withValue(CalendarContract.Events.DIRTY, 0)
                ));
                batch.commit();
            }
        } catch (RemoteException e) {
            throw new CalendarStorageException("Couldn't process locally modified exception", e);
        }
    }

    public static class Factory implements AndroidCalendarFactory {
        public static final Factory INSTANCE = new Factory();

        @Override
        public AndroidCalendar newInstance(Account account, ContentProviderClient provider, long id) {
            return new LocalCalendar(account, provider, id);
        }

        @Override
        public AndroidCalendar[] newArray(int size) {
            return new LocalCalendar[size];
        }
    }

    @Override
    protected void populate(ContentValues info) {
        super.populate(info);
    }

    protected String[] eventBaseInfoColumns() {
        return BASE_INFO_COLUMNS;
    }
}
