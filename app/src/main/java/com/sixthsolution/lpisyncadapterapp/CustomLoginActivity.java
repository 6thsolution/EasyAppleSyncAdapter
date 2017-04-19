package com.sixthsolution.lpisyncadapterapp;

import android.os.Bundle;

import com.sixthsolution.lpisyncadapter.authenticator.BaseLoginActivity;


/**
 * @author mehdok (mehdok@gmail.com) on 3/15/2017.
 */

public class CustomLoginActivity extends BaseLoginActivity {


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_custom_login);
        init();
    }
}
