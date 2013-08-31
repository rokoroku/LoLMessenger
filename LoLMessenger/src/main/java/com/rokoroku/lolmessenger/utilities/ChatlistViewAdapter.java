package com.rokoroku.lolmessenger.utilities;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rokoroku.lolmessenger.ClientActivity;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ChatInformation;
import com.rokoroku.lolmessenger.classes.ParcelableRoster;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * Created by Youngrok Kim on 13. 8. 20.
 */
public class ChatlistViewAdapter extends BaseAdapter{

    private ArrayList<ChatInformation> mChatList;
    private Map<String, ParcelableRoster> mRosterMap;
    private LayoutInflater mInflater;
    private Activity mActivity;

    public ChatlistViewAdapter(ArrayList<ChatInformation> listItem, Map<String, ParcelableRoster> rosterMap) {
        this.mChatList = listItem;
        this.mRosterMap = rosterMap;
    }

    public void setInflater(LayoutInflater mInflater, Activity act) {
        this.mInflater = mInflater;
        mActivity = act;
    }

    @Override
    public int getCount() {
        return mChatList.size();
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

        final ChatInformation tempEntry = mChatList.get(i);
        ParcelableRoster tempBuddy = mRosterMap.get(tempEntry.getBuddyID());

        //view inflate of my message
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.row_chatlist, null);
        }

        //edit Name TextView
        TextView viewName = (TextView) convertView.findViewById(R.id.textName);
        viewName.setText(tempBuddy.getUserName());

        //edit Name TextView
        TextView viewMessage = (TextView) convertView.findViewById(R.id.textMessage);
        viewMessage.setText(tempEntry.getLatestMessage().getBody());

        //edit Timestamp
        TextView viewTime = (TextView) convertView.findViewById(R.id.textTime);

        Date today = new Date();
        String timeString;
        long diff = ( today.getTime() - tempEntry.getLatestMessage().getTimeStamp() ) / (24 * 60 * 60 * 1000);

        if(diff == 0) {
            timeString = new String(String.format("%tR", tempEntry.getLatestMessage().getTimeStamp()));
        } else {
            timeString = new String(String.format("%tb %<te", tempEntry.getLatestMessage().getTimeStamp()));
        }
        viewTime.setText(timeString);

        //status icon & name color
        ImageView statusIcon = (ImageView) convertView.findViewById(R.id.statusIcon);
        if(!tempBuddy.getAvailablity().equals("unavailable")) {
            if(tempBuddy.getMode().equals("chat")) {
                statusIcon.setImageResource(R.drawable.icon_green);
                viewName.setTextColor(Color.parseColor("#1db918"));
            } else if(tempBuddy.getMode().equals("away")) {
                statusIcon.setImageResource(R.drawable.icon_red);
                viewName.setTextColor(Color.RED);
            } else if(tempBuddy.getMode().equals("dnd")) {
                statusIcon.setImageResource(R.drawable.icon_yellow);
                viewName.setTextColor(Color.parseColor("#ffe400"));
            }
        } else {
            statusIcon.setImageResource(R.drawable.icon_black);
            viewName.setTextColor(Color.parseColor("#646464"));
        }

        //reply icon
        ImageView replyIcon = (ImageView) convertView.findViewById(R.id.replyIcon);
        if(tempEntry.getLatestMessage().getFromID().equals(tempEntry.getBuddyID())) {
            replyIcon.setVisibility(View.GONE);
        } else {
            replyIcon.setVisibility(View.VISIBLE);
        }

        //message count
        final TextView countView = (TextView) convertView.findViewById(R.id.countIcon);
        if(tempEntry.getCount()>0) {
            countView.setText(String.format("%d", tempEntry.getCount()));
            countView.setVisibility(View.VISIBLE);
        } else {
            countView.setVisibility(View.GONE);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(activity, tempEntry.getUserName(),Toast.LENGTH_SHORT).show();
                countView.setVisibility(View.GONE);
                ( (ClientActivity)mActivity ).openChat(tempEntry.getBuddyID());
            }
        });

        return convertView;
    }
}
