package com.rokoroku.lolmessenger.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rokoroku.lolmessenger.ClientActivity;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ParcelableMatchHistory;
import com.rokoroku.lolmessenger.classes.ParcelableRoster;
import com.rokoroku.lolmessenger.utilities.MatchlistViewAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Youngrok Kim on 13. 8. 22.
 */
public class SummonerInformationDialogFragment extends DialogFragment {

    private String mUserID = null;
    private ClientActivity mActivity = null;
    private Date mFWOTD = null;
    private ArrayList<ParcelableMatchHistory> mMatchHistory = null;
    private ParcelableRoster mRoster = null;

    private View mSummonerView;
    private View mLoadingView;

    private TextView mSummonerNameTextView;
    private TextView mSummonerLevelTextView;
    private ImageView mSummonerIcon;
    private View mFWOTDview;
    private TextView mFWOTDtimeTextView;
    private ImageView mFWOTDtimeIcon;
    private TextView mTierTextView;
    private TextView mTierGroupTextView;
    private TextView mTierStatTextView;
    private ImageView mTierIcon;
    private ListView mMatchList;

    private boolean isReady = false;

    private Timer mTimer = null;

    public SummonerInformationDialogFragment() {
    }

    public SummonerInformationDialogFragment(ClientActivity activity, String userID) {
        this.mActivity = activity;
        this.mUserID = userID;
    }

    public SummonerInformationDialogFragment(ArrayList<ParcelableMatchHistory> matchHistories, ParcelableRoster roster, Date fwotd) {
        this.mMatchHistory = matchHistories;
        this.mRoster = roster;
        this.mFWOTD = fwotd;
        this.mUserID = roster.getUserID();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        // creating the fullscreen dialog
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_summoner, container, false);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mActivity.dismissSummonerInformationDialog();
        super.onDismiss(dialog);
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            if(mLoadingView.getVisibility() == View.VISIBLE) refreshSummoner();
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(mActivity == null) mActivity = (ClientActivity)getActivity();

        mLoadingView = view.findViewById(R.id.loading_status);
        mSummonerView = view.findViewById(R.id.summoner_form);
        mLoadingView.setVisibility(View.VISIBLE);
        mSummonerView.setVisibility(View.GONE);

        mSummonerNameTextView = (TextView) view.findViewById(R.id.summonerName);
        mSummonerLevelTextView = (TextView) view.findViewById(R.id.summonerLevel);
        mSummonerIcon = (ImageView) view.findViewById(R.id.summonerIcon);
        mFWOTDtimeTextView = (TextView) view.findViewById(R.id.FWOTDtime);
        mFWOTDtimeIcon = (ImageView) view.findViewById(R.id.FWOTDicon);
        mFWOTDview = view.findViewById(R.id.FWOTDview);
        mTierTextView = (TextView) view.findViewById(R.id.tierText);
        mTierGroupTextView = (TextView) view.findViewById(R.id.tierGroup);
        mTierStatTextView = (TextView) view.findViewById(R.id.tierStat);
        mTierIcon = (ImageView) view.findViewById(R.id.tierIcon);
        mMatchList = (ListView) view.findViewById(R.id.matchlistView);

        if(mActivity.isReady()) {
            refreshSummoner();
            isReady = true;
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("dialog","destroy");
    }

