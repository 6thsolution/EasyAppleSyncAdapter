package com.sixthsolution.lpisyncadapter.authenticator;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sixthsolution.lpisyncadapter.GlobalConstant;
import com.sixthsolution.lpisyncadapter.R;
import com.sixthsolution.lpisyncadapter.authenticator.crypto.Crypto;
import com.sixthsolution.lpisyncadapter.authenticator.crypto.KeyManager;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static com.sixthsolution.lpisyncadapter.GlobalConstant.PARAM_PRINCIPAL;


/**
 * Every custom login activity must extend this class, and in their layout they must have 2 {@link EditText} with
 * Exact id 'user_name' and 'password' and a {@link Button} with id of 'signin_button'.
 * After calling {@link #setContentView(int)} in {@link #onCreate(Bundle)} you must call {@link #init()}
 *
 * @author mehdok (mehdok@gmail.com) on 3/15/2017.
 */

public class BaseLoginActivity extends AccountAuthenticatorActivity {
    private final static String SIGNIN_ERROR = "signin_error";

    private EditText userName;
    private EditText password;
    private Button signIn;
    private View progressLayout;

    private AccountManager accountManager;
    private AuthServerHandler serverHandler;
    private String authTokenType;
    private boolean isNewAccount = true;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    protected void init() {
        initCrypto();

        accountManager = AccountManager.get(getBaseContext());
        serverHandler = new AuthServerHandlerImpl();

        userName = (EditText) findViewById(R.id.user_name);
        password = (EditText) findViewById(R.id.password);
        signIn = (Button) findViewById(R.id.signin_button);
        progressLayout = findViewById(R.id.progress_layout);

        Intent intent = getIntent();
        authTokenType = getIntent().getStringExtra(GlobalConstant.AUTH_TYPE);
        if (authTokenType == null) {
            authTokenType = GlobalConstant.AUTHTOKEN_TYPE_FULL_ACCESS;
        }

        isNewAccount = intent.getBooleanExtra(GlobalConstant.IS_ADDING_NEW_ACCOUNT, true);
        if (!isNewAccount) {
            // existing account
            String accountName = getIntent().getStringExtra(GlobalConstant.ACCOUNT_NAME);
            userName.setText(accountName);
        }

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    /**
     * In first run we must create a random keys and save it for further use
     */
    private void initCrypto() {
        String pref_name = "ical_authenticator_pref";
        String first_run = "pref_first_run";

        SharedPreferences sharedPreferences = getSharedPreferences(pref_name, Context.MODE_PRIVATE);

        if (sharedPreferences.getBoolean(first_run, true)) {
            // set private key for any encryption, this will run once
            KeyManager keyManager = new KeyManager();
            keyManager.setId(UUID.randomUUID().toString().substring(0, 32).getBytes(), this);
            keyManager.setIv(UUID.randomUUID().toString().substring(0, 16).getBytes(), this);

            sharedPreferences.edit().putBoolean(first_run, false).apply();
        }
    }

    private void login() {
        final String usr = userName.getText().toString();
        final String pass = password.getText().toString();
        new AsyncTask<String, Void, Intent>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgress(true);
            }

            @Override
            protected Intent doInBackground(String... params) {
                String iCalUserId = null;
                Bundle data = new Bundle();
                try {
                    iCalUserId = serverHandler.userSignIn(usr, pass);

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, usr);
                    data.putString(AccountManager.KEY_AUTHTOKEN, iCalUserId);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, authTokenType);
                    data.putString(GlobalConstant.PARAM_USER_PASS, pass);

                } catch (Exception e) {
                    e.printStackTrace();
                    data.putSerializable(SIGNIN_ERROR, e);
                }

                final Intent res = new Intent();
                res.putExtras(data);
                return res;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                try {
                    passDataToAuthenticator(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(BaseLoginActivity.this, "Can not connect to server or wrong info",
                                   Toast.LENGTH_SHORT).show();
                }
                showProgress(false);
            }
        }.execute();
    }


    /**
     * Pass the data from server to authenticator class
     *
     * @param intent the intent contain data from user and server {@link AccountManager#KEY_ACCOUNT_NAME},
     *               {@link AccountManager#KEY_AUTHTOKEN}, {@link AccountManager#KEY_ACCOUNT_TYPE} and
     *               {@link GlobalConstant#PARAM_USER_PASS}
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws SignInException
     */
    private void passDataToAuthenticator(Intent intent)
            throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException, UnsupportedEncodingException,
            SignInException {

        if (intent.hasExtra(SIGNIN_ERROR)) throw new SignInException();

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(GlobalConstant.PARAM_USER_PASS);
        accountPassword = Crypto.armorEncrypt(accountPassword.getBytes("UTF-8"), this);

        final Account account = new Account(accountName, authTokenType);

        if (isNewAccount) {
            String iCalId = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String authtoken = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);

            // encrypt user id and pass it to intent
            iCalId = Crypto.armorEncrypt(iCalId.getBytes("UTF-8"), this);
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, iCalId);

            final Bundle extraData = new Bundle();
            extraData.putString(PARAM_PRINCIPAL, iCalId);

            accountManager.addAccountExplicitly(account, accountPassword, extraData);
            accountManager.setAuthToken(account, authtoken, iCalId);
        } else {
            accountManager.setPassword(account, accountPassword);
        }

        // encrypt password and pass it to intent
        intent.putExtra(GlobalConstant.PARAM_USER_PASS, accountPassword);


        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void showProgress(boolean show) {
        if (show) {
            progressLayout.setVisibility(View.VISIBLE);
        } else {
            progressLayout.setVisibility(View.GONE);
        }
    }
}
