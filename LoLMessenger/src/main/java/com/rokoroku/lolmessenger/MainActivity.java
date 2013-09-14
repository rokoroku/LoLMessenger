package com.rokoroku.lolmessenger;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

/**
 * Created by Youngrok Kim on 13. 8. 13.
 */
public class MainActivity extends Activity {

    private static final String TAG = "com.rokoroku.lolmessenger.MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Login
        if(!isRunningChatService() || LolMessengerApplication.isLoggingOn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
        // Goto Client if connected
        // else if(LolMessengerApplication.isConnected()) {
        else {
            Intent intent = new Intent(this, ClientActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }

        finish();
        setContentView(R.layout.activity_main);
    }

    private boolean isRunningChatService()
    {
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
