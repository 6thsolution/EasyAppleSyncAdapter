package com.sixthsolution.lpisyncadapter.syncadapter;

import android.accounts.Account;
import android.content.Context;

import com.sixthsolution.lpisyncadapter.entitiy.CalendarData;


/**
 * @author mehdok (mehdok@gmail.com) on 3/18/2017.
 */

public interface SyncServerHandler {
    CalendarData getServerCalendar(Context context, Account account);
}
