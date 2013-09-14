package com.rokoroku.lolmessenger.utilities;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rokoroku.lolmessenger.ChatActivity;
import com.rokoroku.lolmessenger.LolMessengerApplication;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ParcelableMessage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Youngrok Kim on 13. 8. 19.
 */
public class MessageViewAdapter extends BaseAdapter{

    final static int LIST_TYPE_1 = 1;
    final static int LIST_TYPE_2 = 2;

    private ArrayList<ParcelableMessage> mListItem;
    private LayoutInflater mInflater;
    private Activity activity;

    public MessageViewAdapter(ArrayList<ParcelableMessage> listItem) {
        this.mListItem = listItem;
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
        ParcelableMessage tempEntry = mListItem.get(i);
        if(tempEntry.getFromID().contains(LolMessengerApplication.getUserJID())) {
            //view inflate of my message
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_chat_user, null);
                convertView.setTag(LIST_TYPE_1);
            } else if (!convertView.getTag().equals(LIST_TYPE_1)) {
                convertView = mInflater.inflate(R.layout.row_chat_user, null);
                convertView.setTag(LIST_TYPE_1);
            }
        } else {
            //view inflate of others message
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_chat_buddy, null);
                convertView.setTag(LIST_TYPE_2);
            } else if (!convertView.getTag().equals(LIST_TYPE_2)) {
                convertView = mInflater.inflate(R.layout.row_chat_buddy, null);
                convertView.setTag(LIST_TYPE_2);
            }
        }
        //edit Name TextView
        TextView messageView = (TextView) convertView.findViewById(R.id.textViewMessage);
        if(tempEntry.getBody().contains("\n")) messageView.setGravity(Gravity.CENTER_HORIZONTAL);
        messageView.setText(tempEntry.getBody());


        //edit Timestamp
        TextView timestampView = (TextView) convertView.findViewById(R.id.textViewTimestamp);

        Date today = new Date();
        String timeString;

        if(removeTime(new Date(tempEntry.getTimeStamp())).before(removeTime(today))) {
            if( Locale.getDefault().getLanguage().equals("ko") ) { //\uC77C == 일
                timeString = String.format("%tb %<te\uC77C\n%<tR", tempEntry.getTimeStamp());
            } else {
                timeString = String.format("%tb %<te\n%<tR", tempEntry.getTimeStamp());
            }
        } else {
            timeString = String.format("%tR", tempEntry.getTimeStamp());
        }

        timestampView.setText(timeString);

        return convertView;
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
