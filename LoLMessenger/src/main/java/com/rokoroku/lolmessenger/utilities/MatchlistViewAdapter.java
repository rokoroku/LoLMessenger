package com.rokoroku.lolmessenger.utilities;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ParcelableMatchHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Youngrok Kim on 13. 8. 28.
 */
public class MatchlistViewAdapter extends BaseAdapter{

    private int LIST_TYPE_1 = 101;

    private ArrayList<ParcelableMatchHistory> mListItem;
    private LayoutInflater mInflater;
    private Activity activity;

    public MatchlistViewAdapter(ArrayList<ParcelableMatchHistory> listItem) {
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

        if(tempEntry.isLeave()) { viewVictory.setText("Leave"); viewVictory.setTextColor( Color.parseColor("#cc0000")); }
        else if(tempEntry.isVictory()) { viewVictory.setText("Victory"); viewVictory.setTextColor(Color.parseColor("#669900")); }
        else { viewVictory.setText("Defeat"); viewVictory.setTextColor( Color.parseColor("#cc0000")); }

        viewGameType.setText("(" + tempEntry.getMatchType() + ")");
        viewGameMode.setText(tempEntry.getMatchMode());
        viewGameMap.setText(tempEntry.getMatchMap());

        viewGameStat1.setText(String.valueOf(tempEntry.getKill()));
        viewGameStat2.setText(String.valueOf(tempEntry.getDeath()));
        viewGameStat3.setText(String.valueOf(tempEntry.getAssist()));

        if(tempEntry.getEarnedEXP() == 0) viewIpEarned.setText("+" + tempEntry.getEarnedIP() +"IP");
        else viewIpEarned.setText("+" + tempEntry.getEarnedEXP() +"EXP  +" + tempEntry.getEarnedIP() +"IP");

        SimpleDateFormat formatter = new SimpleDateFormat ("yyyy/MM/dd");
        viewDate.setText(formatter.format(tempEntry.getMatchDate()));

        return convertView;
    }
}
