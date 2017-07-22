Easy Apple Sync Adapter is an Android Library for syncing with **apple** calendar service.

Performing authentication and full duplex sync with **apple caldav** server.

_This library is based on [DavDroid](https://gitlab.com/bitfireAT/davdroid), and borrows many code from them.
 we just simplify the process of reusing the library_

## Features
* Easy to use.
* Powerful encryption for passwords.

## Installation
1) Configure your top-level `build.gradle` to include our repository
```groovy
allprojects {
    repositories {
        jcenter()
        maven { url "http://dl.bintray.com/6thsolution/public-maven" }
    }
}
```
Then config your app-level `build.gradle` to include the library as dependency:
``` groovy
compile 'com.sixthsolution:easyapplesyncadapter:1.0.0-beta1'
```

2) config: 

**Authenticator config** : Add Authenticator service to your manifest:
```xml
<service
    android:name="com.sixthsolution.lpisyncadapter.authenticator.ICalAuthenticatorService"
    android:exported="false"
    >
    <intent-filter>
        <action android:name="android.accounts.AccountAuthenticator"/>
    </intent-filter>

    <meta-data
        android:name="android.accounts.AccountAuthenticator"
        android:resource="@xml/authenticator"
        />
    <meta-data
        android:name="login_activity_class"
        android:value="com.sixthsolution.applecalendar.CustomLoginActivity"
        />
    <meta-data
        android:name="unique_authentication_type"
        android:value="com.sixthsolution.lpisyncadapter.ical_access"
        />
</service>
```
There is a few metadata that you can pass your parameters to the service via them:

1) `android.accounts.AccountAuthenticator`
The authenticator config file, something like this:
```xml
<account-authenticator 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accountType="com.sixthsolution.lpisyncadapter.ical_access"
    android:icon="@drawable/ical_icon"
    android:smallIcon="@drawable/ical_icon"
    android:label="@string/caldav_authenticator">
</account-authenticator>
```
2) `android:accountType` parameter must be the same value you passed via `unique_authentication_type` metadata.
this must be **unique**, otherwise your app may not work. so use something package specific.

3) `login_activity_class`
You can pass a custom login activity with custom ui for login process.
The value must be _complete path with package name_ like: `com.sixthsolution.applecalendar.CustomLoginActivity`.
Your activity must extend **BaseLoginActivity** and your layout must have some view that is necessary for login process.
two _EditText_ with exact id as `user_name` and `password` . and one _Button_ with id `signin_button` and
After calling _setContentView()_ in _onCreate()_ you must call **init()**.

**SyncAdapter config** :
You also need to add sync adapter service to your manifest:
```xml
<service
    android:name="com.sixthsolution.lpisyncadapter.syncadapter.ICalSyncService"
    android:exported="true"
    >
    <intent-filter>
        <action android:name="android.content.SyncAdapter"/>
    </intent-filter>
    <meta-data
        android:name="android.content.SyncAdapter"
        android:resource="@xml/sync_adapter"
    />
</service>
```
1) `sync_adapter` file is something like this:
```xml
<sync-adapter xmlns:android="http://schemas.android.com/apk/res/android"
    android:contentAuthority="com.android.calendar"
    android:accountType="com.sixthsolution.lpisyncadapter.ical_access"
    android:userVisible="true"
    android:allowParallelSyncs="false"
    android:isAlwaysSyncable="true"
    android:supportsUploading="true">
</sync-adapter>
```
2) `android:contentAuthority="com.android.calendar"` always must be it. (do NOT change it except when you need to).
3) `android:accountType` must be the same value you set for authenticator config.

## Usage
* **Add new account**:
You must call `AccountManager#addAccount` for adding new account. it automatically you the config and open
 `LoginActivity` and handle add progress by itself.
```java
accountManager.addAccount(AUTHTOKEN_TYPE_FULL_ACCESS, AUTHTOKEN_TYPE_FULL_ACCESS, null, null, this,
                                          new AccountManagerCallback<Bundle>() {
                                              @Override
                                              public void run(AccountManagerFuture<Bundle> future) {
                                                  try {
                                                      Bundle bnd = future.getResult();
                                                  } catch (Exception e) {
                                                      e.printStackTrace();
                                                  }
                                              }
                                          }, null);
```
* **Get list of available accounts**:
```java
AccountManager accountManager = AccountManager.get(this);
Account availableAccounts[] = accountManager.getAccountsByType(AUTHTOKEN_TYPE_FULL_ACCESS);
```
* **Requesting manual sync**:
```java
Bundle params = new Bundle();
params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
ContentResolver.requestSync(account, GlobalConstant.AUTHORITY, params);
```
* **Listening for sync finish**:
if you wana know when the sync finish or show  progress during progress use a `ContentObserver` :
```java
ContentObserver contentObserver = new ContentObserver(new Handler()) {
    @Override
    public void onChange(boolean selfChange, Uri uri) {
    super.onChange(selfChange, uri);
    showMessage("Sync Finished");
    }
};
```
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getContentResolver().registerContentObserver(GlobalConstant.CONTENT_URI, true, contentObserver);
}
```
do NOT forget to unregister it:
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    getContentResolver().unregisterContentObserver(contentObserver);
}
```
* **Get list of Calendars associated with account**:
```java
LocalCalendar[] localCalendars = (LocalCalendar[]) LocalCalendar.find(account,
                                                         // get contentProviderClient for your authority
                                                         getContentResolver().acquireContentProviderClient(GlobalConstant.AUTHORITY),
                                                         LocalCalendar.Factory.INSTANCE,
                                                         null,
                                                         null);
```
* **Get list of events associated with LocalCalendar**:
You can get list of all resource by:
```java
LocalResource[] localResources = localCalendar.getAll();
```
Then cast if to `LocalEvent`:
```java
(LocalEvent) localResources[i]
```

## License
```
Copyright 2016-2017 6thSolution Technologies Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
