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

    // Connection Management Object
    //private ConnectionManager mConnectionManager;

    // Login Request Code
    static final int REQUEST_CODE = 1234;
    private static final String TAG = "com.rokoroku.lolmessenger.MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get ConnectionManager from Application
        // mConnectionManager = (ConnectionManager) getApplication();

        // Login
        if(!isRunningChatService()) {
            Intent intentForLogin = new Intent(this, LoginActivity.class);
            //startActivityForResult(intentForLogin, 1234);
            startActivity(intentForLogin);
        }
        else {
            startActivity( new Intent(this, ClientActivity.class) );
        }
        finish();
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode){
            case REQUEST_CODE:
                //if(resultCode == RESULT_OK){ }
                //mConnectionManager.addToModel(mConnectionManager.getRosterCollection());
                startActivity( new Intent(this, ClientActivity.class) );
                finish();
        }
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
