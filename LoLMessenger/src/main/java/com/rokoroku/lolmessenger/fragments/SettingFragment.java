package com.rokoroku.lolmessenger.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.rokoroku.lolmessenger.ClientActivity;
import com.rokoroku.lolmessenger.EditingActivity;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ParcelableRoster;


/**
 * Created by Youngrok Kim on 13. 8. 22.
 */
public class SettingFragment extends DialogFragment {

    private final int EDIT_STATUS_MSG = 1000;

    private String mUserAccount = null;
    private ParcelableRoster mPresense = null;
    private ClientActivity mActivity = null;

    private TextView mAccountView;
    private TextView mStatusMsgView;
    private TextView mAppNameView;
    private TextView mAppVersionVIew;

    private View mBoxStatusMsg;
    private View mBoxMailto;
    private Button mButtonLogout;
    private Button mButtonDropTable;
    private Switch mSwitchRunningNotification;
    private Switch mSwitchMessageNotification;
    private Switch mSwitchVibration;
    private Switch mSwitchSound;

    private boolean mRunningNotification;
    private boolean mMessageNotification;
    private boolean mVibration;
    private boolean mSound;

    private boolean isReady = false;


    public SettingFragment() {
    }

    public SettingFragment(ClientActivity activity, String userAccount, ParcelableRoster userRoster) {
        this.mActivity = activity;
        this.mUserAccount = userAccount;
        this.mPresense = userRoster;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        // the content
        //final RelativeLayout root = new RelativeLayout(getActivity());
        //root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        //root.setPadding(20, 20, 20, 20);

        // creating the fullscreen dialog
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setCancelable(true);
        //dialog.setContentView(root);
        //dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_frame);

        return dialog;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = mActivity.getSharedPreferences( "settings", Context.MODE_PRIVATE);
        mRunningNotification= prefs.getBoolean("RunningNotification", true);
        mMessageNotification= prefs.getBoolean("MessageNotification", true);
        mVibration          = prefs.getBoolean("Vibration", true);
        mSound              = prefs.getBoolean("Sound", true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(mActivity == null) mActivity = (ClientActivity)getActivity();
        if(mUserAccount == null) mUserAccount = mActivity.getUserAccount();
        if(mPresense == null) mPresense = mActivity.getUserPresence();

        mAccountView = (TextView) view.findViewById(R.id.textAccount);
        mStatusMsgView = (TextView) view.findViewById(R.id.textStatusMsg);
        mAppNameView = (TextView) view.findViewById(R.id.appname);
        mAppVersionVIew = (TextView) view.findViewById(R.id.appversion);

        mButtonLogout = (Button) view.findViewById(R.id.button_logout);
        mButtonDropTable = (Button) view.findViewById(R.id.button_drop_table);
        mBoxStatusMsg = view.findViewById(R.id.box_status);
        mBoxMailto = view.findViewById(R.id.mailto);

        mSwitchRunningNotification  = (Switch) view.findViewById(R.id.toggle_keep_notify);
        mSwitchMessageNotification  = (Switch) view.findViewById(R.id.toggle_incomming_notify);
        mSwitchVibration            = (Switch) view.findViewById(R.id.toggle_vibration);
        mSwitchSound                = (Switch) view.findViewById(R.id.toggle_sound);

        mSwitchRunningNotification.setChecked(mRunningNotification);
        mSwitchMessageNotification.setChecked(mMessageNotification);
        mSwitchVibration.setChecked(mVibration);
        mSwitchSound.setChecked(mSound);

        PackageManager pm = mActivity.getPackageManager();
        PackageInfo pi = null;

        try {
            pi = pm.getPackageInfo(mActivity.getPackageName(),PackageManager.GET_META_DATA);
            mAppVersionVIew.setText("(v" + pi.versionName + ")");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mSwitchRunningNotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getView().playSoundEffect(android.view.SoundEffectConstants.CLICK);
                mRunningNotification = b;
                updateSetting();
            }
        });
        mSwitchMessageNotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getView().playSoundEffect(android.view.SoundEffectConstants.CLICK);
                mMessageNotification = b;
                updateSetting();
            }
        });
        mSwitchVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getView().playSoundEffect(android.view.SoundEffectConstants.CLICK);
                mVibration = b;
                updateSetting();
            }
        });
        mSwitchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getView().playSoundEffect(android.view.SoundEffectConstants.CLICK);
                mSound = b;
                updateSetting();
            }
        });

        mBoxStatusMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editStatusMessage();
            }
        });
        mStatusMsgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBoxStatusMsg.callOnClick();
            }
        });
        mBoxMailto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mailto();
            }
        });
        mButtonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.doLogout();
            }
        });
        mButtonDropTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteTable();
            }
        });

        isReady = true;
        refreshSettings();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            // 보인다.
            if(isReady) refreshSettings();
        } else {
            // 안보인다.
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    public void refreshSettings() {
        if(this.isReady) {
            mAccountView.setText(mUserAccount);
            mStatusMsgView.setText(mPresense.getStatusMsg());
        }
    }

    public void updateSetting() {
        SharedPreferences prefs = mActivity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean("RunningNotification", mRunningNotification);
        ed.putBoolean("MessageNotification", mMessageNotification);
        ed.putBoolean("Vibration", mVibration);
        ed.putBoolean("Sound", mSound);
        ed.commit();

        Bundle bundleSettings = new Bundle();
        bundleSettings.putBoolean("RunningNotification", mRunningNotification);
        bundleSettings.putBoolean("MessageNotification", mMessageNotification);
        bundleSettings.putBoolean("Vibration", mVibration);
        bundleSettings.putBoolean("Sound", mSound);
        mActivity.updateSetting(bundleSettings);
    }

    public void editStatusMessage() {
        Intent intent = new Intent(mActivity, EditingActivity.class);
        intent.putExtra("original_status_message", mPresense.getStatusMsg());
        startActivityForResult(intent, EDIT_STATUS_MSG);
    }

    public void deleteTable() {
        ConfirmationDialog dialog = new ConfirmationDialog();
        dialog.setDialogTitle("Delete Chatting Log");
        dialog.setDialogContent("Are you sure? \nDeleted message cannot be restored");
        dialog.setPositiveOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.getSQLiteDbAdapter().deleteAllRecords(mActivity.getUserID());
            }
        });

        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        Fragment prev = mActivity.getSupportFragmentManager().findFragmentByTag("dialog");

        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        dialog.show(ft, "dialog");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode){
            case EDIT_STATUS_MSG:
                if(resultCode == Activity.RESULT_OK){
                    mPresense.setStatusMsg( intent.getStringExtra("statusMsg") );
                    mActivity.updatePresence(mPresense);
                    mActivity.refreshStatus();
                    refreshSettings();
                }
            default:
                break;
        } //end of switch
    }

    public void mailto() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "rok0810@gmail.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "I love you");
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }
}