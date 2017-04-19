package com.sixthsolution.lpisyncadapter.authenticator;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * @author mehdok (mehdok@gmail.com) on 3/9/2017.
 */

public interface AuthServerHandler {

    /**
     * Getting the username and password and return the iCal user id
     *
     * @param user iCal username
     * @param pass iCal password
     * @return The user id of iCal
     * @throws Exception, throws various Exceptions such as {@link NoSuchAlgorithmException},
     *                    {@link NoSuchPaddingException}, {@link IllegalBlockSizeException},
     *                    {@link BadPaddingException}, {@link InvalidAlgorithmParameterException}
     *                    or general Exception
     */
    String userSignIn(final String user, final String pass) throws Exception;
}
