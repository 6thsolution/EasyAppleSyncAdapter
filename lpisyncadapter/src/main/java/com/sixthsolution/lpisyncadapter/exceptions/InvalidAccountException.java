/*
 * Copyright © 2013 – 2016 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.sixthsolution.lpisyncadapter.exceptions;

import android.accounts.Account;

public class InvalidAccountException extends Exception {

    public InvalidAccountException(Account account) {
        super("Invalid account: " + account);
    }

}
