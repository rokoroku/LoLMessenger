package com.rokoroku.lolmessenger.fragments;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.rokoroku.lolmessenger.ClientActivity;
import com.rokoroku.lolmessenger.LolMessengerApplication;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ParcelableRoster;

/**
 * Created by Youngrok Kim on 13. 8. 29.
 */
public class SummonerDialog extends DialogFragment {

    ParcelableRoster mRoster = null;
    ClientActivity mActivity = null;

    TextView viewSummonerName = null;
    TextView viewSummonerLevel = null;
    TextView viewSummonerTier = null;
    TextView viewSummonerTierGroup = null;
    TextView viewSummonerStatus1 = null;
    TextView viewSummonerStatus2 = null;
    TextView viewSummonerStatus3 = null;
    ImageView viewSummonerIcon = null;
    Button buttonChat = null;
    Button buttonLook = null;

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);

        return dialog;
    }

    public SummonerDialog(Context context, ParcelableRoster mRoster) {
        this.mActivity = (ClientActivity) context;
        this.mRoster = mRoster;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_summoner, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewSummonerName = (TextView) view.findViewById(R.id.summoner_name);
        viewSummonerLevel = (TextView) view.findViewById(R.id.summoner_level);
        viewSummonerTier = (TextView) view.findViewById(R.id.tier);
        viewSummonerTierGroup = (TextView) view.findViewById(R.id.tierGroup);
        viewSummonerStatus1 = (TextView) view.findViewById(R.id.status1);
        viewSummonerStatus2 = (TextView) view.findViewById(R.id.status2);
        viewSummonerStatus3 = (TextView) view.findViewById(R.id.timestamp);
        viewSummonerIcon = (ImageView) view.findViewById(R.id.summoner_icon);
        buttonChat = (Button) view.findViewById(R.id.buttonChat);
        buttonLook = (Button) view.findViewById(R.id.buttonLook);

        try {
            int iconID = R.drawable.class.getField("profile_icon_" + mRoster.getProfileIcon() ).getInt(null);
            viewSummonerIcon.setImageResource(iconID);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        viewSummonerName.setText(mRoster.getUserName());
        if(mRoster.getSummonerLevel() > 0 ) {
            viewSummonerLevel.setText(mActivity.getString(R.string.status_level) + " " + mRoster.getSummonerLevel());
        } else {
            viewSummonerLevel.setVisibility(View.INVISIBLE);
        }

        if(mRoster.getRankedLeagueTier() == null || mRoster.getRankedLeagueTier().isEmpty() || mRoster.getRankedLeagueTier().equals("UNRANKED")) {
            viewSummonerTier.setVisibility(View.GONE);
            viewSummonerTierGroup.setVisibility(View.GONE);
        } else {
            viewSummonerTier.setText(mRoster.getRankedLeagueTier() + " " + mRoster.getRankedLeagueDivision());
            viewSummonerTierGroup.setText(mRoster.getRankedLeagueName());
        }

        if(mRoster.getMode().equals("dnd")) {
            if(mRoster.getGameStatus().equals("inGame")) {

                String champ = mRoster.getSkinname();
                if(((LolMessengerApplication) mActivity.getApplication()).getChampStringMap() != null ) try {
                    String champSubstitute = ((LolMessengerApplication)mActivity.getApplication()).getChampStringMap().get(champ);
                    if(champSubstitute != null) champ = champSubstitute;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String inGameString ;
                String matchType = mRoster.getGameQueueType();
                if(matchType.contains("ARAM"))          inGameString = mActivity.getString(R.string.status_inNormalGame);
                else if(matchType.contains("NORMAL"))   inGameString = mActivity.getString(R.string.status_inNormalGame);
                else if(matchType.contains("UNRANKED")) inGameString = mActivity.getString(R.string.status_inNormalGame);
                else if(matchType.contains("RANKED"))   inGameString = mActivity.getString(R.string.status_inRankedGame);
                else if(matchType.contains("NONE"))     inGameString = mActivity.getString(R.string.status_inCustomGame);
                else if(matchType.contains("BOT"))      inGameString = mActivity.getString(R.string.status_inAIGame);
                else inGameString = mActivity.getString(R.string.status_inGame);

                viewSummonerStatus1.setText(inGameString);
                viewSummonerStatus2.setText(mActivity.getString(R.string.status_nowPlayingChamp) + " : " + champ);
                viewSummonerStatus2.setVisibility(View.VISIBLE);
            }
            else if(mRoster.getGameStatus().equals("teamSelect")) {
                viewSummonerStatus1.setText(mActivity.getString(R.string.status_teamSelect));
                viewSummonerStatus2.setVisibility(View.GONE);
            }
            else if(mRoster.getGameStatus().equals("hostingPracticeGame")) {
                viewSummonerStatus1.setText(mActivity.getString(R.string.status_hostingPracticeGame));
                viewSummonerStatus2.setVisibility(View.GONE);
            }
            else if(mRoster.getGameStatus().equals("spectating")) {
                viewSummonerStatus1.setText(mActivity.getString(R.string.status_spectating));
                viewSummonerStatus2.setVisibility(View.GONE);
                viewSummonerStatus3.setVisibility(View.GONE);
            }
            else {
                viewSummonerStatus1.setText(mActivity.getString(R.string.status_lookingNewGame));
                viewSummonerStatus2.setVisibility(View.GONE);
            }
            viewSummonerStatus3.setText(String.valueOf((System.currentTimeMillis() - mRoster.getTimeStamp()) / 60000) + mActivity.getString(R.string.status_timestamp_minute));
            viewSummonerStatus1.setTextColor(Color.parseColor("#ffe400"));
            viewSummonerStatus2.setTextColor(Color.parseColor("#ffe400"));
            viewSummonerStatus3.setTextColor(Color.parseColor("#ffe400"));
        } else if(mRoster.getMode().equals("away")) {
            viewSummonerStatus1.setText(mActivity.getString(R.string.status_away));
            viewSummonerStatus1.setTextColor(Color.RED);
            viewSummonerStatus2.setVisibility(View.GONE);
            viewSummonerStatus3.setVisibility(View.GONE);
        } else { // if(tempEntry.getMode().equals("chat")) .. 안드로이드 메신저 쓰는사람은 이 값이 없음.
            viewSummonerStatus3.setVisibility(View.GONE);
            viewSummonerStatus1.setTextColor(Color.parseColor("#1DB918"));
            viewSummonerStatus1.setText(mActivity.getString(R.string.status_online));
            if(mRoster.getStatusMsg() != null && !mRoster.getStatusMsg().isEmpty() ) {
                viewSummonerStatus2.setTextColor(Color.parseColor("#1DB918"));
                viewSummonerStatus2.setText(mRoster.getStatusMsg());
            } else {
                viewSummonerStatus2.setVisibility(View.GONE);
            }
        }

        buttonChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.openChat(mRoster.getUserID());
                dismiss();
            }
        });

        buttonLook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.openSummonerInformation(mRoster.getUserID());
                dismiss();
            }
        });
    }


}
