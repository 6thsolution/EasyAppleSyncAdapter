package com.sixthsolution.lpisyncadapterapp;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.sixthsolution.lpisyncadapter.GlobalConstant;
import com.sixthsolution.lpisyncadapter.authenticator.crypto.Crypto;
import com.sixthsolution.lpisyncadapter.resource.LocalCalendar;
import com.sixthsolution.lpisyncadapter.resource.LocalEvent;
import com.sixthsolution.lpisyncadapter.resource.LocalResource;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Status;

import java.io.FileNotFoundException;
import java.net.URI;

import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.DateUtils;
import at.bitfire.ical4android.Event;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static com.sixthsolution.lpisyncadapter.GlobalConstant.AUTHTOKEN_TYPE_FULL_ACCESS;
import static com.sixthsolution.lpisyncadapterapp.MainActivity.ActionType.CREATE_EVENT;
import static com.sixthsolution.lpisyncadapterapp.MainActivity.ActionType.GET_TOKEN;
import static com.sixthsolution.lpisyncadapterapp.MainActivity.ActionType.SHOW_CALENDARS;
import static com.sixthsolution.lpisyncadapterapp.MainActivity.ActionType.SHOW_EVENTS;
import static com.sixthsolution.lpisyncadapterapp.MainActivity.ActionType.SYNC;
import static com.sixthsolution.lpisyncadapterapp.MainActivityPermissionsDispatcher.showAccountPickerWithCheck;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    private AccountManager accountManager;

    public enum ActionType {
        GET_TOKEN,
        SYNC,
        SHOW_CALENDARS,
        SHOW_EVENTS,
        CREATE_EVENT
    }

    ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            showMessage("Sync Finished");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        accountManager = AccountManager.get(this);

        findViewById(R.id.get_token).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountPickerWithCheck(MainActivity.this, AUTHTOKEN_TYPE_FULL_ACCESS, GET_TOKEN);
            }
        });
        findViewById(R.id.add_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewAccount();
            }
        });
        findViewById(R.id.sync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountPickerWithCheck(MainActivity.this, AUTHTOKEN_TYPE_FULL_ACCESS, SYNC);
            }
        });
        findViewById(R.id.show_calendars).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountPickerWithCheck(MainActivity.this, AUTHTOKEN_TYPE_FULL_ACCESS, SHOW_CALENDARS);
            }
        });
        findViewById(R.id.show_events).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountPickerWithCheck(MainActivity.this, AUTHTOKEN_TYPE_FULL_ACCESS, SHOW_EVENTS);
            }
        });
        findViewById(R.id.create_event).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountPickerWithCheck(MainActivity.this, AUTHTOKEN_TYPE_FULL_ACCESS, CREATE_EVENT);
            }
        });

        syncAutomatically();

        // register a content observer to notify when th sync is finished
        getContentResolver().registerContentObserver(GlobalConstant.CONTENT_URI, true, contentObserver);
    }

    public void addNewAccount() {
        final AccountManagerFuture<Bundle> future =
                accountManager.addAccount(AUTHTOKEN_TYPE_FULL_ACCESS, AUTHTOKEN_TYPE_FULL_ACCESS, null, null, this,
                                          new AccountManagerCallback<Bundle>() {
                                              @Override
                                              public void run(AccountManagerFuture<Bundle> future) {
                                                  try {
                                                      Bundle bnd = future.getResult();
                                                      showMessage("AddNewAccount Bundle is " + bnd);
                                                  } catch (Exception e) {
                                                      e.printStackTrace();
                                                      showMessage(e.getMessage());
                                                  }
                                              }
                                          }, null);
        accountManager.addAccount(AUTHTOKEN_TYPE_FULL_ACCESS, AUTHTOKEN_TYPE_FULL_ACCESS, null, null, this,
                                  new AccountManagerCallback<Bundle>() {
                                      @Override
                                      public void run(AccountManagerFuture<Bundle> future) {
                                          try {
                                              Bundle bnd = future.getResult();
                                              showMessage("AddNewAccount Bundle is " + bnd);
                                          } catch (Exception e) {
                                              e.printStackTrace();
                                              showMessage(e.getMessage());
                                          }
                                      }
                                  }, null);
    }

    @NeedsPermission({Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR})
    public void showAccountPicker(final String authTokenType, final ActionType actionType) {
        // get list of available account
        final Account availableAccounts[] = accountManager.getAccountsByType(AUTHTOKEN_TYPE_FULL_ACCESS);

        if (availableAccounts.length == 0) {
            showMessage("No accounts");
        } else {
            String name[] = new String[availableAccounts.length];
            for (int i = 0; i < availableAccounts.length; i++) {
                name[i] = availableAccounts[i].name;
            }

            // Account picker
            AlertDialog mAlertDialog = new AlertDialog.Builder(this).setTitle("Pick Account")
                    .setAdapter(
                            new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, name),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // invalidateAuthToken(availableAccounts[which], authTokenType);

                                    switch (actionType) {
                                        case GET_TOKEN:
                                            getExistingAccountAuthToken(availableAccounts[which], authTokenType);
                                            break;
                                        case SYNC:
                                            sync(availableAccounts[which]);
                                            break;
                                        case SHOW_CALENDARS:
                                            showCalendars(availableAccounts[which], SHOW_EVENTS);
                                            break;
                                        case SHOW_EVENTS:
                                            showCalendars(availableAccounts[which], SHOW_EVENTS);
                                            break;
                                        case CREATE_EVENT:
                                            showCalendars(availableAccounts[which], CREATE_EVENT);
                                    }
                                }
                            })
                    .create();
            mAlertDialog.show();
        }
    }

    private void invalidateAuthToken(final Account account, String authTokenType) {
        final AccountManagerFuture<Bundle> future =
                accountManager.getAuthToken(account, authTokenType, null, this, null, null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();

                    final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                    accountManager.invalidateAuthToken(account.type, authtoken);
                    Log.d("MainActivity", account.name + " invalidated");
                } catch (Exception e) {
                    e.printStackTrace();
                    showMessage(e.getMessage());
                }
            }
        }).start();
    }

    private void getExistingAccountAuthToken(Account account, String authTokenType) {
        final AccountManagerFuture<Bundle> future =
                accountManager.getAuthToken(account, authTokenType, null, this, null, null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();

                    String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN, "UTF-8");
                    authtoken = Crypto.armorDecrypt(authtoken, MainActivity.this);
                    showMessage(bnd.toString() + "\n encrypted user id: " + authtoken);
                } catch (Exception e) {
                    e.printStackTrace();
                    showMessage(e.getMessage());
                }
            }
        }).start();
    }

    private void showMessage(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * The {@link ContentResolver#setSyncAutomatically} and {@link ContentResolver#addPeriodicSync}
     * must call programmatically
     */
    private void syncAutomatically() {
        //get all account related to our app
        final Account availableAccounts[] = accountManager.getAccountsByType(AUTHTOKEN_TYPE_FULL_ACCESS);

        //set syncable status for all of them
        for (Account account : availableAccounts) {
            ContentResolver.setIsSyncable(account, GlobalConstant.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, GlobalConstant.AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, GlobalConstant.AUTHORITY, new Bundle(), 2 * 60 * 60);
        }
    }

    private void sync(Account account) {
        Bundle params = new Bundle();
        params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account, GlobalConstant.AUTHORITY, params);
    }

    private void showCalendars(Account account, final ActionType actionType) {
        try {
            //get list of local calendar associated to the account
            final LocalCalendar[] localCalendars =
                    (LocalCalendar[]) LocalCalendar.find(account,
                                                         // get contentProviderClient for our authority
                                                         getContentResolver().acquireContentProviderClient(
                                                                 GlobalConstant.AUTHORITY),
                                                         LocalCalendar.Factory.INSTANCE,
                                                         null,
                                                         null);

            if (localCalendars.length == 0) {
                showMessage("No calendar with this account");
            } else {
                String name[] = new String[localCalendars.length];
                for (int i = 0; i < localCalendars.length; i++) {
                    name[i] = localCalendars[i].getDisplayName();
                }

                AlertDialog mAlertDialog = new AlertDialog.Builder(this).setTitle("Pick Calendar")
                        .setAdapter(
                                new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1,
                                                         name),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (actionType) {
                                            case SHOW_EVENTS:
                                                showEvents(localCalendars[which]);
                                                break;
                                            case CREATE_EVENT:
                                                createTestEvent(localCalendars[which]);
                                                break;
                                        }

                                    }
                                })
                        .create();
                mAlertDialog.show();
            }
        } catch (CalendarStorageException e) {
            e.printStackTrace();
            showMessage(e.getMessage());
        }
    }

    private void showEvents(LocalCalendar localCalendar) {
        try {
            // get list of local calendar associated with selected calendar
            LocalResource[] localResources = localCalendar.getAll();
            if (localResources.length == 0) {
                showMessage("No event in this calendar");
            } else {
                String name[] = new String[localResources.length];
                for (int i = 0; i < localResources.length; i++) {
                    name[i] = ((LocalEvent) localResources[i]).getEvent().toString();
                }

                AlertDialog mAlertDialog = new AlertDialog.Builder(this).setTitle("See events")
                        .setAdapter(
                                new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1,
                                                         name),
                                null)
                        .create();
                mAlertDialog.show();
            }
        } catch (CalendarStorageException | FileNotFoundException e) {
            e.printStackTrace();
            showMessage(e.getMessage());
        }
    }

    private void createTestEvent(LocalCalendar calendar) {
        try {
            TimeZone tzVienna = DateUtils.tzRegistry.getTimeZone("Europe/Vienna");

            Event event = new Event();
            event.uid = "sample1@testAddEvent";
            event.summary = "Sample event";
            event.description = "Sample event with date/time";
            event.location = "Sample location";
            event.dtStart = new DtStart("20150501T120000", tzVienna);
            event.dtEnd = new DtEnd("20150501T130000", tzVienna);
            event.organizer = new Organizer(new URI("mailto:organizer@example.com"));
            event.rRule = new RRule("FREQ=DAILY;COUNT=10");
            event.forPublic = false;
            event.status = Status.VEVENT_CONFIRMED;

            // set an alarm one day, two hours, three minutes and four seconds before begin of event
            event.alarms.add(new VAlarm(new Dur(-1, -2, -3, -4)));

            // add two attendees
            event.attendees.add(new Attendee(new URI("mailto:user1@example.com")));
            event.attendees.add(new Attendee(new URI("mailto:user2@example.com")));

            // add exception with alarm and attendee
            Event exception = new Event();
            exception.recurrenceId = new RecurrenceId("20150502T120000", tzVienna);
            exception.summary = "Exception for sample event";
            exception.dtStart = new DtStart("20150502T140000", tzVienna);
            exception.dtEnd = new DtEnd("20150502T150000", tzVienna);
            exception.alarms.add(new VAlarm(new Dur(-2, -3, -4, -5)));
            exception.attendees.add(new Attendee(new URI("mailto:only.here@today")));
            event.exceptions.add(exception);

            // add EXDATE
            event.exDates.add(new ExDate(new DateList("20150502T120000", Value.DATE_TIME, tzVienna)));
            // add to calendar
            Uri uri = new LocalEvent(calendar, event, null, null).add();

            showMessage("Event created: " + uri);
            showEvents(calendar);
        } catch (Exception e) {
            e.printStackTrace();
            showMessage(e.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(contentObserver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
}

