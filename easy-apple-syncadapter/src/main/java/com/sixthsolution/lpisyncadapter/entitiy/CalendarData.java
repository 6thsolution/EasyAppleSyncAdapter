package com.sixthsolution.lpisyncadapter.entitiy;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import okhttp3.HttpUrl;

/**
 * Stub data class
 *
 * @author mehdok (mehdok@gmail.com) on 3/18/2017.
 */

public class CalendarData extends BaseCalendarData {
    private Set<HttpUrl> homeSets;
    private Map<HttpUrl, CollectionInfo> collections;

    public CalendarData() {
        homeSets = new HashSet<>();
        collections = new HashMap<>();
    }

    public CalendarData(Set<HttpUrl> homeSets,
                        Map<HttpUrl, CollectionInfo> collections) {
        this.homeSets = homeSets;
        this.collections = collections;
    }

    /**
     * Create CalendarData from cursor
     *
     * @param cursor
     * @return
     */
    public static CalendarData fromCursor(Cursor cursor) {
        return new CalendarData();
    }

    /**
     * Create Content value for use in Content provider
     *
     * @return
     */
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        return values;
    }

    public void addHomeSet(HttpUrl homeSet) {
        homeSets.add(homeSet);
    }

    public void addCollection(HttpUrl uri, CollectionInfo collectionInfo) {
        collections.put(uri, collectionInfo);
    }

    public Set<HttpUrl> getHomeSets() {
        return homeSets;
    }

    public Map<HttpUrl, CollectionInfo> getCollections() {
        return collections;
    }
}
