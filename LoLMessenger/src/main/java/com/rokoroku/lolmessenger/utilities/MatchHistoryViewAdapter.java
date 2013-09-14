package com.rokoroku.lolmessenger.utilities;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rokoroku.lolmessenger.LolMessengerApplication;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ParcelableMatchHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by Youngrok Kim on 13. 8. 28.
 */
public class MatchHistoryViewAdapter extends BaseAdapter{

    private int LIST_TYPE_1 = 101;

    private ArrayList<ParcelableMatchHistory> mListItem;
    private LayoutInflater mInflater;
    private Activity activity;

    public MatchHistoryViewAdapter(ArrayList<ParcelableMatchHistory> listItem) {
        this.mListItem = listItem;
        Collections.sort(mListItem, new Comparator<ParcelableMatchHistory>() {
            @Override
            public int compare(ParcelableMatchHistory parcelableMatchHistory, ParcelableMatchHistory parcelableMatchHistory2) {
                return (int) ((parcelableMatchHistory2.getMatchDate().getTime() - parcelableMatchHistory.getMatchDate().getTime())/60000);
            }
        });
    }

    public void setInflater(LayoutInflater mInflater, Activity act) {
        this.mInflater = mInflater;
        activity = act;

    }

    @Override
    public int getCount() {
        return mListItem.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ParcelableMatchHistory tempEntry = mListItem.get(i);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.row_matchlist, null);
        }

        TextView viewVictory = (TextView) convertView.findViewById(R.id.textVictory);
        TextView viewGameType = (TextView) convertView.findViewById(R.id.textGameType);
        TextView viewGameMode = (TextView) convertView.findViewById(R.id.textGameMode);
        TextView viewGameMap = (TextView) convertView.findViewById(R.id.textGameMap);
        TextView viewGameStat1 = (TextView) convertView.findViewById(R.id.textStat1);
        TextView viewGameStat2 = (TextView) convertView.findViewById(R.id.textStat2);
        TextView viewGameStat3 = (TextView) convertView.findViewById(R.id.textStat3);
        TextView viewIpEarned = (TextView) convertView.findViewById(R.id.textIpEarned);
        TextView viewDate = (TextView) convertView.findViewById(R.id.textDate);
        ImageView championIcon = (ImageView) convertView.findViewById(R.id.imageChampion);

        try {
            int iconID = R.drawable.class.getField("champion_" + tempEntry.getChampion().toLowerCase().replaceAll("\\p{Punct}|\\p{Space}","")).getInt(null);
            championIcon.setImageResource(iconID);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(tempEntry.isLeave()) { viewVictory.setText(activity.getString(R.string.match_leave)); viewVictory.setTextColor( Color.parseColor("#cc0000")); }
        else if(tempEntry.isVictory()) { viewVictory.setText(activity.getString(R.string.match_victory)); viewVictory.setTextColor(Color.parseColor("#669900")); }
        else { viewVictory.setText(activity.getString(R.string.match_defeat)); viewVictory.setTextColor( Color.parseColor("#cc0000")); }


        String matchType = tempEntry.getMatchType();
        String matchMode = tempEntry.getMatchMode();
        String matchMap = ((LolMessengerApplication)activity.getApplication()).getMapStringMap().get(tempEntry.getMatchMapID());

        if(matchType.contains("ARAM"))          matchType = activity.getString(R.string.match_type_normal);
        else if(matchType.contains("NORMAL"))   matchType = activity.getString(R.string.match_type_normal);
        else if(matchType.contains("UNRANKED")) matchType = activity.getString(R.string.match_type_normal);
        else if(matchType.contains("RANKED"))   matchType = activity.getString(R.string.match_type_ranked);
        else if(matchType.contains("NONE"))     matchType = activity.getString(R.string.match_type_custom);
        else if(matchType.contains("BOT"))      matchType = activity.getString(R.string.match_type_coop);

        if(matchMode.equals("ARAM"))            matchMode = activity.getString(R.string.match_mode_aram);
        else if(matchMode.equals("CLASSIC"))    matchMode = activity.getString(R.string.match_mode_classic);
        else if(matchMode.equals("ODIN"))       matchMode = activity.getString(R.string.match_mode_odin);

        viewGameType.setText("(" + matchType + ")");
        viewGameMode.setText(matchMode);
        viewGameMap.setText(matchMap);

        viewGameStat1.setText(String.valueOf(tempEntry.getKill()));
        viewGameStat2.setText(String.valueOf(tempEntry.getDeath()));
        viewGameStat3.setText(String.valueOf(tempEntry.getAssist()));

        if(tempEntry.getEarnedEXP() == 0) viewIpEarned.setText("+" + tempEntry.getEarnedIP() + activity.getString(R.string.match_ip));
        else viewIpEarned.setText("+" + tempEntry.getEarnedEXP() + activity.getString(R.string.match_exp) + "  +" + tempEntry.getEarnedIP() + activity.getString(R.string.match_ip));

        SimpleDateFormat formatter = new SimpleDateFormat ("yyyy/MM/dd");
        viewDate.setText(formatter.format(tempEntry.getMatchDate()));

        return convertView;
    }
}
