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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
        setItem(listItem, rosterMap);
    }

    public void setItem(ArrayList<ChatInformation> listItem, Map<String, ParcelableRoster> rosterMap) {
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

        try {
            //edit Name TextView
            TextView viewName = (TextView) convertView.findViewById(R.id.textName);
            if(tempBuddy == null) {
                String unknownEntryName = ((ClientActivity) mActivity).getUnknownRosterName(tempEntry.getBuddyID());
                if(unknownEntryName != null ) {
                    viewName.setText(unknownEntryName);
                }
                else {
                    viewName.setText(mActivity.getString(R.string.unknown_entry));
                }
            }
            else viewName.setText(tempBuddy.getUserName());

            //edit Name TextView
            TextView viewMessage = (TextView) convertView.findViewById(R.id.textMessage);
            String trimmedString = tempEntry.getLatestMessage().getBody();
            //if(trimmedString.contains("\n")) { trimmedString.substring(0,trimmedString.indexOf("\n")); }
            if(trimmedString.contains("\n")) {
                trimmedString = trimmedString.substring(0,trimmedString.indexOf('\n'));
            }
            viewMessage.setText(trimmedString);

            //edit Timestamp
            TextView viewTime = (TextView) convertView.findViewById(R.id.textTime);

            SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date today = new Date();
            String timeString;

            if(removeTime(new Date(tempEntry.getLatestMessage().getTimeStamp())).before(removeTime(today))) {
                if( Locale.getDefault().getLanguage().equals("ko") ) { //\uC77C == 일
                    timeString = String.format("%tb %<te\uC77C", tempEntry.getLatestMessage().getTimeStamp());
                } else {
                    timeString = String.format("%tb %<te", tempEntry.getLatestMessage().getTimeStamp());
                }
            } else {
                timeString = String.format("%tR", tempEntry.getLatestMessage().getTimeStamp());
            }

            viewTime.setText(timeString);

            //status icon & name color
            ImageView statusIcon = (ImageView) convertView.findViewById(R.id.statusIcon);
            if(tempBuddy != null && !tempBuddy.getAvailablity().equals("unavailable")) {
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

            convertView.setVisibility(View.VISIBLE);
            return convertView;

        } catch (NullPointerException e) {
            e.printStackTrace();
            convertView.setVisibility(View.GONE);
            return convertView;
        }
    }

    public Date removeTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
