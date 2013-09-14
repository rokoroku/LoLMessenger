package com.rokoroku.lolmessenger;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import java.util.HashMap;

/**
 * Created by 영록 on 13. 9. 5.
 */
public class LolMessengerApplication extends Application {

    private static Context context;
    private static HashMap<Integer, String> mapStringMap = null;
    private static HashMap<String, String> champStringMap = null;

    private static boolean mRunningNotification;
    private static boolean mMessageNotification;
    private static boolean mVibration;
    private static boolean mSound;
    private static boolean mShowGameQueuetype;
    private static boolean mShowPlayingChampion;
    private static boolean mShowTimestamp;
    private static boolean mShowToast;

    private static boolean isLoggingOn = false;
    private static boolean isConnected = false;

    private static String userAccount = null;
    private static String userJID = null;
    private static String userName = null;

    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
        setupStringMap();
        loadSettings();
    }

    public void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        mRunningNotification = prefs.getBoolean("RunningNotification", true);
        mMessageNotification = prefs.getBoolean("MessageNotification", true);
        mVibration           = prefs.getBoolean("Vibration", true);
        mSound               = prefs.getBoolean("Sound", true);
        mShowGameQueuetype   = prefs.getBoolean("ShowGameQueueType", false);
        mShowPlayingChampion = prefs.getBoolean("ShowPlayingChampion", true);
        mShowTimestamp       = prefs.getBoolean("ShowTimestamp", true);
        mShowToast           = prefs.getBoolean("ShowToast", true);

    }

    public void updateSettings(Bundle bundle) {
        mRunningNotification = bundle.getBoolean("RunningNotification");
        mMessageNotification = bundle.getBoolean("MessageNotification");
        mVibration           = bundle.getBoolean("Vibration");
        mSound               = bundle.getBoolean("Sound");
        mShowGameQueuetype   = bundle.getBoolean("ShowGameQueueType");
        mShowPlayingChampion = bundle.getBoolean("ShowPlayingChampion");
        mShowTimestamp       = bundle.getBoolean("ShowTimestamp");
        mShowToast           = bundle.getBoolean("ShowToast");
        updateSettings();
    }

    public void updateSettings() {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean("RunningNotification", mRunningNotification);
        ed.putBoolean("MessageNotification", mMessageNotification);
        ed.putBoolean("Vibration", mVibration);
        ed.putBoolean("Sound", mSound);
        ed.putBoolean("ShowPlayingChampion", mShowPlayingChampion);
        ed.putBoolean("ShowGameQueueType", mShowGameQueuetype);
        ed.putBoolean("ShowTimestamp", mShowTimestamp);
        ed.putBoolean("ShowToast", mShowToast);
        ed.commit();
    }

    public void setupStringMap() {
        //setting map string
        if(mapStringMap == null) try {
            mapStringMap = new HashMap<Integer, String>();
            mapStringMap.put(0, null); // represents a catch-all champion for stats
            String[] items = getResources().getStringArray(R.array.map_strings);

            for(int i=0; i<items.length; i++) {
                int key = Integer.parseInt( items[i++] );
                String str = items[i];
                mapStringMap.put(key, str);
            }
            if(mapStringMap.size() == 0)
                mapStringMap = null;
        } catch (Exception e) {
            e.printStackTrace();
            mapStringMap = null;
        }

        //setting champ string
        if(champStringMap == null) try {
            champStringMap = new HashMap<String, String>();
            String[] items = getResources().getStringArray(R.array.champ_strings);

            for(int i=0; i<items.length; i++) {
                String key = items[i++];
                String str = items[i];
                champStringMap.put(key, str);
            }
            if(champStringMap.size() == 0)
                champStringMap = null;
        } catch (Resources.NotFoundException e) {
            //e.printStackTrace();
            champStringMap = null;
        }

    }

    public static Context getContext() {
        return context;
    }

    public static HashMap<Integer, String> getMapStringMap() {
        return mapStringMap;
    }

    public static HashMap<String, String> getChampStringMap() {
        return champStringMap;
    }

    public static String getUserAccount() {
        return userAccount;
    }

    public static void setUserAccount(String _userAccount) {
        userAccount = _userAccount;
    }

    public static String getUserJID() {
        return userJID;
    }

    public static void setUserJID(String _userJID) {
        if(_userJID != null && _userJID.contains("/")) userJID = _userJID.substring(0, _userJID.indexOf("/"));
        else userJID = _userJID;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setUserName(String _userName) {
        userName = _userName;
    }

    public static boolean isRunningNotification() {
        return mRunningNotification;
    }

    public static boolean isMessageNotification() {
        return mMessageNotification;
    }

    public static boolean isVibration() {
        return mVibration;
    }

    public static boolean isSound() {
        return mSound;
    }

    public static boolean isShowGameQueuetype() {
        return mShowGameQueuetype;
    }

    public static boolean isShowPlayingChampion() {
        return mShowPlayingChampion;
    }

    public static boolean isShowTimestamp() {
        return mShowTimestamp;
    }

    public static boolean isShowToast() {
        return mShowToast;
    }

    public static boolean isLoggingOn() {
        return isLoggingOn;
    }

    public static void setLoggingOn(boolean loggingOn) {
        isLoggingOn = loggingOn;
    }

    public static boolean isConnected() {
        return isConnected;
    }

    public static void setConnected(boolean connected) {
        isConnected = connected;
    }
}
