package com.rokoroku.lolmessenger;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.rokoroku.lolmessenger.classes.ParcelableMatchHistory;
import com.rokoroku.lolmessenger.classes.ParcelableRoster;
import com.rokoroku.lolmessenger.fragments.BuddyListFragment;
import com.rokoroku.lolmessenger.fragments.ChatListFragment;
import com.rokoroku.lolmessenger.fragments.ConfirmationDialog;
import com.rokoroku.lolmessenger.fragments.SettingFragment;
import com.rokoroku.lolmessenger.fragments.SummonerDialog;
import com.rokoroku.lolmessenger.fragments.SummonerInformationDialogFragment;
import com.rokoroku.lolmessenger.utilities.SQLiteDbAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientActivity extends FragmentActivity {

    protected static final String TAG = "com.rokoroku.lolmessenger.ClientActivity";
    private Boolean isReady = false;        // flag that UI Contents are ready
    private Boolean isRestart = false;      // flag for restarting from login activity
    private Boolean isExiting = false;      // flag for exiting the application

    private Boolean isSummonerUpdated = false;
    private Boolean isRosterUpdated = false;
    private Boolean isChatUpdated = false;

    //UI References
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private static final int NUM_PAGES = 3;

    private RelativeLayout mBuddyTabButton;
    private RelativeLayout mChatTabButton;
    private RelativeLayout mSettingTabButton;
    private TextView mBuddyCount;
    private TextView mChatCount;
    private Spinner mSpinner;

    private TextView mUserNameView;
    private TextView mUserStatusView;
    private ImageView mUserStatusIcon;

    private SummonerInformationDialogFragment mSummonerInformationDialog = null;
    private SummonerDialog mSummonerDialog = null;

    // Roster
    private String mUserID = null;
    private String mUserAccount = null;
    private ParcelableRoster mUserPresence = null;
    private Map<String, ParcelableRoster> mRosterMap = null;
    private ArrayList<String> mRosterGroupList = null;
    private Map<String, String> mUnknownRosterMap = new HashMap<String, String>();

    // Roster Subscription variable
    private ArrayList<Bundle> mSubscriptionRequest = new ArrayList<Bundle>();

    // temporary information for user summoner information
    private ParcelableRoster mUserPresence2 = null;
    private ArrayList<ParcelableMatchHistory>  mMatchHistory = null;
    private Date mFWOTD = null;

    // Messages managing
    private SQLiteDbAdapter mSQLiteDbAdapter;

    // Chat Connection Object
    private Messenger mServiceMessenger = null;
    private Messenger mClientMessenger = new Messenger( new ClientHandler() );

    /**Class for interacting with the main interface of the service. */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mServiceMessenger = new Messenger(service);
            try{
                Log.i(TAG, "sending registering message to service");
                Message msg = Message.obtain(null, LolMessengerService.MSG_REGISTER_CLIENT);
                msg.replyTo = mClientMessenger;
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
            mServiceMessenger = null;
        }
    };

    public class ClientHandler extends Handler {
        @Override
        public void handleMessage( Message msg ){
            Bundle bundle = null;
            switch ( msg.what ){
                case LolMessengerService.MSG_REGISTER_CLIENT:
                    Log.i(TAG, "Connected with service");
                    isReady = true;

                    //set static user information
                    bundle = msg.getData();
                    if(bundle.getString("userAccount") != null ) LolMessengerApplication.setUserAccount(bundle.getString("userAccount"));
                    if(bundle.getString("userJID") != null)      LolMessengerApplication.setUserJID(bundle.getString("userJID"));
                    if(bundle.getString("userName") != null )    LolMessengerApplication.setUserName(bundle.getString("userName"));

                    refreshRoster();
                    break;

                case LolMessengerService.MSG_REQUEST_ROSTER:
                    Log.i(TAG, "Get Roster Entries from service");
                    isRosterUpdated = true;
                    isChatUpdated = true; //since roster's status must be updated together

                    bundle = msg.getData();
                    bundle.setClassLoader(ParcelableRoster.class.getClassLoader());
                    mRosterMap = new HashMap<String, ParcelableRoster>();
                    ArrayList<ParcelableRoster> ParcelableRosterList = bundle.getParcelableArrayList("RosterList");
                    for (ParcelableRoster entry : ParcelableRosterList) {
                        mRosterMap.put(entry.getUserID(), entry);
                    }

                    mRosterGroupList = bundle.getStringArrayList("RosterGroupList");
                    mUserID = bundle.getString("UserID");
                    mUserAccount = bundle.getString("UserAccount");
                    mUserPresence = bundle.getParcelable("UserPresence");

                    if(mPagerAdapter == null) {
                        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
                        mPager.setAdapter(mPagerAdapter);
                        mPager.setOffscreenPageLimit(NUM_PAGES);
                        mSpinner.setAdapter(new statusSpinnerAdapter(ClientActivity.this, R.layout.item_dropdown_status));
                        mSpinner.setSelected(false);
                    }

                    refreshView();

                    break;

                case LolMessengerService.MSG_REQUEST_MATCH:
                    Log.i(TAG, "Get Match Histories from service" + msg);
                    bundle = msg.getData();
                    bundle.setClassLoader(ParcelableMatchHistory.class.getClassLoader());
                    bundle.setClassLoader(ParcelableRoster.class.getClassLoader());
                    if(!bundle.getBoolean("error")) {
                        ArrayList<ParcelableMatchHistory> newMatchHistory = msg.getData().getParcelableArrayList("matchList");
                        ParcelableRoster roster = bundle.getParcelable("roster");
                        Date fwotd = new Date(msg.getData().getLong("FWOTD"));

                        if(!isReady || mUserID.contains(roster.getUserID())) {
                            isSummonerUpdated = true;
                            mUserPresence2 = roster;
                            mMatchHistory = newMatchHistory;
                            mFWOTD = fwotd;
                            refreshView();
                        }
                        else if(mSummonerInformationDialog != null) {
                            mSummonerInformationDialog.setFWOTD(fwotd);
                            mSummonerInformationDialog.setMatchHistory(newMatchHistory);
                            mSummonerInformationDialog.setRoster(roster);
                            mSummonerInformationDialog.refreshSummoner();
                        }
                    } else {
                        if(mSummonerInformationDialog != null) {
                            mSummonerInformationDialog.dismiss();
                            Toast.makeText(getApplicationContext(), getString(R.string.error_failure_retrieving_summoner), Toast.LENGTH_SHORT).show();
                            mSummonerInformationDialog = null;
                        }
                    }
                    break;

                case LolMessengerService.MSG_REQUEST_SUMMONER_NAME:
                    Log.i(TAG, "Get unknown entry name from service" + msg.getData().getString("id") + " / " + msg.getData().getString("name") );
                    mUnknownRosterMap.put( msg.getData().getString("id"), msg.getData().getString("name") );
                    isChatUpdated = true;
                    refreshView();
                    break;

                case LolMessengerService.MSG_CHAT_RECEIVE_MESSAGE:
                    Log.i(TAG, "Get new message from service");
                    isChatUpdated = true;
                    if(isReady) refreshView();
                    break;

                case LolMessengerService.MSG_SUBSCRIBE_ROSTER:
                    Log.i(TAG, "Get subscribtion message from service: " + msg.getData() );
                    if(isReady) {
                        openSubscriptionDialog(msg.getData());
                    } else {
                        mSubscriptionRequest.add(msg.getData());
                    }
                    break;

                case LolMessengerService.MSG_SERVICE_DEAD:
                    Log.i(TAG, "service is dead, finish activity");
                    LolMessengerApplication.setConnected(false);
                    //isRestart = false;
                    //isExiting = true;
                    finish();
                    break;

                default:
                    super.handleMessage( msg );
            }
        }
    }

    class statusSpinnerAdapter extends ArrayAdapter {
        private ArrayList<String> mList;
        private LayoutInflater mInflater;
        private Activity mActivity;
        private View tempview;

        statusSpinnerAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            this.mActivity = (Activity) context;
            this.mInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            this.tempview = mInflater.inflate(R.layout.item_user_indicator, null);
            this.setDropDownViewResource(textViewResourceId);
            this.mList = new ArrayList<String>( Arrays.asList(getResources().getStringArray(R.array.client_spinner)) );
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {

            /*
            if(convertView == null) {
                convertView = mInflater.inflate(R.layout.item_user_indicator, null);
                convertView.setTag(1011);
            } else if(!convertView.getTag().equals(1011)) {
                convertView = mInflater.inflate(R.layout.item_user_indicator, null);
                convertView.setTag(1011);
            }*/

            mUserNameView = (TextView) tempview.findViewById(R.id.text_user);
            mUserStatusView = (TextView) tempview.findViewById(R.id.text_userstatus);

            if(mUserPresence.getUserName()!=null) { mUserNameView.setText(mUserPresence.getUserName()); }
            else mUserNameView.setText(mUserAccount);

            return tempview;
        }

        @Override
         public View getDropDownView(int position, View convertView, ViewGroup parent) {
            // Return a view which appears in the spinner list.
            // Ignoring convertView to make things simpler, considering
            // we have different types of views. If the list is long, think twice!
            mSpinner.setSelected(false);
            if(convertView == null) {
                convertView = mInflater.inflate(R.layout.item_dropdown_status, null);
                convertView.setTag(1010);
            } else if(!convertView.getTag().equals(1010)) {
                convertView = mInflater.inflate(R.layout.item_dropdown_status, null);
                convertView.setTag(1010);
            }
            TextView text = (TextView) convertView.findViewById(R.id.textView);
            text.setText(mList.get(position));
            return convertView;
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT < 11) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            getActionBar().hide();
        }

        /*
        if(!isRunningChatService()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }*/

        //set content view
        setContentView(R.layout.activity_client_new);

        /**add adview**/
        AdView adView = new AdView(this, AdSize.BANNER, "a152219a82d191b");
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.adFrame);
        adLayout.addView(adView);
        AdRequest adRequest = new AdRequest();
        adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
        adView.loadAd(adRequest);

        //bind service
        Intent connectIntent = new Intent(ClientActivity.this, LolMessengerService.class);
        if(mServiceMessenger == null) bindService(connectIntent, mConnection, Context.BIND_AUTO_CREATE);

        //start SQLite
        mSQLiteDbAdapter = new SQLiteDbAdapter(this);

        //set spinner
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mSpinner.setSoundEffectsEnabled(true);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG, "spinner selected : " + i);
                switch (i) {
                    case 0: //chat
                        mUserPresence.setMode("chat");
                        mUserPresence.setAvailablity("available");
                        break;
                    case 1: //away
                        mUserPresence.setMode("away");
                        mUserPresence.setAvailablity("available");
                        break;
                    case 2: //offline
                        mUserPresence.setMode("");
                        mUserPresence.setAvailablity("unavailable");
                        break;
                    case 3: //setting
                        if(mUserPresence.getAvailablity().equals("unavailable")) mSpinner.setSelection(2);
                        else if(mUserPresence.getMode().equals("chat")) mSpinner.setSelection(0);
                        else if(mUserPresence.getMode().equals("away")) mSpinner.setSelection(1);
                        openSettingDialog();
                        return;
                    case 4: //exit
                        if(mUserPresence.getAvailablity().equals("unavailable")) mSpinner.setSelection(2);
                        else if(mUserPresence.getMode().equals("chat")) mSpinner.setSelection(0);
                        else if(mUserPresence.getMode().equals("away")) mSpinner.setSelection(1);
                        doLogout(false);
                        return;
                }

                updatePresence(mUserPresence);
                mUserNameView.setText(mUserAccount);
                refreshStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

        });

        //init fragment managers
        mPager = (ViewPager) findViewById(R.id.pager);
        //below will be called after service connected
        //mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        //mPager.setAdapter(mPagerAdapter);

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
                refreshView();
            }

            @Override
            public void onPageSelected(int i) {
                switch(i) {
                    case 0:
                        mBuddyTabButton.setAlpha((float) 1);
                        mBuddyTabButton.setBackgroundResource(R.drawable.tab_underbar);
                        mChatTabButton.setAlpha((float) 0.3);
                        mChatTabButton.setBackgroundColor(android.R.color.transparent);
                        mSettingTabButton.setAlpha((float) 0.3);
                        mSettingTabButton.setBackgroundColor(android.R.color.transparent);
                        break;
                    case 1:
                        mBuddyTabButton.setAlpha((float)0.3);
                        mBuddyTabButton.setBackgroundColor(android.R.color.transparent);
                        mChatTabButton.setAlpha((float)1);
                        mChatTabButton.setBackgroundResource(R.drawable.tab_underbar);
                        mSettingTabButton.setAlpha((float)0.3);
                        mSettingTabButton.setBackgroundColor(android.R.color.transparent);
                        break;
                    case 2:
                        mBuddyTabButton.setAlpha((float)0.3);
                        mBuddyTabButton.setBackgroundColor(android.R.color.transparent);
                        mChatTabButton.setAlpha((float) 0.3);
                        mChatTabButton.setBackgroundColor(android.R.color.transparent);
                        mSettingTabButton.setAlpha((float)1);
                        mSettingTabButton.setBackgroundResource(R.drawable.tab_underbar);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        mBuddyTabButton = (RelativeLayout) findViewById(R.id.tab_indi1);
        mChatTabButton = (RelativeLayout) findViewById(R.id.tab_indi2);
        mSettingTabButton = (RelativeLayout) findViewById(R.id.tab_indi3);
        mBuddyCount = (TextView) findViewById(R.id.text_buddycount);
        mChatCount = (TextView) findViewById(R.id.text_chatcount);
        mUserStatusIcon = (ImageView) findViewById(R.id.statusIcon);

        mBuddyTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPager.setCurrentItem(0, true);
            }
        });
        mChatTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPager.setCurrentItem(1, true);
            }
        });
        mSettingTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPager.setCurrentItem(2, true);
            }
        });
    }

    // manage Fragment in Pager
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        private Map<Integer, Fragment> mPageReferenceMap = new HashMap<Integer, Fragment>();

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch(position) {
                case 0:
                    fragment = new BuddyListFragment(ClientActivity.this, mUserID);
                    break;
                case 1:
                    fragment = new ChatListFragment(ClientActivity.this, mUserID);
                    break;
                case 2:
                    fragment = new SummonerInformationDialogFragment(ClientActivity.this, mUserID);
                    break;
                default:
                    break;
            }
            mPageReferenceMap.put(position, fragment);
            return fragment;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mPageReferenceMap.remove(position);
        }

        public Fragment getFragment(int key) {
            return mPageReferenceMap.get(key);
        }

    }

    @Override
    protected void onDestroy() {
        if(isReady) {
            mSQLiteDbAdapter.close();
            /*
            Message msg = Message.obtain(null, LolMessengerService.MSG_UNREGISTER_CLIENT);
            msg.replyTo = mClientMessenger;
            try {
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            unbindService(mConnection);
            */
        }
        Log.i(TAG," Destroyed, restart: " + isRestart );

        if(isRestart) {
            Log.i(TAG," Sending Restart Intent...");
            Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
            //Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("fromClient",true);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            super.onDestroy();

        } else {
            System.exit(0);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            moveTaskToBack(true);
            return;
            //super.onBackPressed();
        } else {
            mPager.setCurrentItem( 0 );
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( keyCode == KeyEvent.KEYCODE_MENU ) {
            //Put the code for an action menu from the top here
            mSpinner.setSoundEffectsEnabled(false);
            mSpinner.performClick();
            mSpinner.setSoundEffectsEnabled(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        //unregister
        if(isReady == true) {
            if (mServiceMessenger != null) try {
                Message msg = Message.obtain(null, LolMessengerService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = mClientMessenger;
                mServiceMessenger.send(msg);
                mServiceMessenger = null;
                unbindService(mConnection);
            } catch (Exception e) {
                //Log.e(TAG, "service already destroyed");
                e.printStackTrace();
                //finish();
            }
        }

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
        if(isReady) outState.putInt("PAGER", mPager.getCurrentItem());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(isReady) mPager.setCurrentItem(savedInstanceState.getInt("PAGER"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isRunningChatService()) {
            isRestart = true;
            finish();
            return;
        }

        //add
        if(mServiceMessenger == null) {
            Intent connectIntent = new Intent(this, LolMessengerService.class);
            bindService(connectIntent, mConnection, 0);
        } else try {
            Log.i(TAG, "sending registering message to service");
            Message msg = Message.obtain(null, LolMessengerService.MSG_REGISTER_CLIENT);
            msg.replyTo = mClientMessenger;
            mServiceMessenger.send(msg);

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        if(isReady) refreshView();
    }

    public void refreshRoster() {

        //send roster request message to service
        Message msg = Message.obtain(null, LolMessengerService.MSG_REQUEST_ROSTER);
        msg.replyTo = mClientMessenger;
        if (mServiceMessenger != null) try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }

        //refreshed by message handler.
        //end.

    }

    public void requestMatchHistory(String userID) {

        if(isReady) {
            if(userID.equals(mUserID)) {
                if(mMatchHistory != null) {
                    refreshView();
                    return;
                }
            }
        }

        Log.i(TAG, "request Match history message, replyTo:" + mClientMessenger);
        Message msg = Message.obtain(null, LolMessengerService.MSG_REQUEST_MATCH);
        msg.replyTo = mClientMessenger;
        Bundle bundle = new Bundle();
        bundle.putString("userID", userID);
        msg.setData(bundle);
        if (mServiceMessenger != null) try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }

    }

    public void openSubscriptionDialog(final Bundle bundle) {
        ConfirmationDialog dialog = new ConfirmationDialog();
        dialog.setDialogTitle(getString(R.string.dialog_title_buddy_subscription));
        dialog.setDialogContent(String.format(getString(R.string.dialog_content_buddy_subscription), bundle.getString("name")));
        //"A friend request from" + "\n" + bundle.getString("name") + "\n" + "arrived. Do you want to accept?");
        dialog.setPositiveButtonText(getString(R.string.action_accept));
        dialog.setNegativeButtonText(getString(R.string.action_reject));
        dialog.setPositiveOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message msg = Message.obtain(null, LolMessengerService.MSG_SUBSCRIBE_ROSTER);
                msg.replyTo = mClientMessenger;
                msg.arg1 = 1;
                msg.setData(bundle);
                if (mServiceMessenger != null) try {
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        dialog.setNegativeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message msg = Message.obtain(null, LolMessengerService.MSG_SUBSCRIBE_ROSTER);
                msg.replyTo = mClientMessenger;
                msg.arg1 = 0;
                msg.setData(bundle);
                if (mServiceMessenger != null) try {
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        dialog.show(ft, "dialog");
    }

    public void openSummonerDialog(String userID) {
        mSummonerDialog = new SummonerDialog(ClientActivity.this, mRosterMap.get(userID));
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        mSummonerDialog.show(ft, "dialog");
    }

    public void openSummonerInformation(String userID) {
        mSummonerInformationDialog = new SummonerInformationDialogFragment(ClientActivity.this, userID);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        //ft.addToBackStack(null);

        // Create and show the dialog.
        requestMatchHistory(userID);
        mSummonerInformationDialog.show(ft, "dialog");
    }

    public void openSettingDialog() {
        SettingFragment dialog = new SettingFragment(ClientActivity.this, mUserPresence);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        dialog.show(ft, "dialog");
        dialog.refreshSettings();
    }

    public void openChat(String buddyID) {

        Log.i(TAG,"open chat");
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("userID", mUserID);
        intent.putExtra("buddyID", buddyID);
        intent.putExtra("buddy", mRosterMap.get(buddyID));
        intent.setExtrasClassLoader(ParcelableRoster.class.getClassLoader());
        startActivity(intent);

        isChatUpdated = true;
    }

    public void updatePresence(ParcelableRoster userPresence) {
        mUserPresence = userPresence;

        Message msg = Message.obtain(null, LolMessengerService.MSG_UPDATE_PRESENCE);
        msg.replyTo = mClientMessenger;

        Bundle bundle = new Bundle();
        bundle.setClassLoader(ParcelableRoster.class.getClassLoader());
        bundle.putParcelable("userPresence", userPresence);
        msg.setData(bundle);

        if (mServiceMessenger != null) try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateSetting(Bundle bundle) {

        ((LolMessengerApplication)getApplication()).updateSettings(bundle);

        Message msg = Message.obtain(null, LolMessengerService.MSG_UPDATE_SETTING);
        msg.replyTo = mClientMessenger;
        msg.setData(bundle);
        if (mServiceMessenger != null) try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void doLogout(final boolean restart) {
        ConfirmationDialog dialog = new ConfirmationDialog();
        dialog.setDialogTitle( restart ? getString(R.string.dialog_title_logout) : getString(R.string.dialog_title_exit));
        dialog.setDialogContent( restart ? getString(R.string.dialog_content_logout) : getString(R.string.dialog_content_exit));
        dialog.setPositiveOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message msg = Message.obtain(null, LolMessengerService.MSG_STOP_SERVICE);
                msg.replyTo = mClientMessenger;
                msg.arg1 = restart ? 1 : 0; //restart application
                if (mServiceMessenger != null) try {
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                isRestart = restart;
                isExiting = !restart;

                if(isRestart) {
                    SharedPreferences prefs = getSharedPreferences("account", Context.MODE_PRIVATE);
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putBoolean("auto", false);
                    ed.commit();
                }

                Log.i(TAG, "logout");
                ClientActivity.this.finish();
            }
        });

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");

        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        dialog.show(ft, "dialog");
    }

    public Map<String, ParcelableRoster> getRosterMap() {
        return mRosterMap;
    }

    public ParcelableRoster getRoster(String userID) {
        return mRosterMap.get(userID);
    }

    public String getUnknownRosterName(String userID) {
        String result = mUnknownRosterMap.get(userID);
        if(result == null) try {
            result = getString(R.string.unknown_entry);
            Message message = Message.obtain(null, LolMessengerService.MSG_REQUEST_SUMMONER_NAME);
            Bundle bundle = new Bundle();
            bundle.putString("id", userID);
            message.setData(bundle);
            mServiceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getUserID() {
        return mUserID;
    }

    public String getUserAccount() {
        return mUserAccount;
    }

    public ParcelableRoster getUserPresence() {
        return mUserPresence;
    }

    public SQLiteDbAdapter getSQLiteDbAdapter() {
        isChatUpdated = true;
        return mSQLiteDbAdapter;
    }

    public Boolean isReady() {
        return isReady;
    }

    public ArrayList<String> getRosterGroupList() {
        return mRosterGroupList;
    }

    public void refreshView() {

        if(mSubscriptionRequest.size() > 0) {
            openSubscriptionDialog(mSubscriptionRequest.get(0));
            mSubscriptionRequest.remove(0);
        }

        if(isRosterUpdated) {
            int count=0 ;
            for(ParcelableRoster roster : mRosterMap.values()) {
                if(roster.getAvailablity().equals("available")) count++;
            }
            mBuddyCount.setText(String.valueOf(count));
        }

        if(isChatUpdated) {
            mChatCount.setText(String.valueOf(mSQLiteDbAdapter.getNotReadMessageCount(mUserID)));
        }

        Fragment currentFragment = ((ScreenSlidePagerAdapter)mPagerAdapter).getFragment(mPager.getCurrentItem());
        if(currentFragment != null ) {
            switch(mPager.getCurrentItem()) {
                case 0:
                    if(isRosterUpdated) {
                        Log.i(TAG,"Roster view updated");
                        ((BuddyListFragment) currentFragment ).refreshBuddyList();
                        isRosterUpdated = false;
                    }
                    break;
                case 1:
                    if(isChatUpdated) {  //상태(오프라인) 바뀌는거 표시해주기위해 Roster도 포함
                        Log.i(TAG,"Chatlist view updated");
                        ((ChatListFragment) currentFragment ).refreshChatList();
                        isChatUpdated = false;
                    }
                    break;
                case 2:
                    if(isSummonerUpdated) {
                        Log.i(TAG,"summoner view updated");
                        ((SummonerInformationDialogFragment) currentFragment ).setRoster(mUserPresence2);
                        ((SummonerInformationDialogFragment) currentFragment ).setFWOTD(mFWOTD);
                        ((SummonerInformationDialogFragment) currentFragment ).setMatchHistory(mMatchHistory);
                        ((SummonerInformationDialogFragment) currentFragment ).refreshSummoner();
                        isSummonerUpdated = false;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void dismissSummonerInformationDialog() {
        mSummonerInformationDialog = null;
    }

    public void refreshStatus() {

        String statusString;
        if(mUserPresence.getAvailablity().equals("available")) {
            if(mUserPresence.getMode().equals("dnd")) {
                if(mUserPresence.getGameStatus().equals("inGame")) {
                    statusString = getString(R.string.status_inGame) + " : " + mUserPresence.getSkinname() + " (" + ((System.currentTimeMillis() - mUserPresence.getTimeStamp())/60000) + getString(R.string.status_timestamp_minute);
                }
                else if(mUserPresence.getGameStatus().equals("teamSelect")) {
                    statusString = getString(R.string.status_teamSelect);
                }
                else if(mUserPresence.getGameStatus().equals("hostingPracticeGame")) {
                    statusString = getString(R.string.status_hostingPracticeGame);
                }
                else {
                    statusString = getString(R.string.status_lookingNewGame);
                }
                mUserStatusIcon.setImageResource(R.drawable.icon_yellow);
                mUserStatusView.setTextColor( Color.parseColor("#ffe400") );
            } else if(mUserPresence.getMode().equals("away")) {
                statusString = getString(R.string.status_away);
                mUserStatusIcon.setImageResource(R.drawable.icon_red);
                mUserStatusView.setTextColor(Color.RED);
            } else { // if(mUserPresence.getMode().equals("chat"))
                mUserStatusIcon.setImageResource(R.drawable.icon_green);
                if(!mUserPresence.getStatusMsg().isEmpty()) {
                    statusString = mUserPresence.getStatusMsg();
                } else {
                    statusString = getString(R.string.status_online);
                }
                mUserStatusView.setTextColor( Color.parseColor("#1DB918") );
            }
        } else {
            statusString = getString(R.string.status_offline);
            mUserStatusView.setTextColor( Color.parseColor("#646464") );
            mUserStatusIcon.setImageResource(R.drawable.icon_black);
        }
        mUserStatusView.setText(statusString);
    }

    private boolean isRunningChatService() {
        ActivityManager manager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> rsi = manager.getRunningServices(100);

        for(int i=0; i<rsi.size();i++)
        {
            ActivityManager.RunningServiceInfo rsInfo = rsi.get(i);
            if(rsInfo.service.getClassName().equals("com.rokoroku.lolmessenger.LolMessengerService"))
            {
                Log.i(TAG, "LolMessengerService is running : " + rsInfo.service.getClassName());
                return true;
            }
        }
        Log.e(TAG, "LolMessengerService is not running. ");
        return false;
    }
}
