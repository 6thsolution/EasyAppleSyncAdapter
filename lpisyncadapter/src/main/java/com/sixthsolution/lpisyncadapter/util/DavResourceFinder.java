/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package com.sixthsolution.lpisyncadapter.util;

import android.accounts.Account;
import android.content.Context;

import com.sixthsolution.lpisyncadapter.entitiy.CalendarData;
import com.sixthsolution.lpisyncadapter.entitiy.CollectionInfo;
import com.sixthsolution.lpisyncadapter.exceptions.InvalidAccountException;

import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.collections4.iterators.SingletonIterator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import at.bitfire.dav4android.DavCalendar;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.UrlUtils;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.CalendarHomeSet;
import at.bitfire.dav4android.property.CalendarProxyReadFor;
import at.bitfire.dav4android.property.CalendarProxyWriteFor;
import at.bitfire.dav4android.property.GroupMembership;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import timber.log.Timber;

import static com.sixthsolution.lpisyncadapter.GlobalConstant.BASE_URL;

public class DavResourceFinder {

    public static CalendarData getCalendarsData(Context context, Account account, OkHttpClient httpClient)
            throws InvalidAccountException, DavException, IOException, HttpException {
        String host = BASE_URL.getHost();
        String path = BASE_URL.getEncodedPath();
        int port = BASE_URL.getPort();
        URI uri = null;
        try {
            uri = new URI(BASE_URL.getScheme(), null, host, port, path, null, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        AccountSettings settings = new AccountSettings(context, account);
        DavResource dav = new DavResource(httpClient, HttpUrl.parse(uri.toString() + settings.getPrincipal()));

        // refresh home sets
        Set<HttpUrl> homeSets = getHomeSets(httpClient, dav);
        Map<HttpUrl, CollectionInfo> collections = getCollections(homeSets, httpClient);

        return new CalendarData(homeSets, collections);
    }

    public static Set<HttpUrl> getHomeSets(OkHttpClient httpClient, DavResource dav)
            throws IOException, HttpException, DavException {
        Set<HttpUrl> homeSets = new LinkedHashSet<>();

        queryHomeSets(dav, homeSets);

        CalendarProxyReadFor proxyRead = (CalendarProxyReadFor) dav.properties.get(CalendarProxyReadFor.NAME);
        if (proxyRead != null) {
            for (String href : proxyRead.principals) {
                queryHomeSets(new DavResource(httpClient, dav.location.resolve(href)), homeSets);
            }
        }

        CalendarProxyWriteFor proxyWrite = (CalendarProxyWriteFor) dav.properties.get(CalendarProxyWriteFor.NAME);
        if (proxyWrite != null) {
            for (String href : proxyWrite.principals) {
                Timber.d("getCalendarFromServer",
                         "Principal is a read-write proxy for " + href + ", checking for home sets");
                queryHomeSets(new DavResource(httpClient, dav.location.resolve(href)), homeSets);
            }
        }

        // refresh home sets: direct group memberships
        GroupMembership groupMembership = (GroupMembership) dav.properties.get(GroupMembership.NAME);
        if (groupMembership != null) {
            for (String href : groupMembership.hrefs) {
                Timber.d("getCalendarFromServer",
                         "Principal is member of group " + href + ", checking for home sets");
                DavResource group = new DavResource(httpClient, dav.location.resolve(href));
                try {
                    queryHomeSets(group, homeSets);
                } catch (HttpException | DavException e) {
                    Timber.e("getCalendarFromServer", "Couldn't query member group ", e);
                }
            }
        }

        return homeSets;
    }

    private static void queryHomeSets(DavResource dav, Set<HttpUrl> homeSets)
            throws IOException, HttpException, DavException {
        dav.propfind(0, CalendarHomeSet.NAME, CalendarProxyReadFor.NAME, CalendarProxyWriteFor.NAME,
                     GroupMembership.NAME);
        CalendarHomeSet calendarHomeSet = (CalendarHomeSet) dav.properties.get(CalendarHomeSet.NAME);
        if (calendarHomeSet != null) {
            for (String href : calendarHomeSet.hrefs) {
                homeSets.add(UrlUtils.withTrailingSlash(dav.location.resolve(href)));
            }
        }
    }

    public static Map<HttpUrl, CollectionInfo> getCollections(Set<HttpUrl> homeSets, OkHttpClient httpClient) {
        Map<HttpUrl, CollectionInfo> collections = new HashMap<>();

        // refresh collections (taken from home sets)
        for (Iterator<HttpUrl> itHomeSets = homeSets.iterator(); itHomeSets.hasNext(); ) {
            HttpUrl homeSet = itHomeSets.next();
            DavResource dav = new DavResource(httpClient, homeSet);
            try {
                dav.propfind(1, CollectionInfo.DAV_PROPERTIES);
                IteratorChain<DavResource> itCollections =
                        new IteratorChain<>(dav.members.iterator(), new SingletonIterator(dav));
                while (itCollections.hasNext()) {
                    DavResource member = itCollections.next();
                    CollectionInfo info = CollectionInfo.fromDavResource(member);
                    info.confirmed = true;

                    if (info.type == CollectionInfo.Type.CALENDAR) {
                        collections.put(member.location, info);
                    }
                }
            } catch (HttpException e) {
                if (e.status == 403 || e.status == 404 || e.status == 410) {
                    // delete home set only if it was not accessible (40x)
                    itHomeSets.remove();
                }
            } catch (DavException | IOException e) {
                e.printStackTrace();
            }
        }

        // check/refresh unconfirmed collections
        for (Iterator<Map.Entry<HttpUrl, CollectionInfo>> iterator = collections.entrySet().iterator();
             iterator.hasNext(); ) {
            Map.Entry<HttpUrl, CollectionInfo> entry = iterator.next();
            HttpUrl url = entry.getKey();
            CollectionInfo info = entry.getValue();
            if (!info.confirmed) {
                try {
                    DavResource dav = new DavResource(httpClient, url);
                    dav.propfind(0, CollectionInfo.DAV_PROPERTIES);
                    info = CollectionInfo.fromDavResource(dav);
                    info.confirmed = true;

                    // remove unusable collections
                    if (info.type != CollectionInfo.Type.CALENDAR) {
                        iterator.remove();
                    }
                } catch (HttpException e) {
                    if (e.status == 403 || e.status == 404 || e.status == 410) {
                        // delete collection only if it was not accessible (40x)
                        iterator.remove();
                    }
                } catch (DavException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return collections;
    }

    /**
     * Get any data stored in online server for this collection
     *
     * @param httpClient
     * @param collectionUrl
     * @param supportsVEVENT
     * @param supportsVTODO
     * @param start
     * @param end
     * @return
     * @throws DavException
     * @throws IOException
     * @throws HttpException
     */
    public static Map<String, DavResource> getCollectionEvents(OkHttpClient httpClient,
                                                               String collectionUrl,
                                                               boolean supportsVEVENT,
                                                               boolean supportsVTODO,
                                                               Date start,
                                                               Date end)
            throws DavException, IOException, HttpException {
        // TODO: 4/5/2017 Read last sync time from preferences
        /*Date limitStart = null;
        Integer pastDays = settings.getTimeRangePastDays();
        if (pastDays != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -pastDays);
            limitStart = calendar.getTime();
        }*/

        DavCalendar davCalendar = new DavCalendar(httpClient, HttpUrl.parse(collectionUrl));
        Map<String, DavResource> remoteResource = new HashMap<>();

        if (supportsVEVENT) {
            remoteResource.putAll(getCalendarData(davCalendar, "VEVENT", start, end));
        }

        // TODO: 4/8/2017 Proceed TODOs
        /*if (supportsVTODO) {
            remoteResource.putAll(getCalendarData(davCalendar, "VTODO", start, end));
        }*/

        return remoteResource;
    }

    private static Map<String, DavResource> getCalendarData(DavCalendar davCalendar, String what, Date start,
                                                            Date end)
            throws DavException, IOException, HttpException {
        davCalendar.calendarQuery(what, start, end);
        Map<String, DavResource> remoteResources = new HashMap<>(davCalendar.members.size());

        for (DavResource iCal : davCalendar.members) {
            String fileName = iCal.fileName();
            remoteResources.put(fileName, iCal);
        }

        return remoteResources;
    }
}
