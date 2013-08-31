package com.rokoroku.lolmessenger;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientActivity extends FragmentActivity {

    protected static final String TAG = "com.rokoroku.lolmessenger.ClientActivity";
    private Boolean isReady = false;
    private Boolean isRestart = false;

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
    private Map<String, ParcelableRoster> mRosterMap = null;
    private ArrayList<String> mRosterGroupList = null;
    private String mUserID = null;
    private String mUserAccount = null;
    private ParcelableRoster mUserPresence = null;
    private ParcelableRoster mUserPresence2 = null;
    private ArrayList<ParcelableMatchHistory>  mMatchHistory = null;
    private Date mFWOTD = null;

    // Messages managing
    private SQLiteDbAdapter mSQLiteDbAdapter;

    // Chat Connection Object
    private Messenger mServiceMessenger = null;
    private Messenger mClientMessenger = new Messenger( new IncomingHandler() );

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

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage( Message msg ){
            Bundle bundle = null;
            switch ( msg.what ){
                case LolMessengerService.MSG_REGISTER_CLIENT:
                    Log.i(TAG, "Connected with service");
                    refreshRoster();
                    break;

                case LolMessengerService.MSG_REQUEST_ROSTER:
                    Log.i(TAG, "Get Roster Entries from service");
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

                    if(!isReady) {
                        isReady = true;
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

                        if(!isReady || roster.getUserID().equals(mUserID)) {
                            mUserPresence2 = roster;
                            mMatchHistory = newMatchHistory;
                            mFWOTD = fwotd;
                            if(isReady) refreshView();
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
                            Toast.makeText(getApplicationContext(), "Failed to retrieve summoner data", Toast.LENGTH_SHORT).show();
                            mSummonerInformationDialog = null;
                        }
                    }

                    break;

                case LolMessengerService.MSG_RECEIVE_MSG:
                    Log.i(TAG, "Get new message from service");
                    //msg.getData().setClassLoader(ParcelableMessage.class.getClassLoader());
                    //ParcelableMessage newMessage = msg.getData().getParcelable("message");
                    if(isReady) refreshView();
                    break;

                case LolMessengerService.MSG_SERVICE_DEAD:
                    Log.i(TAG, "service is dead, stop and return to mainActivity");
                    isRestart = true;
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
            this.mList = new ArrayList<String>();
            mList.add("Chat");
            mList.add("Away");
            mList.add("Offline");
            mList.add("Setting");
            mList.add("Log Out");
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

        getActionBar().hide();

        if(!isRunningChatService()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        //set content view
        setContentView(R.layout.activity_client_new);

        //bind service
        Intent connectIntent = new Intent(ClientActivity.this, LolMessengerService.class);
        bindService(connectIntent, mConnection, Context.BIND_AUTO_CREATE);

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
                    case 3:
                        if(mUserPresence.getAvailablity().equals("unavailable")) mSpinner.setSelection(2);
                        else if(mUserPresence.getMode().equals("chat")) mSpinner.setSelection(0);
                        else if(mUserPresence.getMode().equals("away")) mSpinner.setSelection(1);
                        openSettingDialog();
                        return;
                    case 4:
                        if(mUserPresence.getAvailablity().equals("unavailable")) mSpinner.setSelection(2);
                        else if(mUserPresence.getMode().equals("chat")) mSpinner.setSelection(0);
                        else if(mUserPresence.getMode().equals("away")) mSpinner.setSelection(1);
                        doLogout();
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
            }

            @Override
            public void onPageSelected(int i) {
                switch(i) {
                    case 0:
                        mBuddyTabButton.setAlpha((float) 1);
                        mBuddyTabButton.setBackgroundResource(R.drawable.tab_underbar);
                        mChatTabButton.setAlpha((float) 0.3);
                        mChatTabButton.setBackground(null);
                        mSettingTabButton.setAlpha((float) 0.3);
                        mSettingTabButton.setBackground(null);
                        break;
                    case 1:
                        mBuddyTabButton.setAlpha((float)0.3);
                        mBuddyTabButton.setBackground(null);
                        mChatTabButton.setAlpha((float)1);
                        mChatTabButton.setBackgroundResource(R.drawable.tab_underbar);
                        mSettingTabButton.setAlpha((float)0.3);
                        mSettingTabButton.setBackground(null);
                        break;
                    case 2:
                        mBuddyTabButton.setAlpha((float)0.3);
                        mBuddyTabButton.setBackground(null);
                        mChatTabButton.setAlpha((float) 0.3);
                        mChatTabButton.setBackground(null);
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

            Message msg = Message.obtain(null, LolMessengerService.MSG_UNREGISTER_CLIENT);
            msg.replyTo = mClientMessenger;
            try {
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            unbindService(mConnection);
        }
        Log.i(TAG," Destroyed");

        if(isRestart == true) {
            Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            mPager.setCurrentItem( 0 );
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
        if(isReady) outState.putInt("PAGER", mPager.getCurrentItem());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(isReady) mPager.setCurrentItem(savedInstanceState.getInt("PAGER"));
    }

    @Override
    protected void onResume() {
        if(!isRunningChatService()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        if(isReady) refreshView();
        super.onResume();
    }

    public void refreshRoster() {

        //send roster request message to service
        Message msg = Message.obtain(null, LolMessengerService.MSG_REQUEST_ROSTER);
        msg.replyTo = mClientMessenger;
        try {
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

        Log.i(TAG, "request Match history message");
        Message msg = Message.obtain(null, LolMessengerService.MSG_REQUEST_MATCH);
        msg.replyTo = mClientMessenger;
        Bundle bundle = new Bundle();
        bundle.putString("userID", userID);
        msg.setData(bundle);
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }

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
        SettingFragment dialog = new SettingFragment(ClientActivity.this, mUserAccount, mUserPresence);
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
    }

    public void updatePresence(ParcelableRoster userPresence) {
        mUserPresence = userPresence;

        Message msg = Message.obtain(null, LolMessengerService.MSG_UPDATE_PRESENCE);
        msg.replyTo = mClientMessenger;

        Bundle bundle = new Bundle();
        bundle.setClassLoader(ParcelableRoster.class.getClassLoader());
        bundle.putParcelable("userPresence", userPresence);

        msg.setData(bundle);
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateSetting(Bundle bundle) {
        Message msg = Message.obtain(null, LolMessengerService.MSG_UPDATE_SETTING);
        msg.replyTo = mClientMessenger;
        msg.setData(bundle);
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void doLogout() {
        ConfirmationDialog dialog = new ConfirmationDialog();
        dialog.setDialogTitle("Log Out");
        dialog.setDialogContent("Are you sure you want to log out?");
        dialog.setPositiveOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message msg = Message.obtain(null, LolMessengerService.MSG_STOP_SERVICE);
                msg.replyTo = mClientMessenger;
                msg.arg1 = 1; //restart application
                try {
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                //unbindService(mConnection); this will be called in onDestroy()
                isRestart = true;
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
        return mSQLiteDbAdapter;
    }

    public Boolean isReady() {
        return isReady;
    }

    public ArrayList<String> getRosterGroupList() {
        return mRosterGroupList;
    }

    public void refreshView() {

        int count=0 ;
        for(ParcelableRoster roster : mRosterMap.values()) {
            if(roster.getAvailablity().equals("available")) count++;
        }

        mBuddyCount.setText(String.valueOf(count));
        mChatCount.setText(String.valueOf(mSQLiteDbAdapter.getNotReadMessageCount(mUserID)));

        Fragment currentFragment = ((ScreenSlidePagerAdapter)mPagerAdapter).getFragment(mPager.getCurrentItem());
        if(currentFragment != null ) {
            switch(mPager.getCurrentItem()) {
                case 0:
                    ((BuddyListFragment) currentFragment ).refreshBuddyList();
                    break;
                case 1:
                    ((ChatListFragment) currentFragment ).refreshChatList();
                    break;
                case 2:
                    ((SummonerInformationDialogFragment) currentFragment ).setRoster(mUserPresence2);
                    ((SummonerInformationDialogFragment) currentFragment ).setFWOTD(mFWOTD);
                    ((SummonerInformationDialogFragment) currentFragment ).setMatchHistory(mMatchHistory);
                    ((SummonerInformationDialogFragment) currentFragment ).refreshSummoner();
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
                    statusString = "In Game : " + mUserPresence.getSkinname() + " (" + ((System.currentTimeMillis() - mUserPresence.getTimeStamp())/60000) + "min)";
                }
                else if(mUserPresence.getGameStatus().equals("teamSelect")) {
                    statusString = "Selecting Team";
                }
                else if(mUserPresence.getGameStatus().equals("hostingPracticeGame")) {
                    statusString = "Hosting Practice Game";
                }
                else {
                    statusString = "Finding Game";
                }
                mUserStatusIcon.setImageResource(R.drawable.icon_yellow);
                mUserStatusView.setTextColor( Color.parseColor("#ffe400") );
            } else if(mUserPresence.getMode().equals("away")) {
                Log.i(TAG, "away");
                statusString = "Away";
                mUserStatusIcon.setImageResource(R.drawable.icon_red);
                mUserStatusView.setTextColor(Color.RED);
            } else { // if(mUserPresence.getMode().equals("chat"))
                Log.i(TAG, "chat");
                mUserStatusIcon.setImageResource(R.drawable.icon_green);

                if(!mUserPresence.getStatusMsg().isEmpty()) {
                    statusString = mUserPresence.getStatusMsg();
                } else {
                    statusString = "Online";
                }
                mUserStatusView.setTextColor( Color.parseColor("#1DB918") );
            }
        } else {
            Log.i(TAG, "off");
            statusString = "Offline";
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
        return false;
    }
}
