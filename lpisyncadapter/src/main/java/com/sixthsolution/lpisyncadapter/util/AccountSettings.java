/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package com.sixthsolution.lpisyncadapter.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.sixthsolution.lpisyncadapter.authenticator.crypto.Crypto;
import com.sixthsolution.lpisyncadapter.exceptions.InvalidAccountException;

import static com.sixthsolution.lpisyncadapter.GlobalConstant.PARAM_PRINCIPAL;


public class AccountSettings {
    private final Context context;
    private final AccountManager accountManager;
    private final Account account;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AccountSettings(@NonNull Context context, @NonNull Account account) throws InvalidAccountException {
        this.context = context;
        this.account = account;

        accountManager = AccountManager.get(context);
    }

    // authentication settings
    public String username() {
        return account.name;
    }

    public String password() {
        try {
            return Crypto.armorDecrypt(accountManager.getPassword(account), context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String getPrincipal() {
        try {
            return Crypto.armorDecrypt(accountManager.getUserData(account, PARAM_PRINCIPAL), context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}
