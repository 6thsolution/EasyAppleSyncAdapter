package com.sixthsolution.lpisyncadapter.authenticator;

import android.os.Bundle;

import com.sixthsolution.lpisyncadapter.R;


public class LoginActivity extends BaseLoginActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        init();
    }
}