    public void refreshSummoner() {

        if(isReady) {
            if(mRoster != null && mMatchHistory != null) {
                mSummonerNameTextView.setText(mRoster.getUserName());
                mSummonerLevelTextView.setText("Level " + mRoster.getSummonerLevel());
                int iconID = 0;
                try {
                    iconID = R.drawable.class.getField("profile_icon_" + mRoster.getProfileIcon() ).getInt(null);
                    mSummonerIcon.setImageResource(iconID);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //mFWOTDtimeTextView = (TextView) mActivity.findViewById(R.id.FWOTDtime);
                //mFWOTDtimeIcon = (ImageView) mActivity.findViewById(R.id.FWOTDicon);
                //mFWOTDview = mActivity.findViewById(R.id.FWOTDview);
                if(!mRoster.getRankedLeagueTier().equals("UNRANKED")) {
                    mTierTextView.setText(mRoster.getRankedLeagueTier() + " " + mRoster.getRankedLeagueDivision());
                    mTierGroupTextView.setText(mRoster.getRankedLeagueName());
                    mTierStatTextView.setText(mRoster.getLeaguePoints() + " LP");
                    if(mRoster.getRankedLeagueTier().equals("CHALLENGER")) iconID = R.drawable.medals_challenger;
                    else if(mRoster.getRankedLeagueTier().equals("DIAMOND")) iconID = R.drawable.medals_diamond;
                    else if(mRoster.getRankedLeagueTier().equals("PLATINUM")) iconID = R.drawable.medals_platinum;
                    else if(mRoster.getRankedLeagueTier().equals("GOLD")) iconID = R.drawable.medals_gold;
                    else if(mRoster.getRankedLeagueTier().equals("SILVER")) iconID = R.drawable.medals_silver;
                    else if(mRoster.getRankedLeagueTier().equals("BRONZE")) iconID = R.drawable.medals_bronze;
                } else {
                    mTierTextView.setText("");
                    mTierGroupTextView.setText(mRoster.getRankedLeagueTier());
                    mTierStatTextView.setText("");
                    iconID = R.drawable.medals_unranked;
                }
                mTierIcon.setImageResource(iconID);
                //mMatchList = (ListView) mActivity.findViewById(R.id.listView);
                MatchlistViewAdapter adapter = new MatchlistViewAdapter( mMatchHistory );
                adapter.setInflater((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), getActivity());
                mMatchList.setAdapter( adapter );
                mLoadingView.setVisibility(View.GONE);
                mSummonerView.setVisibility(View.VISIBLE);
            } else {
                mActivity.requestMatchHistory(mUserID);
            }
        }
    }

    public void updateSummoner() {
        mActivity.requestMatchHistory(mUserID);
    }

    public Date getFWOTD() {
        return mFWOTD;
    }

    public void setFWOTD(Date FWOTD) {
        this.mFWOTD = FWOTD;
        if(FWOTD.getTime() != 0) {
            FWOTD_TimerTask timerTask = new FWOTD_TimerTask();
            mTimer = new Timer();
            mTimer.schedule(timerTask, 500, 1000);
        } else {
            mFWOTDview.setVisibility(View.INVISIBLE);
        }
    }

    public ArrayList<ParcelableMatchHistory> getMatchHistory() {
        return mMatchHistory;
    }

    public void setMatchHistory(ArrayList<ParcelableMatchHistory> MatchHistory) {
        this.mMatchHistory = MatchHistory;
    }

    public ParcelableRoster getRoster() {
        return mRoster;
    }

    public void setRoster(ParcelableRoster mRoster) {
        this.mRoster = mRoster;
    }

    class FWOTD_TimerTask extends TimerTask {
        public void run() {
            mHandler.post(mUpdateTimeTask);
        }
    }
    private Handler mHandler = new Handler();

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            Date current = new Date();

            long diffInSeconds = (mFWOTD.getTime() - current.getTime()) / 1000;

            if(diffInSeconds>0) {
                long diff[] = new long[] { 0, 0, 0, 0 };
                /* sec */  diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
                /* min */  diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
                /* hours */diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
                /* days */ diff[0] = (diffInSeconds = (diffInSeconds / 24));

                String dateString = String.format("%d:%02d:%02d",
                        (int)diff[1],
                        (int)diff[2],
                        (int)diff[3]) ;

                mFWOTDtimeTextView.setText(dateString);
            }
            else {
                mTimer.cancel();
                mFWOTDtimeIcon.setImageResource(R.drawable.icon_bonus_on);
                mFWOTDtimeTextView.setText("Available");
            }
        }
    };

}