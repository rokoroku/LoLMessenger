package com.rokoroku.lolmessenger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.achimala.leaguelib.connection.LeagueAccount;
import com.achimala.leaguelib.connection.LeagueConnection;
import com.achimala.leaguelib.connection.LeagueServer;
import com.achimala.leaguelib.errors.LeagueException;
import com.achimala.leaguelib.models.LeagueSummoner;
import com.achimala.leaguelib.models.MatchHistoryEntry;
import com.rokoroku.lolmessenger.classes.ParcelableMatchHistory;
import com.rokoroku.lolmessenger.classes.ParcelableMessage;
import com.rokoroku.lolmessenger.classes.ParcelableRoster;
import com.rokoroku.lolmessenger.utilities.DummySSLSocketFactory;
import com.rokoroku.lolmessenger.utilities.SQLiteDbAdapter;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ping.packet.Ping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Created by Youngrok Kim on 13. 8. 14.
 */
public class LolMessengerService extends Service {

    private static final String TAG = "com.rokoroku.lolmessenger.LolMessengerService";

    // IPC Message ID
    protected static final int MSG_REGISTER_CLIENT = 4624;
    protected static final int MSG_UNREGISTER_CLIENT = 7427;
    protected static final int MSG_PROGRESS_UPDATE = 5626;
    protected static final int MSG_ATTEMPT_LOGIN = 7647;
    protected static final int MSG_CANCEL_LOGIN = 9192;
    protected static final int MSG_UPDATE_PRESENCE  = 3647;
    protected static final int MSG_UPDATE_SETTING  = 3847;
    protected static final int MSG_REQUEST_ROSTER = 4647;
    protected static final int MSG_SEND_MESSAGE = 5647;
    protected static final int MSG_RECEIVE_MSG = 5647;
    protected static final int MSG_OPEN_CHAT = 6647;
    protected static final int MSG_STOP_SERVICE = 6648;
    protected static final int MSG_SERVICE_DEAD = 6698;
    protected static final int MSG_REQUEST_MATCH = 6699;
    protected static final int RECEIVE_MESSAGE = 919191;
    protected static final int LOST_CONNECTION = 919192;

    static final int LOGIN_FAILED = 0;
    static final int LOGIN_FAILED_TIMEOUT = 1;
    static final int LOGIN_FAILED_INTERRUPTED = 2;
    static final int LOGIN_SUCCESS = 3;
    static final int LOGIN_FAILED_SERVER_NOT_RESPONSE = 4;

    // settings
    private boolean mRunningNotification;
    private boolean mMessageNotification;
    private boolean mVibration;
    private boolean mSound;

    // xmppConnection socket (smack api & rtmps api)
    private ConnectionConfiguration config = null;
    private XMPPConnection xmppConnection = null;
    private LeagueConnection leagueConnection = null;
    private LeagueServer leagueServer = null;
    private boolean useRTMPSservice = true;

    // chat manager (smack api)
    private ChatManager mChatmanager = null;
    private Chat mChat = null;

    // User & Roster data (smack api & rtmps api)
    private Roster mRoster = null;
    private String mUserID = null;
    private String mUserAccount = null;
    private ParcelableRoster mUserPresense = null;
    private LeagueSummoner mUserSummoner = null;

    // SQLite db utility
    private SQLiteDbAdapter mSQLiteDbAdapter = new SQLiteDbAdapter(this);

    // Notification manager
    private NotificationManager mNotificationManager = null;

    //private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    private ArrayList<Messenger> mClientMessenger = new ArrayList<Messenger>();
    private ArrayList<String> mChatParticipant = new ArrayList<String>();

    // async tasks
    private XMPPLoginTask mAuthTask = null;
    private RTMPSloginTask mAuthTask2 = null;
    private MatchListTask mMatchListTask = null;
    private LogoutTask mDisconnectTask = null;

    // IPC Messenger
    private Messenger mMessenger = new Messenger(new IncomingHandler());
    private Handler mPacketHandler = null;
    private Thread mReceivingThread = null;

