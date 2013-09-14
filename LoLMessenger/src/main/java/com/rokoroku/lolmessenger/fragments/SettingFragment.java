package com.rokoroku.lolmessenger.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.rokoroku.lolmessenger.ClientActivity;
import com.rokoroku.lolmessenger.LolMessengerApplication;
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
    private Switch mSwitchShowToast;
    private Switch mSwitchVibration;
    private Switch mSwitchSound;
    private Switch mSwitchShowGameQueueType;
    private Switch mSwitchShowPlayingChampion;
    private Switch mSwitchShowTimestamp;

    private boolean mRunningNotification;
    private boolean mMessageNotification;
    private boolean mFriendRequestNotification;
    private boolean mShowToast;
    private boolean mVibration;
    private boolean mSound;
    private boolean mShowGameQueuetype;
    private boolean mShowPlayingChampion;
    private boolean mShowTimestamp;

    private boolean isEditing = false;
    private boolean isReady = false;


    public SettingFragment() {
    }

    public SettingFragment(ClientActivity activity, ParcelableRoster userRoster) {
        this.mActivity = activity;
        this.mPresense = userRoster;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        // the content
        //final RelativeLayout root = new RelativeLayout(getActivity());
        //root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // retrieve display dimensions
        DisplayMetrics metrics = new DisplayMetrics();
        //getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        //int width = (int)( metrics.widthPixels * 0.8);
        //int height = (int)( metrics.heightPixels * 0.8);

        // creating the fullscreen dialog
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        //dialog.setContentView(root);

        return dialog;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LolMessengerApplication application = (LolMessengerApplication)getActivity().getApplication();

        mRunningNotification= application.isRunningNotification();
        mMessageNotification= application.isMessageNotification();
        mVibration          = application.isVibration();
        mSound              = application.isSound();
        mShowGameQueuetype  = application.isShowGameQueuetype();
        mShowPlayingChampion= application.isShowPlayingChampion();
        mShowTimestamp      = application.isShowTimestamp();
        mShowToast          = application.isShowToast();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(mActivity == null) mActivity = (ClientActivity)getActivity();
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
        mSwitchShowToast            = (Switch) view.findViewById(R.id.toggle_toast);
        mSwitchVibration            = (Switch) view.findViewById(R.id.toggle_vibration);
        mSwitchSound                = (Switch) view.findViewById(R.id.toggle_sound);
        mSwitchShowGameQueueType    = (Switch) view.findViewById(R.id.toggle_show_gametype);
        mSwitchShowPlayingChampion  = (Switch) view.findViewById(R.id.toggle_show_champion);
        mSwitchShowTimestamp        = (Switch) view.findViewById(R.id.toggle_show_timestamp);


        mSwitchRunningNotification.setChecked(mRunningNotification);
        mSwitchMessageNotification.setChecked(mMessageNotification);
        mSwitchVibration.setChecked(mVibration);
        mSwitchSound.setChecked(mSound);
        mSwitchShowGameQueueType.setChecked(mShowGameQueuetype);
        mSwitchShowPlayingChampion.setChecked(mShowPlayingChampion);
        mSwitchShowTimestamp.setChecked(mShowTimestamp);
        mSwitchShowToast.setChecked(mShowToast);

        PackageManager pm = mActivity.getPackageManager();
        PackageInfo pi = null;

        //get version name
        try {
            pi = pm.getPackageInfo(mActivity.getPackageName(),PackageManager.GET_META_DATA);
            mAppVersionVIew.setText(new String("(v" + pi.versionName + ")"));
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
        mSwitchShowGameQueueType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getView().playSoundEffect(android.view.SoundEffectConstants.CLICK);
                mShowGameQueuetype = b;
                updateSetting();
            }
        });
        mSwitchShowPlayingChampion.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getView().playSoundEffect(android.view.SoundEffectConstants.CLICK);
                mShowPlayingChampion = b;
                updateSetting();
            }
        });
        mSwitchShowTimestamp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getView().playSoundEffect(android.view.SoundEffectConstants.CLICK);
                mShowTimestamp = b;
                updateSetting();
            }
        });
        mSwitchShowToast.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getView().playSoundEffect(android.view.SoundEffectConstants.CLICK);
                mShowToast = b;
                updateSetting();
            }
        });


        mBoxStatusMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editStatusMessage();
            }
        });
        /*mStatusMsgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBoxStatusMsg.callOnClick();
            }
        });*/
        mBoxMailto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mailto();
            }
        });
        mButtonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.doLogout(true);
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
            if(isReady) refreshSettings();
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    public void refreshSettings() {
        if(this.isReady) {
            mAccountView.setText(new String(LolMessengerApplication.getUserAccount()));
            mStatusMsgView.setText(new String(mPresense.getStatusMsg()));
        }
    }

    public void updateSetting() {

        Bundle bundleSettings = new Bundle();
        bundleSettings.putBoolean("RunningNotification", mRunningNotification);
        bundleSettings.putBoolean("MessageNotification", mMessageNotification);
        bundleSettings.putBoolean("Vibration", mVibration);
        bundleSettings.putBoolean("Sound", mSound);
        bundleSettings.putBoolean("ShowPlayingChampion", mShowPlayingChampion);
        bundleSettings.putBoolean("ShowGameQueueType", mShowGameQueuetype);
        bundleSettings.putBoolean("ShowTimestamp", mShowTimestamp);
        bundleSettings.putBoolean("ShowToast", mShowToast);

        mActivity.updateSetting(bundleSettings);
    }

    public void editStatusMessage() {
        final EditTextDialog dialog = new EditTextDialog();
        dialog.setDialogContent(mPresense.getStatusMsg());
        dialog.setPositiveOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresense.setStatusMsg(dialog.getText());
                mActivity.updatePresence(mPresense);
                mActivity.refreshStatus();
                refreshSettings();
            }
        });
        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        Fragment prev = mActivity.getSupportFragmentManager().findFragmentByTag("dialog_2");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        dialog.show(ft, "dialog_2");

        //mStatusEditTextView.setText(mStatusMsgView.getText());
        //mBoxStatusMsg.setVisibility(View.GONE);
        //mBoxEditStatusMsg.setVisibility(View.VISIBLE);
        //isEditing = true;

        //Intent intent = new Intent(mActivity, EditingActivity.class);
        //intent.putExtra("original_status_message", mPresense.getStatusMsg());
        //startActivityForResult(intent, EDIT_STATUS_MSG);
    }

    public void deleteTable() {
        ConfirmationDialog dialog = new ConfirmationDialog();
        dialog.setDialogTitle(mActivity.getString(R.string.dialog_title_delete_all_chatting_log));
        dialog.setDialogContent(mActivity.getString(R.string.dialog_content_delete_chatting_log));
        dialog.setPositiveOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.getSQLiteDbAdapter().deleteAllRecords();
                dismiss();
            }
        });

        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        Fragment prev = mActivity.getSupportFragmentManager().findFragmentByTag("dialog_2");

        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        dialog.show(ft, "dialog_2");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode){
            case EDIT_STATUS_MSG:
                if(resultCode == Activity.RESULT_OK){
                    String text = intent.getStringExtra("statusMsg");
                    mPresense.setStatusMsg(text);
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
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Using LOL Messenger for Android.");
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

}