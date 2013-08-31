package com.rokoroku.lolmessenger;

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
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rokoroku.lolmessenger.classes.ParcelableMessage;
import com.rokoroku.lolmessenger.classes.ParcelableRoster;
import com.rokoroku.lolmessenger.fragments.ConfirmationDialog;
import com.rokoroku.lolmessenger.utilities.MessageViewAdapter;
import com.rokoroku.lolmessenger.utilities.SQLiteDbAdapter;

import java.util.ArrayList;

/**
 * Created by Youngrok Kim on 13. 8. 21.
 */
public class ChatActivity extends FragmentActivity {

    protected static final String TAG = "com.rokoroku.lolmessenger.ChatActivity";

    private String mUserID;
    private String mBuddyID;
    private ParcelableRoster mBuddy;
    private ArrayList<ParcelableMessage> MessageLIst;

    private EditText mMessageInputView;
    private ListView mMessageListView;
    private Button mSendButton;
    private ImageButton mDeleteButton;

    private SQLiteDbAdapter mSQLiteDbAdapter = new SQLiteDbAdapter(this);

    // Chat Connection Object
    private Messenger mServiceMessenger = null;
    private Messenger mClientMessenger = new Messenger( new IncomingHandler() );

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mServiceMessenger = new Messenger(service);
            try{
                Log.i(TAG, "sending registering message to service");
                Message msg = Message.obtain(null, LolMessengerService.MSG_REGISTER_CLIENT);
                Bundle bundle = new Bundle();
                bundle.putString("participantID", mBuddyID);
                msg.setData(bundle);
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
            // Message msg = Message.obtain(null, LolMessengerService.MSG_UNREGISTER_CLIENT);
            // unbindService(mConnection);
            mServiceMessenger = null;
        }
    };

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage( Message msg ){
            switch ( msg.what ){
                case LolMessengerService.MSG_REGISTER_CLIENT:
                    Log.i(TAG, "Connected with service");
                    break;

                case LolMessengerService.MSG_RECEIVE_MSG:
                    Log.i(TAG, "Get new message from service");
                    //msg.getData().setClassLoader(ParcelableMessage.class.getClassLoader());
                    //ParcelableMessage newMessage = msg.getData().getParcelable("message");
                    refreshMessageList();
                    break;

                case LolMessengerService.MSG_SERVICE_DEAD:
                    Log.i(TAG, "service is dead, close this chat");
                    finish();
                    break;

                default:
                    super.handleMessage( msg );
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Show the Up button in the action bar.
        // setupActionBar();
        getActionBar().hide();
        findViewById(R.id.user_indi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        TextView textName = (TextView) findViewById(R.id.text_name);
        TextView textStatus = (TextView) findViewById(R.id.text_status);
        ImageView imageIcon = (ImageView) findViewById(R.id.status_icon);

        mMessageInputView = (EditText) findViewById(R.id.input_message_form);
        mMessageListView = (ListView) findViewById(R.id.message_list);

        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        mDeleteButton = (ImageButton) findViewById(R.id.button_delete);
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteChat();
            }
        });

        Intent intent = getIntent();
        intent.setExtrasClassLoader(ParcelableRoster.class.getClassLoader());
        mUserID = intent.getStringExtra("userID");
        mBuddyID = intent.getStringExtra("buddyID");
        mBuddy = intent.getParcelableExtra("buddy");

        //set View
        textName.setText(mBuddy.getUserName());
        String statusString;
        if(mBuddy.getAvailablity().equals("available")) {
            if(mBuddy.getMode().equals("dnd")) {
                if(mBuddy.getGameStatus().equals("inGame")) {
                    statusString = new String("In Game : " + mBuddy.getSkinname() + " (" + ((System.currentTimeMillis() - mBuddy.getTimeStamp())/60000) + "min)");
                }
                else if(mBuddy.getGameStatus().equals("teamSelect")) {
                    statusString = "Selecting Team";
                }
                else if(mBuddy.getGameStatus().equals("hostingPracticeGame")) {
                    statusString = "Hosting Practice Game";
                }
                else {
                    statusString = "Finding Game";
                }
                imageIcon.setImageResource(R.drawable.icon_yellow);
                textStatus.setTextColor( Color.parseColor("#ffe400") );
            } else if(mBuddy.getMode().equals("away")) {
                statusString = "Away";
                imageIcon.setImageResource(R.drawable.icon_red);
                textStatus.setTextColor( Color.RED );
            } else { // if(mBuddy.getMode().equals("chat")) .. 안드로이드 메신저 쓰는사람은 이 값이 없음.
                imageIcon.setImageResource(R.drawable.icon_green);

                if(!mBuddy.getStatusMsg().isEmpty()) {
                    statusString = mBuddy.getStatusMsg();
                } else {
                    statusString = "Online";
                }
                textStatus.setTextColor( Color.parseColor("#1DB918") );
            }
        } else {
            statusString = "Offline";
            textStatus.setTextColor( Color.parseColor("#646464") );
            imageIcon.setImageResource(R.drawable.icon_black);
        }
        textStatus.setText(statusString);

        Intent connectIntent = new Intent(this, LolMessengerService.class);
        bindService(connectIntent, mConnection, 0);

        //if flag is BIND_AUTO_CREATE, service won't be stop until unbinded.

    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
    }

    @Override
    protected void onDestroy() {
        //unregister
        Message msg = Message.obtain(null, LolMessengerService.MSG_UNREGISTER_CLIENT);
        msg.replyTo = mClientMessenger;
        Bundle bundle = new Bundle();
        bundle.putString("participantID", mBuddyID);
        msg.setData(bundle);
        try {
            mServiceMessenger.send(msg);
            unbindService(mConnection);
        } catch (Exception e) {
            Log.e(TAG, "service already destroyed");
            e.printStackTrace();
        }

        //close SQLite
        mSQLiteDbAdapter.close();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMessageList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendMessage() {

        String message = mMessageInputView.getText().toString();
        if(message.isEmpty()) return;
        mMessageInputView.setText("");

        ParcelableMessage newMessage = new ParcelableMessage(mUserID, mBuddyID, message, 0);

        Message msg = Message.obtain(null, LolMessengerService.MSG_SEND_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putParcelable("message", newMessage);
        bundle.setClassLoader(ParcelableMessage.class.getClassLoader());
        msg.setData(bundle);
        msg.replyTo = mClientMessenger;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        mSQLiteDbAdapter.addRecord(newMessage);
        refreshMessageList();

    }

    public void refreshMessageList() {
        mSQLiteDbAdapter.updateRecordsAsRead(mUserID, mBuddyID);
        MessageLIst = (ArrayList<ParcelableMessage>) mSQLiteDbAdapter.getMessages(mUserID, mBuddyID);
        MessageViewAdapter newAdapter = new MessageViewAdapter( MessageLIst );
        newAdapter.setInflater((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE), this);
        mMessageListView.setAdapter(newAdapter);
        mMessageListView.setOnItemClickListener(null);
        mMessageListView.setSelection(newAdapter.getCount());
    }

    public String getUserID() {
        return mUserID;
    }

    public void deleteChat() {
        ConfirmationDialog dialog = new ConfirmationDialog();
        dialog.setDialogTitle("Delete Chat");
        dialog.setDialogContent("Are you sure? \nDeleted message cannot be restored");
        dialog.setPositiveOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSQLiteDbAdapter.deleteRecords(mUserID, mBuddyID);
                finish();
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
}
