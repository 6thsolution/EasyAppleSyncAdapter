package com.sixthsolution.lpisyncadapter.syncadapter;

import android.accounts.Account;
import android.content.Context;

import com.sixthsolution.lpisyncadapter.entitiy.CalendarData;
import com.sixthsolution.lpisyncadapter.exceptions.InvalidAccountException;
import com.sixthsolution.lpisyncadapter.util.DavResourceFinder;
import com.sixthsolution.lpisyncadapter.util.HttpClient;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import okhttp3.OkHttpClient;
import timber.log.Timber;

import static com.sixthsolution.lpisyncadapter.util.Util.getHttpLogger;

/**
 * @author mehdok (mehdok@gmail.com) on 3/18/2017.
 */

public class SyncServerHandlerImpl implements SyncServerHandler {
    @Override
    public CalendarData getServerCalendar(Context context, Account account) {
        try {
            return getCalendarFromServer(context, account);
        } catch (Exception e) {
            e.printStackTrace();
            Timber.e("getServerCalendar", e);
        }
        return null;
    }

    private CalendarData getCalendarFromServer(Context context, Account account)
            throws InvalidAccountException, DavException, IOException, HttpException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {

        OkHttpClient httpClient = HttpClient.create(context, account, getHttpLogger());

        return DavResourceFinder.getCalendarsData(context, account, httpClient);
    }
}
