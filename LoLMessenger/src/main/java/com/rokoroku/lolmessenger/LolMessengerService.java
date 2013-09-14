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
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.achimala.leaguelib.connection.LeagueAccount;
import com.achimala.leaguelib.connection.LeagueConnection;
import com.achimala.leaguelib.connection.LeagueServer;
import com.achimala.leaguelib.errors.LeagueException;
import com.achimala.leaguelib.models.LeagueSummoner;
import com.achimala.leaguelib.models.MatchHistoryEntry;
import com.rokoroku.lolmessenger.utilities.CallbackToMessenger;
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
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Youngrok Kim on 13. 8. 14.
 */
public class LolMessengerService extends Service {

    private static final String TAG = "com.rokoroku.lolmessenger.LolMessengerService";

    // IPC Message ID
    protected static final int MSG_REGISTER_CLIENT = 4624;
    protected static final int MSG_UNREGISTER_CLIENT = 7427;
    protected static final int MSG_LOGIN_PROGRESS_UPDATE = 5626;
    protected static final int MSG_ATTEMPT_LOGIN = 7647;
    protected static final int MSG_CANCEL_LOGIN = 9192;
    protected static final int MSG_UPDATE_PRESENCE  = 3647;
    protected static final int MSG_UPDATE_SETTING  = 3847;
    protected static final int MSG_REQUEST_ROSTER = 4647;
    protected static final int MSG_REQUEST_MATCH = 6699;
    protected static final int MSG_CHAT_OPEN = 6647;
    protected static final int MSG_CHAT_SEND_MESSAGE = 5647;
    protected static final int MSG_CHAT_RECEIVE_MESSAGE = 5647;
    protected static final int MSG_SUBSCRIBE_ROSTER = 6700;
    protected static final int MSG_STOP_SERVICE = 6648;
    protected static final int MSG_SERVICE_DEAD = 6698;
    protected static final int MSG_REQUEST_SUMMONER_NAME = 5353;

    // Listener Thread Message ID
    private static final int RECEIVE_PACKET_CHAT = 919191;
    private static final int RECEIVE_PACKET_INVITE = 919192;
    private static final int RECEIVE_PACKET_SUBSCRIBE = 919193;
    private static final int LOST_CONNECTION = 919194;

    // Login result code
    protected static final int LOGIN_FAILED_WRONG_CREDENTIAL = 0;
    protected static final int LOGIN_FAILED_TIMEOUT = 1;
    protected static final int LOGIN_FAILED_INTERRUPTED = 2;
    protected static final int LOGIN_FAILED_CHECK_CONNECTION = 3;
    protected static final int LOGIN_FAILED_SERVER_NOT_RESPONSE = 4;
    protected static final int LOGIN_FAILED_WRONG_CLIENT_VERSION = 5;
    protected static final int LOGIN_SUCCESS = 6;

    // Notification ID
    private static final int NOTIFICATION_RUNNING    = 1111;
    private static final int NOTIFICATION_MESSAGE    = 1112;
    private static final int NOTIFICATION_CONNECTION = 1113;

    // settings
    private boolean mCombineNotification;
    private boolean mMessageNotification;
    private boolean mShowToast;
    private boolean mVibration;
    private boolean mSound;

    // xmppConnection socket (smack api & rtmps api)
    private ConnectionConfiguration config = null;
    private XMPPConnection xmppConnection = null;
    private LeagueConnection leagueConnection = null;
    private LeagueServer leagueServer = null;
    private boolean useRTMPSservice = true;
    private boolean isConnectionLosted = false;
    private boolean isStopByClient = false;

    // chat manager (smack api)
    private ChatManager mChatmanager = null;
    private Chat mChat = null;

    // User & Roster data (smack api & rtmps api)
    private Roster mRoster = null;
    private String mUserID = null;
    private String mUserAccount = null;
    private String mSummonerName = null;
    private ParcelableRoster mUserPresense = null;
    private LeagueSummoner mUserSummoner = null;

    // SQLite db utility
    private SQLiteDbAdapter mSQLiteDbAdapter = new SQLiteDbAdapter(this);

    // Notification manager
    private NotificationManager mNotificationManager = null;
    private ArrayList<String> mMessageInbox = new ArrayList<String>();
    private int mMessageInboxNum = 0;
    private Toast mToast = null;

    //private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    private ArrayList<Messenger> mClientMessenger = new ArrayList<Messenger>();
    private ArrayList<String> mChatParticipant = new ArrayList<String>();

    // async tasks
    private LoginTask mLoginTask = null;
    private RetrieveMatchListTask mRetrieveMatchListTask = null;
    private LogoutTask mDisconnectTask = null;

    // IPC Messenger & Handler
    private Messenger mMessenger = new Messenger(new IncomingHandler());
    private Handler mPacketHandler = null;
    private Thread mListenerThread = null;
    private ArrayList<Message> mImportantMessageQueue = new ArrayList<Message>();

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        SharedPreferences prefs = getSharedPreferences( "settings", Context.MODE_PRIVATE);
        mCombineNotification = prefs.getBoolean("RunningNotification", true);
        mMessageNotification= prefs.getBoolean("MessageNotification", true);
        mShowToast          = prefs.getBoolean("ShowToast", true);
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
        Log.d(TAG, "onDestroy()");
        mSQLiteDbAdapter.close();

        //Log.d(TAG, "isStopByClient:" + isStopByClient + " / isConnectionLosted:" + isConnectionLosted);
        //if(!isStopByClient) notifyServiceDead();

