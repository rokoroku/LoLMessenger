package com.rokoroku.lolmessenger;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.achimala.leaguelib.connection.LeagueServer;
import com.rokoroku.lolmessenger.utilities.SimpleCrypto;

import java.util.ArrayList;

/**
 * Created by Youngrok Kim on 13. 8. 13.
 */
public class LoginActivity extends Activity {

    protected static final String TAG = "com.rokoroku.lolmessenger.LoginActivity";

    // Values for email and password at the time of the login attempt.
    private String mId;
    private String mPassword;
    private int mServerSelection;

    // UI references.
    private EditText mIdView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;
    private CheckBox mCheckBox;
    private Spinner mSpinner;

    // Chat Connection Object
    private Messenger mServiceMessenger = null;
    private Messenger mActivityMessenger = new Messenger( new IncomingHandler() );

    /**Class for interacting with the main interface of the service. */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mServiceMessenger = new Messenger(service);
            try{
                Log.i(TAG, "sending registering message to service");
                Message msg = Message.obtain(null, LolMessengerService.MSG_REGISTER_CLIENT);        //obtain 메서드는 메시지 풀에서 비슷한 메시지를 꺼내 재사용한다.
                msg.replyTo = mActivityMessenger;
                mServiceMessenger.send( msg );

            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
                e.printStackTrace();
            }

        }
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed
            // Message msg = Message.obtain(null, LolMessengerService.MSG_UNREGISTER_CLIENT);
            mServiceMessenger = null;
        }
    };

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage( Message msg ){
            switch ( msg.what ){
                case LolMessengerService.MSG_PROGRESS_UPDATE:
                    String progress = msg.getData().getString("Progress");
                    Log.i(TAG, "get Progress Update msg, " + progress );
                    mLoginStatusMessageView.setText(progress);
                    break;

                case LolMessengerService.MSG_ATTEMPT_LOGIN:
                    showProgress(false);
                    if( msg.arg1 == LolMessengerService.LOGIN_SUCCESS ) { //LOGIN == TRUE
                        try {
                            msg = Message.obtain(null, LolMessengerService.MSG_UNREGISTER_CLIENT);
                            msg.replyTo = mActivityMessenger;
                            Log.i(TAG, "sending unregister msg" + msg);
                            mServiceMessenger.send(msg);
                            startService(new Intent(LoginActivity.this, LolMessengerService.class));

                        } catch (RemoteException e) {
                            mServiceMessenger = null;
                            e.printStackTrace();
                        }
                        startActivity(new Intent(LoginActivity.this, ClientActivity.class));

                        //encrypt and save if user wants to remember
                        SharedPreferences prefs = LoginActivity.this.getSharedPreferences("account", Context.MODE_PRIVATE);
                        SharedPreferences.Editor ed = prefs.edit();
                        if(mCheckBox.isChecked()) try {
                            ed.putString("userid", mId);
                            ed.putString("password", SimpleCrypto.encrypt(mId + "LOLbasrMgSGR" , mPassword));
                            ed.putInt("server", mServerSelection);
                            ed.putBoolean("remember", true);
                            ed.commit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } else {
                            ed.putBoolean("remember", false);
                            ed.commit();
                        }
                        finish();

                    } else if (msg.arg1 == LolMessengerService.LOGIN_FAILED) {
                        mPasswordView.setError("Check password");
                        mPasswordView.requestFocus();
                    } else if (msg.arg1 == LolMessengerService.LOGIN_FAILED_TIMEOUT) {
                        Toast.makeText(getApplicationContext(), "Connection timeout, try again later", Toast.LENGTH_SHORT).show();
                    } else if (msg.arg1 == LolMessengerService.LOGIN_FAILED_INTERRUPTED) {
                        Toast.makeText(getApplicationContext(), "Login task interrupted", Toast.LENGTH_SHORT).show();
                    } else if (msg.arg1 == LolMessengerService.LOGIN_FAILED_SERVER_NOT_RESPONSE) {
                        Toast.makeText(getApplicationContext(), "Server not responding, try again later", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    super.handleMessage( msg );
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG," Created");
        //mBinder = mServiceMessenger.getBinder();
        bindService(new Intent(LoginActivity.this, LolMessengerService.class), mConnection, Context.BIND_AUTO_CREATE);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mIdView = (EditText) findViewById(R.id.edit_ID);
        mPasswordView = (EditText) findViewById(R.id.edit_Password);
        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);
        mCheckBox = (CheckBox) findViewById(R.id.checkBox);
        mSpinner = (Spinner) findViewById(R.id.spinner);

        // Load remembered preferences
        SharedPreferences prefs = getSharedPreferences( "account", Context.MODE_PRIVATE);
        if(prefs.getBoolean("remember", false)) try {
            mId = prefs.getString("userid", null);
            String cryptedPassword = prefs.getString("password", null).trim();
            mPassword = SimpleCrypto.decrypt( mId + "LOLbasrMgSGR", cryptedPassword);

            mServerSelection = prefs.getInt("server", 0);

            mIdView.setText(mId);
            mPasswordView.setText(mPassword);
            mCheckBox.setChecked(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList serverList = new ArrayList<String>();
        serverList.add(LeagueServer.KOREA.getPublicName());
        serverList.add(LeagueServer.NORTH_AMERICA.getPublicName());
        serverList.add(LeagueServer.EUROPE_WEST.getPublicName());
        serverList.add(LeagueServer.EUROPE_NORDIC_AND_EAST.getPublicName());
        serverList.add(LeagueServer.BRAZIL.getPublicName());
        serverList.add(LeagueServer.LATIN_AMERICA_NORTH.getPublicName());
        serverList.add(LeagueServer.LATIN_AMERICA_SOUTH.getPublicName());
        serverList.add(LeagueServer.OCEANIA.getPublicName());

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, serverList);
        mSpinner.setPrompt("Choose server to connect");
        mSpinner.setAdapter(adapter);

        //add listener to button
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        if(mLoginStatusView.getVisibility()!=View.VISIBLE) {
            super.onBackPressed();
        }/* below code doesn't work ..
        else try {
            showProgress(false);
            Message msg = Message.obtain(null, LolMessengerService.MSG_CANCEL_LOGIN);
            msg.replyTo = mActivityMessenger;
            Log.i(TAG, "sending cancel msg" + msg);
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG," Destroyed");

        if(mServiceMessenger != null) {
            unbindService(mConnection);
        }
    }

    /**on Click button method.*/
    public void attemptLogin() {

        // hide keyboard
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        // Reset errors.
        mIdView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mId = mIdView.getText().toString();
        mPassword = mPasswordView.getText().toString();
        mServerSelection = mSpinner.getSelectedItemPosition();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mId)) {
            mIdView.setError(getString(R.string.error_field_required));
            focusView = mIdView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);

            String[] mCredential = { mId, mPassword };
            Bundle bundle = new Bundle();
            bundle.putStringArray("credential", mCredential);
            bundle.putInt("server", mServerSelection);
            Message msg = Message.obtain(null, LolMessengerService.MSG_ATTEMPT_LOGIN);
            msg.setData(bundle);
            msg.replyTo = mActivityMessenger;
            Log.i(TAG, "sending LOGIN attempt to Service, msg : " + msg);
            try {
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }


        }
    }

    /**Shows the progress UI and hides the login form.*/
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

}