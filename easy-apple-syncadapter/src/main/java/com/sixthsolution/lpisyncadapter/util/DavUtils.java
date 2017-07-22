package com.sixthsolution.lpisyncadapter.util;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import okhttp3.HttpUrl;

/**
 * @author mehdok (mehdok@gmail.com) on 4/12/2017.
 */

public class DavUtils {
    public static String lastSegmentOfUrl(@NonNull String url) {
        // the list returned by HttpUrl.pathSegments() is unmodifiable, so we have to create a copy
        List<String> segments = new LinkedList<>(HttpUrl.parse(url).pathSegments());
        Collections.reverse(segments);

        for (String segment : segments)
            if (!StringUtils.isEmpty(segment)) {
                return segment;
            }

        return "/";
    }
}
