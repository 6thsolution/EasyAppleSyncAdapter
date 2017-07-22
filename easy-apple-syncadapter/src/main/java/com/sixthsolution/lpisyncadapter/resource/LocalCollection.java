package com.sixthsolution.lpisyncadapter.resource;

import java.io.FileNotFoundException;

import at.bitfire.ical4android.CalendarStorageException;

/**
 * @author mehdok (mehdok@gmail.com) on 4/8/2017.
 */

public interface LocalCollection {
    LocalResource[] getDeleted() throws CalendarStorageException;

    LocalResource[] getWithoutFileName() throws CalendarStorageException;

    LocalResource[] getDirty() throws CalendarStorageException, FileNotFoundException;

    LocalResource[] getAll() throws CalendarStorageException;

    String getCTag() throws CalendarStorageException;

    void setCTag(String cTag) throws CalendarStorageException;
}