        if(xmppConnection != null || leagueConnection != null) {
            if(mDisconnectTask == null) try {
                mDisconnectTask = new LogoutTask();
                mDisconnectTask.execute().get();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        if(mNotificationManager != null ) {
            mNotificationManager.cancel(NOTIFICATION_RUNNING);
            mNotificationManager.cancel(NOTIFICATION_MESSAGE);

            if( isConnectionLosted && !isStopByClient ) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK ); //Intent.FLAG_ACTIVITY_NEW_TASK

                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notification = new NotificationCompat.Builder(getApplicationContext())
                        .setContentIntent(pendingIntent)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.notification_lost_connection))
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setTicker(getString(R.string.notification_lost_connection))
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .build();

                if(mSound) { notification.defaults = notification.defaults | Notification.DEFAULT_SOUND; }
                mNotificationManager.notify(NOTIFICATION_CONNECTION, notification);
            }
        }

        Log.i(TAG, "Destroyed");
        //Toast.makeText(this, "Service Destroyed", Toast.LENGTH_SHORT).show();

        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    // Message Handler from Messenger (Other Activities)
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            int mValue = 0;
            try {
                switch( msg.what ){
                    //Activity 바인딩 등록
                    case MSG_REGISTER_CLIENT:
                        if(!mClientMessenger.contains(msg.replyTo)) {
                            mClientMessenger.add(msg.replyTo);

                            if(msg.getData().getString("participantID") != null) {
                                if(!mChatParticipant.contains(msg.getData().getString("participantID"))) {
                                    mChatParticipant.add(msg.getData().getString("participantID"));
                                    mNotificationManager.cancel(NOTIFICATION_MESSAGE);
                                    if(mCombineNotification) notifyRunning(false);
                                    mMessageInbox.clear();
                                    mMessageInboxNum = 0;
                                }
                            }
                            Log.i(TAG, msg.replyTo + " is registered into service, (size " + mClientMessenger.size() + ")");
                            //ack
                            Message ack = msg.obtain(null, MSG_REGISTER_CLIENT);
                            Bundle bundle = new Bundle();
                            bundle.putString("userAccount", mUserAccount);
                            bundle.putString("userJID", mUserID);
                            bundle.putString("userName", mSummonerName);
                            ack.setData(bundle);
                            msg.replyTo.send(ack);

                        } else {
                            Log.e(TAG, msg.replyTo + " is already registered into service, (size " + mClientMessenger.size() + ")");
                        }

                        if(mImportantMessageQueue.size()>0) {
                            Log.i(TAG, "resend urgent message: " + mImportantMessageQueue.get(0));
                            msg.replyTo.send(mImportantMessageQueue.get(0));
                            mImportantMessageQueue.remove(0);
                        }
                        break;

                    //Activity 바인딩 해제
                    case MSG_UNREGISTER_CLIENT:
                        if(mClientMessenger.contains(msg.replyTo)) {
                            mClientMessenger.remove(msg.replyTo);
                            String pid = msg.getData().getString("participantID");
                            if(pid != null) {
                                while(mChatParticipant.contains( pid ))
                                    mChatParticipant.remove( pid );
                            }
                            Log.i(TAG, msg.replyTo + " is unregistered from service, (size " + mClientMessenger.size() + ")");
                        } else {
                            Log.e(TAG, msg.replyTo + " is already unregistered from service, (size " + mClientMessenger.size() + ")");
                        }
                        break;

                    //로그인 시도
                    case MSG_ATTEMPT_LOGIN:
                        Log.i(TAG, "receive LOGIN attempt from activity, msg : " + msg);

                        //receive data
                        Bundle receivedBundle = msg.getData();
                        String[] mCredentialInput = receivedBundle.getStringArray("credential");
                        mUserAccount = mCredentialInput[0];

                        //connection setting
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

                        //login
                        doLogin(mCredentialInput);
                        break;

                    case MSG_CANCEL_LOGIN:
                        Log.i(TAG, "receive cancel login msg");
                        if(mLoginTask != null) mLoginTask.cancel(true);
                        break;

                    case MSG_REQUEST_ROSTER:
                        Log.i(TAG, "receive Roster Request from activity, msg : " + msg);

                        Bundle bundle = new Bundle(ParcelableRoster.class.getClassLoader());
                        bundle.putParcelableArrayList("RosterList", getRosterList());
                        bundle.putStringArrayList("RosterGroupList", getRosterGroupList());
                        bundle.putString("UserID", xmppConnection.getUser());
                        bundle.putString("UserAccount", mUserAccount);
                        bundle.putParcelable("UserPresence", mUserPresense);

                        Message rosterMessage = msg.obtain(null, MSG_REQUEST_ROSTER);
                        rosterMessage.setData(bundle);

                        Log.i(TAG, "send Roster msg : " + rosterMessage);
                        msg.replyTo.send(rosterMessage);
                        break;

                    case MSG_REQUEST_MATCH:
                        Log.i(TAG, "receive Match Request from: " + msg.replyTo);

                        final String userID;
                        final Messenger replyTo = msg.replyTo;

                        if(msg.getData().getString("userID") == null) { userID = mUserID; }
                        else { userID = msg.getData().getString("userID"); }

                        if(mRetrieveMatchListTask != null) {
                            mRetrieveMatchListTask.cancel(true);
                        }
                        mRetrieveMatchListTask = new RetrieveMatchListTask();
                        mRetrieveMatchListTask.addCallback(new CallbackToMessenger() {
                            @Override
                            public void sendMessage(Object object) {
                                Object[] result = (Object[]) object;

                                ArrayList<ParcelableMatchHistory> matchHistories = null;
                                ParcelableRoster roster = null;
                                Date FWOTD = null;
                                Bundle matchbundle = null;

                                if(result != null) {
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
                                    matchbundle.putString("userID", String.valueOf(roster.getUserID()));
                                } else {
                                    matchbundle = new Bundle();
                                    matchbundle.putBoolean("error", true);
                                }

                                Message matchMessage = Message.obtain(null, MSG_REQUEST_MATCH);
                                matchMessage.setData(matchbundle);
                                Log.i(TAG, "send MatchList msg : " + matchMessage);
                                try {
                                    replyTo.send(matchMessage);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        mRetrieveMatchListTask.execute(userID);
                        break;

                    case MSG_REQUEST_SUMMONER_NAME:
                        Log.i(TAG, "receive Summoner Lookup Request from activity, msg : " + msg);
                        final String summonerID = msg.getData().getString("id");

                        RetrieveSummonerNameTask requestSummonerNameTask = new RetrieveSummonerNameTask();
                        requestSummonerNameTask.addCallback(new CallbackToMessenger() {
                            @Override
                            public void sendMessage(Object result) {
                                if(result != null) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("id", summonerID);
                                    bundle.putString("name", (String)result);
                                    sendMessageToClient(MSG_REQUEST_SUMMONER_NAME, bundle, false);
                                }
                            }
                        });
                        requestSummonerNameTask.execute(summonerID);
                        break;

                    case MSG_CHAT_OPEN:
                        Log.i(TAG, "receive Chat open request, msg : " + msg);
                        //receive data
                        String buddyID = msg.getData().getString("buddyID");
                        //sendChatMessage();

                        //reply
                        //msg.obtain(null, MSG_ATTEMPT_LOGIN);
                        break;

                    case MSG_CHAT_SEND_MESSAGE:
                        Log.i(TAG, "receive sending message request , msg : " + msg);

                        //receive data
                        msg.getData().setClassLoader(ParcelableMessage.class.getClassLoader());
                        ParcelableMessage message = msg.getData().getParcelable("message");
                        sendChatMessage(message);
                        break;

                    case MSG_UPDATE_PRESENCE:
                        Log.i(TAG, "receivce update presence request, msg : " + msg);
                        msg.getData().setClassLoader(ParcelableRoster.class.getClassLoader());
                        updatePresence( (ParcelableRoster) msg.getData().getParcelable("userPresence"));
                        notifyRunning(false);
                        break;

                    case MSG_UPDATE_SETTING:
                        Log.i(TAG, "receivce update setting request, msg : " + msg);
                        updateSetting(msg.getData());
                        break;

                    case MSG_SUBSCRIBE_ROSTER:
                        Log.i(TAG, "receivce subscribe response, msg : " + msg);
                        if(msg.arg1 == 1) {
                            String id = msg.getData().getString("from");
                            String name = msg.getData().getString("name");
                            String group = getString(R.string.default_group);
                            addRosterEntry(id, name, group);
                        } else {
                            String id = msg.getData().getString("from");
                            rejectSubscription(id);
                        }
                        break;

                    case MSG_STOP_SERVICE:
                        Log.i(TAG, "receive stop service request , msg : " + msg);
                        isStopByClient = true;

                        notifyServiceDead();
                        stopSelf(); // finish()
                        break;

                    default:
                        super.handleMessage(msg);
                } //end of switch
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClientMessenger = null;
                notifyServiceDead();
                stopSelf();
            }
        }
    }

    // Message Handler from Thread
    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG,"Received message from listener thread: " + msg);
            switch (msg.what) {
                case RECEIVE_PACKET_CHAT:
                case RECEIVE_PACKET_INVITE:
                    org.jivesoftware.smack.packet.Message receivedMessage = (org.jivesoftware.smack.packet.Message )msg.obj;
                    String fromName = StringUtils.parseBareAddress(receivedMessage.getFrom());

                    // get and make message object
                    ParcelableMessage parcelableMessage = new ParcelableMessage(fromName, mUserID, null, 1);

                    if(msg.what==RECEIVE_PACKET_CHAT) { parcelableMessage.setBody(receivedMessage.getBody()); }
                    else { parcelableMessage.setBody(parcelableMessage.parseInviteMessage(receivedMessage.getBody())); }

                    Bundle messageBundle = new Bundle(ParcelableMessage.class.getClassLoader());
                    messageBundle.putParcelable("message", parcelableMessage);

                    // add a record into SQLite DB
                    mSQLiteDbAdapter.addRecord(parcelableMessage);

                    // send to client
                    sendMessageToClient(MSG_CHAT_RECEIVE_MESSAGE, messageBundle, false);

                    // send via Broadcasting
                    //Intent intent = new Intent("com.example.lolmessenger.NEW_MESSAGE");
                    //Bundle bundle = new Bundle(ParcelableMessage.class.getClassLoader());
                    //bundle.putParcelable("message", message);
                    //intent.putExtra("message", bundle);
                    //sendBroadcast(intent);
                    //Log.i(TAG,"Send Boradcast intent : " + intent);

                    // notify

                    // notification setting
                    boolean isParticipant = false;
                    for(String id : mChatParticipant) {
                        if(id.equals(fromName)) isParticipant = true;
                    }
                    if(!isParticipant) {
                        if(mMessageNotification) notifyNewMessage(parcelableMessage);
                    }

                    break;

                case RECEIVE_PACKET_SUBSCRIBE:
                    //send to client
                    if(msg.getData().getString("type").equals("subscribe")) {
                        sendMessageToClient(MSG_SUBSCRIBE_ROSTER, msg.getData(), true);
                        ParcelableMessage message = new ParcelableMessage(
                                null,
                                mUserID,
                                String.format(getString(R.string.notification_content_buddy_subscription), msg.getData().getString("name")),
                                0);
                        notifyNewMessage(message);
                    } else if(msg.getData().getString("type").equals("unsubscribe")) {
                        //notify something
                    } else if(msg.getData().getString("type").equals("unsubscribed")) {
                        //notify rejected
                    }
                    break;


                case LOST_CONNECTION:
                    Log.d(TAG, "Listener Thread Connection Lost");
                    isConnectionLosted = true;
                    notifyServiceDead();
                    stopSelf(); // finish()
                    break;

            }
            super.handleMessage(msg);
        }
    }

    // Initiate Listener Thread
    public void setupListenerThread() {

        //create handler
        if(mPacketHandler != null) return;
        mPacketHandler = new MessageHandler();

        //create thread
        mListenerThread = new Thread() {
            @Override
            public void run() {

                //XMPP connection listners
                xmppConnection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.manual);
                xmppConnection.addPacketListener(new PacketListener() {
                    public void processPacket(Packet packet) {
                        //Presence Packet
                        if(packet instanceof Presence) {
                            Presence pres = (Presence)packet;

                            if(pres.getType().equals(Presence.Type.subscribe)               // new friend request packet
                                    || pres.getType().equals(Presence.Type.unsubscribe)     // friend removed packet
                                    || pres.getType().equals(Presence.Type.unsubscribed))   // request rejected packet
                            {
                                String name;
                                try { name = new RetrieveSummonerNameTask().execute(pres.getFrom()).get(); }
                                catch (Exception e) { name = getString(R.string.unknown_entry); }

                                Bundle bundle = new Bundle();
                                bundle.putString("type", pres.getType().name());
                                bundle.putString("from", pres.getFrom());
                                bundle.putString("name", name);
                                Message msg = mPacketHandler.obtainMessage(RECEIVE_PACKET_SUBSCRIBE);
                                msg.setData(bundle);

                                Log.i("PacketPresence", pres.getType().name() + " packet from " + pres.getFrom() + ", msg:" + msg);
                                mPacketHandler.sendMessage(msg);
                            }
                            else if(pres.getType().equals(Presence.Type.available)) {        // online friend
                                Log.i("PacketAvailable", pres.getType().name() + " packet from " + pres.getFrom()
                                        + " (" + xmppConnection.getRoster().getEntry(pres.getFrom().substring(0, pres.getFrom().indexOf("/"))) + ")");
                            }
                        }
                        //Message Packet
                        if (packet instanceof org.jivesoftware.smack.packet.Message ) {
                            org.jivesoftware.smack.packet.Message message = (org.jivesoftware.smack.packet.Message) packet;

                            // Chat packet
                            if (message.getBody() != null && message.getType().equals(org.jivesoftware.smack.packet.Message.Type.chat)) {
                                mPacketHandler.sendMessage(mPacketHandler.obtainMessage(RECEIVE_PACKET_CHAT, message));
                                Log.i("PacketListener", "Chatting Message received : " + message.getBody());
                            }

                            // Other packet
                            else if (message.getBody() != null && message.getType().equals(org.jivesoftware.smack.packet.Message.Type.normal)) {
                                if (!message.getBody().contains("@lvl.pvp.net")) {
                                    //invite to a game.
                                    mPacketHandler.sendMessage(mPacketHandler.obtainMessage(RECEIVE_PACKET_INVITE, message));
                                } else {
                                    //invite to a chat. ignore
                                }
                                Log.i("PacketListener", "Invite Message received : " + message.getBody());
                            }
                        } } //end of processPacket()
                }, new OrFilter(new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class), new PacketTypeFilter(Presence.class)));
                xmppConnection.addConnectionListener(new ConnectionListener() {
                    @Override
                    public void connectionClosed() {
                    }

                    @Override
                    public void connectionClosedOnError(Exception e) {

                        //if(!isStopByClient) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                                .setContentIntent(pendingIntent)
                                .setContentTitle(getString(R.string.notification_lost_connection_on_error))
                                .setContentText(e.getMessage())
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setTicker(getString(R.string.notification_lost_connection_on_error))
                                .setOnlyAlertOnce(true)
                                .setAutoCancel(true)
                                .build();
                        mNotificationManager.cancel(NOTIFICATION_RUNNING);
                        mNotificationManager.notify(NOTIFICATION_CONNECTION, notification);

                        mPacketHandler.sendMessage(mPacketHandler.obtainMessage(LOST_CONNECTION, null));

                        //}
                    }

                    @Override
                    public void reconnectingIn(int i) {
                        Log.i(TAG, "reconnecting... " + i);
                        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                                .setContentTitle(getString(R.string.app_name))
                                .setContentText(getString(R.string.notification_reconnecting) + "(" + i + ")")
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setTicker(getString(R.string.notification_reconnecting))
                                .setOnlyAlertOnce(true)
                                .setAutoCancel(true)
                                .build();
                        mNotificationManager.notify(NOTIFICATION_CONNECTION, notification);
                    }

                    @Override
                    public void reconnectionSuccessful() {
                        Log.i(TAG, "reconnecting success ");
                        mNotificationManager.cancel(NOTIFICATION_CONNECTION);
                    }

                    @Override
                    public void reconnectionFailed(Exception e) {
                        /*
                        //this.connectionClosedOnError(e);
                        Log.i(TAG, "reconnecting failed");
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                                .setContentTitle(getString(R.string.notification_disconnected))
                                .setContentText(getString(R.string.notification_reconnection_failed))
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setTicker(getString(R.string.notification_reconnection_failed))
                                .setOnlyAlertOnce(true)
                                .setAutoCancel(true)
                                .build();
                        mNotificationManager.notify(NOTIFICATION_CONNECTION, notification);
                        mDisconnectTask = new LogoutTask();
                        mDisconnectTask.execute();
                        */
                    }
                });
                xmppConnection.getRoster().addRosterListener(new RosterListener() {
                    @Override
                    public void entriesAdded(Collection<String> strings) {
                        Log.i(TAG,"Roster listener: entries added");
                        sendRosterToClient();
                    }

                    @Override
                    public void entriesUpdated(Collection<String> strings) {
                        Log.i(TAG,"Roster listener: entries updated");
                        sendRosterToClient();
                    }

                    @Override
                    public void entriesDeleted(Collection<String> strings) {
                        Log.i(TAG,"Roster listener: entries deleted");
                        sendRosterToClient();
                    }

                    @Override
                    public void presenceChanged(Presence presence) {
                        Log.i(TAG,"Roster listener: presence Changed");
                        sendRosterToClient();
                    }

                    public void sendRosterToClient() {
                        Bundle bundle = new Bundle(ParcelableRoster.class.getClassLoader());
                        bundle.putParcelableArrayList("RosterList", getRosterList());
                        bundle.putStringArrayList("RosterGroupList", getRosterGroupList());
                        bundle.putString("UserID", xmppConnection.getUser());
                        bundle.putString("UserAccount", mUserAccount);
                        bundle.putParcelable("UserPresence", mUserPresense);

                        sendMessageToClient(MSG_REQUEST_ROSTER, bundle, false);
                    }
                });

                //Listener Setup Complete
                Log.i(TAG + ".listenerThread", "Listener Setup Complete." );

                while(true) try {

                    Log.d(TAG + ".listenerThread", "Deamon thread is running...");

                    if(mClientMessenger.size() > 0) sleep(10000); //Client foreground : sleep 10sec
                    else                            sleep(30000); //Client background : sleep 60sec

                    //Checking connection
                    xmppConnection.getRoster().reload();                          //Throws: java.lang.IllegalStateException - if connection is not logged in or logged in anonymously
                    //leagueConnection.getSummonerService().getSummonerName(0);   //Throws: LeagueException - if AuthToken is lost
                    if( xmppConnection.isSocketClosed() || !leagueConnection.getAccountQueue().isAnyAccountConnected() ) {
                        Log.e(TAG + ".listenerThread","Connection Losted");
                        mPacketHandler.sendMessage(mPacketHandler.obtainMessage(LOST_CONNECTION, null));
                        break;
                    }
                    mRoster = xmppConnection.getRoster();

                } catch (InterruptedException e) {
                    //Interrupted by service
                    Log.e(TAG + ".listenerThread","Interrupted");

                } catch (Exception e) {
                    //other exceptions caused by above connections
                    Log.e(TAG + ".listenerThread", "EXCEPTION " + e.getMessage());
                    mPacketHandler.sendMessage(mPacketHandler.obtainMessage(LOST_CONNECTION, null));
                } //end of while
            } //end of run()
        }; //end of thread definition

        mListenerThread.setDaemon(true);
        mListenerThread.start();
    }

    // IPC communication method
    public void sendMessageToClient(int type, Bundle bundle, boolean important) {
        Message msg = Message.obtain(null, type);
        msg.setData(bundle);

        if(mClientMessenger.size()>0) try {
            Log.i(TAG, "Send message to Activity : " + msg);
            for(Messenger messenger : mClientMessenger) {
                try {
                    messenger.send(msg);
                } catch (DeadObjectException e) {
                    //Client is dead. remove it
                    //mClientMessenger.remove(messenger);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } else {
            if(important) {
                Log.e(TAG, "No client is connected, added to urgent queue:" + msg);
                mImportantMessageQueue.add(msg);
            }
        }
    }

    // Login
    public void doLogin(String[] _credential) {
        if(mLoginTask == null) {
            mLoginTask = new LoginTask();
            mLoginTask.execute(_credential);
        }
    }

    // XMPP methods
    public void refreshRoster() {

        try {
            mRoster = xmppConnection.getRoster();
        } catch (Exception e) {
            isConnectionLosted = true;

            notifyServiceDead();
            stopSelf(); // finish()
            return;
        }
    }

    public ArrayList<ParcelableRoster> getRosterList() {

        refreshRoster();

        if(mRoster != null) {

            ArrayList<ParcelableRoster> rosterList = new ArrayList<ParcelableRoster>();
            ArrayList<String> nullEntry = new ArrayList<String>();

            for(RosterEntry entry : mRoster.getEntries()) {

                if(entry.getName() != null) {
                    ParcelableRoster tempRoster = new ParcelableRoster(entry, mRoster.getPresence(entry.getUser()));
                    rosterList.add(tempRoster);
                } else try {

                    Log.e(TAG, "null entry found: " + entry.getUser());

                    if(nullEntry.contains(entry.getUser())) continue;
                    else nullEntry.add(entry.getUser());

                    RetrieveSummonerNameTask retrieveSummonerNameTask = new RetrieveSummonerNameTask();
                    String name = retrieveSummonerNameTask.execute(entry.getUser()).get(5, TimeUnit.SECONDS);
                    if(name == null) name = getString(R.string.unknown_entry);

                    entry.setName(name);
                    ParcelableRoster tempRoster = new ParcelableRoster(entry, mRoster.getPresence(entry.getUser()));

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            nullEntry.clear();
            return rosterList;

        }

        return null;
    }

    public ArrayList<String> getRosterGroupList() {
        ArrayList<String> rosterGroupList = new ArrayList<String>();
        for(RosterGroup entry : mRoster.getGroups()) {
            rosterGroupList.add(entry.getName());
        }
        rosterGroupList.add("Offline");
        return rosterGroupList;
    }

    public boolean sendChatMessage(ParcelableMessage message) {
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
        SharedPreferences prefs = getSharedPreferences("presence_" + mUserAccount, MODE_PRIVATE);
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
            presence.setStatus(mUserPresense.toPresencePacketString());
        }


        Log.i(TAG, "update user presence: " + presence.toString());
        try {
            xmppConnection.sendPacket(presence);
        } catch (Exception e) {
            isConnectionLosted = true;

            notifyServiceDead();
            stopSelf(); // finish()
            return;
        }
    }

    public void addRosterEntry(String id, String name, String groupname) {
        String[] group = { groupname };
        try {
            xmppConnection.getRoster().createEntry(id, name, group);
        } catch (XMPPException e) {
            //e.printStackTrace();
            //stopself();
        }
    }

    public void rejectSubscription(String id) {
        Presence packet = new Presence(Presence.Type.unsubscribed);
        packet.setFrom(mUserID.substring(0,mUserID.indexOf("/")));
        packet.setTo(id);
        xmppConnection.sendPacket(packet);
    }

    public void updateSetting(Bundle bundle) {

        mCombineNotification= bundle.getBoolean("RunningNotification");
        mMessageNotification= bundle.getBoolean("MessageNotification");
        mVibration          = bundle.getBoolean("Vibration");
        mSound              = bundle.getBoolean("Sound");
        mShowToast          = bundle.getBoolean("ShowToast");

        //if(!mCombineNotification) mNotificationManager.cancel(NOTIFICATION_RUNNING);
        //else notifyRunning(false);
        notifyRunning(false);
    }

    public String getUserID() {
        return mUserID;
    }

    public String getUserAccount() {
        return mUserAccount;
    }

    // LeagueConnection Methods
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
            if(entry.getIpEarned() > 165 ) {
                if(FWOTD == null) FWOTD = entry.getCreationDate();
                else if(entry.getCreationDate().getTime() > FWOTD.getTime()) FWOTD = entry.getCreationDate();
            }
        }

        if(FWOTD != null) FWOTD = new Date( FWOTD.getTime() + (long)1000*60*60*22 );

        return FWOTD;
    }

    // Notification Methods
    public void notifyNewMessage(ParcelableMessage message) {

        PendingIntent pendingIntent;
        boolean isValidMessage;
        if(message.getFromID() != null) {
            //valid user
            isValidMessage = true;
            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.putExtra("userID", mUserID);
            intent.putExtra("buddyID", message.getFromID());
            ParcelableRoster buddy = new ParcelableRoster(
                    mRoster.getEntry(message.getFromID()), mRoster.getPresence(message.getFromID()));
            intent.putExtra("buddy", buddy);
            intent.setExtrasClassLoader(ParcelableRoster.class.getClassLoader());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            //invalid user
            isValidMessage = false;
            Intent intent = new Intent(getApplicationContext(), ClientActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // Set inbox message
        String trimmedString = message.getBody();
        if(trimmedString.contains("\n")) trimmedString = trimmedString.substring(0,trimmedString.indexOf('\n'));
        if(isValidMessage) {
            mMessageInbox.add( ((mRoster.getEntry(message.getFromID()) != null ) ? mRoster.getEntry(message.getFromID()).getName() : getString(R.string.unknown_entry)) + " : " + trimmedString );
        } else {
            mMessageInbox.add( trimmedString );
        }
        if(mMessageInbox.size() > 3)     mMessageInbox.remove(0);
        mMessageInboxNum++;

        // Set summary text
        String summary = String.format(getString(R.string.notification_total_unread), mSQLiteDbAdapter.getNotReadMessageCount(mUserID));

        // Sets an Inbox style big view
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        // Sets attributes for Inbox style
        inboxStyle.setBigContentTitle(getString(R.string.notification_inbox_title));
        inboxStyle.setSummaryText(summary);

        for (int i=0; i < mMessageInbox.size(); i++) {
            inboxStyle.addLine(mMessageInbox.get(i));
        }

        String contentTitle;
        if(isValidMessage) contentTitle = (mRoster.getEntry(message.getFromID())!=null) ? mRoster.getEntry(message.getFromID()).getName() : getString(R.string.unknown_entry);
        else contentTitle = getString(R.string.notification_title_buddy_subscription);

        Notification notification;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(contentTitle)
                .setContentText(message.getBody())
                .setContentIntent(pendingIntent)
                //.setContentInfo(summary)
                //.setNumber(mMessageInboxNum)
                .setStyle(inboxStyle)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker( isValidMessage ? mMessageInbox.get(mMessageInbox.size() - 1) : contentTitle )
                .setDefaults(Notification.DEFAULT_LIGHTS
                        | (mSound ? Notification.DEFAULT_SOUND : 0)
                        | (mVibration ? Notification.DEFAULT_VIBRATE : 0));

        if(mCombineNotification) {
            builder.setOngoing(true);
            builder.setOnlyAlertOnce(false);
            notification = builder.build();
            //mNotificationManager.cancel(NOTIFICATION_RUNNING);
            //mNotificationManager.notify(NOTIFICATION_RUNNING, notification);
            //stopForeground(true);
            startForeground(NOTIFICATION_RUNNING, notification);
        } else {
            builder.setAutoCancel(true);
            notification = builder.build();
            mNotificationManager.notify(NOTIFICATION_MESSAGE, notification);
        }

        //Show toast
        if(isValidMessage) {
            Log.i(TAG,"mShowToast" + mShowToast);
            if(mShowToast) {
                ParcelableRoster tempEntry = new ParcelableRoster(mRoster.getEntry(message.getFromID()), mRoster.getPresence(mRoster.getEntry(message.getFromID()).getUser()));

                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.toast, null);
                //(ViewGroup) findViewById(R.id.toast_layout_root));

                ImageView profileIcon = (ImageView) layout.findViewById(R.id.profileicon);
                TextView substituteText = (TextView) layout.findViewById(R.id.profileiconSubstituteText);
                try {
                    int iconID = R.drawable.class.getField("profile_icon_" + tempEntry.getProfileIcon() ).getInt(null);
                    profileIcon.setImageResource(iconID);
                    profileIcon.setVisibility(View.VISIBLE);
                    substituteText.setVisibility(View.GONE);
                } catch (Exception e) {
                    if(tempEntry.getProfileIcon() == -1) substituteText.setText("");
                    else substituteText.setText(String.valueOf(tempEntry.getProfileIcon()));
                    substituteText.setVisibility(View.VISIBLE);
                    profileIcon.setVisibility(View.GONE);
                }

                //edit Name TextView
                TextView nameText = (TextView) layout.findViewById(R.id.textViewName);
                nameText.setText((tempEntry.getUserName()!=null) ? tempEntry.getUserName() : getString(R.string.unknown_entry));

                //edit Message TextView
                TextView messageText = (TextView) layout.findViewById(R.id.textViewMessage);
                messageText.setText(trimmedString);

                if(mToast != null) mToast.cancel();
                mToast = new Toast(getApplicationContext());
                mToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 200);
                mToast.setDuration(Toast.LENGTH_LONG);
                mToast.setView(layout);
                mToast.show();
            }
        }
    }

    public void notifyRunning(boolean ringing) {

        //if(!mCombineNotification) return;

        String statusString;
        if(mUserPresense.getAvailablity().equals("available")) {
            if(mUserPresense.getMode().equals("away")) {
                statusString = getString(R.string.status_away);
            } else {  //if(mUserPresense.getMode().equals("chat"))
                //if(!mUserPresense.getStatusMsg().isEmpty()) {
                //    statusString = "( " + getString(R.string.status_online) + ") " + mUserPresense.getStatusMsg();
                //} else {
                    statusString = getString(R.string.status_online);
                //}
            }
        } else {
            statusString = getString(R.string.status_offline);
        }

        Intent intent = new Intent(getApplicationContext(), ClientActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle( mUserPresense.getUserName() + " (" + statusString +")")
                .setContentText( getString(R.string.notification_app_running))
                //.setContentInfo(getString(R.string.app_name))
                .setWhen(0)
                .setSmallIcon(R.drawable.ic_launcher)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent);

        if(ringing) {
            builder.setTicker(getString(R.string.notification_app_running));
            builder.setDefaults(Notification.DEFAULT_LIGHTS
                | (mSound ? Notification.DEFAULT_SOUND : 0)
                | (mVibration ? Notification.DEFAULT_VIBRATE : 0));
        } else {
            builder.setTicker(null);
        }

        Notification notification = builder.build();


        //mNotificationManager.notify(NOTIFICATION_RUNNING, notification);
        startForeground(NOTIFICATION_RUNNING, notification);
        mNotificationManager.cancel(NOTIFICATION_CONNECTION);
    }

    public void notifyServiceDead() {
        if(mClientMessenger != null && mClientMessenger.size() > 0) try {
            Message msg = Message.obtain(null, MSG_SERVICE_DEAD);
            for(Messenger msgr : mClientMessenger) {
                Log.i(TAG, "send service dead message, " + msg);
                msgr.send(msg);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        stopForeground(true);
    }

    //Asynchronous Task
    public class LoginTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected Integer doInBackground(String... params) {

            String loginId = params[0];
            String loginPassword = params[1];

            // old SSL authentication method
            config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
            config.setSocketFactory(new DummySSLSocketFactory());

            //try to connect RTMPS server
            LeagueAccount leagueAccount;
            sendLoginProgressMessage(getString(R.string.login_process_connecting));
            try {
                //if(leagueServer.getServerCode().equals("KR")) version = getString(R.string.client_version);
                //else version = getString(R.string.client_version_2);
                String version = getString(R.string.client_version);

                Log.i(TAG, "leagueserver" + leagueServer);
                leagueAccount = new LeagueAccount(leagueServer, version, loginId, loginPassword);
                leagueAccount.connect(new CallbackToMessenger() {
                    @Override
                    public void sendMessage(Object object) {
                        String string = (String) object;
                        if( Locale.getDefault().getLanguage().equals("ko") ) {
                            StringBuffer sb = new StringBuffer();
                            for(int i = 0; i < string.length(); i++){
                                if( Character.isDigit( string.charAt(i) ) ) {
                                    sb.append( string.charAt(i) );
                                }
                            }
                            string = getString(R.string.message_logging_in_line) + Integer.parseInt(sb.toString());
                        }
                        sendLoginProgressMessage(string);
                    }
                });
                leagueConnection.getAccountQueue().addAccount(leagueAccount);

            } catch (LeagueException e) {
                if(e.getMessage() != null && e.getMessage().contains("Wrong client version")) try {
                    String version = getString(R.string.client_version_2);

                    sendLoginProgressMessage(getString(R.string.error_wrong_client_version) + version);

                    Log.i(TAG, "Wrong client version for server, try " + version);
                    leagueAccount = new LeagueAccount(leagueServer, version, loginId, loginPassword);
                    leagueAccount.connect(new CallbackToMessenger() {
                        @Override
                        public void sendMessage(Object object) {
                            String string = (String) object;
                            if( Locale.getDefault().getLanguage().equals("ko") ) {
                                StringBuffer sb = new StringBuffer();
                                for(int i = 0; i < string.length(); i++){
                                    if( Character.isDigit( string.charAt(i) ) ) {
                                        sb.append( string.charAt(i) );
                                    }
                                }
                                string = getString(R.string.message_logging_in_line) + Integer.parseInt(sb.toString());
                            }
                            sendLoginProgressMessage(string);
                        }
                    });
                    leagueConnection.getAccountQueue().addAccount(leagueAccount);

                } catch (LeagueException e1) {
                    e1.printStackTrace();
                    return LOGIN_FAILED_WRONG_CLIENT_VERSION;
                } else if(e.getMessage() != null && e.getMessage().contains("Unable to resolve host")) {
                    e.printStackTrace();
                    return LOGIN_FAILED_SERVER_NOT_RESPONSE;
                } else if(e.getMessage() != null && e.getMessage().contains("Incorrect username")){
                    e.printStackTrace();
                    return LOGIN_FAILED_WRONG_CREDENTIAL;
                } else {
                    e.printStackTrace();
                    return LOGIN_FAILED_CHECK_CONNECTION;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return LOGIN_FAILED_CHECK_CONNECTION;
            }

            Log.i(TAG, "Connected to RMTP");

            //attempt to contact the XMPP server
            try {
                xmppConnection.connect();
                Log.i(TAG, "Connected to " + xmppConnection.getHost());
            } catch (XMPPException e) {
                Log.e(TAG, "Failed to connect to " + xmppConnection.getHost());
                return LOGIN_FAILED_CHECK_CONNECTION;
            }

            //athenticate to the XMPP server
            sendLoginProgressMessage(getString(R.string.login_process_chattingserver));
            try {
                xmppConnection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.manual);
                xmppConnection.getRoster().setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
                xmppConnection.login(loginId, "AIR_" + loginPassword, "xiff");
                //xmppConnection.login(loginId, "AIR_" + loginPassword);
                xmppConnection.sendPacket(new Presence(Presence.Type.unavailable));
                xmppConnection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.manual);
                xmppConnection.getRoster().setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
                mUserID = xmppConnection.getUser();
                Log.i(TAG, "Authenticated as " + loginId + " / " + mUserID);
            } catch (XMPPException e) {
                Log.e(TAG, "Failed to login as " + loginId);
                return LOGIN_FAILED_WRONG_CREDENTIAL;
            } catch (Exception e) {
                return LOGIN_FAILED_CHECK_CONNECTION;
            }

            //Connected to XMPP server
            Log.i(TAG, "Connected to XMPP server");

            return LOGIN_SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer result) {

            //make result bundle
            Bundle bundle = new Bundle();
            bundle.putInt("result", result);
            Log.i(TAG, "OnPostExcute: " + result);

            //login success
            if(result == LOGIN_SUCCESS) {

                // setup listener thread
                setupListenerThread();

                // get xmpp objects
                mChatmanager = xmppConnection.getChatManager();
                mRoster = xmppConnection.getRoster();

                // load presence
                try {
                    mSummonerName = new RetrieveSummonerNameTask().execute(mUserID).get();
                    LeagueSummoner summoner = new RetrieveSummonerTask().execute(mSummonerName).get();

                    mUserPresense = new ParcelableRoster(summoner);
                    mUserPresense.setUserID(mUserID);
                    mUserPresense.setAvailablity("available");

                    SharedPreferences prefs = getSharedPreferences("presence_" + mUserAccount, MODE_PRIVATE);
                    mUserPresense.setStatusMsg( prefs.getString("StatusMsg", getString(R.string.default_status_message)) );
                    updatePresence(mUserPresense);

                    //set static user information
                    bundle.putString("userAccount", mUserAccount);
                    bundle.putString("userJID", mUserID);
                    bundle.putString("userName", mSummonerName);

                } catch (Exception e) {
                    return;
                }

                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notifyRunning(true);

            } else {
                try {
                    mDisconnectTask = new LogoutTask();
                    mDisconnectTask.execute().get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            //send result message
            sendMessageToClient(MSG_ATTEMPT_LOGIN, bundle, true);

            mLoginTask = null;

            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.e(TAG, "LoginTask Canceled");
            try {
                mDisconnectTask = new LogoutTask();
                mDisconnectTask.execute().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            mLoginTask = null;
        }

        protected void sendLoginProgressMessage(String progress) {
            Bundle bundle = new Bundle();
            bundle.putString("Progress", progress);
            sendMessageToClient(MSG_LOGIN_PROGRESS_UPDATE, bundle, false);
        }

    }

    public class LogoutTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            Log.i(TAG, "LogoutTask is Running..");

            if(mListenerThread != null) {
                mListenerThread.interrupt();
                mListenerThread = null;
            }

            if(xmppConnection != null) {
                xmppConnection.disconnect();
            }

            if(leagueConnection != null) {
                if(leagueConnection.getAccountQueue().isAnyAccountConnected()) leagueConnection.getAccountQueue().nextAccount().close();
                leagueConnection = null;
            }

            Log.i(TAG, "Disconnected");
            return true;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mDisconnectTask = null;
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG,"LogoutTask cancelled");
            mDisconnectTask = null;
        }

    }

    public class RetrieveSummonerNameTask extends AsyncTask<String, Void, String> {

        CallbackToMessenger callback = null;

        public void addCallback(CallbackToMessenger callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... userID) {
            try {
                return leagueConnection.getSummonerService().getSummonerName(Integer.parseInt(userID[0].substring(3, userID[0].indexOf("@"))));
            } catch (LeagueException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if(callback != null) callback.sendMessage(result);
        }
    }

    public class RetrieveSummonerTask extends AsyncTask<String, Void, LeagueSummoner> {

        @Override
        protected LeagueSummoner doInBackground(String... userID) {

            try {
                return leagueConnection.getSummonerService().getSummonerByName(userID[0]);
            } catch (LeagueException e) {
                e.printStackTrace();
                return null;
            }

        }
    }

    public class RetrieveMatchListTask extends AsyncTask<String, Void, Object[]> {

        CallbackToMessenger callback = null;

        public void addCallback(CallbackToMessenger callback) {
            this.callback = callback;
        }

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

            } catch (LeagueException e) {
                //if(e.getMessage() != null && e.getMessage().equals("org.springframework.security.authentication.AuthenticationCredentialsNotFoundException"))
                //    leagueConnection.getAccountQueue().nextAccount().close();
                Log.i(TAG, "code" + e.getErrorCode() + " / mesg " + e.getMessage() + "");
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Object[] result) {
            mRetrieveMatchListTask = null;
            if(callback != null) callback.sendMessage(result);
        }

        @Override
        protected void onCancelled() {
            mRetrieveMatchListTask = null;
        }

    }

}

