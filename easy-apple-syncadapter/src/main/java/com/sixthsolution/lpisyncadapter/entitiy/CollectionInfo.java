/*
 * Copyright © 2013 – 2016 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.sixthsolution.lpisyncadapter.entitiy;

import android.text.TextUtils;

import com.sixthsolution.lpisyncadapter.util.DavUtils;

import org.apache.commons.lang3.StringUtils;

import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.Property;
import at.bitfire.dav4android.property.AddressbookDescription;
import at.bitfire.dav4android.property.CalendarColor;
import at.bitfire.dav4android.property.CalendarDescription;
import at.bitfire.dav4android.property.CalendarTimezone;
import at.bitfire.dav4android.property.CurrentUserPrivilegeSet;
import at.bitfire.dav4android.property.DisplayName;
import at.bitfire.dav4android.property.ResourceType;
import at.bitfire.dav4android.property.SupportedAddressData;
import at.bitfire.dav4android.property.SupportedCalendarComponentSet;
import lombok.ToString;

@ToString
public class CollectionInfo extends BaseCollectionInfo {
    public static final int defaultColor = 0xFF8bc34a;     // light green 500

    public enum Type {
        CALENDAR
    }

    public Type type;

    public String url;

    public boolean readOnly;
    public String displayName, description;
    public Integer color;

    public String timeZone;
    public Boolean supportsVEVENT;
    public Boolean supportsVTODO;

    public boolean selected;

    // non-persistent properties
    public boolean confirmed;


    public static final Property.Name[] DAV_PROPERTIES = {
            ResourceType.NAME,
            CurrentUserPrivilegeSet.NAME,
            DisplayName.NAME,
            AddressbookDescription.NAME, SupportedAddressData.NAME,
            CalendarDescription.NAME, CalendarColor.NAME, SupportedCalendarComponentSet.NAME
    };

    public static CollectionInfo fromDavResource(DavResource dav) {
        CollectionInfo info = new CollectionInfo();
        info.url = dav.location.toString();

        ResourceType type = (ResourceType) dav.properties.get(ResourceType.NAME);
        if (type != null) {
            if (type.types.contains(ResourceType.CALENDAR)) {
                info.type = Type.CALENDAR;
            }
        }

        info.readOnly = false;
        CurrentUserPrivilegeSet privilegeSet =
                (CurrentUserPrivilegeSet) dav.properties.get(CurrentUserPrivilegeSet.NAME);
        if (privilegeSet != null) {
            info.readOnly = !privilegeSet.mayWriteContent;
        }

        DisplayName displayName = (DisplayName) dav.properties.get(DisplayName.NAME);
        if (displayName != null && !StringUtils.isEmpty(displayName.displayName)) {
            info.displayName = displayName.displayName;
        }

        if (info.type == Type.CALENDAR) {
            CalendarDescription calendarDescription =
                    (CalendarDescription) dav.properties.get(CalendarDescription.NAME);
            if (calendarDescription != null) {
                info.description = calendarDescription.description;
            }

            CalendarColor calendarColor = (CalendarColor) dav.properties.get(CalendarColor.NAME);
            if (calendarColor != null) {
                info.color = calendarColor.color;
            }

            CalendarTimezone timeZone = (CalendarTimezone) dav.properties.get(CalendarTimezone.NAME);
            if (timeZone != null) {
                info.timeZone = timeZone.vTimeZone;
            }

            info.supportsVEVENT = info.supportsVTODO = true;
            SupportedCalendarComponentSet supportedCalendarComponentSet =
                    (SupportedCalendarComponentSet) dav.properties.get(
                            SupportedCalendarComponentSet.NAME);
            if (supportedCalendarComponentSet != null) {
                info.supportsVEVENT = supportedCalendarComponentSet.supportsEvents;
                info.supportsVTODO = supportedCalendarComponentSet.supportsTasks;
            }
        }

        return info;
    }

    public String getDisplayName() {
        return !TextUtils.isEmpty(displayName) ? displayName : DavUtils.lastSegmentOfUrl(url);
    }

    public Integer getColor() {
        return color != null ? color : defaultColor;
    }
}