    // Message Handler from Messenger (Other Activities)
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int mValue = 0;
            try {
                switch( msg.what ){

                    //Activity 바인딩 등록
                    case MSG_REGISTER_CLIENT:
                        mClientMessenger.add(msg.replyTo);
                        if(msg.getData().getString("participantID") != null) {
                            mChatParticipant.add(msg.getData().getString("participantID"));
                        }
                        msg.obtain(null, MSG_REGISTER_CLIENT);
                        Log.i(TAG, msg.replyTo + " is registered into service, (size " + mClientMessenger.size() + ")");

                        //ack
                        mClientMessenger.get(mClientMessenger.indexOf(msg.replyTo)).send(msg);
                        break;

                    //Activity 바인딩 해제
                    case MSG_UNREGISTER_CLIENT:
                        mClientMessenger.remove(msg.replyTo);
                        if(msg.getData().getString("participantID") != null) {
                            mChatParticipant.remove(msg.getData().getString("participantID"));
                        }
                        Log.i(TAG, msg.replyTo + " is unregistered from service, (size " + mClientMessenger.size() + ")");
                        break;

                    //로그인 시도
                    case MSG_ATTEMPT_LOGIN:
                        Log.i(TAG, "receive LOGIN attempt from activity, msg : " + msg);

                        //receive data
                        Bundle receivedBundle = msg.getData();
                        String[] mCredentialInput = receivedBundle.getStringArray("credential");
                        mUserAccount = mCredentialInput[0];

                        //server
                        switch(receivedBundle.getInt("server")) {
                            case 0:
                                config = new ConnectionConfiguration("chat.kr.lol.riotgames.com", 5223, "pvp.net");
                                leagueServer = LeagueServer.KOREA;
                                break;
                            case 1:
                                config = new ConnectionConfiguration("chat.na1.lol.riotgames.com", 5223, "pvp.net");
                                leagueServer = LeagueServer.NORTH_AMERICA;
                                break;
                            case 2:
                                config = new ConnectionConfiguration("chat.eu.lol.riotgames.com", 5223, "pvp.net");
                                leagueServer = LeagueServer.EUROPE_WEST;
                                break;
                            case 3:
                                config = new ConnectionConfiguration("chat.eun1.lol.riotgames.com", 5223, "pvp.net");
                                leagueServer = LeagueServer.EUROPE_NORDIC_AND_EAST;
                                break;
                            case 4:
                                config = new ConnectionConfiguration("chat.br.lol.riotgames.com", 5223, "pvp.net");
                                leagueServer = LeagueServer.BRAZIL;
                                break;
                            case 5:
                                config = new ConnectionConfiguration("chat.la1.lol.riotgames.com", 5223, "pvp.net");
                                leagueServer = LeagueServer.LATIN_AMERICA_NORTH;
                                break;
                            case 6:
                                config = new ConnectionConfiguration("chat.la2.lol.riotgames.com", 5223, "pvp.net");
                                leagueServer = LeagueServer.LATIN_AMERICA_SOUTH;
                                break;
                            case 7:
                                config = new ConnectionConfiguration("chat.oc1.lol.riotgames.com", 5223, "pvp.net");
                                leagueServer = LeagueServer.OCEANIA;
                                break;
                        }

                        leagueConnection = new LeagueConnection(leagueServer);
                        xmppConnection = new XMPPConnection(config);

                        //reply
                        msg.obtain(null, MSG_ATTEMPT_LOGIN);
                        msg.arg1 = doLogin(mCredentialInput);
                        if(msg.arg1 == LOGIN_SUCCESS) {
                            receivedBundle = new Bundle();
                            receivedBundle.putString("userID", getUserID());
                            msg.setData(receivedBundle);
                        }
                        mClientMessenger.get(mClientMessenger.indexOf(msg.replyTo)).send(msg);
                        break;

                    case MSG_CANCEL_LOGIN:
                        if(mAuthTask != null) mAuthTask.cancel(true);
                        else if(mAuthTask2 != null) mAuthTask2.cancel(true);
                        break;

                    case MSG_REQUEST_ROSTER:
                        Log.i(TAG, "receive Roster Request from activity, msg : " + msg);
                        msg.obtain(null, MSG_REQUEST_ROSTER);

                        Bundle bundle = new Bundle(ParcelableRoster.class.getClassLoader());
                        bundle.putParcelableArrayList("RosterList", getRosterList());
                        bundle.putStringArrayList("RosterGroupList", getRosterGroupList());
                        bundle.putString("UserID", xmppConnection.getUser());
                        bundle.putString("UserAccount", mUserAccount);
                        bundle.putParcelable("UserPresence", mUserPresense);
                        msg.setData(bundle);

                        Log.i(TAG, "send Roster msg : " + msg);
                        mClientMessenger.get(mClientMessenger.indexOf(msg.replyTo)).send(msg);
                        break;

                    case MSG_REQUEST_MATCH:
                        Log.i(TAG, "receive Match Request from activity, msg : " + msg);
                        msg.obtain(null, MSG_REQUEST_MATCH);

                        String userID;
                        ArrayList<ParcelableMatchHistory> matchHistories = null;
                        ParcelableRoster roster = null;
                        Date FWOTD = null;
                        Bundle matchbundle = null;

                        if(msg.getData().getString("userID") == null) { userID = mUserID; }
                        else { userID = msg.getData().getString("userID"); }
                        try {
                            if(mMatchListTask == null) {
                                mMatchListTask = new MatchListTask();
                                mMatchListTask.execute( userID );
                            }

                            Object[] result = mMatchListTask.get();
                            matchHistories = (ArrayList<ParcelableMatchHistory>) result[0];
                            roster = (ParcelableRoster) result[1];
                            FWOTD = (Date) result[2];

                            matchbundle = new Bundle();
                            matchbundle.putBoolean("error", false);
                            matchbundle.setClassLoader(ParcelableMatchHistory.class.getClassLoader());
                            matchbundle.putParcelableArrayList("matchList", matchHistories);
                            matchbundle.setClassLoader(ParcelableRoster.class.getClassLoader());
                            matchbundle.putParcelable("roster", roster);
                            if(FWOTD == null) { FWOTD = new Date(0); }
                            matchbundle.putLong("FWOTD", FWOTD.getTime());
                            matchbundle.putString("userID", String.valueOf(userID));
                            msg.setData(matchbundle);

                        } catch (Exception e) {
                            e.printStackTrace();
                            matchbundle = new Bundle();
                            matchbundle.putBoolean("error", true);
                            msg.setData(matchbundle);
                        }

                        Log.i(TAG, "send MatchList msg : " + msg);
                        mClientMessenger.get(mClientMessenger.indexOf(msg.replyTo)).send(msg);
                        break;

                        case MSG_OPEN_CHAT:
                            Log.i(TAG, "receive Chat open request, msg : " + msg);
                            //receive data
                            String buddyID = msg.getData().getString("buddyID");
                            //sendMessageToBuddy();

                            //reply
                            //msg.obtain(null, MSG_ATTEMPT_LOGIN);
                            break;

                        case MSG_SEND_MESSAGE:
                            Log.i(TAG, "receive sending message request , msg : " + msg);

                            //receive data
                            msg.getData().setClassLoader(ParcelableMessage.class.getClassLoader());
                            ParcelableMessage message = msg.getData().getParcelable("message");
                            sendMessageToBuddy(message);

                            //reply
                            //msg.obtain(null, MSG_ATTEMPT_LOGIN);
                            break;

                        case MSG_UPDATE_PRESENCE:
                            Log.i(TAG, "receivce update presence request, msg : " + msg);
                            msg.getData().setClassLoader(ParcelableRoster.class.getClassLoader());
                            updatePresence( (ParcelableRoster) msg.getData().getParcelable("userPresence"));
                            break;

                        case MSG_UPDATE_SETTING:
                            Log.i(TAG, "receivce update setting request, msg : " + msg);
                            updateSetting(msg.getData());
                            break;

                        case MSG_STOP_SERVICE:
                            Log.i(TAG, "receive stop message request , msg : " + msg);
                            if(mNotificationManager != null) mNotificationManager.cancelAll();

                            try {
                                new LogoutTask().execute().get();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            stopSelf(); // finish()
                            break;

                            default:
                            super.handleMessage(msg);
                        }

            }
            catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClientMessenger = null;
            }
        }
    }

    // Message Handler from Thread
    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECEIVE_MESSAGE:
                    Log.i(TAG,"Recv message : " + msg.obj);
                    org.jivesoftware.smack.packet.Message receivedMessage = (org.jivesoftware.smack.packet.Message )msg.obj;
                    String fromName = StringUtils.parseBareAddress(receivedMessage.getFrom());
                    System.out.print(fromName + ":");
                    System.out.println(receivedMessage.getBody() );

                    // get and make message object
                    Message newmsg = Message.obtain(null, MSG_RECEIVE_MSG);
                    ParcelableMessage message = new ParcelableMessage(fromName, xmppConnection.getUser(), receivedMessage.getBody(), 1);

                    // add a record into SQLite DB
                    mSQLiteDbAdapter.addRecord(message);

                    // send via Messenger
                    if(mClientMessenger.size() > 0){ sendReceivedMsg(message); }

                    // send via Broadcasting
                    //Intent intent = new Intent("com.example.lolmessenger.NEW_MESSAGE");
                    //Bundle bundle = new Bundle(ParcelableMessage.class.getClassLoader());
                    //bundle.putParcelable("message", message);
                    //intent.putExtra("message", bundle);
                    //sendBroadcast(intent);
                    //Log.i(TAG,"Send Boradcast intent : " + intent);

                    // notify
                    Boolean isParticipant = false;
                    for(String id : mChatParticipant) {
                        if(id.equals(fromName)) isParticipant = true;
                    }
                    if(!isParticipant) {
                        if(mMessageNotification) notifyNewMessage(message);
                    }

                    break;

                case LOST_CONNECTION:
                    Log.d(TAG, "Thread Connection Lost");
                    Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
                    mNotificationManager.cancelAll();

                    Notification notification = new NotificationCompat.Builder(getApplicationContext())
                            .setContentTitle("Lol messenger disconnected")
                            .setContentText("Connection Lost")
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setTicker("Connection Lost")
                            .setOnlyAlertOnce(true)
                            .setAutoCancel(true)
                            .build();

                    mNotificationManager.notify(1113, notification);

                    xmppConnection.disconnect();
                    notifyServiceDead();
                    stopSelf(); // finish()
                    break;

            }
            super.handleMessage(msg);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        SharedPreferences prefs = getSharedPreferences( "settings", Context.MODE_PRIVATE);
        mRunningNotification= prefs.getBoolean("RunningNotification", true);
        mMessageNotification= prefs.getBoolean("MessageNotification", true);
        mVibration          = prefs.getBoolean("Vibration", true);
        mSound              = prefs.getBoolean("Sound", true);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Tell the user we stopped.
        Log.i(TAG, "Destroyed");
        mSQLiteDbAdapter.close();

        if(mClientMessenger != null) {
            notifyServiceDead();
        }

        //mNotificationManager.cancel(1112);
        if( mNotificationManager != null) {
            mNotificationManager.cancelAll();
            mNotificationManager.cancel(1112);
        }
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_SHORT).show();
    }

    // do Login
    public int doLogin(String[] _credential) {

        //login into XMPP server
        mAuthTask = (XMPPLoginTask) new XMPPLoginTask().execute(_credential);
        try {
            if(mAuthTask.get(15, TimeUnit.SECONDS) != true) {
                mAuthTask.cancel(true);
                return LOGIN_FAILED;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();//canceled
            return LOGIN_FAILED_INTERRUPTED;
        } catch (ExecutionException e) {
            e.printStackTrace();//login fail
            return LOGIN_FAILED;
        } catch (TimeoutException e) {
            e.printStackTrace();//timeout
            return LOGIN_FAILED_TIMEOUT;
        }

        //login into RTMPS server
        mAuthTask2 = (RTMPSloginTask) new RTMPSloginTask().execute(_credential);
        try {
            if(mAuthTask2.get() != true) { return LOGIN_FAILED_SERVER_NOT_RESPONSE; }
        } catch (InterruptedException e) {
            e.printStackTrace();//canceled
            return LOGIN_FAILED_INTERRUPTED;
        } catch (ExecutionException e) {
            e.printStackTrace();//login fail
            return LOGIN_FAILED;
        }

        //login success
        mChatmanager = xmppConnection.getChatManager();
        mRoster = xmppConnection.getRoster();
        mRoster.addRosterListener(new RosterListener() {
            @Override
            public void entriesAdded(Collection<String> strings) {
                Log.i(TAG,"Roster listener: entries added");
                sendUpdatedRosterToClient();
            }

            @Override
            public void entriesUpdated(Collection<String> strings) {
                Log.i(TAG,"Roster listener: entries updated");
                sendUpdatedRosterToClient();
            }

            @Override
            public void entriesDeleted(Collection<String> strings) {
                Log.i(TAG,"Roster listener: entries deleted");
                sendUpdatedRosterToClient();
            }

            @Override
            public void presenceChanged(Presence presence) {
                Log.i(TAG,"Roster listener: presence Changed");
                sendUpdatedRosterToClient();
            }
        });

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(mRunningNotification) notifyRunning();

        runMessageHandler();

        xmppConnection.addConnectionListener(new ConnectionListener() {
            @Override
            public void connectionClosed() {

            }

            @Override
            public void connectionClosedOnError(Exception e) {

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentIntent(pendingIntent)
                        .setContentTitle("Lost Connection")
                        .setContentText("Lost connection on error")
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setTicker("Lost connection on error")
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .build();
                mNotificationManager.notify(1113, notification);
            }

            @Override
            public void reconnectingIn(int i) {
                Log.i(TAG, "reconnecting... " + i);
                Notification notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Lol messenger disconnected")
                        .setContentText("Reconnecting...(" + i + ")")
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setTicker("Reconnecting...")
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .build();
                mNotificationManager.notify(1113, notification);
            }

            @Override
            public void reconnectionSuccessful() {
                Log.i(TAG, "reconnecting success ");
                mNotificationManager.cancel(1113);
            }

            @Override
            public void reconnectionFailed(Exception e) {
                Log.i(TAG, "reconnecting failed");
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("Lol messenger disconnected")
                        .setContentText("Reconnection Failed")
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setTicker("Reconnection Failed")
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .build();
                mNotificationManager.notify(1113, notification);
            }
        });

        return LOGIN_SUCCESS;
    }

    public void sendUpdatedRosterToClient() {
        mRoster = xmppConnection.getRoster();
        Message msg = Message.obtain(null, MSG_REQUEST_ROSTER);

        Bundle bundle = new Bundle(ParcelableRoster.class.getClassLoader());
        bundle.putParcelableArrayList("RosterList", getRosterList());
        bundle.putStringArrayList("RosterGroupList", getRosterGroupList());
        bundle.putString("UserID", xmppConnection.getUser());
        bundle.putString("UserAccount", mUserAccount);
        bundle.putParcelable("UserPresence", mUserPresense);
        msg.setData(bundle);

        try {
            if(mClientMessenger.size() > 0) mClientMessenger.get(0).send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // create chat and send a message
    public boolean sendMessageToBuddy(ParcelableMessage message) {
        String buddyID = message.getToID();
        String messageBody = message.getBody();
        Log.i(TAG," to " + buddyID + "/ " + messageBody);
        try {
            Chat chat = mChatmanager.createChat(buddyID, new MessageListener() {
                @Override
                public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
                    Log.i(TAG, "message listened from opened chat room: " + message.getBody());
                }
            });
            chat.sendMessage(messageBody);
        } catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void updatePresence(ParcelableRoster userPresense) {
        //save preferences
        SharedPreferences prefs = getSharedPreferences( "presence_" + mUserAccount, MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("StatusMsg", userPresense.getStatusMsg());
        ed.commit();

        //send presence packet
        Presence presence;
        mUserPresense = userPresense;
        if(mUserPresense.getAvailablity().equals("unavailable")) { presence = new Presence(Presence.Type.unavailable); }
        else {
            presence = new Presence(Presence.Type.available);
            if(mUserPresense.getMode().equals("chat")) { presence.setMode(Presence.Mode.chat); }
            else if(mUserPresense.getMode().equals("away")) { presence.setMode(Presence.Mode.away); }
            presence.setStatus(mUserPresense.getPresenseStatus());
        }

        Log.i(TAG, "update presence packet:" + presence.toString());
        xmppConnection.sendPacket(presence);
    }

    public void updateSetting(Bundle bundle) {

        if(!mRunningNotification && bundle.getBoolean("RunningNotification")) {
            notifyRunning();
        }

        mRunningNotification= bundle.getBoolean("RunningNotification");
        mMessageNotification= bundle.getBoolean("MessageNotification");
        mVibration          = bundle.getBoolean("Vibration");
        mSound              = bundle.getBoolean("Sound");

        Log.i(TAG, "setting flag : " + mRunningNotification + mMessageNotification + mVibration + mSound);

        if(!mRunningNotification) mNotificationManager.cancel(1112);

    }

    // initiate incomming message handler thread
    public void runMessageHandler() {

        //create handler
        if(mPacketHandler != null) return;

        mPacketHandler = new MessageHandler();

        //create thread
        mReceivingThread = new Thread() {
            @Override
            public void run() {
                Log.i("lolmessenger.listenerThread", "Message Listener Running " );
                // Listen for incoming messages from users not already in conversation
                PacketFilter filter = new MessageTypeFilter(org.jivesoftware.smack.packet.Message.Type.chat);
                xmppConnection.addPacketListener(new PacketListener() {
                    public void processPacket(Packet packet) {
                        org.jivesoftware.smack.packet.Message message = (org.jivesoftware.smack.packet.Message) packet;
                        if (message.getBody() != null) {
                            mPacketHandler.sendMessage(mPacketHandler.obtainMessage(RECEIVE_MESSAGE, message));
                            Log.i("lolmessenger.listenerThread", "Message received : " + message);
                        }
                    }
                }, filter);

                while(true) {
                    try {
                        //연결 검사
                        sleep(3000);
                        xmppConnection.sendPacket(new Ping());
                        if(xmppConnection.isSocketClosed() || !xmppConnection.isConnected() || !leagueConnection.getAccountQueue().isAnyAccountConnected() ) { // xmppConnection.getRoster().getEntryCount()==0
                            mPacketHandler.sendMessage(mPacketHandler.obtainMessage(LOST_CONNECTION, null));
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "THREAD EXCEPTION");
                        mPacketHandler.sendMessage(mPacketHandler.obtainMessage(LOST_CONNECTION, null));
                        break;
                    }
                }
            }

        };//end of thread definition

        mReceivingThread.setDaemon(true);
        mReceivingThread.start();
    }

    public void sendReceivedMsg(ParcelableMessage message) {
        Message msg = Message.obtain(null, MSG_RECEIVE_MSG);

        Bundle bundle = new Bundle(ParcelableMessage.class.getClassLoader());
        bundle.putParcelable("message", message);
        msg.setData(bundle);

        try {
            Log.i(TAG,"Send message to Activity : " + msg);
            for(Messenger msgr : mClientMessenger) {
                msgr.send(msg);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    // Returns a collection of the users buddy list entries
    public ArrayList<ParcelableRoster> getRosterList() {
        ArrayList<ParcelableRoster> rosterList = new ArrayList<ParcelableRoster>();
        mRoster = xmppConnection.getRoster();
        for(RosterEntry entry : mRoster.getEntries()) {
            ParcelableRoster tempRoster = new ParcelableRoster(entry, mRoster.getPresence(entry.getUser()));
            rosterList.add(tempRoster);
        }
        return rosterList;
    }

    public ArrayList<String> getRosterGroupList() {
        ArrayList<String> rosterGroupList = new ArrayList<String>();
        for(RosterGroup entry : mRoster.getGroups()) {
            rosterGroupList.add(entry.getName());
        }
        rosterGroupList.add("Offline");
        return rosterGroupList;
    }

    public String getUserID() {
        return mUserID;
    }

    public String getUserAccount() {
        return mUserAccount;
    }

    // RTMPS connection methods
    public void fillPublicUserPresence(ParcelableRoster userPresense, String userID) {
        try {
            String name = leagueConnection.getSummonerService().getSummonerName(Integer.parseInt(userID));
            LeagueSummoner summoner = leagueConnection.getSummonerService().getSummonerByName(name);

            userPresense.setUserName(name);
            userPresense.setSummonerLevel(summoner.getLevel());
            userPresense.setProfileIcon(summoner.getProfileIconId());

        } catch (LeagueException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ParcelableMatchHistory> getMatchHistory(String sumonner_name) {
        ArrayList<ParcelableMatchHistory> matchList = new ArrayList<ParcelableMatchHistory>();
        try {
            LeagueSummoner summoner = leagueConnection.getSummonerService().getSummonerByName(sumonner_name);
            leagueConnection.getPlayerStatsService().fillMatchHistory(summoner);
            for(MatchHistoryEntry entry : summoner.getMatchHistory()) {
                ParcelableMatchHistory temp = new ParcelableMatchHistory(entry, summoner);
                matchList.add(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return matchList;
    }

    public Date getFirstWInOfTheDayDate(LeagueSummoner summoner) {
        Date FWOTD = null;

        if(summoner.getMatchHistory() == null) try {
            leagueConnection.getPlayerStatsService().fillMatchHistory(summoner);
        } catch (LeagueException e) {
            e.printStackTrace();
        }

        for(MatchHistoryEntry entry : summoner.getMatchHistory()) {
            if(entry.getIpEarned() > 170 ) {
                if(FWOTD == null) FWOTD = entry.getCreationDate();
                else if(entry.getCreationDate().getTime() > FWOTD.getTime()) FWOTD = entry.getCreationDate();
            }
        }

        if(FWOTD != null) FWOTD = new Date( FWOTD.getTime() + (long)1000*60*60*22 );

        return FWOTD;
    }

    public void notifyNewMessage(ParcelableMessage message) {

        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra("userID", xmppConnection.getUser());
        intent.putExtra("buddyID", message.getFromID());
        ParcelableRoster buddy = new ParcelableRoster(
                mRoster.getEntry(message.getFromID()), mRoster.getPresence(message.getFromID()));
        intent.putExtra("buddy", buddy);
        intent.setExtrasClassLoader(ParcelableRoster.class.getClassLoader());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(mRoster.getEntry(message.getFromID()).getName())
                .setContentText(message.getBody())
                .setContentInfo(getString(R.string.app_name))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(mRoster.getEntry(message.getFromID()).getName() + " : " + message.getBody())
                //.setOnlyAlertOnce(true)
                //.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)   //.setDefaults(Notification.DEFAULT_SOUND)
                //.setDefaults(Notification.DEFAULT_VIBRATE)            //.setVibrate(Long [] vibpattern)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setAutoCancel(true)
                .build();

        if(mSound) { notification.defaults = notification.defaults | Notification.DEFAULT_SOUND; }
        if(mVibration) { notification.defaults = notification.defaults | Notification.DEFAULT_VIBRATE; }

        mNotificationManager.notify(1111, notification);
    }

    public void notifyRunning() {

        Intent intent = new Intent(getApplicationContext(), ClientActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Lol messenger is running")
                .setContentText("Logged in as " + mUserAccount)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker("Lol messenger is running")
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .build();

        //notification.flags = Notification.FLAG_NO_CLEAR; == ongoing notification

        mNotificationManager.notify(1112, notification);
    }

    public void notifyServiceDead() {
        Message msg = Message.obtain(null, MSG_SERVICE_DEAD);
        try {
            for(Messenger msgr : mClientMessenger) {
                msgr.send(msg);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //Login Asynchronous Task
    public class XMPPLoginTask extends AsyncTask<String, Integer, Boolean> {

        private int mProgress;

        @Override
        protected Boolean doInBackground(String... params) {

            // old SSL authentication method
            config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
            config.setSocketFactory(new DummySSLSocketFactory());

            //attempt to contact the XMPP server
            try {
                Message msg = Message.obtain(null, MSG_PROGRESS_UPDATE);
                Bundle bundle = new Bundle();
                bundle.putString("Progress", "Connecting...");
                msg.setData(bundle);
                mClientMessenger.get(0).send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                xmppConnection.connect();
                Log.i(TAG, "Connected to " + xmppConnection.getHost());
            } catch (XMPPException e) {
                Log.e(TAG, "Failed to connect to " + xmppConnection.getHost());
                return false;
            }

            //attempt to login with given certification
            try {
                Message msg = Message.obtain(null, MSG_PROGRESS_UPDATE);
                Bundle bundle = new Bundle();
                bundle.putString("Progress", "Authenticating...");
                msg.setData(bundle);
                mClientMessenger.get(0).send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            String loginAccount = params[0];
            String password = params[1];
            try {
                xmppConnection.login(loginAccount, "AIR_" + password);
                mUserID = xmppConnection.getUser();
                Log.i(TAG, "Authenticated as " + loginAccount + " / " + mUserID);
            } catch (XMPPException e) {
                Log.e(TAG, "Failed to login as " + loginAccount);
                return false;
            }

            //attempt to login Lol AIR RMTPS server
            Log.i(TAG, "Connected to XMPP server");
            return true;
        }

        protected void onProgressUpdate(Integer[] values) {
            mProgress = values[0];
            switch(mProgress) {
                case -1:
                    //Toast.makeText(getApplicationContext(), "통신 오류", 0).show();
                    break;
                case 1:

                    break;
                case 2:
                    try {
                        Message msg = Message.obtain(null, MSG_PROGRESS_UPDATE);
                        Bundle bundle = new Bundle();
                        bundle.putString("Progress", "Authenticating...");
                        msg.setData(bundle);
                        mClientMessenger.get(0).send(msg);

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            if (success) {
                //success
            } else {
                //fail
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }

    public class RTMPSloginTask extends AsyncTask<String, Integer, Boolean> {

        private int mProgress;

        @Override
        protected Boolean doInBackground(String... params) {

            //attempt to login Lol AIR RMTPS server
            String loginAccount = params[0];
            String password = params[1];

            publishProgress(1);
            try {

                try {
                    //Toast.makeText(getApplicationContext(), "서버에 접속 중", 0).show();
                    Message msg = Message.obtain(null, MSG_PROGRESS_UPDATE);
                    Bundle bundle = new Bundle();
                    bundle.putString("Progress", "In login queue. Wait a minute...");
                    msg.setData(bundle);
                    mClientMessenger.get(0).send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                Log.i(TAG, "leagueserver" + leagueServer);
                leagueConnection.getAccountQueue().addAccount(new LeagueAccount(leagueServer, "3.10.xx", loginAccount, password));
                Map<LeagueAccount, LeagueException> exceptions = leagueConnection.getAccountQueue().connectAll();
                if(exceptions != null) {
                    for(LeagueAccount account : exceptions.keySet())
                        System.out.println(account + " error: " + exceptions.get(account));
                    return false;
                }

                //load presence
                SharedPreferences prefs = getSharedPreferences("presence_" + mUserAccount, MODE_PRIVATE);
                LeagueSummoner summoner = leagueConnection.getSummonerService().getSummonerByName(
                        leagueConnection.getSummonerService().getSummonerName(Integer.parseInt(mUserID.substring(3, mUserID.indexOf("@")))));

                mUserPresense = new ParcelableRoster(summoner);
                mUserPresense.setUserID(mUserID);
                mUserPresense.setStatusMsg( prefs.getString("StatusMsg", "Using Android Client") );
                updatePresence(mUserPresense);

            } catch (LeagueException e) {
                e.printStackTrace();
                return false;
            }
            //Send the packet (don't need it)
            //Presence presence = new Presence(Presence.Type.available);
            //xmppConnection.sendPacket(presence);

            //save
            //user = new ParcelableRoster(xmppConnection.getUser(), presence);
            Log.i(TAG, "Connected");

            return true;
        }

        protected void onProgressUpdate(Integer[] values) {
            mProgress = values[0];
            switch(mProgress) {
                case -1:
                    //Toast.makeText(getApplicationContext(), "통신 오류", 0).show();
                    break;
                case 1:

                    break;
                case 2:
                    try {
                        //Accepted
                        Message msg = Message.obtain(null, MSG_PROGRESS_UPDATE);
                        Bundle bundle = new Bundle();
                        bundle.putString("Progress", "Login success");
                        msg.setData(bundle);
                        mClientMessenger.get(0).send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            if (success) {
                //success
            } else {
                //fail
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }

    public class LogoutTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            xmppConnection.disconnect();
            Log.i(TAG, "Disconnected");
            return true;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mDisconnectTask = null;
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG,"authtask cancelled");
            mDisconnectTask = null;
        }

    }

    public class MatchListTask extends AsyncTask<String, Void, Object[]> {

        @Override
        protected Object[] doInBackground(String... params) {

            String summoner_name = null;
            LeagueSummoner summoner = null;
            String inputID = params[0];
            try {
                int userid = Integer.parseInt(inputID.substring(3, inputID.indexOf("@")));
                summoner_name = leagueConnection.getSummonerService().getSummonerName(userid);
                summoner = leagueConnection.getSummonerService().getSummonerByName(summoner_name);
                leagueConnection.getLeaguesService().fillSoloQueueLeagueData(summoner);
                leagueConnection.getPlayerStatsService().fillMatchHistory(summoner);
                ArrayList<ParcelableMatchHistory> matchHistories = getMatchHistory(summoner_name);
                Date FWOTD = getFirstWInOfTheDayDate(summoner);

                ParcelableRoster presence = new ParcelableRoster(summoner);
                presence.setUserID(inputID);

                Object[] result = new Object[]{ matchHistories, presence, FWOTD };
                return result;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Object[] success) {
            mMatchListTask = null;
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG,"authtask cancelled");
            mMatchListTask = null;
        }

    }

}

