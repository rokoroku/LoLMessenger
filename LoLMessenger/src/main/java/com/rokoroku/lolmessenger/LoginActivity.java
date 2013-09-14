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
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.achimala.leaguelib.connection.LeagueServer;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
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
    private CheckBox mCheckBoxRemember;
    private CheckBox mCheckBoxAuto;
    private Spinner mSpinner;
    private Button mLoginButton;

    // AdView object
    private AdView mAdView;

    // Chat Connection Object
    private Messenger mServiceMessenger = null;
    private Messenger mActivityMessenger = new Messenger( new IncomingHandler() );

    private boolean isLoginned = false;
    private boolean isBackButtonTouched = false;

    /**Class for interacting with the main interface of the service. */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LolMessengerApplication.setLoggingOn(true);
            mServiceMessenger = new Messenger(service);
            try{
                Log.i(TAG, "sending registering message to service");
                Message msg = Message.obtain(null, LolMessengerService.MSG_REGISTER_CLIENT);        //obtain 메서드는 메시지 풀에서 비슷한 메시지를 꺼내 재사용한다.
                msg.replyTo = mActivityMessenger;
                mServiceMessenger.send( msg );

                //login here
                String[] mCredential = { mId, mPassword };
                Bundle bundle = new Bundle();
                bundle.putStringArray("credential", mCredential);
                bundle.putInt("server", mServerSelection);

                msg = Message.obtain(null, LolMessengerService.MSG_ATTEMPT_LOGIN);
                msg.setData(bundle);
                msg.replyTo = mActivityMessenger;
                Log.i(TAG, "sending LOGIN attempt to Service, msg : " + msg);
                mServiceMessenger.send(msg);
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
                case LolMessengerService.MSG_LOGIN_PROGRESS_UPDATE:
                    String progress = msg.getData().getString("Progress");
                    Log.i(TAG, "get Progress Update msg, " + progress );
                    mLoginStatusMessageView.setText(progress);
                    break;

                case LolMessengerService.MSG_ATTEMPT_LOGIN:
                    showProgress(false);
                    LolMessengerApplication.setLoggingOn(false);
                    Bundle bundle = msg.getData();
                    switch ( bundle.getInt("result") ) {
                        case LolMessengerService.LOGIN_SUCCESS:
                            try {
                                msg = Message.obtain(null, LolMessengerService.MSG_UNREGISTER_CLIENT);
                                msg.replyTo = mActivityMessenger;
                                Log.i(TAG, "sending unregister msg" + msg);
                                mServiceMessenger.send(msg);
                                LolMessengerApplication.setConnected(true);
                                startService(new Intent(LoginActivity.this, LolMessengerService.class));
                            } catch (RemoteException e) {
                                mServiceMessenger = null;
                                e.printStackTrace();
                            }
                            startActivity(new Intent(LoginActivity.this, ClientActivity.class));

                            //set static user information
                            LolMessengerApplication.setUserAccount(bundle.getString("userAccount"));
                            LolMessengerApplication.setUserJID(bundle.getString("userJID"));
                            LolMessengerApplication.setUserName(bundle.getString("userName"));

                            //encrypt and save if user wants to remember
                            SharedPreferences prefs = LoginActivity.this.getSharedPreferences("account", Context.MODE_PRIVATE);
                            SharedPreferences.Editor ed = prefs.edit();

                            if(mCheckBoxRemember.isChecked()) try {
                                String key =  Settings.Secure.ANDROID_ID + "LOl" + mId + "Msgr";
                                ed.putString("userid", mId);
                                ed.putString("password", SimpleCrypto.encrypt(key , mPassword));
                                ed.putInt("server", mServerSelection);
                                ed.putBoolean("remember", mCheckBoxRemember.isChecked());
                                ed.putBoolean("auto", mCheckBoxAuto.isChecked());
                                ed.commit();
                            } catch (Exception e) {
                                e.printStackTrace();
                            } else {
                                ed.clear().commit();
                            }

                            isLoginned = true;
                            finish();
                            break;

                        case LolMessengerService.LOGIN_FAILED_WRONG_CREDENTIAL:
                            mPasswordView.setError(getString(R.string.error_check_password));
                            mPasswordView.requestFocus();
                            break;

                        case LolMessengerService.LOGIN_FAILED_TIMEOUT:
                            Toast.makeText(getApplicationContext(), getString(R.string.error_login_failed_timeout), Toast.LENGTH_SHORT).show();
                            break;

                        case LolMessengerService.LOGIN_FAILED_INTERRUPTED:
                            Toast.makeText(getApplicationContext(), getString(R.string.error_login_failed_interrupted), Toast.LENGTH_SHORT).show();
                            break;

                        case LolMessengerService.LOGIN_FAILED_SERVER_NOT_RESPONSE:
                            Toast.makeText(getApplicationContext(), getString(R.string.error_login_failed_not_responding), Toast.LENGTH_SHORT).show();
                            break;

                        case LolMessengerService.LOGIN_FAILED_CHECK_CONNECTION:
                            Toast.makeText(getApplicationContext(), getString(R.string.error_login_failed_check_connection), Toast.LENGTH_SHORT).show();
                            break;
                    }

                    if(mServiceMessenger != null) {
                        unbindService(mConnection);
                        mServiceMessenger = null;
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
        setContentView(R.layout.activity_login);

        /*
        if(Build.VERSION.SDK_INT < 11) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            getActionBar().hide();
        }*/

        // Set up the login form.
        mIdView = (EditText) findViewById(R.id.edit_ID);
        mPasswordView = (EditText) findViewById(R.id.edit_password);
        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);
        mCheckBoxRemember = (CheckBox) findViewById(R.id.checkBox);
        mCheckBoxAuto = (CheckBox) findViewById(R.id.checkBox2);
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mLoginButton = (Button) findViewById(R.id.sign_in_button);

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
        mSpinner.setPrompt(getString(R.string.spinner_login_title_choose_server));
        mSpinner.setAdapter(adapter);

        // Load remembered account
        SharedPreferences prefs = getSharedPreferences( "account", Context.MODE_PRIVATE);
        if(prefs.getBoolean("remember", false)) try {
            mId = prefs.getString("userid", null);
            String cryptedPassword = prefs.getString("password", null).trim();
            String key =  Settings.Secure.ANDROID_ID + "LOl" + mId + "Msgr";

            mPassword = SimpleCrypto.decrypt( key , cryptedPassword);
            mServerSelection = prefs.getInt("server", 0);

            mIdView.setText(mId);
            mPasswordView.setText(mPassword);
            mCheckBoxRemember.setChecked(prefs.getBoolean("remember", false));
            mCheckBoxAuto.setChecked(prefs.getBoolean("auto", false));
            mSpinner.setSelection(mServerSelection);

            if(mCheckBoxRemember.isChecked()) mCheckBoxAuto.setEnabled(true);

        } catch (Exception e) {
            // decode failed. ignore
        }

        //add listener to button
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mCheckBoxAuto.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCheckBoxAuto.playSoundEffect(android.view.SoundEffectConstants.CLICK);
            }
        });
        mCheckBoxRemember.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCheckBoxRemember.playSoundEffect(android.view.SoundEffectConstants.CLICK);
            }
        });

        mCheckBoxRemember.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mCheckBoxAuto.setEnabled(true);
                } else {
                    mCheckBoxAuto.setEnabled(false);
                    mCheckBoxAuto.setChecked(false);
                }
            }
        });

        mSpinner.setSoundEffectsEnabled(true);
        mLoginButton.setSoundEffectsEnabled(true);

    }

    @Override
    protected void onStart() {
        super.onStart();

        mIdView.clearFocus();
        mPasswordView.clearFocus();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        if(mCheckBoxAuto.isChecked()) {
            if(!getIntent().getBooleanExtra("fromClient", false)) {
                //Client에서 넘어오지 않은 경우 : 자동로그인.
                Log.i(TAG, "login automatically");
                attemptLogin();
            }
        } else {
            showAdView();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if(mLoginStatusView.getVisibility()==View.VISIBLE) {
            if (isBackButtonTouched == false) {
                isBackButtonTouched = true;
                Toast.makeText(this, getString(R.string.action_back_to_quit), Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isBackButtonTouched = false;
                    }
                }, 2000);
            }
            else  {
                if(mServiceMessenger != null) try {
                    //send cancel message
                    mServiceMessenger.send(Message.obtain(null, LolMessengerService.MSG_CANCEL_LOGIN));

                    //send unregister message
                    Message msg = Message.obtain(null, LolMessengerService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mActivityMessenger;
                    mServiceMessenger.send(msg);

                    //unbind service
                    unbindService(mConnection);
                    mServiceMessenger = null;

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this, getString(R.string.error_cancelled), Toast.LENGTH_SHORT).show();
                showProgress(false);
            }
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG," Destroyed");

        if(mServiceMessenger != null) try {
            unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!isLoginned) System.exit(0);

    }

    /**on Click button method.*/
    public void attemptLogin() {

        // hide keyboard
        try {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),InputMethodManager.RESULT_HIDDEN);
        } catch (NullPointerException e) {
            //ignore
        }

        isBackButtonTouched = false;

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
            return;

        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            bindService(new Intent(LoginActivity.this, LolMessengerService.class), mConnection, Context.BIND_AUTO_CREATE);
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

        if(show == false) {
            showAdView();
        }
    }

    private void showAdView() {
        /**add adview**/
        if(mAdView == null) {
            mAdView = new AdView(this, AdSize.BANNER, "a152219a82d191b");
            LinearLayout adLayout = (LinearLayout) findViewById(R.id.adFrame);
            adLayout.addView(mAdView);
            AdRequest adRequest = new AdRequest();
            adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
            mAdView.loadAd(adRequest);
        }
    }
}