package com.sixthsolution.lpisyncadapter.authenticator;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import at.bitfire.dav4android.BasicDigestAuthHandler;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.UrlUtils;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.CurrentUserPrincipal;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import static com.sixthsolution.lpisyncadapter.GlobalConstant.BASE_URL;

/**
 * This class is responsible for handling network communication and parsing iCal user id
 * at the end It will use {@link BasicDigestAuthHandler} and {@link DavResource} to find and parse iCal user id
 *
 * @author mehdok (mehdok@gmail.com) on 3/9/2017.
 */

public class AuthServerHandlerImpl implements AuthServerHandler {

    @Override
    public String userSignIn(String user, String pass) throws Exception {
        try {
            URI uri = new URI(BASE_URL.getScheme(),
                              null,
                              BASE_URL.getHost(),
                              BASE_URL.getPort(),
                              BASE_URL.getEncodedPath(),
                              null,
                              null);
            return getICalID(uri, user, pass);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param uri  the base {@link URI} of ical caldav server
     * @param user username
     * @param pass password
     * @return the ical User id
     * @throws DavException
     * @throws IOException
     * @throws HttpException
     */
    private String getICalID(URI uri, String user, String pass) throws DavException, IOException, HttpException {
        OkHttpClient client = new OkHttpClient();
        OkHttpClient.Builder builder = client.newBuilder();
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(120, TimeUnit.SECONDS);

        // don't allow redirects, because it would break PROPFIND handling
        builder.followRedirects(false);

        BasicDigestAuthHandler
                authHandler = new BasicDigestAuthHandler(UrlUtils.hostToDomain(null), user, pass);
        builder.addNetworkInterceptor(authHandler).authenticator(authHandler);
        final HttpUrl baseURL = HttpUrl.get(uri);
        Logger log = Logger.getLogger("ICalServerHandler.DavResourceFinder");
        log.setLevel(Level.OFF);

        client = builder.build();
        DavResource davBase = new DavResource(client, baseURL, log);
        davBase.propfind(0, CurrentUserPrincipal.NAME);

        CurrentUserPrincipal currentUserPrincipal =
                (CurrentUserPrincipal) davBase.properties.get(CurrentUserPrincipal.NAME);
        if (currentUserPrincipal != null && currentUserPrincipal.href != null) {
            /*String[] nodeText = currentUserPrincipal.href.split("/");
            if (nodeText.length > 1) {
                return nodeText[1];
            }*/
            return currentUserPrincipal.href;
        }

        return null;
    }
}
