package com.rokoroku.lolmessenger.utilities;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rokoroku.lolmessenger.ClientActivity;
import com.rokoroku.lolmessenger.R;
import com.rokoroku.lolmessenger.classes.ParcelableRoster;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Youngrok Kim on 13. 8. 15.
 */
@SuppressWarnings("unchecked")
public class RosterAdapter extends BaseExpandableListAdapter {

    final static int LIST_TYPE_1 = 1;
    final static int LIST_TYPE_2 = 2;

    private Map<String, ParcelableRoster> mRosterMap = null;
    private ArrayList<String> groupItem = new ArrayList<String>();
    private ArrayList<Object> childItem = new ArrayList<Object>();
    private LayoutInflater mInflater;
    private Activity activity;

    public RosterAdapter(Map<String, ParcelableRoster> rosterMap, ArrayList<String> rosterGroup) {
        this.mRosterMap = rosterMap;

        ArrayList<String> offlineChild = new ArrayList<String>();
        groupItem = rosterGroup;
        childItem.clear();
        for(int i=0; i<rosterGroup.size(); i++) {
            childItem.add(new ArrayList<String>());
        }

        for(ParcelableRoster entry : mRosterMap.values()) {
            ArrayList<String> tempChild = new ArrayList<String>();
            for(int i=0; i<rosterGroup.size()-1; i++ ) {
                //put into offline group
                if(entry.getAvailablity().equals("unavailable")) {
                    ((ArrayList<String>)childItem.get(rosterGroup.size()-1)).add( entry.getUserID() );
                    break;
                }
                //put into same group
                else if(entry.getGroup().equals(rosterGroup.get(i))) {
                    ((ArrayList<String>)childItem.get(i)).add( entry.getUserID() );
                    break;
                }
            }
        }
    }

    public void setInflater(LayoutInflater mInflater, Activity act) {
        this.mInflater = mInflater;
        activity = act;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        //Child Entry 획득
        final ParcelableRoster tempEntry = mRosterMap.get( ((ArrayList<String>) childItem.get(groupPosition)).get(childPosition) );

        //Online Child
        if(!groupItem.get(groupPosition).equals("Offline")) {

            //view inflate
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_buddylist_buddy, null);
                convertView.setTag(LIST_TYPE_1);
            } else if (!convertView.getTag().equals(LIST_TYPE_1)) {
                convertView = mInflater.inflate(R.layout.row_buddylist_buddy, null);
                convertView.setTag(LIST_TYPE_1);
            }

            //edit Name TextView
            TextView nameText = (TextView) convertView.findViewById(R.id.textViewName);
            nameText.setText(tempEntry.getUserName());

            //edit Status TextView
            TextView statusText = (TextView) convertView.findViewById(R.id.textViewGameStatus);
            String statusString;

            //edit Status TextView
            ImageView icon = (ImageView) convertView.findViewById(R.id.statusImageView);
            if(tempEntry.getMode().equals("dnd")) {
                if(tempEntry.getGameStatus().equals("inGame")) {
                    statusString = new String("In Game : " + tempEntry.getSkinname() + " (" + ((System.currentTimeMillis() - tempEntry.getTimeStamp())/60000) + "min)");
                }
                else if(tempEntry.getGameStatus().equals("teamSelect")) {
                    statusString = "Selecting Team";
                }
                else if(tempEntry.getGameStatus().equals("hostingPracticeGame")) {
                    statusString = "Hosting a Practice Game";
                }
                else {
                    statusString = "Looking for a Game";
                }
                icon.setImageResource(R.drawable.icon_yellow);
                statusText.setTextColor( Color.parseColor("#ffe400") );
            } else if(tempEntry.getMode().equals("away")) {
                statusString = "Away";
                icon.setImageResource(R.drawable.icon_red);
                statusText.setTextColor( Color.RED );
            } else { // if(tempEntry.getMode().equals("chat")) .. 안드로이드 메신저 쓰는사람은 이 값이 없음.
                icon.setImageResource(R.drawable.icon_green);
                statusText.setTextColor( Color.parseColor("#1DB918") );
                if(tempEntry.getStatusMsg() != null && !tempEntry.getStatusMsg().isEmpty() ) {
                    statusString = tempEntry.getStatusMsg();
                } else {
                    statusString = "Online";
                }
            }
            statusText.setText(statusString);

            //edit icon view
            ImageView profileIcon = (ImageView) convertView.findViewById(R.id.profileicon);
            TextView substituteText = (TextView) convertView.findViewById(R.id.profileiconSubstituteText);
            try {
                int iconID = R.drawable.class.getField("profile_icon_" + tempEntry.getProfileIcon() ).getInt(null);
                profileIcon.setImageResource(iconID);
                profileIcon.setVisibility(View.VISIBLE);
                substituteText.setVisibility(View.GONE);
            } catch (Exception e) {
                substituteText.setText(String.valueOf(tempEntry.getProfileIcon()));
                substituteText.setVisibility(View.VISIBLE);
                profileIcon.setVisibility(View.GONE);
                e.printStackTrace();
            }


            //set onClick Listener
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Toast.makeText(activity, tempEntry.getUserName(),Toast.LENGTH_SHORT).show();
                    ( (ClientActivity)activity ).openSummonerDialog(tempEntry.getUserID());
                    //( (ClientActivity)activity ).openChat(tempEntry.getUserID());
                }
            });
        }
        //Online Child 별도 처리
        else {
            //view inflate
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_buddylist_offlinebuddy, null);
                convertView.setTag(LIST_TYPE_2);
            } else if (!convertView.getTag().equals(LIST_TYPE_2)) {
                convertView = mInflater.inflate(R.layout.row_buddylist_offlinebuddy, null);
                convertView.setTag(LIST_TYPE_2);
            }

            //edit Name TextView
            TextView nameText = (TextView) convertView.findViewById(R.id.textViewName2);
            nameText.setText(tempEntry.getUserName());

        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return ((ArrayList<String>) childItem.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public int getGroupCount() {
        return groupItem.size();
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.row_buddylist_group, null);
        }
        ((TextView)convertView.findViewById(R.id.textView)).setText((groupItem.get(groupPosition)));
        if(isExpanded) {
            ((ImageView)convertView.findViewById(R.id.imageView)).setImageResource(R.drawable.ic_indicator_expanded);
        } else {
            ((ImageView)convertView.findViewById(R.id.imageView)).setImageResource(R.drawable.ic_indicator_collapsed);
        }
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

}