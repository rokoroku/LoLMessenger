package com.rokoroku.lolmessenger.utilities;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rokoroku.lolmessenger.ChatActivity;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ParcelableMessage;

import java.util.ArrayList;
import java.util.Date;

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
        if(tempEntry.getFromID().equals( ((ChatActivity)activity).getUserID() )) {
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
        messageView.setText(tempEntry.getBody());

        //edit Timestamp
        TextView timestampView = (TextView) convertView.findViewById(R.id.textViewTimestamp);
        Date today = new Date();
        String timeString;

        long diff = ( today.getTime() - tempEntry.getTimeStamp() ) / (24 * 60 * 60 * 1000);
        if(diff == 0) {
            timeString = new String(String.format("%tR", tempEntry.getTimeStamp()));
        } else {
            timeString = new String(String.format("%tb %<te", tempEntry.getTimeStamp()));
        }

        timestampView.setText(timeString);

        return convertView;
    }
}
