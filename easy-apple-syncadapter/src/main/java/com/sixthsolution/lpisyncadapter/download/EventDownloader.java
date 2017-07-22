package com.sixthsolution.lpisyncadapter.download;


import com.sixthsolution.lpisyncadapter.resource.LocalCalendar;
import com.sixthsolution.lpisyncadapter.resource.LocalEvent;
import com.sixthsolution.lpisyncadapter.resource.LocalResource;
import com.sixthsolution.lpisyncadapter.util.ArrayUtils;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.bitfire.dav4android.DavCalendar;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.CalendarData;
import at.bitfire.dav4android.property.GetContentType;
import at.bitfire.dav4android.property.GetETag;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import lombok.Cleanup;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import timber.log.Timber;

/**
 * @author mehdok (mehdok@gmail.com) on 4/8/2017.
 */

public class EventDownloader {
    private static final int MAX_MULTIGET = 20;

    /**
     * Download server event and update local
     *
     * @param toDownload
     * @param calendar
     * @param localCalendar
     * @param localResources
     * @throws DavException
     * @throws IOException
     * @throws HttpException
     * @throws CalendarStorageException
     */
    public static void downloadEvents(Set<DavResource> toDownload, DavCalendar calendar,
                                      LocalCalendar localCalendar, Map<String, LocalResource> localResources)
            throws DavException, IOException, HttpException, CalendarStorageException {
        for (DavResource[] bunch : ArrayUtils.partition(toDownload.toArray(new DavResource[toDownload.size()]),
                                                        MAX_MULTIGET)) {
            // instant cancel
            if (Thread.interrupted()) {
                return;
            }

            if (bunch.length == 1) {
                // only one contact, use GET
                final DavResource remote = bunch[0];

                ResponseBody body = remote.get("text/calendar");

                // CalDAV servers MUST return ETag on GET [https://tools.ietf.org/html/rfc4791#section-5.3.4]
                GetETag eTag = (GetETag) remote.properties.get(GetETag.NAME);
                if (eTag == null || StringUtils.isEmpty(eTag.eTag)) {
                    throw new DavException("Received CalDAV GET response without ETag for " + remote.location);
                }

                Charset charset = Charsets.UTF_8;
                MediaType contentType = body.contentType();
                if (contentType != null) {
                    charset = contentType.charset(Charsets.UTF_8);
                }

                @Cleanup InputStream stream = body.byteStream();
                processVEvent(remote.fileName(), eTag.eTag, stream, charset, localCalendar, localResources);
            } else {
                // multiple contacts, use multi-get
                List<HttpUrl> urls = new LinkedList<>();
                for (DavResource remote : bunch)
                    urls.add(remote.location);

                calendar.multiget(urls.toArray(new HttpUrl[urls.size()]));

                // process multiget results
                for (final DavResource remote : calendar.members) {
                    String eTag;
                    GetETag getETag = (GetETag) remote.properties.get(GetETag.NAME);
                    if (getETag != null) {
                        eTag = getETag.eTag;
                    } else {
                        throw new DavException("Received multi-get response without ETag");
                    }

                    Charset charset = Charsets.UTF_8;
                    GetContentType getContentType = (GetContentType) remote.properties.get(GetContentType.NAME);
                    if (getContentType != null && getContentType.type != null) {
                        MediaType type = MediaType.parse(getContentType.type);
                        if (type != null) {
                            charset = type.charset(Charsets.UTF_8);
                        }
                    }

                    CalendarData calendarData = (CalendarData) remote.properties.get(CalendarData.NAME);
                    if (calendarData == null || calendarData.iCalendar == null) {
                        throw new DavException("Received multi-get response without address data");
                    }

                    @Cleanup InputStream stream = new ByteArrayInputStream(calendarData.iCalendar.getBytes());
                    processVEvent(remote.fileName(), eTag, stream, charset, localCalendar, localResources);
                }
            }
        }
    }

    private static void processVEvent(String fileName, String eTag, InputStream stream, Charset charset,
                                      LocalCalendar localCalendar, Map<String, LocalResource> localResources)
            throws IOException, CalendarStorageException {
        Event[] events;
        try {
            events = Event.fromStream(stream, charset);
        } catch (InvalidCalendarException e) {
            Timber.w("Received invalid iCalendar, ignoring", e);
            return;
        }

        if (events.length == 1) {
            Event newData = events[0];

            // delete local event, if it exists
            LocalEvent localEvent = (LocalEvent) localResources.get(fileName);
            if (localEvent != null) {
                localEvent.setETag(eTag);
                localEvent.update(newData);
            } else {
                localEvent = new LocalEvent(localCalendar, newData, fileName, eTag);
                localEvent.add();
            }
        } else {
            Timber.w(
                    "Received VCALENDAR with not exactly one VEVENT with UID, but without RECURRENCE-ID; ignoring " +
                            fileName);
        }
    }
}
