package com.sixthsolution.lpisyncadapter.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.sixthsolution.lpisyncadapter.GlobalConstant;
import com.sixthsolution.lpisyncadapter.authenticator.crypto.Crypto;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static com.sixthsolution.lpisyncadapter.GlobalConstant.IS_ADDING_NEW_ACCOUNT;

/**
 * This class is responsible for getting iCal user id if it is exist or show login screen if there is not one,
 * By default this class suppose the Saved (in {@link AccountManager}) password and user id is encrypted via
 * {@link Crypto#armorEncrypt(byte[], Context)}, so before saving password and user id just encrypt it via
 * {@link Crypto#armorEncrypt(byte[], Context)}
 *
 * @author mehdok (mehdok@gmail.com) on 3/9/2017.
 */

public class ICalAuthenticator extends AbstractAccountAuthenticator {
    private final Context context;
    private AuthServerHandler serverHandler;
    private Class loginActivity;

    /**
     * @param context
     * @param loginActivity the custom login activity to run
     * @param authType      the custom authentication type that you set in your xml file
     */
    public ICalAuthenticator(Context context, Class loginActivity, String authType) {
        this(context, loginActivity);
        GlobalConstant.AUTHTOKEN_TYPE_FULL_ACCESS = authType;
    }

    /**
     * @param context
     * @param loginActivity the custom login activity to run
     */
    public ICalAuthenticator(Context context, Class loginActivity) {
        this(context);
        this.loginActivity = loginActivity;
    }

    public ICalAuthenticator(Context context) {
        super(context);
        this.context = context;
        serverHandler = new AuthServerHandlerImpl();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
                             String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        final Intent intent = new Intent(context, loginActivity);
        intent.putExtra(GlobalConstant.ACCOUNT_TYPE, accountType);
        intent.putExtra(GlobalConstant.AUTH_TYPE, authTokenType);
        intent.putExtra(IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType,
                               Bundle options) throws NetworkErrorException {
        final AccountManager am = AccountManager.get(context);
        String authToken = am.peekAuthToken(account, authTokenType);

        // get new token if there is no one
        if (TextUtils.isEmpty(authToken)) {
            String password = am.getPassword(account);
            if (password != null) {
                try {
                    password = Crypto.armorDecrypt(password, context);
                    authToken = serverHandler.userSignIn(account.name, password);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // there is new token, return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // no token and no password, show login screen
        final Intent intent = new Intent(context, loginActivity);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(GlobalConstant.ACCOUNT_TYPE, account.type);
        intent.putExtra(GlobalConstant.ACCOUNT_NAME, account.name);
        intent.putExtra(IS_ADDING_NEW_ACCOUNT, false);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        // we have one account type
        return "iCal access";
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
            throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        // no op
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
                                    Bundle options) throws NetworkErrorException {
        // no op
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
            throws NetworkErrorException {
        // no op
        return null;
    }
}
